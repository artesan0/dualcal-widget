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

        // Barra lateral con el color real del calendario
        rv.setInt(R.id.barra, "setBackgroundColor", ev.color)
        rv.setTextViewText(R.id.titulo, ev.titulo)

        if (ev.ubicacion != null) {
            rv.setTextViewText(R.id.ubicacion, ev.ubicacion)
            rv.setViewVisibility(R.id.ubicacion, View.VISIBLE)
        } else {
            rv.setViewVisibility(R.id.ubicacion, View.GONE)
        }

        // Doble huso: Asunción arriba, Madrid (ES) debajo
        if (ev.todoElDia) {
            rv.setTextViewText(R.id.hora_pri, "Todo")
            rv.setTextViewText(R.id.hora_sec, "el día")
        } else {
            rv.setTextViewText(R.id.hora_pri, Huso.horaPrimaria(ev.inicioMs))
            rv.setTextViewText(R.id.hora_sec, Huso.horaSecundaria(ev.inicioMs))
        }

        // Propaga el clic al PendingIntent plantilla (abre DualCal)
        rv.setOnClickFillInIntent(R.id.item_root, Intent())
        return rv
    }
}
