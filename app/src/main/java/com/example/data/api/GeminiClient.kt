package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Generates a text response from the Gemini model.
     * @param prompt The prompt to send to the model.
     * @param systemInstruction Optional system instruction to guide the model's persona/behavior.
     */
    suspend fun generateContent(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "API Key is missing or using default placeholder! Falling back to local educational template generator.")
            return@withContext "[NOTA: Utilizando el motor de plantillas local educativo porque no se ha configurado la clave de API de Gemini en los Secretos de AI Studio. Para usar la IA de Google en tiempo real, agregue su GEMINI_API_KEY en el panel de secretos.]\n\n" + generateFallbackContent(prompt)
        }

        try {
            // Construct the request body using org.json
            val requestJson = JSONObject()

            // Contents list
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            // Optional System Instruction
            if (!systemInstruction.isNullOrBlank()) {
                val systemInstructionObj = JSONObject()
                val systemPartsArray = JSONArray()
                val systemPartObj = JSONObject()
                systemPartObj.put("text", systemInstruction)
                systemPartsArray.put(systemPartObj)
                systemInstructionObj.put("parts", systemPartsArray)
                requestJson.put("systemInstruction", systemInstructionObj)
            }

            // Optional Generation Config (e.g. temperature)
            val generationConfig = JSONObject()
            generationConfig.put("temperature", 0.7)
            requestJson.put("generationConfig", generationConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                if (!response.isSuccessful || responseBody == null) {
                    Log.e(TAG, "Unsuccessful response from Gemini: Code ${response.code}, Body: $responseBody")
                    return@withContext "[NOTA: Error del servidor de IA (Código ${response.code}). Utilizando el motor de plantillas local como respaldo.]\n\n" + generateFallbackContent(prompt)
                }

                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "Sin respuesta.")
                        }
                    }
                }
                return@withContext "[NOTA: No se generó ninguna respuesta por la API. Utilizando motor local de respaldo.]\n\n" + generateFallbackContent(prompt)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini API call", e)
            return@withContext "[NOTA: Error de conexión (${e.localizedMessage}). Utilizando motor de plantillas local de respaldo.]\n\n" + generateFallbackContent(prompt)
        }
    }

    private fun generateFallbackContent(prompt: String): String {
        val lowercasePrompt = prompt.lowercase()
        return when {
            lowercasePrompt.contains("planif") || lowercasePrompt.contains("curricular") || lowercasePrompt.contains("docente") || lowercasePrompt.contains("clase") -> {
                """
                PLANIFICACIÓN MICROCURRICULAR SEMANAL DE REFUERZO ACADÉMICO
                UNIDAD EDUCATIVA FISCAL {{institucion}}
                Distrito: {{distrito}} | Año Lectivo: {{anio_lectivo}}

                1. DATOS INFORMATIVOS
                Docente Tutor: {{docente}}
                Grado: Noveno Año de EGB | Paralelo: "A"
                Asignatura: Lengua y Literatura / Matemáticas

                2. OBJETIVO DE APRENDIZAJE
                Fortalecer las destrezas con criterio de desempeño que presentan bajo rendimiento mediante el desarrollo de tutorías dirigidas y talleres interactivos que motiven la autorregulación escolar.

                3. MATRIZ DE PLANIFICACIÓN
                - Destreza: LL.4.1.2. Construir significados implícitos a partir de textos literarios.
                - Estrategia: Lectura crítica en parejas, subrayado de ideas principales y foro abierto.
                - Evaluación: Rúbrica de análisis de lectura y carpeta de evidencias físicas.

                4. RECOMENDACIONES PARA EL REPRESENTANTE LEGAL
                Se exhorta a {{representante}} a realizar un control riguroso de las horas de estudio en casa y asegurar la asistencia de {{estudiante}} a las jornadas vespertinas.
                """.trimIndent()
            }
            lowercasePrompt.contains("acta") || lowercasePrompt.contains("compromiso") || lowercasePrompt.contains("asistencia") || lowercasePrompt.contains("padre") || lowercasePrompt.contains("representante") -> {
                """
                ACTA DE COMPROMISO EDUCATIVO Y ASISTENCIA DE LA LOEI
                UNIDAD EDUCATIVA FISCAL {{institucion}}
                Código AMIE: {{codigo_amie}} | Distrito: {{distrito}}

                En las instalaciones de la Unidad Educativa {{institucion}}, a los {{fecha}}, se reúne el docente tutor, {{docente}}, el profesional DECE, {{dece}}, y el representante legal, {{representante}}, con el fin de suscribir el presente acuerdo de apoyo académico integral del estudiante {{estudiante}}.

                El representante legal se compromete solemnemente a:
                1. Garantizar la asistencia diaria y puntual de su representado a las jornadas de clase y refuerzo académico.
                2. Controlar diariamente el cumplimiento de las tareas y actividades curriculares enviadas a casa.
                3. Acudir de manera puntual a todas las convocatorias, talleres y citaciones que realice la institución.
                4. Colaborar activamente en las actividades extracurriculares en beneficio de la comunidad.

                Por su parte, la institución se compromete a brindar el acompañamiento pedagógico e intervenciones oportunas para velar por el interés superior del menor y su óptimo desarrollo.
                """.trimIndent()
            }
            lowercasePrompt.contains("dece") || lowercasePrompt.contains("psicol") || lowercasePrompt.contains("conducta") || lowercasePrompt.contains("informe") || lowercasePrompt.contains("caso") -> {
                """
                INFORME DE DERIVACIÓN Y SEGUIMIENTO PSICOPEDAGÓGICO DECE
                UNIDAD EDUCATIVA FISCAL {{institucion}}
                Departamento de Consejería Estudiantil | Distrito: {{distrito}}

                A: Rectorado de la Unidad Educativa {{institucion}}
                DE: Coordinación del Departamento de Consejería Estudiantil (DECE)
                ASUNTO: Informe de derivación y medidas de adaptación psicopedagógica

                1. DATOS DEL ESTUDIANTE
                Nombres: {{estudiante}}
                Grado/Curso: Décimo Año de Educación General Básica
                Profesional Evaluador: {{dece}}

                2. HALLAZGOS Y ANTECEDENTES
                Se observa que el estudiante experimenta desmotivación académica significativa y dificultades de concentración durante las evaluaciones escritas. No se registran incidentes disciplinarios graves, pero sí se denota una necesidad crítica de acompañamiento psicoemocional en el núcleo familiar que requiere la intervención directa de {{dece}}.

                3. RECOMENDACIONES DE ADAPTACIÓN
                - Adaptar evaluaciones a formatos orales o de opción múltiple según el nivel de dificultad.
                - Brindar tiempos adicionales durante las lecciones formales para reducir la ansiedad.
                - Seguimiento semanal obligatorio por el profesional {{dece}} con informes mensuales a la junta.
                """.trimIndent()
            }
            else -> {
                """
                DOCUMENTO OFICIAL GENERADO POR EL ASISTENTE INTELIGENTE
                UNIDAD EDUCATIVA FISCAL {{institucion}}
                Código AMIE: {{codigo_amie}} | Distrito: {{distrito}}

                Referencia: SOLICITUD DE REFORMA PEDAGÓGICA Y ESTRUCTURACIÓN
                Solicitante: {{docente}} / {{rector}}
                Año Lectivo: {{anio_lectivo}}

                Estimada comunidad educativa, de conformidad con las disposiciones generales de la Ley Orgánica de Educación Intercultural (LOEI) del Ecuador, se emite este documento formal para reglamentar los procesos de co-evaluación y refuerzo académico continuo.

                Este documento ha sido generado en base a la solicitud del usuario: "$prompt".
                Téngase en cuenta que esta plantilla es completamente editable en el panel lateral de Canva y contiene variables adaptativas autogestionadas por el sistema de base de datos local SQLite.
                """.trimIndent()
            }
        }
    }
}
