package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_state")
data class PlayerState(
    @PrimaryKey val id: Int = 1,
    val money: Int = 15000,
    val activeLocation: String = "Germany", // "Germany" or "USA"
    val activeCarId: String = "m3_e30",
    val energy: Int = 80,
    val miaAffinity: Int = 20,
    val sarahAffinity: Int = 15,
    val leoAffinity: Int = 10,
    val totalMileage: Int = 0
)

@Entity(tableName = "car_state")
data class CarState(
    @PrimaryKey val id: String,
    val name: String,
    val basePrice: Int,
    val isOwned: Boolean,
    val colorHex: String, // e.g. Yas Marina Blue, Alpine White...
    val turboLevel: Int = 1, // 1 to 5
    val tyresLevel: Int = 1, // 1 to 5
    val nitroLevel: Int = 1, // 1 to 5
    val damagePercent: Int = 0 // 0 to 100
)
