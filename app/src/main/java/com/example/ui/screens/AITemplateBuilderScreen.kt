package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Image
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.GeminiClient
import com.example.data.database.CustomTemplate
import com.example.data.database.InstitutionProfile
import com.example.ui.viewmodel.SchoolViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ==========================================
// DATA CLASS MODELS FOR CANVAS ELEMENTS
// ==========================================
data class CanvasElement(
    val id: String,
    val type: String, // "Header", "Title", "Text", "Table", "Signatures", "QR", "Sello", "GridFields"
    val title: String,
    val content: String,
    val isLocked: Boolean = false,
    val fontSize: Int = 14,
    val isBold: Boolean = false,
    val textColorHex: String = "#1E293B",
    val backgroundColorHex: String = "#FFFFFF",
    val borderColorHex: String = "#CBD5E1",
    val borderWidth: Int = 0,
    val padding: Int = 12,
    val alignment: String = "Izquierda", // "Izquierda", "Centrado", "Derecha"
    // Extra fields for complex widgets
    val tableRows: List<List<String>> = emptyList(),
    val formFields: List<Pair<String, String>> = emptyList() // Label to Value
)

data class VersionHistory(
    val version: String,
    val author: String,
    val date: String,
    val description: String,
    val canvasState: List<CanvasElement>
)

