package com.example.nualia3.ui.diario

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.nualia3.R
import com.example.nualia3.databinding.FragmentEditarDiarioBinding
import com.example.nualia3.datos.Entrada
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
/**
 * Fragmento utilizado para editar una entrada de tipo "diario".
 *
 * Permite al usuario modificar:
 * - Título
 * - Descripción
 * - Emoción asociada
 * - Imagen opcional
 *
 * También permite eliminar la entrada.
 * La entrada se recupera de Firestore usando su ID y puede actualizarse o eliminarse.
 * Si se selecciona una nueva imagen, se sube a Firebase Storage.
 *
 */
class FragmentEditarDiario : Fragment() {

    /** ViewBinding para acceder a las vistas del layout */
    private lateinit var binding: FragmentEditarDiarioBinding

    /** ID del usuario autenticado */
    private lateinit var usuarioId: String

    /** ID de la entrada a editar */
    private var entradaId: String? = null

    /** Emoción seleccionada por el usuario */
    private var emocionSeleccionada: String = ""

    /** URI de la nueva imagen seleccionada (si se cambia) */
    private var uriImagenSeleccionada: Uri? = null

    /** Launcher para seleccionar imagen desde galería */
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    /**
     * Creamos la vista del fragmento de edición de diario.
     * Establecemos listeners para botones, cargamos los datos si existe una entrada previa
     * y configuramos los componentes visuales.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflamos el layout y accedemos al binding
        binding = FragmentEditarDiarioBinding.inflate(inflater, container, false)

        // Obtenemos el ID del usuario autenticado
        usuarioId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Recuperamos el ID de la entrada desde los argumentos del Bundle
        entradaId = arguments?.getString("entradaId")

        // Si el ID de entrada es inválido o nulo, mostramos error y volvemos atrás
        if (entradaId.isNullOrEmpty()) {
            Toast.makeText(context, getString(R.string.msg_id_entrada_invalido), Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return binding.root
        }

        // Cargamos los datos actuales de la entrada para editar
        cargarEntrada()

        // Establecemos el listener para guardar los cambios al pulsar el botón "Guardar"
        binding.btnGuardar.setOnClickListener { guardarCambios() }

        // Establecemos el listener para eliminar la entrada actual
        binding.btnEliminar.setOnClickListener {
            entradaId?.let { id ->
                FirebaseFirestore.getInstance()
                    .collection("usuarios_datos").document(usuarioId)
                    .collection("entradas").document(id)
                    .delete()
                    .addOnSuccessListener {
                        // Si la vista sigue activa, mostramos confirmación y volvemos atrás
                        if (!isAdded) return@addOnSuccessListener
                        Toast.makeText(
                            context,
                            getString(R.string.msg_entrada_eliminada),
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().navigateUp()
                    }
                    .addOnFailureListener {
                        // Si ocurre un error al eliminar, mostramos mensaje
                        if (!isAdded) return@addOnFailureListener
                        Toast.makeText(
                            context,
                            getString(R.string.msg_error_eliminar),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }

        // Registramos el resultado del selector de imágenes
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                // Guardamos la URI de la imagen seleccionada y la mostramos en pantalla
                uriImagenSeleccionada = data?.data
                binding.ivDiario.setImageURI(uriImagenSeleccionada)
            }
        }

        // Establecemos el comportamiento del botón para seleccionar una nueva imagen
        binding.btnSeleccionarImagen.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            imagePickerLauncher.launch(intent)
        }

        // Configuramos los botones de selección de emoción (feliz, triste, etc.)
        configurarSeleccionEmocion()

        // Devolvemos la raíz del layout inflado como vista principal del fragmento
        return binding.root
    }


    /**
     * Cargamos los datos de una entrada específica desde Firestore
     * y actualizamos los campos de la interfaz con su información.
     */
    private fun cargarEntrada() {
        // Verificamos que tengamos un ID de entrada válido
        entradaId?.let { id ->
            // Accedemos a la colección Firestore del usuario autenticado y buscamos la entrada
            FirebaseFirestore.getInstance()
                .collection("usuarios_datos").document(usuarioId)
                .collection("entradas").document(id)
                .get()
                .addOnSuccessListener { doc ->
                    // Si el fragmento ya no está asociado a la actividad, salimos
                    if (!isAdded) return@addOnSuccessListener

                    // Convertimos el documento obtenido en un objeto Entrada
                    val entrada = doc.toObject(Entrada::class.java)

                    // Si la entrada existe, mostramos sus datos en pantalla
                    if (entrada != null) {
                        // Rellenamos los campos de título y descripción
                        binding.etTitulo.setText(entrada.titulo)
                        binding.etDescripcion.setText(entrada.descripcion)

                        // Mostramos la fecha con la hora si está disponible
                        binding.tvFecha.setText(
                            if (entrada.hora.isNotEmpty() && entrada.hora != "00:00") {
                                "${entrada.fecha} • ${entrada.hora}"
                            } else {
                                entrada.fecha
                            }
                        )

                        // Guardamos la emoción original para que se mantenga seleccionada
                        emocionSeleccionada = entrada.emocion
                        resaltarEmocion(emocionSeleccionada)

                        // Si hay imagen asociada, la cargamos con Glide
                        if (!entrada.imagenUrl.isNullOrEmpty()) {
                            binding.ivDiario.visibility = View.VISIBLE
                            Glide.with(this)
                                .load(entrada.imagenUrl)
                                .placeholder(R.color.placeholderColor)
                                .into(binding.ivDiario)
                        } else {
                            // Si no hay imagen, ocultamos el ImageView
                            binding.ivDiario.visibility = View.GONE
                        }
                    } else {
                        // Si no se encontró la entrada, avisamos al usuario y regresamos
                        Toast.makeText(
                            context,
                            getString(R.string.msg_entrada_no_encontrada),
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().navigateUp()
                        return@addOnSuccessListener
                    }
                }
                .addOnFailureListener {
                    // Si ocurre un error al obtener la entrada, mostramos mensaje y navegamos atrás
                    if (!isAdded) return@addOnFailureListener
                    Toast.makeText(
                        context,
                        getString(R.string.msg_error_generico, it.message),
                        Toast.LENGTH_SHORT
                    ).show()
                    findNavController().navigateUp()
                }
        }
    }


    /**
     * Guardamos los cambios realizados en la entrada de tipo diario.
     * Si se ha seleccionado una nueva imagen, la subimos a Firebase Storage y luego
     * actualizamos su URL en Firestore. Si no, conservamos la imagen actual.
     */
    private fun guardarCambios() {
        // Obtenemos el nuevo título introducido por el usuario
        val nuevoTitulo = binding.etTitulo.text.toString().trim()

        // Validamos que el campo de título no esté vacío
        if (nuevoTitulo.isEmpty()) {
            Toast.makeText(context, getString(R.string.msg_titulo_vacio), Toast.LENGTH_SHORT).show()
            return  // Cancelamos la operación si falta el título
        }

        // Comprobamos si el usuario ha seleccionado una nueva imagen
        if (uriImagenSeleccionada != null) {
            // Creamos una referencia única en Firebase Storage para la imagen
            val storageRef = FirebaseStorage.getInstance().reference
                .child("diarios/$usuarioId/${System.currentTimeMillis()}.jpg")

            // Subimos la imagen seleccionada a esa ruta
            storageRef.putFile(uriImagenSeleccionada!!)
                .addOnSuccessListener {
                    // Cuando la subida se completa, recuperamos la URL de descarga
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        // Llamamos al método para guardar la entrada con la URL de la imagen subida
                        guardarEntradaConImagen(uri.toString())
                    }
                }
                .addOnFailureListener {
                    // Si algo falla al subir la imagen, mostramos mensaje de error
                    if (!isAdded) return@addOnFailureListener
                    Toast.makeText(context, getString(R.string.msg_error_subir_imagen), Toast.LENGTH_SHORT).show()
                }
        } else {
            // Si no se ha seleccionado nueva imagen, recuperamos la entrada actual desde Firestore
            FirebaseFirestore.getInstance()
                .collection("usuarios_datos").document(usuarioId)
                .collection("entradas").document(entradaId!!)
                .get()
                .addOnSuccessListener { doc ->
                    // Extraemos la URL de imagen actual (si existe)
                    val entrada = doc.toObject(Entrada::class.java)
                    val currentimagenUrl = entrada?.imagenUrl ?: ""

                    // Guardamos los cambios manteniendo la imagen actual
                    guardarEntradaConImagen(currentimagenUrl)
                }
                .addOnFailureListener {
                    // Mostramos error si falla la lectura de la entrada
                    if (!isAdded) return@addOnFailureListener
                    Toast.makeText(context, getString(R.string.msg_error_generico, it.message), Toast.LENGTH_SHORT).show()
                }
        }
    }


