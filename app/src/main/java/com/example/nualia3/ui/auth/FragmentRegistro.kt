package com.example.nualia3.ui.auth

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nualia3.MainActivity
import com.example.nualia3.R
import com.example.nualia3.databinding.FragmentRegistroBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

/**
 * Fragmento para que los nuevos usuarios puedan registrarse
 *
 * Aquí podrán crear una cuenta dando su nombre, su email, creando una contraseña y poniendo una imagen de perfil opcional
 * Usaré Firebase Authentication para el registro y Firebase Storage para subir la imagen de perfil, y Firstore para guardar datos adicionales
 *
 * Funcionalidades principales:
 * - Validación en tiempo real de los campos del formulario
 * - Comprobar la fuerza de la contraseña
 * - Cargar imagen de perfil desde galería
 * - Registrar la cuenta en Firebase
 * - Guardar los datos del usuario en Firestore
 * - Navegar al Login al completar el registro
 *
 * Navegación:
 * - A LoginFragment si el registro es exitoso
 * - También a LoginFragment si el usuario quiere volver atrás
 */

class FragmentRegistro : Fragment() {
    private var _binding : FragmentRegistroBinding? = null
    private val binding get() = _binding!!

    // Instancias de Firebase
    private lateinit var auth : FirebaseAuth
    private lateinit var db : FirebaseFirestore

    // Imagen de perfil seleccionada por el usuario
    private var uriImagenSeleccionada: Uri? = null