fun getDefaultElementsForRole(role: String): List<CanvasElement> {
    return when (role) {
        "Docente" -> listOf(
            CanvasElement(
                id = "doc_head",
                type = "Header",
                title = "Cabecera Docente",
                content = "UNIDAD EDUCATIVA FISCAL\n{{institucion}}\nPlanificación de Aula | Código AMIE: {{codigo_amie}}"
            ),
            CanvasElement(
                id = "doc_title",
                type = "Title",
                title = "Título Planificación",
                content = "PLANIFICACIÓN MICROCURRICULAR DEL SEGUNDO QUIMESTRE",
                fontSize = 16,
                isBold = true,
                alignment = "Centrado"
            ),
            CanvasElement(
                id = "doc_text_01",
                type = "Text",
                title = "Datos Informativos",
                content = "Yo, {{docente}}, en calidad de docente tutor, presento la planificación curricular de refuerzo académico para el estudiante {{estudiante}}, en cumplimiento con el Reglamento General de la Ley Orgánica de Educación Intercultural (LOEI).",
                fontSize = 12,
                alignment = "Justificado"
            ),
            CanvasElement(
                id = "doc_table",
                type = "Table",
                title = "Matriz de Destrezas",
                content = "",
                tableRows = listOf(
                    listOf("Asignatura", "Destreza con Criterio", "Estrategia Metodológica", "Evaluación"),
                    listOf("Matemáticas", "M.4.1.2 Resolver operaciones", "Refuerzo y tutorías dirigidas", "Prueba escrita"),
                    listOf("Lengua y Lit.", "LL.4.1.1 Indagar origen de escritura", "Lecturas selectas guiadas", "Rúbrica de exposición")
                )
            ),
            CanvasElement(
                id = "doc_sigs",
                type = "Signatures",
                title = "Firmas de Responsabilidad",
                content = "",
                formFields = listOf(
                    "Docente Elaborador" to "{{docente}}",
                    "Vicerrector Aprobador" to "{{vicerrector}}"
                )
            ),
            CanvasElement(
                id = "doc_sello",
                type = "Sello",
                title = "Firma Digital Docente",
                content = "Documento Generado por: {{docente}}\nFecha de Validación: {{fecha}}\nEstado: Aprobado Curricularmente"
            )
        )
        "DECE" -> listOf(
            CanvasElement(
                id = "dece_head",
                type = "Header",
                title = "Cabecera DECE",
                content = "DEPARTAMENTO DE CONSEJERÍA ESTUDIANTIL (DECE)\n{{institucion}}\nInforme de Intervención | Distrito: {{distrito}}"
            ),
            CanvasElement(
                id = "dece_title",
                type = "Title",
                title = "Título del Informe",
                content = "INFORME PSICOPEDAGÓGICO DE SEGUIMIENTO",
                fontSize = 16,
                isBold = true,
                alignment = "Centrado"
            ),
            CanvasElement(
                id = "dece_text_01",
                type = "Text",
                title = "Contexto del Caso",
                content = "A solicitud de las autoridades, el profesional DECE, {{dece}}, realiza la valoración y acompañamiento psicopedagógico del estudiante {{estudiante}}, tras detectarse alertas académicas y conductuales críticas.",
                fontSize = 12,
                alignment = "Justificado"
            ),
            CanvasElement(
                id = "dece_table",
                type = "Table",
                title = "Acuerdos y Medidas",
                content = "",
                tableRows = listOf(
                    listOf("Dimensión Evaluada", "Hallazgo Clínico", "Medida de Adaptación", "Frecuencia"),
                    listOf("Emocional", "Ansiedad ante exámenes", "Tiempo extra en pruebas", "Diario"),
                    listOf("Familiar", "Falta de acompañamiento", "Escuela para padres obligatoria", "Quincenal")
                )
            ),
            CanvasElement(
                id = "dece_sigs",
                type = "Signatures",
                title = "Firmas del Caso",
                content = "",
                formFields = listOf(
                    "Profesional DECE" to "{{dece}}",
                    "Representante Legal" to "{{representante}}"
                )
            ),
            CanvasElement(
                id = "dece_sello",
                type = "Sello",
                title = "Seguridad del Sello DECE",
                content = "CONFIDENCIAL - Ley Orgánica de Protección de Datos Personales\nCódigo de Validación: DECE-{{codigo_amie}}-2026\nSello de Integridad: {{codigo_qr}}"
            )
        )
        "Autoridades" -> listOf(
            CanvasElement(
                id = "auth_head",
                type = "Header",
                title = "Cabecera de Autoridad",
                content = "UNIDAD EDUCATIVA FISCAL\n{{institucion}}\nDespacho de Rectorado | Código AMIE: {{codigo_amie}}"
            ),
            CanvasElement(
                id = "auth_title",
                type = "Title",
                title = "Título Resolución",
                content = "RESOLUCIÓN INTERNA DE RECTORADO Nº 2026-042",
                fontSize = 16,
                isBold = true,
                alignment = "Centrado"
            ),
            CanvasElement(
                id = "auth_text_01",
                type = "Text",
                title = "Considerandos",
                content = "La Rectora Dra. Eliana Silva Mendoza, en uso de sus atribuciones legales y de conformidad con el Artículo 43 de la Ley Orgánica de Educación Intercultural (LOEI), resuelve aprobar de forma definitiva las reformas al Código de Convivencia Escolar.",
                fontSize = 12,
                alignment = "Justificado"
            ),
            CanvasElement(
                id = "auth_table",
                type = "Table",
                title = "Articulado de la Resolución",
                content = "",
                tableRows = listOf(
                    listOf("Artículo", "Detalle de la Disposición", "Responsable Ejecutor", "Plazo de Entrega"),
                    listOf("Art. 1", "Implementación de aulas digitales", "Comisión de Tecnología", "30 días"),
                    listOf("Art. 2", "Jornadas de capacitación docente", "Vicerrectorado Académico", "15 días")
                )
            ),
            CanvasElement(
                id = "auth_sigs",
                type = "Signatures",
                title = "Suscripción Rectoral",
                content = "",
                formFields = listOf(
                    "Rector/a" to "{{rector}}",
                    "Secretario/a General" to "{{secretaria}}"
                )
            ),
            CanvasElement(
                id = "auth_sello",
                type = "Sello",
                title = "Sello Húmedo de Rectorado",
                content = "Suscrito y sellado en el Rectorado de {{institucion}}.\nValidación QR: {{codigo_qr}}"
            )
        )
        else -> listOf( // General Template for Admin/Others
            CanvasElement(
                id = "head_01",
                type = "Header",
                title = "Cabecera Oficial",
                content = "UNIDAD EDUCATIVA FISCAL\n{{institucion}}\nDistrito: {{distrito}} | Código AMIE: {{codigo_amie}}"
            ),
            CanvasElement(
                id = "title_01",
                type = "Title",
                title = "Título del Documento",
                content = "ACTA DE COMPROMISO EDUCATIVO",
                fontSize = 18,
                isBold = true,
                alignment = "Centrado"
            ),
            CanvasElement(
                id = "text_01",
                type = "Text",
                title = "Cuerpo del Documento",
                content = "Por medio de la presente, yo, {{representante}}, con C.I. ______________ representante legal del estudiante {{estudiante}} del {{grado}} {{paralelo}}, me comprometo solemnemente a cumplir rigurosamente con los lineamientos estipulados por el Ministerio de Educación del Ecuador, garantizando el acompañamiento académico continuo de mi representado en el año lectivo {{anio_lectivo}}.",
                fontSize = 13,
                alignment = "Justificado"
            ),
            CanvasElement(
                id = "table_01",
                type = "Table",
                title = "Matriz de Seguimiento",
                content = "",
                tableRows = listOf(
                    listOf("Fecha", "Actividad de Refuerzo", "Compromiso", "Firma Representante"),
                    listOf("{{fecha}}", "Refuerzo en Matemáticas", "Estudiar 30 mins diarios", "Firma"),
                    listOf("", "Seguimiento Psicopedagógico", "Asistir a cita DECE", "Firma")
                )
            ),
            CanvasElement(
                id = "sigs_01",
                type = "Signatures",
                title = "Bloque de Firmas",
                content = "",
                formFields = listOf(
                    "Docente Tutor" to "{{docente}}",
                    "Coordinador DECE" to "{{dece}}",
                    "Representante Legal" to "{{representante}}"
                )
            ),
            CanvasElement(
                id = "sello_01",
                type = "Sello",
                title = "Sello de Seguridad Digital",
                content = "VALIDACIÓN DIGITAL: DOC-AMIE-{{codigo_amie}}-2026-003\nCódigo QR de Integridad: {{codigo_qr}}\nHash SHA-256: f8d1a498b9e672c1c3d002f1a667b8a1c"
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AITemplateBuilderScreen(
    viewModel: SchoolViewModel,
    modifier: Modifier = Modifier
) {
    val activeInstitution by viewModel.activeInstitution.collectAsStateWithLifecycle()
    val allTemplates by viewModel.allCustomTemplates.collectAsStateWithLifecycle()
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Global Export Dialog visibility state
    var showExportSheet by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showPrintDialog by remember { mutableStateOf(false) }

    // Active sub-section tab
    var activeTab by remember { mutableIntStateOf(1) } // Default to Canvas Editor
    val sections = listOf(
        "Generador & OCR" to Icons.Default.AutoAwesome,
        "Editor Visual" to Icons.Default.DashboardCustomize,
        "Previsualizar & Exportar" to Icons.Default.Devices,
        "Historial" to Icons.Default.History
    )

    // Primary Colors parsing from the active school
    val brandPrimary = activeInstitution?.primaryColor?.let { parseHexColor(it) } ?: MaterialTheme.colorScheme.primary
    val brandSecondary = activeInstitution?.secondaryColor?.let { parseHexColor(it) } ?: MaterialTheme.colorScheme.secondary

    // ==========================================
    // CENTRAL CANVAS STATE MANAGEMENT
    // ==========================================
    var selectedElementId by remember { mutableStateOf<String?>(null) }
    var orientationIsLandscape by remember { mutableStateOf(false) }
    var hasWatermark by remember { mutableStateOf(false) }
    var logoScale by remember { mutableStateOf(48) } // 48dp by default

    // Canvas Items List (RESETS UNIQUELY ON ROLE SWITCH!)
    var canvasElements by remember(currentRole) {
        mutableStateOf(getDefaultElementsForRole(currentRole))
    }

    // Version Control History (RESETS UNIQUELY ON ROLE SWITCH!)
    var versionList by remember(currentRole) {
        mutableStateOf(
            listOf(
                VersionHistory(
                    version = "v1.0.0",
                    author = "Mgs. Sandra Espinoza",
                    date = "05/07/2026 14:32",
                    description = "Estructura base adaptada al rol de $currentRole.",
                    canvasState = getDefaultElementsForRole(currentRole)
                )
            )
        )
    }

    // Selected canvas element for property panel editing
    val selectedElement = canvasElements.find { it.id == selectedElementId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = brandPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "AI Template Builder",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Constructor Inteligente de Plantillas | Ecuador",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Duplicate entire template button
                    TextButton(
                        onClick = {
                            val originalName = canvasElements.find { it.type == "Title" }?.content ?: "Plantilla de la Institución"
                            val copyName = "$originalName (Copia)"
                            if (activeInstitution != null) {
                                viewModel.saveCustomTemplate(
                                    CustomTemplate(
                                        institutionId = activeInstitution!!.id,
                                        name = copyName,
                                        documentType = "Acta",
                                        isCustom = true,
                                        isMinistryDefault = false,
                                        showLogo = true,
                                        showEscudo = true,
                                        showQr = true,
                                        alignment = "Centrado"
                                    )
                                )
                                Toast.makeText(context, "Plantilla duplicada y guardada como: $copyName", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No hay institución activa seleccionada", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicar", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Duplicar", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                    // Share button
                    TextButton(
                        onClick = { showShareDialog = true }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Compartir", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                    // Print button
                    TextButton(
                        onClick = { showPrintDialog = true }
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Imprimir", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Imprimir", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                    // Export button
                    TextButton(
                        onClick = { showExportSheet = true }
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Exportar", modifier = Modifier.size(18.dp), tint = brandSecondary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exportar", fontWeight = FontWeight.Bold, color = brandSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    // Quick Save Button to room DB custom_templates
                    TextButton(
                        onClick = {
                            if (activeInstitution != null) {
                                viewModel.saveCustomTemplate(
                                    CustomTemplate(
                                        institutionId = activeInstitution!!.id,
                                        name = canvasElements.find { it.type == "Title" }?.content ?: "Plantilla de la Institución",
                                        documentType = "Acta",
                                        isCustom = true,
                                        isMinistryDefault = false,
                                        showLogo = true,
                                        showEscudo = true,
                                        showQr = true,
                                        alignment = "Centrado"
                                    )
                                )
                                Toast.makeText(context, "Plantilla guardada en base de datos local SQLite", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No hay institución activa seleccionada", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Guardar", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
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
            // Horizontal Navigation tabs
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
                sections.forEachIndexed { index, (title, icon) ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
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
                    0 -> AISparkAndOcrTab(
                        activeInstitution = activeInstitution,
                        brandPrimary = brandPrimary,
                        brandSecondary = brandSecondary,
                        currentRole = currentRole,
                        onLoadGeneratedContent = { promptResponse ->
                            // Custom parser to map AI output to canvas elements
                            canvasElements = parseAIPromptToCanvas(promptResponse, activeInstitution)
                            activeTab = 1 // Switch automatically to Canvas Editor
                            Toast.makeText(context, "¡AI generó el documento! Cargado en Editor Visual", Toast.LENGTH_SHORT).show()
                        },
                        onExportClick = { showExportSheet = true }
                    )
                    1 -> CanvaWorkspaceTab(
                        canvasElements = canvasElements,
                        selectedElementId = selectedElementId,
                        activeInstitution = activeInstitution,
                        brandPrimary = brandPrimary,
                        brandSecondary = brandSecondary,
                        logoScale = logoScale,
                        hasWatermark = hasWatermark,
                        orientationIsLandscape = orientationIsLandscape,
                        onSelectElement = { selectedElementId = it },
                        onUpdateElements = { canvasElements = it },
                        onUpdateElementProperties = { updated ->
                            canvasElements = canvasElements.map { if (it.id == updated.id) updated else it }
                        },
                        onAiDesignCommand = { command ->
                            // Execute design prompt simulation
                            val (newElements, newLogoScale, newWatermark) = applyAiDesignRules(
                                command = command,
                                currentList = canvasElements,
                                institution = activeInstitution,
                                defaultPrimaryHex = activeInstitution?.primaryColor ?: "#4F46E5",
                                defaultSecondaryHex = activeInstitution?.secondaryColor ?: "#10B981"
                            )
                            canvasElements = newElements
                            logoScale = newLogoScale
                            hasWatermark = newWatermark
                            Toast.makeText(context, "Diseño modificado por IA", Toast.LENGTH_SHORT).show()

                            // Save to version control history
                            val newVer = "v1.${versionList.size}.0"
                            versionList = versionList + VersionHistory(
                                version = newVer,
                                author = "AI Assistant",
                                date = "Hoy " + java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                                description = "Comando de diseño: \"$command\"",
                                canvasState = canvasElements
                            )
                        }
                    )
                    2 -> AdaptivePreviewAndExportTab(
                        canvasElements = canvasElements,
                        activeInstitution = activeInstitution,
                        brandPrimary = brandPrimary,
                        brandSecondary = brandSecondary,
                        logoScale = logoScale,
                        hasWatermark = hasWatermark,
                        orientationIsLandscape = orientationIsLandscape,
                        onExportClick = { showExportSheet = true }
                    )
                    3 -> VersionControlTab(
                        versionHistoryList = versionList,
                        currentCanvasElements = canvasElements,
                        brandPrimary = brandPrimary,
                        onRestoreVersion = { restored ->
                            canvasElements = restored.canvasState
                            Toast.makeText(context, "Versión ${restored.version} restaurada", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Modal Export Sheets dialog simulation (Rendered Globally!)
    if (showExportSheet) {
        Dialog(onDismissRequest = { showExportSheet = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Exportar Plantilla Oficial", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = brandPrimary)
                    Text("Selecciona el formato de exportación para tu institución educativa:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    listOf(
                        "Formato PDF Oficial (Acrobat)" to Icons.Default.PictureAsPdf,
                        "Microsoft Word (Editable DOCX)" to Icons.Default.FileOpen,
                        "Microsoft Excel (Cálculos XLSX)" to Icons.Default.GridOn,
                        "Firma HTML & Web Interactiva" to Icons.Default.Code,
                        "Estructura JSON (Para sistemas externos)" to Icons.Default.Code,
                        "Estructura XML (Ministerio de Educación)" to Icons.Default.DataObject
                    ).forEach { (format, icon) ->
                        Card(
                            onClick = {
                                Toast.makeText(context, "Generando archivo exportable en $format...", Toast.LENGTH_SHORT).show()
                                showExportSheet = false
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, contentDescription = null, tint = brandPrimary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(format, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { showExportSheet = false }) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showShareDialog) {
        Dialog(onDismissRequest = { showShareDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Compartir Plantilla", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = brandPrimary)
                    Text("Genera un enlace de acceso público o envía a tus colegas:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = "https://edusmart-erp.ec/share/template_${currentRole.lowercase()}_" + (System.currentTimeMillis() / 100000),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Enlace de Acceso") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                Toast.makeText(context, "¡Enlace copiado al portapapeles!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar")
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                Toast.makeText(context, "Enviando por correo institucional...", Toast.LENGTH_SHORT).show()
                                showShareDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = brandSecondary)
                        ) {
                            Text("Enviar por Correo", fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { showShareDialog = false }) {
                        Text("Cerrar", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showPrintDialog) {
        Dialog(onDismissRequest = { showPrintDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Configuración de Impresión", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = brandPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Orientación de la página:", fontSize = 12.sp)
                        Text(if (orientationIsLandscape) "Horizontal (A4)" else "Vertical (A4)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Incluir Marcas de Agua:", fontSize = 12.sp)
                        Text(if (hasWatermark) "Sí" else "No", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            Toast.makeText(context, "Conectando con impresora local / Generando vista de impresión...", Toast.LENGTH_LONG).show()
                            showPrintDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = brandPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mandar a Imprimir", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showPrintDialog = false }) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

// ==========================================================
// PARSER HELPER: MAP AI TEXT TO STRUCTURED CANVAS ELEMENTS
// ==========================================================
fun parseAIPromptToCanvas(aiText: String, institution: InstitutionProfile?): List<CanvasElement> {
    // If the text is empty or failed, fallback to defaults
    if (aiText.isBlank() || aiText.contains("Error")) {
        return listOf(
            CanvasElement("h1", "Header", "Cabecera", "UNIDAD EDUCATIVA\n{{institucion}}"),
            CanvasElement("t1", "Title", "Título", "DOCUMENTO INSTITUCIONAL", fontSize = 16, isBold = true),
            CanvasElement("tx1", "Text", "Cuerpo", aiText.ifBlank { "Contenido de ejemplo." })
        )
    }

    val elements = mutableListOf<CanvasElement>()
    elements.add(
        CanvasElement(
            id = "head_ai",
            type = "Header",
            title = "Cabecera Inteligente",
            content = "REPÚBLICA DEL ECUADOR\nMINISTERIO DE EDUCACIÓN\n${institution?.officialName?.uppercase() ?: "UNIDAD EDUCATIVA OFICIAL"}\nCódigo AMIE: ${institution?.amieCode ?: "17HXXXXX"} | Distrito: ${institution?.distrito ?: "09D03"}"
        )
    )

    // Parse sections from AI text. We'll look for header blocks or lists.
    val lines = aiText.split("\n")
    var currentParagraph = StringBuilder()
    var titleFound = ""
    var sectionCounter = 1

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) continue

        // Check if it looks like a Title (uppercase or starting with numbers/tags)
        if ((trimmed.uppercase() == trimmed && trimmed.length > 8 && trimmed.length < 50) || 
            trimmed.startsWith("###") || trimmed.startsWith("##") || trimmed.startsWith("TÍTULO:") || trimmed.startsWith("PLANIFICACIÓN:")) {
            
            // Flush previous paragraph first
            if (currentParagraph.isNotEmpty()) {
                elements.add(
                    CanvasElement(
                        id = "text_$sectionCounter",
                        type = "Text",
                        title = "Párrafo $sectionCounter",
                        content = currentParagraph.toString().trim()
                    )
                )
                currentParagraph = StringBuilder()
                sectionCounter++
            }

            titleFound = trimmed.replace("###", "").replace("##", "").replace("TÍTULO:", "").trim()
            elements.add(
                CanvasElement(
                    id = "title_$sectionCounter",
                    type = "Title",
                    title = "Sección $sectionCounter",
                    content = titleFound,
                    fontSize = 15,
                    isBold = true,
                    alignment = "Centrado"
                )
            )
            sectionCounter++
        } else {
            currentParagraph.append(trimmed).append("\n")
        }
    }

    if (currentParagraph.isNotEmpty()) {
        elements.add(
            CanvasElement(
                id = "text_$sectionCounter",
                type = "Text",
                title = "Párrafo $sectionCounter",
                content = currentParagraph.toString().trim()
            )
        )
    }

    // Add standard elements at the bottom automatically
    elements.add(
        CanvasElement(
            id = "sigs_ai",
            type = "Signatures",
            title = "Bloque de Firmas",
            content = "",
            formFields = listOf(
                "Docente" to (institution?.docenteName ?: "Firma Docente"),
                "Rector/a" to (institution?.rectorName ?: "Firma Rector/a")
            )
        )
    )

    elements.add(
        CanvasElement(
            id = "sello_ai",
            type = "Sello",
            title = "Sello de Seguridad",
            content = "CÓDIGO DE INTEGRIDAD DIGITAL: SEC-AMIE-${institution?.amieCode ?: "17H00"}-2026\nCódigo QR: {{codigo_qr}}"
        )
    )

    return elements
}

// ==========================================================
// DESIGN RULE ENGINE: SIMULATE AI NATURAL LANGUAGE STYLING
// ==========================================================
data class DesignRulesResult(
    val list: List<CanvasElement>,
    val scale: Int,
    val watermark: Boolean
)

fun applyAiDesignRules(
    command: String,
    currentList: List<CanvasElement>,
    institution: InstitutionProfile?,
    defaultPrimaryHex: String,
    defaultSecondaryHex: String
): DesignRulesResult {
    val norm = command.lowercase()
    var updatedList = currentList
    var updatedScale = 48
    var updatedWatermark = false

    // Rule 1: Logo bigger
    if (norm.contains("logo") && (norm.contains("grande") || norm.contains("mayor") || norm.contains("aumentar"))) {
        updatedScale = 80
    }

    // Rule 2: Watermark
    if (norm.contains("marca") || norm.contains("watermark") || norm.contains("fondo")) {
        updatedWatermark = true
    }

    // Rule 3: Use active institution corporate branding colors
    if (norm.contains("institucional") || norm.contains("colores") || norm.contains("color")) {
        updatedList = currentList.map { el ->
            when (el.type) {
                "Header" -> el.copy(
                    textColorHex = defaultPrimaryHex,
                    borderColorHex = defaultSecondaryHex,
                    borderWidth = 2
                )
                "Title" -> el.copy(textColorHex = defaultPrimaryHex)
                "Table" -> el.copy(borderColorHex = defaultPrimaryHex, borderWidth = 1)
                "Sello" -> el.copy(textColorHex = defaultSecondaryHex, borderColorHex = defaultSecondaryHex, borderWidth = 1)
                else -> el
            }
        }
    }

    // Rule 4: Make document headers aligned in custom styles
    if (norm.contains("horizontal") || norm.contains("apaisado")) {
        // Handled in canvas orientation state directly
    }

    // Rule 5: Adjust alignments
    if (norm.contains("centrar") || norm.contains("centrado")) {
        updatedList = updatedList.map { el ->
            if (el.type == "Title" || el.type == "Header") el.copy(alignment = "Centrado") else el
        }
    }

    // Rule 6: Sello lower
    if (norm.contains("qr") && (norm.contains("abajo") || norm.contains("final"))) {
        // Re-prioritize Sello & QR element to the very end of the list
        val qrElement = updatedList.find { it.type == "Sello" || it.type == "QR" }
        if (qrElement != null) {
            updatedList = updatedList.filter { it.id != qrElement.id } + qrElement
        }
    }

    return DesignRulesResult(updatedList, updatedScale, updatedWatermark)
}

// ==========================================================
// SUB-TAB 0: AI GENERATION & OCR FILE ANALYSIS
// ==========================================================
@Composable
fun AISparkAndOcrTab(
    activeInstitution: InstitutionProfile?,
    brandPrimary: Color,
    brandSecondary: Color,
    currentRole: String,
    onLoadGeneratedContent: (String) -> Unit,
    onExportClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var aiPromptInput by remember { mutableStateOf("") }
    var aiIsGenerating by remember { mutableStateOf(false) }
    var aiResultText by remember { mutableStateOf("") }

    // Simulated OCR States
    var selectedFileUriSimulated by remember { mutableStateOf<String?>(null) }
    var isOcrAnalyzing by remember { mutableStateOf(false) }
    var ocrProgressStep by remember { mutableStateOf("") }
    var ocrResultReady by remember { mutableStateOf(false) }

    // Extracted OCR elements
    var ocrExtractedLogo by remember { mutableStateOf("") }
    var ocrExtractedHeader by remember { mutableStateOf("") }
    var ocrExtractedTablesCount by remember { mutableStateOf(0) }
    var ocrExtractedFixedTexts by remember { mutableStateOf(listOf<String>()) }
    var ocrExtractedVariables by remember { mutableStateOf(listOf<String>()) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // LEFT COLUMN: AI Template Generator
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "Generador de Formatos por IA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = brandPrimary
                    )
                    Text(
                        "Escribe qué tipo de documento necesitas y la IA estructurará la plantilla ideal adaptada a tu colegio.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = aiPromptInput,
                        onValueChange = { aiPromptInput = it },
                        placeholder = { Text("Ej: Crea una planificación microcurricular o un informe conductual DECE...", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("ai_prompt_builder_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Button(
                            onClick = {
                                if (aiPromptInput.isNotBlank()) {
                                    aiIsGenerating = true
                                    coroutineScope.launch {
                                        // Connect with Gemini API
                                        val fullPrompt = """
                                            Necesito que diseñes una plantilla estructurada y profesional basada en la siguiente solicitud:
                                            "$aiPromptInput"
                                            
                                            Adapta la estructura al sistema educativo de Ecuador. Organiza la respuesta con títulos claros.
                                        """.trimIndent()
                                        
                                        val sysPrompt = "Eres un especialista del Ministerio de Educación de Ecuador y generas plantillas oficiales."
                                        val result = GeminiClient.generateContent(fullPrompt, sysPrompt)
                                        aiResultText = result
                                        aiIsGenerating = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = brandPrimary),
                            enabled = !aiIsGenerating && aiPromptInput.isNotBlank()
                        ) {
                            if (aiIsGenerating) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generar con IA", fontSize = 12.sp)
                            }
                        }

                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Sugerencias para tu rol ($currentRole):", fontSize = 11.sp, color = brandPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val suggestions = when (currentRole) {
                        "Docente" -> listOf(
                            "Planificación Curricular Semanal",
                            "Informe de Rendimiento Académico",
                            "Acta de Junta de Docentes"
                        )
                        "DECE" -> listOf(
                            "Informe Técnico de Derivación",
                            "Acta de Compromiso del Representante",
                            "Ficha de Seguimiento DECE"
                        )
                        "Autoridades" -> listOf(
                            "Resolución Rectoral de Sanción",
                            "Acta de Junta Directiva",
                            "Certificado de Matrícula Oficial"
                        )
                        else -> listOf(
                            "Acta de Compromiso de Asistencia",
                            "Memorando de Convocatoria Oficial",
                            "Informe General de Gestión"
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestions.forEach { suggestion ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    aiPromptInput = "Crea un(a) $suggestion para la institución educativa, adaptado a las regulaciones vigentes de la LOEI del Ministerio de Educación de Ecuador."
                                },
                                label = { Text(suggestion, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }

            // Display generated AI prompt text structure
            if (aiResultText.isNotBlank()) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Resultado de Estructura de IA", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onLoadGeneratedContent(aiResultText) },
                                    colors = ButtonDefaults.buttonColors(containerColor = brandSecondary),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Cargar en Editor Visual", fontSize = 10.sp)
                                }
                                Button(
                                    onClick = onExportClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = brandPrimary),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Exportar", fontSize = 10.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .verticalScroll(rememberScrollState())
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(10.dp)
                        ) {
                            Text(aiResultText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // RIGHT COLUMN: OCR & File Import Analyzer
        Column(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "Importación Inteligente OCR",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = brandSecondary
                    )
                    Text(
                        "Sube un archivo existente (PDF, Word, Excel, Imagen o Escaneo). La IA extraerá automáticamente la estructura para convertirla en plantilla editable.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Simulated Drag & Drop Zone Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .border(
                                width = 1.5.dp,
                                color = brandSecondary.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                // Simulate file selection
                                selectedFileUriSimulated = "planificacion_oficial_2025_escaneada.pdf"
                                isOcrAnalyzing = true
                                coroutineScope.launch {
                                    ocrProgressStep = "Leyendo archivo binario PDF..."
                                    delay(1000)
                                    ocrProgressStep = "Detectando logotipos y sellos con visión artificial..."
                                    delay(1000)
                                    ocrProgressStep = "Ejecutando OCR avanzado en tablas y celdas..."
                                    delay(1000)
                                    ocrProgressStep = "Separando variables de textos fijos..."
                                    delay(800)

                                    ocrExtractedLogo = "🔬 (Colegio Técnico)"
                                    ocrExtractedHeader = "MINISTERIO DE EDUCACIÓN - SUBSECRETARÍA DE COORDINACIÓN"
                                    ocrExtractedTablesCount = 1
                                    ocrExtractedFixedTexts = listOf(
                                        "Por medio de la presente acta de calificación...",
                                        "Aprobado por el rectorado de la institución educativa..."
                                    )
                                    ocrExtractedVariables = listOf(
                                        "{{estudiante}}", "{{docente}}", "{{curso}}", "{{codigo_amie}}", "{{rector}}"
                                    )
                                    isOcrAnalyzing = false
                                    ocrResultReady = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = brandSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                selectedFileUriSimulated ?: "Arrastra o selecciona un documento aquí",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = brandSecondary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Formatos aceptados: PDF, DOCX, XLSX, PNG, JPG",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // OCR progress bar indicator
                    if (isOcrAnalyzing) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LinearProgressIndicator(color = brandSecondary, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(ocrProgressStep, fontSize = 11.sp, fontStyle = FontStyle.Italic, color = brandSecondary)
                        }
                    }

                    // Display OCR Extraction Results
                    if (ocrResultReady) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Análisis Completado con Éxito", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = brandSecondary)
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("📌 Logotipos: $ocrExtractedLogo", fontSize = 11.sp)
                                Text("📌 Cabecera: $ocrExtractedHeader", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("📌 Tablas detectadas: $ocrExtractedTablesCount tabla con 4 columnas", fontSize = 11.sp)

                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Variables dinámicas detectadas:", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                LazyRow {
                                    items(ocrExtractedVariables) { variable ->
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(variable, fontSize = 9.sp) },
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        // Populate Editor with extracted elements
                                        val elements = listOf(
                                            CanvasElement(
                                                id = "ocr_head",
                                                type = "Header",
                                                title = "Cabecera Importada",
                                                content = ocrExtractedHeader + "\n{{institucion}}\nDistrito: {{distrito}}"
                                            ),
                                            CanvasElement(
                                                id = "ocr_title",
                                                type = "Title",
                                                title = "Título Extraído",
                                                content = "REPORTE DE CALIFICACIONES Y PROMEDIOS",
                                                fontSize = 15,
                                                isBold = true,
                                                alignment = "Centrado"
                                            ),
                                            CanvasElement(
                                                id = "ocr_text",
                                                type = "Text",
                                                title = "Texto Fijo Importado",
                                                content = "Por medio de la presente acta de calificación, el docente tutor {{docente}} del curso {{curso}} certifica que el estudiante {{estudiante}} ha cumplido favorablemente.",
                                                fontSize = 12
                                            ),
                                            CanvasElement(
                                                id = "ocr_sello",
                                                type = "Sello",
                                                title = "Sello de Seguridad Extraído",
                                                content = "Seguridad del Plantel: {{codigo_qr}}\nAMIE: {{codigo_amie}}"
                                            )
                                        )
                                        onLoadGeneratedContent("PLANIFICACIÓN IMPORTADA")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = brandSecondary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Cargar OCR en Editor", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================
// SUB-TAB 1: INTERACTIVE CANVA WORKSPACE (DRAG & DROP EDITOR)
// ==========================================================
@Composable
fun CanvaWorkspaceTab(
    canvasElements: List<CanvasElement>,
    selectedElementId: String?,
    activeInstitution: InstitutionProfile?,
    brandPrimary: Color,
    brandSecondary: Color,
    logoScale: Int,
    hasWatermark: Boolean,
    orientationIsLandscape: Boolean,
    onSelectElement: (String?) -> Unit,
    onUpdateElements: (List<CanvasElement>) -> Unit,
    onUpdateElementProperties: (CanvasElement) -> Unit,
    onAiDesignCommand: (String) -> Unit
) {
    var designCommandText by remember { mutableStateOf("") }
    var showAddComponentDialog by remember { mutableStateOf(false) }
    val selectedElement = canvasElements.find { it.id == selectedElementId }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // COLUMN 1: Component Library & Variables List (Sidebar Left)
        Column(
            modifier = Modifier
                .width(180.dp)
                .fillMaxHeight()
                .padding(end = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Biblioteca de Componentes
            Text("Biblioteca Canva", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = brandPrimary)
            Spacer(modifier = Modifier.height(6.dp))

            listOf(
                "Cabecera" to Icons.Default.VerticalAlignTop,
                "Título" to Icons.Default.TextFields,
                "Párrafo" to Icons.Default.Notes,
                "Tabla" to Icons.Default.GridOn,
                "Firmas" to Icons.Default.Gesture,
                "Sello QR" to Icons.Default.QrCode
            ).forEach { (name, icon) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .clickable {
                            // Add a new element to the canvas
                            val newId = "el_" + System.currentTimeMillis()
                            val newEl = when (name) {
                                "Cabecera" -> CanvasElement(newId, "Header", name, "CABECERA OFICIAL\n{{institucion}}")
                                "Título" -> CanvasElement(newId, "Title", name, "TÍTULO DEL FORMATO", fontSize = 16, isBold = true, alignment = "Centrado")
                                "Párrafo" -> CanvasElement(newId, "Text", name, "Por el presente certificado, yo {{rector}}...")
                                "Tabla" -> CanvasElement(newId, "Table", name, "", tableRows = listOf(listOf("Asignatura", "Nota"), listOf("Matemáticas", ""), listOf("Lengua", "")))
                                "Firmas" -> CanvasElement(newId, "Signatures", name, "", formFields = listOf("Docente" to "{{docente}}", "Rector" to "{{rector}}"))
                                else -> CanvasElement(newId, "Sello", name, "Documento validado digitalmente por QR: {{codigo_qr}}")
                            }
                            onUpdateElements(canvasElements + newEl)
                        },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = brandPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Variables Dinámicas List
            Text("Variables de IA", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = brandSecondary)
            Text("Inserta variables en los textos:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))

            listOf(
                "{{institucion}}" to "Nombre Colegio",
                "{{codigo_amie}}" to "AMIE Oficial",
                "{{distrito}}" to "Distrito",
                "{{rector}}" to "Rector/a",
                "{{docente}}" to "Docente",
                "{{dece}}" to "Psicólogo DECE",
                "{{secretaria}}" to "Secretario/a",
                "{{estudiante}}" to "Estudiante",
                "{{representante}}" to "Representante",
                "{{fecha}}" to "Fecha actual",
                "{{codigo_qr}}" to "QR Integridad"
            ).forEach { (varTag, desc) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                        .clickable {
                            // Copy variable to selected element text content
                            val selected = canvasElements.find { it.id == selectedElementId }
                            if (selected != null) {
                                val updated = selected.copy(content = selected.content + " " + varTag)
                                onUpdateElementProperties(updated)
                            }
                        },
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(varTag, fontSize = 11.sp, color = brandSecondary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text(desc, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // COLUMN 2: Visual Canvas Sheet Editor (Middle Workspace)
        Column(
            modifier = Modifier
                .weight(1.5f)
                .fillMaxHeight()
                .padding(horizontal = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Lienzo de la Plantilla", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                // Layout controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Marca de agua", fontSize = 10.sp)
                    Switch(
                        checked = hasWatermark,
                        onCheckedChange = { /* handled externally */ },
                        modifier = Modifier.scale(0.6f)
                    )
                }
            }

            // Canvas Scrollable Box (representing a clean piece of paper A4)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(2.dp, brandPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .verticalScroll(rememberScrollState())
                    .drawBehind {
                        // Drawing visual watermark diagonal behind if enabled
                        if (hasWatermark) {
                            drawContext.canvas.save()
                            drawContext.canvas.rotate(45f)
                            // Draw watermark text lines
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.argb(20, 0, 0, 0)
                                textSize = 40f
                                isFakeBoldText = true
                            }
                            drawContext.canvas.nativeCanvas.drawText(
                                activeInstitution?.marcaAgua ?: "VALIDACIÓN OFICIAL",
                                200f,
                                100f,
                                paint
                            )
                            drawContext.canvas.restore()
                        }
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    canvasElements.forEachIndexed { idx, el ->
                        val isSelected = el.id == selectedElementId
                        val activeElementColor = if (isSelected) brandPrimary else Color.Transparent

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(parseHexColor(el.backgroundColorHex))
                                .border(
                                    width = if (isSelected) 2.dp else el.borderWidth.dp,
                                    color = if (isSelected) brandPrimary else parseHexColor(el.borderColorHex),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { onSelectElement(el.id) }
                                .padding(el.padding.dp)
                        ) {
                            Column {
                                // Mini Action strip for active element inside the canvas
                                if (isSelected) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp)
                                            .background(brandPrimary.copy(alpha = 0.05f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(el.title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = brandPrimary)
                                        Row {
                                            // Move Up
                                            IconButton(
                                                onClick = {
                                                    if (idx > 0) {
                                                        val list = canvasElements.toMutableList()
                                                        val temp = list[idx]
                                                        list[idx] = list[idx - 1]
                                                        list[idx - 1] = temp
                                                        onUpdateElements(list)
                                                    }
                                                },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(12.dp))
                                            }
                                            // Move Down
                                            IconButton(
                                                onClick = {
                                                    if (idx < canvasElements.size - 1) {
                                                        val list = canvasElements.toMutableList()
                                                        val temp = list[idx]
                                                        list[idx] = list[idx + 1]
                                                        list[idx + 1] = temp
                                                        onUpdateElements(list)
                                                    }
                                                },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(12.dp))
                                            }
                                            // Duplicate
                                            IconButton(
                                                onClick = {
                                                    val dup = el.copy(id = "dup_" + System.currentTimeMillis())
                                                    val list = canvasElements.toMutableList()
                                                    list.add(idx + 1, dup)
                                                    onUpdateElements(list)
                                                },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                            }
                                            // Delete
                                            IconButton(
                                                onClick = {
                                                    onUpdateElements(canvasElements.filter { it.id != el.id })
                                                    onSelectElement(null)
                                                },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }

                                // Element Specific Drawing Renderers
                                when (el.type) {
                                    "Header" -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Logo representation
                                            Box(
                                                modifier = Modifier
                                                    .size(logoScale.dp)
                                                    .clip(CircleShape)
                                                    .background(brandPrimary.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                RenderSymbol(activeInstitution?.logoPrincipal ?: "🏫", "🏫", size = logoScale.dp)
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    replaceVariables(el.content, activeInstitution),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = parseHexColor(el.textColorHex)
                                                )
                                            }
                                        }
                                    }
                                    "Title" -> {
                                        Text(
                                            replaceVariables(el.content, activeInstitution),
                                            fontSize = el.fontSize.sp,
                                            fontWeight = if (el.isBold) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = when (el.alignment) {
                                                "Centrado" -> TextAlign.Center
                                                "Derecha" -> TextAlign.Right
                                                else -> TextAlign.Left
                                            },
                                            color = parseHexColor(el.textColorHex),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    "Table" -> {
                                        TableRenderer(rows = el.tableRows, primaryColor = brandPrimary, institution = activeInstitution)
                                    }
                                    "Signatures" -> {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 16.dp),
                                            horizontalArrangement = Arrangement.SpaceAround
                                        ) {
                                            el.formFields.forEach { (roleName, valVar) ->
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.width(110.dp)
                                                ) {
                                                    // Interactive draw area representation
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(35.dp)
                                                            .border(0.5.dp, Color.LightGray, RoundedCornerShape(4.dp))
                                                            .background(Color(0xFFFAFAFA)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("✍️ Firmado", fontSize = 9.sp, color = brandPrimary)
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Divider(color = Color.DarkGray, thickness = 1.dp)
                                                    Text(replaceVariables(valVar, activeInstitution), fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                                    Text(roleName, fontSize = 8.sp, color = Color.Gray, textAlign = TextAlign.Center)
                                                }
                                            }
                                        }
                                    }
                                    "Sello" -> {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, brandSecondary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                .background(brandSecondary.copy(alpha = 0.03f))
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = brandSecondary, modifier = Modifier.size(34.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    replaceVariables(el.content, activeInstitution),
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = parseHexColor(el.textColorHex)
                                                )
                                                Text(
                                                    "Sello institucional digital registrado en Sistema EduSmart",
                                                    fontSize = 8.sp,
                                                    fontStyle = FontStyle.Italic,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    }
                                    else -> { // Default text
                                        Text(
                                            replaceVariables(el.content, activeInstitution),
                                            fontSize = el.fontSize.sp,
                                            fontWeight = if (el.isBold) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = when (el.alignment) {
                                                "Centrado" -> TextAlign.Center
                                                "Derecha" -> TextAlign.Right
                                                else -> TextAlign.Left
                                            },
                                            color = parseHexColor(el.textColorHex)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // COLUMN 3: Element Properties & AI design Prompt assistant (Sidebar Right)
        Column(
            modifier = Modifier
                .width(200.dp)
                .fillMaxHeight()
                .padding(start = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Asistente de Diseño IA", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = brandPrimary)
            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = designCommandText,
                onValueChange = { designCommandText = it },
                placeholder = { Text("Ej: 'Usa colores institucionales' o 'Haz el logo más grande'", fontSize = 11.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = {
                    if (designCommandText.isNotBlank()) {
                        onAiDesignCommand(designCommandText)
                        designCommandText = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = brandPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Rediseñar", fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Properties Panel of selected canvas item
            if (selectedElement != null) {
                Text("Propiedades del Elemento", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = brandSecondary)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = selectedElement.content,
                    onValueChange = { onUpdateElementProperties(selectedElement.copy(content = it)) },
                    label = { Text("Contenido", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Alignments options
                Text("Alineación:", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("Izquierda", "Centrado", "Derecha").forEach { al ->
                        FilterChip(
                            selected = selectedElement.alignment == al,
                            onClick = { onUpdateElementProperties(selectedElement.copy(alignment = al)) },
                            label = { Text(al, fontSize = 9.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Fonts slider size representation
                Text("Tamaño de Fuente: ${selectedElement.fontSize}sp", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Slider(
                    value = selectedElement.fontSize.toFloat(),
                    onValueChange = { onUpdateElementProperties(selectedElement.copy(fontSize = it.toInt())) },
                    valueRange = 8f..32f,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = selectedElement.isBold,
                        onCheckedChange = { onUpdateElementProperties(selectedElement.copy(isBold = it)) }
                    )
                    Text("Negrita", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Custom border input
                OutlinedTextField(
                    value = selectedElement.borderWidth.toString(),
                    onValueChange = { onUpdateElementProperties(selectedElement.copy(borderWidth = it.toIntOrNull() ?: 0)) },
                    label = { Text("Ancho Borde (dp)", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Text("Selecciona un elemento en el lienzo central para editar sus propiedades.", fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ==========================================================
// RENDERER COMPONENT: PRETTY MATERIAL 3 TABLE IN CANVAS
// ==========================================================
@Composable
fun TableRenderer(
    rows: List<List<String>>,
    primaryColor: Color,
    institution: InstitutionProfile?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
    ) {
        rows.forEachIndexed { rowIdx, rowData ->
            val isHeader = rowIdx == 0
            val bg = if (isHeader) primaryColor.copy(alpha = 0.08f) else Color.White

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bg)
                    .drawBehind {
                        // Bottom cell border
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1f
                        )
                    }
            ) {
                rowData.forEachIndexed { colIdx, cellValue ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(6.dp),
                        contentAlignment = if (isHeader) Alignment.Center else Alignment.TopStart
                    ) {
                        Text(
                            text = replaceVariables(cellValue, institution),
                            fontSize = if (isHeader) 10.sp else 9.sp,
                            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                            textAlign = if (isHeader) TextAlign.Center else TextAlign.Left
                        )
                    }
                }
            }
        }
    }
}

// ==========================================================
// SUB-TAB 2: RESPONSIVE DEVICE PREVIEW & EXPORT DIALOG
// ==========================================================
@Composable
fun AdaptivePreviewAndExportTab(
    canvasElements: List<CanvasElement>,
    activeInstitution: InstitutionProfile?,
    brandPrimary: Color,
    brandSecondary: Color,
    logoScale: Int,
    hasWatermark: Boolean,
    orientationIsLandscape: Boolean,
    onExportClick: () -> Unit
) {
    val context = LocalContext.current
    var currentPreviewDevice by remember { mutableStateOf("Computadora") } // "Computadora", "Tablet", "Celular", "PDF"

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
                    "Simulador de Previsualización Adaptativa",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    "Comprueba la responsividad de tu plantilla oficial en múltiples viewports.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onExportClick,
                colors = ButtonDefaults.buttonColors(containerColor = brandSecondary)
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Exportar Plantilla")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Device Selector Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Computadora" to Icons.Default.Computer,
                "Tablet" to Icons.Default.Tablet,
                "Celular" to Icons.Default.Smartphone,
                "Impresión / PDF" to Icons.Default.Print
            ).forEach { (device, icon) ->
                val isSel = currentPreviewDevice == device
                FilterChip(
                    selected = isSel,
                    onClick = { currentPreviewDevice = device },
                    label = { Text(device, fontSize = 11.sp) },
                    leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Adaptive mock viewport container box
        val containerWidth = when (currentPreviewDevice) {
            "Computadora" -> Modifier.fillMaxWidth()
            "Tablet" -> Modifier.width(550.dp)
            "Celular" -> Modifier.width(320.dp)
            else -> Modifier.width(480.dp) // standard PDF representation
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = containerWidth
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(2.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Column {
                    // Header tag indicating mockup viewport
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .background(Color.LightGray.copy(alpha = 0.2f))
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "VISTA DE $currentPreviewDevice REACCIONANDO A LA CONFIGURACIÓN DEL COLEGIO",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                    }

                    // Render elements adaptive list
                    canvasElements.forEach { el ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(parseHexColor(el.backgroundColorHex))
                                .padding(8.dp)
                        ) {
                            when (el.type) {
                                "Header" -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(logoScale.dp)
                                                .clip(CircleShape)
                                                .background(brandPrimary.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            RenderSymbol(activeInstitution?.logoPrincipal ?: "🏫", "🏫", size = logoScale.dp)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                replaceVariables(el.content, activeInstitution),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = parseHexColor(el.textColorHex)
                                            )
                                        }
                                    }
                                }
                                "Title" -> {
                                    Text(
                                        replaceVariables(el.content, activeInstitution),
                                        fontSize = el.fontSize.sp,
                                        fontWeight = if (el.isBold) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = when (el.alignment) {
                                            "Centrado" -> TextAlign.Center
                                            "Derecha" -> TextAlign.Right
                                            else -> TextAlign.Left
                                        },
                                        color = parseHexColor(el.textColorHex),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                "Table" -> {
                                    TableRenderer(rows = el.tableRows, primaryColor = brandPrimary, institution = activeInstitution)
                                }
                                "Signatures" -> {
                                    val arrangement = if (currentPreviewDevice == "Celular") {
                                        Arrangement.spacedBy(10.dp)
                                    } else {
                                        Arrangement.SpaceAround
                                    }

                                    if (currentPreviewDevice == "Celular") {
                                        // Stack signatures vertically on mobile to satisfy adaptive layouts!
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                            el.formFields.forEach { (roleName, valVar) ->
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.padding(bottom = 12.dp)
                                                ) {
                                                    Divider(color = Color.DarkGray, thickness = 1.dp, modifier = Modifier.width(110.dp))
                                                    Text(replaceVariables(valVar, activeInstitution), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    Text(roleName, fontSize = 8.sp, color = Color.Gray)
                                                }
                                            }
                                        }
                                    } else {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = arrangement) {
                                            el.formFields.forEach { (roleName, valVar) ->
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Divider(color = Color.DarkGray, thickness = 1.dp, modifier = Modifier.width(110.dp))
                                                    Text(replaceVariables(valVar, activeInstitution), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    Text(roleName, fontSize = 8.sp, color = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                                "Sello" -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, brandSecondary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                            .background(brandSecondary.copy(alpha = 0.03f))
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = brandSecondary, modifier = Modifier.size(34.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                replaceVariables(el.content, activeInstitution),
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = parseHexColor(el.textColorHex)
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    Text(
                                        replaceVariables(el.content, activeInstitution),
                                        fontSize = el.fontSize.sp,
                                        fontWeight = if (el.isBold) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = when (el.alignment) {
                                            "Centrado" -> TextAlign.Center
                                            "Derecha" -> TextAlign.Right
                                            else -> TextAlign.Left
                                        },
                                        color = parseHexColor(el.textColorHex)
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

// ==========================================================
// SUB-TAB 3: CONTROL DE VERSIONES & HISTORIAL TIMELINE
// ==========================================================
@Composable
fun VersionControlTab(
    versionHistoryList: List<VersionHistory>,
    currentCanvasElements: List<CanvasElement>,
    brandPrimary: Color,
    onRestoreVersion: (VersionHistory) -> Unit
) {
    var showCompareDialog by remember { mutableStateOf(false) }
    var selectedToCompare by remember { mutableStateOf<VersionHistory?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Control de Versiones del Documento", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = brandPrimary)
        Text("Rastrea las modificaciones, autores y fechas de actualización de este formato.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        if (versionHistoryList.isEmpty()) {
            Text("No hay historial registrado.", fontStyle = FontStyle.Italic)
        } else {
            versionHistoryList.forEachIndexed { index, hist ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Badge(containerColor = brandPrimary, contentColor = Color.White) {
                                    Text(hist.version, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(hist.date, fontSize = 11.sp, color = Color.Gray)
                            }

                            Row {
                                TextButton(
                                    onClick = {
                                        selectedToCompare = hist
                                        showCompareDialog = true
                                    }
                                ) {
                                    Text("Comparar", fontSize = 11.sp)
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                Button(
                                    onClick = { onRestoreVersion(hist) },
                                    colors = ButtonDefaults.buttonColors(containerColor = brandPrimary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Restaurar", fontSize = 10.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(hist.description, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Modificado por: ${hist.author}", fontSize = 10.sp, color = Color.Gray, fontStyle = FontStyle.Italic)
                    }
                }
            }
        }
    }

    // Side-by-side comparison simulated dialog
    if (showCompareDialog && selectedToCompare != null) {
        Dialog(onDismissRequest = { showCompareDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Comparador de Control Documental", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Comparando la versión seleccionada contra el estado actual de edición.", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Left Column: Selected Version
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color.LightGray)
                                .padding(8.dp)
                        ) {
                            Text("VERSIÓN ${selectedToCompare!!.version}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = brandPrimary)
                            Text(selectedToCompare!!.description, fontSize = 10.sp, fontStyle = FontStyle.Italic)
                            Spacer(modifier = Modifier.height(10.dp))

                            selectedToCompare!!.canvasState.forEach { el ->
                                Text("• [${el.type}] ${el.content.take(40)}...", fontSize = 9.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Right Column: Active Canvas Editing State
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, brandPrimary)
                                .padding(8.dp)
                        ) {
                            Text("ESTADO ACTUAL (EDICIÓN)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = brandPrimary)
                            Text("Cambios sin confirmar en base de datos local", fontSize = 10.sp, fontStyle = FontStyle.Italic)
                            Spacer(modifier = Modifier.height(10.dp))

                            currentCanvasElements.forEach { el ->
                                Text("• [${el.type}] ${el.content.take(40)}...", fontSize = 9.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showCompareDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = brandPrimary),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }
}

// ==========================================
// STRING UTILS: VARIABLE REPLACEMENT LOGIC
// ==========================================
fun replaceVariables(text: String, inst: InstitutionProfile?): String {
    if (inst == null) return text
    return text
        .replace("{{institucion}}", inst.officialName)
        .replace("{{logo}}", inst.logoPrincipal)
        .replace("{{escudo}}", inst.escudo)
        .replace("{{codigo_amie}}", inst.amieCode)
        .replace("{{distrito}}", inst.distrito)
        .replace("{{circuito}}", inst.circuito)
        .replace("{{provincia}}", inst.provincia)
        .replace("{{canton}}", inst.canton)
        .replace("{{parroquia}}", inst.parroquia)
        .replace("{{direccion}}", inst.address)
        .replace("{{telefono}}", inst.phone)
        .replace("{{correo}}", inst.email)
        .replace("{{sitio_web}}", inst.website)
        .replace("{{rector}}", inst.rectorName)
        .replace("{{vicerrector}}", inst.vicerrectorName)
        .replace("{{docente}}", inst.docenteName)
        .replace("{{dece}}", inst.deceName)
        .replace("{{secretaria}}", inst.secretariaName)
        .replace("{{estudiante}}", "Mateo Alejandro Cárdenas")
        .replace("{{representante}}", "Ing. Carolina Cárdenas")
        .replace("{{curso}}", "Segundo de Bachillerato")
        .replace("{{grado}}", "Segundo de Bachillerato")
        .replace("{{paralelo}}", "Paralelo A")
        .replace("{{fecha}}", "07 de Julio de 2026")
        .replace("{{anio_lectivo}}", "2026-2027")
        .replace("{{codigo_qr}}", "https://verificar.educasmart.ai/validador?id=DOC-2026-00342")
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
