package com.dualcal.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Servicio que conecta la lista del widget con los datos del calendario. */
class AgendaRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        AgendaFactory(applicationContext)
}

/**
 * "Adaptador" del widget. Produce dos tipos de fila: cabecera de día
 * (separador) y evento. Así la lista no mezcla días sin avisar.
 */
class AgendaFactory(private val ctx: Context) : RemoteViewsService.RemoteViewsFactory {

    private sealed class Fila
    private class Cabecera(val texto: String) : Fila()
    private class Item(val evento: Evento) : Fila()

    private var filas: List<Fila> = emptyList()

    override fun onCreate() {}
    override fun onDestroy() { filas = emptyList() }

    override fun onDataSetChanged() {
        // Ordenar por día de calendario, luego "todo el día" primero, luego hora.
        // Hace falta porque los eventos de todo el día se guardan en UTC y su
        // instante puede caer en otro día respecto a los eventos con hora.
        val eventos = CalendarRepositorio.proximosEventos(ctx)
            .sortedWith(
                compareBy(
                    { diaCalendario(it).toEpochDay() },
                    { if (it.todoElDia) 0 else 1 },
                    { it.inicioMs }
                )
            )
        val nuevas = ArrayList<Fila>()
        var dia: LocalDate? = null
        for (ev in eventos) {
            val d = diaCalendario(ev)
            if (d != dia) {
                nuevas.add(Cabecera(Huso.etiquetaDia(d)))
                dia = d
            }
            nuevas.add(Item(ev))
        }
        filas = nuevas
    }

    override fun getCount(): Int = filas.size
    override fun getViewTypeCount(): Int = 2   // cabecera + evento
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = false
    override fun getLoadingView(): RemoteViews? = null

    override fun getViewAt(position: Int): RemoteViews =
        when (val fila = filas[position]) {
            is Cabecera -> RemoteViews(ctx.packageName, R.layout.widget_dia).apply {
                setTextViewText(R.id.dia_texto, fila.texto)
            }
            is Item -> construirItem(fila.evento)
        }

    /** Una píldora con TODO el fondo del color del evento (colores de Google). */
    private fun construirItem(ev: Evento): RemoteViews {
        val rv = RemoteViews(ctx.packageName, R.layout.widget_item)
        rv.setInt(R.id.fondo, "setColorFilter", ev.color)

        // Texto que contraste: oscuro si el fondo es claro, blanco si es oscuro
        val texto = colorTexto(ev.color)
        val tenue = conAlpha(texto, 0xCC)
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

        rv.setOnClickFillInIntent(R.id.item_root, Intent())
        return rv
    }

    /** Día de calendario del evento. Los de todo el día se guardan en UTC. */
    private fun diaCalendario(ev: Evento): LocalDate =
        if (ev.todoElDia)
            Instant.ofEpochMilli(ev.inicioMs).atZone(ZoneOffset.UTC).toLocalDate()
        else
            Instant.ofEpochMilli(ev.inicioMs).atZone(Huso.PRIMARIA).toLocalDate()

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
