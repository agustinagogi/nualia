package com.example.nualia3.ui.diario

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.nualia3.R
import com.example.nualia3.databinding.FragmentDetalleBinding
import com.example.nualia3.ui.home.HomeViewModel
import com.example.nualia3.utils.FechaUtils

/**
 * Fragmento encargado de mostrar los detalles completos de una entrada (tarea, evento, nota o diario).
 *
 * Este fragmento:
 * - Carga y muestra la entrada usando su ID recibido por argumentos.
 * - Adapta la UI según el tipo de entrada (emociones, checkbox, notificación, imagen, etc.).
 * - Permite al usuario editar o eliminar la entrada.
 * - Redirige al usuario a login si no está autenticado.
 *
 * Usa el ViewModel compartido [HomeViewModel] para acceder a los datos en tiempo real desde Firestore.
 *
 * @see HomeViewModel.cargarEntradaPorIdTiempoReal
 * @see HomeViewModel.eliminarEntrada
 */
class FragmentDetalle : Fragment() {

    private lateinit var binding: FragmentDetalleBinding
    private val viewModel: HomeViewModel by viewModels()

    private var entradaId: String? = null

    // Obtenemos el ID de la entrada desde los argumentos al crear el fragmento
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        entradaId = arguments?.getString("entradaId")
    }

    // Inflamos la vista del fragmento usando ViewBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDetalleBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Lógica principal del fragmento. Cargamos la entrada desde Firestore,
     * observamos la autenticación y mostramos los datos visualmente.
     *
     * También configuramos los botones para editar o eliminar la entrada.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Ocultamos el contenido principal y mostramos el indicador de carga
        binding.contentLayout.visibility = View.GONE
        binding.cargarProgreso.visibility = View.VISIBLE

        // Comprobamos si el usuario está autenticado, si no lo redirigimos
        viewModel.authState.observe(viewLifecycleOwner) { isAuthenticated ->
            if (!isAuthenticated) {
                findNavController().navigate(R.id.loginFragment)
                return@observe
            }
        }

        // Si tenemos un ID válido, buscamos la entrada correspondiente
        entradaId?.let { id ->
            viewModel.cargarEntradaPorIdTiempoReal(id) { entrada ->
                if (!isAdded) return@cargarEntradaPorIdTiempoReal

                // Si encontramos la entrada, mostramos todos los datos
                if (entrada != null) {
                    // Ponemos el tipo como título de la barra
                    val tipo = when (entrada.tipo) {
                        "tarea" -> getString(R.string.tarea)
                        "evento" -> getString(R.string.evento)
                        "nota" -> getString(R.string.nota)
                        "diario" -> getString(R.string.diario)
                        else -> getString(R.string.entrada)
                    }
                    (requireActivity() as AppCompatActivity).supportActionBar?.title = tipo

                    // Mostramos título y fecha/hora
                    binding.tvTitulo.text = entrada.titulo
                    binding.tvHora.text = FechaUtils.formatearFechaHoraCompleta(
                        requireContext(),
                        entrada.fecha,
                        entrada.hora
                    )

                    // Si hay imagen, la cargamos con Glide
                    if (!entrada.imagenUrl.isNullOrEmpty()) {
                        binding.ivDiario.visibility = View.VISIBLE
                        Glide.with(this)
                            .load(entrada.imagenUrl)
                            .placeholder(R.color.placeholderColor)
                            .into(binding.ivDiario)
                    } else {
                        binding.ivDiario.visibility = View.GONE
                    }

                    // Mostramos u ocultamos elementos según el tipo de entrada
                    when (entrada.tipo) {
                        "tarea" -> {
                            // Mostramos notificación, checkbox, y hora
                            binding.layoutNotificarInfo.visibility = View.VISIBLE
                            binding.iconoNotificarInfo.setImageResource(
                                if (entrada.notificar) R.drawable.ic_notificacion_activada else R.drawable.ic_notificacion_desactivada
                            )
                            binding.tvHora.visibility = View.VISIBLE
                            binding.tvEmocion.visibility = View.GONE
                            binding.tvDescripcion.visibility = View.GONE
                            binding.checkHechoDetalle.visibility = View.VISIBLE
                            binding.checkHechoDetalle.isChecked = entrada.hecho
                        }

                        "evento" -> {
                            // Mostramos notificación, descripción, y hora
                            binding.layoutNotificarInfo.visibility = View.VISIBLE
                            binding.iconoNotificarInfo.setImageResource(
                                if (entrada.notificar) R.drawable.ic_notificacion_activada else R.drawable.ic_notificacion_desactivada
                            )
                            binding.tvHora.visibility = View.VISIBLE
                            binding.tvEmocion.visibility = View.GONE
                            binding.tvDescripcion.visibility = View.VISIBLE
                            binding.tvDescripcion.text = entrada.descripcion
                            binding.checkHechoDetalle.visibility = View.GONE
                        }

                        "nota", "diario" -> {
                            // Mostramos emoción y descripción, ocultamos notificación
                            binding.layoutNotificarInfo.visibility = View.GONE
                            binding.tvHora.visibility = View.VISIBLE
                            binding.tvEmocion.visibility = View.VISIBLE
                            binding.tvDescripcion.visibility = View.VISIBLE
                            binding.tvEmocion.setImageResource(getEmocionDrawableRes(entrada.emocion))
                            binding.tvDescripcion.text = entrada.descripcion
                            binding.checkHechoDetalle.visibility = View.GONE
                        }

                        else -> {
                            // En cualquier otro caso, ocultamos todo excepto la hora
                            binding.layoutNotificarInfo.visibility = View.GONE
                            binding.tvHora.visibility = View.VISIBLE
                            binding.tvDescripcion.visibility = View.GONE
                            binding.tvEmocion.visibility = View.GONE
                            binding.checkHechoDetalle.visibility = View.GONE
                        }
                    }

                    // Botón para eliminar la entrada
                    binding.btnEliminar.setOnClickListener {
                        viewModel.eliminarEntrada(entrada.id)
                        if (isAdded) findNavController().navigateUp()
                    }

                    // Botón para editar la entrada, abrimos el fragmento según tipo
                    binding.btnEditar.setOnClickListener {
                        val bundle = Bundle().apply { putString("entradaId", entrada.id) }
                        val destino = when (entrada.tipo) {
                            "tarea" -> R.id.fragmentEditarTarea
                            "evento" -> R.id.fragmentEditarEvento
                            "nota" -> R.id.fragmentEditNote
                            "diario" -> R.id.fragmentEditarDiario
                            else -> R.id.anadirEntradaFragment
                        }
                        findNavController().navigate(destino, bundle)
                    }

                    // Mostramos el contenido y ocultamos el loader
                    binding.contentLayout.visibility = View.VISIBLE
                    binding.cargarProgreso.visibility = View.GONE
                } else {
                    // Si no encontramos la entrada, mostramos mensaje y volvemos atrás
                    Toast.makeText(context, getString(R.string.msg_entrada_no_encontrada), Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        } ?: run {
            // Si no nos pasaron un ID válido, avisamos y cerramos el fragmento
            Toast.makeText(context, getString(R.string.msg_id_entrada_invalido), Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    /**
     * Devuelve el recurso gráfico correspondiente a una emoción.
     *
     * @param emocion Nombre de la emoción ("feliz", "triste", "enfadado", "enfermo").
     * @return ID del drawable asociado a esa emoción.
     */
    private fun getEmocionDrawableRes(emocion: String): Int {
        return when (emocion) {
            "feliz" -> R.drawable.emocion_feliz
            "triste" -> R.drawable.emocion_triste
            "enfadado" -> R.drawable.emocion_enfadado
            "enfermo" -> R.drawable.emocion_enfermo
            else -> R.drawable.emocion_feliz
        }
    }
}
