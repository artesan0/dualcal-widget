package com.dualcal.widget

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Un evento ya listo para pintar en el widget. */
data class Evento(
    val id: Long,
    val titulo: String,
    val inicioMs: Long,
    val finMs: Long,
    val ubicacion: String?,
    val todoElDia: Boolean,
    val color: Int
)

/**
 * Conversión a DOBLE HUSO con java.time. Nunca se usan offsets fijos: las
 * zonas IANA aplican solas el horario de verano (Madrid lo tiene; Asunción
 * no desde 2024). El instante de inicio que da el CalendarProvider es UTC
 * absoluto, así que basta con proyectarlo a cada zona.
 */
object Huso {
    val PRIMARIA: ZoneId = ZoneId.of("America/Asuncion")
    val SECUNDARIA: ZoneId = ZoneId.of("Europe/Madrid")
    const val ETIQUETA_SECUNDARIA = "ES"

    private val HM = DateTimeFormatter.ofPattern("HH:mm")
    private val ES = Locale("es", "ES")

    fun horaPrimaria(ms: Long): String =
        Instant.ofEpochMilli(ms).atZone(PRIMARIA).format(HM)

    /** Hora en Madrid con sufijo "ES" y marca +1/−1 si cae en otro día. */
    fun horaSecundaria(ms: Long): String {
        val inst = Instant.ofEpochMilli(ms)
        val pri = inst.atZone(PRIMARIA).toLocalDate()
        val sec = inst.atZone(SECUNDARIA)
        val marca = when {
            sec.toLocalDate().isAfter(pri) -> "⁺¹"
            sec.toLocalDate().isBefore(pri) -> "⁻¹"
            else -> ""
        }
        return "${sec.format(HM)}$marca $ETIQUETA_SECUNDARIA"
    }

    /** Texto de la cabecera de día: "Hoy · …", "Mañana · …" o la fecha. */
    fun etiquetaDia(dia: LocalDate): String {
        val hoy = LocalDate.now(PRIMARIA)
        val fecha = dia.format(DateTimeFormatter.ofPattern("EEEE d MMM", ES))
            .replaceFirstChar { it.uppercase() }
        return when (dia) {
            hoy -> "Hoy · $fecha"
            hoy.plusDays(1) -> "Mañana · $fecha"
            else -> fecha
        }
    }
}
