package com.dualcal.widget

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat

/**
 * Lee los próximos eventos del CalendarProvider del sistema, es decir, los
 * que la app de Google Calendar ya tiene sincronizados en el teléfono.
 * Así el widget no necesita red ni inicio de sesión propios.
 */
object CalendarRepositorio {

    fun hayPermiso(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    private val PROYECCION = arrayOf(
        CalendarContract.Instances.EVENT_ID,           // 0
        CalendarContract.Instances.TITLE,              // 1
        CalendarContract.Instances.BEGIN,              // 2 (epoch ms, UTC)
        CalendarContract.Instances.END,                // 3
        CalendarContract.Instances.EVENT_LOCATION,     // 4
        CalendarContract.Instances.ALL_DAY,            // 5
        CalendarContract.Instances.DISPLAY_COLOR,      // 6 (color efectivo)
        CalendarContract.Instances.SELF_ATTENDEE_STATUS // 7
    )

    fun proximosEventos(ctx: Context, dias: Int = 7, maxEventos: Int = 50): List<Evento> {
        if (!hayPermiso(ctx)) return emptyList()

        val ahora = System.currentTimeMillis()
        val hasta = ahora + dias.toLong() * 24L * 3_600_000L

        // Instances expande automáticamente las recurrencias dentro del rango
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(ahora.toString())
            .appendPath(hasta.toString())
            .build()

        val eventos = ArrayList<Evento>()
        ctx.contentResolver.query(
            uri, PROYECCION, null, null,
            CalendarContract.Instances.BEGIN + " ASC"
        )?.use { c ->
            while (c.moveToNext() && eventos.size < maxEventos) {
                // Descartar invitaciones rechazadas
                if (c.getInt(7) == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED) continue

                val finMs = c.getLong(3)
                if (finMs <= ahora) continue // ya terminado

                val color = c.getInt(6)
                eventos.add(
                    Evento(
                        id = c.getLong(0),
                        titulo = c.getString(1)?.takeIf { it.isNotBlank() } ?: "(Sin título)",
                        inicioMs = c.getLong(2),
                        finMs = finMs,
                        ubicacion = c.getString(4)?.takeIf { it.isNotBlank() },
                        todoElDia = c.getInt(5) == 1,
                        // Forzar opacidad por si el color viene sin canal alfa
                        color = if (color == 0) 0xFF4F8CFF.toInt() else color or 0xFF000000.toInt()
                    )
                )
            }
        }
        return eventos
    }
}
