package com.example.nualia3.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nualia3.R
import com.example.nualia3.databinding.FragmentHomeBinding
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class FragmentHome : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var adapter: EntradaAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = EntradaAdapter { entrada  ->
            val bundle = Bundle().apply { putString("entradaId", entrada .id) }
            findNavController().navigate(R.id.fragmentDetalle, bundle)
        }

        binding.rvEntradas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEntradas.adapter = adapter

        viewModel.entradasLiveData.observe(viewLifecycleOwner) { entradas ->
            val fechaSeleccionada = viewModel.fechaSeleccionada.value
            val filteredEntries = entradas.filter { it.fecha == fechaSeleccionada?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) }
            adapter.submitList(filteredEntries)
        }

        // Observa el estado de autenticación
        viewModel.authState.observe(viewLifecycleOwner) { isAuthenticated ->
            if (!isAuthenticated) {
                findNavController().navigate(R.id.loginFragment)
            }
        }

        // Observa y mantiene la fecha seleccionada
        viewModel.fechaSeleccionada.observe(viewLifecycleOwner) { fecha ->
            Log.d("Calendario", "Fecha seleccionada: $fecha")
            binding.calendarView.date = fecha.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            viewModel.cargarEntradasPorFecha(fecha)
        }

        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val fechaSeleccionada = LocalDate.of(year, month + 1, dayOfMonth)
            viewModel.selecFecha(fechaSeleccionada)
        }

        // Inicializa con fecha actual solo si aún no hay selección
        if (viewModel.fechaSeleccionada.value == null) {
            val today = LocalDate.now()
            viewModel.selecFecha(today)
        }

        binding.fabAnadirEntrada.setOnClickListener {
            findNavController().navigate(R.id.anadirEntradaFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}