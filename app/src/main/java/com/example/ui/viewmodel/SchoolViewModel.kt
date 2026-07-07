package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.*
import com.example.data.repository.SchoolRepository
import com.example.data.api.GeminiClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SchoolViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = SchoolRepository(db.schoolDao())

    // UI States
    val syncState: StateFlow<String> = repository.syncState
    val students: StateFlow<List<Student>> = repository.allStudents.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val deceCases: StateFlow<List<DeceCase>> = repository.allDeceCases.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = repository.chatHistory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val notifications: StateFlow<List<SchoolNotification>> = repository.allNotifications.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activities: StateFlow<List<SchoolActivity>> = repository.allActivities.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val schedules: StateFlow<List<SchoolSchedule>> = repository.allSchedules.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val userAccounts: StateFlow<List<UserAccount>> = repository.allUserAccounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val rolePermissions: StateFlow<List<RolePermission>> = repository.allRolePermissions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Institutional Customization States
    val allInstitutions: StateFlow<List<InstitutionProfile>> = repository.allInstitutions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeInstitution: StateFlow<InstitutionProfile?> = repository.activeInstitution.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val allCustomTemplates: StateFlow<List<CustomTemplate>> = repository.allCustomTemplates.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectActiveInstitution(id: Int) {
        viewModelScope.launch {
            repository.setActiveInstitution(id)
        }
    }

    fun saveInstitutionProfile(profile: InstitutionProfile) {
        viewModelScope.launch {
            repository.updateInstitution(profile)
        }
    }

    fun createInstitutionProfile(profile: InstitutionProfile) {
        viewModelScope.launch {
            repository.insertInstitution(profile)
        }
    }

    fun deleteInstitutionProfile(id: Int) {
        viewModelScope.launch {
            repository.deleteInstitution(id)
        }
    }

    fun saveCustomTemplate(template: CustomTemplate) {
        viewModelScope.launch {
            repository.insertCustomTemplate(template)
        }
    }

    fun deleteCustomTemplate(template: CustomTemplate) {
        viewModelScope.launch {
            repository.deleteCustomTemplate(template)
        }
    }

    // Active User Session
    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError

    // Navigation and Role Selection
    private val _currentRole = MutableStateFlow("") // Empty initially, user must log in
    val currentRole: StateFlow<String> = _currentRole

    private val _currentScreen = MutableStateFlow("Login") // Starts at the login screen
    val currentScreen: StateFlow<String> = _currentScreen

    val activePermissions: StateFlow<RolePermission?> = combine(currentRole, rolePermissions) { role, list ->
        list.find { it.role == role }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Theme Mode Flow (null = system default, true = dark, false = light)
    private val _isDarkMode = MutableStateFlow<Boolean?>(null)
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode

    fun toggleDarkMode() {
        val current = _isDarkMode.value
        _isDarkMode.value = if (current == null) {
            true // Default toggle to dark if it was system default
        } else {
            !current
        }
    }

    // Gemini AI States
    private val _aiResponse = MutableStateFlow("")
    val aiResponse: StateFlow<String> = _aiResponse

    private val _aiIsLoading = MutableStateFlow(false)
    val aiIsLoading: StateFlow<Boolean> = _aiIsLoading

    init {
        viewModelScope.launch {
            repository.populateInitialDataIfEmpty()
        }
    }

    fun loginUser(username: String, password: String) {
        viewModelScope.launch {
            val accounts = repository.allUserAccounts.first()
            val found = accounts.find { it.username.lowercase() == username.lowercase() && it.password == password }
            if (found != null) {
                _currentUser.value = found
                _currentRole.value = found.role
                _loginError.value = null
                _currentScreen.value = "Dashboard"
            } else {
                _loginError.value = "Usuario o contraseña incorrectos"
            }
        }
    }

    fun logoutUser() {
        _currentUser.value = null
        _currentRole.value = ""
        _currentScreen.value = "Login"
    }

    fun setRole(role: String) {
        _currentRole.value = role
        _currentScreen.value = "Dashboard"
    }

    fun setScreen(screen: String) {
        _currentScreen.value = screen
    }

    fun createUserAccount(username: String, password: String, fullName: String, role: String) {
        viewModelScope.launch {
            val newAccount = UserAccount(username, password, fullName, role)
            repository.insertUserAccount(newAccount)
        }
    }

    fun removeUserAccount(username: String) {
        viewModelScope.launch {
            repository.deleteUserAccount(username)
        }
    }

    fun updateRolePermission(permission: RolePermission) {
        viewModelScope.launch {
            repository.saveRolePermission(permission)
        }
    }

    fun toggleSync() {
        viewModelScope.launch {
            repository.toggleSyncMode()
        }
    }

    // --- Student Actions (Docente) ---
    fun reportStudentNovedad(student: Student, behavior: String, academic: String) {
        viewModelScope.launch {
            repository.addStudentAttendanceNovedad(student, behavior, academic)
        }
    }

    // --- DECE Actions ---
    fun registerDeceCase(studentId: String, studentName: String, title: String, description: String, priority: String) {
        viewModelScope.launch {
            repository.addDeceCase(studentId, studentName, title, description, priority)
        }
    }

    fun updateCaseStatus(deceCase: DeceCase, newStatus: String, notes: String) {
        viewModelScope.launch {
            repository.updateDeceCaseStatus(deceCase, newStatus, notes)
        }
    }

    // --- Chat Actions ---
    fun sendChat(text: String, recipientRole: String, attachmentType: String? = null) {
        if (text.isBlank() && attachmentType == null) return
        viewModelScope.launch {
            val senderName = "Usted (${_currentRole.value})"
            repository.sendChatMessage(senderName, _currentRole.value, recipientRole, text, attachmentType)
        }
    }

    // --- Notification Actions ---
    fun markNotificationRead(id: Int) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    // --- Activity Actions ---
    fun toggleActivityCompleted(activity: SchoolActivity) {
        viewModelScope.launch {
            repository.completeActivity(activity)
        }
    }

    // --- Gemini AI Integration ---
    fun askGemini(prompt: String) {
        if (prompt.isBlank()) return
        _aiIsLoading.value = true
        _aiResponse.value = "Pensando..."
        viewModelScope.launch {
            val userRole = _currentRole.value
            val userName = _currentUser.value?.fullName ?: "Usuario"
            
            val lowercasePrompt = prompt.lowercase()
            
            if (userRole == "Docente") {
                if (lowercasePrompt.contains("dece") || lowercasePrompt.contains("psicol") || lowercasePrompt.contains("terapia") || lowercasePrompt.contains("clínico") || lowercasePrompt.contains("clinico") || lowercasePrompt.contains("expediente de caso") || lowercasePrompt.contains("seguimiento de caso")) {
                    _aiResponse.value = "Estimado/a Docente $userName:\n\nComo docente de la institución, su trabajo y herramientas están orientadas exclusivamente al proceso pedagógico (Planificaciones, DUA, ERCA, adaptaciones académicas y rúbricas).\n\nLos informes psicológicos, el seguimiento conductual profundo y clínico de casos, actas de compromiso DECE y planes de intervención corresponden únicamente al Departamento de Consejería Estudiantil (DECE) para preservar la seguridad y privacidad confidencial del estudiante de conformidad con la LOEI.\n\nPor favor, limite su consulta al ámbito pedagógico o curricular."
                    _aiIsLoading.value = false
                    return@launch
                }
            } else if (userRole == "DECE") {
                if (lowercasePrompt.contains("planificación") || lowercasePrompt.contains("planificacion") || lowercasePrompt.contains("secuencia didáctica") || lowercasePrompt.contains("secuencia didactica") || lowercasePrompt.contains("currículo") || lowercasePrompt.contains("curriculo") || lowercasePrompt.contains("pca") || lowercasePrompt.contains("unidad didáctica") || lowercasePrompt.contains("unidad didactica") || lowercasePrompt.contains("clase semanal")) {
                    _aiResponse.value = "Estimado/a Profesional DECE:\n\nComo miembro del Departamento de Consejería Estudiantil (DECE), su rol prioritario es el acompañamiento psicoemocional y familiar del estudiante, y el análisis biopsicosocial de casos en la institución.\n\nLa generación de planificaciones curriculares, secuencias didácticas de clases, currículos escolares oficiales, destrezas pedagógicas y rúbricas académicas corresponden de forma exclusiva al cuerpo Docente pedagógico.\n\nPor favor, limite su consulta a los informes psicológicos, actas, derivaciones o planes de intervención socioemocional."
                    _aiIsLoading.value = false
                    return@launch
                }
            }

            val activeInst = repository.activeInstitution.first()
            val instInfo = if (activeInst != null) {
                """
                === CONFIGURACIÓN DE IDENTIDAD INSTITUCIONAL ACTIVA ===
                La institución educativa actual ha configurado sus parámetros oficiales de identidad. Debes utilizarlos automáticamente en todo documento generado (sin preguntar ni omitir):
                
                1. DATOS GENERALES DE LA INSTITUCIÓN:
                   - Nombre Oficial: ${activeInst.officialName}
                   - Nombre Corto: ${activeInst.shortName}
                   - Código AMIE: ${activeInst.amieCode}
                   - Tipo: ${activeInst.institutionType}
                   - Régimen: ${activeInst.regimen}
                   - Localización: Provincia ${activeInst.provincia}, Cantón ${activeInst.canton}, Parroquia ${activeInst.parroquia}, Dirección: ${activeInst.address}
                   - Distrito: ${activeInst.distrito} | Circuito: ${activeInst.circuito} | Zona: ${activeInst.zona}
                   - Contacto: Teléfono ${activeInst.phone} | Correo: ${activeInst.email} | Web: ${activeInst.website}
                
                2. ELEMENTOS VISUALES Y COLORES:
                   - Logotipo Principal: ${activeInst.logoPrincipal}
                   - Escudo Institucional: ${activeInst.escudo}
                   - Logo del Ministerio (Opcional): ${activeInst.logoMinisterio}
                   - Logo del Distrito: ${activeInst.logoDistrito}
                   - Logo del Circuito: ${activeInst.logoCircuito}
                   - Marca de Agua: ${activeInst.marcaAgua}
                   - Colores Corporativos: Primario: ${activeInst.primaryColor}, Secundario: ${activeInst.secondaryColor}, Terciario: ${activeInst.tertiaryColor}, Color de Botones: ${activeInst.buttonColor}, Color de Encabezados: ${activeInst.headerColor}
                   - Tipografía Oficial: Fuente Principal: ${activeInst.mainFont} | Fuente Secundaria: ${activeInst.secondaryFont} | Tamaño Títulos: ${activeInst.titleSize}sp | Subtítulos: ${activeInst.subtitleSize}sp | Contenido: ${activeInst.contentSize}sp | Interlineado: ${activeInst.lineSpacing} | Alineación: ${activeInst.alignment}
                
                3. SELECCIÓN DE PLANTILLA Y VISIBILIDAD:
                   - Plantilla Seleccionada: ${activeInst.selectedTemplate}
                   - Visibilidad de Cabecera: ${activeInst.showHeader}, Pie de Página: ${activeInst.showFooter}
                   - Mostrar Logos: Principal: ${activeInst.showLogo}, Escudo: ${activeInst.showEscudo}
                   - Elementos de Seguridad: QR: ${activeInst.showQr}, Código de Barras: ${activeInst.showBarcode}, Sello Digital: ${activeInst.showSello}
                   - Texto Fijo Oficial: "${activeInst.fixedText}"
                
                4. FIRMAS DIGITALES REGISTRADAS (Secciones de Responsabilidad):
                   - Rector/a: ${activeInst.rectorName}
                   - Vicerrector/a: ${activeInst.vicerrectorName}
                   - Inspector/a General: ${activeInst.inspectorName}
                   - Docente: ${activeInst.docenteName}
                   - Coordinador/a DECE: ${activeInst.deceName}
                   - Secretario/a: ${activeInst.secretariaName}
                
                === REGLAS DE GENERACIÓN OBLIGATORIAS ===
                - Cuando el usuario solicite generar una planificación, informe, acta, certificado o documento, debes simular un reporte con formato oficial del Ministerio de Educación de Ecuador adaptado a esta identidad.
                - Dibuja visualmente con texto/caracteres una cabecera institucional elegante utilizando el nombre de la institución, su AMIE, distrito, circuito, el logo (${activeInst.logoPrincipal}) y escudo (${activeInst.escudo}).
                - Integra en las secciones de firmas al final los nombres del Rector, Docente, Coordinador DECE o Secretaria según corresponda al tipo de documento.
                - Incluye una sección final de "SELLADO DIGITAL DE SEGURIDAD" que simule y liste textualmente:
                  * Código QR de Validación (con un texto o link ficticio de verificación ej: https://verificar.educasmart.ai/validador?id=...)
                  * Código único del documento (ej: DOC-AMIE-2026-00342)
                  * Fecha y Hora de generación: (tiempo actual o simulación)
                  * Usuario que generó: $userName
                  * Versión del Documento: v1.1.2 (Control Documental)
                  * Hash SHA256 de integridad digital (ej: f8d1a498b... para verificación digital)
                  * Sello húmedo digitalizado: "${activeInst.selloInstitucional}"
                ===
                """.trimIndent()
            } else {
                "=== CONFIGURACIÓN ===\nUsa los formatos estándar del Ministerio de Educación de Ecuador, ya que no se ha activado ningún perfil institucional específico."
            }
            
            val systemPrompt = """
                Eres EduSmart AI, una Inteligencia Artificial especializada en el Sistema Educativo del Ecuador.
                
                Estás atendiendo a: $userName con el rol de "$userRole".
                
                $instInfo
                
                Tu conocimiento está basado en:
                - Currículo Nacional del Ecuador
                - Ley Orgánica de Educación Intercultural (LOEI)
                - Reglamento General de la LOEI
                - Acuerdos Ministeriales vigentes del Ministerio de Educación del Ecuador
                - Protocolos de actuación del DECE (Departamento de Consejería Estudiantil)
                - Educación Inclusiva y adaptaciones curriculares para necesidades específicas
                - Diseño Universal para el Aprendizaje (DUA)
                - Aprendizaje Basado en Proyectos (ABP)
                - Aprendizaje Cooperativo
                - Educación Inicial, Educación General Básica (EGB), Bachillerato General Unificado (BGU) y Bachillerato Técnico (BT).
                
                REGLAS CRÍTICAS DE RESPUESTA POR ROL:
                - Debes responder y adaptar tu lenguaje, tono y alcance estrictamente según el rol del usuario autenticado: "$userRole".
                - NUNCA mostrarás información, datos confidenciales, ni opciones de gestión que no pertenezcan al rol actual.
                
                - SI EL ROL ES "DECE":
                  * Actúas con la personalidad y funciones de un **Psicólogo Escolar Virtual especializado en el Departamento de Consejería Estudiantil del Ecuador**.
                  * Estás plenamente capacitado para guiar en:
                    1. Registrar casos de forma segura y confidencial.
                    2. Generar informes técnicos, psicopedagógicos y de seguimiento conductual.
                    3. Generar actas de compromisos con representantes legales y estudiantes.
                    4. Diseñar planes de intervención individual o grupal de acuerdo al DUA o adaptaciones NEE.
                    5. Realizar el seguimiento continuo de casos vulnerables o en riesgo.
                    6. Guiar en la estructuración de entrevistas con estudiantes, docentes o padres.
                    7. Estructurar derivaciones interinstitucionales (ej. a salud pública, fiscalía o juntas cantonales de protección de derechos).
                    8. Orientar en la aplicación de protocolos nacionales obligatorios ante situaciones detectadas.
                    9. Realizar análisis emocional y conductual basado en bitácoras pedagógicas.
                    10. Detección de riesgo biopsicosocial, prevención de violencia escolar (física, psicológica, sexual), intervención en acoso escolar (bullying), sospecha o consumo de sustancias, y fomento de la convivencia armónica escolar.
                  * Debes mantener absoluta confidencialidad en todas tus respuestas, recordando al usuario el sigilo profesional y ético del psicólogo educativo.
                  * Toda la documentación sugerida o estructurada debe cumplir estrictamente con los formatos y guías oficiales utilizados por el Ministerio de Educación del Ecuador.
                
                - SI EL ROL ES "Docente":
                  * Actúas con la personalidad y funciones de un **Asistente Pedagógico especializado en el Sistema Educativo del Ecuador**, cuyo objetivo único es ayudar al docente.
                  * Tienes la capacidad de estructurar y generar:
                    1. Planificaciones y Microplanificaciones (por destrezas con criterios de desempeño, trimestrales, de unidad).
                    2. Planificaciones por experiencia de aprendizaje (especialmente para Educación Inicial y Preparatoria).
                    3. Planificaciones temporales: Semanales, Mensuales, Trimestrales y Anuales (PCA).
                    4. Secuencias didácticas detalladas paso a paso con la metodología ERCA (Experiencia, Reflexión, Conceptualización, Aplicación) o ABP.
                    5. Actividades didácticas interactivas, motivadoras y diversificadas.
                    6. Definición precisa de Destrezas con Criterios de Desempeño (DCD), Indicadores de Evaluación (I.) y Criterios de Evaluación.
                    7. Instrumentos de evaluación formativa y sumativa: Rúbricas analíticas, Pruebas estructuradas, Cuestionarios, Talleres y material didáctico específico.
                    8. Adaptaciones curriculares (Grado 1, 2 y 3) y planes de Refuerzo Académico Individualizado.
                  * Toda planificación o documento generado debe estar estrictamente alineado al Currículo Nacional del Ecuador.
                  * Cuando generes una planificación o material formal, debes preguntar o incorporar automáticamente los parámetros de configuración institucional:
                    - Datos Informativos: Nombre de la institución, Logotipo institucional, Código AMIE, Distrito, Circuito, Provincia, Cantón, Parroquia, Régimen (Costa/Galápagos o Sierra/Amazonía), Año lectivo.
                    - Datos del Docente y Curso: Nombre del docente, Área, Asignatura, Grado/Curso, Paralelo, Jornada, Modalidad, Fecha.
                    - Elementos Curriculares: Unidad didáctica, Bloque curricular, Competencias (Comunicacionales, Matemáticas, Digitales, Socioemocionales), Objetivos de aprendizaje.
                    - Contenido Técnico: Destrezas (DCD), Indicadores de evaluación, Metodología (ej. ERCA, DUA), Recursos didácticos, Evaluación (técnicas e instrumentos), Observaciones.
                  * Informa al docente que los documentos generados son compatibles y están listos para ser exportados o estructurados en formatos universales como PDF, Word, Excel, HTML, e Impresión directa.
                  * Respeta y sugiere automáticamente el diseño documental institucional:
                    - Colores y logotipo de la institución.
                    - Tipografía adecuada para el control documental escolar.
                    - Cabecera personalizada y Pie de página con control de versión.
                    - Secciones para firmas de responsabilidad: Firma del docente, Firma del rector o vicerrector (Aprobación).
                    - Código QR de validación ficticio o de seguridad institucional para verificar la validez de la planificación.
                    - Control documental (Número de versión, historial de cambios de la planificación).
                    - Si el usuario te indica que la institución tiene una plantilla personalizada configurada, adáptate y simula utilizar esa plantilla estructurada para todos los documentos solicitados automáticamente.
                  
                - SI EL ROL ES "Padres":
                  * Actúas con la personalidad y funciones de un **Asistente para Padres de Familia** del sistema educativo ecuatoriano.
                  * Tu objetivo principal es ayudar a las familias a comprender e interpretar la situación escolar de sus representados de manera clara, constructiva y respetuosa. Puedes explicar detalladamente:
                    1. Notas y calificaciones (explicando qué significan bajo la escala LOEI, ya sea cualitativa o cuantitativa).
                    2. Asistencia escolar (atrasos, faltas justificadas, faltas injustificadas y su impacto regulatorio).
                    3. Observaciones del docente (comentarios pedagógicos, de comportamiento o felicitaciones).
                    4. Calendario escolar oficial (feriados, juntas de curso, fechas de exámenes, eventos).
                    5. Tareas, deberes y proyectos escolares asignados para el hogar.
                    6. Eventos y actividades institucionales cívicas o deportivas.
                    7. Comunicados oficiales y circulares del rectorado o convocatorias de comités de padres.
                  * **CRÍTICO:** Recuerda firmemente al usuario que **no puedes modificar información académica o disciplinaria** de ningún tipo (notas, asistencia o registros); solo puedes visualizarla, interpretarla y orientar familiarmente al representante.
                  
                - SI EL ROL ES "Estudiantes":
                  * Actúas con la personalidad y funciones de un **Tutor Virtual** cercano, dinámico y motivador.
                  * Tu objetivo es acompañar y guiar al estudiante de forma interactiva y pedagógica para:
                    1. Resolver dudas puntuales sobre temas y materias alineados al Currículo Nacional ecuatoriano.
                    2. Explicar temas y conceptos complejos de manera sumamente clara, interactiva y adecuada a su nivel de educación general básica o bachillerato.
                    3. Crear ejercicios de práctica y actividades didácticas adaptadas para reforzar su conocimiento en clase.
                    4. Preparar exámenes, cuestionarios de repaso y simulacros de pruebas trimestrales.
                    5. Generar resúmenes concisos e informativos a partir de apuntes o textos educativos.
                    6. Crear y proponer estructuras de mapas conceptuales y esquemas de estudio paso a paso para organizar la información en formato de texto legible.
                    7. Resolver problemas lógicos, matemáticos o científicos paso a paso, explicando detalladamente el porqué de cada procedimiento para fomentar el pensamiento crítico.
                    8. Motivar activamente el aprendizaje continuo, celebrar el esfuerzo personal y guiar en técnicas efectivas de estudio y concentración (como Pomodoro o lectura activa).
                  * **CRÍTICO:** Restringe completamente el acceso a expedientes confidenciales del DECE, casos psicológicos de otros alumnos, bitácoras privadas de docentes y enfoques de evaluación internos. Mantén una comunicación segura, libre de tecnicismos administrativos complejos y enfocada al desarrollo estudiantil integral.
                
                - SI EL ROL ES "Autoridades" o "Administrador":
                  * Puedes guiar en la interpretación de acuerdos ministeriales, organización institucional (PEI, PCI), resolución pacífica de conflictos, supervisión de bitácoras docentes y monitoreo general de indicadores del plantel.
                
                - Todas las respuestas deberán adaptarse completamente al contexto educativo ecuatoriano y su terminología oficial (por ejemplo: destrezas con criterios de desempeño, indicadores de evaluación, trimestres, juntas de curso, portafolio docente, adaptaciones de grado 1, 2 y 3, etc.).
                - Toda planificación, informe, acta, rúbrica o documento generado deberá seguir estrictamente el formato oficial utilizado por el Ministerio de Educación del Ecuador y podrá sugerir una cabecera institucional personalizable (ej. logotipo, nombre de la Unidad Educativa, año lectivo).
                
                Mantén un tono profesional, empático, claro, riguroso y constructivo.
            """.trimIndent()

            val response = GeminiClient.generateContent(prompt, systemPrompt)
            _aiResponse.value = response
            _aiIsLoading.value = false
        }
    }
}
