package com.example.data.repository

import com.example.data.database.CarState
import com.example.data.database.GameDao
import com.example.data.database.PlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class GameRepository(private val gameDao: GameDao) {

    val playerState: Flow<PlayerState?> = gameDao.getPlayerState()
    val allCars: Flow<List<CarState>> = gameDao.getAllCars()

    suspend fun initializeDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        // Ensure default player state
        val existingState = gameDao.getPlayerStateOneTime()
        if (existingState == null) {
            gameDao.savePlayerState(PlayerState())
        }

        // Ensure default cars
        val existingCars = gameDao.getAllCarsOneTime()
        if (existingCars.isEmpty()) {
            val defaultCars = listOf(
                CarState(
                    id = "m3_e30",
                    name = "BMW M3 E30 (Retro Outlaw)",
                    basePrice = 0,
                    isOwned = true,
                    colorHex = "#E31E24", // Alpine Red
                    turboLevel = 1,
                    tyresLevel = 1,
                    nitroLevel = 1,
                    damagePercent = 0
                ),
                CarState(
                    id = "m5_e60",
                    name = "BMW M5 E60 (V10 Screamer)",
                    basePrice = 18000,
                    isOwned = false,
                    colorHex = "#1F2E8E", // Imperial Blue
                    turboLevel = 1,
                    tyresLevel = 1,
                    nitroLevel = 1,
                    damagePercent = 0
                ),
                CarState(
                    id = "m4_g82",
                    name = "BMW M4 G82 (TwinTurbo Track)",
                    basePrice = 45000,
                    isOwned = false,
                    colorHex = "#0C7550", // Isle of Man Green
                    turboLevel = 1,
                    tyresLevel = 1,
                    nitroLevel = 1,
                    damagePercent = 0
                ),
                CarState(
                    id = "i8_cyber",
                    name = "BMW i8 (Cyber Hybrid Exotic)",
                    basePrice = 95000,
                    isOwned = false,
                    colorHex = "#02F2EC", // Cyber Cyan
                    turboLevel = 1,
                    tyresLevel = 1,
                    nitroLevel = 1,
                    damagePercent = 0
                )
            )
            gameDao.insertCars(defaultCars)
        }
    }

    suspend fun updatePlayerState(state: PlayerState) = withContext(Dispatchers.IO) {
        gameDao.savePlayerState(state)
    }

    suspend fun updateCarState(car: CarState) = withContext(Dispatchers.IO) {
        gameDao.insertCar(car)
    }

    suspend fun repairCar(carId: String, cost: Int) = withContext(Dispatchers.IO) {
        val player = gameDao.getPlayerStateOneTime() ?: return@withContext
        val car = gameDao.getCarById(carId) ?: return@withContext
        
        if (player.money >= cost && car.damagePercent > 0) {
            gameDao.savePlayerState(player.copy(money = player.money - cost))
            gameDao.insertCar(car.copy(damagePercent = 0))
        }
    }

    suspend fun buyCar(carId: String, price: Int) = withContext(Dispatchers.IO) {
        val player = gameDao.getPlayerStateOneTime() ?: return@withContext
        val car = gameDao.getCarById(carId) ?: return@withContext

        if (player.money >= price && !car.isOwned) {
            gameDao.savePlayerState(player.copy(money = player.money - price, activeCarId = carId))
            gameDao.insertCar(car.copy(isOwned = true))
        }
    }

    suspend fun upgradeCar(carId: String, upgradeType: String, cost: Int) = withContext(Dispatchers.IO) {
        val player = gameDao.getPlayerStateOneTime() ?: return@withContext
        val car = gameDao.getCarById(carId) ?: return@withContext

        if (player.money >= cost) {
            val upgradedCar = when (upgradeType) {
                "turbo" -> if (car.turboLevel < 5) car.copy(turboLevel = car.turboLevel + 1) else car
                "tyres" -> if (car.tyresLevel < 5) car.copy(tyresLevel = car.tyresLevel + 1) else car
                "nitro" -> if (car.nitroLevel < 5) car.copy(nitroLevel = car.nitroLevel + 1) else car
                else -> car
            }
            if (upgradedCar != car) {
                gameDao.savePlayerState(player.copy(money = player.money - cost))
                gameDao.insertCar(upgradedCar)
            }
        }
    }

    suspend fun changeCarColor(carId: String, newColorHex: String) = withContext(Dispatchers.IO) {
        val car = gameDao.getCarById(carId) ?: return@withContext
        gameDao.insertCar(car.copy(colorHex = newColorHex))
    }
}
