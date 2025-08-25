package com.example.nualia3.ui.calendario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nualia3.R
import com.example.nualia3.databinding.FragmentViewSemanalBinding
import com.example.nualia3.ui.home.EntradaAdapter
import com.example.nualia3.ui.home.HomeViewModel
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Fragmento que muestra la vista semanal de entradas.
 * Presenta una lista de días de la semana y permite ver entradas diarias,
 * navegar entre semanas, y acceder al detalle de cada entrada.
 */
class FragmentViewSemanal : Fragment() {

    // ViewBinding para acceder a las vistas
    private lateinit var binding: FragmentViewSemanalBinding

    // ViewModel para gestionar y observar datos compartidos
    private val viewModel: HomeViewModel by viewModels()

    // Adaptadores para la semana (lista de días) y las entradas de cada día
    private lateinit var semanaAdapter: SemanaAdapter
    private lateinit var entradaAdapter: EntradaAdapter

    // Guardamos el primer día (lunes) de la semana actual
    private var comienzoSemanaActual: LocalDate = LocalDate.now().with(DayOfWeek.MONDAY)

    /**
     * Infla el layout del fragmento
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentViewSemanalBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Se ejecuta cuando la vista ya está creada.
     * Aquí se configuran los adaptadores, listeners y observadores.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Configuramos el adaptador de días de la semana
        semanaAdapter = SemanaAdapter(getSemanaActual()) { fechaSeleccionada ->
            viewModel.selecDiaSemana(fechaSeleccionada)            // Actualizamos la fecha en el ViewModel
            viewModel.cargarEntradasPorFecha(fechaSeleccionada)    // Cargamos las entradas de ese día
        }

        // Mostramos los 7 días en formato de cuadrícula horizontal
        binding.rvDiasSemana.layoutManager = GridLayoutManager(requireContext(), 7)
        binding.rvDiasSemana.adapter = semanaAdapter

        // Configura el adaptador para mostrar las entradas del día seleccionado
        entradaAdapter = EntradaAdapter { entrada ->
            val bundle = Bundle().apply { putString("entradaId", entrada.id) }

            // Redirigimos al fragmento de detalle según el tipo (todos van al mismo en este caso)
            when (entrada.tipo) {
                "tarea", "evento", "nota", "diario" ->
                    findNavController().navigate(R.id.fragmentDetalle, bundle)
                else ->
                    findNavController().navigate(R.id.fragmentEditNote, bundle) // fallback
            }
        }

        binding.fabAnadirEntrada.setOnClickListener {
            findNavController().navigate(R.id.anadirEntradaFragment)
        }

        // Configuramos el RecyclerView de entradas con diseño lineal vertical
        binding.rvEntradasDelDia.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEntradasDelDia.adapter = entradaAdapter

        // Botón para ir a la semana anterior
        binding.btnSemAnterior.setOnClickListener {
            comienzoSemanaActual = comienzoSemanaActual.minusWeeks(1)
            actualizarSemana()
        }

        // Botón para ir a la semana siguiente
        binding.btnSemSig.setOnClickListener {
            comienzoSemanaActual = comienzoSemanaActual.plusWeeks(1)
            actualizarSemana()
        }

        // Observa las entradas filtradas por día
        viewModel.entradasLiveData.observe(viewLifecycleOwner) { entradas ->
            entradaAdapter.submitList(entradas)
        }

        // Si el usuario no está autenticado, lo redirige a login
        viewModel.authState.observe(viewLifecycleOwner) { isAuthenticated ->
            if (!isAuthenticated) {
                findNavController().navigate(R.id.loginFragment)
            }
        }

        // Determinamos la fecha inicial a mostrar (día seleccionado o hoy)
        val fechaInicial = viewModel.diaSemSelec.value ?: LocalDate.now()
        viewModel.selecDiaSemana(fechaInicial)
        viewModel.cargarEntradasPorFecha(fechaInicial)

        // Actualizamos visualmente la semana para que el día seleccionado aparezca resaltado
        semanaAdapter.actualizarSemana(getSemanaActual(), fechaInicial)
    }

    /**
     * Devolvemos una lista de los 7 días correspondientes a la semana actual.
     */
    private fun getSemanaActual(): List<LocalDate> {
        return (0..6).map { comienzoSemanaActual.plusDays(it.toLong()) }
    }

    /**
     * Actualizamos la lista de días y entradas según la nueva semana actual.
     */
    private fun actualizarSemana() {
        val nuevaSemana = getSemanaActual()
        val primerDiaSemana = nuevaSemana[0]
        viewModel.selecDiaSemana(primerDiaSemana)
        viewModel.cargarEntradasPorFecha(primerDiaSemana)
        semanaAdapter.actualizarSemana(nuevaSemana, primerDiaSemana)
    }
}