package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ScienceNote
import com.example.ui.theme.*
import kotlin.math.*

// --- 3D Trigonometry & Projection Helper Classes ---
data class Vector3D(val x: Float, val y: Float, val z: Float) {
    fun rotate(yawDegrees: Float, pitchDegrees: Float): Vector3D {
        val yawRad = Math.toRadians(yawDegrees.toDouble()).toFloat()
        val pitchRad = Math.toRadians(pitchDegrees.toDouble()).toFloat()

        // 1. Rotation around Y-axis (Yaw)
        val cosY = cos(yawRad)
        val sinY = sin(yawRad)
        val rX = x * cosY - z * sinY
        val rZ = x * sinY + z * cosY

        // 2. Rotation around X-axis (Pitch)
        val cosP = cos(pitchRad)
        val sinP = sin(pitchRad)
        val rY = y * cosP - rZ * sinP
        val finalZ = y * sinP + rZ * cosP

        return Vector3D(rX, rY, finalZ)
    }

    fun project(centerX: Float, centerY: Float, zoom: Float): Point2D {
        val viewDistance = 350f
        val denom = viewDistance + z
        val perspective = if (denom <= 10f) {
            viewDistance / 10f
        } else {
            viewDistance / denom
        }
        val screenX = centerX + x * perspective * zoom
        val screenY = centerY + y * perspective * zoom
        return Point2D(screenX, screenY, perspective)
    }
}

fun Color.safeCopy(alpha: Float): Color {
    val safeAlpha = if (alpha.isNaN() || alpha.isInfinite()) 0.5f else alpha.coerceIn(0f, 1f)
    return this.copy(alpha = safeAlpha)
}

data class Point2D(val x: Float, val y: Float, val scale: Float)

data class Atom3D(
    val element: String,
    val pos: Vector3D,
    val color: Color,
    val r: Float
)

// Main ScienceLab UI
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScienceAppUI(viewModel: ScienceViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val currentSubject by viewModel.currentSubject.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "QUANTUM PLATFORM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.5.sp,
                                color = QuantumCyan
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Science,
                                contentDescription = "Atom Logo",
                                tint = QuantumCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "OmniScience 3D",
                                color = PureWhite,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 20.sp
                                ),
                                letterSpacing = (-0.5).sp
                            )
                        }
                    }
                },
                actions = {
                    Column(
                        modifier = Modifier.padding(end = 16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Learning Engine v1.4",
                            style = MaterialTheme.typography.bodySmall,
                            color = QuantumCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "UTC Live Sandbox",
                            style = MaterialTheme.typography.labelSmall,
                            color = CosmicSlate
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightBlack,
                    titleContentColor = PureWhite
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SolarCoal,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .testTag("main_navigation_bar")
                    .drawBehind {
                        // Drawing a super clean top border: border-t border-white/5 as in HTML style
                        drawLine(
                            color = Color.White.copy(alpha = 0.05f),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
            ) {
                NavigationBarItem(
                    selected = currentTab == ScienceTab.HOME,
                    onClick = { viewModel.selectTab(ScienceTab.HOME) },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = QuantumCyan,
                        selectedTextColor = QuantumCyan,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = CosmicSlate.copy(alpha = 0.4f),
                        unselectedTextColor = CosmicSlate.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("nav_home")
                )
                NavigationBarItem(
                    selected = currentTab == ScienceTab.LAB_3D,
                    onClick = { viewModel.selectTab(ScienceTab.LAB_3D) },
                    icon = { Icon(Icons.Filled.Layers, contentDescription = "3D Lab") },
                    label = { Text("3D Labs") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = QuantumCyan,
                        selectedTextColor = QuantumCyan,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = CosmicSlate.copy(alpha = 0.4f),
                        unselectedTextColor = CosmicSlate.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("nav_lab_3d")
                )
                NavigationBarItem(
                    selected = currentTab == ScienceTab.VIDEO_LECTURES,
                    onClick = { viewModel.selectTab(ScienceTab.VIDEO_LECTURES) },
                    icon = { Icon(Icons.Filled.PlayCircle, contentDescription = "Video Lectures") },
                    label = { Text("3D Videos") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = QuantumCyan,
                        selectedTextColor = QuantumCyan,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = CosmicSlate.copy(alpha = 0.4f),
                        unselectedTextColor = CosmicSlate.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("nav_videos")
                )
                NavigationBarItem(
                    selected = currentTab == ScienceTab.AI_TUTOR,
                    onClick = { viewModel.selectTab(ScienceTab.AI_TUTOR) },
                    icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Assistant") },
                    label = { Text("AI Assist") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = QuantumCyan,
                        selectedTextColor = QuantumCyan,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = CosmicSlate.copy(alpha = 0.4f),
                        unselectedTextColor = CosmicSlate.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("nav_ai_tutor")
                )
                NavigationBarItem(
                    selected = currentTab == ScienceTab.NOTES_VAULT,
                    onClick = { viewModel.selectTab(ScienceTab.NOTES_VAULT) },
                    icon = { Icon(Icons.Filled.Bookmark, contentDescription = "Notes Vault") },
                    label = { Text("Vault") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = QuantumCyan,
                        selectedTextColor = QuantumCyan,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = CosmicSlate.copy(alpha = 0.4f),
                        unselectedTextColor = CosmicSlate.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("nav_notes_vault")
                )
            }
        },
        containerColor = MidnightBlack
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                ScienceTab.HOME -> ScreenHome(viewModel)
                ScienceTab.LAB_3D -> ScreenLab3D(viewModel)
                ScienceTab.VIDEO_LECTURES -> ScreenVideoLectures(viewModel)
                ScienceTab.AI_TUTOR -> ScreenAITutor(viewModel)
                ScienceTab.NOTES_VAULT -> ScreenNotesVault(viewModel)
            }
        }
    }
}

@Composable
fun ScreenHome(viewModel: ScienceViewModel) {
    val subjects = Subject.values()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "Omniscience 3D Hub",
                    color = PureWhite,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.testTag("home_title")
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Welcome to the unified immersive learning matrix. Select any topic below to query the 3D projection, run math surface parameters, or interact with the AI assistant.",
                    color = CosmicSlate,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        item {
            Text(
                text = "EXPLORE DISCIPLINARY DOMAINS",
                color = QuantumCyan,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        // Render 9 subjects in custom styled cards
        items(subjects.size) { index ->
            val subject = subjects[index]
            val icon = when (subject) {
                Subject.PHYSICS -> Icons.Filled.RocketLaunch
                Subject.CHEMISTRY -> Icons.Filled.Science
                Subject.BIOLOGY -> Icons.Filled.Coronavirus
                Subject.COMPUTER_SCIENCE -> Icons.Filled.Computer
                Subject.MATHEMATICS -> Icons.Filled.Calculate
                Subject.HISTORY -> Icons.Filled.History
                Subject.PHILOSOPHY -> Icons.Filled.Psychology
                Subject.ETHICAL_HACKING -> Icons.Filled.Terminal
                Subject.RELIGION -> Icons.Filled.Church
            }
            val accentColor = when (subject) {
                Subject.PHYSICS -> QuantumCyan
                Subject.CHEMISTRY -> OrangeGlow
                Subject.BIOLOGY -> BioGreen
                Subject.COMPUTER_SCIENCE -> LaserBlue
                Subject.MATHEMATICS -> MathPurple
                Subject.HISTORY -> Color(0xFFD4AF37) // Golden Bronze
                Subject.PHILOSOPHY -> Color(0xFF9E7BFF) // Lavender
                Subject.ETHICAL_HACKING -> Color(0xFFFF4D4D) // Red
                Subject.RELIGION -> Color(0xFFE0B0FF) // Violet
            }
            val desc = when (subject) {
                Subject.PHYSICS -> "Planetary orbit simulations, wave kinematics, quantum state fluctuations."
                Subject.CHEMISTRY -> "Atoms bonding configurations, covalent grids, dynamic lattice representations."
                Subject.BIOLOGY -> "DNA helical sequence transcription, neural wave propagation impulses."
                Subject.COMPUTER_SCIENCE -> "Binary Search Tree iterations, multi-layer neural flow layouts."
                Subject.MATHEMATICS -> "Calculus wireframes plotting, surface equations, attractor graphs coordinates."
                Subject.HISTORY -> "Interactive timelines of key epochs from the Stone Age to Space Exploration."
                Subject.PHILOSOPHY -> "Conceptual thought flows, connection nodes mapping major existential schools."
                Subject.ETHICAL_HACKING -> "Interactive firewall defensive matrices and penetration simulation vectors."
                Subject.RELIGION -> "Symbolic geometric matrices modeling major world theological principles."
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NebulaDark, RoundedCornerShape(12.dp))
                    .clickable {
                        viewModel.selectSubject(subject)
                        viewModel.selectTab(ScienceTab.LAB_3D)
                    }
                    .testTag("subject_card_${subject.name.lowercase()}"),
                colors = CardDefaults.cardColors(containerColor = SolarCoal)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = subject.displayName,
                            tint = accentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = subject.displayName,
                            color = PureWhite,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = desc,
                            color = CosmicSlate,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = "Launch",
                        tint = CosmicSlate.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            // Quick status card or summary
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QuantumCyan.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = NebulaDark)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Status Info",
                        tint = QuantumCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Tip: Log observations across different modes to automatically save them to your encrypted Vault or consult with the real-time AI Assist chatbot.",
                        color = PureWhite,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// --- 1. 3D LABS SCREEN ---
@Composable
fun ScreenLab3D(viewModel: ScienceViewModel) {
    val currentSubject by viewModel.currentSubject.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // Horizontal Subject Selector Tab
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(SolarCoal)
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(Subject.values()) { subject ->
                val isSelected = currentSubject == subject
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectSubject(subject) },
                    label = { Text(subject.displayName, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = QuantumCyan,
                        selectedLabelColor = MidnightBlack,
                        containerColor = NebulaDark,
                        labelColor = CosmicSlate
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = when (subject) {
                                Subject.PHYSICS -> Icons.Filled.RocketLaunch
                                Subject.CHEMISTRY -> Icons.Filled.Science
                                Subject.BIOLOGY -> Icons.Filled.Coronavirus
                                Subject.COMPUTER_SCIENCE -> Icons.Filled.Computer
                                Subject.MATHEMATICS -> Icons.Filled.Calculate
                                Subject.HISTORY -> Icons.Filled.History
                                Subject.PHILOSOPHY -> Icons.Filled.Psychology
                                Subject.ETHICAL_HACKING -> Icons.Filled.Terminal
                                Subject.RELIGION -> Icons.Filled.Church
                            },
                            contentDescription = subject.displayName,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) MidnightBlack else QuantumCyan
                        )
                    },
                    modifier = Modifier.testTag("subject_chip_${subject.name.lowercase()}")
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Interactive 3D Canvas Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.1f)
                        .background(MidnightBlack)
                        .border(1.dp, NebulaDark)
                ) {
                    // Touch controls and 3D Visualisations
                    val yaw by viewModel.rotationYaw.collectAsStateWithLifecycle()
                    val pitch by viewModel.rotationPitch.collectAsStateWithLifecycle()
                    val zoom by viewModel.zoomScale.collectAsStateWithLifecycle()

                    // Simulated 3D Render Canvas based on Subject
                    Canvas3DRenderer(
                        subject = currentSubject,
                        viewModel = viewModel,
                        yaw = yaw,
                        pitch = pitch,
                        zoom = zoom,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    viewModel.rotationYaw.value = (viewModel.rotationYaw.value + dragAmount.x * 0.4f) % 360f
                                    viewModel.rotationPitch.value =
                                        (viewModel.rotationPitch.value - dragAmount.y * 0.4f).coerceIn(-85f, 85f)
                                }
                            }
                            .testTag("interactive_3d_canvas")
                    )

                    // Overlay HUD Indicators
                    Column(
                        modifier = Modifier
                            .padding(14.dp)
                            .align(Alignment.TopStart)
                            .background(SolarCoal.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                            .border(1.dp, QuantumCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "CAMERA TELEMETRY",
                            color = QuantumCyan,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Yaw: ${yaw.toInt()}° | Pitch: ${pitch.toInt()}°",
                            color = PureWhite,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Projection: Ortho+Perspective",
                            color = CosmicSlate,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    // Interactive Help Icon Overlay (Top-Right)
                    var showHelpDialog by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { showHelpDialog = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Controls Help",
                            tint = QuantumCyan
                        )
                    }

                    if (showHelpDialog) {
                        Dialog(onDismissRequest = { showHelpDialog = false }) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SolarCoal,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "3D Navigation Guide",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = QuantumCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "• Drag across the canvas to rotate models along Yaw/Pitch axes.\n" +
                                                "• Use parameters sidebar to dynamically update mathematical equations.\n" +
                                                "• Use the quick action buttons to bookmark coordinates and structures.",
                                        color = PureWhite,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { showHelpDialog = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = QuantumCyan)
                                    ) {
                                        Text("Acknowledge", color = MidnightBlack)
                                    }
                                }
                            }
                        }
                    }
                }

                // Lab Parameters & Notebook Panel (Bottom half)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.9f)
                        .background(SolarCoal)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .padding(16.dp)
                ) {
                    ScienceLabControlPanel(subject = currentSubject, viewModel = viewModel)
                }
            }
        }
    }
}

