package com.dualcal.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService

/** Servicio que conecta la lista del widget con los datos del calendario. */
class AgendaRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        AgendaFactory(applicationContext)
}

/** "Adaptador" del widget: produce una fila (RemoteViews) por evento. */
class AgendaFactory(private val ctx: Context) : RemoteViewsService.RemoteViewsFactory {

    private var eventos: List<Evento> = emptyList()

    override fun onCreate() {}
    override fun onDestroy() { eventos = emptyList() }

    // Se llama en un hilo aparte: aquí sí podemos consultar el proveedor
    override fun onDataSetChanged() {
        eventos = CalendarRepositorio.proximosEventos(ctx)
    }

    override fun getCount(): Int = eventos.size
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = eventos[position].id
    override fun hasStableIds(): Boolean = true
    override fun getLoadingView(): RemoteViews? = null

    override fun getViewAt(position: Int): RemoteViews {
        val ev = eventos[position]
        val rv = RemoteViews(ctx.packageName, R.layout.widget_item)

        // Píldora con TODO el fondo del color del evento (colores de Google)
        rv.setInt(R.id.fondo, "setColorFilter", ev.color)

        // Texto que contraste con ese fondo: oscuro si es claro, blanco si es oscuro
        val texto = colorTexto(ev.color)
        val tenue = conAlpha(texto, 0xCC) // ubicación y hora ES, algo atenuadas
        rv.setTextColor(R.id.titulo, texto)
        rv.setTextColor(R.id.ubicacion, tenue)
        rv.setTextColor(R.id.hora_pri, texto)
        rv.setTextColor(R.id.hora_sec, tenue)

        rv.setTextViewText(R.id.titulo, ev.titulo)
        if (ev.ubicacion != null) {
            rv.setTextViewText(R.id.ubicacion, ev.ubicacion)
            rv.setViewVisibility(R.id.ubicacion, View.VISIBLE)
        } else {
            rv.setViewVisibility(R.id.ubicacion, View.GONE)
        }

        // Doble huso: Asunción arriba, Madrid (ES) debajo
        if (ev.todoElDia) {
            rv.setTextViewText(R.id.hora_pri, "Todo el día")
            rv.setViewVisibility(R.id.hora_sec, View.GONE)
        } else {
            rv.setViewVisibility(R.id.hora_sec, View.VISIBLE)
            rv.setTextViewText(R.id.hora_pri, Huso.horaPrimaria(ev.inicioMs))
            rv.setTextViewText(R.id.hora_sec, Huso.horaSecundaria(ev.inicioMs))
        }

        // Propaga el clic al PendingIntent plantilla (abre DualCal)
        rv.setOnClickFillInIntent(R.id.item_root, Intent())
        return rv
    }

    /** Negro sobre fondos claros, blanco sobre oscuros (luminancia percibida). */
    private fun colorTexto(fondo: Int): Int {
        val r = (fondo shr 16) and 0xFF
        val g = (fondo shr 8) and 0xFF
        val b = fondo and 0xFF
        val luminancia = 0.299 * r + 0.587 * g + 0.114 * b
        return if (luminancia > 150) 0xFF10131A.toInt() else 0xFFFFFFFF.toInt()
    }

    private fun conAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or (alpha shl 24)
}
