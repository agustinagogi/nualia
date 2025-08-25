package com.example.nualia3.ui.busqueda

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nualia3.R
import com.example.nualia3.databinding.FragmentBuscarBinding
import com.example.nualia3.ui.home.EntradaAdapter
import com.example.nualia3.ui.home.HomeViewModel

/**
 * Fragmento que permite al usaurio buscar entradas mediante un campo de texto y muestra los resultados en tiempo real
 */
class FragmentBuscar : Fragment() {
    // ViewBinding
    private var _binding: FragmentBuscarBinding? = null
    private val binding get() = _binding!!

    // ViewModel compartido con el Home que gestiona las entradas
    private val viewModel: HomeViewModel by viewModels()

    // Adaptador del RecyclerView que muestra las entradas filtradas
    private lateinit var adaptador: EntradaAdapter

    /**
     * Infla el layout del fragment
     */

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBuscarBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Configuramos el RecyclerView, carga las entradas y escucha campios en el campo de búsqueda
     */

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adaptador = EntradaAdapter { entrada ->
            val bundle = Bundle().apply { putString("entradaId", entrada.id) }

            // Según el tipo de entrada, llevará al usaurio a la pantalla de edición que le corresponda
            when (entrada.tipo){
                "tarea" -> findNavController().navigate(R.id.fragmentEditarTarea, bundle)
                "evento" -> findNavController().navigate(R.id.fragmentEditarEvento, bundle)
                "nota" -> findNavController().navigate(R.id.fragmentEditNote, bundle)
                "diario" -> findNavController().navigate(R.id.fragmentEditarDiario, bundle)
                else -> findNavController().navigate(R.id.fragmentDetalle, bundle)
            }
        }

        // Configuramos el RecyclerView con el adaptador y diseño LinearLayout
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adaptador

        // Cargamos todas las entradas del usuario para poder filtrarlas localmente
        viewModel.cargarTodasEntradas()

        // Observamos los cambios en la lista d eentradas filtradas y actualiza el adaptador
        viewModel.entradasLiveData.observe(viewLifecycleOwner) { entradasFiltradas ->
            adaptador.submitList(entradasFiltradas)
        }

        // Añadimos un Listener para detectar texto mientras el usuario escribe
        binding.editTextBusqueda.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Actualizamos el texto de búsqueda en el ViewModel para filtrar resultados
                viewModel.establecerTextoBusqueda(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    /**
     * Limpiamos la referencia al binding para evitar fugas de memoria
     */
    override fun onDestroyView(){
        super.onDestroyView()
        _binding = null
    }
}