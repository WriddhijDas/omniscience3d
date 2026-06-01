package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.api.GeminiClient
import com.example.data.ScienceDatabase
import com.example.data.ScienceNote
import com.example.data.ScienceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Screen navigation tabs
enum class ScienceTab {
    HOME,
    LAB_3D,
    VIDEO_LECTURES,
    AI_TUTOR,
    NOTES_VAULT
}

// Science/STEM and Humanist subjects
enum class Subject(val displayName: String, val iconName: String) {
    PHYSICS("Physics", "rocket"),
    CHEMISTRY("Chemistry", "science"),
    BIOLOGY("Biology", "biology"),
    COMPUTER_SCIENCE("Computer Sci", "computer"),
    MATHEMATICS("Math", "calculate"),
    HISTORY("History", "history"),
    PHILOSOPHY("Philosophy", "psychology"),
    ETHICAL_HACKING("Ethical Hacking", "terminal"),
    RELIGION("Religion", "church")
}

// Prepared Video Lecture Models
data class VideoLecture(
    val id: String,
    val title: String,
    val subject: String,
    val lengthSeconds: Int,
    val summary: String,
    val subtitles: Map<Int, String>, // Subtitle at second key
    val hologramType: String, // "black_hole", "nerve_signal", "gene_edit", "neural_net", "fractal"
    val telemetryParams: List<Pair<String, () -> String>> // list of values shown live
)

class ScienceViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        ScienceDatabase::class.java, "science_db"
    ).fallbackToDestructiveMigration().build()

    private val repository = ScienceRepository(db.scienceNoteDao())

    // UI state streams from Room persistence
    val allNotes: StateFlow<List<ScienceNote>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedNotes: StateFlow<List<ScienceNote>> = repository.bookmarkedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tab State
    private val _currentTab = MutableStateFlow(ScienceTab.HOME)
    val currentTab = _currentTab.asStateFlow()

    // Subject state
    private val _currentSubject = MutableStateFlow(Subject.PHYSICS)
    val currentSubject = _currentSubject.asStateFlow()

    // 3D Controls
    val rotationYaw = MutableStateFlow(45f)
    val rotationPitch = MutableStateFlow(30f)
    val zoomScale = MutableStateFlow(1f)
    val simSpeed = MutableStateFlow(1.0f)

    // Molecular state (Chemistry)
    val selectedMolecule = MutableStateFlow("Water") // Water, CO2, Methane, Ethanol, Graphene Grid, Sodium Chloride

    // Physics state
    val gravityScale = MutableStateFlow(1f)
    val orbitTrailEnabled = MutableStateFlow(true)
    val physicsMode = MutableStateFlow("Planetary Orbit") // Planetary Orbit, Wave-Particle Duality, Quantum Field

    // Math function choice
    val mathEquation = MutableStateFlow("Sine Ripple") // Sine Ripple, Saddle, EggCrate, Wave, Mobius Strip, Lorenz Attractor

    // Biology State
    val biologyMode = MutableStateFlow("DNA Double Helix") // DNA Double Helix, Nerve Impulse, Chloroplast Cell

    // CS State
    val computerScienceMode = MutableStateFlow("Binary Search Tree") // Binary Search Tree, Neural Net Layers, Graph Mesh

    // New Subject Selections
    val selectedHistoryEpoch = MutableStateFlow("Space Age") // Stone Age, Ancient Egypt, Roman Empire, Industrial Rev, Space Age
    val selectedPhilosophySchool = MutableStateFlow("Stoicism") // Stoicism, Existentialism, Rationalism, Nihilism
    val selectedHackingSystem = MutableStateFlow("Firewall Shield") // Firewall Shield, Brute Force, Packet Sniffer, SQL Injection
    val selectedReligionSymbol = MutableStateFlow("Dharma Wheel") // Dharma Wheel, Taoist Yin-Yang, Cross & Crescent, Aura Mandala

    // Tree values (CS)
    private val _treeNodes = MutableStateFlow(listOf(50, 30, 70, 20, 40, 60, 80))
    val treeNodes = _treeNodes.asStateFlow()

    // Interactive notes typing sheet
    val notesInput = MutableStateFlow("")

    // AI Science Tutor states
    private val _tutorResponse = MutableStateFlow("")
    val tutorResponse = _tutorResponse.asStateFlow()

    private val _isTutorLoading = MutableStateFlow(false)
    val isTutorLoading = _isTutorLoading.asStateFlow()

    val tutorQueryInput = MutableStateFlow("")

    // Simulated 3D Video lecture playback states
    private val _currentLectureId = MutableStateFlow("l1")
    val currentLectureId = _currentLectureId.asStateFlow()

    private val _lecturePlaying = MutableStateFlow(false)
    val lecturePlaying = _lecturePlaying.asStateFlow()

    private val _lectureProgressSeconds = MutableStateFlow(0)
    val lectureProgressSeconds = _lectureProgressSeconds.asStateFlow()

    // List of mock interactive videos with telemetry hooks
    val videoLectures = listOf(
        VideoLecture(
            id = "l1",
            title = "Space-Time Warping & Singularity",
            subject = "Physics",
            lengthSeconds = 60,
            summary = "Explore how mass bends the geometry of space-time and simulates Schwarzschild radius boundaries.",
            hologramType = "black_hole",
            subtitles = mapOf(
                0 to "Welcome to Space-Time Singularity Physics.",
                5 to "Einstein's general relativity states that mass-energy warps gravity.",
                12 to "Notice the light path bending as it gets near the event horizon.",
                22 to "At the singularity, spacetime curvature becomes theoretically infinite.",
                35 to "Using this rotating metric, we track particle velocity and orbital shear.",
                48 to "Try adjusting the rotation angles to see the Einstein gravitational lens."
            ),
            telemetryParams = listOf(
                "Event Horizon" to { "R_g = 2.95 km" },
                "Spin Parameter" to { "a = 0.985 c" },
                "Gravity Shear" to { "T_{uv} ≈ 3.42 e12 s⁻²" }
            )
        ),
        VideoLecture(
            id = "l2",
            title = "Valence Shells and Carbon Orbitals",
            subject = "Chemistry",
            lengthSeconds = 60,
            summary = "Dive deep into modern covalent bonding, orbital hybridization (sp3), and carbon shapes.",
            hologramType = "molecules",
            subtitles = mapOf(
                0 to "In this video, we investigate Carbon carbon sp3 hybridization.",
                6 to "Four valence electrons distribute into distinct tetrahedral symmetry orbitals.",
                15 to "Each bond angle measures exactly 109.5 degrees.",
                25 to "This molecular alignment creates methane, making it very stable.",
                38 to "By shifting chemical bonds, we produce long hydrocarbon chains.",
                50 to "Rotate the interactive sticking 3D lattice in real time to explore poles."
            ),
            telemetryParams = listOf(
                "Hybridization" to { "sp³ Orbitals" },
                "Bond Angle" to { "109.5° Geometry" },
                "Valence e-" to { "4 Carbon Shells" }
            )
        ),
        VideoLecture(
            id = "l3",
            title = "Transcription Cycles and Base Pairing",
            subject = "Biology",
            lengthSeconds = 60,
            summary = "Visualize translation polymerases sliding alongside the double helix to transcript mRNA strings.",
            hologramType = "nerve_signal",
            subtitles = mapOf(
                0 to "Transcription represents the core flow of protein synthesis.",
                7 to "The RNA Polymerase slides to read the molecular double-stranded scale.",
                16 to "Adenine binds with Uracil instead of Thymine during RNA creation.",
                27 to "Cytosine with Guanine maintains triple hydrogen chemical bonding.",
                40 to "See the mRNA strand release to pass outer boundaries to ribosomes.",
                51 to "Observe the dynamic nucleotide pairings on the right."
            ),
            telemetryParams = listOf(
                "Helix Spin" to { "θ = 36° / Base" },
                "Hydrogen Bonds" to { "A=T (2), C≡G (3)" },
                "Reading Rate" to { "≈ 45 nt / s" }
            )
        ),
        VideoLecture(
            id = "l4",
            title = "Vector Fields & Multivariable Calculus",
            subject = "Mathematics",
            lengthSeconds = 60,
            summary = "An immersive mathematical look into Fourier wave frequencies, double integrals and complex surfaces.",
            hologramType = "fractal",
            subtitles = mapOf(
                0 to "Let's explore multi-variable gradients and curl systems.",
                6 to "Calculating derivatives yields surface slopes at custom vector coordinates.",
                14 to "Here we map the Saddle functional surface: z = x² - y².",
                24 to "By sweeping the vector grid, we observe diverging math flow lines.",
                36 to "Critical inflection bounds create this saddle-contour geometry.",
                50 to "Modify parameters to alter coordinate amplitude slopes in real-time."
            ),
            telemetryParams = listOf(
                "Gradient ∇z" to { "[2x, -2y]" },
                "Inflection" to { "Saddle Point (0,0)" },
                "Mesh Density" to { "25 x 25 Quadrants" }
            )
        )
    )

    fun incrementLectureProgress() {
        val currentProgress = _lectureProgressSeconds.value
        if (currentProgress < 60) {
            _lectureProgressSeconds.value = currentProgress + 1
        } else {
            _lectureProgressSeconds.value = 0
        }
    }

    init {
        // Initialization without hanging background coroutines
    }

    fun selectTab(tab: ScienceTab) {
        _currentTab.value = tab
    }

    fun selectSubject(subject: Subject) {
        _currentSubject.value = subject
        // Reset rotational defaults per subject focus
        rotationYaw.value = 45f
        rotationPitch.value = 30f
        simSpeed.value = 1.0f
    }

    // Insert custom tree node (CS Lab)
    fun addTreeNode(value: Int) {
        val current = _treeNodes.value.toMutableList()
        if (!current.contains(value)) {
            current.add(value)
            _treeNodes.value = current
        }
    }

    fun resetTree() {
        _treeNodes.value = listOf(50, 30, 70, 20, 40, 60, 80)
    }

    // Video Selection
    fun selectLecture(lectureId: String) {
        _currentLectureId.value = lectureId
        _lectureProgressSeconds.value = 0
        _lecturePlaying.value = false
    }

    fun toggleLecturePlay() {
        _lecturePlaying.value = !_lecturePlaying.value
    }

    fun seekLecture(seconds: Int) {
        _lectureProgressSeconds.value = seconds.coerceIn(0, 60)
    }

    fun getActiveSubtitle(): String {
        val seconds = _lectureProgressSeconds.value
        val subtitleKeys = videoLectures.firstOrNull { it.id == _currentLectureId.value }?.subtitles ?: emptyMap()
        val matchKey = subtitleKeys.keys.filter { it <= seconds }.maxOrNull() ?: 0
        return subtitleKeys[matchKey] ?: ""
    }

    // Save notes with user description to SQLite DB
    fun saveScienceNote(subjectText: String, keyText: String) {
        val text = notesInput.value.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            val note = ScienceNote(
                subject = subjectText,
                modelKey = keyText,
                content = text
            )
            repository.saveNote(note)
            notesInput.value = "" // clear input
        }
    }

    fun toggleBookmarkState(id: Long, currentVal: Boolean) {
        viewModelScope.launch {
            repository.toggleBookmark(id, !currentVal)
        }
    }

    fun deleteNote(note: ScienceNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    // Gemini API Assistant tutor call
    fun askTutor(prebuiltPrompt: String? = null) {
        val query = prebuiltPrompt ?: tutorQueryInput.value.trim()
        if (query.isEmpty()) return

        _isTutorLoading.value = true
        _tutorResponse.value = "Thinking deeply..."

        if (prebuiltPrompt == null) {
            tutorQueryInput.value = ""
        }

        viewModelScope.launch {
            val systemInstructions = """
                You are a premium AI Science & Mathematics Tutor inside "ScienceLab 3D" application. 
                Keep your explanations highly educational, rigorous yet easy to understand, and visually descriptive.
                Always write output formatted with Markdown. Since we focus on 3D, describe the geometric space, 
                vector dimensions, and the beautiful spatial curves of molecules, orbits, organelles, or structures.
                Break down physical equations into clear steps. Use clear headings and bullets for readability.
            """.trimIndent()

            val response = GeminiClient.askGemini(query, systemPrompt = systemInstructions)
            _tutorResponse.value = response
            _isTutorLoading.value = false
        }
    }

    fun clearTutor() {
        _tutorResponse.value = ""
        tutorQueryInput.value = ""
    }
}
