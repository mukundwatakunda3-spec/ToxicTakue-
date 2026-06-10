package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM player_state WHERE id = 1")
    fun getPlayerState(): Flow<PlayerState?>

    @Query("SELECT * FROM player_state WHERE id = 1")
    suspend fun getPlayerStateOneTime(): PlayerState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePlayerState(state: PlayerState)

    @Query("SELECT * FROM car_state")
    fun getAllCars(): Flow<List<CarState>>

    @Query("SELECT * FROM car_state")
    suspend fun getAllCarsOneTime(): List<CarState>

    @Query("SELECT * FROM car_state WHERE id = :carId")
    suspend fun getCarById(carId: String): CarState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCar(car: CarState)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCars(cars: List<CarState>)
}
