package com.example.nualia3.ui.diario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nualia3.R
import com.example.nualia3.databinding.FragmentListaDiarioBinding
import com.example.nualia3.ui.home.HomeViewModel

/**
 * Fragmento encargado de mostrar una lista de entradas del tipo "diario" del usuario actual.
 *
 * Esta clase se conecta al `HomeViewModel` para observar y mostrar las entradas
 * almacenadas en Firestore, filtrando solo las de tipo `"diario"`.
 * Utiliza un RecyclerView con el adaptador `DiarioAdapter`.
 *
 * Funcionalidades:
 * - Muestra un ProgressBar mientras se cargan los datos.
 * - Filtra las entradas tipo "diario" y las ordena por fecha de actualización.
 * - Navega al detalle de una entrada al hacer clic sobre ella.
 * - Muestra un mensaje si la lista de diarios está vacía.
 *
 * @see HomeViewModel
 * @see DiarioAdapter
 */
class FragmentListaDiario : Fragment() {

    private var _binding: FragmentListaDiarioBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var adapter: DiarioAdapter

    /**
     * Inflamos el layout del fragmento utilizando view binding.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentListaDiarioBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Configuramos el RecyclerView y observamos los datos del ViewModel.
     * Mostramos un mensaje si no hay diarios disponibles.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Inicializamos el adaptador y el comportamiento al hacer clic
        adapter = DiarioAdapter { entrada ->
            val bundle = Bundle().apply { putString("entradaId", entrada.id) }
            findNavController().navigate(R.id.fragmentDetalle, bundle)
        }

        binding.rvDiarios.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDiarios.adapter = adapter

        // Mostramos el progreso de carga
        binding.cargarProgreso.visibility = View.VISIBLE
        binding.contentListaDiario.visibility = View.GONE

        // Observamos los datos del ViewModel
        viewModel.entradasLiveData.observe(viewLifecycleOwner) { entradas ->
            val diarios = entradas.filter { it.tipo == "diario" }.sortedByDescending { it.actualizado }
            adapter.submitList(diarios)

            // Ocultamos el progreso y mostramos el contenido
            binding.cargarProgreso.visibility = View.GONE
            binding.contentListaDiario.visibility = View.VISIBLE

            // Mostramos un mensaje si no hay diarios
            binding.tvMensajeVacio.visibility = if (diarios.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.fabAnadirEntrada.setOnClickListener {
            findNavController().navigate(R.id.anadirEntradaFragment)
        }

        // Cargamos todas las entradas de tipo diario
        viewModel.cargarTodosLosDiarios()
    }

    /**
     * Recargamos los diarios cada vez que el fragmento vuelve a primer plano.
     */
    override fun onResume() {
        super.onResume()
        viewModel.cargarTodosLosDiarios()
    }

    /**
     * Liberamos el binding al destruir la vista.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
