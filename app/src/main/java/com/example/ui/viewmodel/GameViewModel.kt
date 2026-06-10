package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.CarState
import com.example.data.database.PlayerState
import com.example.data.repository.GameRepository
import com.example.utils.SoundSynth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameViewModel(
    application: Application,
    private val repository: GameRepository
) : AndroidViewModel(application) {

    val playerState: StateFlow<PlayerState?> = repository.playerState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val allCars: StateFlow<List<CarState>> = repository.allCars
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Detailing canvas points of mud
    private val _mudPoints = MutableStateFlow<List<MudSpot>>(emptyList())
    val mudPoints: StateFlow<List<MudSpot>> = _mudPoints.asStateFlow()

    // Screen effects (racing, kissing, eating)
    private val _romanceActiveKiss = MutableStateFlow<String?>(null) // Name of kissed partner
    val romanceActiveKiss: StateFlow<String?> = _romanceActiveKiss.asStateFlow()

    private val _eatingFinishedAnimation = MutableStateFlow(false)
    val eatingFinishedAnimation: StateFlow<Boolean> = _eatingFinishedAnimation.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeDatabaseIfEmpty()
            resetDetailingMud()
        }
    }

    fun setLocation(location: String) {
        val current = playerState.value ?: return
        viewModelScope.launch {
            repository.updatePlayerState(current.copy(activeLocation = location))
            SoundSynth.playCoin()
        }
    }

    fun resetDetailingMud() {
        val spots = listOf(
            MudSpot(150f, 120f, true),
            MudSpot(300f, 150f, true),
            MudSpot(450f, 130f, true),
            MudSpot(220f, 180f, true),
            MudSpot(380f, 170f, true),
            MudSpot(500f, 210f, true),
            MudSpot(100f, 220f, true),
            MudSpot(400f, 240f, true)
        )
        _mudPoints.value = spots
    }

    fun earnMoneyAndDrive(miles: Int, cashEarned: Int, damageTaken: Int) {
        val current = playerState.value ?: return
        val currentCarId = current.activeCarId
        viewModelScope.launch {
            // Update Player money, miles, and energy (-10 for exhausting race)
            val newEnergy = (current.energy - 12).coerceAtLeast(0)
            repository.updatePlayerState(
                current.copy(
                    money = current.money + cashEarned,
                    totalMileage = current.totalMileage + miles,
                    energy = newEnergy
                )
            )

            // Update car damage
            val targetCar = repository.allCars.stateIn(viewModelScope).value.find { it.id == currentCarId }
            if (targetCar != null) {
                val newDamage = (targetCar.damagePercent + damageTaken).coerceIn(0, 100)
                repository.updateCarState(targetCar.copy(damagePercent = newDamage))
            }
        }
    }

    fun changeActiveCar(carId: String) {
        val current = playerState.value ?: return
        viewModelScope.launch {
            repository.updatePlayerState(current.copy(activeCarId = carId))
            SoundSynth.playEngine(200f)
        }
    }

    fun buyCar(carId: String, price: Int) {
        viewModelScope.launch {
            repository.buyCar(carId, price)
        }
    }

    fun upgradeCar(carId: String, upgradeType: String, cost: Int) {
        viewModelScope.launch {
            repository.upgradeCar(carId, upgradeType, cost)
            SoundSynth.playCoin()
        }
    }

    fun sprayCarColor(carId: String, colorHex: String) {
        viewModelScope.launch {
            repository.changeCarColor(carId, colorHex)
            SoundSynth.playSplash()
        }
    }

    fun repairCar(carId: String, cost: Int) {
        viewModelScope.launch {
            repository.repairCar(carId, cost)
            SoundSynth.playCrash() // metallic reverse chime-like representation
        }
    }

    // Flame-Broiled Bistro eating
    fun eatFood(energyGain: Int, cost: Int) {
        val current = playerState.value ?: return
        if (current.money < cost) return

        viewModelScope.launch {
            val newMoney = current.money - cost
            val newEnergy = (current.energy + energyGain).coerceAtMost(100)
            repository.updatePlayerState(current.copy(money = newMoney, energy = newEnergy))
            SoundSynth.playChomp()
        }
    }

    // Onsen Spa bathing
    fun takeBath(temp: Float, cost: Int = 18) {
        val current = playerState.value ?: return
        if (current.money < cost) return

        // Optimal temp is 37°C - 41°C: doubles restoration!
        val isOptimal = temp in 37f..41f
        val energyGain = if (isOptimal) 45 else 20

        viewModelScope.launch {
            val newMoney = current.money - cost
            val newEnergy = (current.energy + energyGain).coerceAtMost(100)
            repository.updatePlayerState(current.copy(money = newMoney, energy = newEnergy))
            SoundSynth.playSplash()
        }
    }

    // Car Wash Detailing scrubbing logic
    fun scrubMudAt(x: Float, y: Float) {
        val thresholdRadius = 50f
        val currentSpots = _mudPoints.value
        var updated = false
        val newSpots = currentSpots.map { spot ->
            if (spot.isActive) {
                val dx = spot.x - x
                val dy = spot.y - y
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                if (dist <= thresholdRadius) {
                    updated = true
                    spot.copy(isActive = false)
                } else {
                    spot
                }
            } else {
                spot
            }
        }
        if (updated) {
            _mudPoints.value = newSpots
            SoundSynth.playSplash()

            // If ALL points are wiped away, we repair some car damage and grant showroom bonus!
            if (newSpots.none { it.isActive }) {
                triggerDetailingJobComplete()
            }
        }
    }

    private fun triggerDetailingJobComplete() {
        val current = playerState.value ?: return
        val activeCarId = current.activeCarId
        viewModelScope.launch {
            val carList = repository.allCars.stateIn(viewModelScope).value
            val targetCar = carList.find { it.id == activeCarId }
            if (targetCar != null) {
                // Fully clean repairs damage by 15%
                val newDamage = (targetCar.damagePercent - 20).coerceAtLeast(0)
                repository.updateCarState(targetCar.copy(damagePercent = newDamage))
                SoundSynth.playCoin()
            }
            // Add a small cash bonus for hard detailing work!
            repository.updatePlayerState(current.copy(money = current.money + 150))
            resetDetailingMud()
        }
    }

    // Romance Dialog Interactions
    fun romanceTalk(partner: String, responseScore: Int) {
        val current = playerState.value ?: return
        viewModelScope.launch {
            val stateCopy = when (partner) {
                "Mia" -> current.copy(miaAffinity = (current.miaAffinity + responseScore).coerceIn(0, 100))
                "Sarah" -> current.copy(sarahAffinity = (current.sarahAffinity + responseScore).coerceIn(0, 100))
                "Leo" -> current.copy(leoAffinity = (current.leoAffinity + responseScore).coerceIn(0, 100))
                else -> current
            }
            repository.updatePlayerState(stateCopy)
            SoundSynth.playCoin()
        }
    }

    fun romanceGift(partner: String, cost: Int) {
        val current = playerState.value ?: return
        if (current.money < cost) return

        viewModelScope.launch {
            val stateCopy = when (partner) {
                "Mia" -> current.copy(money = current.money - cost, miaAffinity = (current.miaAffinity + 15).coerceIn(0, 100))
                "Sarah" -> current.copy(money = current.money - cost, sarahAffinity = (current.sarahAffinity + 15).coerceIn(0, 100))
                "Leo" -> current.copy(money = current.money - cost, leoAffinity = (current.leoAffinity + 15).coerceIn(0, 100))
                else -> current
            }
            repository.updatePlayerState(stateCopy)
            SoundSynth.playCoin()
        }
    }

    fun romanceKiss(partner: String) {
        _romanceActiveKiss.value = partner
        SoundSynth.playKiss()
        // kissing costs 5 energy or releases heart warmth!
        viewModelScope.launch {
            val current = playerState.value ?: return@launch
            val energyCost = 5
            val newEnergy = (current.energy - energyCost).coerceAtLeast(0)
            repository.updatePlayerState(current.copy(energy = newEnergy))
        }
    }

    fun clearKissAnimation() {
        _romanceActiveKiss.value = null
    }

    // --- High-Fidelity Custom Playlist Management ---
    val tracks = listOf(
        Track("Not Like Us", "Kendrick Lamar", 254, "West Coast Hip-Hop", "👑", 110f, 101, "#00E5FF"),
        Track("HUMBLE.", "Kendrick Lamar", 177, "Boom Bap Trap", "🍷", 140f, 150, "#FFD700"),
        Track("Alright", "Kendrick Lamar", 219, "Jazz-Rap Anthem", "🎷", 120f, 110, "#4CAF50"),
        Track("FE!N", "Travis Scott", 191, "Rage Trap", "🔥", 160f, 150, "#FF3D00"),
        Track("SICKO MODE", "Travis Scott", 312, "Psychedelic Trap", "🎢", 130f, 155, "#FFEB3B"),
        Track("Goosebumps", "Travis Scott", 242, "Dark Ambient Beat", "🦇", 115f, 130, "#9C27B0"),
        Track("Fall Back", "Lithe", 168, "Alternative Alt-R&B", "🪐", 100f, 120, "#00B0FF"),
        Track("What You Need", "Lithe", 185, "Dark Pop Lounge", "🌙", 95f, 118, "#FF4081"),
        Track("Selfish", "Lithe", 152, "Melodic Neo-Soul", "🔮", 105f, 125, "#E040FB"),
        Track("Midnight Drift", "Runna rulez", 202, "Phonk Wave Drift", "🚗", 150f, 145, "#76FF03"),
        Track("Hyper-speed", "Runna rulez", 184, "Cyber Outlaw Beat", "🏎️", 180f, 160, "#FF5252"),
        Track("Outlaw Bass", "Runna rulez", 215, "Low-End Drift Phonk", "🔊", 135f, 138, "#00FFFF")
    )

    private val _currentTrack = MutableStateFlow<Track>(tracks[0])
    val currentTrack: StateFlow<Track> = _currentTrack.asStateFlow()

    private val _isMusicPlaying = MutableStateFlow(false)
    val isMusicPlaying: StateFlow<Boolean> = _isMusicPlaying.asStateFlow()

    private val _musicProgress = MutableStateFlow(0f)
    val musicProgress: StateFlow<Float> = _musicProgress.asStateFlow()

    private val _visualizerAmplitudes = MutableStateFlow(List(12) { 0.1f })
    val visualizerAmplitudes: StateFlow<List<Float>> = _visualizerAmplitudes.asStateFlow()

    private var musicJob: kotlinx.coroutines.Job? = null

    fun togglePlayPause() {
        _isMusicPlaying.value = !_isMusicPlaying.value
        if (_isMusicPlaying.value) {
            startMusicEngine()
        } else {
            stopMusicEngine()
        }
    }

    fun playTrack(track: Track) {
        _currentTrack.value = track
        _musicProgress.value = 0f
        _isMusicPlaying.value = true
        startMusicEngine()
        SoundSynth.playCoin()
    }

    fun nextTrack() {
        val currentIndex = tracks.indexOf(_currentTrack.value)
        val nextIndex = (currentIndex + 1) % tracks.size
        playTrack(tracks[nextIndex])
    }

    fun prevTrack() {
        val currentIndex = tracks.indexOf(_currentTrack.value)
        val prevIndex = if (currentIndex - 1 < 0) tracks.size - 1 else currentIndex - 1
        playTrack(tracks[prevIndex])
    }

    private fun startMusicEngine() {
        musicJob?.cancel()
        musicJob = viewModelScope.launch {
            var lastBeatTime = 0L
            val tickRateMs = 150L
            while (_isMusicPlaying.value) {
                kotlinx.coroutines.delay(tickRateMs)
                val track = _currentTrack.value
                val increment = tickRateMs.toFloat() / (track.durationSeconds * 1000f)
                val newProgress = _musicProgress.value + increment
                if (newProgress >= 1f) {
                    _musicProgress.value = 0f
                    val currentIndex = tracks.indexOf(track)
                    _currentTrack.value = tracks[(currentIndex + 1) % tracks.size]
                } else {
                    _musicProgress.value = newProgress
                }

                _visualizerAmplitudes.value = List(12) {
                    (0.15f + Math.random().toFloat() * 0.85f).coerceIn(0.1f, 1.0f)
                }

                val now = System.currentTimeMillis()
                val beatInterval = (60_000 / track.tempoBpm).toLong()
                if (now - lastBeatTime >= beatInterval) {
                    lastBeatTime = now
                    SoundSynth.playBeatTone(track.artist, track.baseFreq)
                }
            }
        }
    }

    private fun stopMusicEngine() {
        musicJob?.cancel()
        _visualizerAmplitudes.value = List(12) { 0.1f }
    }

    override fun onCleared() {
        super.onCleared()
        stopMusicEngine()
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
                val database = AppDatabase.getDatabase(application)
                val repository = GameRepository(database.gameDao())
                @Suppress("UNCHECKED_CAST")
                return GameViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

data class MudSpot(
    val x: Float,
    val y: Float,
    val isActive: Boolean
)

data class Track(
    val title: String,
    val artist: String,
    val durationSeconds: Int,
    val genre: String,
    val albumArtEmoji: String,
    val baseFreq: Float,
    val tempoBpm: Int,
    val hexColor: String
)
