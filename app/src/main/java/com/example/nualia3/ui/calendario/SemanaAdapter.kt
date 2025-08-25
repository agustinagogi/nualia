package com.example.nualia3.ui.calendario

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.nualia3.R
import java.time.DayOfWeek
import java.time.LocalDate


/**
 * Adaptador para el RecyclerView que muestra los días de una semana (lunes a domingo)
 * Permite al usuario seleccionar un día, resaltar la selección y notificar al ViewModel
 *
 * @param diaSemana Lista de fechas correspondientes a los días de la semana actual
 * @param onDateSelected Función lambda que se ejecuta cuando el usuario selecciona una fecha
 */
class SemanaAdapter(
    private var diaSemana: List<LocalDate>,
    private val onDateSelected: (LocalDate) -> Unit
) : RecyclerView.Adapter<SemanaAdapter.SemanaViewHolder>() {

    // Fecha actualmente seleccionada (inicialmente el primer día de la semana)
    private var fechaSeleccionada: LocalDate = diaSemana[0]

    /**
     * ViewHolder que contiene las vistas de cada ítem de día en el calendario semanal.
     */
    inner class SemanaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDia: TextView = view.findViewById(R.id.tvDia)       // LUN, MAR, etc.
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)   // 1, 2, 3...
    }

    /**
     * Infla el layout de cada celda (día de la semana).
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SemanaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dia_semana, parent, false)
        return SemanaViewHolder(view)
    }

    /**
     * Asocia los datos del día a la vista correspondiente.
     */
    override fun onBindViewHolder(holder: SemanaViewHolder, position: Int) {
        val fecha = diaSemana[position]
        val context = holder.itemView.context

        // Establecemos el nombre corto del día (ej: LU, MA) y el número (ej: 5)
        holder.tvDia.text = getNombreCortoDia(fecha.dayOfWeek)
        holder.tvFecha.text = fecha.dayOfMonth.toString()

        // Determinamos si esta celda representa el día seleccionado
        val isSeleccionado = fecha == fechaSeleccionada

        // Colores personalizados para día seleccionado vs. no seleccionado
        val colorSeleccionado = ContextCompat.getColor(context, R.color.white)
        val colorPorDefecto = ContextCompat.getColor(context, R.color.texto)

        // Fondo: si está seleccionado, usa un fondo redondeado, si no, transparente
        holder.itemView.setBackgroundResource(
            if (isSeleccionado) R.drawable.bg_day_selected else android.R.color.transparent
        )

        // Cambiamos el color del texto del día y número según si está seleccionado
        holder.tvDia.setTextColor(if (isSeleccionado) colorSeleccionado else colorPorDefecto)
        holder.tvFecha.setTextColor(if (isSeleccionado) colorSeleccionado else colorPorDefecto)

        // Cuando el usuario pulsa un día
        holder.itemView.setOnClickListener {
            fechaSeleccionada = fecha                   // Marcamos esta fecha como seleccionada
            notifyDataSetChanged()                      // Forzamos la actualización visual
            onDateSelected(fecha)                       // Notificamos al fragmento (callback)
        }
    }

    /**
     * Devuelve cuántos días hay en la semana (normalmente 7).
     */
    override fun getItemCount(): Int = diaSemana.size

    /**
     * Permite actualizar la semana mostrada y cuál día está seleccionado.
     *
     * @param nuevasFechas Nueva lista de días (una semana).
     * @param newSelectedDate Fecha a marcar como seleccionada visualmente.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun actualizarSemana(nuevasFechas: List<LocalDate>, newSelectedDate: LocalDate) {
        this.diaSemana = nuevasFechas
        this.fechaSeleccionada = newSelectedDate
        notifyDataSetChanged()
    }

    /**
     * Devuelve un nombre abreviado personalizado del día de la semana.
     * Ej: Lunes -> "LU", Martes -> "MA", etc.
     */
    private fun getNombreCortoDia(diaSemana: DayOfWeek): String {
        return when (diaSemana) {
            DayOfWeek.MONDAY -> "LU"
            DayOfWeek.TUESDAY -> "MA"
            DayOfWeek.WEDNESDAY -> "MI"
            DayOfWeek.THURSDAY -> "JU"
            DayOfWeek.FRIDAY -> "VI"
            DayOfWeek.SATURDAY -> "SA"
            DayOfWeek.SUNDAY -> "DO"
        }
    }
}
