package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SchoolRepository(private val dao: SchoolDao) {

    private val TAG = "SchoolRepository"

    // Synchronization status
    private val _syncState = MutableStateFlow("Sincronizado") // "Sincronizado", "Modo Offline", "Sincronizando..."
    val syncState: StateFlow<String> = _syncState

    val allStudents: Flow<List<Student>> = dao.getAllStudents()
    val allDeceCases: Flow<List<DeceCase>> = dao.getAllDeceCases()
    val chatHistory: Flow<List<ChatMessage>> = dao.getChatHistory()
    val allNotifications: Flow<List<SchoolNotification>> = dao.getAllNotifications()
    val allActivities: Flow<List<SchoolActivity>> = dao.getAllActivities()
    val allSchedules: Flow<List<SchoolSchedule>> = dao.getAllSchedules()
    val allUserAccounts: Flow<List<UserAccount>> = dao.getAllUserAccounts()
    val allRolePermissions: Flow<List<RolePermission>> = dao.getAllRolePermissions()

    // --- Institutional Profile Flows ---
    val allInstitutions: Flow<List<InstitutionProfile>> = dao.getAllInstitutions()
    val activeInstitution: Flow<InstitutionProfile?> = dao.getActiveInstitution()
    val allCustomTemplates: Flow<List<CustomTemplate>> = dao.getAllCustomTemplates()

    suspend fun insertInstitution(profile: InstitutionProfile) {
        dao.insertInstitution(profile)
    }

    suspend fun updateInstitution(profile: InstitutionProfile) {
        dao.updateInstitution(profile)
    }

    suspend fun setActiveInstitution(id: Int) {
        dao.setActiveInstitution(id)
        
        // Notify the system of the active institution change
        val selected = dao.getAllInstitutions().first().find { it.id == id }
        if (selected != null) {
            addNotification(
                "Institución Cambiada",
                "Se ha activado el perfil de la institución: ${selected.officialName}.",
                "Comunicado"
            )
        }
    }

    suspend fun deleteInstitution(id: Int) {
        dao.deleteInstitution(id)
    }

    suspend fun insertCustomTemplate(template: CustomTemplate) {
        dao.insertCustomTemplate(template)
    }

    suspend fun deleteCustomTemplate(template: CustomTemplate) {
        dao.deleteCustomTemplate(template)
    }

    suspend fun insertUserAccount(user: UserAccount) {
        dao.insertUserAccount(user)
    }

    suspend fun deleteUserAccount(username: String) {
        dao.deleteUserAccount(username)
    }

    suspend fun saveRolePermission(permission: RolePermission) {
        dao.insertRolePermissions(listOf(permission))
    }

    suspend fun getRolePermissionByRole(role: String): RolePermission? {
        return dao.getRolePermissionByRole(role)
    }

    suspend fun toggleSyncMode() {
        if (_syncState.value == "Modo Offline") {
            _syncState.value = "Sincronizando..."
            // Simulate sync delay
            withContext(Dispatchers.IO) {
                kotlinx.coroutines.delay(1500)
                // Sync any unsynced dece cases
                val cases = dao.getAllDeceCases().first()
                cases.forEach {
                    if (!it.isSynced) {
                        dao.updateDeceCase(it.copy(isSynced = true))
                    }
                }
            }
            _syncState.value = "Sincronizado"
            addNotification(
                "Sincronización Exitosa",
                "Todos los datos locales se han sincronizado con el servidor central.",
                "Comunicado"
            )
        } else {
            _syncState.value = "Modo Offline"
            addNotification(
                "Modo Offline Activado",
                "La app continuará funcionando localmente. Los cambios se sincronizarán al recuperar conexión.",
                "Comunicado"
            )
        }
    }

    suspend fun addDeceCase(studentId: String, studentName: String, title: String, description: String, priority: String) {
        val isOffline = _syncState.value == "Modo Offline"
        val newCase = DeceCase(
            studentId = studentId,
            studentName = studentName,
            title = title,
            description = description,
            status = "Abierto",
            priority = priority,
            lastInterviewDate = "Sin entrevistas registradas",
            isSynced = !isOffline
        )
        dao.insertDeceCase(newCase)

        // Trigger push notification simulation
        addNotification(
            "Nuevo Caso DECE Creado",
            "Se ha registrado un caso de atención para $studentName - Prioridad: $priority.",
            "DECE"
        )
    }

    suspend fun updateDeceCaseStatus(deceCase: DeceCase, newStatus: String, notes: String) {
        val isOffline = _syncState.value == "Modo Offline"
        val updated = deceCase.copy(
            status = newStatus,
            lastInterviewDate = "Registrado hoy: $notes",
            isSynced = !isOffline
        )
        dao.updateDeceCase(updated)

        addNotification(
            "Seguimiento DECE",
            "Actualización de caso de ${deceCase.studentName}: Estado cambiado a $newStatus.",
            "DECE"
        )
    }

    suspend fun sendChatMessage(senderName: String, senderRole: String, recipientRole: String, text: String, attachmentType: String? = null) {
        val msg = ChatMessage(
            senderName = senderName,
            senderRole = senderRole,
            recipientRole = recipientRole,
            message = text,
            attachmentType = attachmentType,
            attachmentUrl = if (attachmentType != null) "local_file_attachment" else null
        )
        dao.insertChatMessage(msg)

        // Simulate an automatic response after 1.5 seconds if online
        if (_syncState.value != "Modo Offline") {
            CoroutineScope(Dispatchers.IO).launch {
                kotlinx.coroutines.delay(1500)
                val replyText = when (recipientRole) {
                    "Docente" -> "Hola, entiendo la situación. Estaré revisando las tareas y el reporte del estudiante mañana a primera hora. Saludos."
                    "DECE" -> "Saludos cordiales. He recibido su mensaje y estaré dando seguimiento al caso del estudiante. Agendemos una cita si es necesario."
                    "Padre" -> "Estimado representante, le saluda el docente. Quería informarle que el estudiante ha mejorado notablemente en clase. Sigamos trabajando juntos."
                    "Autoridades" -> "Estimado, el reporte ha sido recibido y aprobado. Manténganos informados sobre cualquier novedad disciplinaria relevante."
                    else -> "Mensaje recibido. Gracias por comunicarse con la institución."
                }
                val responseMsg = ChatMessage(
                    senderName = "Simulador ($recipientRole)",
                    senderRole = recipientRole,
                    recipientRole = senderRole,
                    message = replyText
                )
                dao.insertChatMessage(responseMsg)

                addNotification(
                    "Nuevo mensaje de chat",
                    "De ${responseMsg.senderName}: \"${if (replyText.length > 40) replyText.take(40) + "..." else replyText}\"",
                    "Comunicado"
                )
            }
        }
    }

    suspend fun addNotification(title: String, message: String, category: String) {
        val notification = SchoolNotification(
            title = title,
            message = message,
            category = category
        )
        dao.insertNotification(notification)
    }

    suspend fun markNotificationAsRead(id: Int) {
        dao.markNotificationAsRead(id)
    }

    suspend fun completeActivity(activity: SchoolActivity) {
        dao.updateActivity(activity.copy(isCompleted = true))
        addNotification(
            "Tarea Entregada",
            "Se ha marcado la actividad \"${activity.title}\" como completada.",
            "Evaluación"
        )
    }

    suspend fun addStudentAttendanceNovedad(student: Student, behavior: String, academic: String) {
        val updated = student.copy(
            behaviorStatus = behavior,
            academicStatus = academic,
            attendanceRate = if (behavior == "Falta Injustificada") student.attendanceRate - 5.0 else student.attendanceRate
        )
        dao.updateStudent(updated)

        val detail = "Conducta: $behavior, Nivel Académico: $academic"
        addNotification(
            "Reporte de Novedad Escolar",
            "Estudiante ${student.name} tiene un nuevo reporte. $detail",
            if (behavior == "Grave" || academic == "En Riesgo") "Urgente" else "Asistencia"
        )

        // Automatically trigger DECE alert if student becomes "En Riesgo" or "Grave"
        if (behavior == "Grave" || academic == "En Riesgo") {
            addDeceCase(
                studentId = student.id,
                studentName = student.name,
                title = "Alerta Automática: Riesgo Académico/Conductual",
                description = "El sistema detectó un nivel crítico en el reporte del docente para este estudiante. Requiere evaluación.",
                priority = "Alta"
            )
        }
    }

    suspend fun populateInitialDataIfEmpty() {
        withContext(Dispatchers.IO) {
            val studentCount = dao.getAllStudents().first().size
            if (studentCount == 0) {
                Log.d(TAG, "Database is empty. Populating initial mock data...")

                // Insert Students
                val initialStudents = listOf(
                    Student("STU001", "María Paula Chimbo", "10mo EGB", 94.5, "Excelente", "Excelente"),
                    Student("STU002", "Juan Carlos Andrade", "10mo EGB", 78.0, "Novedad", "En Riesgo"),
                    Student("STU003", "Mateo Alejandro Noboa", "9no EGB", 96.0, "Excelente", "Excelente"),
                    Student("STU004", "Sofía Valentina Castro", "10mo EGB", 82.5, "Novedad", "Regular"),
                    Student("STU005", "Dilan Josué Guanoluisa", "11vo Bachillerato", 65.0, "Grave", "En Riesgo")
                )
                dao.insertStudents(initialStudents)

                // Insert Schedules
                val initialSchedules = listOf(
                    SchoolSchedule(dayOfWeek = "Lunes", timeSlot = "07:15 - 08:00", subject = "Matemáticas (Álgebra)", grade = "10mo EGB"),
                    SchoolSchedule(dayOfWeek = "Lunes", timeSlot = "08:00 - 08:45", subject = "Matemáticas (Álgebra)", grade = "10mo EGB"),
                    SchoolSchedule(dayOfWeek = "Martes", timeSlot = "08:45 - 09:30", subject = "Física Elemental", grade = "11vo Bachillerato"),
                    SchoolSchedule(dayOfWeek = "Miércoles", timeSlot = "09:50 - 10:35", subject = "Lengua y Literatura", grade = "9no EGB"),
                    SchoolSchedule(dayOfWeek = "Jueves", timeSlot = "10:35 - 11:20", subject = "Ciencias Naturales", grade = "10mo EGB"),
                    SchoolSchedule(dayOfWeek = "Viernes", timeSlot = "11:20 - 12:05", subject = "Historia del Ecuador", grade = "11vo Bachillerato")
                )
                dao.insertSchedules(initialSchedules)

                // Insert Activities
                val initialActivities = listOf(
                    SchoolActivity("ACT001", "Taller de Ecuaciones Cuadráticas", "Resolver ejercicios de factorización del módulo pág 45.", "Matemáticas", "Mañana, 08:00"),
                    SchoolActivity("ACT002", "Lectura Crítica: Huasipungo", "Redactar ensayo analítico de 3 páginas sobre el realismo social.", "Lengua y Literatura", "En 3 días"),
                    SchoolActivity("ACT003", "Proyecto de Ciencias: Maqueta de Célula", "Crear maqueta comestible de célula eucariota y rotular partes.", "Ciencias Naturales", "El próximo Lunes")
                )
                dao.insertActivities(initialActivities)

                // Insert Initial DECE Case
                val initialCase = DeceCase(
                    studentId = "STU002",
                    studentName = "Juan Carlos Andrade",
                    title = "Bajo rendimiento continuo y faltas reiteradas",
                    description = "El estudiante no presenta tareas y se muestra distraído. Representante reporta problemas familiares en casa.",
                    status = "Abierto",
                    priority = "Media",
                    lastInterviewDate = "Registrado hoy: Reunión inicial agendada."
                )
                dao.insertDeceCase(initialCase)

                // Insert Initial Chat Messages
                val initialChats = listOf(
                    ChatMessage(senderName = "María Paula Chimbo", senderRole = "Estudiantes", recipientRole = "Docente", message = "Estimado profesor, ¿podría enviarme el material de la clase de álgebra de hoy?"),
                    ChatMessage(senderName = "Simulador (Docente)", senderRole = "Docente", recipientRole = "Estudiantes", message = "Claro María Paula, ya está disponible en la sección de material didáctico. Saludos.")
                )
                initialChats.forEach { dao.insertChatMessage(it) }

                // Insert Initial Notifications
                val initialNotifications = listOf(
                    SchoolNotification(title = "Inicio de Ciclo Escolar", message = "Bienvenidos al nuevo periodo lectivo. Recuerde revisar sus horarios y planificaciones.", category = "Comunicado"),
                    SchoolNotification(title = "Alerta DECE: Juan Carlos Andrade", message = "Se ha asignado un nuevo caso de seguimiento para el estudiante de 10mo EGB.", category = "DECE")
                )
                initialNotifications.forEach { dao.insertNotification(it) }

                // Insert User Accounts (Supports both quick fallback logins and personalized initial-based usernames)
                val initialUserAccounts = listOf(
                    // 1. Administrators
                    UserAccount("aalvarezp", "aalvarezp", "Ana Alvarez Palacios", "Administrador"),
                    UserAccount("admin", "admin", "Administrador del Sistema", "Administrador"),

                    // 2. Docentes (Teachers & Course Tutors)
                    UserAccount("jruizs", "jruizs", "Javier Ruiz Solís", "Docente"),
                    UserAccount("docente", "docente", "Prof. Javier Ruiz Solís", "Docente"),
                    UserAccount("lmerizalden", "lmerizalden", "Laura Merizalde Noboa", "Docente"), // Tutor 9no
                    UserAccount("fortegac", "fortegac", "Fabián Ortega Cevallos", "Docente"), // Tutor 11vo

                    // 3. DECE Counselors
                    UserAccount("acastroo", "acastroo", "Ana Belén Castro Ortiz", "DECE"),
                    UserAccount("dece", "dece", "Psi. Ana Belén Castro", "DECE"),
                    UserAccount("despinozat", "despinozat", "Daniela Espinoza Terán", "DECE"), // Consejera de Básica

                    // 4. Padres (Parent representatives of each course)
                    UserAccount("candradep", "candradep", "Carlos Andrade Pazmiño", "Padres"), // Rep 10mo
                    UserAccount("padre", "padre", "Rep. Carlos Andrade Pazmiño", "Padres"),
                    UserAccount("wponcev", "wponcev", "Wilson Ponce Vaca", "Padres"), // Rep 9no
                    UserAccount("bortizp", "bortizp", "Beatriz Ortiz Palacios", "Padres"), // Rep 11vo

                    // 5. Estudiantes (Students)
                    UserAccount("mchimbos", "mchimbos", "María Paula Chimbo Silva", "Estudiantes"),
                    UserAccount("estudiante", "estudiante", "María Paula Chimbo Silva", "Estudiantes"),

                    // 6. Autoridades
                    UserAccount("esilvam", "esilvam", "Eliana Silva Mendoza", "Autoridades"),
                    UserAccount("autoridad", "autoridad", "Dra. Eliana Silva Mendoza", "Autoridades")
                )
                initialUserAccounts.forEach { dao.insertUserAccount(it) }

                // Insert Default Role Permissions
                val defaultRolePermissions = listOf(
                    RolePermission("Docente", canAccessDashboard = true, canAccessStudents = true, canAccessDECE = false, canAccessChat = true, canAccessIA = true, canAccessNotificaciones = true),
                    RolePermission("DECE", canAccessDashboard = true, canAccessStudents = false, canAccessDECE = true, canAccessChat = true, canAccessIA = true, canAccessNotificaciones = true),
                    RolePermission("Padres", canAccessDashboard = true, canAccessStudents = false, canAccessDECE = false, canAccessChat = true, canAccessIA = false, canAccessNotificaciones = true),
                    RolePermission("Estudiantes", canAccessDashboard = true, canAccessStudents = false, canAccessDECE = false, canAccessChat = true, canAccessIA = true, canAccessNotificaciones = true),
                    RolePermission("Autoridades", canAccessDashboard = true, canAccessStudents = true, canAccessDECE = true, canAccessChat = true, canAccessIA = true, canAccessNotificaciones = true),
                    RolePermission("Administrador", canAccessDashboard = true, canAccessStudents = true, canAccessDECE = true, canAccessChat = true, canAccessIA = true, canAccessNotificaciones = true)
                )
                dao.insertRolePermissions(defaultRolePermissions)

                // Insert Default Institution Profiles
                val defaultInstitutions = listOf(
                    InstitutionProfile(
                        id = 1,
                        isActive = true,
                        officialName = "Unidad Educativa Fiscal Manuela Cañizares",
                        shortName = "U.E. Manuela Cañizares",
                        amieCode = "17H00587",
                        institutionType = "Fiscal",
                        hasInicial = true,
                        hasEgb = true,
                        hasBachillerato = true,
                        hasTecnico = false,
                        hasMatutina = true,
                        hasVespertina = true,
                        hasNocturna = false,
                        regimen = "Sierra",
                        distrito = "17D05",
                        circuito = "17D05C04",
                        zona = "Zona 9",
                        provincia = "Pichincha",
                        canton = "Quito",
                        parroquia = "Chaupicruz",
                        address = "Av. 10 de Agosto N24-11 y Carrión",
                        phone = "02-2543-324",
                        email = "uef_manuelacanizares@educacion.gob.ec",
                        website = "www.manuelacanizares.edu.ec",
                        logoPrincipal = "🏫",
                        escudo = "🛡️",
                        logoMinisterio = "🇪🇨",
                        logoDistrito = "🏢",
                        logoCircuito = "📍",
                        marcaAgua = "UNIDAD EDUCATIVA FISCAL MANUELA CAÑIZARES - OFICIAL",
                        primaryColor = "#0F172A", // Slate Dark
                        secondaryColor = "#3B82F6", // Vibrant Blue
                        tertiaryColor = "#EF4444", // Crimson Red
                        buttonColor = "#3B82F6",
                        headerColor = "#0F172A",
                        tableColor = "#F1F5F9",
                        backgroundColor = "#FFFFFF",
                        linkColor = "#2563EB",
                        menuColor = "#0F172A",
                        dashboardColor = "#F8FAFC",
                        rectorName = "Dra. Eliana Silva Mendoza",
                        vicerrectorName = "Mgs. Javier Ruiz Solís",
                        inspectorName = "Lic. Carlos Andrade Pazmiño",
                        docenteName = "Prof. Javier Ruiz Solís",
                        deceName = "Psi. Ana Belén Castro",
                        secretariaName = "Abg. Laura Merizalde Noboa"
                    ),
                    InstitutionProfile(
                        id = 2,
                        isActive = false,
                        officialName = "Colegio Municipal Sebastián de Benalcázar",
                        shortName = "Colegio Benalcázar",
                        amieCode = "17H01334",
                        institutionType = "Municipal",
                        hasInicial = false,
                        hasEgb = true,
                        hasBachillerato = true,
                        hasTecnico = true,
                        hasMatutina = true,
                        hasVespertina = false,
                        hasNocturna = false,
                        regimen = "Sierra",
                        distrito = "17D06",
                        circuito = "17D06C08",
                        zona = "Zona 9",
                        provincia = "Pichincha",
                        canton = "Quito",
                        parroquia = "Iñaquito",
                        address = "Av. 6 de Diciembre N31-23 y Portugal",
                        phone = "02-2254-884",
                        email = "benalcazar@quito.gob.ec",
                        website = "www.benalcazar.edu.ec",
                        logoPrincipal = "🔬",
                        escudo = "👑",
                        logoMinisterio = "🇪🇨",
                        logoDistrito = "🏢",
                        logoCircuito = "📍",
                        marcaAgua = "COLEGIO MUNICIPAL SEBASTIÁN DE BENALCÁZAR - COPIA CONTROLADA",
                        primaryColor = "#120B44", // Deep Violet Indigo
                        secondaryColor = "#EAB308", // Yellow Gold
                        tertiaryColor = "#10B981", // Emerald Green
                        buttonColor = "#EAB308",
                        headerColor = "#120B44",
                        tableColor = "#FEF08A", // Tinted Table Yellow
                        backgroundColor = "#FAFDF9",
                        linkColor = "#EAB308",
                        menuColor = "#120B44",
                        dashboardColor = "#FEFEE9",
                        rectorName = "Dr. Fabián Ortega Cevallos",
                        vicerrectorName = "Mgs. Daniela Espinoza Terán",
                        inspectorName = "Lic. Wilson Ponce Vaca",
                        docenteName = "Dra. Laura Merizalde Noboa",
                        deceName = "Psi. Beatriz Ortiz Palacios",
                        secretariaName = "Abg. Ana Belén Castro"
                    )
                )
                defaultInstitutions.forEach { dao.insertInstitution(it) }

                // Insert Default Custom Templates (inspired by Ministerio format)
                val defaultTemplates = listOf(
                    CustomTemplate(
                        institutionId = 1,
                        name = "Planificación de Unidad Microcurricular",
                        documentType = "Planificación",
                        isCustom = false,
                        isMinistryDefault = true
                    ),
                    CustomTemplate(
                        institutionId = 1,
                        name = "Informe de Derivación DECE Nacional",
                        documentType = "Informe DECE",
                        isCustom = false,
                        isMinistryDefault = true
                    ),
                    CustomTemplate(
                        institutionId = 1,
                        name = "Acta de Compromiso y Corresponsabilidad Educativa",
                        documentType = "Acta",
                        isCustom = false,
                        isMinistryDefault = true
                    ),
                    CustomTemplate(
                        institutionId = 2,
                        name = "Planificación Curricular Anual (PCA) Benalcázar",
                        documentType = "Planificación",
                        isCustom = true,
                        isMinistryDefault = false
                    ),
                    CustomTemplate(
                        institutionId = 2,
                        name = "Certificado de Matrícula Oficial",
                        documentType = "Certificado",
                        isCustom = false,
                        isMinistryDefault = true
                    )
                )
                defaultTemplates.forEach { dao.insertCustomTemplate(it) }
            }
        }
    }
}
