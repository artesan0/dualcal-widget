package com.dualcal.widget

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

/**
 * Pantalla de ayuda: concede el permiso de calendario y explica cómo añadir
 * el widget a la pantalla de inicio. Toda la función real está en el widget.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var estado: TextView
    private lateinit var botonPermiso: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        estado = findViewById(R.id.estado)
        botonPermiso = findViewById(R.id.boton_permiso)

        botonPermiso.setOnClickListener {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.READ_CALENDAR), 1
            )
        }
        findViewById<Button>(R.id.boton_abrir).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AgendaWidgetProvider.URL_DUALCAL)))
        }
    }

    override fun onResume() {
        super.onResume()
        actualizarEstado()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        actualizarEstado()
        AgendaWidgetProvider.refrescarTodos(this) // que el widget se rellene ya
    }

    private fun actualizarEstado() {
        if (CalendarRepositorio.hayPermiso(this)) {
            estado.text = getString(R.string.estado_ok)
            botonPermiso.visibility = Button.GONE
        } else {
            estado.text = getString(R.string.estado_falta_permiso)
            botonPermiso.visibility = Button.VISIBLE
        }
    }
}
