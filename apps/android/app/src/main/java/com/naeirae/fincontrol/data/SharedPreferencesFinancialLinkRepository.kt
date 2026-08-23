package com.naeirae.fincontrol.data

import android.content.Context
import com.naeirae.fincontrol.domain.CurrencyCode
import com.naeirae.fincontrol.domain.FinancialLink
import com.naeirae.fincontrol.domain.FinancialLinkType
import com.naeirae.fincontrol.domain.MoneyAmount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

class SharedPreferencesFinancialLinkRepository(
    context: Context,
) : FinancialLinkRepository {
    private val prefs = context.getSharedPreferences("fincontrol_links", Context.MODE_PRIVATE)
    private val items = MutableStateFlow(load())

    override fun observeAll(): Flow<List<FinancialLink>> = items.asStateFlow()

    override suspend fun upsert(link: FinancialLink) {
        items.update { current ->
            val next = current.toMutableList()
            val index = next.indexOfFirst { it.id == link.id }
            if (index < 0) next += link else next[index] = link
            persist(next)
            next
        }
    }

    override suspend fun delete(id: String) {
        items.update { current ->
            val next = current.filterNot { it.id == id }
            persist(next)
            next
        }
    }

    private fun persist(value: List<FinancialLink>) {
        val array = JSONArray()
        value.forEach { link ->
            array.put(JSONObject().apply {
                put("id", link.id)
                put("fromObjectId", link.fromObjectId)
                put("toObjectId", link.toObjectId)
                put("type", link.type.name)
                put("probabilityPercent", link.probabilityPercent)
                put("note", link.note ?: JSONObject.NULL)
                put("amount", link.amount?.amount ?: JSONObject.NULL)
                put("currency", link.amount?.currency?.name ?: JSONObject.NULL)
            })
        }
        prefs.edit().putString(KEY_LINKS, array.toString()).apply()
    }

    private fun load(): List<FinancialLink> {
        val raw = prefs.getString(KEY_LINKS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.getJSONObject(index)
                    val amount = if (!json.isNull("amount") && !json.isNull("currency")) {
                        MoneyAmount(json.getDouble("amount"), CurrencyCode.valueOf(json.getString("currency")))
                    } else null
                    add(
                        FinancialLink(
                            id = json.getString("id"),
                            fromObjectId = json.getString("fromObjectId"),
                            toObjectId = json.getString("toObjectId"),
                            type = FinancialLinkType.valueOf(json.getString("type")),
                            amount = amount,
                            probabilityPercent = json.optInt("probabilityPercent", 100),
                            note = if (json.isNull("note")) null else json.getString("note"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val KEY_LINKS = "links_json"
    }
}
