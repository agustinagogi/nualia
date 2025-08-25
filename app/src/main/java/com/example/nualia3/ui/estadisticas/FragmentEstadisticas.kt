package com.example.nualia3.ui.estadisticas

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.nualia3.R
import com.example.nualia3.ui.home.HomeViewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
/**
 * Fragmento encargado de mostrar estadísticas visuales sobre las emociones registradas en entradas tipo "diario".
 *
 * Este fragmento realiza las siguientes acciones:
 * - Carga desde Firebase Firestore todas las entradas del tipo "diario" del usuario autenticado.
 * - Cuenta cuántas veces se repite cada emoción registrada (feliz, triste, enfadado, enfermo).
 * - Genera un gráfico circular (`PieChart`) con la proporción de cada emoción.
 * - Muestra un resumen textual de los totales y una barra de progreso para cada emoción.
 * - Si no hay entradas, muestra un mensaje correspondiente.
 *
 * Además, controla:
 * - El estado de autenticación: si no hay usuario autenticado, redirige al login.
 * - El tema del dispositivo para adaptar los colores del gráfico (modo claro/oscuro).
 *
 * Librerías utilizadas:
 * - Firebase Firestore: para acceder a las entradas de diario.
 * - MPAndroidChart: para renderizar el gráfico circular.
 * - ViewModel (`HomeViewModel`): para observar el estado de autenticación.
 */
class FragmentEstadisticas : Fragment() {

    private lateinit var containerEstadisticas: LinearLayout
    private lateinit var graficoCircular: PieChart
    private lateinit var progressBar: ProgressBar
    private lateinit var contentLayout: LinearLayout
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_estadisticas, container, false)
        containerEstadisticas = view.findViewById(R.id.containerEstadisticas)
        graficoCircular = view.findViewById(R.id.pieChart)
        progressBar = view.findViewById(R.id.cargarProgreso)
        contentLayout = view.findViewById(R.id.contentEstadisticas)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Mostramos el círculo de carga y ocultamos el contenido principal mientras se cargan los datos
        progressBar.visibility = View.VISIBLE
        contentLayout.visibility = View.GONE

        // Obtenemos el ID del usuario autenticado
        val usuarioId = FirebaseAuth.getInstance().currentUser?.uid
        if (usuarioId == null) {
            // Redirigimos al login si no hay usuario autenticado
            findNavController().navigate(R.id.loginFragment)
            return
        }

        val firestore = FirebaseFirestore.getInstance()

        // Consultamos en Firestore todas las entradas del tipo "diario" del usuario
        firestore.collection("usuarios_datos").document(usuarioId).collection("entradas")
            .whereEqualTo("tipo", "diario")
            .get()
            .addOnSuccessListener { snapshot ->
                // Ocultamos el progreso y mostramos el contenido cuando los datos ya están disponibles
                progressBar.visibility = View.GONE
                contentLayout.visibility = View.VISIBLE

                // Obtenemos la lista de emociones registradas
                val emociones = snapshot.documents.mapNotNull { it.getString("emocion") }
                val total = emociones.size
                val contadorEmociones = emociones.groupingBy { it }.eachCount()

                // Limpiamos el contenedor antes de agregar las nuevas estadísticas
                containerEstadisticas.removeAllViews()

                // Mostramos el número total de entradas de diario
                val totalText = getString(R.string.total_entradas_de_diario, total)
                view.findViewById<TextView>(R.id.txtResumen).text = totalText

                // Si no hay entradas, mostramos un mensaje y salimos del listener
                if (total == 0) {
                    graficoCircular.clear()
                    containerEstadisticas.addView(TextView(requireContext()).apply {
                        text = getString(R.string.no_hay_entradas_diario)
                    })
                    return@addOnSuccessListener
                }

                // Definimos las etiquetas traducidas para cada emoción
                val etiquetasEmocion = mapOf(
                    "feliz" to getString(R.string.mood_feliz),
                    "triste" to getString(R.string.mood_triste),
                    "enfadado" to getString(R.string.mood_enfadado),
                    "enfermo" to getString(R.string.mood_enfermo)
                )

                // Preparamos las entradas y colores para el gráfico circular
                val pieEntries = ArrayList<PieEntry>()
                val colors = ArrayList<Int>()

                // Recorremos cada emoción y agregamos sus datos al gráfico
                etiquetasEmocion.forEach { (key, label) ->
                    val count = contadorEmociones[key] ?: 0
                    if (count > 0) {
                        pieEntries.add(PieEntry(count.toFloat(), label))
                        colors.add(
                            when (key) {
                                "feliz" -> ContextCompat.getColor(requireContext(), R.color.feliz)
                                "triste" -> ContextCompat.getColor(requireContext(), R.color.triste)
                                "enfadado" -> ContextCompat.getColor(requireContext(), R.color.enfadado)
                                "enfermo" -> ContextCompat.getColor(requireContext(), R.color.enfermo)
                                else -> Color.GRAY
                            }
                        )
                    }
                }

                // Creamos el set de datos y lo configuramos
                val setDatos = PieDataSet(pieEntries, "")
                setDatos.colors = colors
                setDatos.valueTextSize = 14f

                // Asignamos los datos al gráfico
                val datos = PieData(setDatos)
                graficoCircular.data = datos
                graficoCircular.description.isEnabled = false
                graficoCircular.setUsePercentValues(true)

                // Determinamos si el sistema está en modo oscuro
                val isDarkTheme = resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                        android.content.res.Configuration.UI_MODE_NIGHT_YES

                // Establecemos el color del texto del gráfico según el tema
                val textColor = if (isDarkTheme) Color.WHITE else Color.BLACK
                graficoCircular.setEntryLabelColor(textColor)
                graficoCircular.setCenterTextColor(textColor)
                graficoCircular.setCenterText(getString(R.string.center_text_mood))
                graficoCircular.legend.textColor = textColor

                // Refrescamos el gráfico para que se dibuje
                graficoCircular.invalidate()

                // Mostramos debajo del gráfico un resumen con cada emoción y su porcentaje
                etiquetasEmocion.forEach { (key, label) ->
                    val cuenta = contadorEmociones[key] ?: 0
                    val porcentaje = (cuenta * 100) / total

                    val emocionView = LayoutInflater.from(context).inflate(R.layout.item_emocion_estadistica, containerEstadisticas, false)
                    val emocionTexto = getString(R.string.emocion_con_contador, label, cuenta)
                    emocionView.findViewById<TextView>(R.id.tvEmocion).text = emocionTexto
                    emocionView.findViewById<ProgressBar>(R.id.progressBar).progress = porcentaje

                    containerEstadisticas.addView(emocionView)
                }
            }
            .addOnFailureListener {
                // Si ocurre un error, ocultamos el progreso, mostramos el contenido
                // y avisamos al usuario con un mensaje
                progressBar.visibility = View.GONE
                contentLayout.visibility = View.VISIBLE
                containerEstadisticas.addView(TextView(requireContext()).apply {
                    text = getString(R.string.error_cargar_datos)
                })
            }

        // Observamos el estado de autenticación
        viewModel.authState.observe(viewLifecycleOwner) { isAuthenticated ->
            if (!isAuthenticated) {
                findNavController().navigate(R.id.loginFragment)
            }
        }

    }

}