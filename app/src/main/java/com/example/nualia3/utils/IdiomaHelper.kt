package com.example.nualia3.utils

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit
import java.util.Locale

/**
 * Utilidad para gestionar el idioma de la aplicación.
 *
 * Permite guardar el idioma elegido por el usuario, aplicarlo al contexto
 * y recuperar el idioma actual guardado.
 */
object IdiomaHelper {

    /**
     * Establecemos el idioma en el contexto de forma segura para todas las versiones.
     *
     * @param context Contexto actual.
     * @param idioma Código del idioma (por ejemplo "es", "en").
     * @return Contexto con la nueva configuración de idioma.
     */
    fun establecerIdioma(context: Context, idioma: String): Context {
        val locale = Locale(idioma)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        // Siempre devolvemos un nuevo contexto actualizado
        return context.createConfigurationContext(config)
    }

    /**
     * Recuperamos el idioma guardado en SharedPreferences.
     *
     * @param context Contexto para acceder a las preferencias.
     * @return Código de idioma, por defecto "es".
     */
    fun getIdiomaGuardado(context: Context): String {
        val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)
        return prefs.getString("idioma", "es") ?: "es"
    }

    /**
     * Guardamos el idioma elegido por el usuario en SharedPreferences.
     *
     * @param context Contexto para acceder a las preferencias.
     * @param idioma Código del idioma (por ejemplo "es", "en").
     */
    fun guardarIdioma(context: Context, idioma: String) {
        val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)
        prefs.edit {
            putString("idioma", idioma)
        }
    }
}
