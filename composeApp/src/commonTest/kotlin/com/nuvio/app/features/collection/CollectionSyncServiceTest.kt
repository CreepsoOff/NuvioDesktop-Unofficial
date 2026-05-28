package com.nuvio.app.features.collection

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CollectionSyncServiceTest {
    @Test
    fun emptyRemoteJsonArrayIsTreatedAsEmpty() {
        assertTrue(isRemoteCollectionsEmpty(JsonArray(emptyList())))
    }

    @Test
    fun nullRemoteJsonIsTreatedAsEmpty() {
        assertTrue(isRemoteCollectionsEmpty(JsonNull))
    }

    @Test
    fun nonEmptyRemoteJsonArrayIsNotEmpty() {
        assertFalse(isRemoteCollectionsEmpty(JsonArray(listOf(JsonPrimitive("collection-id")))))
    }
}
