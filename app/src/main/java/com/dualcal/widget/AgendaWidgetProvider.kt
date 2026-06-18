package com.dualcal.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews

/** Proveedor del widget de agenda en doble huso. */
class AgendaWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) actualizarWidget(ctx, mgr, id)
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == ACTION_REFRESH) refrescarTodos(ctx)
    }

    private fun actualizarWidget(ctx: Context, mgr: AppWidgetManager, id: Int) {
        val rv = RemoteViews(ctx.packageName, R.layout.widget_agenda)

        // La cabecera superior es fija ("DualCal"); el día de cada bloque lo
        // marcan ahora los separadores dentro de la propia lista.

        // Lista de eventos alimentada por el servicio
        val servicio = Intent(ctx, AgendaRemoteViewsService::class.java)
        rv.setRemoteAdapter(R.id.lista, servicio)
        rv.setEmptyView(R.id.lista, R.id.vacio)

        // Tap en un evento → abrir la PWA DualCal
        val abrir = Intent(Intent.ACTION_VIEW, Uri.parse(URL_DUALCAL))
        val piAbrir = PendingIntent.getActivity(
            ctx, 0, abrir,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        rv.setPendingIntentTemplate(R.id.lista, piAbrir)
        rv.setOnClickPendingIntent(R.id.cabecera, piAbrir)

        // Botón de refresco manual
        val refrescar = Intent(ctx, AgendaWidgetProvider::class.java).setAction(ACTION_REFRESH)
        val piRefrescar = PendingIntent.getBroadcast(
            ctx, 0, refrescar,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        rv.setOnClickPendingIntent(R.id.boton_refrescar, piRefrescar)

        mgr.updateAppWidget(id, rv)
        mgr.notifyAppWidgetViewDataChanged(id, R.id.lista)
    }

    companion object {
        const val ACTION_REFRESH = "com.dualcal.widget.ACTION_REFRESH"
        const val URL_DUALCAL = "https://artesan0.github.io/dualcal/"

        /** Refresca cabecera y datos de todos los widgets colocados. */
        fun refrescarTodos(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, AgendaWidgetProvider::class.java))
            if (ids.isEmpty()) return
            AgendaWidgetProvider().onUpdate(ctx, mgr, ids)
        }
    }
}
