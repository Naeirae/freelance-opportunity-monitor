package com.naeirae.fincontrol.data

import android.content.Context

object AppContainer {
    // Production data must never be seeded from real personal financial facts in this public repository.
    private val seedObjects = emptyList<com.naeirae.fincontrol.domain.FinancialObject>()

    private var initialized = false
    private var objectRepository: FinancialObjectRepository = InMemoryFinancialObjectRepository(seedObjects)
    private var linkRepository: FinancialLinkRepository = InMemoryFinancialLinkRepository()

    val financialObjects: FinancialObjectRepository
        get() = objectRepository

    val financialLinks: FinancialLinkRepository
        get() = linkRepository

    fun initialize(context: Context) {
        if (initialized) return
        val appContext = context.applicationContext
        objectRepository = SharedPreferencesFinancialObjectRepository(
            context = appContext,
            seed = seedObjects,
        )
        linkRepository = SharedPreferencesFinancialLinkRepository(appContext)
        initialized = true
    }
}