// 3D Canvas Projection & Shader Logic
@Composable
fun Canvas3DRenderer(
    subject: Subject,
    viewModel: ScienceViewModel,
    yaw: Float,
    pitch: Float,
    zoom: Float,
    modifier: Modifier
) {
    // Generate animated timer ticking for continuous rotational orbits
    val transition = rememberInfiniteTransition(label = "SimulationCycle")
    val animTick by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitTick"
    )

    // Collect slider parameters
    val speed by viewModel.simSpeed.collectAsStateWithLifecycle()
    val rawMolecules by viewModel.selectedMolecule.collectAsStateWithLifecycle()
    val gravityFactor by viewModel.gravityScale.collectAsStateWithLifecycle()
    val equationName by viewModel.mathEquation.collectAsStateWithLifecycle()
    val treeNodes by viewModel.treeNodes.collectAsStateWithLifecycle()

    val physicsMode by viewModel.physicsMode.collectAsStateWithLifecycle()
    val biologyMode by viewModel.biologyMode.collectAsStateWithLifecycle()
    val computerScienceMode by viewModel.computerScienceMode.collectAsStateWithLifecycle()

    val selectedHistoryEpoch by viewModel.selectedHistoryEpoch.collectAsStateWithLifecycle()
    val selectedPhilosophySchool by viewModel.selectedPhilosophySchool.collectAsStateWithLifecycle()
    val selectedHackingSystem by viewModel.selectedHackingSystem.collectAsStateWithLifecycle()
    val selectedReligionSymbol by viewModel.selectedReligionSymbol.collectAsStateWithLifecycle()

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val scaleDimension = min(size.width, size.height) * 0.0022f * zoom

        // 1. Draw subtle background coordinate reference space grid
        drawBackgroundGrid(centerX, centerY)

        when (subject) {
            Subject.PHYSICS -> {
                val t = animTick * speed
                when (physicsMode) {
                    "Planetary Orbit" -> drawPlanetarySystem(centerX, centerY, t, scaleDimension, yaw, pitch, gravityFactor)
                    "Wave-Particle Duality" -> drawQuantumWaveParticle(centerX, centerY, scaleDimension, yaw, pitch, t)
                    "Quantum Field" -> drawQuantumField(centerX, centerY, scaleDimension, yaw, pitch, t)
                    "Thermodynamics Entropy" -> drawThermodynamicsEntropy(centerX, centerY, scaleDimension, yaw, pitch, t)
                    "Electromagnetic Induction" -> drawElectromagneticInduction(centerX, centerY, scaleDimension, yaw, pitch, t)
                    else -> drawPlanetarySystem(centerX, centerY, t, scaleDimension, yaw, pitch, gravityFactor)
                }
            }
            Subject.CHEMISTRY -> {
                // Ball and Stick Molecular Model Drawer (with Z-sorting depth shading)
                drawMolecules(centerX, centerY, scaleDimension, yaw, pitch, rawMolecules)
            }
            Subject.BIOLOGY -> {
                val t = animTick * speed * 0.2f
                when (biologyMode) {
                    "DNA Double Helix" -> drawDNAHelix(centerX, centerY, scaleDimension, yaw, pitch, t)
                    "Nerve Impulse" -> drawNerveImpulseSignal(centerX, centerY, scaleDimension, yaw, pitch, t)
                    "Chloroplast Cell" -> drawChloroplastCell(centerX, centerY, scaleDimension, yaw, pitch, t)
                    "Mitochondria Membrane" -> drawMitochondriaMembrane(centerX, centerY, scaleDimension, yaw, pitch, t)
                    "Mitosis Splitting" -> drawMitosisSplitting(centerX, centerY, scaleDimension, yaw, pitch, t)
                    else -> drawDNAHelix(centerX, centerY, scaleDimension, yaw, pitch, t)
                }
            }
            Subject.COMPUTER_SCIENCE -> {
                when (computerScienceMode) {
                    "Binary Search Tree" -> drawComputerScienceBST(centerX, centerY, scaleDimension, yaw, pitch, treeNodes)
                    "Neural Net Layers" -> drawNeuralNetLayers(centerX, centerY, scaleDimension, yaw, pitch, animTick * speed)
                    "Graph Mesh" -> drawGraphMesh(centerX, centerY, scaleDimension, yaw, pitch, animTick * speed)
                    "Blockchain Consensus" -> drawBlockchainConsensus(centerX, centerY, scaleDimension, yaw, pitch, animTick * speed)
                    "Bubble Sort Track" -> drawBubbleSort(centerX, centerY, scaleDimension, yaw, pitch, animTick * speed)
                    else -> drawComputerScienceBST(centerX, centerY, scaleDimension, yaw, pitch, treeNodes)
                }
            }
            Subject.MATHEMATICS -> {
                val t = animTick * speed * 3f
                when (equationName) {
                    "Mobius Strip" -> drawMobiusStrip(centerX, centerY, scaleDimension, yaw, pitch, t)
                    "Lorenz Attractor" -> drawLorenzAttractor(centerX, centerY, scaleDimension, yaw, pitch, t)
                    "Torus Ring" -> drawTorusRing(centerX, centerY, scaleDimension, yaw, pitch, t)
                    "Mandelbrot Fractal" -> drawMandelbrotFractal(centerX, centerY, scaleDimension, yaw, pitch, t)
                    else -> drawMathSurfaceGrapher(centerX, centerY, scaleDimension, yaw, pitch, equationName, t)
                }
            }
            Subject.HISTORY -> {
                val t = animTick * speed
                drawHistoricalTimescales(centerX, centerY, scaleDimension, yaw, pitch, t, selectedHistoryEpoch)
            }
            Subject.PHILOSOPHY -> {
                val t = animTick * speed
                drawPhilosophicalIdeaGraphs(centerX, centerY, scaleDimension, yaw, pitch, t, selectedPhilosophySchool)
            }
            Subject.ETHICAL_HACKING -> {
                val t = animTick * speed
                drawCyberSecurityThreatSandbox(centerX, centerY, scaleDimension, yaw, pitch, t, selectedHackingSystem)
            }
            Subject.RELIGION -> {
                val t = animTick * speed
                drawTheologicalAuraCompass(centerX, centerY, scaleDimension, yaw, pitch, t, selectedReligionSymbol)
            }
        }
    }
}

// Draw dark outer space grid lines
private fun DrawScope.drawBackgroundGrid(centerX: Float, centerY: Float) {
    val gridCount = 8
    val radiusStep = 35f
    // Drawing orbital concentric circles for perspective space
    for (i in 1..gridCount) {
        drawCircle(
            color = ParticleGrid.copy(alpha = 0.08f * (gridCount - i) / gridCount),
            radius = i * radiusStep,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1f)
        )
    }

    // Centered axis indicators
    drawLine(
        color = ParticleGrid.copy(alpha = 0.15f),
        start = Offset(centerX - 100f, centerY),
        end = Offset(centerX + 100f, centerY),
        strokeWidth = 1f
    )
    drawLine(
        color = ParticleGrid.copy(alpha = 0.15f),
        start = Offset(centerX, centerY - 100f),
        end = Offset(centerX, centerY + 100f),
        strokeWidth = 1f
    )
}

// PHYSICS RENDER: Orbits with gravity warping
private fun DrawScope.drawPlanetarySystem(
    centerX: Float, centerY: Float,
    time: Float, scale: Float,
    yaw: Float, pitch: Float,
    gravity: Float
) {
    val radialFactor = 0.85f / (gravity.coerceAtLeast(0.1f)) // Warps orbit bounds

    // Central Sun Position (fixed at origin)
    val sunVec = Vector3D(0f, 0f, 0f).rotate(yaw, pitch)
    val sunProj = sunVec.project(centerX, centerY, scale)

    // Earth vector calculations
    val earthRad = 75f * radialFactor
    val earthAngle = Math.toRadians(time.toDouble()).toFloat()
    val earthVecRaw = Vector3D(earthRad * cos(earthAngle), 0f, earthRad * sin(earthAngle))
    val earthVec = earthVecRaw.rotate(yaw, pitch)
    val earthProj = earthVec.project(centerX, centerY, scale)

    // Moon vector calculations (orbits Earth)
    val moonRad = 18f
    val moonAngle = Math.toRadians(time.toDouble() * 11.0).toFloat()
    val moonVecRaw = Vector3D(
        earthVecRaw.x + moonRad * cos(moonAngle),
        moonRad * sin(moonAngle) * 0.4f,
        earthVecRaw.z + moonRad * sin(moonAngle)
    )
    val moonVec = moonVecRaw.rotate(yaw, pitch)
    val moonProj = moonVec.project(centerX, centerY, scale)

    // Mars vector calculations
    val marsRad = 125f * radialFactor
    val marsAngle = Math.toRadians(time.toDouble() * 0.55).toFloat()
    val marsVecRaw = Vector3D(marsRad * cos(marsAngle), 0f, marsRad * sin(marsAngle))
    val marsVec = marsVecRaw.rotate(yaw, pitch)
    val marsProj = marsVec.project(centerX, centerY, scale)

    // 1. Draw central gravity warp contour lines
    drawCircle(
        color = QuantumCyan.copy(alpha = 0.08f),
        radius = earthRad * scale * earthProj.scale,
        center = Offset(centerX, centerY),
        style = Stroke(width = 2f)
    )
    drawCircle(
        color = LaserBlue.copy(alpha = 0.06f),
        radius = marsRad * scale * marsProj.scale,
        center = Offset(centerX, centerY),
        style = Stroke(width = 1.5f)
    )

    // 2. Draw Planetary vectors & gravity connection cables
    drawLine(
        color = QuantumCyan.copy(alpha = 0.3f),
        start = Offset(sunProj.x, sunProj.y),
        end = Offset(earthProj.x, earthProj.y),
        strokeWidth = 1f
    )
    drawLine(
        color = StellarCoral.copy(alpha = 0.25f),
        start = Offset(sunProj.x, sunProj.y),
        end = Offset(marsProj.x, marsProj.y),
        strokeWidth = 1f
    )

    // 3. Draw Kepler objects (Sun, Earth, Moon, Mars)
    // Central Sun
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(ChemAmber, StellarCoral, Color.Transparent),
            center = Offset(sunProj.x, sunProj.y),
            radius = 32f * scale * sunProj.scale
        ),
        radius = 32f * scale * sunProj.scale,
        center = Offset(sunProj.x, sunProj.y)
    )
    drawCircle(
        color = PureWhite,
        radius = 12f * scale * sunProj.scale,
        center = Offset(sunProj.x, sunProj.y)
    )

    // Draw Earth
    drawCircle(
        color = LaserBlue,
        radius = 8f * scale * earthProj.scale,
        center = Offset(earthProj.x, earthProj.y)
    )
    drawCircle(
        color = QuantumCyan.copy(alpha = 0.6f),
        radius = 11f * scale * earthProj.scale,
        center = Offset(earthProj.x, earthProj.y),
        style = Stroke(width = 1.5f)
    )

    // Draw Moon
    drawCircle(
        color = CosmicSlate,
        radius = 3f * scale * moonProj.scale,
        center = Offset(moonProj.x, moonProj.y)
    )

    // Draw Mars
    drawCircle(
        color = StellarCoral,
        radius = 6f * scale * marsProj.scale,
        center = Offset(marsProj.x, marsProj.y)
    )
}

// CHEMISTRY RENDER: molecules ball/stick with depth-Z sorting
private fun DrawScope.drawMolecules(
    centerX: Float, centerY: Float,
    scale: Float, yaw: Float, pitch: Float,
    moleculeName: String
) {
    // Generate molecule atomic parameters
    val atomsRaw = when (moleculeName) {
        "Water" -> listOf(
            Atom3D("O", Vector3D(0f, 15f, 0f), StellarCoral, 22f), // Oxygen center
            Atom3D("H", Vector3D(-55f, -40f, 0f), QuantumCyan, 14f), // Hydrogen 1
            Atom3D("H", Vector3D(55f, -40f, 0f), QuantumCyan, 14f) // Hydrogen 2
        )
        "Carbon Dioxide" -> listOf(
            Atom3D("C", Vector3D(0f, 0f, 0f), SolarCoal, 20f), // Carbon Center
            Atom3D("O", Vector3D(-75f, 0f, 0f), StellarCoral, 24f), // Double Bond O-1
            Atom3D("O", Vector3D(75f, 0f, 0f), StellarCoral, 24f) // Double Bond O-2
        )
        "Methane" -> listOf(
            Atom3D("C", Vector3D(0f, 0f, 0f), LaserBlue, 22f),
            Atom3D("H", Vector3D(0f, 70f, 0f), QuantumCyan, 14f),
            Atom3D("H", Vector3D(65f, -23f, -23f), QuantumCyan, 14f),
            Atom3D("H", Vector3D(-33f, -23f, 58f), QuantumCyan, 14f),
            Atom3D("H", Vector3D(-33f, -23f, -58f), QuantumCyan, 14f)
        )
        "Ethanol" -> listOf(
            Atom3D("C", Vector3D(-45f, 0f, 0f), LaserBlue, 20f), // Methyl C
            Atom3D("C", Vector3D(25f, -15f, 0f), LaserBlue, 20f), // Ch2 C
            Atom3D("O", Vector3D(75f, 35f, 0f), StellarCoral, 22f), // OH Oxygen
            Atom3D("H", Vector3D(115f, 15f, 0f), QuantumCyan, 13f), // OH Hydrogen
            Atom3D("H", Vector3D(-45f, -60f, 0f), QuantumCyan, 13f),
            Atom3D("H", Vector3D(-85f, 30f, 40f), QuantumCyan, 13f),
            Atom3D("H", Vector3D(-85f, 30f, -40f), QuantumCyan, 13f),
            Atom3D("H", Vector3D(25f, -75f, 35f), QuantumCyan, 13f),
            Atom3D("H", Vector3D(25f, -75f, -35f), QuantumCyan, 13f)
        )
        "Sodium Chloride" -> listOf(
            Atom3D("Cl", Vector3D(-55f, -55f, -55f), LaserBlue, 20f),
            Atom3D("Na", Vector3D(55f, -55f, -55f), BioGreen, 14f),
            Atom3D("Cl", Vector3D(55f, 55f, -55f), LaserBlue, 20f),
            Atom3D("Na", Vector3D(-55f, 55f, -55f), BioGreen, 14f),
            Atom3D("Na", Vector3D(-55f, -55f, 55f), BioGreen, 14f),
            Atom3D("Cl", Vector3D(55f, -55f, 55f), LaserBlue, 20f),
            Atom3D("Na", Vector3D(55f, 55f, 55f), BioGreen, 14f),
            Atom3D("Cl", Vector3D(-55f, 55f, 55f), LaserBlue, 20f)
        )
        "Graphene Grid" -> listOf(
            Atom3D("C", Vector3D(0f, -60f, 0f), PureWhite, 18f),
            Atom3D("C", Vector3D(52f, -30f, 0f), PureWhite, 18f),
            Atom3D("C", Vector3D(52f, 30f, 0f), PureWhite, 18f),
            Atom3D("C", Vector3D(0f, 60f, 0f), PureWhite, 18f),
            Atom3D("C", Vector3D(-52f, 30f, 0f), PureWhite, 18f),
            Atom3D("C", Vector3D(-52f, -30f, 0f), PureWhite, 18f)
        )
        "Benzene Ring" -> listOf(
            Atom3D("C", Vector3D(50f, 0f, 0f), LaserBlue, 20f),
            Atom3D("C", Vector3D(25f, 43.3f, 0f), LaserBlue, 20f),
            Atom3D("C", Vector3D(-25f, 43.3f, 0f), LaserBlue, 20f),
            Atom3D("C", Vector3D(-50f, 0f, 0f), LaserBlue, 20f),
            Atom3D("C", Vector3D(-25f, -43.3f, 0f), LaserBlue, 20f),
            Atom3D("C", Vector3D(25f, -43.3f, 0f), LaserBlue, 20f),
            Atom3D("H", Vector3D(85f, 0f, 0f), QuantumCyan, 13f),
            Atom3D("H", Vector3D(42.5f, 73.6f, 0f), QuantumCyan, 13f),
            Atom3D("H", Vector3D(-42.5f, 73.6f, 0f), QuantumCyan, 13f),
            Atom3D("H", Vector3D(-85f, 0f, 0f), QuantumCyan, 13f),
            Atom3D("H", Vector3D(-42.5f, -73.6f, 0f), QuantumCyan, 13f),
            Atom3D("H", Vector3D(42.5f, -73.6f, 0f), QuantumCyan, 13f)
        )
        "Aspirin Molecule" -> listOf(
            Atom3D("C", Vector3D(0f, -40f, 0f), LaserBlue, 20f),
            Atom3D("C", Vector3D(35f, -20f, 0f), LaserBlue, 20f),
            Atom3D("C", Vector3D(35f, 20f, 0f), LaserBlue, 20f),
            Atom3D("C", Vector3D(0f, 40f, 0f), LaserBlue, 20f),
            Atom3D("C", Vector3D(-35f, 20f, 0f), LaserBlue, 20f),
            Atom3D("C", Vector3D(-35f, -20f, 0f), LaserBlue, 20f),
            Atom3D("H", Vector3D(65f, -35f, 0f), QuantumCyan, 13f),
            Atom3D("H", Vector3D(65f, 35f, 0f), QuantumCyan, 13f),
            Atom3D("H", Vector3D(0f, 70f, 0f), QuantumCyan, 13f),
            Atom3D("H", Vector3D(-65f, 35f, 0f), QuantumCyan, 13f),
            Atom3D("C", Vector3D(-70f, -60f, 0f), LaserBlue, 20f),
            Atom3D("O", Vector3D(-110f, -50f, 0f), StellarCoral, 22f),
            Atom3D("O", Vector3D(-70f, -100f, 0f), StellarCoral, 22f),
            Atom3D("H", Vector3D(-110f, -100f, 0f), QuantumCyan, 13f),
            Atom3D("O", Vector3D(0f, -80f, 0f), StellarCoral, 22f),
            Atom3D("C", Vector3D(40f, -100f, 30f), LaserBlue, 20f),
            Atom3D("O", Vector3D(80f, -90f, 30f), StellarCoral, 22f),
            Atom3D("C", Vector3D(30f, -145f, 40f), LaserBlue, 20f)
        )
        else -> listOf(Atom3D("X", Vector3D(0f, 0f, 0f), BioGreen, 20f))
    }

    // Target bonding connections
    val bonds = when (moleculeName) {
        "Water" -> listOf(0 to 1, 0 to 2)
        "Carbon Dioxide" -> listOf(0 to 1, 0 to 2) // Represent bond links
        "Methane" -> listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4)
        "Ethanol" -> listOf(0 to 1, 1 to 2, 2 to 3, 0 to 4, 0 to 5, 0 to 6, 1 to 7, 1 to 8)
        "Sodium Chloride" -> listOf(
            0 to 1, 1 to 2, 2 to 3, 3 to 0, // front
            4 to 5, 5 to 6, 6 to 7, 7 to 4, // back
            0 to 4, 1 to 5, 2 to 6, 3 to 7  // links
        )
        "Graphene Grid" -> listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 0)
        "Benzene Ring" -> listOf(
            0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 0,
            0 to 6, 1 to 7, 2 to 8, 3 to 9, 4 to 10, 5 to 11
        )
        "Aspirin Molecule" -> listOf(
            0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 0,
            1 to 6, 2 to 7, 3 to 8, 4 to 9,
            0 to 10, 10 to 11, 10 to 12, 12 to 13,
            5 to 14, 14 to 15, 15 to 16, 15 to 17
        )
        else -> emptyList()
    }

    // 1. Project rotatable atoms and tracking depths
    val rotatedAtoms = atomsRaw.map { atom ->
        val rot = atom.pos.rotate(yaw, pitch)
        val proj = rot.project(centerX, centerY, scale)
        atom to proj
    }

    // 2. Render Molecular Bonds (connecting cylinders)
    bonds.forEach { (first, second) ->
        val (_, p1) = rotatedAtoms.getOrNull(first) ?: return@forEach
        val (_, p2) = rotatedAtoms.getOrNull(second) ?: return@forEach

        // Draw double lines for carbon double bonds
        if (moleculeName == "Carbon Dioxide") {
            drawLine(
                color = CosmicSlate,
                start = Offset(p1.x - 4f, p1.y - 4f),
                end = Offset(p2.x - 4f, p2.y - 4f),
                strokeWidth = 3.5f * (p1.scale + p2.scale) / 2f
            )
            drawLine(
                color = CosmicSlate,
                start = Offset(p1.x + 4f, p1.y + 4f),
                end = Offset(p2.x + 4f, p2.y + 4f),
                strokeWidth = 3.5f * (p1.scale + p2.scale) / 2f
            )
        } else {
            drawLine(
                color = PureWhite.copy(alpha = 0.5f),
                start = Offset(p1.x, p1.y),
                end = Offset(p2.x, p2.y),
                strokeWidth = 4f * (p1.scale + p2.scale) / 2f
            )
        }
    }

    // 3. Render Atoms Sorted by Depth (draw distant objects behind)
    val sortedAtoms = rotatedAtoms.sortedBy { (_, proj) -> proj.scale } // smaller scale = deeper z distance
    sortedAtoms.forEach { (atom, proj) ->
        val finalR = atom.r * scale * proj.scale

        // Atomic drop-shadow backing
        drawCircle(
            color = Color.Black.copy(alpha = 0.4f),
            radius = finalR + 2f,
            center = Offset(proj.x, proj.y)
        )

        // Atom sphere body
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(PureWhite, atom.color, atom.color.copy(alpha = 0.4f)),
                center = Offset(proj.x - finalR * 0.3f, proj.y - finalR * 0.3f),
                radius = finalR
            ),
            radius = finalR,
            center = Offset(proj.x, proj.y)
        )

        // Atomic valence labels
        val textLabel = atom.element
        // Draw small label indicators next to core atoms
        drawCircle(
            color = PureWhite.copy(alpha = 0.15f),
            radius = finalR,
            center = Offset(proj.x, proj.y),
            style = Stroke(width = 1.2f)
        )
    }
}

