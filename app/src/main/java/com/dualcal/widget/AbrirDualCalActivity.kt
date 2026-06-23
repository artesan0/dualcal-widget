package com.dualcal.widget

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle

/**
 * Actividad puente del widget: al pulsar un evento, intenta abrir DualCal
 * como APP INSTALADA (PWA en pantalla completa, sin barra de navegador).
 * Si no está instalada, cae al navegador.
 *
 * El truco es FLAG_ACTIVITY_REQUIRE_NON_BROWSER (Android 11+): obliga a
 * resolver la URL con una app que NO sea un navegador (la PWA instalada);
 * si no hay ninguna, lanza ActivityNotFoundException y usamos el navegador.
 */
class AbrirDualCalActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = Uri.parse(AgendaWidgetProvider.URL_DUALCAL)
        try {
            val comoApp = Intent(Intent.ACTION_VIEW, url).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    addFlags(Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER)
                }
            }
            startActivity(comoApp)
        } catch (e: ActivityNotFoundException) {
            // No hay PWA instalada: abrir en el navegador como hasta ahora
            startActivity(
                Intent(Intent.ACTION_VIEW, url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        finish()
    }
}
