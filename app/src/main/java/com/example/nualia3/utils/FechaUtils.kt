package com.example.nualia3.utils

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Objeto utilitario para formatear fechas y horas de manera amigable al usuario,
 * adaptándonos al idioma guardado.
 */
object FechaUtils {

    /**
     * Formatea una fecha (yyyy-MM-dd) y hora (HH:mm) en un formato más legible.
     * Usamos el idioma configurado en la app para mostrar el mes en el idioma correcto.
     *
     * @param context Contexto para obtener el idioma actual del usuario.
     * @param fecha Fecha en formato "yyyy-MM-dd".
     * @param hora Hora en formato "HH:mm".
     * @return Cadena como "4 de junio • 18:30" o solo la fecha si la hora está vacía.
     */
    fun formatearFechaHora(context: Context?, fecha: String, hora: String): String {
        return try {
            // Obtenemos el idioma guardado o usamos "es" por defecto
            val idioma = context?.let { IdiomaHelper.getIdiomaGuardado(it) } ?: "es"
            val locale = Locale(idioma)

            // Parseamos la fecha original
            val parser = SimpleDateFormat("yyyy-MM-dd", locale)
            val parsedDate = parser.parse(fecha)

            // Le damos formato largo al estilo "4 de junio"
            val formatter = SimpleDateFormat("d 'de' MMMM", locale)
            val formattedDate = parsedDate?.let { formatter.format(it) } ?: fecha

            // Devolvemos fecha con o sin hora según corresponda
            if (hora.isNotEmpty() && hora != "00:00") {
                "$formattedDate • $hora"
            } else {
                formattedDate
            }
        } catch (e: Exception) {
            // Si falla el parseo, devolvemos lo que tengamos
            if (hora.isNotEmpty() && hora != "00:00") "$fecha • $hora" else fecha
        }
    }

    /**
     * Formatea fecha y hora completa a una cadena legible, incluyendo el día de la semana.
     * Ejemplo: "martes 4 de junio, 18:30".
     *
     * @param context Contexto para extraer el idioma.
     * @param fechaStr Fecha en formato "yyyy-MM-dd".
     * @param hora Hora en formato "HH:mm".
     * @return Fecha completa formateada como texto.
     */
    fun formatearFechaHoraCompleta(context: Context?, fechaStr: String, hora: String): String {
        return try {
            // Obtenemos el idioma preferido o usamos español
            val idioma = context?.let { IdiomaHelper.getIdiomaGuardado(it) } ?: "es"
            val locale = Locale(idioma)

            // Parseamos fecha y hora juntas
            val parser = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val fecha = parser.parse("$fechaStr $hora")

            // Le damos formato como "martes 4 de junio, 18:30"
            val formateador = SimpleDateFormat("EEEE d 'de' MMMM, HH:mm", locale)
            formateador.format(fecha!!).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(locale) else it.toString()
            }
        } catch (e: Exception) {
            // Si algo sale mal, devolvemos el string original
            "$fechaStr $hora"
        }
    }
}
