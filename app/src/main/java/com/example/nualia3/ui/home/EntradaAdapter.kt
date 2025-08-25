package com.example.nualia3.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nualia3.R
import com.example.nualia3.datos.Entrada
import com.example.nualia3.utils.FechaUtils
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Adaptador para mostrar una lista de entradas en un RecyclerView
 *
 * Es para tareas, eventos, notas y diarios, adapta su visualización según el tipo
 * Permite detectar clics sobre las entradas y actualiza automáticamente el estado de una tarea (campo "hecho")
 *
 * Usa DiffUtil para mejorar el rendimiento al actualizar la lista
 *
 * @param onItemClick Función que se ejecutará cuando el usuario haga click en una entrada
 */
class EntradaAdapter(private val onItemClick: (Entrada) -> Unit):
    ListAdapter<Entrada, EntradaAdapter.EntradaViewHolder>(EntradaViewHolder.DiffCallback()){

    //Inflamos la vista del item y creamos el ViewHolder

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int
    ): EntradaAdapter.EntradaViewHolder {
        val vista = LayoutInflater.from(parent.context).inflate(R.layout.item_entrada, parent, false)
        return EntradaViewHolder(vista)
    }

    // Asociamos los datos de una entrada con la vista correspondiente
    override fun onBindViewHolder(holder: EntradaViewHolder, position: Int){
        val entrada = getItem(position)
        holder.bind(entrada)
        holder.itemView.setOnClickListener{
            onItemClick(entrada)
        }
    }

    /**
     * ViewHolder que representará una entrada en la interfaz
     * Configura el icono, el título, el subtítulo y el checkbox si es una tarea
     */
    class EntradaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        /**
         * Muestra los datos de una entrada según su tipo
         * Actualiza visualmente el icono, texto y checkbox
         *
         * @param entrada Entrada a representar
         */

        fun bind(entrada: Entrada){
            val iconoView = itemView.findViewById<ImageView>(R.id.tipoIcono)
            val tvTitulo = itemView.findViewById<TextView>(R.id.tvTitulo)
            val tvSubtitulo = itemView.findViewById<TextView>(R.id.tvSubtitulo)
            val checkHecho = itemView.findViewById<CheckBox>(R.id.checkHecho)

            val contexto = itemView.context
            val fechaFormateada = FechaUtils.formatearFechaHora(contexto, entrada.fecha, entrada.hora )

            when(entrada.tipo){
                "tarea" ->{
                    // Solo las tareas tienen checkbox
                    tvTitulo.text = entrada.titulo
                    tvSubtitulo.text = fechaFormateada
                    iconoView.visibility = View.GONE
                    checkHecho.visibility  = View.VISIBLE
                    checkHecho.isChecked = entrada.hecho

                    // Actualizamos Firestore si el checkbox cambia
                    checkHecho.setOnClickListener {
                        val nuevoHecho = checkHecho.isChecked
                        FirebaseFirestore.getInstance()
                            // Dentro de la colección usuarios_datos
                            .collection("usuarios_datos")
                            // En el documento entrada.usuarioId
                            .document(entrada.usuarioId)
                            // En la colección entrada
                            .collection("entradas")
                            // En el documento entrada.id
                            .document(entrada.id)
                            // Actualizamos el campo hecho
                            .update("hecho", nuevoHecho)
                    }
                }

                "evento" -> {
                    tvTitulo.text = entrada.titulo
                    tvSubtitulo.text = fechaFormateada
                    iconoView.visibility = View.VISIBLE
                    iconoView.setImageResource(R.drawable.ic_calendario)
                    iconoView.setBackgroundResource(R.drawable.bg_icono_evento)
                    checkHecho.visibility = View.GONE
                }

                "nota" -> {
                    tvTitulo.text = entrada.titulo
                    tvSubtitulo.text = fechaFormateada
                    iconoView.visibility = View.VISIBLE
                    iconoView.setImageResource(R.drawable.ic_nota)
                    iconoView.setBackgroundResource(R.drawable.bg_icono_notas)
                    checkHecho.visibility = View.GONE
                }

                "diario" -> {
                    tvTitulo.text = entrada.titulo
                    tvSubtitulo.text = fechaFormateada
                    iconoView.visibility = View.VISIBLE
                    iconoView.setImageResource(getEmocionDrawable(entrada.emocion))
                    iconoView.setBackgroundResource(R.drawable.bg_icono_diario)
                    checkHecho.visibility = View.GONE
                }

                else -> {
                    tvTitulo.text = entrada.titulo
                    tvSubtitulo.text = entrada.hora
                    iconoView.visibility = View.VISIBLE
                    iconoView.setImageResource(R.drawable.ic_libro)
                    iconoView.setBackgroundResource(R.drawable.bg_icono_diario)
                    checkHecho.visibility = View.GONE
                }
            }
        }

        /**
         * Da los iconos de emoción a las entradas de tipo diario
         *
         * @param emocion Nombre de la emoción
         * @return ID del recurso drawable correspondiente
         */

        private fun getEmocionDrawable(emocion: String): Int{
            return when (emocion){
                "feliz" -> R.drawable.emocion_feliz
                "triste" -> R.drawable.emocion_triste
                "enfadado" -> R.drawable.emocion_enfadado
                "enfermo" -> R.drawable.emocion_enfermo
                else -> R.drawable.emocion_feliz
            }
        }

        /**
         * Utiliza DiffUtil para optimizar las actualizaciones del RecyclerView
         */

        class DiffCallback : DiffUtil.ItemCallback<Entrada>() {
            override fun areItemsTheSame(oldItem: Entrada, newItem: Entrada): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Entrada, newItem: Entrada): Boolean =
                oldItem == newItem
        }
    }

}