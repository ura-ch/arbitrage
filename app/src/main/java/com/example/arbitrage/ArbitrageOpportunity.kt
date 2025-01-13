package com.example.arbitrage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ArbitrageOpportunity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val exchange: String,
    val price: Double
)