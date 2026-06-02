package com.example.pantrypure.data.repository

import android.util.Log
import com.example.pantrypure.data.model.Offer
import com.example.pantrypure.data.model.PantryUnit
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiRepository(apiKey: String) {
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val offerListAdapter = moshi.adapter<List<OfferJson>>(
        Types.newParameterizedType(List::class.java, OfferJson::class.java)
    )

    data class OfferJson(
        val itemName: String,
        val store: String,
        val price: Double,
        val quantity: Double,
        val unit: String,
        val category: String,
        val validUntil: Long? = null
    )

    suspend fun extractOffersFromText(text: String): List<Offer> = withContext(Dispatchers.IO) {
        val prompt = """
            Extract promotional offers from the following text extracted from a flyer or receipt.
            Return the data as a JSON array of objects with these fields:
            - itemName (String)
            - store (String)
            - price (Double)
            - quantity (Double)
            - unit (String, use one of: PIECES, GRAMS, KILOGRAMS, MILLILITERS, LITERS)
            - category (String)
            - validUntil (Long, Unix timestamp in ms or null)

            Text:
            $text
        """.trimIndent()

        try {
            val response = generativeModel.generateContent(
                content {
                    text(prompt)
                }
            )
            val jsonString = response.text?.replace("```json", "")?.replace("```", "")?.trim()
            if (jsonString != null) {
                val extracted = offerListAdapter.fromJson(jsonString) ?: emptyList()
                return@withContext extracted.map {
                    Offer(
                        itemName = it.itemName,
                        store = it.store,
                        price = it.price,
                        offerQuantity = it.quantity,
                        offerUnit = parseUnit(it.unit),
                        category = it.category,
                        validFrom = System.currentTimeMillis(),
                        validUntil = it.validUntil ?: (System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiRepository", "Error extracting offers", e)
        }
        return@withContext emptyList()
    }

    private fun parseUnit(unitStr: String): PantryUnit {
        return try {
            PantryUnit.valueOf(unitStr.uppercase())
        } catch (e: Exception) {
            PantryUnit.PIECES
        }
    }
}
