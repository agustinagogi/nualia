package com.example.nualia3.ui.notas

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nualia3.R
import com.example.nualia3.databinding.FragmentListaNotasBinding
import com.example.nualia3.datos.Entrada
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
/**
 * Fragmento que muestra la lista de notas del usuario actual.
 *
 * Al iniciar, carga todas las entradas del tipo "nota" desde Firestore y las muestra en un RecyclerView.
 * También permite navegar a la pantalla de detalle de una nota o añadir una nueva.
 *
 * Se muestra un mensaje si no hay notas, y se controla la visibilidad de la UI con una barra de progreso.
 */
class FragmentListarNota : Fragment() {

    private lateinit var binding: FragmentListaNotasBinding
    private lateinit var adapter: NotasAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflamos el layout y accedemos al binding
        binding = FragmentListaNotasBinding.inflate(inflater, container, false)

        // Creamos el adaptador vacío por defecto
        adapter = NotasAdapter(listOf()) { nota ->
            // Cuando pulsamos una nota, navegamos al detalle con su ID
            val bundle = Bundle().apply { putString("entradaId", nota.id) }
            findNavController().navigate(R.id.fragmentDetalle, bundle)
        }

        // Ponemos el RecyclerView en modo vertical
        binding.recyclerNotas.layoutManager = LinearLayoutManager(context)
        binding.recyclerNotas.adapter = adapter

        // Mostramos la barra de carga y ocultamos el contenido
        binding.cargarProgreso.visibility = View.VISIBLE
        binding.contenidoListaNotas.visibility = View.GONE

        // Llamamos a la función para cargar las notas desde Firestore
        cargarNotas()

        // Si pulsamos el botón flotante, vamos a la pantalla para añadir una nota nueva
        binding.fabAnadirNota.setOnClickListener {
            findNavController().navigate(R.id.anadirEntradaFragment)
        }

        return binding.root
    }

    /**
     * Carga todas las notas del usuario actual desde Firestore.
     * Ordenamos las notas por fecha de actualización y las mostramos en el RecyclerView.
     * Si no hay notas, mostramos un mensaje.
     */
    @SuppressLint("StringFormatInvalid")
    private fun cargarNotas() {
        // Obtenemos el ID del usuario
        val usuarioId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Accedemos a Firestore y filtramos solo las entradas de tipo "nota"
        FirebaseFirestore.getInstance()
            .collection("usuarios_datos")
            .document(usuarioId)
            .collection("entradas")
            .whereEqualTo("tipo", "nota")
            .get()
            .addOnSuccessListener { documents ->
                // Convertimos los documentos a objetos Entrada y los ordenamos por fecha
                val notas = documents.mapNotNull { it.toObject(Entrada::class.java) }
                    .sortedByDescending { it.actualizado }

                // Volvemos a crear el adaptador con la lista real de notas
                adapter = NotasAdapter(notas) { note ->
                    val bundle = Bundle().apply { putString("entradaId", note.id) }
                    findNavController().navigate(R.id.fragmentDetalle, bundle)
                }
                binding.recyclerNotas.adapter = adapter

                // Mostramos mensaje si está vacío
                binding.tvMensajeVacio.visibility = if (notas.isEmpty()) View.VISIBLE else View.GONE

                // Ocultamos el loader y mostramos el contenido
                binding.cargarProgreso.visibility = View.GONE
                binding.contenidoListaNotas.visibility = View.VISIBLE
            }
            .addOnFailureListener {
                // Si falla la carga, mostramos un mensaje de error
                Toast.makeText(context, getString(R.string.error_cargar_datos, it.message), Toast.LENGTH_SHORT).show()
                binding.cargarProgreso.visibility = View.GONE
                binding.contenidoListaNotas.visibility = View.VISIBLE
                binding.tvMensajeVacio.visibility = View.VISIBLE
            }
    }

}
