package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class Student(
    @PrimaryKey val id: String,
    val name: String,
    val grade: String,
    val attendanceRate: Double,
    val behaviorStatus: String, // "Excelente", "Novedad", "Grave"
    val academicStatus: String, // "Excelente", "Regular", "En Riesgo"
    val photoUrl: String? = null
)

@Entity(tableName = "dece_cases")
data class DeceCase(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val studentName: String,
    val title: String,
    val description: String,
    val status: String, // "Abierto", "En Seguimiento", "Resuelto"
    val priority: String, // "Alta", "Media", "Baja"
    val lastInterviewDate: String,
    val isSynced: Boolean = true
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderName: String,
    val senderRole: String, // e.g. "Docente", "Padre", "DECE", "Autoridades"
    val recipientRole: String, // e.g. "Padre", "Docente", "DECE"
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val attachmentUrl: String? = null,
    val attachmentType: String? = null // "PDF", "IMAGE", "VOICE"
)

@Entity(tableName = "school_notifications")
data class SchoolNotification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val category: String, // "Evaluación", "Asistencia", "Comunicado", "Urgente", "DECE"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "school_activities")
data class SchoolActivity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val subject: String,
    val dueDate: String,
    val isCompleted: Boolean = false
)

@Entity(tableName = "schedules")
data class SchoolSchedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dayOfWeek: String, // "Lunes", "Martes", "Miércoles", "Jueves", "Viernes"
    val timeSlot: String, // "07:15 - 08:00", etc.
    val subject: String,
    val grade: String
)

@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey val username: String,
    val password: String,
    val fullName: String,
    val role: String, // "Docente", "DECE", "Padres", "Estudiantes", "Autoridades", "Administrador"
    val isActive: Boolean = true
)

@Entity(tableName = "role_permissions")
data class RolePermission(
    @PrimaryKey val role: String, // e.g. "Docente", "DECE", etc.
    val canAccessDashboard: Boolean = true,
    val canAccessStudents: Boolean = true,
    val canAccessDECE: Boolean = true,
    val canAccessChat: Boolean = true,
    val canAccessIA: Boolean = true,
    val canAccessNotificaciones: Boolean = true
)

@Entity(tableName = "institution_profiles")
data class InstitutionProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val isActive: Boolean = false,
    
    // DATOS GENERALES
    val officialName: String,
    val shortName: String,
    val amieCode: String,
    val institutionType: String, // "Fiscal", "Fiscomisional", "Particular", "Municipal"
    
    // NIVELES EDUCATIVOS
    val hasInicial: Boolean = true,
    val hasEgb: Boolean = true,
    val hasBachillerato: Boolean = true,
    val hasTecnico: Boolean = false,

    // JORNADAS
    val hasMatutina: Boolean = true,
    val hasVespertina: Boolean = true,
    val hasNocturna: Boolean = false,
    
    // RÉGIMEN
    val regimen: String, // "Costa", "Sierra", "Amazonía", "Galápagos"
    
    // GEOLOCALIZACIÓN Y CONTACTO
    val distrito: String = "Distrito 17D05",
    val circuito: String = "Circuito 04_05",
    val zona: String = "Zona 9",
    val provincia: String = "Pichincha",
    val canton: String = "Quito",
    val parroquia: String = "Chaupicruz",
    val address: String = "Av. Amazonas N34-451 y Atahualpa",
    val phone: String = "02-3961-300",
    val email: String = "contacto@educaecuador.gob.ec",
    val website: String = "www.educacion.gob.ec",
    
    // IDENTIDAD VISUAL
    val logoPrincipal: String = "🏫", // fallback emoji or text descriptor
    val escudo: String = "🛡️",
    val logoMinisterio: String = "🇪🇨",
    val logoDistrito: String = "🏢",
    val logoCircuito: String = "📍",
    val marcaAgua: String = "Borrador de Validación",
    val firmaInstitucional: String = "✍️ Firma Digitalizada",
    val selloInstitucional: String = "🖃 Sello Húmedo",
    
    // COLORES INSTITUCIONALES (Hex strings)
    val primaryColor: String = "#1E3A8A", // Deep Blue
    val secondaryColor: String = "#0D9488", // Teal
    val tertiaryColor: String = "#F59E0B", // Amber
    val buttonColor: String = "#1E3A8A",
    val headerColor: String = "#1E3A8A",
    val tableColor: String = "#F3F4F6",
    val backgroundColor: String = "#FFFFFF",
    val linkColor: String = "#2563EB",
    val menuColor: String = "#1E293B",
    val dashboardColor: String = "#F8FAFC",
    val isDarkMode: Boolean = false,
    
    // TIPOGRAFÍA
    val mainFont: String = "Roboto", // Roboto, Inter, Montserrat, Space Grotesk
    val secondaryFont: String = "Inter",
    val titleSize: Float = 22f,
    val subtitleSize: Float = 16f,
    val contentSize: Float = 14f,
    val lineSpacing: Float = 1.2f,
    val letterSpacing: Float = 0.5f,
    val alignment: String = "Justificado", // "Izquierda", "Centrado", "Justificado"
    
    // PLANTILLA SELECCIONADA
    val selectedTemplate: String = "Modelo del Ministerio", // Modelo del Ministerio, Moderno Alternativo, Solemne Clásico
    val showLogo: Boolean = true,
    val showEscudo: Boolean = true,
    val showHeader: Boolean = true,
    val showFooter: Boolean = true,
    val showQr: Boolean = true,
    val showBarcode: Boolean = true,
    val showPageNumber: Boolean = true,
    val showDateTime: Boolean = true,
    val showUser: Boolean = true,
    val showSignature: Boolean = true,
    val showSello: Boolean = true,
    
    // TEXTO FIJO
    val fixedText: String = "Documento oficial generado bajo la Ley Orgánica de Educación Intercultural (LOEI).",
    
    // FIRMAS DIGITALES (Nombres y Cargos)
    val rectorName: String = "Dra. Eliana Silva Mendoza (Rector/a)",
    val vicerrectorName: String = "Mgs. Javier Ruiz Solís (Vicerrector/a)",
    val inspectorName: String = "Lic. Carlos Andrade Pazmiño (Inspector/a General)",
    val docenteName: String = "Prof. Javier Ruiz Solís (Docente)",
    val deceName: String = "Psi. Ana Belén Castro (Coordinador/a DECE)",
    val secretariaName: String = "Abg. Laura Merizalde Noboa (Secretario/a)"
)

@Entity(tableName = "custom_templates")
data class CustomTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val institutionId: Int,
    val name: String,
    val documentType: String, // "Planificación", "Informe DECE", "Acta", "Certificado", "Memorando", "Boletín"
    val isCustom: Boolean = true,
    val isMinistryDefault: Boolean = false,
    val showLogo: Boolean = true,
    val showEscudo: Boolean = true,
    val showQr: Boolean = true,
    val alignment: String = "Centrado",
    val lastUpdated: Long = System.currentTimeMillis()
)

