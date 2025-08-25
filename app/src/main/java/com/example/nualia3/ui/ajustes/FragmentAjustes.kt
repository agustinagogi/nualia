package com.example.nualia3.ui.ajustes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.nualia3.utils.IdiomaHelper
import com.example.nualia3.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Fragmento de ajustes del usuario
 * Permite:
 * - Cambiar el nombre de usaurio
 * - Cambiar el idioma de la app (se guarda localmente y se reinicia la actividad)
 */
class FragmentAjustes : Fragment() {

    //Infla el layout del fragmento de ajustes.
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_ajustes, container, false)
    }

    /**
     * Configuramos los botones después de que la vista ha sido creada.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Referencias a los botones en el layout
        val btnCambiarNombre = view.findViewById<Button>(R.id.btnCambiarNombre)
        val btnCambiarIdioma = view.findViewById<Button>(R.id.btnCambiarIdioma)
        // Abrimos un cuadro de diálogo para cambiar el nombe de usuario
        btnCambiarNombre.setOnClickListener {
            mostrarDialogoCambiarNombre()
        }
        // Mostramos un cuadro de selección de idioma
        btnCambiarIdioma.setOnClickListener {
            val idiomas = arrayOf("Español", "English")  // Idiomas disponibles
            android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.seleccionar_idioma_titulo))  // Título del diálogo
                .setItems(idiomas) { _, which ->  // Acción al seleccionar idioma
                    val nuevoIdioma = when (which) {
                        0 -> "es" // Español
                        1 -> "en" // Inglés
                        else -> "es" // Por defecto
                    }

                    // Guardamos el idioma seleccionado en preferencias
                    IdiomaHelper.guardarIdioma(requireContext(), nuevoIdioma)

                    // Reiniciamos la actividad para aplicar el nuevo idioma
                    val intent = requireActivity().intent
                    requireActivity().finish()
                    startActivity(intent)
                }
                .show()
        }
    }

    private fun mostrarDialogoCambiarNombre() {
        val campoNombreNuevo = EditText(requireContext())
        campoNombreNuevo.hint = getString(R.string.hint_nombre_nuevo) // opcional

        android.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_nuevo_nombre))
            .setView(campoNombreNuevo)

            .setPositiveButton(getString(R.string.dialog_guardar)) { _, _ ->
                val nuevoNombre = campoNombreNuevo.text.toString().trim()

                // Validamos que el nombre no esté vacío
                if (nuevoNombre.isEmpty()) {
                    Toast.makeText(context, getString(R.string.msg_nombre_vacio), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Verificamos que el usuario esté autenticado
                val user = FirebaseAuth.getInstance().currentUser
                if (user == null) {
                    Toast.makeText(context, getString(R.string.msg_usuario_no_autenticado), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val uid = user.uid

                // Actualizamos el nombre en Firestore
                FirebaseFirestore.getInstance()
                    .collection("usuarios_datos")
                    .document(uid)
                    .update("nombre", nuevoNombre)
                    .addOnSuccessListener {
                        Toast.makeText(context, getString(R.string.nombre_actualizado), Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            context,
                            "${getString(R.string.error_actualizar_nombre)}: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }

            .setNegativeButton(getString(R.string.dialog_cancelar), null)
            .show()
    }



}