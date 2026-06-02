package com.example.pantrypure.data.dao

import androidx.room.*
import com.example.pantrypure.data.model.Offer
import kotlinx.coroutines.flow.Flow

@Dao
interface OfferDao {
    @Query("SELECT * FROM offers WHERE validUntil >= :currentTime ORDER BY validUntil ASC")
    fun getActiveOffers(currentTime: Long): Flow<List<Offer>>

    @Query("SELECT * FROM offers WHERE itemName = :name AND validUntil >= :currentTime")
    suspend fun getOffersForItem(name: String, currentTime: Long): List<Offer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffer(offer: Offer): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffers(offers: List<Offer>)

    @Delete
    suspend fun deleteOffer(offer: Offer)

    @Query("DELETE FROM offers WHERE validUntil < :currentTime")
    suspend fun deleteExpiredOffers(currentTime: Long)

    @Query("UPDATE offers SET isAddedToShoppingList = :added WHERE id = :offerId")
    suspend fun updateShoppingListStatus(offerId: Long, added: Boolean)
}