    /**
     * Como activityOnResult está deprecado vamos a usar un launcher para seleccionar la imagen de perfil desde la galería
     * Cuando se obtiene la imagen se actualiza la vista y se valida el formulario
     */
    private val seleccionarImagenLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uriImagenSeleccionada = uri
            binding.imagenPerfil.setImageURI(uri)
        }
        validarCampos()
    }

    /**
     * Inflar la vista del fragment y la configuración inicial
     * Inicializo Firebase, configuro listeners y las validaciones de formulario
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflamos el layout del fragment
        _binding = FragmentRegistroBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.btnSeleccionarImagen.setOnClickListener{
            seleccionarImagenLauncher.launch("image/*")
        }

        // Hacemos la validación de campos
        binding.nombre.doAfterTextChanged { validarCampos() }
        binding.email.doAfterTextChanged { validarCampos() }
        binding.contrasena.doAfterTextChanged { validarCampos() }

        // Deshabilitamos el botón de crear cuenta inicialmente, para poder habilitarlo cuando esté todo validado
        binding.crearCuentaButton.isEnabled = false

        // Lo que ocurrirá al pulsar el botón de "crear cuenta":
        binding.crearCuentaButton.setOnClickListener {
            val nombre = binding.nombre.text.toString().trim()
            val contrasena = binding.contrasena.text.toString().trim()
            val email = binding.email.text.toString().trim()

            if (validarContrasena(contrasena)){
                registrarUsuario(nombre, email, contrasena)
            } else {
                Toast.makeText(context, getString(R.string.msg_contrasena_invalida), Toast.LENGTH_LONG).show()
            }
        }

        // Para volver a la pantalla de Login
        binding.volverLogin.setOnClickListener{
            findNavController().navigate(R.id.action_fragmentRegistro_to_loginFragment)
        }

        return binding.root
    }

    /**
     * Verificamos si todos los campos obligatorios están completos
     * Habilitamos o deshabilitamos el botón según esa información
     */
    private fun validarCampos(){
        val nombreCompleto = binding.nombre.text.toString().trim().isNotEmpty()
        val emailCompleto = binding.email.text.toString().trim().isNotEmpty()
        val contrasenaCompleto = binding.contrasena.text.toString().trim().isNotEmpty()

        binding.crearCuentaButton.isEnabled = nombreCompleto && emailCompleto && contrasenaCompleto
    }

    /**
     * Validar si la contraseña cumple con los requisitos mínimos de seguridad
     *
     * @param contrasena Contraseña a validar
     * @return "true" si es válida, "false" si no lo es
     */
    private fun validarContrasena(contrasena: String): Boolean{
        // Más de 8 caracteres
        val longitudMinima = contrasena.length >=8
        // Tiene que tener alguna mayúscula
        val contieneMayuscula = contrasena.any {it.isUpperCase() }
        // Tiene que tener alguna minúscula
        val contieneMinuscula = contrasena.any {it.isLowerCase()}
        // Tiene que tener algún número
        val contieneNumero = contrasena.any { it.isDigit()}

        return longitudMinima && contieneMayuscula && contieneMinuscula && contieneNumero
    }

    /**
     * Registramos al usuario en Firebase Authentication y subimos los datos a Firestore
     * Guardamos la imagen de perfil en Firebase Storage SI la ha seleccionado
     *
     * @param nombre Nombre del usuario
     * @param email Correo electrónico
     * @param contrasena Contraseña
     */
    private fun registrarUsuario(nombre: String, email: String, contrasena: String){
        // Vamos a usar el método de Firebase para crear el usuario
        // Intentamos crear el usuario
        auth.createUserWithEmailAndPassword(email, contrasena)
            .addOnCompleteListener { tarea ->
                if(tarea.isSuccessful){
                    // Obtenemos el usuario autenticado actual
                    val usuario = auth.currentUser
                    // Por si acaso vemos que el usuario no sea null
                    if (usuario != null){
                        val uid = usuario.uid
                        // Comprobamos si el usuario ha subido una imagen de perfil, si es así, la vamos a subir a Firebase Storage
                        if(uriImagenSeleccionada != null){
                            // Creamos una referencia a la ruta donde guardaremos la imagen /perfil/UID.jpg
                            val ref = FirebaseStorage.getInstance().reference.child("perfil/$uid.jpg")
                            // Subimos la imagen que el usuario ha elegido
                            ref.putFile(uriImagenSeleccionada!!)
                                .addOnSuccessListener{
                                    // Obtenemos la url publica de descarga de esa imagen
                                    ref.downloadUrl.addOnSuccessListener { uri ->
                                        // Llamamos a guardarDatosUsuario con la URL para guardarlo todo en Firestore
                                        guardarDatosUsuario(uid, nombre, email, uri.toString())
                                    }.addOnFailureListener{
                                        // Si no ha funcionado
                                        Toast.makeText(context, "${getString(R.string.msg_error_url)}: ${it.message}", Toast.LENGTH_SHORT).show()
                                        // Guardamos los datos sin imagen
                                        guardarDatosUsuario(uid, nombre, email, "")
                                    }
                                }
                                .addOnFailureListener {
                                    // Si no ha funcionado
                                    Toast.makeText(context, "${getString(R.string.msg_error_subir_imagen)}: ${it.message}", Toast.LENGTH_SHORT).show()
                                    // Guardamos los datos sin imagen
                                    guardarDatosUsuario(uid, nombre, email, "")
                                }
                        } else {
                            // Si no se ha seleccionado una imagen
                            // Guardamos el nombre, el email y el campo vacío para la imagen
                            guardarDatosUsuario(uid, nombre, email, "")
                        }
                    } else {
                        // Si auth.currentUser es null (algo que no debería ocurrir)
                        Toast.makeText(context, getString(R.string.msg_usuario_no_autenticado), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "${getString(R.string.msg_error_registro)}: ${tarea.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    /**
     * Guardar los datos del usuario en Firestore
     * Incluir el nombre, email y URL de imagen (si se ha subido)
     *
     * @param uid ID del usuario EN FIREBASE
     * @param nombre Nombre del usuario
     * @param email Email del usuario
     * @param fotoUrl URL de la imagen de perfil (puede estar vacío porque es opcional)
     */
    private fun guardarDatosUsuario(uid: String, nombre: String, email: String, fotoUrl: String){
        // Creamos un hashmap con los datos que queremos guardar en la bbdd
        val userData = hashMapOf(
            "nombre" to nombre,
            "email" to email,
            "fotoUrl" to fotoUrl
        )
        // Accedemos a la colección usuarios_datos en Firestore (si no existe la crea automáticamente)
        // Usamos el uid de Firebase como ID del documento
        db.collection("usuarios_datos").document(uid)
            // Guardamos o sobreescribimos los datos del usuario
            .set(userData)
            .addOnSuccessListener {
                // Mostrar un mensaje de éxito
                Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show()
                // Llamamos a la función cargarDatosUsuarioEnDrawer que pertenece a MainActivity
                (requireActivity() as? MainActivity)?.cargarDatosUsuarioEnDrawer()
                // Si todo ha funcionado, redirigimos al usuario a la pantalla de login para que pueda iniciar sesion
                findNavController().navigate(R.id.loginFragment)
            }
            .addOnFailureListener{
                // Si no, lanzamos un mensaje de error
                Toast.makeText(context, "${getString(R.string.msg_error_guardar_usuario)}: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * Limpia el binding para evitar fugas de memoria al destruir la vista.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}