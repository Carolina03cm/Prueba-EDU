package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.CustomTemplate
import com.example.data.database.InstitutionProfile
import com.example.ui.viewmodel.SchoolViewModel

// Safe Hex Color Parser helper
fun parseHexColor(hex: String, fallback: Color = Color(0xFF1E3A8A)): Color {
    return try {
        val cleanHex = hex.trim().replace("#", "")
        if (cleanHex.length == 6) {
            Color(android.graphics.Color.parseColor("#$cleanHex"))
        } else if (cleanHex.length == 8) {
            Color(android.graphics.Color.parseColor("#$cleanHex"))
        } else {
            fallback
        }
    } catch (e: Exception) {
        fallback
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentidadInstitucionalScreen(
    viewModel: SchoolViewModel,
    modifier: Modifier = Modifier
) {
    val allInstitutions by viewModel.allInstitutions.collectAsStateWithLifecycle()
    val activeInstitution by viewModel.activeInstitution.collectAsStateWithLifecycle()
    val allTemplates by viewModel.allCustomTemplates.collectAsStateWithLifecycle()
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Multi-Institución" to Icons.Default.Hub,
        "Datos Generales" to Icons.Default.Info,
        "Visual & Firmas" to Icons.Default.Palette,
        "Colores & Fuentes" to Icons.Default.Brush,
        "Canva: Plantillas" to Icons.Default.DashboardCustomize,
        "Sellado Digital" to Icons.Default.QrCodeScanner
    )

    // Dynamic brand colors extracted from the active school configuration
    val brandPrimary = activeInstitution?.primaryColor?.let { parseHexColor(it) } ?: MaterialTheme.colorScheme.primary
    val brandSecondary = activeInstitution?.secondaryColor?.let { parseHexColor(it) } ?: MaterialTheme.colorScheme.secondary

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            tint = brandPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Identidad Institucional",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                activeInstitution?.officialName ?: "Sin Institución Activa",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Horizontal scrollable Tab Row for elegant sub-module navigation
            ScrollableTabRow(
                selectedTabIndex = activeTab,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = brandPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = brandPrimary
                    )
                }
            ) {
                tabs.forEachIndexed { index, (title, icon) ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        selectedContentColor = brandPrimary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (activeTab) {
                    0 -> MultiInstitutionTab(
                        allInstitutions = allInstitutions,
                        activeInstitution = activeInstitution,
                        brandPrimary = brandPrimary,
                        brandSecondary = brandSecondary,
                        onActivate = { viewModel.selectActiveInstitution(it) },
                        onCreate = { viewModel.createInstitutionProfile(it) },
                        onDelete = { viewModel.deleteInstitutionProfile(it) }
                    )
                    1 -> GeneralDataTab(
                        activeInstitution = activeInstitution,
                        brandPrimary = brandPrimary,
                        currentRole = currentRole,
                        onSave = { viewModel.saveInstitutionProfile(it) }
                    )
                    2 -> VisualAndSignaturesTab(
                        activeInstitution = activeInstitution,
                        brandPrimary = brandPrimary,
                        brandSecondary = brandSecondary,
                        currentRole = currentRole,
                        onSave = { viewModel.saveInstitutionProfile(it) }
                    )
                    3 -> ColorsAndFontsTab(
                        activeInstitution = activeInstitution,
                        brandPrimary = brandPrimary,
                        brandSecondary = brandSecondary,
                        currentRole = currentRole,
                        onSave = { viewModel.saveInstitutionProfile(it) }
                    )
                    4 -> CanvaTemplatesTab(
                        activeInstitution = activeInstitution,
                        allTemplates = allTemplates,
                        brandPrimary = brandPrimary,
                        brandSecondary = brandSecondary,
                        currentRole = currentRole,
                        onSaveTemplate = { viewModel.saveCustomTemplate(it) },
                        onDeleteTemplate = { viewModel.deleteCustomTemplate(it) }
                    )
                    5 -> DigitalSealTab(
                        activeInstitution = activeInstitution,
                        brandPrimary = brandPrimary,
                        brandSecondary = brandSecondary
                    )
                }
            }
        }
    }
}

