package com.naeirae.fincontrol.data

import android.content.Context
import com.naeirae.fincontrol.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.OffsetDateTime

class SharedPreferencesFinancialObjectRepository(
    context: Context,
    seed: List<FinancialObject> = emptyList(),
) : FinancialObjectRepository {
    private val prefs = context.getSharedPreferences("fincontrol_objects", Context.MODE_PRIVATE)
    private val items = MutableStateFlow(load().ifEmpty { seed })

    init {
        if (!prefs.contains(KEY_OBJECTS) && seed.isNotEmpty()) persist(seed)
    }

    override fun observeAll(): Flow<List<FinancialObject>> = items.asStateFlow()

    override suspend fun get(id: String): FinancialObject? = items.value.firstOrNull { it.id == id }

    override suspend fun upsert(item: FinancialObject) {
        items.update { current ->
            val next = current.toMutableList()
            val index = next.indexOfFirst { it.id == item.id }
            val updated = item.copy(updatedAt = OffsetDateTime.now())
            if (index < 0) next += updated else next[index] = updated
            persist(next)
            next
        }
    }

    override suspend fun archive(id: String) {
        items.update { current ->
            val next = current.map { item ->
                if (item.id == id) item.copy(status = ObjectStatus.ARCHIVED, updatedAt = OffsetDateTime.now()) else item
            }
            persist(next)
            next
        }
    }

    private fun persist(value: List<FinancialObject>) {
        val array = JSONArray()
        value.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_OBJECTS, array.toString()).apply()
    }

    private fun load(): List<FinancialObject> {
        val raw = prefs.getString(KEY_OBJECTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) add(array.getJSONObject(index).toFinancialObject())
            }
        }.getOrDefault(emptyList())
    }

    private fun FinancialObject.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", type.name)
        put("title", title)
        putMoney("amount", amount)
        put("status", status.name)
        putNullable("dueDate", dueDate?.toString())
        putNullable("probabilityPercent", probabilityPercent)
        putMoney("protectedAmount", protectedAmount)
        putNullable("capitalRole", capitalRole?.name)
        putMoney("capitalRequired", capitalRequired)
        putMoney("guaranteedSaving", guaranteedSaving)
        putMoney("expectedGain", expectedGain)
        putNullable("annualRatePercent", annualRatePercent)
        putNullable("riskScore", riskScore)
        putNullable("liquidityLockDays", liquidityLockDays)
        putNullable("scalable", scalable)
        put("tags", JSONArray(tags.toList()))
        putNullable("nextAction", nextAction)
        putNullable("notes", notes)
        put("sourceKind", source.kind.name)
        putNullable("sourceLabel", source.label)
        putNullable("sourceUri", source.uri)
        put("sourceCapturedAt", source.capturedAt.toString())
        put("createdAt", createdAt.toString())
        put("updatedAt", updatedAt.toString())
    }

    private fun JSONObject.toFinancialObject(): FinancialObject = FinancialObject(
        id = getString("id"),
        type = FinancialObjectType.valueOf(getString("type")),
        title = getString("title"),
        amount = readMoney("amount"),
        status = ObjectStatus.valueOf(optString("status", ObjectStatus.ACTIVE.name)),
        dueDate = optNullableString("dueDate")?.let(LocalDate::parse),
        probabilityPercent = optNullableInt("probabilityPercent"),
        protectedAmount = readMoney("protectedAmount"),
        capitalRole = optNullableString("capitalRole")?.let(CapitalRole::valueOf),
        capitalRequired = readMoney("capitalRequired"),
        guaranteedSaving = readMoney("guaranteedSaving"),
        expectedGain = readMoney("expectedGain"),
        annualRatePercent = optNullableDouble("annualRatePercent"),
        riskScore = optNullableDouble("riskScore"),
        liquidityLockDays = optNullableInt("liquidityLockDays"),
        scalable = if (has("scalable") && !isNull("scalable")) getBoolean("scalable") else null,
        tags = optJSONArray("tags")?.let { arr -> buildSet { for (i in 0 until arr.length()) add(arr.getString(i)) } } ?: emptySet(),
        nextAction = optNullableString("nextAction"),
        notes = optNullableString("notes"),
        source = SourceReference(
            kind = SourceKind.valueOf(optString("sourceKind", SourceKind.MANUAL.name)),
            label = optNullableString("sourceLabel"),
            uri = optNullableString("sourceUri"),
            capturedAt = optNullableString("sourceCapturedAt")?.let(OffsetDateTime::parse) ?: OffsetDateTime.now(),
        ),
        createdAt = optNullableString("createdAt")?.let(OffsetDateTime::parse) ?: OffsetDateTime.now(),
        updatedAt = optNullableString("updatedAt")?.let(OffsetDateTime::parse) ?: OffsetDateTime.now(),
    )

    private fun JSONObject.putMoney(prefix: String, value: MoneyAmount?) {
        putNullable("${prefix}Amount", value?.amount)
        putNullable("${prefix}Currency", value?.currency?.name)
    }

    private fun JSONObject.readMoney(prefix: String): MoneyAmount? {
        val amount = optNullableDouble("${prefix}Amount") ?: return null
        val currency = optNullableString("${prefix}Currency")?.let(CurrencyCode::valueOf) ?: return null
        return MoneyAmount(amount, currency)
    }

    private fun JSONObject.putNullable(key: String, value: Any?) {
        if (value == null) put(key, JSONObject.NULL) else put(key, value)
    }

    private fun JSONObject.optNullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else getString(key)

    private fun JSONObject.optNullableInt(key: String): Int? =
        if (!has(key) || isNull(key)) null else getInt(key)

    private fun JSONObject.optNullableDouble(key: String): Double? =
        if (!has(key) || isNull(key)) null else getDouble(key)

    companion object {
        private const val KEY_OBJECTS = "objects_json"
    }
}