// BIOLOGY RENDER: Helix molecular double base pairing
private fun DrawScope.drawDNAHelix(
    centerX: Float, centerY: Float,
    scale: Float, yaw: Float, pitch: Float,
    time: Float
) {
    val helixHeight = 240f
    val BasePairsCount = 20
    val stepHeight = helixHeight / BasePairsCount

    val pointsStrandA = ArrayList<Point2D>()
    val pointsStrandB = ArrayList<Point2D>()
    val baseColors = listOf(QuantumCyan, LaserBlue, BioGreen, ChemAmber)

    for (i in 0..BasePairsCount) {
        val relativeY = -helixHeight / 2f + (i * stepHeight)
        val theta = (i * 0.38f) + Math.toRadians(time.toDouble()).toFloat()

        // Spiral Coordinates
        val helixRadius = 45f
        val rawVecA = Vector3D(helixRadius * cos(theta), relativeY, helixRadius * sin(theta))
        val rawVecB = Vector3D(-helixRadius * cos(theta), relativeY, -helixRadius * sin(theta))

        val prA = rawVecA.rotate(yaw, pitch).project(centerX, centerY, scale)
        val prB = rawVecB.rotate(yaw, pitch).project(centerX, centerY, scale)

        pointsStrandA.add(prA)
        pointsStrandB.add(prB)

        // Draw connecting Base Pairing rungs
        val pairColor = baseColors[i % baseColors.size]
        drawLine(
            color = pairColor.copy(alpha = 0.8f),
            start = Offset(prA.x, prA.y),
            end = Offset((prA.x + prB.x) / 2f, (prA.y + prB.y) / 2f),
            strokeWidth = 3f * (prA.scale + prB.scale) / 2f
        )
        drawLine(
            color = baseColors[(i + 1) % baseColors.size].copy(alpha = 0.8f),
            start = Offset((prA.x + prB.x) / 2f, (prA.y + prB.y) / 2f),
            end = Offset(prB.x, prB.y),
            strokeWidth = 3f * (prA.scale + prB.scale) / 2f
        )
    }

    // Draw connecting strands lines
    for (i in 0 until BasePairsCount) {
        val a1 = pointsStrandA[i]
        val a2 = pointsStrandA[i + 1]
        val b1 = pointsStrandB[i]
        val b2 = pointsStrandB[i + 1]

        // Strand side A
        drawLine(
            color = QuantumCyan,
            start = Offset(a1.x, a1.y),
            end = Offset(a2.x, a2.y),
            strokeWidth = 5f * a1.scale
        )

        // Strand side B
        drawLine(
            color = BioGreen,
            start = Offset(b1.x, b1.y),
            end = Offset(b2.x, b2.y),
            strokeWidth = 5f * b1.scale
        )
    }
}

// COMPUTER SCIENCE RENDER: Interactive 3D Binary Search Tree (BST) Node Nodes
private fun DrawScope.drawComputerScienceBST(
    centerX: Float, centerY: Float,
    scale: Float, yaw: Float, pitch: Float,
    nodes: List<Int>
) {
    if (nodes.isEmpty()) return

    // Quick helper to index and locate binary node depths
    data class TreePlottedNode(
        val value: Int,
        val x: Float,
        val y: Float,
        val z: Float,
        val parentValue: Int?
    )

    // Build absolute coordinates
    val levelsList = mutableListOf<TreePlottedNode>()

    fun assignCoordinates(nodeValue: Int, currX: Float, currY: Float, currZ: Float, depth: Int, parentVal: Int?) {
        levelsList.add(TreePlottedNode(nodeValue, currX, currY, currZ, parentVal))

        // Find smaller nodes on the left, larger on right based on children
        val leftNode = nodes.filter { it < nodeValue }.maxOrNull()
        val rightNode = nodes.filter { it > nodeValue }.minOrNull()

        // Limit recursive depth representation
        if (depth >= 4) return

        val offsetMultiplier = 95f / (depth + 1)
        val nextLeft = nodes.filter { it < nodeValue }.sorted().getOrNull(0)
        val nextRight = nodes.filter { it > nodeValue }.sorted().getOrNull(0)

        // Represent BST path splits
        val remainingLeft = nodes.filter { it < nodeValue && it != parentVal }
        if (remainingLeft.isNotEmpty()) {
            val childVal = remainingLeft.first()
            if (levelsList.none { it.value == childVal }) {
                assignCoordinates(childVal, currX - offsetMultiplier, currY + 45f, currZ - 20f, depth + 1, nodeValue)
            }
        }

        val remainingRight = nodes.filter { it > nodeValue && it != parentVal }
        if (remainingRight.isNotEmpty()) {
            val childVal = remainingRight.first()
            if (levelsList.none { it.value == childVal }) {
                assignCoordinates(childVal, currX + offsetMultiplier, currY + 45f, currZ + 20f, depth + 1, nodeValue)
            }
        }
    }

    // Root node begins at 50
    val rootVal = nodes.firstOrNull() ?: 50
    assignCoordinates(rootVal, 0f, -80f, 0f, 0, null)

    // 1. Draw connection links
    levelsList.forEach { node ->
        if (node.parentValue != null) {
            val parent = levelsList.find { it.value == node.parentValue }
            if (parent != null) {
                val prParent = Vector3D(parent.x, parent.y, parent.z).rotate(yaw, pitch).project(centerX, centerY, scale)
                val prChild = Vector3D(node.x, node.y, node.z).rotate(yaw, pitch).project(centerX, centerY, scale)

                drawLine(
                    color = QuantumCyan.copy(alpha = 0.5f),
                    start = Offset(prParent.x, prParent.y),
                    end = Offset(prChild.x, prChild.y),
                    strokeWidth = 3f * prChild.scale
                )
            }
        }
    }

    // 2. Draw circular node shells
    levelsList.sortedBy { Vector3D(it.x, it.y, it.z).rotate(yaw, pitch).z }.forEach { node ->
        val pr = Vector3D(node.x, node.y, node.z).rotate(yaw, pitch).project(centerX, centerY, scale)
        val nodeRadius = 14f * pr.scale

        drawCircle(
            color = SolarCoal,
            radius = nodeRadius,
            center = Offset(pr.x, pr.y)
        )
        drawCircle(
            color = if (node.value == rootVal) BioGreen else QuantumCyan,
            radius = nodeRadius,
            center = Offset(pr.x, pr.y),
            style = Stroke(width = 2f)
        )

        // Small indicator circle for visual weight
        drawCircle(
            color = PureWhite.copy(alpha = 0.4f),
            radius = nodeRadius * 0.3f,
            center = Offset(pr.x - nodeRadius * 0.4f, pr.y - nodeRadius * 0.4f)
        )
    }
}

// MATHEMATICS RENDER: Wireframe Multivariable Calculus graphing
private fun DrawScope.drawMathSurfaceGrapher(
    centerX: Float, centerY: Float,
    scale: Float, yaw: Float, pitch: Float,
    equationName: String, time: Float
) {
    val divisions = 14
    val step = 140f / divisions
    val mathPoints = Array(divisions + 1) { FloatArray(divisions + 1) }

    // Generates surface height (Z) profiles
    for (i in 0..divisions) {
        val xVal = -70f + i * step
        for (j in 0..divisions) {
            val yVal = -70f + j * step
            // Compute corresponding height
            val zHeight = when (equationName) {
                "Sine Ripple" -> {
                    val radius = sqrt(xVal * xVal + yVal * yVal)
                    35f * sin((radius / 15f) - (time * 0.08f))
                }
                "Saddle" -> {
                    (xVal * xVal - yVal * yVal) / 120f
                }
                "Egg Crate" -> {
                    28f * cos(xVal / 10f) * sin(yVal / 10f)
                }
                else -> {
                    22f * sin(xVal / 12f)
                }
            }
            mathPoints[i][j] = zHeight
        }
    }

    // Convert vector point arrays mapping perspective screen locations
    val projectedPoints = Array(divisions + 1) { arrayOfNulls<Point2D>(divisions + 1) }
    for (i in 0..divisions) {
        val xVal = -60f + i * (120f / divisions)
        for (j in 0..divisions) {
            val yVal = -60f + j * (120f / divisions)
            val vec3D = Vector3D(xVal, mathPoints[i][j], yVal)
            val rot = vec3D.rotate(yaw, pitch)
            projectedPoints[i][j] = rot.project(centerX, centerY, scale)
        }
    }

    // Sweeping wire mesh rows
    for (i in 0..divisions) {
        for (j in 0..divisions) {
            val pCurr = projectedPoints[i][j] ?: continue

            // Line towards X successor
            if (i < divisions) {
                val pNextX = projectedPoints[i + 1][j]
                if (pNextX != null) {
                    val heightRatio = (mathPoints[i][j] + 40f) / 80f
                    val lineColor = Color.interpolate(BioGreen, QuantumCyan, heightRatio)
                    drawLine(
                        color = lineColor.copy(alpha = 0.35f),
                        start = Offset(pCurr.x, pCurr.y),
                        end = Offset(pNextX.x, pNextX.y),
                        strokeWidth = 1f
                    )
                }
            }

            // Line towards Y successor
            if (j < divisions) {
                val pNextY = projectedPoints[i][j + 1]
                if (pNextY != null) {
                    val heightRatio = (mathPoints[i][j] + 40f) / 80f
                    val lineColor = Color.interpolate(LaserBlue, ChemAmber, heightRatio)
                    drawLine(
                        color = lineColor.copy(alpha = 0.35f),
                        start = Offset(pCurr.x, pCurr.y),
                        end = Offset(pNextY.x, pNextY.y),
                        strokeWidth = 1f
                    )
                }
            }
        }
    }
}

// Utility extension for interpolating mathematical colors
fun Color.Companion.interpolate(start: Color, end: Color, ratio: Float): Color {
    val r = ratio.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * r,
        green = start.green + (end.green - start.green) * r,
        blue = start.blue + (end.blue - start.blue) * r,
        alpha = start.alpha + (end.alpha - start.alpha) * r
    )
}

