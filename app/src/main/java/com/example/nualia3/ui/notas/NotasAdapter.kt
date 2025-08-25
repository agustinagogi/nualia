package com.example.nualia3.ui.notas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.nualia3.R
import com.example.nualia3.databinding.ItemNotaBinding
import com.example.nualia3.datos.Entrada
import com.example.nualia3.utils.FechaUtils

/**
 * Adaptador personalizado para mostrar una lista de notas en un RecyclerView.
 *
 * Cada ítem representa una entrada del tipo "nota", mostrando su título, fecha y un icono decorativo.
 * Se aplica formateo de fecha según el idioma del usuario. Al pulsar sobre un ítem, se llama al callback proporcionado.
 *
 * @param listaNotas Lista de objetos Entrada con tipo "nota".
 * @param onItemClick Acción a ejecutar cuando se pulsa una nota.
 */
class NotasAdapter(
    private val listaNotas: List<Entrada>,
    private val onItemClick: (Entrada) -> Unit
) : RecyclerView.Adapter<NotasAdapter.ViewHolderNotas>() {

    /**
     * ViewHolder que contiene el binding de cada ítem de nota.
     */
    class ViewHolderNotas(val binding: ItemNotaBinding) : RecyclerView.ViewHolder(binding.root)

    /**
     * Inflamos el layout de la nota y creamos el ViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderNotas {
        // Ponemos el layout de la nota
        val binding = ItemNotaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolderNotas(binding)
    }

    /**
     * Asignamos los datos a cada elemento de la lista.
     */
    override fun onBindViewHolder(holder: ViewHolderNotas, position: Int) {
        // Obtenemos la nota en la posición actual
        val nota = listaNotas[position]

        // Ponemos el título
        holder.binding.txtTitulo.text = nota.titulo

        // Formateamos y mostramos la fecha y hora
        val fechaHoraFormateada = FechaUtils.formatearFechaHora(holder.itemView.context, nota.fecha, nota.hora)
        holder.binding.txtFecha.text = fechaHoraFormateada

        // Ponemos el icono y fondo específico de notas
        holder.binding.iconoNota.setImageResource(R.drawable.ic_nota)
        holder.binding.iconoNota.setBackgroundResource(R.drawable.bg_icono_notas)

        // Si se pulsa una nota, llamamos al callback
        holder.itemView.setOnClickListener {
            onItemClick(nota)
        }
    }

    /**
     * Devuelve la cantidad total de notas en la lista.
     */
    override fun getItemCount(): Int = listaNotas.size
}