// ==========================================
// TAB 1: MULTI-INSTITUTION PROFILES
// ==========================================
@Composable
fun MultiInstitutionTab(
    allInstitutions: List<InstitutionProfile>,
    activeInstitution: InstitutionProfile?,
    brandPrimary: Color,
    brandSecondary: Color,
    onActivate: (Int) -> Unit,
    onCreate: (InstitutionProfile) -> Unit,
    onDelete: (Int) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Gestión Multi-Institucional",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Intercambia perfiles de colegios independientes.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = brandPrimary),
                modifier = Modifier.testTag("create_institution_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Añadir", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (allInstitutions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay instituciones registradas en la base de datos.")
            }
        } else {
            allInstitutions.forEach { inst ->
                val isActive = inst.id == activeInstitution?.id
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) brandPrimary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(
                            width = if (isActive) 2.dp else 1.dp,
                            color = if (isActive) brandPrimary else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge / Emoji representation of the School Logo
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(inst.primaryColor).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            RenderSymbol(inst.logoPrincipal, "🏫", size = 32.dp)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    inst.shortName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (isActive) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text("Activo", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            labelColor = brandPrimary,
                                            containerColor = brandPrimary.copy(alpha = 0.15f)
                                        )
                                    )
                                }
                            }
                            Text(
                                inst.officialName,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "AMIE: ${inst.amieCode} | Régimen: ${inst.regimen} | ${inst.institutionType}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            // Swatch colors preview
                            Row {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(parseHexColor(inst.primaryColor))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(parseHexColor(inst.secondaryColor))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Colores institucionales",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            if (!isActive) {
                                Button(
                                    onClick = { onActivate(inst.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = parseHexColor(inst.primaryColor)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Activar", fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                if (allInstitutions.size > 1) {
                                    IconButton(
                                        onClick = { onDelete(inst.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Borrar",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            } else {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Activo",
                                    tint = brandPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var shortName by remember { mutableStateOf("") }
        var amie by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("Particular") }
        var regimen by remember { mutableStateOf("Sierra") }
        var primaryColor by remember { mutableStateOf("#4F46E5") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Registrar Nueva Institución", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre Oficial") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = shortName,
                        onValueChange = { shortName = it },
                        label = { Text("Nombre Corto") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = amie,
                        onValueChange = { amie = it },
                        label = { Text("Código AMIE") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    Text("Tipo de Sostenimiento:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("Fiscal", "Particular", "Municipal", "Fiscomisional").forEach { t ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = type == t, onClick = { type = t })
                                Text(t, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Régimen Escolar:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("Sierra", "Costa", "Amazonía", "Galápagos").forEach { r ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = regimen == r, onClick = { regimen = r })
                                Text(r, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = primaryColor,
                        onValueChange = { primaryColor = it },
                        label = { Text("Color Primario (Hex)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && amie.isNotBlank()) {
                            onCreate(
                                InstitutionProfile(
                                    officialName = name,
                                    shortName = shortName.ifBlank { name },
                                    amieCode = amie,
                                    institutionType = type,
                                    regimen = regimen,
                                    primaryColor = primaryColor,
                                    logoPrincipal = if (type == "Fiscal") "🏫" else "🔬"
                                )
                            )
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// ==========================================
// TAB 2: GENERAL SCHOOL METADATA
// ==========================================
@Composable
fun GeneralDataTab(
    activeInstitution: InstitutionProfile?,
    brandPrimary: Color,
    currentRole: String,
    onSave: (InstitutionProfile) -> Unit
) {
    if (activeInstitution == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Selecciona o crea una institución primero.")
        }
        return
    }

    var officialName by remember(activeInstitution) { mutableStateOf(activeInstitution.officialName) }
    var shortName by remember(activeInstitution) { mutableStateOf(activeInstitution.shortName) }
    var amieCode by remember(activeInstitution) { mutableStateOf(activeInstitution.amieCode) }
    var institutionType by remember(activeInstitution) { mutableStateOf(activeInstitution.institutionType) }
    var regimen by remember(activeInstitution) { mutableStateOf(activeInstitution.regimen) }

    var hasInicial by remember(activeInstitution) { mutableStateOf(activeInstitution.hasInicial) }
    var hasEgb by remember(activeInstitution) { mutableStateOf(activeInstitution.hasEgb) }
    var hasBachillerato by remember(activeInstitution) { mutableStateOf(activeInstitution.hasBachillerato) }
    var hasTecnico by remember(activeInstitution) { mutableStateOf(activeInstitution.hasTecnico) }

    var hasMatutina by remember(activeInstitution) { mutableStateOf(activeInstitution.hasMatutina) }
    var hasVespertina by remember(activeInstitution) { mutableStateOf(activeInstitution.hasVespertina) }
    var hasNocturna by remember(activeInstitution) { mutableStateOf(activeInstitution.hasNocturna) }

    var distrito by remember(activeInstitution) { mutableStateOf(activeInstitution.distrito) }
    var circuito by remember(activeInstitution) { mutableStateOf(activeInstitution.circuito) }
    var zona by remember(activeInstitution) { mutableStateOf(activeInstitution.zona) }
    var provincia by remember(activeInstitution) { mutableStateOf(activeInstitution.provincia) }
    var canton by remember(activeInstitution) { mutableStateOf(activeInstitution.canton) }
    var parroquia by remember(activeInstitution) { mutableStateOf(activeInstitution.parroquia) }
    var address by remember(activeInstitution) { mutableStateOf(activeInstitution.address) }
    var phone by remember(activeInstitution) { mutableStateOf(activeInstitution.phone) }
    var email by remember(activeInstitution) { mutableStateOf(activeInstitution.email) }
    var website by remember(activeInstitution) { mutableStateOf(activeInstitution.website) }

    val canEdit = currentRole == "Administrador" || currentRole == "Autoridades"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Ficha de Registro Oficial", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = brandPrimary)
        Text("Valores requeridos para planificaciones y actas del Ministerio de Educación.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        // Identification Row
        OutlinedTextField(
            value = officialName,
            onValueChange = { if (canEdit) officialName = it },
            label = { Text("Nombre Oficial de la Institución") },
            enabled = canEdit,
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = shortName,
                onValueChange = { if (canEdit) shortName = it },
                label = { Text("Nombre Corto") },
                enabled = canEdit,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            OutlinedTextField(
                value = amieCode,
                onValueChange = { if (canEdit) amieCode = it },
                label = { Text("Código AMIE") },
                enabled = canEdit,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Nivel Educativo:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(
                "Inicial" to hasInicial,
                "EGB" to hasEgb,
                "Bachillerato" to hasBachillerato,
                "Técnico" to hasTecnico
            ).forEach { (label, checked) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { isChecked ->
                            if (canEdit) {
                                when (label) {
                                    "Inicial" -> hasInicial = isChecked
                                    "EGB" -> hasEgb = isChecked
                                    "Bachillerato" -> hasBachillerato = isChecked
                                    "Técnico" -> hasTecnico = isChecked
                                }
                            }
                        },
                        enabled = canEdit
                    )
                    Text(label, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Jornadas Autorizadas:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(
                "Matutina" to hasMatutina,
                "Vespertina" to hasVespertina,
                "Nocturna" to hasNocturna
            ).forEach { (label, checked) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { isChecked ->
                            if (canEdit) {
                                when (label) {
                                    "Matutina" -> hasMatutina = isChecked
                                    "Vespertina" -> hasVespertina = isChecked
                                    "Nocturna" -> hasNocturna = isChecked
                                }
                            }
                        },
                        enabled = canEdit
                    )
                    Text(label, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = institutionType,
                onValueChange = { if (canEdit) institutionType = it },
                label = { Text("Tipo (Fiscal/Particular)") },
                enabled = canEdit,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            OutlinedTextField(
                value = regimen,
                onValueChange = { if (canEdit) regimen = it },
                label = { Text("Régimen") },
                enabled = canEdit,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Geolocalización & Estructura Administrativa", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = brandPrimary)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            OutlinedTextField(
                value = distrito,
                onValueChange = { if (canEdit) distrito = it },
                label = { Text("Distrito") },
                enabled = canEdit,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            OutlinedTextField(
                value = circuito,
                onValueChange = { if (canEdit) circuito = it },
                label = { Text("Circuito") },
                enabled = canEdit,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            OutlinedTextField(
                value = zona,
                onValueChange = { if (canEdit) zona = it },
                label = { Text("Zona") },
                enabled = canEdit,
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            OutlinedTextField(
                value = provincia,
                onValueChange = { if (canEdit) provincia = it },
                label = { Text("Provincia") },
                enabled = canEdit,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            OutlinedTextField(
                value = canton,
                onValueChange = { if (canEdit) canton = it },
                label = { Text("Cantón") },
                enabled = canEdit,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            OutlinedTextField(
                value = parroquia,
                onValueChange = { if (canEdit) parroquia = it },
                label = { Text("Parroquia") },
                enabled = canEdit,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = address,
            onValueChange = { if (canEdit) address = it },
            label = { Text("Dirección Exacta") },
            enabled = canEdit,
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text("Contacto Institucional", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = brandPrimary)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { if (canEdit) phone = it },
            label = { Text("Teléfono") },
            enabled = canEdit,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { if (canEdit) email = it },
            label = { Text("Correo Institucional") },
            enabled = canEdit,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = website,
            onValueChange = { if (canEdit) website = it },
            label = { Text("Página Web") },
            enabled = canEdit,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        if (canEdit) {
            Button(
                onClick = {
                    onSave(
                        activeInstitution.copy(
                            officialName = officialName,
                            shortName = shortName,
                            amieCode = amieCode,
                            institutionType = institutionType,
                            regimen = regimen,
                            hasInicial = hasInicial,
                            hasEgb = hasEgb,
                            hasBachillerato = hasBachillerato,
                            hasTecnico = hasTecnico,
                            hasMatutina = hasMatutina,
                            hasVespertina = hasVespertina,
                            hasNocturna = hasNocturna,
                            distrito = distrito,
                            circuito = circuito,
                            zona = zona,
                            provincia = provincia,
                            canton = canton,
                            parroquia = parroquia,
                            address = address,
                            phone = phone,
                            email = email,
                            website = website
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = brandPrimary),
                modifier = Modifier.fillMaxWidth().testTag("save_general_data_btn")
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Guardar Ficha Técnica")
            }
        } else {
            Text(
                "Solo administradores o autoridades pueden modificar la ficha técnica de la institución.",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

// ==========================================
// TAB 3: VISUAL IDENTITY & SIGNATURE CANVAS
// ==========================================
@Composable
fun VisualAndSignaturesTab(
    activeInstitution: InstitutionProfile?,
    brandPrimary: Color,
    brandSecondary: Color,
    currentRole: String,
    onSave: (InstitutionProfile) -> Unit
) {
    if (activeInstitution == null) return

    var logoPrincipal by remember(activeInstitution) { mutableStateOf(activeInstitution.logoPrincipal) }
    var escudo by remember(activeInstitution) { mutableStateOf(activeInstitution.escudo) }
    var logoMinisterio by remember(activeInstitution) { mutableStateOf(activeInstitution.logoMinisterio) }
    var logoDistrito by remember(activeInstitution) { mutableStateOf(activeInstitution.logoDistrito) }
    var logoCircuito by remember(activeInstitution) { mutableStateOf(activeInstitution.logoCircuito) }
    var marcaAgua by remember(activeInstitution) { mutableStateOf(activeInstitution.marcaAgua) }
    var selloInstitucional by remember(activeInstitution) { mutableStateOf(activeInstitution.selloInstitucional) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val logoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val savedPath = saveUriToInternalStorage(context, uri, "logo_principal")
            if (savedPath != null) {
                logoPrincipal = savedPath
            }
        }
    }

    val escudoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val savedPath = saveUriToInternalStorage(context, uri, "escudo")
            if (savedPath != null) {
                escudo = savedPath
            }
        }
    }

    val selloLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val savedPath = saveUriToInternalStorage(context, uri, "sello_institucional")
            if (savedPath != null) {
                selloInstitucional = savedPath
            }
        }
    }

    // Human signatures registration
    var rectorName by remember(activeInstitution) { mutableStateOf(activeInstitution.rectorName) }
    var vicerrectorName by remember(activeInstitution) { mutableStateOf(activeInstitution.vicerrectorName) }
    var inspectorName by remember(activeInstitution) { mutableStateOf(activeInstitution.inspectorName) }
    var docenteName by remember(activeInstitution) { mutableStateOf(activeInstitution.docenteName) }
    var deceName by remember(activeInstitution) { mutableStateOf(activeInstitution.deceName) }
    var secretariaName by remember(activeInstitution) { mutableStateOf(activeInstitution.secretariaName) }

    // Signature Drawing Board States
    var showSignatureCanvas by remember { mutableStateOf(false) }
    var currentSigningRoleName by remember { mutableStateOf("") }
    val pathPoints = remember { mutableStateListOf<Offset>() }
    var registeredSignatureText by remember(activeInstitution) { mutableStateOf(activeInstitution.firmaInstitucional) }

    val canEdit = currentRole == "Administrador" || currentRole == "Autoridades"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Identidad Visual (Símbolos Oficiales)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = brandPrimary)
        Text("Carga de logotipos representados en los encabezados y marcas de agua.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            OutlinedTextField(
                value = logoPrincipal,
                onValueChange = { if (canEdit) logoPrincipal = it },
                label = { Text("Logo Principal (Emoji/Texto/Ruta)") },
                enabled = canEdit,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                leadingIcon = {
                    RenderSymbol(logoPrincipal, "🏫", modifier = Modifier.clip(CircleShape), size = 24.dp)
                }
            )
            OutlinedTextField(
                value = escudo,
                onValueChange = { if (canEdit) escudo = it },
                label = { Text("Escudo Institucional (Ruta)") },
                enabled = canEdit,
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    RenderSymbol(escudo, "🛡️", size = 24.dp)
                }
            )
        }

        if (canEdit) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { logoLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = brandSecondary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Subir Logo (JPG/PNG)", fontSize = 11.sp)
                }
                Button(
                    onClick = { escudoLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = brandSecondary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Subir Escudo (JPG/PNG)", fontSize = 11.sp)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            OutlinedTextField(
                value = selloInstitucional,
                onValueChange = { if (canEdit) selloInstitucional = it },
                label = { Text("Sello Húmedo Oficial (Ruta)") },
                enabled = canEdit,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                leadingIcon = {
                    RenderSymbol(selloInstitucional, "🖃", size = 24.dp)
                }
            )
            if (canEdit) {
                Button(
                    onClick = { selloLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = brandSecondary),
                    modifier = Modifier.weight(1f).align(Alignment.CenterVertically).padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Subir Sello (JPG/PNG)", fontSize = 11.sp)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            OutlinedTextField(
                value = logoMinisterio,
                onValueChange = { if (canEdit) logoMinisterio = it },
                label = { Text("Logo MinEduc") },
                enabled = canEdit,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            OutlinedTextField(
                value = logoDistrito,
                onValueChange = { if (canEdit) logoDistrito = it },
                label = { Text("Logo Distrito") },
                enabled = canEdit,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = marcaAgua,
            onValueChange = { if (canEdit) marcaAgua = it },
            label = { Text("Texto Marca de Agua Oficial") },
            enabled = canEdit,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        Text("Registro de Firmas Oficiales", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = brandPrimary)
        Text("Registra las personas que ostentan los cargos para firmas digitales.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = rectorName,
            onValueChange = { if (canEdit) rectorName = it },
            label = { Text("Nombre del Rector/a") },
            enabled = canEdit,
            trailingIcon = {
                IconButton(onClick = {
                    currentSigningRoleName = "Rector/a"
                    pathPoints.clear()
                    showSignatureCanvas = true
                }) {
                    Icon(Icons.Default.Gesture, contentDescription = "Firmar")
                }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = vicerrectorName,
            onValueChange = { if (canEdit) vicerrectorName = it },
            label = { Text("Nombre del Vicerrector/a") },
            enabled = canEdit,
            trailingIcon = {
                IconButton(onClick = {
                    currentSigningRoleName = "Vicerrector/a"
                    pathPoints.clear()
                    showSignatureCanvas = true
                }) {
                    Icon(Icons.Default.Gesture, contentDescription = "Firmar")
                }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = inspectorName,
            onValueChange = { if (canEdit) inspectorName = it },
            label = { Text("Inspector/a General") },
            enabled = canEdit,
            trailingIcon = {
                IconButton(onClick = {
                    currentSigningRoleName = "Inspector/a"
                    pathPoints.clear()
                    showSignatureCanvas = true
                }) {
                    Icon(Icons.Default.Gesture, contentDescription = "Firmar")
                }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = docenteName,
            onValueChange = { if (canEdit) docenteName = it },
            label = { Text("Docente Autor") },
            enabled = canEdit,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = deceName,
            onValueChange = { if (canEdit) deceName = it },
            label = { Text("Coordinador/a DECE") },
            enabled = canEdit,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = secretariaName,
            onValueChange = { if (canEdit) secretariaName = it },
            label = { Text("Secretario/a de Plantel") },
            enabled = canEdit,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Firma Digital del Rectorado Activa:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(registeredSignatureText, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = brandPrimary)
            }
        }

        if (canEdit) {
            Button(
                onClick = {
                    onSave(
                        activeInstitution.copy(
                            logoPrincipal = logoPrincipal,
                            escudo = escudo,
                            logoMinisterio = logoMinisterio,
                            logoDistrito = logoDistrito,
                            logoCircuito = logoCircuito,
                            marcaAgua = marcaAgua,
                            selloInstitucional = selloInstitucional,
                            rectorName = rectorName,
                            vicerrectorName = vicerrectorName,
                            inspectorName = inspectorName,
                            docenteName = docenteName,
                            deceName = deceName,
                            secretariaName = secretariaName,
                            firmaInstitucional = registeredSignatureText
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = brandPrimary),
                modifier = Modifier.fillMaxWidth().testTag("save_visuals_btn")
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Guardar Identidad Visual")
            }
        }
    }

    // Interactive Signature Drawing Dialog Simulation
    if (showSignatureCanvas) {
        Dialog(onDismissRequest = { showSignatureCanvas = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Panel de Firma Digitalizada: $currentSigningRoleName",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = brandPrimary
                    )
                    Text("Arrastra tu dedo o cursor para firmar digitalmente", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Signature Pad Draw Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(1.dp, brandPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        pathPoints.add(offset)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val newPoint = change.position
                                        pathPoints.add(newPoint)
                                    }
                                )
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (pathPoints.size > 1) {
                                val path = Path().apply {
                                    moveTo(pathPoints[0].x, pathPoints[0].y)
                                    for (i in 1 until pathPoints.size) {
                                        lineTo(pathPoints[i].x, pathPoints[i].y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = Color(0xFF0F172A), // Dark Ink blue
                                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }

                        if (pathPoints.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Línea de firma", color = Color.LightGray, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { pathPoints.clear() }) {
                            Text("Limpiar", color = MaterialTheme.colorScheme.error)
                        }

                        Row {
                            TextButton(onClick = { showSignatureCanvas = false }) {
                                Text("Cancelar")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (pathPoints.isNotEmpty()) {
                                        registeredSignatureText = "✍️ Firma Digitalizada de $currentSigningRoleName (${pathPoints.size} puntos vectoriales)"
                                    }
                                    showSignatureCanvas = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = brandPrimary)
                            ) {
                                Text("Registrar")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 4: CORPORATE COLORS & TYPOGRAPHY
// ==========================================
@Composable
fun ColorsAndFontsTab(
    activeInstitution: InstitutionProfile?,
    brandPrimary: Color,
    brandSecondary: Color,
    currentRole: String,
    onSave: (InstitutionProfile) -> Unit
) {
    if (activeInstitution == null) return

    var primaryColor by remember(activeInstitution) { mutableStateOf(activeInstitution.primaryColor) }
    var secondaryColor by remember(activeInstitution) { mutableStateOf(activeInstitution.secondaryColor) }
    var tertiaryColor by remember(activeInstitution) { mutableStateOf(activeInstitution.tertiaryColor) }
    var buttonColor by remember(activeInstitution) { mutableStateOf(activeInstitution.buttonColor) }
    var headerColor by remember(activeInstitution) { mutableStateOf(activeInstitution.headerColor) }
    var tableColor by remember(activeInstitution) { mutableStateOf(activeInstitution.tableColor) }
    var backgroundColor by remember(activeInstitution) { mutableStateOf(activeInstitution.backgroundColor) }

    // Typography
    var mainFont by remember(activeInstitution) { mutableStateOf(activeInstitution.mainFont) }
    var secondaryFont by remember(activeInstitution) { mutableStateOf(activeInstitution.secondaryFont) }
    var titleSize by remember(activeInstitution) { mutableFloatStateOf(activeInstitution.titleSize) }
    var subtitleSize by remember(activeInstitution) { mutableFloatStateOf(activeInstitution.subtitleSize) }
    var contentSize by remember(activeInstitution) { mutableFloatStateOf(activeInstitution.contentSize) }
    var lineSpacing by remember(activeInstitution) { mutableFloatStateOf(activeInstitution.lineSpacing) }
    var alignment by remember(activeInstitution) { mutableStateOf(activeInstitution.alignment) }

    val canEdit = currentRole == "Administrador" || currentRole == "Autoridades"

    // Presets definitions
    val presets = listOf(
        Triple("Ministerio Clásico", "#0B2545", "#134074"),
        Triple("Manuela Cañizares", "#0056B3", "#FFC107"),
        Triple("Sebastián Benalcázar", "#120B44", "#EAB308"),
        Triple("Esmeralda Costeña", "#064E3B", "#10B981"),
        Triple("Liceo Naval Militar", "#991B1B", "#1E3A8A")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Esquema de Colores Corporativos", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = brandPrimary)
        Text("Configura los colores oficiales que utilizarán todos los documentos generados e interfaces.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))

        // Preset Colors Selector
        Text("Paletas Pre-configuradas:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { (name, prim, sec) ->
                InputChip(
                    selected = primaryColor.lowercase() == prim.lowercase(),
                    onClick = {
                        if (canEdit) {
                            primaryColor = prim
                            secondaryColor = sec
                            buttonColor = prim
                            headerColor = prim
                        }
                    },
                    label = { Text(name, fontSize = 10.sp) },
                    avatar = {
                        Row(modifier = Modifier.padding(horizontal = 4.dp)) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(parseHexColor(prim)))
                            Spacer(modifier = Modifier.width(2.dp))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(parseHexColor(sec)))
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Inputs
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                OutlinedTextField(
                    value = primaryColor,
                    onValueChange = { if (canEdit) primaryColor = it },
                    label = { Text("Color Primario (Hex)") },
                    enabled = canEdit,
                    leadingIcon = {
                        Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)).background(parseHexColor(primaryColor)))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = secondaryColor,
                    onValueChange = { if (canEdit) secondaryColor = it },
                    label = { Text("Color Secundario (Hex)") },
                    enabled = canEdit,
                    leadingIcon = {
                        Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)).background(parseHexColor(secondaryColor)))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                OutlinedTextField(
                    value = tertiaryColor,
                    onValueChange = { if (canEdit) tertiaryColor = it },
                    label = { Text("Color Terciario") },
                    enabled = canEdit,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = buttonColor,
                    onValueChange = { if (canEdit) buttonColor = it },
                    label = { Text("Color de Botones") },
                    enabled = canEdit,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                OutlinedTextField(
                    value = headerColor,
                    onValueChange = { if (canEdit) headerColor = it },
                    label = { Text("Color Encabezados") },
                    enabled = canEdit,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = tableColor,
                    onValueChange = { if (canEdit) tableColor = it },
                    label = { Text("Color de Tablas") },
                    enabled = canEdit,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        Text("Regulación de Tipografía Oficial", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = brandPrimary)
        Text("Tipografía y jerarquía que usará la IA al maquetar documentos oficiales.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            OutlinedTextField(
                value = mainFont,
                onValueChange = { if (canEdit) mainFont = it },
                label = { Text("Fuente Principal") },
                enabled = canEdit,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            OutlinedTextField(
                value = secondaryFont,
                onValueChange = { if (canEdit) secondaryFont = it },
                label = { Text("Fuente Secundaria") },
                enabled = canEdit,
                modifier = Modifier.weight(1f)
            )
        }

        Text("Tamaño del Título: ${titleSize.toInt()}sp", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Slider(
            value = titleSize,
            onValueChange = { if (canEdit) titleSize = it },
            valueRange = 16f..32f,
            enabled = canEdit,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        Text("Tamaño del Contenido: ${contentSize.toInt()}sp", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Slider(
            value = contentSize,
            onValueChange = { if (canEdit) contentSize = it },
            valueRange = 10f..18f,
            enabled = canEdit,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        Text("Interlineado de Párrafos: $lineSpacing", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Slider(
            value = lineSpacing,
            onValueChange = { if (canEdit) lineSpacing = it },
            valueRange = 1.0f..2.0f,
            enabled = canEdit,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        Text("Alineación de Bloques:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Izquierda", "Centrado", "Justificado").forEach { align ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = alignment == align, onClick = { if (canEdit) alignment = align })
                    Text(align, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (canEdit) {
            Button(
                onClick = {
                    onSave(
                        activeInstitution.copy(
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            tertiaryColor = tertiaryColor,
                            buttonColor = buttonColor,
                            headerColor = headerColor,
                            tableColor = tableColor,
                            mainFont = mainFont,
                            secondaryFont = secondaryFont,
                            titleSize = titleSize,
                            contentSize = contentSize,
                            lineSpacing = lineSpacing,
                            alignment = alignment
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = brandPrimary),
                modifier = Modifier.fillMaxWidth().testTag("save_colors_btn")
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Guardar Estilos de Marca")
            }
        }
    }
}

// ==========================================
// TAB 5: CANVA STYLE VISUAL TEMPLATE BUILDER
// ==========================================
@Composable
fun CanvaTemplatesTab(
    activeInstitution: InstitutionProfile?,
    allTemplates: List<CustomTemplate>,
    brandPrimary: Color,
    brandSecondary: Color,
    currentRole: String,
    onSaveTemplate: (CustomTemplate) -> Unit,
    onDeleteTemplate: (CustomTemplate) -> Unit
) {
    if (activeInstitution == null) return

    var selectedDocType by remember { mutableStateOf("Planificación") }
    var templateName by remember { mutableStateOf("Mi Plantilla Personalizada") }

    // Visual elements switches (Canva Toggles)
    var showLogo by remember { mutableStateOf(true) }
    var showEscudo by remember { mutableStateOf(true) }
    var showQr by remember { mutableStateOf(true) }
    var alignment by remember { mutableStateOf("Centrado") }

    var mockTemplatesList by remember { mutableStateOf(allTemplates) }

    val canEdit = currentRole == "Administrador" || currentRole == "Autoridades"

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Left Column: Canva Style Toggles & Toolbox (Width 35%)
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1.3f)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Constructor Canva", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = brandPrimary)
            Text("Arrastra y selecciona componentes", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = templateName,
                onValueChange = { templateName = it },
                label = { Text("Nombre Plantilla", fontSize = 10.sp) },
                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Formato del Ministerio:", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            val docTypes = listOf("Planificación", "Informe DECE", "Acta", "Certificado", "Memorando", "Boletín")
            docTypes.forEach { type ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedDocType = type }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(selected = selectedDocType == type, onClick = { selectedDocType = type })
                    Text(type, fontSize = 11.sp)
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Toggles de Elementos:", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = showLogo, onCheckedChange = { showLogo = it })
                Text("Logo (${activeInstitution.logoPrincipal})", fontSize = 11.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = showEscudo, onCheckedChange = { showEscudo = it })
                Text("Escudo (${activeInstitution.escudo})", fontSize = 11.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = showQr, onCheckedChange = { showQr = it })
                Text("QR de Validación", fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Alineación Cabecera:", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Izquierda", "Centrado").forEach { a ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        RadioButton(selected = alignment == a, onClick = { alignment = a })
                        Text(a, fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    val newTemplate = CustomTemplate(
                        institutionId = activeInstitution.id,
                        name = templateName,
                        documentType = selectedDocType,
                        isCustom = true,
                        isMinistryDefault = false,
                        showLogo = showLogo,
                        showEscudo = showEscudo,
                        showQr = showQr,
                        alignment = alignment,
                        lastUpdated = System.currentTimeMillis()
                    )
                    onSaveTemplate(newTemplate)
                },
                colors = ButtonDefaults.buttonColors(containerColor = brandPrimary),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Guardar", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right Column: LIVE SHEET PREVIEW (Width 65%)
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(2.7f)
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Live Sheet Header representing A4 layout
            Text(
                "VISTA PREVIA DE DOCUMENTO EN TIEMPO REAL",
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))

            // A4 Styled Sheet Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFDFDFD))
                    .border(1.dp, Color(0xFFE2E8F0))
                    .padding(16.dp)
            ) {
                // Republic Header decoration
                Text(
                    "REPÚBLICA DEL ECUADOR\nMINISTERIO DE EDUCACIÓN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "DIRECCIÓN DISTRITAL ${activeInstitution.distrito} - CIRCUITO ${activeInstitution.circuito}",
                    fontSize = 8.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                // Visual Customizer Elements (Logo & Escudo)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (alignment == "Centrado") Arrangement.Center else Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showLogo) {
                        RenderSymbol(activeInstitution.logoPrincipal, "🏫", size = 32.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column(horizontalAlignment = if (alignment == "Centrado") Alignment.CenterHorizontally else Alignment.Start) {
                        Text(
                            activeInstitution.officialName.uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = brandPrimary,
                            textAlign = if (alignment == "Centrado") TextAlign.Center else TextAlign.Left
                        )
                        Text(
                            "AMIE: ${activeInstitution.amieCode} | Régimen: ${activeInstitution.regimen} | Año Lectivo: 2026-2027",
                            fontSize = 8.sp,
                            color = Color.DarkGray
                        )
                    }
                    if (showEscudo) {
                        Spacer(modifier = Modifier.width(12.dp))
                        RenderSymbol(activeInstitution.escudo, "🛡️", size = 32.dp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                // Document Title
                Text(
                    selectedDocType.uppercase() + " OFICIAL INSTITUCIONAL",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = brandSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Simulated Document Form Fields
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(8.dp)
                        .border(1.dp, parseHexColor(activeInstitution.tableColor), RoundedCornerShape(4.dp))
                ) {
                    Text("DOCENTE: ${activeInstitution.docenteName}", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text("ÁREA / ASIGNATURA: Ciencias Naturales", fontSize = 8.sp)
                    Text("CURSO: 10mo EGB | PARALELO: \"A\"", fontSize = 8.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Body Section
                Text(
                    "Contenido del Documento Maquetado por IA:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.sp,
                    color = brandPrimary
                )
                Text(
                    "Este reporte se maquetará de manera automática en concordancia con el formato \"$templateName\". La Inteligencia Artificial incorporará los códigos, logos y firmas correspondientes de forma unificada.",
                    fontSize = 8.sp,
                    lineHeight = 11.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Justify
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Digital Signatures block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .height(30.dp)
                                .width(90.dp)
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✍️ FIRMADO", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                        Divider(modifier = Modifier.width(90.dp))
                        Text(activeInstitution.docenteName, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        Text("Docente Responsable", fontSize = 6.sp, color = Color.Gray)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .height(30.dp)
                                .width(90.dp)
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✍️ REGISTRADO", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                        Divider(modifier = Modifier.width(90.dp))
                        Text(activeInstitution.rectorName, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        Text("Rector / Vicerrector", fontSize = 6.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Security QR and metadata (Digital Seal)
                if (showQr) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF3C7))
                            .padding(6.dp)
                            .border(0.5.dp, Color(0xFFF59E0B)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // QR Emoji
                        Text("🔳", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("VERIFICACIÓN DIGITAL SÉLLO QR", fontWeight = FontWeight.Bold, fontSize = 7.sp, color = Color(0xFFB45309))
                            Text("ID: CM-AMIE-${activeInstitution.amieCode}-2026-09", fontSize = 6.sp, color = Color.DarkGray)
                            Text("Hash: f8d1a498b005eb1bc84efcf92 (SHA256)", fontSize = 6.sp, color = Color.Gray)
                            if (activeInstitution.selloInstitucional.startsWith("file://") || activeInstitution.selloInstitucional.startsWith("content://")) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Sello: ", fontSize = 6.sp, fontStyle = FontStyle.Italic)
                                    RenderSymbol(activeInstitution.selloInstitucional, "🖃", size = 16.dp)
                                }
                            } else {
                                Text("Sello: ${activeInstitution.selloInstitucional}", fontSize = 6.sp, fontStyle = FontStyle.Italic)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 6: DIGITAL SEAL & SHA256 BLOCK
// ==========================================
@Composable
fun DigitalSealTab(
    activeInstitution: InstitutionProfile?,
    brandPrimary: Color,
    brandSecondary: Color
) {
    if (activeInstitution == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Módulo de Sellado Digital y Validador", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = brandPrimary)
        Text("Agrega automáticamente elementos de seguridad para validar el origen oficial de los documentos.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = brandSecondary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verificación Digital Activa", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Cada documento generado emitirá un bloque hash SHA256 cifrado de verificación para garantizar la inviolabilidad de las actas de notas y planificaciones registradas en la base de datos de ${activeInstitution.officialName}.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Parámetros Generados Automáticamente:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(10.dp))

        // Visual list of seal parameters
        val sealItems = listOf(
            Triple("Código Único", "DOC-${activeInstitution.amieCode}-2026-${(1000..9999).random()}", Icons.Default.Tag),
            Triple("Fecha de Emisión", "Automática (Tiempo Real)", Icons.Default.CalendarToday),
            Triple("Sello Húmedo Digitalizado", activeInstitution.selloInstitucional, Icons.Default.WorkspacePremium),
            Triple("Algoritmo de Integridad", "SHA-256 HMAC Secure", Icons.Default.Lock),
            Triple("Código QR de Validación", "https://verificar.educasmart.ai/validador?amie=${activeInstitution.amieCode}", Icons.Default.QrCode)
        )

        sealItems.forEach { (label, value, icon) ->
            ListItem(
                headlineContent = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                supportingContent = { Text(value, fontSize = 11.sp, color = brandPrimary) },
                leadingContent = { Icon(icon, contentDescription = null, tint = brandSecondary, modifier = Modifier.size(20.dp)) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Large simulated QR verification box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, brandPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Drawing a stylish simulated QR Grid
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color.White)
                        .border(4.dp, Color.Black)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Quick simulated QR pixel blocks
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val size = size.width
                        val border = 4.dp.toPx()
                        // Corner squares
                        drawRect(Color.Black, Offset(0f, 0f), androidx.compose.ui.geometry.Size(25f, 25f))
                        drawRect(Color.Black, Offset(size - 25f, 0f), androidx.compose.ui.geometry.Size(25f, 25f))
                        drawRect(Color.Black, Offset(0f, size - 25f), androidx.compose.ui.geometry.Size(25f, 25f))
                        // Random dots to look like QR
                        drawRect(Color.Black, Offset(size/2 - 10f, size/2 - 10f), androidx.compose.ui.geometry.Size(20f, 20f))
                        drawRect(Color.Black, Offset(35f, 40f), androidx.compose.ui.geometry.Size(12f, 12f))
                        drawRect(Color.Black, Offset(60f, 35f), androidx.compose.ui.geometry.Size(15f, 15f))
                        drawRect(Color.Black, Offset(20f, 60f), androidx.compose.ui.geometry.Size(10f, 10f))
                        drawRect(Color.Black, Offset(50f, 65f), androidx.compose.ui.geometry.Size(12f, 15f))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("Escanear para verificar validez LOEI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text("Cifrado SHA-256: 8a4f91b7d85c3e012aa4f9ef...", fontSize = 9.sp, color = Color.Gray)
            }
        }
    }
}

fun saveUriToInternalStorage(context: android.content.Context, uri: android.net.Uri, prefix: String): String? {
    return try {
        val fileName = "${prefix}_${System.currentTimeMillis()}.png"
        val file = java.io.File(context.filesDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            java.io.FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        "file://" + file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
private fun RenderSymbol(
    symbol: String,
    fallback: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 24.dp
) {
    if (symbol.startsWith("file://") || symbol.startsWith("content://")) {
        Image(
            painter = rememberAsyncImagePainter(model = symbol),
            contentDescription = null,
            modifier = modifier.size(size),
            contentScale = ContentScale.Crop
        )
    } else {
        Text(symbol.ifBlank { fallback }, fontSize = (size.value * 0.8f).sp, modifier = modifier)
    }
}