    /**
     * Configura los listeners de clic para los botones de emoción.
     */
    private fun configurarSeleccionEmocion() {
        val emojis = mapOf(
            binding.emojiFeliz to "feliz",
            binding.emojiTriste to "triste",
            binding.emojiEnfadado to "enfadado",
            binding.emojiEnfermo to "enfermo"
        )

        emojis.forEach { (emoji, emocion) ->
            emoji.setOnClickListener {
                emocionSeleccionada = emocion
                resaltarEmocion(emocion)
            }
        }
    }
    /**
     * Guardamos los cambios de una entrada de tipo diario incluyendo la URL de la imagen.
     *
     * @param imagenUrl URL de la imagen que se ha subido (o la original si no se ha cambiado)
     */
    private fun guardarEntradaConImagen(imagenUrl: String) {
        // Verificamos que entradaId no sea nulo y continuamos con la actualización
        entradaId?.let { id ->

            // Creamos un mapa con los nuevos valores que queremos guardar en Firestore
            val updates = mapOf(
                "titulo" to binding.etTitulo.text.toString().trim(),  // Tomamos el título del campo de texto
                "descripcion" to binding.etDescripcion.text.toString().trim(),  // Tomamos la descripción
                "emocion" to emocionSeleccionada,  // Guardamos la emoción seleccionada
                "imagenUrl" to imagenUrl,  // Asignamos la URL de la imagen (puede ser nueva o la actual)
                "actualizado" to System.currentTimeMillis()  // Registramos la hora de última modificación
            )

            // Accedemos a Firestore y actualizamos la entrada correspondiente al usuario y al ID
            FirebaseFirestore.getInstance()
                .collection("usuarios_datos").document(usuarioId)
                .collection("entradas").document(id)
                .update(updates)
                .addOnSuccessListener {
                    // Si el fragmento ya no está activo, evitamos continuar
                    if (!isAdded) return@addOnSuccessListener

                    // Mostramos un mensaje de éxito y volvemos a la pantalla anterior
                    Toast.makeText(context, getString(R.string.msg_entrada_actualizada), Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                .addOnFailureListener {
                    // Si el fragmento ya no está activo, evitamos mostrar mensajes
                    if (!isAdded) return@addOnFailureListener

                    // Mostramos un mensaje de error si no se pudo actualizar
                    Toast.makeText(context, getString(R.string.msg_error_actualizar), Toast.LENGTH_SHORT).show()
                }
        }
    }


    /**
     * Aplica opacidad a los iconos de emoción para resaltar la seleccionada.
     *
     * @param mood Emoción seleccionada ("feliz", "triste", etc.)
     */
    private fun resaltarEmocion(mood: String) {
        binding.emojiFeliz.alpha = if (mood == "feliz") 1f else 0.5f
        binding.emojiTriste.alpha = if (mood == "triste") 1f else 0.5f
        binding.emojiEnfadado.alpha = if (mood == "enfadado") 1f else 0.5f
        binding.emojiEnfermo.alpha = if (mood == "enfermo") 1f else 0.5f
    }
}
