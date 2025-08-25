package com.example.nualia3.ui.diario

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nualia3.R
import com.example.nualia3.datos.Entrada
import com.example.nualia3.utils.FechaUtils

// Creamos un adaptador para mostrar entradas tipo diario en un RecyclerView.
// Usamos ListAdapter con DiffUtil para optimizar la actualización de elementos.
class DiarioAdapter(private val onItemClick: (Entrada) -> Unit) :
    ListAdapter<Entrada, DiarioAdapter.JournalViewHolder>(JournalViewHolder.ComparadorEntradas()) {

    // Inflamos el layout de cada ítem y creamos un ViewHolder para manejarlo
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_entrada, parent, false)
        return JournalViewHolder(view)
    }

    // Asociamos los datos de la entrada al ViewHolder y configuramos el click
    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {
        val entrada = getItem(position)
        holder.bind(entrada)
        holder.itemView.setOnClickListener {
            onItemClick(entrada)
        }
    }

    // ViewHolder interno que se encarga de mostrar los datos de una entrada en la vista
    class JournalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(entrada: Entrada) {
            // Obtenemos referencias a las vistas del ítem
            val iconView = itemView.findViewById<ImageView>(R.id.tipoIcono)
            val tvTitulo = itemView.findViewById<TextView>(R.id.tvTitulo)
            val tvSubtitulo = itemView.findViewById<TextView>(R.id.tvSubtitulo)

            // Mostramos el título de la entrada
            tvTitulo.text = entrada.titulo

            // Formateamos la fecha y hora y la mostramos en el subtítulo
            tvSubtitulo.text = FechaUtils.formatearFechaHora(itemView.context, entrada.fecha, entrada.hora)

            // Hacemos visible el ícono de emoción y le asignamos la imagen correspondiente
            iconView.visibility = View.VISIBLE
            iconView.setImageResource(getEmocionRecurso(entrada.emocion))

            // Establecemos el fondo del icono con estilo diario
            iconView.setBackgroundResource(R.drawable.bg_icono_diario)
        }

        // Elegimos qué imagen usar según la emoción de la entrada
        private fun getEmocionRecurso(emocion: String): Int {
            return when (emocion) {
                "feliz" -> R.drawable.emocion_feliz
                "triste" -> R.drawable.emocion_triste
                "enfadado" -> R.drawable.emocion_enfadado
                "enfermo" -> R.drawable.emocion_enfermo
                else -> R.drawable.emocion_feliz  // Mostramos feliz como valor por defecto
            }
        }

        // Usamos DiffUtil para actualizar solo los ítems que realmente cambiaron
        class ComparadorEntradas : DiffUtil.ItemCallback<Entrada>() {
            override fun areItemsTheSame(oldItem: Entrada, newItem: Entrada) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Entrada, newItem: Entrada) = oldItem == newItem
        }
    }
}
