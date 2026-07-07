package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.SchoolViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: SchoolViewModel) {
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val students by viewModel.students.collectAsStateWithLifecycle()
    val deceCases by viewModel.deceCases.collectAsStateWithLifecycle()
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val userAccounts by viewModel.userAccounts.collectAsStateWithLifecycle()
    val rolePermissions by viewModel.rolePermissions.collectAsStateWithLifecycle()
    val activePermissions by viewModel.activePermissions.collectAsStateWithLifecycle()
    val isDarkModeState by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isDarkMode = isDarkModeState ?: isSystemInDarkTheme()

    val unreadNotificationsCount = notifications.count { !it.isRead }

    if (currentScreen == "Login") {
        LoginScreen(
            userAccounts = userAccounts,
            loginError = viewModel.loginError.collectAsStateWithLifecycle().value,
            onLogin = { u, p -> viewModel.loginUser(u, p) }
        )
    } else {
        Scaffold(
            topBar = {
                TopBarComponent(
                    currentRole = currentRole,
                    currentUser = currentUser,
                    syncState = syncState,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                    onLogout = { viewModel.logoutUser() },
                    onSyncToggle = { viewModel.toggleSync() }
                )
            },
            bottomBar = {
                BottomNavigationBarComponent(
                    currentScreen = currentScreen,
                    currentRole = currentRole,
                    unreadNotificationsCount = unreadNotificationsCount,
                    activePermissions = activePermissions,
                    onScreenSelect = { viewModel.setScreen(it) }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        "Dashboard" -> DashboardScreen(
                            currentRole = currentRole,
                            students = students,
                            activities = activities,
                            schedules = schedules,
                            deceCases = deceCases,
                            activePermissions = activePermissions,
                            onNavigate = { viewModel.setScreen(it) },
                            onToggleActivity = { viewModel.toggleActivityCompleted(it) },
                            onLogout = { viewModel.logoutUser() }
                        )
                        "Estudiantes" -> {
                            if (currentRole == "Administrador" || activePermissions?.canAccessStudents == true) {
                                StudentListScreen(
                                    currentRole = currentRole,
                                    students = students,
                                    onReportNovedad = { student, behavior, academic ->
                                        viewModel.reportStudentNovedad(student, behavior, academic)
                                    }
                                )
                            } else {
                                AccessDeniedScreen()
                            }
                        }
                        "DECE" -> {
                            if (currentRole == "Administrador" || activePermissions?.canAccessDECE == true) {
                                DeceManagementScreen(
                                    currentRole = currentRole,
                                    students = students,
                                    deceCases = deceCases,
                                    onCreateCase = { id, name, title, desc, prio ->
                                        viewModel.registerDeceCase(id, name, title, desc, prio)
                                    },
                                    onUpdateCase = { deceCase, status, notes ->
                                        viewModel.updateCaseStatus(deceCase, status, notes)
                                    }
                                )
                            } else {
                                AccessDeniedScreen()
                            }
                        }
                        "Chat" -> {
                            if (currentRole == "Administrador" || activePermissions?.canAccessChat == true) {
                                ChatScreen(
                                    currentRole = currentRole,
                                    chatHistory = chatHistory,
                                    onSendMessage = { text, recipient ->
                                        viewModel.sendChat(text, recipient)
                                    },
                                    onSendAttachment = { type, recipient ->
                                        viewModel.sendChat("Se envió un adjunto de tipo $type", recipient, attachmentType = type)
                                    }
                                )
                            } else {
                                AccessDeniedScreen()
                            }
                        }
                        "Notificaciones" -> {
                            if (currentRole == "Administrador" || activePermissions?.canAccessNotificaciones == true) {
                                NotificationsScreen(
                                    notifications = notifications,
                                    onMarkRead = { viewModel.markNotificationRead(it) }
                                )
                            } else {
                                AccessDeniedScreen()
                            }
                        }
                        "IA_Asistente" -> {
                            if (currentRole == "Administrador" || activePermissions?.canAccessIA == true) {
                                AiAssistantScreen(
                                    currentRole = currentRole,
                                    aiResponse = viewModel.aiResponse.collectAsStateWithLifecycle().value,
                                    aiIsLoading = viewModel.aiIsLoading.collectAsStateWithLifecycle().value,
                                    onAsk = { viewModel.askGemini(it) }
                                )
                            } else {
                                AccessDeniedScreen()
                            }
                        }
                        "AdminPanel" -> {
                            if (currentRole == "Administrador") {
                                AdminPanelScreen(
                                    userAccounts = userAccounts,
                                    rolePermissions = rolePermissions,
                                    onCreateUser = { u, p, f, r -> viewModel.createUserAccount(u, p, f, r) },
                                    onDeleteUser = { viewModel.removeUserAccount(it.username) },
                                    onUpdatePermission = { viewModel.updateRolePermission(it) }
                                )
                            } else {
                                AccessDeniedScreen()
                            }
                        }
                        "Identidad_Institucional" -> {
                            if (currentRole == "Administrador" || currentRole == "Autoridades" || currentRole == "Docente" || currentRole == "DECE") {
                                IdentidadInstitucionalScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                AccessDeniedScreen()
                            }
                        }
                        "Constructor_Plantillas" -> {
                            if (currentRole == "Administrador" || currentRole == "Autoridades" || currentRole == "Docente" || currentRole == "DECE") {
                                AITemplateBuilderScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                AccessDeniedScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================
// COMPONENT: Top Bar (App Identity, Role Selector, Sync Engine)
// ==========================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarComponent(
    currentRole: String,
    currentUser: UserAccount?,
    syncState: String,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onLogout: () -> Unit,
    onSyncToggle: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // App Logo and Name (Sleek Interface customized profile header)
                val initials = if (!currentUser?.fullName.isNullOrBlank()) {
                    val parts = currentUser!!.fullName.split(" ")
                    if (parts.size >= 2) {
                        "${parts[0].firstOrNull() ?: ""}${parts[1].firstOrNull() ?: ""}".uppercase()
                    } else {
                        (currentUser!!.fullName.take(2)).uppercase()
                    }
                } else {
                    "US"
                }
                val fullName = currentUser?.fullName ?: "Usuario Escolar"

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Avatar bubble
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    
                    Column {
                        Text(
                            text = "EduSmart ERP - $currentRole",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            lineHeight = 11.sp
                        )
                        Text(
                            text = fullName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Sync Status & Logout Action
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sincronización Chip
                    val (syncIcon, syncColor) = when (syncState) {
                        "Modo Offline" -> Icons.Default.CloudOff to StatusDanger
                        "Sincronizando..." -> Icons.Default.Sync to StatusWarning
                        else -> Icons.Default.CloudQueue to StatusSuccess
                    }

                    AssistChip(
                        onClick = onSyncToggle,
                        label = { Text(syncState, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(
                                imageVector = syncIcon,
                                contentDescription = "Sync State",
                                tint = syncColor,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = syncColor.copy(alpha = 0.12f),
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("sync_state_chip")
                    )

                    // Theme Toggle Button
                    IconButton(
                        onClick = onToggleDarkMode,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.WbSunny else Icons.Default.NightsStay,
                            contentDescription = "Cambiar Tema",
                            tint = if (isDarkMode) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary
                        )
                    }

                    // Logout Button
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Cerrar Sesión",
                            tint = StatusDanger
                        )
                    }
                }
            }

            // Quick Banner when Offline
            if (syncState == "Modo Offline") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StatusDanger)
                        .padding(vertical = 4.dp, horizontal = 16.dp)
                ) {
                    Text(
                        text = "Trabajando sin conexión local. Los datos se guardarán en el almacenamiento local SQLite.",
                        color = Color.White,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ==========================================================
// COMPONENT: Bottom Navigation Component (Adaptive navigation)
// ==========================================================
@Composable
fun BottomNavigationBarComponent(
    currentScreen: String,
    currentRole: String,
    unreadNotificationsCount: Int,
    activePermissions: RolePermission?,
    onScreenSelect: (String) -> Unit
) {
    NavigationBar {
        // Core Tab: Dashboard (All roles see this)
        NavigationBarItem(
            selected = currentScreen == "Dashboard",
            onClick = { onScreenSelect("Dashboard") },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Panel") },
            label = { Text("Panel", fontSize = 11.sp) }
        )

        // Core Tab: Students / academic view
        if (currentRole == "Administrador" || activePermissions?.canAccessStudents == true) {
            val studentsLabel = when (currentRole) {
                "Padres" -> "Notas Hijos"
                "Estudiantes" -> "Mis Notas"
                else -> "Estudiantes"
            }
            NavigationBarItem(
                selected = currentScreen == "Estudiantes",
                onClick = { onScreenSelect("Estudiantes") },
                icon = { Icon(Icons.Default.People, contentDescription = "Estudiantes") },
                label = { Text(studentsLabel, fontSize = 11.sp) }
            )
        }

        // Tab: DECE (Only visible or active for permitted roles)
        if (currentRole == "Administrador" || activePermissions?.canAccessDECE == true) {
            NavigationBarItem(
                selected = currentScreen == "DECE",
                onClick = { onScreenSelect("DECE") },
                icon = { Icon(Icons.Default.Psychology, contentDescription = "DECE") },
                label = { Text("DECE", fontSize = 11.sp) }
            )
        }

        // Core Tab: Chat
        if (currentRole == "Administrador" || activePermissions?.canAccessChat == true) {
            NavigationBarItem(
                selected = currentScreen == "Chat",
                onClick = { onScreenSelect("Chat") },
                icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") },
                label = { Text("Chat", fontSize = 11.sp) }
            )
        }

        // Core Tab: Notifications
        if (currentRole == "Administrador" || activePermissions?.canAccessNotificaciones == true) {
            NavigationBarItem(
                selected = currentScreen == "Notificaciones",
                onClick = { onScreenSelect("Notificaciones") },
                icon = {
                    BadgedBox(
                        badge = {
                            if (unreadNotificationsCount > 0) {
                                Badge { Text(unreadNotificationsCount.toString()) }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = "Alertas")
                    }
                },
                label = { Text("Alertas", fontSize = 11.sp) }
            )
        }

        // Core Tab: AI Assistant (permitted roles have direct access)
        if (currentRole == "Administrador" || activePermissions?.canAccessIA == true) {
            NavigationBarItem(
                selected = currentScreen == "IA_Asistente",
                onClick = { onScreenSelect("IA_Asistente") },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Asistente IA", tint = MaterialTheme.colorScheme.primary) },
                label = { Text("Asistente", fontSize = 11.sp) }
            )
        }

        // Identidad Tab: Visible for Administrador, Autoridades, Docente, DECE
        if (currentRole == "Administrador" || currentRole == "Autoridades" || currentRole == "Docente" || currentRole == "DECE") {
            NavigationBarItem(
                selected = currentScreen == "Identidad_Institucional",
                onClick = { onScreenSelect("Identidad_Institucional") },
                icon = { Icon(Icons.Default.School, contentDescription = "Identidad", tint = MaterialTheme.colorScheme.secondary) },
                label = { Text("Identidad", fontSize = 11.sp) }
            )
        }

        // Plantillas Tab: Visible for Administrador, Autoridades, Docente, DECE
        if (currentRole == "Administrador" || currentRole == "Autoridades" || currentRole == "Docente" || currentRole == "DECE") {
            NavigationBarItem(
                selected = currentScreen == "Constructor_Plantillas",
                onClick = { onScreenSelect("Constructor_Plantillas") },
                icon = { Icon(Icons.Default.DashboardCustomize, contentDescription = "Plantillas", tint = MaterialTheme.colorScheme.primary) },
                label = { Text("Plantillas", fontSize = 11.sp) }
            )
        }

        // Admin Tab: Only visible for Administrators
        if (currentRole == "Administrador") {
            NavigationBarItem(
                selected = currentScreen == "AdminPanel",
                onClick = { onScreenSelect("AdminPanel") },
                icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin", tint = StatusWarning) },
                label = { Text("Admin", fontSize = 11.sp) }
            )
        }
    }
}

// ==========================================================
// SCREEN: Dashboard Dashboard Screen
// ==========================================================
@Composable
fun DashboardScreen(
    currentRole: String,
    students: List<Student>,
    activities: List<SchoolActivity>,
    schedules: List<SchoolSchedule>,
    deceCases: List<DeceCase>,
    activePermissions: RolePermission?,
    onNavigate: (String) -> Unit,
    onToggleActivity: (SchoolActivity) -> Unit,
    onLogout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Banner Custom per Role
        item {
            WelcomeBannerCard(currentRole)
        }

        // Quick KPI Stats Widgets based on selected role
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (currentRole) {
                    "Docente" -> {
                        KpiCard(
                            title = "Estudiantes",
                            value = students.size.toString(),
                            icon = Icons.Default.Group,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            title = "Mis Horas",
                            value = schedules.size.toString(),
                            icon = Icons.Default.Schedule,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            title = "Casos DECE",
                            value = deceCases.count { it.status == "Abierto" }.toString(),
                            icon = Icons.Default.Psychology,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    "DECE" -> {
                        KpiCard(
                            title = "Casos Abiertos",
                            value = deceCases.count { it.status == "Abierto" }.toString(),
                            icon = Icons.Default.AssignmentLate,
                            modifier = Modifier.weight(1f),
                            color = StatusDanger
                        )
                        KpiCard(
                            title = "Estudiantes Riesgo",
                            value = students.count { it.academicStatus == "En Riesgo" }.toString(),
                            icon = Icons.Default.Warning,
                            modifier = Modifier.weight(1f),
                            color = StatusWarning
                        )
                    }
                    "Padres" -> {
                        KpiCard(
                            title = "Asistencia Promedio",
                            value = "88.2%",
                            icon = Icons.Default.CalendarToday,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            title = "Tareas Pendientes",
                            value = activities.count { !it.isCompleted }.toString(),
                            icon = Icons.Default.Assignment,
                            modifier = Modifier.weight(1f),
                            color = StatusWarning
                        )
                    }
                    "Estudiantes" -> {
                        KpiCard(
                            title = "Materias",
                            value = "6",
                            icon = Icons.Default.Book,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            title = "Mis Tareas",
                            value = "${activities.count { it.isCompleted }}/${activities.size}",
                            icon = Icons.Default.AssignmentTurnedIn,
                            modifier = Modifier.weight(1f),
                            color = StatusSuccess
                        )
                    }
                    "Autoridades" -> {
                        KpiCard(
                            title = "Asistencia General",
                            value = "91.8%",
                            icon = Icons.Default.BarChart,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            title = "Alertas de Riesgo",
                            value = (students.count { it.academicStatus == "En Riesgo" } + deceCases.count { it.priority == "Alta" }).toString(),
                            icon = Icons.Default.NotificationImportant,
                            modifier = Modifier.weight(1f),
                            color = StatusDanger
                        )
                    }
                    "Administrador" -> {
                        KpiCard(
                            title = "Cuentas",
                            value = "18", // Total pre-populated users
                            icon = Icons.Default.Group,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            title = "Roles LOEI",
                            value = "6",
                            icon = Icons.Default.Security,
                            modifier = Modifier.weight(1f),
                            color = StatusWarning
                        )
                        KpiCard(
                            title = "Casos DECE",
                            value = deceCases.size.toString(),
                            icon = Icons.Default.Psychology,
                            modifier = Modifier.weight(1f),
                            color = StatusInfo
                        )
                    }
                }
            }
        }

        // Sleek Interface Quick Actions Grid
        item {
            Text(
                text = "Acciones Rápidas",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
            QuickActionsGrid(activePermissions = activePermissions, currentRole = currentRole, onNavigate = onNavigate)
        }

        // Section: Content Widget depending on selected role
        when (currentRole) {
            "Docente" -> {
                item {
                    SectionHeader("Mi Horario de Clases Hoy")
                }
                if (schedules.isEmpty()) {
                    item { EmptyStateCard("No hay clases registradas hoy.") }
                } else {
                    items(schedules) { schedule ->
                        ScheduleItemCard(schedule)
                    }
                }
            }
            "DECE" -> {
                item {
                    SectionHeader("Estudiantes de Alto Riesgo Reciente")
                }
                val riskStudents = students.filter { it.academicStatus == "En Riesgo" || it.behaviorStatus == "Grave" }
                if (riskStudents.isEmpty()) {
                    item { EmptyStateCard("No se reportan estudiantes en riesgo crítico.") }
                } else {
                    items(riskStudents) { student ->
                        StudentRiskItemCard(student, onAction = { onNavigate("Estudiantes") })
                    }
                }
            }
            "Padres" -> {
                item {
                    SectionHeader("Tareas Pendientes de mis Hijos")
                }
                val pending = activities.filter { !it.isCompleted }
                if (pending.isEmpty()) {
                    item { EmptyStateCard("¡Felicidades! No hay tareas pendientes registradas.") }
                } else {
                    items(pending) { activity ->
                        ActivityItemCard(activity, currentRole, onToggleActivity)
                    }
                }
            }
            "Estudiantes" -> {
                item {
                    SectionHeader("Mis Tareas Escolares")
                }
                if (activities.isEmpty()) {
                    item { EmptyStateCard("No hay actividades cargadas.") }
                } else {
                    items(activities) { activity ->
                        ActivityItemCard(activity, currentRole, onToggleActivity)
                    }
                }
            }
            "Autoridades" -> {
                item {
                    SectionHeader("Indicadores Institucionales y Alertas")
                }
                item {
                    AuthoritiesIndicatorsCard()
                }
            }
            "Administrador" -> {
                item {
                    SectionHeader("Consola de Control del Sistema")
                }
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Panel de Configuración y Control Activo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Como Administrador del Sistema, usted puede crear nuevas cuentas, asignar roles y sintonizar en tiempo real los permisos de acceso de cada perfil.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onNavigate("AdminPanel") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Ir a Control de Roles", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Custom AI prompt suggestion inside Dashboard
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("IA_Asistente") }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Asistente Escolar IA",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Planificaciones del currículo ecuatoriano, rúbricas LOEI, estrategias de aprendizaje e informes con un toque.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Prominent logout block for comfortable UX
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, StatusDanger.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLogout() }
                    .testTag("dashboard_logout_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Cerrar Sesión",
                        tint = StatusDanger,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Cerrar Sesión Activa",
                        color = StatusDanger,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun WelcomeBannerCard(role: String) {
    val badgeText = when (role) {
        "Docente" -> "Próxima Clase: 10:30 AM"
        "DECE" -> "Caso Abierto Prioritario"
        "Padres" -> "Último Reporte Recibido"
        "Estudiantes" -> "Mi Próxima Clase: 08:30 AM"
        else -> "Indicadores LOEI"
    }
    
    val titleText = when (role) {
        "Docente" -> "Matemáticas II"
        "DECE" -> "Daniel Torres - Seguimiento"
        "Padres" -> "Reunión de Padres"
        "Estudiantes" -> "Química Inorgánica"
        else -> "Control de Planificación"
    }

    val infoText = when (role) {
        "Docente" -> "2do de Bachillerato \"B\" • Aula 402"
        "DECE" -> "Aula 302 • Nivel de Riesgo: Alto"
        "Padres" -> "Viernes 17:00 • Aula de Audiovisuales"
        "Estudiantes" -> "3er de Bachillerato \"A\" • Laboratorio"
        else -> "Revisión curricular de Matemáticas II pendiente"
    }

    val actionButtonText = when (role) {
        "Docente" -> "Pasar Lista QR"
        "DECE" -> "Ver Ficha Completa"
        "Padres" -> "Justificar Falta"
        "Estudiantes" -> "Escanear Asistencia"
        else -> "Firmar Planificación"
    }

    Card(
        shape = RoundedCornerShape(32.dp), // rounded-[2rem]
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Reloj",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = titleText,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = infoText,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { /* Simulated list check or detail action */ },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = actionButtonText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .clickable { /* Fast action */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Acción rápida",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionsGrid(
    activePermissions: RolePermission?,
    currentRole: String,
    onNavigate: (String) -> Unit
) {
    val canAccessStudents = currentRole == "Administrador" || activePermissions?.canAccessStudents == true
    val canAccessIA = currentRole == "Administrador" || activePermissions?.canAccessIA == true
    val canAccessChat = currentRole == "Administrador" || activePermissions?.canAccessChat == true

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                title = "Calificaciones",
                icon = Icons.Default.Assessment,
                backgroundColor = ActionBlueBg,
                iconColor = ActionBlueIconBg,
                textColor = ActionBlueText,
                modifier = Modifier.weight(1f),
                isLocked = !canAccessStudents,
                onClick = { onNavigate("Estudiantes") }
            )
            QuickActionCard(
                title = "Asistente IA",
                icon = Icons.Default.AutoAwesome,
                backgroundColor = ActionPurpleBg,
                iconColor = ActionPurpleIconBg,
                textColor = ActionPurpleText,
                modifier = Modifier.weight(1f),
                isLocked = !canAccessIA,
                onClick = { onNavigate("IA_Asistente") }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                title = "Horario",
                icon = Icons.Default.CalendarToday,
                backgroundColor = ActionEmeraldBg,
                iconColor = ActionEmeraldIconBg,
                textColor = ActionEmeraldText,
                modifier = Modifier.weight(1f),
                isLocked = false,
                onClick = { onNavigate("Dashboard") }
            )
            QuickActionCard(
                title = "Mensajes",
                icon = Icons.Default.Chat,
                backgroundColor = ActionOrangeBg,
                iconColor = ActionOrangeIconBg,
                textColor = ActionOrangeText,
                modifier = Modifier.weight(1f),
                isLocked = !canAccessChat,
                onClick = { onNavigate("Chat") }
            )
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    backgroundColor: Color,
    iconColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    isLocked: Boolean = false,
    onClick: () -> Unit
) {
    val finalBgColor = if (isLocked) Color.LightGray.copy(alpha = 0.35f) else backgroundColor
    val finalIconBgColor = if (isLocked) Color.Gray.copy(alpha = 0.5f) else iconColor
    val finalTextColor = if (isLocked) Color.Gray else textColor

    Card(
        shape = RoundedCornerShape(24.dp), // rounded-3xl
        colors = CardDefaults.cardColors(containerColor = finalBgColor),
        modifier = modifier
            .height(115.dp)
            .clickable(enabled = !isLocked) { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(finalIconBgColor, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                if (isLocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Bloqueado",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = finalTextColor
            )
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun ScheduleItemCard(schedule: SchoolSchedule) {
    Card(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(schedule.subject, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Grado: ${schedule.grade}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = "Hora", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(schedule.timeSlot, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StudentRiskItemCard(student: Student, onAction: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAction() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left border-l-4 style
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(WarningAmberBorder)
            )
            
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(WarningAmberBg, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alerta",
                            tint = WarningAmberText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Column {
                        Text(
                            text = "Falta o Alerta Académica",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmberText
                        )
                        Text(
                            text = student.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Asistencia: ${student.attendanceRate}%",
                                fontSize = 12.sp,
                                color = StatusDanger,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Estatus: ${student.academicStatus}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
                
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Ver",
                    tint = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ActivityItemCard(
    activity: SchoolActivity,
    role: String,
    onToggle: (SchoolActivity) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        colors = CardDefaults.cardColors(
            containerColor = if (activity.isCompleted) StatusSuccess.copy(alpha = 0.04f) else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = activity.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (activity.isCompleted) StatusSuccess else MaterialTheme.colorScheme.onSurface
                    )
                    if (activity.isCompleted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.CheckCircle, contentDescription = "Completa", tint = StatusSuccess, modifier = Modifier.size(16.dp))
                    }
                }
                Text("Materia: ${activity.subject}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text(activity.description, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                Text(
                    text = "Entrega: ${activity.dueDate}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activity.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else StatusWarning,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (role == "Estudiantes" && !activity.isCompleted) {
                IconButton(
                    onClick = { onToggle(activity) },
                    modifier = Modifier.testTag("complete_activity_btn_${activity.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Marcar Entregada",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun AuthoritiesIndicatorsCard() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Asistencia por Sección Lectiva", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))

            LabelAndProgressBar("Inicial y Preparatoria", 0.95f, StatusSuccess)
            Spacer(modifier = Modifier.height(8.dp))
            LabelAndProgressBar("Básica Elemental", 0.92f, StatusSuccess)
            Spacer(modifier = Modifier.height(8.dp))
            LabelAndProgressBar("Básica Media", 0.89f, StatusWarning)
            Spacer(modifier = Modifier.height(8.dp))
            LabelAndProgressBar("Básica Superior", 0.86f, StatusWarning)
            Spacer(modifier = Modifier.height(8.dp))
            LabelAndProgressBar("Bachillerato General Unificado", 0.79f, StatusDanger)
        }
    }
}

@Composable
fun LabelAndProgressBar(label: String, progress: Float, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp)
            Text("${(progress * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            color = color,
            trackColor = color.copy(alpha = 0.15f),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

@Composable
fun EmptyStateCard(text: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), textAlign = TextAlign.Center)
        }
    }
}

// ==========================================================
// SCREEN: Students / Classroom List & Attendance reporting
// ==========================================================
@Composable
fun StudentListScreen(
    currentRole: String,
    students: List<Student>,
    onReportNovedad: (Student, String, String) -> Unit
) {
    var selectedStudentForReport by remember { mutableStateOf<Student?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Directorio y Registro de Clases",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = if (currentRole in listOf("Docente", "DECE", "Autoridades"))
                "Registre novedades disciplinarias, asistenia y rendimiento académico del estudiantado."
            else "Visualización del expediente y rendimiento académico institucional.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(students) { student ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(student.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Curso: ${student.grade}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            // Attendance Badge
                            val attendanceColor = when {
                                student.attendanceRate >= 90.0 -> StatusSuccess
                                student.attendanceRate >= 80.0 -> StatusWarning
                                else -> StatusDanger
                            }
                            Box(
                                modifier = Modifier
                                    .background(attendanceColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "Asistencia: ${student.attendanceRate}%",
                                    color = attendanceColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Estatus Académico", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                val acadColor = when (student.academicStatus) {
                                    "Excelente" -> StatusSuccess
                                    "Regular" -> StatusWarning
                                    else -> StatusDanger
                                }
                                Text(student.academicStatus, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = acadColor)
                            }
                            Column {
                                Text("Comportamiento", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                val behColor = when (student.behaviorStatus) {
                                    "Excelente" -> StatusSuccess
                                    "Novedad" -> StatusWarning
                                    else -> StatusDanger
                                }
                                Text(student.behaviorStatus, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = behColor)
                            }

                            // Docente Action Button
                            if (currentRole in listOf("Docente", "DECE")) {
                                Button(
                                    onClick = { selectedStudentForReport = student },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("report_btn_${student.id}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Registrar", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reportar", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog to report Novedad
    if (selectedStudentForReport != null) {
        val student = selectedStudentForReport!!
        var behaviorReport by remember { mutableStateOf("Excelente") }
        var academicReport by remember { mutableStateOf("Excelente") }

        Dialog(onDismissRequest = { selectedStudentForReport = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Reportar Novedad",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Estudiante: ${student.name}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    // Comportamiento / Asistencia
                    Text("Asistencia o Conducta", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Excelente", "Falta Injustificada", "Grave").forEach { option ->
                            val selected = behaviorReport == option
                            val chipColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            val textColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(chipColor, RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .clickable { behaviorReport = option }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(option, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor, textAlign = TextAlign.Center)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Estatus Académico
                    Text("Nivel Académico Actual", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Excelente", "Regular", "En Riesgo").forEach { option ->
                            val selected = academicReport == option
                            val chipColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            val textColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(chipColor, RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .clickable { academicReport = option }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(option, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor, textAlign = TextAlign.Center)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { selectedStudentForReport = null }) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onReportNovedad(student, behaviorReport, academicReport)
                                selectedStudentForReport = null
                            },
                            modifier = Modifier.testTag("submit_report_button")
                        ) {
                            Text("Guardar Reporte")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================
// SCREEN: DECE Counselor management
// ==========================================================
@Composable
fun DeceManagementScreen(
    currentRole: String,
    students: List<Student>,
    deceCases: List<DeceCase>,
    onCreateCase: (String, String, String, String, String) -> Unit,
    onUpdateCase: (DeceCase, String, String) -> Unit
) {
    var showCreateCaseDialog by remember { mutableStateOf(false) }
    var selectedCaseForInterview by remember { mutableStateOf<DeceCase?>(null) }

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
                    "Expedientes y Seguimiento DECE",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Departamento de Consejería Estudiantil (DECE)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            if (currentRole == "DECE") {
                FloatingActionButton(
                    onClick = { showCreateCaseDialog = true },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("create_case_fab"),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Crear Caso", tint = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (deceCases.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay casos de consejería cargados.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(deceCases) { deceCase ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(deceCase.studentName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(deceCase.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                                }

                                // Sync status and priority
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (deceCase.isSynced) {
                                        Icon(Icons.Default.Check, contentDescription = "Sincronizado", tint = StatusSuccess, modifier = Modifier.size(16.dp))
                                    } else {
                                        Icon(Icons.Default.CloudQueue, contentDescription = "Cambio local", tint = StatusWarning, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))

                                    val badgeColor = when (deceCase.priority) {
                                        "Alta" -> StatusDanger
                                        "Media" -> StatusWarning
                                        else -> StatusSuccess
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(deceCase.priority, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Text(
                                deceCase.description,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Última entrevista/visita", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text(deceCase.lastInterviewDate, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }

                                Box(
                                    modifier = Modifier
                                        .background(
                                            when (deceCase.status) {
                                                "Abierto" -> StatusDanger.copy(alpha = 0.12f)
                                                "En Seguimiento" -> StatusWarning.copy(alpha = 0.12f)
                                                else -> StatusSuccess.copy(alpha = 0.12f)
                                            },
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        deceCase.status,
                                        color = when (deceCase.status) {
                                            "Abierto" -> StatusDanger
                                            "En Seguimiento" -> StatusWarning
                                            else -> StatusSuccess
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (currentRole == "DECE" && deceCase.status != "Resuelto") {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { selectedCaseForInterview = deceCase },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                        .testTag("followup_case_${deceCase.id}"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Notes, contentDescription = "Seguimiento", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Registrar Entrevista o Seguimiento", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Create Case
    if (showCreateCaseDialog) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var selectedStudent by remember { mutableStateOf(students.firstOrNull()) }
        var priority by remember { mutableStateOf("Media") }

        Dialog(onDismissRequest = { showCreateCaseDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Registrar Nuevo Caso DECE", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Seleccionar Estudiante", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    // Simple simulated Dropdown/Chips for student selection
                    Box(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            students.forEach { s ->
                                FilterChip(
                                    selected = selectedStudent?.id == s.id,
                                    onClick = { selectedStudent = s },
                                    label = { Text(s.name, fontSize = 11.sp) },
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Motivo / Título del Caso") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción de la Novedad") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Prioridad", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Baja", "Media", "Alta").forEach { p ->
                            val isSel = priority == p
                            Button(
                                onClick = { priority = p },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(p, fontSize = 11.sp, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCreateCaseDialog = false }) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (selectedStudent != null && title.isNotBlank() && description.isNotBlank()) {
                                    onCreateCase(selectedStudent!!.id, selectedStudent!!.name, title, description, priority)
                                    showCreateCaseDialog = false
                                }
                            },
                            modifier = Modifier.testTag("submit_case_creation_btn")
                        ) {
                            Text("Abrir Caso")
                        }
                    }
                }
            }
        }
    }

    // Modal Add Case Interview Note
    if (selectedCaseForInterview != null) {
        val deceCase = selectedCaseForInterview!!
        var interviewNotes by remember { mutableStateOf("") }
        var nextStatus by remember { mutableStateOf("En Seguimiento") }

        Dialog(onDismissRequest = { selectedCaseForInterview = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Registrar Seguimiento DECE", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    Text("Estudiante: ${deceCase.studentName}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = interviewNotes,
                        onValueChange = { interviewNotes = it },
                        label = { Text("Resumen de la Entrevista o Acuerdos") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Nuevo Estado del Caso", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Abierto", "En Seguimiento", "Resuelto").forEach { s ->
                            val selected = nextStatus == s
                            Button(
                                onClick = { nextStatus = s },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(s, fontSize = 9.sp, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { selectedCaseForInterview = null }) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (interviewNotes.isNotBlank()) {
                                    onUpdateCase(deceCase, nextStatus, interviewNotes)
                                    selectedCaseForInterview = null
                                }
                            },
                            modifier = Modifier.testTag("submit_followup_btn")
                        ) {
                            Text("Guardar Cambios")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================
// SCREEN: Real-time Multi-Role Chat Spaces
// ==========================================================
@Composable
fun ChatScreen(
    currentRole: String,
    chatHistory: List<ChatMessage>,
    onSendMessage: (String, String) -> Unit,
    onSendAttachment: (String, String) -> Unit
) {
    // Determine possible chat channels based on active role
    val channels = when (currentRole) {
        "Docente" -> listOf("Padres", "DECE", "Autoridades", "Estudiantes")
        "DECE" -> listOf("Docente", "Padres", "Autoridades")
        "Padres" -> listOf("Docente", "DECE")
        "Estudiantes" -> listOf("Docente")
        else -> listOf("Docente", "DECE", "Padres") // Autoridades
    }

    var selectedChannel by remember { mutableStateOf(channels.first()) }
    var chatInputText by remember { mutableStateOf("") }

    // Filter chat history relevant to current role and target role
    val currentChatThread = chatHistory.filter {
        (it.senderRole == currentRole && it.recipientRole == selectedChannel) ||
        (it.senderRole == selectedChannel && it.recipientRole == currentRole)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Mensajería Instantánea Institucional",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Canal de comunicación seguro. Toda la información está encriptada y sincronizada.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Chat Channels Selector Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            channels.forEach { channel ->
                FilterChip(
                    selected = selectedChannel == channel,
                    onClick = { selectedChannel = channel },
                    label = { Text("Chat con $channel", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = channel, modifier = Modifier.size(14.dp))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chat bubble lists
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            if (currentChatThread.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MarkChatRead, contentDescription = "No hay mensajes", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Inicio de conversación segura.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            "Escriba un mensaje para interactuar con $selectedChannel.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    reverseLayout = false
                ) {
                    items(currentChatThread) { msg ->
                        val isMe = msg.senderName.startsWith("Usted")
                        val align = if (isMe) Alignment.End else Alignment.Start
                        val bubbleBg = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        val textColor = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
                        val shape = if (isMe) {
                            RoundedCornerShape(12.dp, 12.dp, 0.dp, 12.dp)
                        } else {
                            RoundedCornerShape(12.dp, 12.dp, 12.dp, 0.dp)
                        }

                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
                            Card(
                                shape = shape,
                                colors = CardDefaults.cardColors(containerColor = bubbleBg),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    if (!isMe) {
                                        Text(
                                            msg.senderName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }

                                    // Display attachments beautifully if they exist
                                    if (msg.attachmentType != null) {
                                        Row(
                                            modifier = Modifier
                                                .background(
                                                    if (isMe) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(6.dp)
                                                .fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = when (msg.attachmentType) {
                                                    "PDF" -> Icons.Default.Description
                                                    "IMAGE" -> Icons.Default.Photo
                                                    else -> Icons.Default.VolumeUp
                                                },
                                                contentDescription = "Attachment",
                                                tint = if (isMe) Color.White else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = "Adjunto ${msg.attachmentType}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = textColor
                                                )
                                                Text("archivo_firmado.${msg.attachmentType.lowercase()}", fontSize = 9.sp, color = textColor.copy(alpha = 0.7f))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }

                                    Text(
                                        msg.message,
                                        fontSize = 13.sp,
                                        color = textColor
                                    )
                                }
                            }

                            // Blue ticks confirmation
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "10:00 AM",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                                if (isMe) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = "Leído",
                                        tint = StatusSuccess,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text("Leído", fontSize = 8.sp, color = StatusSuccess, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Simulated Typist Indicator
        if (currentChatThread.isNotEmpty() && currentChatThread.last().senderName.startsWith("Usted")) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            ) {
                Text(
                    "Simulador ($selectedChannel) está escribiendo",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text("...", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
        }

        // Bottom Attachment Bar and Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Attachment Actions Menu
            var showAttachmentMenu by remember { mutableStateOf(false) }
            IconButton(
                onClick = { showAttachmentMenu = true },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                    .size(40.dp)
                    .testTag("attachment_menu_btn")
            ) {
                Icon(Icons.Default.AttachFile, contentDescription = "Adjuntar", tint = MaterialTheme.colorScheme.primary)
            }

            DropdownMenu(
                expanded = showAttachmentMenu,
                onDismissRequest = { showAttachmentMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Escanear Reporte PDF") },
                    onClick = {
                        onSendAttachment("PDF", selectedChannel)
                        showAttachmentMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF") }
                )
                DropdownMenuItem(
                    text = { Text("Evidencia de Foto") },
                    onClick = {
                        onSendAttachment("IMAGE", selectedChannel)
                        showAttachmentMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = "Camera") }
                )
                DropdownMenuItem(
                    text = { Text("Grabar Nota de Voz") },
                    onClick = {
                        onSendAttachment("VOICE", selectedChannel)
                        showAttachmentMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Mic, contentDescription = "Audio") }
                )
            }

            // Chat input TextField
            OutlinedTextField(
                value = chatInputText,
                onValueChange = { chatInputText = it },
                placeholder = { Text("Escriba un mensaje...", fontSize = 13.sp) },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(max = 48.dp)
                    .testTag("chat_input_text_field"),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (chatInputText.isNotBlank()) {
                                onSendMessage(chatInputText, selectedChannel)
                                chatInputText = ""
                            }
                        },
                        modifier = Modifier.testTag("send_chat_btn")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Enviar", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    }
}

// ==========================================================
// SCREEN: Live push notifications logs
// ==========================================================
@Composable
fun NotificationsScreen(
    notifications: List<SchoolNotification>,
    onMarkRead: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Centro de Alertas y Notificaciones",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Historial de notificaciones push de evaluaciones, novedades y comunicados urgentes.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay alertas registradas.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(notifications) { item ->
                    val border = if (item.isRead) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    } else {
                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    }
                    val bg = if (item.isRead) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        border = border,
                        colors = CardDefaults.cardColors(containerColor = bg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (!item.isRead) onMarkRead(item.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Category Icon
                            val icon = when (item.category) {
                                "DECE" -> Icons.Default.Psychology
                                "Evaluación" -> Icons.Default.Assignment
                                "Asistencia" -> Icons.Default.EventBusy
                                "Urgente" -> Icons.Default.Error
                                else -> Icons.Default.Info
                            }
                            val iconColor = when (item.category) {
                                "DECE" -> StatusInfo
                                "Evaluación" -> StatusSuccess
                                "Asistencia" -> StatusWarning
                                "Urgente" -> StatusDanger
                                else -> MaterialTheme.colorScheme.primary
                            }

                            Icon(
                                imageVector = icon,
                                contentDescription = item.category,
                                tint = iconColor,
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(iconColor.copy(alpha = 0.12f), CircleShape)
                                    .padding(6.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.title,
                                        fontWeight = if (item.isRead) FontWeight.Medium else FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!item.isRead) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(StatusDanger, CircleShape)
                                        )
                                    }
                                }
                                Text(
                                    text = item.message,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Text(
                                    text = "Recibido hoy",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================
// SCREEN: Gemini AI Assistant conversational layout
// ==========================================================
@Composable
fun AiAssistantScreen(
    currentRole: String,
    aiResponse: String,
    aiIsLoading: Boolean,
    onAsk: (String) -> Unit
) {
    var aiQueryInput by remember { mutableStateOf("") }

    val assistantTitle = when (currentRole) {
        "Docente" -> "Asistente Pedagógico Inteligente"
        "DECE" -> "Asistente DECE Inteligente"
        else -> "Asistente Escolar Inteligente"
    }

    val assistantSubtitle = when (currentRole) {
        "Docente" -> "Especializado únicamente en procesos pedagógicos, planificación curricular, DUA, ERCA, adaptaciones y rúbricas. (No tiene acceso a expedientes DECE)."
        "DECE" -> "Especializado únicamente en acompañamiento estudiantil, informes psicológicos, actas, derivaciones y planes de intervención. (No genera planificaciones pedagógicas)."
        else -> "Cumple con los lineamientos del Ministerio de Educación de Ecuador y la LOEI."
    }

    val promptSuggestions = when (currentRole) {
        "Docente" -> listOf(
            "Planificación Anual: Unidad de Álgebra de 10mo grado.",
            "Generar Rúbrica: Evaluación de exposición oral (LOEI).",
            "Secuencia Didáctica ERCA: Proyecto de Ciencias Naturales.",
            "Adaptación Curricular: Estudiante con dislexia de grado 2.",
            "Banco de Preguntas: Prueba trimestral de Estudios Sociales."
        )
        "DECE" -> listOf(
            "Informe Psicológico: Estudiante con conductas disruptivas.",
            "Acta de Compromiso: Firma del representante por bajo rendimiento.",
            "Plan de Intervención: Alumno con desmotivación escolar.",
            "Informe de Derivación: Estudiante derivado a centro de salud pública.",
            "Análisis de Caso: Estudiante de 15 años con ausentismo severo."
        )
        else -> listOf(
            "Planificación: Unidad de Álgebra de 10mo grado Ecuador.",
            "Generar Rúbrica: Evaluación de exposición oral (LOEI).",
            "Estrategias DECE: Niño 9 años con sospecha de TDAH.",
            "Detectar Riesgo: Alumno con 45% faltas injustificadas.",
            "Normativa LOEI: Evaluación y recuperación de calificaciones."
        )
    }

    val inputPlaceholder = when (currentRole) {
        "Docente" -> "Planificaciones, DUA, ERCA, rúbricas..."
        "DECE" -> "Informes psicológicos, actas de compromiso, derivaciones..."
        else -> "Pregunte sobre el currículo ecuatoriano..."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                assistantTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            assistantSubtitle,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Suggestion Chips Scroll
        Text("Sugerencias Rápidas de la IA:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            promptSuggestions.forEach { suggestion ->
                SuggestionChip(
                    onClick = {
                        aiQueryInput = suggestion
                        onAsk(suggestion)
                    },
                    label = { Text(suggestion.take(30) + "...", fontSize = 10.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // AI Output Display Card
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            if (aiIsLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Consultando al Asistente EducaSmart...",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else if (aiResponse.isBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PsychologyAlt,
                            contentDescription = "IA",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "¡Haga una pregunta pedagógica o legal!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "El modelo generará rúbricas, planificaciones y análisis de casos bajo el currículo de Ecuador.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                // Scrollable Response Panel
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Text(
                        text = aiResponse,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Copy action Simulation
                    Button(
                        onClick = { /* simulated download/copy */ },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copiar al Portapapeles", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Input Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = aiQueryInput,
                onValueChange = { aiQueryInput = it },
                placeholder = { Text(inputPlaceholder, fontSize = 13.sp) },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_input_text_field"),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (aiQueryInput.isNotBlank()) {
                                onAsk(aiQueryInput)
                            }
                        },
                        modifier = Modifier.testTag("ask_ai_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Preguntar", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    }
}

// ==========================================================
// SCREEN: Login Screen (Secure credential access & Quick Login)
// ==========================================================
@Composable
fun LoginScreen(
    userAccounts: List<UserAccount>,
    loginError: String?,
    onLogin: (String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // App Header
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "Logo",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "EduSmart ERP",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Sistema Integrado de Permisos Dinámicos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }

            // Error Message Card
            if (loginError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StatusDanger.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = StatusDanger
                        )
                        Text(
                            text = loginError,
                            color = StatusDanger,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Credentials Fields Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Inicio de Sesión",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Usuario") },
                        placeholder = { Text("ej. docente") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_username_field"),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "User") }
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        placeholder = { Text("••••••••") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_field"),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") }
                    )

                    Button(
                        onClick = { onLogin(username.trim(), password) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_submit_button")
                    ) {
                        Text("Ingresar al Sistema", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Quick Access Section (Crucial for testing RBAC easily!)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "⚡ Acceso Rápido para Demostración",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Haga clic en cualquiera para auto-completar e ingresar al instante con su respectivo rol:",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    // Horizontal scrolling quick login chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("Administrador", "admin", "admin"),
                            Triple("Docente", "docente", "docente"),
                            Triple("DECE", "dece", "dece"),
                            Triple("Padres", "padre", "padre"),
                            Triple("Estudiante", "estudiante", "estudiante")
                        ).forEach { (label, u, p) ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    username = u
                                    password = p
                                    onLogin(u, p)
                                },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (label) {
                                            "Administrador" -> Icons.Default.AdminPanelSettings
                                            "Docente" -> Icons.Default.SupervisorAccount
                                            "DECE" -> Icons.Default.Psychology
                                            "Padres" -> Icons.Default.FamilyRestroom
                                            else -> Icons.Default.Face
                                        },
                                        contentDescription = label,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================
// SCREEN: Access Denied Screen (Elegant restricted banner)
// ==========================================================
@Composable
fun AccessDeniedScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.widthIn(max = 350.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(StatusDanger.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Bloqueado",
                    tint = StatusDanger,
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                text = "Acceso Restringido",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Su cuenta no cuenta con permisos asignados por el administrador para ver este módulo escolar.",
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            
            Text(
                text = "Consulte al Administrador para habilitar esta pestaña en el panel dinámico de roles.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==========================================================
// SCREEN: Admin Panel Screen (Full RBAC Matrix and Accounts)
// ==========================================================
@Composable
fun AdminPanelScreen(
    userAccounts: List<UserAccount>,
    rolePermissions: List<RolePermission>,
    onCreateUser: (String, String, String, String) -> Unit,
    onDeleteUser: (UserAccount) -> Unit,
    onUpdatePermission: (RolePermission) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Matriz de Permisos", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Security, contentDescription = "Permisos") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Cuentas de Usuarios", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.GroupAdd, contentDescription = "Cuentas") }
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (selectedTab == 0) {
                // Permissions Matrix Tab
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column {
                            Text(
                                text = "Matriz de Roles y Permisos Dinámicos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Tenga en cuenta que cualquier cambio se aplica inmediatamente a todas las cuentas asignadas a dicho Rol en SQLite.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    items(rolePermissions) { permission ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = when (permission.role) {
                                            "Docente" -> Icons.Default.SupervisorAccount
                                            "DECE" -> Icons.Default.Psychology
                                            "Padres" -> Icons.Default.FamilyRestroom
                                            "Estudiantes" -> Icons.Default.Face
                                            else -> Icons.Default.ManageAccounts
                                        },
                                        contentDescription = "Rol",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = permission.role,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Switches
                                PermissionSwitchRow(
                                    label = "Ver Estudiantes e Historial Académico",
                                    checked = permission.canAccessStudents,
                                    onCheckedChange = { onUpdatePermission(permission.copy(canAccessStudents = it)) }
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                                
                                PermissionSwitchRow(
                                    label = "Ver Casos y Fichas de Apoyo DECE",
                                    checked = permission.canAccessDECE,
                                    onCheckedChange = { onUpdatePermission(permission.copy(canAccessDECE = it)) }
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))

                                PermissionSwitchRow(
                                    label = "Enviar Mensajes y Utilizar Chat Escolar",
                                    checked = permission.canAccessChat,
                                    onCheckedChange = { onUpdatePermission(permission.copy(canAccessChat = it)) }
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))

                                PermissionSwitchRow(
                                    label = "Utilizar el Asistente Inteligente IA",
                                    checked = permission.canAccessIA,
                                    onCheckedChange = { onUpdatePermission(permission.copy(canAccessIA = it)) }
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))

                                PermissionSwitchRow(
                                    label = "Recibir Alertas y Notificaciones Escolares",
                                    checked = permission.canAccessNotificaciones,
                                    onCheckedChange = { onUpdatePermission(permission.copy(canAccessNotificaciones = it)) }
                                )
                            }
                        }
                    }
                }
            } else {
                // User Accounts Tab
                var newUsername by remember { mutableStateOf("") }
                var newPassword by remember { mutableStateOf("") }
                var newFullName by remember { mutableStateOf("") }
                var selectedRole by remember { mutableStateOf("Docente") }
                var expandedRoleDropdown by remember { mutableStateOf(false) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Registrar Nueva Cuenta Escolar",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                OutlinedTextField(
                                    value = newFullName,
                                    onValueChange = {
                                        newFullName = it
                                        newUsername = generateSchoolUsername(it)
                                    },
                                    label = { Text("Nombre Completo") },
                                    placeholder = { Text("Ej. Ana Alvarez Palacios") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = newUsername,
                                    onValueChange = { newUsername = it },
                                    label = { Text("Nombre de Usuario (LOEI Auto)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = newPassword,
                                    onValueChange = { newPassword = it },
                                    label = { Text("Contraseña") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { expandedRoleDropdown = true },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Rol Asignado: $selectedRole")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                    }

                                    DropdownMenu(
                                        expanded = expandedRoleDropdown,
                                        onDismissRequest = { expandedRoleDropdown = false },
                                        modifier = Modifier.fillMaxWidth(0.8f)
                                    ) {
                                        listOf("Docente", "DECE", "Padres", "Estudiantes", "Autoridades", "Administrador").forEach { role ->
                                            DropdownMenuItem(
                                                text = { Text(role) },
                                                onClick = {
                                                    selectedRole = role
                                                    expandedRoleDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (newUsername.isNotBlank() && newPassword.isNotBlank() && newFullName.isNotBlank()) {
                                            onCreateUser(
                                                newUsername.trim().lowercase(),
                                                newPassword,
                                                newFullName.trim(),
                                                selectedRole
                                            )
                                            newUsername = ""
                                            newPassword = ""
                                            newFullName = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Registrar")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Agregar Cuenta")
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Lista de Cuentas Registradas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(userAccounts) { account ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (account.role) {
                                                "Administrador" -> Icons.Default.AdminPanelSettings
                                                "Docente" -> Icons.Default.SupervisorAccount
                                                "DECE" -> Icons.Default.Psychology
                                                "Padres" -> Icons.Default.FamilyRestroom
                                                else -> Icons.Default.Face
                                            },
                                            contentDescription = "Rol icon",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = account.fullName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "@${account.username} • Rol: ${account.role}",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                // Prevent deleting the default admin account to prevent lockout
                                if (account.username != "admin") {
                                    IconButton(
                                        onClick = { onDeleteUser(account) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar",
                                            tint = StatusDanger
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Generates a normalized LOEI-compliant school username based on a person's full name.
 * Example: "Ana Alvarez Palacios" -> "aalvarezp"
 * It supports titles (Prof., Dr., Dra., Psi., etc.) and normalizes accented Spanish vowels and 'ñ'.
 */
fun generateSchoolUsername(fullName: String): String {
    val clean = fullName.trim().lowercase()
        .replace('á', 'a')
        .replace('é', 'e')
        .replace('í', 'i')
        .replace('ó', 'o')
        .replace('ú', 'u')
        .replace('ñ', 'n')
    
    val parts = clean.split("\\s+".toRegex()).filter { it.isNotBlank() }
    if (parts.isEmpty()) return ""
    
    // Filter out common titles to get to the real name parts
    val titles = listOf("prof.", "prof", "dr.", "dr", "dra.", "dra", "psi.", "psi", "rep.", "rep", "ing.", "ing", "lic.", "lic", "dra")
    val filteredParts = parts.filter { it !in titles }
    if (filteredParts.isEmpty()) return "usuario"
    
    return when (filteredParts.size) {
        1 -> filteredParts[0]
        2 -> {
            val firstInitial = filteredParts[0].firstOrNull()?.toString() ?: ""
            firstInitial + filteredParts[1]
        }
        else -> {
            // First letter of first name + complete first surname + first letter of second surname
            val firstInitial = filteredParts[0].firstOrNull()?.toString() ?: ""
            val firstSurname = filteredParts[filteredParts.size - 2]
            val secondSurnameInitial = filteredParts[filteredParts.size - 1].firstOrNull()?.toString() ?: ""
            firstInitial + firstSurname + secondSurnameInitial
        }
    }
}
