package com.example.nualia3.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nualia3.MainActivity
import com.example.nualia3.R
import com.example.nualia3.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth

/**
 * Fragmento que gestiona la pantalla de inicio de sesión
 * Aquí los usuarios podrán iniciar sesión con el email y la contraseña
 * También podrán acceder al registro si aún no tienen cuenta
 * O solicitar un correo para restablecer la contraseña
 *
 * Funciona con Firebase Authentication para validar las credenciales
 *
 * Navegaciones posibles:
 *  - A la pantalla de registro (FragmentRegistro)
 *  - A la pantalla principal de la app (HomeFragment)
 *
 *  Componentes principales:
 *  - FirebaseAuth para autenticarse
 *  - FragmentLoginBinding para acceder a la vista
 */
class FragmentLogin : Fragment() {
    // Binding a la vista del layout
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // Instancia de FirebaseAuth
    private lateinit var auth: FirebaseAuth

    /**
     * Crea y devuelve la vista del fragmento, inicializa FirebaseAuth y configura
     * los listeners de los botones de login, registro y restablecer contraseña
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (auth.currentUser != null) {
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            return
        }

        // Listener del botón registrarse
        binding.botonRegistro.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_fragmentRegistro)
        }

        // Listener del botón "Iniciar sesión"
        binding.botonLogin.setOnClickListener {
            // Cogemos el texto que el usuario ha puesto en los campos, convertimos el editable a un String, eliminamos espacios en blanco al inicio y al final y lo pasamos a una variable
            val email = binding.correo.text.toString().trim()
            val contrasena = binding.contrasena.text.toString().trim()
            if (email.isNotEmpty() && contrasena.isNotEmpty()){
                iniciarSesionUsuario(email, contrasena)
            } else {
                Toast.makeText(context, getString(R.string.msg_completar_campos), Toast.LENGTH_SHORT).show()
            }
        }
        // Listener del texto "¿Has olvidado la contraseña?"
        binding.textoOlvidoContrasena.setOnClickListener {
            val email = binding.correo.text.toString().trim()
            if(email.isNotEmpty()){
                enviarRestaurarContrasena(email)
            } else {
                Toast.makeText(context, getString(R.string.msg_introducir_correo), Toast.LENGTH_SHORT).show()
            }
        }
    }


    /**
     * Intentar iniciar sesión en Firebase con el email y la contraseña
     * Mostramos un error si la autenticación falla
     * @param email Correo electrónico del usuario
     * @param contrasena Contraseña del usuario
     */
    private fun iniciarSesionUsuario(email: String, contrasena: String){
        // Como usamos Firebase para iniciar sesión usaremos su metodo signInWithEmailAndPassword
        // Que devolverá una tarea que se ejecuta en segundo plano
        auth.signInWithEmailAndPassword(email, contrasena)
            .addOnCompleteListener { tarea ->
                // Aquí definimos lo que ocurrirá cuando al tarea se complete
                if(tarea.isSuccessful){
                    // Si la vista del fragmento aún existe y no ha sido destruida entonces ejecutamos el bloque let
                    view?.let {
                        (requireActivity() as? MainActivity)?.cargarDatosUsuarioEnDrawer() // << AÑADIR ESTA LÍNEA
                        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                    }
                } else {
                    Toast.makeText(context, getString(R.string.msg_error_login), Toast.LENGTH_SHORT).show()
                }

            }
    }

    /**
     * Enviamos un correo electrónico para restablecer la contraseña del usuario
     *
     * @param email Correo electrónico del usuario
     */
    private fun enviarRestaurarContrasena(email: String){
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { tarea ->
                if(tarea.isSuccessful){
                    Toast.makeText(context, getString(R.string.msg_correo_restablecer), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "${getString(R.string.msg_error_generico)}: ${tarea.exception?.message}", Toast.LENGTH_LONG).show()
                }

            }
    }

    /**
     * Libera el binding al destruir la vista para evitar fugas de memoria
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}