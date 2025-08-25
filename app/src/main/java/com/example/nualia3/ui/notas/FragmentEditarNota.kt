package com.example.nualia3.ui.notas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nualia3.R
import com.example.nualia3.databinding.FragmentEditarNotaBinding
import com.example.nualia3.datos.Entrada
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
/**
 * Fragmento que permite editar una nota existente del usuario.
 *
 * Cargamos los datos desde Firestore usando el ID recibido por argumentos,
 * los mostramos en pantalla y permitimos modificarlos y guardarlos.
 * También se puede eliminar la nota desde este fragmento.
 *
 * Requiere que el usuario esté autenticado (se obtiene el UID con FirebaseAuth).
 */
class FragmentEditarNota : Fragment() {

    private lateinit var binding: FragmentEditarNotaBinding
    private lateinit var usuarioId: String
    private var notaId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflamos el layout y lo asignamos al binding
        binding = FragmentEditarNotaBinding.inflate(inflater, container, false)

        // Obtenemos el UID del usuario autenticado
        usuarioId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Recuperamos el ID de la nota que viene por argumentos
        notaId = arguments?.getString("entradaId")

        // Ocultamos el contenido principal y mostramos el indicador de carga
        binding.contentLayout.visibility = View.GONE
        binding.cargarProgreso.visibility = View.VISIBLE

        // Si no recibimos un ID válido, mostramos un mensaje y salimos
        if (notaId.isNullOrEmpty()) {
            Toast.makeText(context, getString(R.string.msg_id_entrada_invalido), Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return binding.root
        }

        // Cargamos los detalles de la nota desde Firestore
        cargarDetallesNota()

        // Guardamos los cambios si se pulsa el botón
        binding.btnGuardar.setOnClickListener { guardarCambios() }

        // Eliminamos la nota si se pulsa el botón
        binding.btnEliminar.setOnClickListener {
            notaId?.let { id ->
                FirebaseFirestore.getInstance()
                    .collection("usuarios_datos").document(usuarioId)
                    .collection("entradas").document(id)
                    .delete()
                    .addOnSuccessListener {
                        if (!isAdded) return@addOnSuccessListener
                        Toast.makeText(context, getString(R.string.msg_entrada_eliminada), Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                    .addOnFailureListener {
                        if (!isAdded) return@addOnFailureListener
                        Toast.makeText(context, getString(R.string.msg_error_eliminar, it.message), Toast.LENGTH_SHORT).show()
                    }
            }
        }

        return binding.root
    }

    /**
     * Carga los datos de la nota desde Firestore y los coloca en el formulario.
     */
    private fun cargarDetallesNota() {
        notaId?.let { id ->
            FirebaseFirestore.getInstance()
                .collection("usuarios_datos").document(usuarioId)
                .collection("entradas").document(id)
                .get()
                .addOnSuccessListener { doc ->
                    if (!isAdded) return@addOnSuccessListener

                    val entrada = doc.toObject(Entrada::class.java)
                    if (entrada != null) {
                        // Ponemos el texto en los campos de título y descripción
                        binding.etTitulo.setText(entrada.titulo)
                        binding.etDescripcion.setText(entrada.descripcion)

                        // Mostramos el contenido y ocultamos la barra de progreso
                        binding.contentLayout.visibility = View.VISIBLE
                        binding.cargarProgreso.visibility = View.GONE
                    } else {
                        Toast.makeText(context, getString(R.string.msg_entrada_no_encontrada), Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                }
                .addOnFailureListener {
                    if (!isAdded) return@addOnFailureListener
                    Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
        }
    }

    /**
     * Guarda los cambios realizados en la nota actualizando Firestore.
     */
    private fun guardarCambios() {
        val nuevoTitulo = binding.etTitulo.text.toString().trim()
        val nuevaDescripcion = binding.etDescripcion.text.toString().trim()

        // Verificamos que el título no esté vacío
        if (nuevoTitulo.isEmpty()) {
            Toast.makeText(context, getString(R.string.msg_titulo_vacio), Toast.LENGTH_SHORT).show()
            return
        }

        notaId?.let { id ->
            // Creamos un mapa con los datos actualizados
            val actualizaciones = mapOf(
                "titulo" to nuevoTitulo,
                "descripcion" to nuevaDescripcion,
                "actualizado" to System.currentTimeMillis()
            )

            // Subimos los cambios a Firestore
            FirebaseFirestore.getInstance()
                .collection("usuarios_datos").document(usuarioId)
                .collection("entradas").document(id)
                .update(actualizaciones)
                .addOnSuccessListener {
                    if (!isAdded) return@addOnSuccessListener
                    Toast.makeText(context, getString(R.string.msg_entrada_actualizada), Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                .addOnFailureListener {
                    if (!isAdded) return@addOnFailureListener
                    Toast.makeText(context, getString(R.string.msg_error_actualizar, it.message), Toast.LENGTH_SHORT).show()
                }
        }
    }
}
