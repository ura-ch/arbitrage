package com.example.arbitrage

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OpportunityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(opportunities: List<ArbitrageOpportunity>)

    @Query("SELECT * FROM ArbitrageOpportunity")
    fun getAll(): Flow<List<ArbitrageOpportunity>>
}

@Database(entities = [ArbitrageOpportunity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun opportunityDao(): OpportunityDao
}