// PANEL: Science simulation controls & bookmarks notes
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScienceLabControlPanel(subject: Subject, viewModel: ScienceViewModel) {
    // Collect settings
    val speed by viewModel.simSpeed.collectAsStateWithLifecycle()
    val gravity by viewModel.gravityScale.collectAsStateWithLifecycle()
    val moleculeChosen by viewModel.selectedMolecule.collectAsStateWithLifecycle()
    val mathEquation by viewModel.mathEquation.collectAsStateWithLifecycle()
    val noteTyped by viewModel.notesInput.collectAsStateWithLifecycle()

    val physicsMode by viewModel.physicsMode.collectAsStateWithLifecycle()
    val biologyMode by viewModel.biologyMode.collectAsStateWithLifecycle()
    val computerScienceMode by viewModel.computerScienceMode.collectAsStateWithLifecycle()
    val selectedHistoryEpoch by viewModel.selectedHistoryEpoch.collectAsStateWithLifecycle()
    val selectedPhilosophySchool by viewModel.selectedPhilosophySchool.collectAsStateWithLifecycle()
    val selectedHackingSystem by viewModel.selectedHackingSystem.collectAsStateWithLifecycle()
    val selectedReligionSymbol by viewModel.selectedReligionSymbol.collectAsStateWithLifecycle()

    var showNoteSavedBadge by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${subject.displayName} Laboratory Core Controls",
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            // Bookmark/Save button for recording observation
            Button(
                onClick = {
                    viewModel.saveScienceNote(
                        subjectText = subject.displayName,
                        keyText = when (subject) {
                            Subject.PHYSICS -> "PhysicsMode_${physicsMode}"
                            Subject.CHEMISTRY -> "Molecules_$moleculeChosen"
                            Subject.BIOLOGY -> "BiologyMode_${biologyMode}"
                            Subject.COMPUTER_SCIENCE -> "CSMode_${computerScienceMode}"
                            Subject.MATHEMATICS -> "Equation_$mathEquation"
                            Subject.HISTORY -> "HistoryEpoch_${selectedHistoryEpoch}"
                            Subject.PHILOSOPHY -> "PhilosophySchool_${selectedPhilosophySchool}"
                            Subject.ETHICAL_HACKING -> "HackingSystem_${selectedHackingSystem}"
                            Subject.RELIGION -> "ReligionSymbol_${selectedReligionSymbol}"
                        }
                    )
                    showNoteSavedBadge = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = QuantumCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(35.dp)
                    .testTag("save_lab_note_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = "Save Note",
                    tint = MidnightBlack,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Log Observation", color = MidnightBlack, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Animated confirmation state overlay
        if (showNoteSavedBadge) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showNoteSavedBadge = false },
                colors = CardDefaults.cardColors(containerColor = BioGreen.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Saved", tint = BioGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Interactive lab research logged to system notebook vault!",
                        color = BioGreen,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(modifier = Modifier.weight(1f)) {
            // Speed controls parameters (Left Panel)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text("SIMULATION MATRIX", color = QuantumCyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                // Render distinct controls relative to Subject
                when (subject) {
                    Subject.PHYSICS -> {
                        Text("Gravity Warp ($gravity)", color = PureWhite, style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = gravity,
                            onValueChange = { viewModel.gravityScale.value = it },
                            valueRange = 0.5f..3.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = QuantumCyan,
                                activeTrackColor = QuantumCyan,
                                inactiveTrackColor = NebulaDark
                            ),
                            modifier = Modifier.testTag("gravity_slider")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Active Physics Mode", color = CosmicSlate, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        listOf("Planetary Orbit", "Wave-Particle Duality", "Quantum Field", "Thermodynamics Entropy", "Electromagnetic Induction").forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (physicsMode == mode) NebulaDark else Color.Transparent)
                                    .clickable { viewModel.physicsMode.value = mode }
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(mode, color = if (physicsMode == mode) QuantumCyan else PureWhite, style = MaterialTheme.typography.bodySmall)
                                if (physicsMode == mode) Icon(Icons.Filled.RadioButtonChecked, contentDescription = "Active", tint = QuantumCyan, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                    Subject.CHEMISTRY -> {
                        Text("Active Compound", color = CosmicSlate, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        listOf("Water", "Carbon Dioxide", "Methane", "Ethanol", "Sodium Chloride", "Graphene Grid", "Benzene Ring", "Aspirin Molecule").forEach { molecule ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (moleculeChosen == molecule) NebulaDark else Color.Transparent)
                                    .clickable { viewModel.selectedMolecule.value = molecule }
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (molecule) {
                                        "Water" -> "Water (H₂O)"
                                        "Carbon Dioxide" -> "Carbon Dioxide (CO₂)"
                                        "Methane" -> "Methane (CH₄)"
                                        "Ethanol" -> "Ethanol (C₂H₅OH)"
                                        "Sodium Chloride" -> "Solt Grid (NaCl)"
                                        "Graphene Grid" -> "Graphene (C₆)"
                                        "Benzene Ring" -> "Benzene (C₆H₆)"
                                        "Aspirin Molecule" -> "Aspirin (C₉H₈O₄)"
                                        else -> molecule
                                    },
                                    color = if (moleculeChosen == molecule) QuantumCyan else PureWhite,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (moleculeChosen == molecule) {
                                    Icon(Icons.Filled.RadioButtonChecked, contentDescription = "Active", tint = QuantumCyan, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                    Subject.BIOLOGY -> {
                        Text("Active Biology Model", color = CosmicSlate, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        listOf("DNA Double Helix", "Nerve Impulse", "Chloroplast Cell", "Mitochondria Membrane", "Mitosis Splitting").forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (biologyMode == mode) NebulaDark else Color.Transparent)
                                    .clickable { viewModel.biologyMode.value = mode }
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(mode, color = if (biologyMode == mode) QuantumCyan else PureWhite, style = MaterialTheme.typography.bodySmall)
                                if (biologyMode == mode) Icon(Icons.Filled.RadioButtonChecked, contentDescription = "Active", tint = QuantumCyan, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                    Subject.COMPUTER_SCIENCE -> {
                        Text("CS View State", color = CosmicSlate, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        listOf("Binary Search Tree", "Neural Net Layers", "Graph Mesh", "Blockchain Consensus", "Bubble Sort Track").forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (computerScienceMode == mode) NebulaDark else Color.Transparent)
                                    .clickable { viewModel.computerScienceMode.value = mode }
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(mode, color = if (computerScienceMode == mode) QuantumCyan else PureWhite, style = MaterialTheme.typography.bodySmall)
                                if (computerScienceMode == mode) Icon(Icons.Filled.RadioButtonChecked, contentDescription = "Active", tint = QuantumCyan, modifier = Modifier.size(12.dp))
                            }
                        }
                        
                        if (computerScienceMode == "Binary Search Tree") {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("BST Node Insert", color = CosmicSlate, style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(6.dp))
                            var customNodeInput by remember { mutableStateOf("") }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                TextField(
                                    value = customNodeInput,
                                    onValueChange = { customNodeInput = it },
                                    placeholder = { Text("Num", color = CosmicSlate) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .border(1.dp, NebulaDark, RoundedCornerShape(8.dp))
                                        .testTag("node_input_field"),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = NebulaDark,
                                        unfocusedContainerColor = NebulaDark,
                                        focusedTextColor = PureWhite,
                                        unfocusedTextColor = PureWhite,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        val number = customNodeInput.toIntOrNull()
                                        if (number != null && number in 1..99) {
                                            viewModel.addTreeNode(number)
                                            customNodeInput = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = LaserBlue)
                                ) {
                                    Text("+", color = PureWhite, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { viewModel.resetTree() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = NebulaDark)
                            ) {
                                // Explicit note: "Tree reset code"
                                Text("Reset BST", color = CosmicSlate, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Subject.MATHEMATICS -> {
                        Text("Active Mathematical Graph", color = CosmicSlate, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        listOf("Sine Ripple", "Saddle", "Egg Crate", "Wave", "Mobius Strip", "Lorenz Attractor", "Torus Ring", "Mandelbrot Fractal").forEach { eq ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (mathEquation == eq) NebulaDark else Color.Transparent)
                                    .clickable { viewModel.mathEquation.value = eq }
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (eq) {
                                        "Sine Ripple" -> "z = sin(√(x²+y²))"
                                        "Saddle" -> "z = x² - y²"
                                        "Egg Crate" -> "z = cos(x)sin(y)"
                                        "Wave" -> "z = sine wave oscillation"
                                        "Mobius Strip" -> "Complex surface mapping"
                                        "Lorenz Attractor" -> "Chaotic attractor plot"
                                        "Torus Ring" -> "Torus donut wireframe"
                                        "Mandelbrot Fractal" -> "Mandelbrot attractor map"
                                        else -> eq
                                    },
                                    color = if (mathEquation == eq) QuantumCyan else PureWhite,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (mathEquation == eq) {
                                    Icon(Icons.Filled.RadioButtonChecked, contentDescription = "Active", tint = QuantumCyan, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                    Subject.HISTORY -> {
                        Text("Active Historical Epoch", color = CosmicSlate, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        listOf("Stone Age", "Ancient Egypt", "Roman Empire", "Industrial Rev", "Space Age", "Renaissance Period", "Information Age").forEach { epoch ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (selectedHistoryEpoch == epoch) NebulaDark else Color.Transparent)
                                    .clickable { viewModel.selectedHistoryEpoch.value = epoch }
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(epoch, color = if (selectedHistoryEpoch == epoch) QuantumCyan else PureWhite, style = MaterialTheme.typography.bodySmall)
                                if (selectedHistoryEpoch == epoch) Icon(Icons.Filled.RadioButtonChecked, contentDescription = "Active", tint = QuantumCyan, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                    Subject.PHILOSOPHY -> {
                        Text("Active Philosophy School", color = CosmicSlate, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        listOf("Stoicism", "Existentialism", "Rationalism", "Nihilism", "Platonic Idealism", "Empirical Science").forEach { school ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (selectedPhilosophySchool == school) NebulaDark else Color.Transparent)
                                    .clickable { viewModel.selectedPhilosophySchool.value = school }
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(school, color = if (selectedPhilosophySchool == school) QuantumCyan else PureWhite, style = MaterialTheme.typography.bodySmall)
                                if (selectedPhilosophySchool == school) Icon(Icons.Filled.RadioButtonChecked, contentDescription = "Active", tint = QuantumCyan, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                    Subject.ETHICAL_HACKING -> {
                        Text("Intrusion Safeguard Matrix", color = CosmicSlate, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        listOf("Firewall Shield", "Brute Force", "Packet Sniffer", "SQL Injection", "Phishing Anchor", "Zero Day Exploit").forEach { system ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (selectedHackingSystem == system) NebulaDark else Color.Transparent)
                                    .clickable { viewModel.selectedHackingSystem.value = system }
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(system, color = if (selectedHackingSystem == system) QuantumCyan else PureWhite, style = MaterialTheme.typography.bodySmall)
                                if (selectedHackingSystem == system) Icon(Icons.Filled.RadioButtonChecked, contentDescription = "Active", tint = QuantumCyan, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                    Subject.RELIGION -> {
                        Text("Active Theological Philosophy", color = CosmicSlate, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        listOf("Dharma Wheel", "Taoist Yin-Yang", "Cross & Crescent", "Aura Mandala", "Sufi Whirling Star", "Tree of Life Nodes").forEach { symbol ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (selectedReligionSymbol == symbol) NebulaDark else Color.Transparent)
                                    .clickable { viewModel.selectedReligionSymbol.value = symbol }
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(symbol, color = if (selectedReligionSymbol == symbol) QuantumCyan else PureWhite, style = MaterialTheme.typography.bodySmall)
                                if (selectedReligionSymbol == symbol) Icon(Icons.Filled.RadioButtonChecked, contentDescription = "Active", tint = QuantumCyan, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text("Rotation Speed ($speed)", color = PureWhite, style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = speed,
                    onValueChange = { viewModel.simSpeed.value = it },
                    valueRange = 0.0f..2.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = QuantumCyan,
                        activeTrackColor = QuantumCyan,
                        inactiveTrackColor = NebulaDark
                    ),
                    modifier = Modifier.testTag("speed_slider")
                )
            }

            // Observations Notepad (Right Panel)
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .padding(start = 8.dp)
            ) {
                Text("RESEARCH NOTEBOOK", color = BioGreen, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                TextField(
                    value = noteTyped,
                    onValueChange = { viewModel.notesInput.value = it },
                    placeholder = {
                        Text(
                            text = "Write your observations or hypothesis about this 3D model...",
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmicSlate
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, NebulaDark, RoundedCornerShape(8.dp))
                        .testTag("observation_note_text_field"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = NebulaDark,
                        unfocusedContainerColor = NebulaDark,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}

// --- 2. 3D VIDEO LECTURES ---
@Composable
fun ScreenVideoLectures(viewModel: ScienceViewModel) {
    val currentLectureId by viewModel.currentLectureId.collectAsStateWithLifecycle()
    val isPlaying by viewModel.lecturePlaying.collectAsStateWithLifecycle()
    val progress by viewModel.lectureProgressSeconds.collectAsStateWithLifecycle()

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                viewModel.incrementLectureProgress()
            }
        }
    }

    val lecturesList = viewModel.videoLectures
    val activeLecture = lecturesList.find { it.id == currentLectureId } ?: lecturesList.first()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "3D Immersive Video Lectures",
            color = PureWhite,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Watch professional narrated visualisations synchronised with math telemetry graphs.",
            color = CosmicSlate,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Lecture List Selector (Left side on wide or standard column)
            ScrollableLectureList(
                lectures = lecturesList,
                activeId = currentLectureId,
                onSelected = { viewModel.selectLecture(it) },
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
                    .padding(end = 12.dp)
            )

            // Cinematic Telemetry Player (Right side)
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
            ) {
                // TV Player Panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MidnightBlack)
                        .border(2.dp, NebulaDark)
                ) {
                    // Simulated Animated video backdrop content
                    AnimatedVideoBackdrop(progress = progress, hologramType = activeLecture.hologramType)

                    // Subtitle overlay block
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = viewModel.getActiveSubtitle(),
                            color = QuantumCyan,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Live Hologram Badge Icon
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .background(StellarCoral.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .border(1.dp, StellarCoral, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.VideoSettings, contentDescription = "Live HD", tint = StellarCoral, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("3D HUD ALIVE", color = StellarCoral, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                // Subtitle playback controls
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = SolarCoal)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.toggleLecturePlay() }) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                                    contentDescription = "Play/Pause",
                                    tint = QuantumCyan,
                                    modifier = Modifier.size(34.dp)
                                )
                            }

                            // Dynamic timing labels
                            Text(
                                text = "Time: 0:${progress.toString().padStart(2, '0')} / 1:00",
                                color = PureWhite,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Slider(
                            value = progress.toFloat(),
                            onValueChange = { viewModel.seekLecture(it.toInt()) },
                            valueRange = 0f..60f,
                            colors = SliderDefaults.colors(
                                thumbColor = QuantumCyan,
                                activeTrackColor = QuantumCyan,
                                inactiveTrackColor = NebulaDark
                            ),
                            modifier = Modifier.testTag("video_progress_slider")
                        )
                    }
                }

                // Video telemetry stats values
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NebulaDark)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "SCIENCE REAL-TIME TELEMETRY STATS",
                            color = BioGreen,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        activeLecture.telemetryParams.forEach { (label, valueCalc) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(label, color = CosmicSlate, style = MaterialTheme.typography.labelSmall)
                                Text(valueCalc(), color = PureWhite, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Left hand lecture chooser scroll list
@Composable
fun ScrollableLectureList(
    lectures: List<VideoLecture>,
    activeId: String,
    onSelected: (String) -> Unit,
    modifier: Modifier
) {
    LazyColumn(modifier = modifier) {
        items(lectures) { lec ->
            val isActive = lec.id == activeId
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clickable { onSelected(lec.id) }
                    .testTag("video_card_${lec.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) NebulaDark else SolarCoal
                ),
                border = if (isActive) BorderStroke(1.dp, QuantumCyan) else null
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = lec.title,
                        color = if (isActive) QuantumCyan else PureWhite,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Subject: ${lec.subject} | Time: 1:00",
                        color = CosmicSlate,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = lec.summary,
                        color = CosmicSlate,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// Custom simulated 3D rendering visual playing in the TV module
@Composable
fun AnimatedVideoBackdrop(progress: Int, hologramType: String) {
    val transition = rememberInfiniteTransition(label = "BackdropCycles")
    val animFactor by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "TrigFactor"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        drawRect(color = MidnightBlack)

        // Draw spatial radar rings playing
        drawCircle(
            color = LaserBlue.copy(alpha = 0.08f),
            radius = cx * 0.7f,
            center = Offset(cx, cy),
            style = Stroke(width = 1f)
        )

        when (hologramType) {
            "black_hole" -> {
                // Drawing orbiting black hole singularity mesh
                val steps = 40
                val angleInc = 360f / steps
                for (i in 0..steps) {
                    val theta = Math.toRadians((i * angleInc).toDouble()).toFloat()
                    val spiralRadius = 80f + sin(animFactor + (i * 0.5f)) * 30f
                    val ptX = cx + spiralRadius * cos(theta)
                    val ptY = cy + spiralRadius * sin(theta) * 0.4f // distorted projection

                    drawLine(
                        color = QuantumCyan.copy(alpha = 0.25f),
                        start = Offset(cx, cy),
                        end = Offset(ptX, ptY),
                        strokeWidth = 1f
                    )
                }
                // Singularity Void
                drawCircle(color = MidnightBlack, radius = 32f, center = Offset(cx, cy))
                drawCircle(color = ChemAmber, radius = 33f, center = Offset(cx, cy), style = Stroke(width = 2f))
            }
            "molecules" -> {
                // Spinning atomic grid particles
                for (i in 1..4) {
                    val orbitAngle = animFactor + i * 1.5f
                    val rX = 70f * cos(orbitAngle)
                    val rY = 30f * sin(orbitAngle)
                    drawCircle(
                        color = BioGreen,
                        radius = 8f,
                        center = Offset(cx + rX, cy + rY)
                    )
                    drawLine(
                        color = PureWhite.copy(alpha = 0.15f),
                        start = Offset(cx, cy),
                        end = Offset(cx + rX, cy + rY),
                        strokeWidth = 2f
                    )
                }
                drawCircle(color = LaserBlue, radius = 15f, center = Offset(cx, cy))
            }
            "nerve_signal" -> {
                // Synaptic pulse signals waveforms
                for (i in 0..25) {
                    val x = size.width * (i / 25f)
                    val y = cy + sin(animFactor + i * 0.3f) * 40f
                    drawCircle(
                        color = QuantumCyan.copy(alpha = 0.7f),
                        radius = 3f,
                        center = Offset(x, y)
                    )
                    if (i > 0) {
                        drawLine(
                            color = QuantumCyan.copy(alpha = 0.3f),
                            start = Offset(size.width * ((i - 1) / 25f), cy + sin(animFactor + (i - 1) * 0.3f) * 40f),
                            end = Offset(x, y),
                            strokeWidth = 2.5f
                        )
                    }
                }
            }
            "fractal" -> {
                // Expanding contour field grids
                val steps = 8
                for (i in 1..steps) {
                    val radialGrowth = (i * 15f + (progress * 3f) % 20f) * 1.2f
                    drawCircle(
                        color = ChemAmber.copy(alpha = (steps - i) / (steps.toFloat() * 1.5f)),
                        radius = radialGrowth,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.8f)
                    )
                }
            }
        }
    }
}

// --- 3. AI TUTOR SCREEN ---
@Composable
fun ScreenAITutor(viewModel: ScienceViewModel) {
    val responseText by viewModel.tutorResponse.collectAsStateWithLifecycle()
    val isTutorLoading by viewModel.isTutorLoading.collectAsStateWithLifecycle()
    val queryVal by viewModel.tutorQueryInput.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "AI Science & Math Solver",
            color = PureWhite,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Ask complex questions or solve advanced multi-variable calculus equations.",
            color = CosmicSlate,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Prebuilt Quick Prompts Rows
        Text(
            text = "TAP READY STEM STUDY TOPICS:",
            color = QuantumCyan,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))

        val promptRows = listOf(
            "Explain Methane Covalent (sp3) bonds geometry in 3D.",
            "Deconstruct Einstein Event Horizon mechanics.",
            "Solve gradient & saddle path multivariable contours.",
            "How does DNA replication sequence bases transcription?"
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(promptRows) { promptText ->
                Card(
                    modifier = Modifier
                        .widthIn(max = 240.dp)
                        .clickable { viewModel.askTutor(promptText) }
                        .testTag("terminal_study_template"),
                    colors = CardDefaults.cardColors(containerColor = SolarCoal),
                    border = BorderStroke(1.dp, NebulaDark)
                ) {
                    Box(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = promptText,
                            style = MaterialTheme.typography.labelSmall,
                            color = PureWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Ask Input field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = queryVal,
                onValueChange = { viewModel.tutorQueryInput.value = it },
                placeholder = { Text("Ask your scientific tutor...", color = CosmicSlate) },
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, NebulaDark, RoundedCornerShape(8.dp))
                    .testTag("ai_tutor_input_field"),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        viewModel.askTutor()
                        focusManager.clearFocus()
                    }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SolarCoal,
                    unfocusedContainerColor = SolarCoal,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    viewModel.askTutor()
                    focusManager.clearFocus()
                },
                modifier = Modifier
                    .height(54.dp)
                    .testTag("ai_tutor_ask_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = QuantumCyan),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isTutorLoading) {
                    CircularProgressIndicator(color = MidnightBlack, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = "Query", tint = MidnightBlack)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Explanation Console Terminal
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = NebulaDark),
            border = BorderStroke(1.dp, QuantumCyan.copy(alpha = 0.25f))
        ) {
            Box(modifier = Modifier.padding(14.dp)) {
                if (responseText.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LiveHelp,
                            contentDescription = "Ready to assist",
                            tint = CosmicSlate,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tutor Console Idle.",
                            color = PureWhite,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Type your coordinates or choose a prepopulated study topic to see quantum answers.",
                            color = CosmicSlate,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SolarCoal)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("SOLVER TERMINAL ACCELERATED", color = BioGreen, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                IconButton(
                                    onClick = { viewModel.clearTutor() },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear Console", tint = CosmicSlate, modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = responseText,
                                color = PureWhite,
                                style = MaterialTheme.typography.bodyMedium,
                                letterSpacing = 0.4.sp,
                                fontFamily = FontFamily.SansSerif,
                                modifier = Modifier.testTag("ai_tutor_response_text")
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- 4. NOTES & BOOKMARKS VAULT ---
@Composable
fun ScreenNotesVault(viewModel: ScienceViewModel) {
    val allNotes by viewModel.allNotes.collectAsStateWithLifecycle()
    val bookmarkedNotes by viewModel.bookmarkedNotes.collectAsStateWithLifecycle()

    var showOnlyBookmarks by remember { mutableStateOf(false) }
    val displayNotes = if (showOnlyBookmarks) bookmarkedNotes else allNotes

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Laboratory Notes Vault",
                    color = PureWhite,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Observe stored details logged from your 3D sandboxes.",
                    color = CosmicSlate,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Quick toggle filter chip
            FilterChip(
                selected = showOnlyBookmarks,
                onClick = { showOnlyBookmarks = !showOnlyBookmarks },
                label = { Text("Starred", fontWeight = FontWeight.Bold) },
                leadingIcon = {
                    Icon(
                        imageVector = if (showOnlyBookmarks) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = "Filter Starred",
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ChemAmber,
                    selectedLabelColor = MidnightBlack,
                    containerColor = NebulaDark,
                    labelColor = CosmicSlate
                )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (displayNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.NoteAlt,
                        contentDescription = "Empty Notes",
                        tint = CosmicSlate,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No observation logs in this sector.",
                        color = PureWhite,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Visit the 3D lab or video players and tap 'Log Observation' to persist records inside this Room Database.",
                        color = CosmicSlate,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayNotes, key = { it.id }) { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("science_note_card_${note.id}"),
                        colors = CardDefaults.cardColors(containerColor = SolarCoal),
                        border = BorderStroke(1.dp, NebulaDark)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Colored pill indicator
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                when (note.subject) {
                                                    "Physics" -> LaserBlue
                                                    "Chemistry" -> ChemAmber
                                                    "Biology" -> BioGreen
                                                    "Computer Sci" -> QuantumCyan
                                                    "Math" -> StellarCoral
                                                    else -> CosmicSlate
                                                }
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = note.subject.uppercase(),
                                        color = QuantumCyan,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "ID: ${note.modelKey}",
                                        color = CosmicSlate,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row {
                                    // Bookmark button toggle
                                    IconButton(
                                        onClick = { viewModel.toggleBookmarkState(note.id, note.isBookmarked) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (note.isBookmarked) Icons.Filled.Star else Icons.Outlined.Star,
                                            contentDescription = "Toggle Star",
                                            tint = if (note.isBookmarked) ChemAmber else CosmicSlate,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Delete button
                                    IconButton(
                                        onClick = { viewModel.deleteNote(note) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete Note",
                                            tint = StellarCoral,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = note.content,
                                color = PureWhite,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
                                    .format(java.util.Date(note.timestamp))
                                Text(
                                    text = "Logged: $date",
                                    color = CosmicSlate,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- ADVANCED 3D SIMULATION DRAWING ENGINE ---

private fun DrawScope.drawQuantumWaveParticle(
    centerX: Float, centerY: Float,
    scale: Float, yaw: Float, pitch: Float,
    time: Float
) {
    val pointsCount = 60
    val waveColor1 = QuantumCyan
    val waveColor2 = StellarCoral
    
    // Draw rotating wave packet
    for (i in 0..pointsCount) {
        val tRatio = i.toFloat() / pointsCount
        val xPos = (tRatio - 0.5f) * 200f
        
        // Dynamic envelope wave packet function: Envelope * cos(x)
        val envelope = exp(-0.0002f * xPos * xPos) * 50f
        val theta = (xPos * 0.08f) + Math.toRadians(time.toDouble()).toFloat()
        val yPos = envelope * cos(theta)
        val zPos = envelope * sin(theta)
        
        val proj = Vector3D(xPos, yPos, zPos).rotate(yaw, pitch).project(centerX, centerY, scale)
        
        // Draw dual orbital path lines
        drawCircle(
            color = Color.interpolate(waveColor1, waveColor2, tRatio).copy(alpha = 0.8f * proj.scale),
            radius = 3.5f * proj.scale,
            center = Offset(proj.x, proj.y)
        )
        
        // Draw connector line to axis of propagation
        val axisProj = Vector3D(xPos, 0f, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
        drawLine(
            color = waveColor1.copy(alpha = 0.2f * proj.scale),
            start = Offset(axisProj.x, axisProj.y),
            end = Offset(proj.x, proj.y),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawQuantumField(
    centerX: Float, centerY: Float,
    scale: Float, yaw: Float, pitch: Float,
    time: Float
) {
    // Holographic grid fields
    val gridCount = 5
    for (gx in -gridCount..gridCount) {
        for (gz in -gridCount..gridCount) {
            val x = gx * 25f
            val z = gz * 25f
            
            // Fluctuating height depending on distance from center
            val dist = sqrt(x * x + z * z)
            val wave = sin((dist * 0.05f) - (time * 0.05f)) * 20f * exp(-0.005f * dist)
            
            val proj = Vector3D(x, wave, z).rotate(yaw, pitch).project(centerX, centerY, scale)
            
            drawCircle(
                color = ParticleGrid.copy(alpha = 0.4f * proj.scale),
                radius = 2.5f * proj.scale,
                center = Offset(proj.x, proj.y)
            )
            
            // Draw field vector lines down to baseline plane
            val baseProj = Vector3D(x, -15f, z).rotate(yaw, pitch).project(centerX, centerY, scale)
            drawLine(
                color = QuantumCyan.copy(alpha = 0.15f * proj.scale),
                start = Offset(baseProj.x, baseProj.y),
                end = Offset(proj.x, proj.y),
                strokeWidth = 1f
            )
        }
    }
}

private fun DrawScope.drawNerveImpulseSignal(
    centerX: Float, centerY: Float,
    scale: Float, yaw: Float, pitch: Float,
    time: Float
) {
    // Render a 3D spinal neuron axon path and dynamic action potential pulse
    val segments = 45
    val neuronColor = BioGreen
    val pulseColor = StellarCoral
    
    // Draw axon tube wireframe
    var prevProj: Point2D? = null
    for (i in 0..segments) {
        val radiusValue = 120f
        val angle = (i.toFloat() / segments) * 6.28f
        val x = cos(angle) * radiusValue
        val z = sin(angle) * radiusValue
        
        // Sine wave nodes
        val y = sin((angle * 5f) + (time * 0.04f)) * 25f
        val proj = Vector3D(x, y, z).rotate(yaw, pitch).project(centerX, centerY, scale)
        
        drawCircle(
            color = neuronColor.copy(alpha = 0.5f),
            radius = 3.5f * proj.scale,
            center = Offset(proj.x, proj.y)
        )
        
        if (prevProj != null) {
            drawLine(
                color = neuronColor.copy(alpha = 0.3f),
                start = Offset(prevProj.x, prevProj.y),
                end = Offset(proj.x, proj.y),
                strokeWidth = 2f * proj.scale
            )
        }
        prevProj = proj
        
        // Draw the nerve action potentials charging pulse
        val pulseCenterAngle = (time * 0.015f) % 6.28f
        val deltaAngle = abs(angle - pulseCenterAngle.toFloat())
        if (deltaAngle < 0.35f) {
            val pulseRatio = 1f - (deltaAngle / 0.35f)
            drawCircle(
                color = pulseColor.copy(alpha = pulseRatio),
                radius = 7.5f * proj.scale,
                center = Offset(proj.x, proj.y)
            )
            
            // Emit synapse spark lines
            drawLine(
                color = PureWhite.copy(alpha = pulseRatio),
                start = Offset(proj.x, proj.y),
                end = Offset(proj.x + (x * 0.15f * pulseRatio), proj.y + (y * 0.15f * pulseRatio)),
                strokeWidth = 1.5f * proj.scale
            )
        }
    }
}

private fun DrawScope.drawChloroplastCell(
    centerX: Float, centerY: Float,
    scale: Float, yaw: Float, pitch: Float,
    time: Float
) {
    // Chloroplast layout with stacking thylakoids (grana columns) in 3D perspective
    val chloroplastColor = Color(0xFF2E8B57)
    val granaCount = 6
    val animAngle = Math.toRadians(time.toDouble()).toFloat()
    
    // Draw outer membrane ellipses
    for (i in 1..4) {
        val rX = 140f
        val rZ = 90f
        val yHeight = (i - 2.5f) * 15f
        
        var prevP: Point2D? = null
        for (angIdx in 0..24) {
            val ang = (angIdx.toFloat() / 24f) * 6.28f
            val x = cos(ang) * rX
            val z = sin(ang) * rZ
            val proj = Vector3D(x, yHeight, z).rotate(yaw, pitch).project(centerX, centerY, scale)
            
            if (prevP != null) {
                drawLine(
                    color = chloroplastColor.copy(alpha = 0.15f),
                    start = Offset(prevP.x, prevP.y),
                    end = Offset(proj.x, proj.y),
                    strokeWidth = 1.5f
                )
            }
            prevP = proj
        }
    }
    
    // Draw 3D Thylakoid stacks
    for (idx in 0 until granaCount) {
        val angOffset = (idx.toFloat() / granaCount) * 6.28f
        val pX = cos(angOffset) * 75f
        val pZ = sin(angOffset) * 45f
        
        // Multiple disks in a vertical stack
        for (lh in 0..4) {
            val pY = -30f + (lh * 12f) + sin((lh + idx + time * 0.05f)) * 4f
            val diskCenter = Vector3D(pX, pY, pZ).rotate(yaw, pitch).project(centerX, centerY, scale)
            
            drawCircle(
                color = BioGreen.copy(alpha = 0.8f * diskCenter.scale),
                radius = 12f * diskCenter.scale,
                center = Offset(diskCenter.x, diskCenter.y)
            )
            
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = 7f * diskCenter.scale,
                center = Offset(diskCenter.x, diskCenter.y),
                style = Stroke(width = 1f)
            )
        }
    }
}

private fun DrawScope.drawNeuralNetLayers(
    centerX: Float, centerY: Float,
    scale: Float, yaw: Float, pitch: Float,
    time: Float
) {
    // 3 layered deep neural network containing nodes in 3D projection
    val inputNodes = 4
    val hiddenNodes = 5
    val outputNodes = 3
    
    val layersX = listOf(-120f, 0f, 120f)
    val layerNeurons = listOf(inputNodes, hiddenNodes, outputNodes)
    val colorPrimary = LaserBlue
    val colorPulse = QuantumCyan
    
    data class NetworkPlottedNeuron(val pos: Vector3D, val color: Color, val charge: Float)
    val points = mutableListOf<NetworkPlottedNeuron>()
    
    // Build network layout positions
    for (layerIdx in layersX.indices) {
        val count = layerNeurons[layerIdx]
        val x = layersX[layerIdx]
        for (i in 0 until count) {
            val y = (i.toFloat() - (count - 1) / 2f) * 45f
            val z = sin((i * 1.5f) + (time * 0.05f)) * 12f
            
            // Neuron individual activation charge calculations
            val charge = 0.2f + 0.8f * sin((layerIdx * 12f) + (i * 8f) + (time * 0.05f)).coerceIn(0f, 1f)
            points.add(NetworkPlottedNeuron(Vector3D(x, y, z), colorPrimary, charge))
        }
    }
    
    // 1. Draw synaptical connector links with flowing weight signals
    for (idx in points.indices) {
        val cur = points[idx]
        val nextLayerPoints = points.filter { it.pos.x > cur.pos.x }
        // Connect fully to succeeding layers
        val currentProj = cur.pos.rotate(yaw, pitch).project(centerX, centerY, scale)
        
        nextLayerPoints.forEach { target ->
            // Only connect adjacent layer
            if (abs(target.pos.x - cur.pos.x) < 130f) {
                val targetProj = target.pos.rotate(yaw, pitch).project(centerX, centerY, scale)
                val connectionAlpha = 0.12f * min(currentProj.scale, targetProj.scale)
                
                drawLine(
                    color = colorPulse.copy(alpha = connectionAlpha),
                    start = Offset(currentProj.x, currentProj.y),
                    end = Offset(targetProj.x, targetProj.y),
                    strokeWidth = 1.5f
                )
                
                // Signal flow particles
                val pulseRatio = ((time * 0.15f) + idx * 0.25f) % 1f
                val fx = cur.pos.x + (target.pos.x - cur.pos.x) * pulseRatio
                val fy = cur.pos.y + (target.pos.y - cur.pos.y) * pulseRatio
                val fz = cur.pos.z + (target.pos.z - cur.pos.z) * pulseRatio
                
                val fproj = Vector3D(fx, fy, fz).rotate(yaw, pitch).project(centerX, centerY, scale)
                drawCircle(
                    color = PureWhite.copy(alpha = 0.7f * fproj.scale),
                    radius = 1.8f * fproj.scale,
                    center = Offset(fproj.x, fproj.y)
                )
            }
        }
    }
    
    // 2. Draw synapse glowing node bodies
    points.forEach { neuron ->
        val proj = neuron.pos.rotate(yaw, pitch).project(centerX, centerY, scale)
        val alphaScale = proj.scale
        
        drawCircle(
            color = colorPrimary.copy(alpha = 0.2f + 0.8f * neuron.charge),
            radius = 7.5f * alphaScale,
            center = Offset(proj.x, proj.y)
        )
        
        drawCircle(
            color = PureWhite.copy(alpha = alphaScale),
            radius = 3.5f * alphaScale,
            center = Offset(proj.x, proj.y)
        )
    }
}

private fun DrawScope.drawGraphMesh(
    centerX: Float, centerY: Float,
    scale: Float, yaw: Float, pitch: Float,
    time: Float
) {
    // Dynamic distributed neural or network web graphs (Holographic mesh)
    val nodesCount = 14
    val connectionsMaxDistance = 90f
    
    val nodes = mutableListOf<Vector3D>()
    for (i in 0 until nodesCount) {
        val angle = (i.toFloat() / nodesCount) * 6.28f
        val x = cos(angle) * 110f + sin(time * 0.02f + i) * 15f
        val y = sin(angle) * 70f + cos(time * 0.03f + i) * 10f
        val z = sin(time * 0.04f + i * 2) * 45f
        nodes.add(Vector3D(x, y, z))
    }
    
    // Draw links
    for (i in 0 until nodesCount) {
        val nodeA = nodes[i]
        val projA = nodeA.rotate(yaw, pitch).project(centerX, centerY, scale)
        for (j in (i + 1) until nodesCount) {
            val nodeB = nodes[j]
            val dist = sqrt((nodeA.x - nodeB.x) * (nodeA.x - nodeB.x) + (nodeA.y - nodeB.y) * (nodeA.y - nodeB.y) + (nodeA.z - nodeB.z) * (nodeA.z - nodeB.z))
            if (dist < connectionsMaxDistance) {
                val projB = nodeB.rotate(yaw, pitch).project(centerX, centerY, scale)
                val alpha = (1f - (dist / connectionsMaxDistance)) * 0.25f
                drawLine(
                    color = QuantumCyan.copy(alpha = alpha),
                    start = Offset(projA.x, projA.y),
                    end = Offset(projB.x, projB.y),
                    strokeWidth = 1.5f
                )
            }
        }
    }
    
    // Draw core nodes
    nodes.forEach { node ->
        val proj = node.rotate(yaw, pitch).project(centerX, centerY, scale)
        drawCircle(
            color = QuantumCyan.copy(alpha = 0.8f * proj.scale),
            radius = 4.5f * proj.scale,
            center = Offset(proj.x, proj.y)
        )
    }
}

private fun DrawScope.drawMobiusStrip(
    centerX: Float, centerY: Float,
    scale: Float, yaw: Float, pitch: Float,
    time: Float
) {
    val uSteps = 45
    val vSteps = 10
    
    // Parametric coordinates mapping:
    // x = (1 + v/2 * cos(u/2)) * cos(u)
    // y = (1 + v/2 * cos(u/2)) * sin(u)
    // z = v/2 * sin(u/2)
    for (i in 0..uSteps) {
        val u = (i.toFloat() / uSteps) * 6.28f
        for (j in 0..vSteps) {
            val v = (j.toFloat() / vSteps) * 2f - 1f // Range [-1, 1]
            val r = 80f // radius scale
            
            val offsetRadius = 1f + (v * 0.35f * cos(u / 2f))
            val x = r * offsetRadius * cos(u)
            val y = r * offsetRadius * sin(u)
            val z = r * v * 0.35f * sin(u / 2f)
            
            val proj = Vector3D(x, y, z).rotate(yaw, pitch).project(centerX, centerY, scale)
            val ratio = i.toFloat() / uSteps
            val color = Color.interpolate(QuantumCyan, MathPurple, ratio)
            
            drawCircle(
                color = color.copy(alpha = 0.4f * proj.scale),
                radius = 1.5f * proj.scale,
                center = Offset(proj.x, proj.y)
            )
        }
    }
}

private fun DrawScope.drawLorenzAttractor(
    centerX: Float, centerY: Float,
    scale: Float, yaw: Float, pitch: Float,
    time: Float
) {
    // Lorenz chaotic attractor numerical integrating lines trail
    // s=10, r=28, b=8/3
    val sigma = 10.0
    val rho = 28.0
    val beta = 8.0 / 3.0
    
    var cx = 0.1
    var cy = 0.0
    var cz = 0.0
    val dt = 0.012
    
    var prevP: Point2D? = null
    val trailPoints = 110
    
    for (i in 0 until trailPoints) {
        val dx = sigma * (cy - cx)
        val dy = cx * (rho - cz) - cy
        val dz = cx * cy - beta * cz
        
        cx += dx * dt
        cy += dy * dt
        cz += dz * dt
        
        // Render attractor centered coordinates
        val xVal = (cx * 4f).toFloat()
        val yVal = ((cy * 4f) - 60f).toFloat() // offset y
        val zVal = ((cz - 25f) * 4f).toFloat()
        
        val proj = Vector3D(xVal, yVal, zVal).rotate(yaw, pitch).project(centerX, centerY, scale)
        
        if (prevP != null) {
            val tRatio = i.toFloat() / trailPoints
            drawLine(
                color = Color.interpolate(StellarCoral, QuantumCyan, tRatio).copy(alpha = 0.8f * proj.scale),
                start = Offset(prevP.x, prevP.y),
                end = Offset(proj.x, proj.y),
                strokeWidth = 2f * proj.scale
            )
        }
        prevP = proj
    }
}

private fun DrawScope.drawHistoricalTimescales(
    centerX: Float, centerY: Float,
    scale: Float, yaw: Float, pitch: Float,
    time: Float,
    epoch: String
) {
    val bronzeColor = Color(0xFFCD7F32)
    val egyptColor = Color(0xFFE2C56F)
    val romanColor = Color(0xFFA52A2A)
    val industrialColor = Color(0xFF708090)
    val spaceColor = CosmicSlate
    
    val activeColor = when (epoch) {
        "Stone Age" -> bronzeColor
        "Ancient Egypt" -> egyptColor
        "Roman Empire" -> romanColor
        "Industrial Rev" -> industrialColor
        "Space Age" -> spaceColor
        else -> PureWhite
    }
    
    // Draw 3D Time grid timeline axle
    drawLine(
        color = activeColor.copy(alpha = 0.2f),
        start = Offset(centerX - 150f * scale, centerY),
        end = Offset(centerX + 150f * scale, centerY),
        strokeWidth = 2f
    )
    
    when (epoch) {
        "Stone Age" -> {
            // Stonehenge ring model
            val pillars = 12
            for (i in 0 until pillars) {
                val angle = (i.toFloat() / pillars) * 6.28f
                val x = cos(angle) * 70f
                val z = sin(angle) * 70f
                
                // Draw 3D pillar lines
                val base = Vector3D(x, 25f, z).rotate(yaw, pitch).project(centerX, centerY, scale)
                val top = Vector3D(x, -25f, z).rotate(yaw, pitch).project(centerX, centerY, scale)
                
                drawLine(color = activeColor.copy(alpha = 0.8f), start = Offset(base.x, base.y), end = Offset(top.x, top.y), strokeWidth = 5f * base.scale)
                
                // Connection caps lintels
                val nextAngle = ((i + 1).toFloat() / pillars) * 6.28f
                val nextTop = Vector3D(cos(nextAngle) * 70f, -25f, sin(nextAngle) * 70f).rotate(yaw, pitch).project(centerX, centerY, scale)
                drawLine(color = activeColor.copy(alpha = 0.5f), start = Offset(top.x, top.y), end = Offset(nextTop.x, nextTop.y), strokeWidth = 3f * top.scale)
            }
        }
        "Ancient Egypt" -> {
            // 3D Obelisk / wireframe Pyramid with dynamic shadow lines
            val pyramidNodes = listOf(
                Vector3D(0f, -50f, 0f),       // Tip
                Vector3D(-60f, 40f, -60f),    // Base corners
                Vector3D(60f, 40f, -60f),
                Vector3D(60f, 40f, 60f),
                Vector3D(-60f, 40f, 60f)
            )
            val projected = pyramidNodes.map { it.rotate(yaw, pitch).project(centerX, centerY, scale) }
            
            // Draw four sloped edge borders
            for (i in 1..4) {
                drawLine(color = egyptColor, start = Offset(projected[0].x, projected[0].y), end = Offset(projected[i].x, projected[i].y), strokeWidth = 3f * projected[i].scale)
            }
            // Draw base
            for (i in 1..4) {
                val next = if (i == 4) 1 else i + 1
                drawLine(color = egyptColor.copy(alpha = 0.5f), start = Offset(projected[i].x, projected[i].y), end = Offset(projected[next].x, projected[next].y), strokeWidth = 2f)
            }
        }
        "Roman Empire" -> {
            // Roman amphitheater ruins (arcs circles)
            val archesCount = 14
            for (level in 0..1) {
                val y = -35f + level * 35f
                val radius = 75f - level * 10f
                for (i in 0 until archesCount) {
                    val angle = (i.toFloat() / archesCount) * 6.28f
                    val x = cos(angle) * radius
                    val z = sin(angle) * radius
                    val proj = Vector3D(x, y, z).rotate(yaw, pitch).project(centerX, centerY, scale)
                    
                    drawCircle(color = romanColor.copy(alpha = 0.7f), radius = 6f * proj.scale, center = Offset(proj.x, proj.y))
                    
                    // Column links Down
                    val baseProj = Vector3D(x, y + 35f, z).rotate(yaw, pitch).project(centerX, centerY, scale)
                    drawLine(color = romanColor.copy(alpha = 0.4f), start = Offset(proj.x, proj.y), end = Offset(baseProj.x, baseProj.y), strokeWidth = 2f)
                }
            }
        }
        "Industrial Rev" -> {
            // Rotating interlinked steam gears
            val r1 = 50f
            val r2 = 30f
            
            // Gear 1 (Center)
            drawGearAt(centerX, centerY, scale, yaw, pitch, time, r1, Vector3D(0f, 0f, 0f), industrialColor)
            // Gear 2 (Interlinked offset)
            val angleOffset = Math.toRadians(time.toDouble()).toFloat() * 0.5f
            drawGearAt(centerX, centerY, scale, yaw, pitch, -time * 1.6f, r2, Vector3D(r1 + r2, 0f, 0f), StellarCoral)
        }
        "Space Age" -> {
            // Solar-panels Space station orbiting Earth globes
            // Draw central globe
            drawCircle(color = LaserBlue, radius = 25f * scale, center = Offset(centerX, centerY))
            drawCircle(color = BioGreen.copy(alpha = 0.4f), radius = 25f * scale, center = Offset(centerX - 4f, centerY - 2f))
            
            // Orbital space ring
            val tAngle = Math.toRadians(time.toDouble()).toFloat()
            val sx = cos(tAngle) * 90f
            val sz = sin(tAngle) * 90f
            val sy = -15f
            
            // Draw Orbit path line ellipse
            var lastP: Point2D? = null
            for (step in 0..30) {
                val alpha = (step.toFloat() / 30f) * 6.28f
                val p = Vector3D(cos(alpha) * 90f, sy, sin(alpha) * 90f).rotate(yaw, pitch).project(centerX, centerY, scale)
                if (lastP != null) {
                    drawLine(color = Color.White.copy(alpha = 0.15f), start = Offset(lastP.x, lastP.y), end = Offset(p.x, p.y), strokeWidth = 1f)
                }
                lastP = p
            }
            
            // Space station vector node points
            val boxCenter = Vector3D(sx, sy, sz).rotate(yaw, pitch).project(centerX, centerY, scale)
            drawRect(
                color = PureWhite.copy(alpha = 0.9f),
                topLeft = Offset(boxCenter.x - 8f * boxCenter.scale, boxCenter.y - 4f * boxCenter.scale),
                size = androidx.compose.ui.geometry.Size(16f * boxCenter.scale, 8f * boxCenter.scale)
            )
            
            // Panel wings lists
            val w1 = Vector3D(sx - 20f, sy, sz - 5f).rotate(yaw, pitch).project(centerX, centerY, scale)
            val w2 = Vector3D(sx + 20f, sy, sz + 5f).rotate(yaw, pitch).project(centerX, centerY, scale)
            drawLine(color = QuantumCyan, start = Offset(w1.x, w1.y), end = Offset(w2.x, w2.y), strokeWidth = 3f * boxCenter.scale)
        }
    }
}

// Helper to draw a gear structure in 3D projection space
private fun DrawScope.drawGearAt(
    centerX: Float, centerY: Float,
    scale: Float, yaw: Float, pitch: Float,
    rotationTick: Float,
    radius: Float,
    posOffset: Vector3D,
    color: Color
) {
    val teeth = 8
    val radOffset = Math.toRadians(rotationTick.toDouble()).toFloat()
    
    // Draw center core
    val coreProj = posOffset.rotate(yaw, pitch).project(centerX, centerY, scale)
    drawCircle(color = color.copy(alpha = 0.3f), radius = radius * coreProj.scale, center = Offset(coreProj.x, coreProj.y))
    
    for (i in 0 until teeth) {
        val angle = (i.toFloat() / teeth) * 6.28f + radOffset
        val xInner = posOffset.x + cos(angle) * (radius - 5f)
        val zInner = posOffset.z + sin(angle) * (radius - 5f)
        
        val xOuter = posOffset.x + cos(angle) * (radius + 10f)
        val zOuter = posOffset.z + sin(angle) * (radius + 10f)
        
        val innerProj = Vector3D(xInner, posOffset.y, zInner).rotate(yaw, pitch).project(centerX, centerY, scale)
        val outerProj = Vector3D(xOuter, posOffset.y, zOuter).rotate(yaw, pitch).project(centerX, centerY, scale)
        
        drawLine(
            color = color,
            start = Offset(innerProj.x, innerProj.y),
            end = Offset(outerProj.x, outerProj.y),
            strokeWidth = 3f * outerProj.scale
        )
    }
}

private fun DrawScope.drawPhilosophicalIdeaGraphs(
    centerX: Float, centerY: Float,
    scale: Float, yaw: Float, pitch: Float,
    time: Float,
    school: String
) {
    // 3D conceptual maps centering diverging thought flows
    val core = Vector3D(0f, 0f, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
    
    val concepts = when (school) {
        "Stoicism" -> listOf("Virtue", "Nature", "Logos", "Control Threshold")
        "Existentialism" -> listOf("Choice", "Absurd", "Freedom", "Being")
        "Rationalism" -> listOf("Deduction", "Logic", "Intellect", "Infinite")
        "Nihilism" -> listOf("No Meaning", "Void", "Skepticism", "Chaos")
        else -> listOf("Abstract", "Idea", "Mind")
    }
    
    drawCircle(color = PureWhite, radius = 8f * core.scale, center = Offset(core.x, core.y))
    
    concepts.forEachIndexed { i, label ->
        val rotAngle = (i.toFloat() / concepts.size) * 6.28f + Math.toRadians(time.toDouble()).toFloat() * 0.3f
        val x = cos(rotAngle) * 90f
        val z = sin(rotAngle) * 90f
        val y = sin((rotAngle * 3f)) * 15f
        
        val proj = Vector3D(x, y, z).rotate(yaw, pitch).project(centerX, centerY, scale)
        
        // Dynamic pulsating connection line
        drawLine(
            color = QuantumCyan.copy(alpha = 0.4f * proj.scale),
            start = Offset(core.x, core.y),
            end = Offset(proj.x, proj.y),
            strokeWidth = 2f
        )
        
        drawCircle(
            color = MathPurple.copy(alpha = 0.8f * proj.scale),
            radius = 16f * proj.scale,
            center = Offset(proj.x, proj.y)
        )
        
        drawCircle(
            color = Color.White.copy(alpha = 0.3f * proj.scale),
            radius = 11f * proj.scale,
            center = Offset(proj.x, proj.y),
            style = Stroke(width = 1f)
        )
    }
}

private fun DrawScope.drawCyberSecurityThreatSandbox(
    centerX: Float, centerY: Float,
    scale: Float, yaw: Float, pitch: Float,
    time: Float,
    safeguard: String
) {
    val matrixColor = Color(0xFF00FF00) // Terminal green hacker glow
    val warningRed = Color(0xFFFF3333)
    val cyberBlue = Color(0xFF1E90FF)
    
    when (safeguard) {
        "Firewall Shield" -> {
            // Draw outer shield cellular wall honeycomb arc
            for (cell in -3..3) {
                val angle = cell * 0.4f
                val hHeight = 50f
                val x = cos(angle) * 100f
                val z = sin(angle) * 100f
                
                val base = Vector3D(x, hHeight, z).rotate(yaw, pitch).project(centerX, centerY, scale)
                val top = Vector3D(x, -hHeight, z).rotate(yaw, pitch).project(centerX, centerY, scale)
                
                drawLine(color = cyberBlue.copy(alpha = 0.7f), start = Offset(base.x, base.y), end = Offset(top.x, top.y), strokeWidth = 3f * base.scale)
                
                // Horizontal links
                if (cell < 3) {
                    val nextAngle = (cell + 1) * 0.4f
                    val nextTop = Vector3D(cos(nextAngle) * 100f, -hHeight, sin(nextAngle) * 100f).rotate(yaw, pitch).project(centerX, centerY, scale)
                    val nextBase = Vector3D(cos(nextAngle) * 100f, hHeight, sin(nextAngle) * 100f).rotate(yaw, pitch).project(centerX, centerY, scale)
                    drawLine(color = cyberBlue.copy(alpha = 0.5f), start = Offset(top.x, top.y), end = Offset(nextTop.x, nextTop.y), strokeWidth = 1.5f)
                    drawLine(color = cyberBlue.copy(alpha = 0.5f), start = Offset(base.x, base.y), end = Offset(nextBase.x, nextBase.y), strokeWidth = 1.5f)
                }
            }
            
            // Draw central protected core database cylindrical server node
            val coreY = sin(time * 0.08f) * 10f
            val serverProj = Vector3D(0f, coreY, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
            drawCircle(color = matrixColor, radius = 18f * serverProj.scale, center = Offset(serverProj.x, serverProj.y))
        }
        "Brute Force" -> {
            // Central core server node being battered by swarm of attacking packets rotating and crashing
            val server = Vector3D(0f, 0f, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
            
            // Server core database
            drawCircle(color = warningRed.copy(alpha = 0.3f), radius = (25f + sin(time * 0.15f) * 5f) * server.scale, center = Offset(server.x, server.y))
            drawCircle(color = matrixColor, radius = 10f * server.scale, center = Offset(server.x, server.y))
            
            // Swarm attackers
            val swarm = 14
            for (i in 0 until swarm) {
                val tAngle = (i.toFloat() / swarm) * 6.28f + (time * 0.05f)
                val dist = 110f - ((time * 0.8f + i * 15f) % 90f) // spiral inwards
                
                val sx = cos(tAngle) * dist
                val sz = sin(tAngle) * dist
                val sy = (i - swarm/2f) * 6f
                
                val attacker = Vector3D(sx, sy, sz).rotate(yaw, pitch).project(centerX, centerY, scale)
                drawCircle(color = warningRed, radius = 3f * attacker.scale, center = Offset(attacker.x, attacker.y))
                
                // Small trail towards center
                val tail = Vector3D(cos(tAngle) * (dist + 12f), sy, sin(tAngle) * (dist + 12f)).rotate(yaw, pitch).project(centerX, centerY, scale)
                drawLine(color = warningRed.copy(alpha = 0.3f), start = Offset(attacker.x, attacker.y), end = Offset(tail.x, tail.y), strokeWidth = 1f)
            }
        }
        "Packet Sniffer" -> {
            // Complex matrix network graph showing intercept sensor listening to signal line
            val source = Vector3D(-110f, 0f, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
            val destination = Vector3D(110f, 0f, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
            
            // Core line
            drawLine(color = PureWhite.copy(alpha = 0.2f), start = Offset(source.x, source.y), end = Offset(destination.x, destination.y), strokeWidth = 2f)
            
            // Intercepting wiretap probe tap
            val interceptionTapX = 15f
            val interceptProbe = Vector3D(interceptionTapX, -65f, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
            val tapCenter = Vector3D(interceptionTapX, 0f, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
            
            drawLine(color = warningRed, start = Offset(interceptProbe.x, interceptProbe.y), end = Offset(tapCenter.x, tapCenter.y), strokeWidth = 1.5f)
            drawCircle(color = warningRed, radius = 6f * interceptProbe.scale, center = Offset(interceptProbe.x, interceptProbe.y))
            
            // Draw flowing packets
            val packets = 3
            for (p in 0 until packets) {
                val ratio = ((time * 0.08f) + p * 0.33f) % 1f
                val px = -110f + 220f * ratio
                val py = 0f
                val pz = 0f
                
                val packetProj = Vector3D(px, py, pz).rotate(yaw, pitch).project(centerX, centerY, scale)
                drawCircle(color = matrixColor, radius = 4f * packetProj.scale, center = Offset(packetProj.x, packetProj.y))
            }
        }
        "SQL Injection" -> {
            // Database blocks layout showing injection characters attacking cell grid structure
            for (row in -1..1) {
                for (col in -1..1) {
                    val sx = col * 45f
                    val sy = row * 35f
                    val databaseCell = Vector3D(sx, sy, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
                    
                    val isCorrupted = col == 1 && row == 0
                    drawRect(
                        color = if (isCorrupted) warningRed.copy(alpha = 0.5f) else cyberBlue.copy(alpha = 0.25f),
                        topLeft = Offset(databaseCell.x - 14f * databaseCell.scale, databaseCell.y - 10f * databaseCell.scale),
                        size = androidx.compose.ui.geometry.Size(28f * databaseCell.scale, 20f * databaseCell.scale),
                        style = Stroke(width = 1.5f)
                    )
                    
                    // Code characters injected flows
                    if (isCorrupted) {
                        val attackerAngle = time * 0.1f
                        val injectionDot = Vector3D(sx + cos(attackerAngle) * 20f, sy + sin(attackerAngle) * 20f, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
                        drawCircle(color = warningRed, radius = 3.5f * injectionDot.scale, center = Offset(injectionDot.x, injectionDot.y))
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawTheologicalAuraCompass(
    centerX: Float, centerY: Float,
    scale: Float, yaw: Float, pitch: Float,
    time: Float,
    symbol: String
) {
    val auraColor = Color(0xFFBA55D3) // Threology orchid color glow
    val goldColor = Color(0xFFE5C158)
    
    when (symbol) {
        "Dharma Wheel" -> {
            // 8-spoke wheel in 3D rotation space
            val ringCenter = Vector3D(0f, 0f, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
            drawCircle(color = goldColor.copy(alpha = 0.15f), radius = 80f * ringCenter.scale, center = Offset(ringCenter.x, ringCenter.y))
            
            // Outer rim
            var prevP: Point2D? = null
            val divisions = 24
            for (i in 0..divisions) {
                val angle = (i.toFloat() / divisions) * 6.28f + Math.toRadians(time.toDouble()).toFloat() * 0.15f
                val x = cos(angle) * 80f
                val z = sin(angle) * 80f
                val proj = Vector3D(x, 0f, z).rotate(yaw, pitch).project(centerX, centerY, scale)
                
                if (prevP != null) {
                    drawLine(color = goldColor, start = Offset(prevP.x, prevP.y), end = Offset(proj.x, proj.y), strokeWidth = 3f * proj.scale)
                }
                prevP = proj
            }
            
            // Spokes
            for (spoke in 0 until 8) {
                val angle = (spoke.toFloat() / 8f) * 6.28f + Math.toRadians(time.toDouble()).toFloat() * 0.15f
                val x = cos(angle) * 80f
                val z = sin(angle) * 80f
                val rimProj = Vector3D(x, 0f, z).rotate(yaw, pitch).project(centerX, centerY, scale)
                drawLine(color = goldColor.copy(alpha = 0.7f), start = Offset(ringCenter.x, ringCenter.y), end = Offset(rimProj.x, rimProj.y), strokeWidth = 2f * ringCenter.scale)
            }
            drawCircle(color = goldColor, radius = 9f * ringCenter.scale, center = Offset(ringCenter.x, ringCenter.y))
        }
        "Taoist Yin-Yang" -> {
            val center = Vector3D(0f, 0f, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
            val radAngle = Math.toRadians(-time.toDouble() * 0.6).toFloat()
            val radiusWheel = 75f
            
            // Yin half shaded circle
            // Drawing simple projection circles relative to orientation
            drawCircle(color = Color.White.copy(alpha = 0.12f), radius = radiusWheel * center.scale, center = Offset(center.x, center.y))
            
            // Yin and yang dual dynamic center orbits
            val yinOrbit = Vector3D(cos(radAngle) * 35f, 0f, sin(radAngle) * 35f).rotate(yaw, pitch).project(centerX, centerY, scale)
            val yangOrbit = Vector3D(cos(radAngle + 3.14f) * 35f, 0f, sin(radAngle + 3.14f) * 35f).rotate(yaw, pitch).project(centerX, centerY, scale)
            
            drawCircle(color = PureWhite, radius = 15f * yinOrbit.scale, center = Offset(yinOrbit.x, yinOrbit.y))
            drawCircle(color = MidnightBlack, radius = 15f * yangOrbit.scale, center = Offset(yangOrbit.x, yangOrbit.y))
            
            // Dots opposite
            drawCircle(color = MidnightBlack, radius = 4f * yinOrbit.scale, center = Offset(yinOrbit.x, yinOrbit.y))
            drawCircle(color = PureWhite, radius = 4f * yangOrbit.scale, center = Offset(yangOrbit.x, yangOrbit.y))
        }
        "Cross & Crescent" -> {
            // Crescent and cross visual overlaps in 3D space
            // Cross vertical
            val center = Vector3D(0f, 0f, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
            
            val tTop = Vector3D(0f, -60f, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
            val tBase = Vector3D(0f, 60f, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
            val tLeft = Vector3D(-35f, -20f, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
            val tRight = Vector3D(35f, -20f, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
            
            // Drawing elegant Latin style cross coordinates alignment
            drawLine(color = goldColor, start = Offset(tTop.x, tTop.y), end = Offset(tBase.x, tBase.y), strokeWidth = 3f * center.scale)
            drawLine(color = goldColor, start = Offset(tLeft.x, tLeft.y), end = Offset(tRight.x, tRight.y), strokeWidth = 3f * center.scale)
            
            // Radiant crescent orbiting cross
            val crescentAngle = Math.toRadians(time.toDouble()).toFloat() * 1.2f
            val radCrescent = 65f
            val cx = cos(crescentAngle) * radCrescent
            val cz = sin(crescentAngle) * radCrescent
            val moonCenter = Vector3D(cx, 0f, cz).rotate(yaw, pitch).project(centerX, centerY, scale)
            
            drawCircle(color = PureWhite.copy(alpha = 0.8f * moonCenter.scale), radius = 11f * moonCenter.scale, center = Offset(moonCenter.x, moonCenter.y))
            // Inverse shadow mask crescent curve
            drawCircle(color = MidnightBlack, radius = 9f * moonCenter.scale, center = Offset(moonCenter.x - 3f * moonCenter.scale, moonCenter.y - 1f * moonCenter.scale))
        }
        "Aura Mandala" -> {
            // Multiple concentric polygon rotating vectors
            val pCenter = Vector3D(0f, 0f, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
            val nestedLayers = 4
            for (level in 1..nestedLayers) {
                val nodesCount = LevelMapping(level)
                val vertexRadius = level * 25f
                val rotAngle = (level * 3.14f / 4f) + (time * 0.05f * (nestedLayers - level))
                
                var lastProj: Point2D? = null
                for (v in 0..nodesCount) {
                    val vertexAngle = (v.toFloat() / nodesCount) * 6.28f + rotAngle
                    val x = cos(vertexAngle) * vertexRadius
                    val z = sin(vertexAngle) * vertexRadius
                    val proj = Vector3D(x, 0f, z).rotate(yaw, pitch).project(centerX, centerY, scale)
                    
                    if (lastProj != null) {
                        drawLine(
                            color = auraColor.safeCopy((1f / level) * proj.scale),
                            start = Offset(lastProj.x, lastProj.y),
                            end = Offset(proj.x, proj.y),
                            strokeWidth = 1.5f
                        )
                    }
                    lastProj = proj
                }
            }
        }
    }
}

// Maps level matrix coordinates
private fun LevelMapping(level: Int): Int {
    return when (level) {
        1 -> 4
        2 -> 6
        3 -> 8
        4 -> 12
        else -> 6
    }
}

private fun DrawScope.drawThermodynamicsEntropy(
    centerX: Float, centerY: Float,
    scale: Float, yaw: Float, pitch: Float,
    time: Float
) {
    // Draw 3D-angled container box boundary limits
    val side = 80f
    val corners = listOf(
        Vector3D(-side, -side, -side),
        Vector3D(side, -side, -side),
        Vector3D(side, side, -side),
        Vector3D(-side, side, -side),
        Vector3D(-side, -side, side),
        Vector3D(side, -side, side),
        Vector3D(side, side, side),
        Vector3D(-side, side, side)
    ).map { it.rotate(yaw, pitch).project(centerX, centerY, scale) }

    // Connect the box wireframe boundaries
    val boxLines = listOf(
        0 to 1, 1 to 2, 2 to 3, 3 to 0, // front
        4 to 5, 5 to 6, 6 to 7, 7 to 4, // back
        0 to 4, 1 to 5, 2 to 6, 3 to 7  // depth links
    )
    boxLines.forEach { (first, second) ->
        drawLine(
            color = CosmicSlate.copy(alpha = 0.35f),
            start = Offset(corners[first].x, corners[first].y),
            end = Offset(corners[second].x, corners[second].y),
            strokeWidth = 1f
        )
    }

    // Draw 15 bouncing/moving gas atom spheres that bounce inside
    for (i in 1..15) {
        // Pseudo-random deterministic movement paths using indices
        val phaseX = (i * 1.7f)
        val phaseY = (i * 2.3f)
        val phaseZ = (i * 3.1f)
        val x = sin(time * 0.08f + phaseX) * (side - 10f)
        val y = cos(time * 0.11f + phaseY) * (side - 10f)
        val z = sin(time * 0.05f + phaseZ) * (side - 10f)

        val proj = Vector3D(x, y, z).rotate(yaw, pitch).project(centerX, centerY, scale)
        // High temp particles are red/orange, cold particles are cyan/blue
        val particleColor = if (i % 2 == 0) StellarCoral else QuantumCyan

        // Glowing atom sphere
        drawCircle(
            color = particleColor.safeCopy(0.8f * proj.scale),
            radius = 5f * proj.scale,
            center = Offset(proj.x, proj.y)
        )
        // Motion tail tracking trace
        val tail = Vector3D(
            sin((time - 3f) * 0.08f + phaseX) * (side - 10f),
            cos((time - 3f) * 0.11f + phaseY) * (side - 10f),
            sin((time - 3f) * 0.05f + phaseZ) * (side - 10f)
        ).rotate(yaw, pitch).project(centerX, centerY, scale)

        drawLine(
            color = particleColor.copy(alpha = 0.25f),
            start = Offset(proj.x, proj.y),
            end = Offset(tail.x, tail.y),
            strokeWidth = 1.5f
        )
    }
}

private fun DrawScope.drawElectromagneticInduction(
    centerX: Float, centerY: Float,
    scale: Float, yaw: Float, pitch: Float,
    time: Float
) {
    // 1. Draw solid copper induction coil (spiral along Z axis)
    val coils = 5
    val pointsPerCoil = 24
    val totalPoints = coils * pointsPerCoil
    var prevP: Point2D? = null
    for (i in 0..totalPoints) {
        val fraction = i.toFloat() / totalPoints
        val angle = fraction * coils * 6.28f
        val x = cos(angle) * 45f
        val y = sin(angle) * 45f
        val z = -80f + fraction * 160f // distributed along length helically

        val proj = Vector3D(x, y, z).rotate(yaw, pitch).project(centerX, centerY, scale)
        if (prevP != null) {
            drawLine(
                color = ChemAmber.safeCopy(0.8f * proj.scale),
                start = Offset(prevP.x, prevP.y),
                end = Offset(proj.x, proj.y),
                strokeWidth = 3f * proj.scale
            )
        }
        prevP = proj
    }

    // 2. Draw moving bar magnet (oscillating back and forth along Z)
    val magZ = sin(time * 0.05f) * 110f
    val magN = Vector3D(0f, 0f, magZ - 30f).rotate(yaw, pitch).project(centerX, centerY, scale)
    val magS = Vector3D(0f, 0f, magZ + 30f).rotate(yaw, pitch).project(centerX, centerY, scale)

    // Red North poles
    drawLine(
        color = StellarCoral,
        start = Offset(magN.x, magN.y),
        end = Offset((magN.x + magS.x)/2f, (magN.y + magS.y)/2f),
        strokeWidth = 8f * magN.scale
    )
    // Blue South poles
    drawLine(
        color = LaserBlue,
        start = Offset((magN.x + magS.x)/2f, (magN.y + magS.y)/2f),
        end = Offset(magS.x, magS.y),
        strokeWidth = 8f * magS.scale
    )

    // Center identifiers
    drawCircle(color = PureWhite, radius = 4f * magN.scale, center = Offset(magN.x, magN.y))
    drawCircle(color = PureWhite, radius = 4f * magS.scale, center = Offset(magS.x, magS.y))

    // 3. Draw magnetic field lines looping from North to South
    val magneticLines = 6
    for (line in 0 until magneticLines) {
        val angle = (line.toFloat() / magneticLines) * 6.28f
        var lastLF: Point2D? = null
        val steps = 20
        for (step in 0..steps) {
            val ratio = step.toFloat() / steps
            val theta = ratio * 3.14f
            val loopRadX = sin(theta) * 60f * cos(angle)
            val loopRadY = sin(theta) * 60f * sin(angle)
            val loopZ = magZ - 30f + cos(theta) * 60f

            val lfProj = Vector3D(loopRadX, loopRadY, loopZ).rotate(yaw, pitch).project(centerX, centerY, scale)
            if (lastLF != null) {
                drawLine(
                    color = QuantumCyan.safeCopy(0.25f * lfProj.scale * sin(theta)),
                    start = Offset(lastLF.x, lastLF.y),
                    end = Offset(lfProj.x, lfProj.y),
                    strokeWidth = 1f
                )
            }
            lastLF = lfProj
        }
    }
}

private fun DrawScope.drawMitochondriaMembrane(
    centerX: Float, centerY: Float,
    scale: Float, yaw: Float, pitch: Float,
    time: Float
) {
    val outerRadX = 110f
    val outerRadY = 60f

    // Outer membrane shell wireframe
    val divisions = 30
    var lastOuter: Point2D? = null
    for (i in 0..divisions) {
        val angle = (i.toFloat() / divisions) * 6.28f
        val x = cos(angle) * outerRadX
        val y = sin(angle) * outerRadY

        val proj = Vector3D(x, y, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
        if (lastOuter != null) {
            drawLine(
                color = BioGreen.safeCopy(0.6f * proj.scale),
                start = Offset(lastOuter.x, lastOuter.y),
                end = Offset(proj.x, proj.y),
                strokeWidth = 2.5f * proj.scale
            )
        }
        lastOuter = proj
    }

    // Inner folded cristae membrane pattern
    var lastInner: Point2D? = null
    val innerDivisions = 60
    for (i in 0..innerDivisions) {
        val angle = (i.toFloat() / innerDivisions) * 6.28f
        val foldFactor = 1f + sin(angle * 12f + time * 0.1f) * 0.22f
        val x = cos(angle) * (outerRadX - 25f) * foldFactor
        val y = sin(angle) * (outerRadY - 15f) * foldFactor

        val proj = Vector3D(x, y, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
        if (lastInner != null) {
            drawLine(
                color = StellarCoral.safeCopy(0.8f * proj.scale),
                start = Offset(lastInner.x, lastInner.y),
                end = Offset(proj.x, proj.y),
                strokeWidth = 1.8f * proj.scale
            )
        }
        lastInner = proj
    }

    // ATP Synthase hubs floating inside
    for (i in 1..6) {
        val angle = i * (6.28f / 6f) + time * 0.02f
        val x = cos(angle) * 45f
        val y = sin(angle) * 25f

        val proj = Vector3D(x, y, 10f).rotate(yaw, pitch).project(centerX, centerY, scale)
        drawCircle(
            color = ChemAmber.safeCopy(0.9f * proj.scale),
            radius = 4f * proj.scale,
            center = Offset(proj.x, proj.y)
        )
        drawCircle(
            color = ChemAmber.copy(alpha = 0.35f),
            radius = 8f * proj.scale,
            center = Offset(proj.x, proj.y)
        )
    }
}

private fun DrawScope.drawMitosisSplitting(
    centerX: Float, centerY: Float,
    scale: Float, yaw: Float, pitch: Float,
    time: Float
) {
    val cycle = (time * 0.03f) % 6.28f
    val splitRatio = sin(cycle) * sin(cycle)
    val splitDist = splitRatio * 65f

    val c1 = Vector3D(-splitDist, 0f, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
    val c2 = Vector3D(splitDist, 0f, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)

    // Dumbbell constriction cell perimeter curve
    var lastP: Point2D? = null
    val steps = 36
    for (i in 0..steps) {
        val angle = (i.toFloat() / steps) * 6.28f
        val rx = cos(angle) * 80f
        val ry = sin(angle) * 55f

        val constriction = 1f - splitRatio * 0.55f * exp(-4f * (cos(angle) * cos(angle)))
        val x = rx * constriction
        val y = ry * constriction

        val proj = Vector3D(x, y, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
        if (lastP != null) {
            drawLine(
                color = BioGreen.copy(alpha = 0.7f),
                start = Offset(lastP.x, lastP.y),
                end = Offset(proj.x, proj.y),
                strokeWidth = 3f * proj.scale
            )
        }
        lastP = proj
    }

    // Spindle fibers
    if (splitRatio > 0.15f) {
        for (i in -3..3) {
            val fY = i * 15f
            val pA = Vector3D(-splitDist + 15f, fY, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
            val pB = Vector3D(splitDist - 15f, fY, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
            drawLine(
                color = QuantumCyan.copy(alpha = 0.25f * splitRatio),
                start = Offset(pA.x, pA.y),
                end = Offset(pB.x, pB.y),
                strokeWidth = 1f
            )
        }
    }

    // Dual chromosome sets splitting apart
    for (i in -1..1) {
        val yOff = i * 18f
        val chr1A = Vector3D(-splitDist - 5f, yOff - 8f, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
        val chr1B = Vector3D(-splitDist + 10f * (1f - splitRatio), yOff, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
        val chr2A = Vector3D(splitDist + 5f, yOff - 8f, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
        val chr2B = Vector3D(splitDist - 10f * (1f - splitRatio), yOff, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)

        drawLine(color = LaserBlue, start = Offset(chr1A.x, chr1A.y), end = Offset(chr1B.x, chr1B.y), strokeWidth = 2.5f)
        drawLine(color = LaserBlue, start = Offset(chr2A.x, chr2A.y), end = Offset(chr2B.x, chr2B.y), strokeWidth = 2.5f)
    }
}

private fun DrawScope.drawBlockchainConsensus(
    centerX: Float, centerY: Float,
    scale: Float, yaw: Float, pitch: Float,
    time: Float
) {
    val blocksCount = 3
    for (b in 0 until blocksCount) {
        val bOffsetZ = -100f + b * 100f
        val bOffsetY = sin(time * 0.08f + b * 2f) * 15f

        val side = 24f
        val corners = listOf(
            Vector3D(-side, -side + bOffsetY, -side + bOffsetZ),
            Vector3D(side, -side + bOffsetY, -side + bOffsetZ),
            Vector3D(side, side + bOffsetY, -side + bOffsetZ),
            Vector3D(-side, side + bOffsetY, -side + bOffsetZ),
            Vector3D(-side, -side + bOffsetY, side + bOffsetZ),
            Vector3D(side, -side + bOffsetY, side + bOffsetZ),
            Vector3D(side, side + bOffsetY, side + bOffsetZ),
            Vector3D(-side, side + bOffsetY, side + bOffsetZ)
        ).map { it.rotate(yaw, pitch).project(centerX, centerY, scale) }

        val boxLines = listOf(
            0 to 1, 1 to 2, 2 to 3, 3 to 0,
            4 to 5, 5 to 6, 6 to 7, 7 to 4,
            0 to 4, 1 to 5, 2 to 6, 3 to 7
        )
        boxLines.forEach { (first, second) ->
            drawLine(
                color = QuantumCyan.safeCopy(0.7f * corners[first].scale),
                start = Offset(corners[first].x, corners[first].y),
                end = Offset(corners[second].x, corners[second].y),
                strokeWidth = 2f
            )
        }

        // Consensus state core
        val ringVal = Vector3D(0f, bOffsetY, bOffsetZ).rotate(yaw, pitch).project(centerX, centerY, scale)
        drawCircle(
            color = BioGreen.safeCopy(0.8f * ringVal.scale),
            radius = 6f * ringVal.scale,
            center = Offset(ringVal.x, ringVal.y)
        )

        // Ledger chaining links
        if (b < blocksCount - 1) {
            val linkStart = Vector3D(0f, bOffsetY, bOffsetZ + side).rotate(yaw, pitch).project(centerX, centerY, scale)
            val nextOffsetY = sin(time * 0.08f + (b + 1) * 2f) * 15f
            val linkEnd = Vector3D(0f, nextOffsetY, bOffsetZ + 100f - side).rotate(yaw, pitch).project(centerX, centerY, scale)
            drawLine(
                color = LaserBlue,
                start = Offset(linkStart.x, linkStart.y),
                end = Offset(linkEnd.x, linkEnd.y),
                strokeWidth = 3f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), time * 0.5f)
            )
        }
    }
}

private fun DrawScope.drawBubbleSort(
    centerX: Float, centerY: Float,
    scale: Float, yaw: Float, pitch: Float,
    time: Float
) {
    val bars = 10
    val listHeights = listOf(45f, 75f, 25f, 90f, 60f, 35f, 80f, 15f, 70f, 50f)
    val compareIdx = ((time * 0.08f) % (bars - 1)).toInt()

    for (i in 0 until bars) {
        val xOffset = -100f + i * 22f
        val height = listHeights[i]

        val baseLeft = Vector3D(xOffset - 8f, 50f, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
        val baseRight = Vector3D(xOffset + 8f, 50f, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
        val topLeft = Vector3D(xOffset - 8f, 50f - height, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
        val topRight = Vector3D(xOffset + 8f, 50f - height, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)

        val isComparing = i == compareIdx || i == compareIdx + 1
        val barColor = if (isComparing) StellarCoral else QuantumCyan

        drawLine(color = barColor, start = Offset(baseLeft.x, baseLeft.y), end = Offset(baseRight.x, baseRight.y), strokeWidth = 2f)
        drawLine(color = barColor, start = Offset(topLeft.x, topLeft.y), end = Offset(topRight.x, topRight.y), strokeWidth = 2f)
        drawLine(color = barColor, start = Offset(baseLeft.x, baseLeft.y), end = Offset(topLeft.x, topLeft.y), strokeWidth = 2f)
        drawLine(color = barColor, start = Offset(baseRight.x, baseRight.y), end = Offset(topRight.x, topRight.y), strokeWidth = 2f)

        val fillCenter = Vector3D(xOffset, 50f - height / 2f, 0f).rotate(yaw, pitch).project(centerX, centerY, scale)
        drawCircle(
            color = barColor.copy(alpha = 0.25f),
            radius = 6f * fillCenter.scale,
            center = Offset(fillCenter.x, fillCenter.y)
        )
    }
}

private fun DrawScope.drawTorusRing(
    centerX: Float, centerY: Float,
    scale: Float, yaw: Float, pitch: Float,
    time: Float
) {
    val rMajor = 75f
    val rMinor = 22f
    val uSegments = 16
    val vSegments = 12

    for (i in 0..uSegments) {
        val u = (i.toFloat() / uSegments) * 6.28f + time * 0.02f
        val cosU = cos(u)
        val sinU = sin(u)

        var prevNode: Point2D? = null
        for (j in 0..vSegments) {
            val v = (j.toFloat() / vSegments) * 6.28f
            val cosV = cos(v)
            val sinV = sin(v)

            val x = (rMajor + rMinor * cosV) * cosU
            val y = rMinor * sinV
            val z = (rMajor + rMinor * cosV) * sinU

            val proj = Vector3D(x, y, z).rotate(yaw, pitch).project(centerX, centerY, scale)
            if (prevNode != null) {
                drawLine(
                    color = LaserBlue.safeCopy(0.45f * proj.scale),
                    start = Offset(prevNode.x, prevNode.y),
                    end = Offset(proj.x, proj.y),
                    strokeWidth = 1f
                )
            }
            prevNode = proj
        }
    }
}

private fun DrawScope.drawMandelbrotFractal(
    centerX: Float, centerY: Float,
    scale: Float, yaw: Float, pitch: Float,
    time: Float
) {
    val pointsCount = 60
    var lastP: Point2D? = null
    val rotationFactor = time * 0.03f

    for (i in 0 until pointsCount) {
        val tVal = i * 0.15f
        val zoomFactor = 10f + i * 1.5f
        val angle = tVal + rotationFactor

        val x = cos(angle) * zoomFactor
        val y = sin(angle) * zoomFactor
        val z = sin(tVal * 3f) * 20f

        val proj = Vector3D(x, z, y).rotate(yaw, pitch).project(centerX, centerY, scale)

        val colorVal = if (i % 2 == 0) MathPurple else QuantumCyan
        drawCircle(
            color = colorVal.safeCopy(0.7f * proj.scale),
            radius = (2f + sin(time * 0.1f + i) * 1f) * proj.scale,
            center = Offset(proj.x, proj.y)
        )

        if (lastP != null) {
            drawLine(
                color = colorVal.copy(alpha = 0.25f),
                start = Offset(lastP.x, lastP.y),
                end = Offset(proj.x, proj.y),
                strokeWidth = 1.2f
            )
        }
        lastP = proj
    }
}
