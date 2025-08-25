package com.example.nualia3.ui.tareas

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nualia3.R
import com.example.nualia3.databinding.FragmentEditarTareaBinding
import com.example.nualia3.datos.Entrada
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.provider.Settings
import com.example.nualia3.utils.FechaUtils

/**
 * Fragmento para editar una entrada del tipo "tarea".
 * Permitimos modificar su título, estado de hecho, fecha, hora y si tiene notificación.
 * También se puede eliminar la tarea, o programar una notificación si aplica.
 */
class FragmentEditarTarea : Fragment() {

    private lateinit var binding: FragmentEditarTareaBinding
    private lateinit var usuarioId: String
    private var entradaId: String? = null
    private var fechaBbdd: String = ""
    private var horaBbdd: String = ""
    private var notificar = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflamos el layout
        binding = FragmentEditarTareaBinding.inflate(inflater, container, false)

        // Obtenemos el ID del usuario actual
        usuarioId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Recuperamos el ID de la tarea desde los argumentos
        entradaId = arguments?.getString("entradaId")

        // Ocultamos el contenido y mostramos el loader
        binding.contentLayout.visibility = View.GONE
        binding.cargarProgreso.visibility = View.VISIBLE

        // Si no tenemos ID, salimos
        if (entradaId.isNullOrEmpty()) {
            Toast.makeText(context, getString(R.string.msg_id_entrada_invalido), Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return binding.root
        }

        // Cargamos los datos de la tarea desde Firestore
        cargarDetallesTarea()

        // Ponemos el listener del icono para activar/desactivar notificación
        binding.iconoNotificar.setOnClickListener {
            notificar = !notificar
            binding.iconoNotificar.setImageResource(
                if (notificar) R.drawable.ic_notificacion_activada else R.drawable.ic_notificacion_desactivada
            )
        }

        // Guardamos los cambios al pulsar el botón
        binding.btnGuardar.setOnClickListener { guardarCambios() }

        // Eliminamos la tarea si se pulsa el botón de eliminar
        binding.btnEliminar.setOnClickListener {
            entradaId?.let { id ->
                FirebaseFirestore.getInstance()
                    .collection("usuarios_datos").document(usuarioId)
                    .collection("entradas").document(id)
                    .delete()
                    .addOnSuccessListener {
                        if (!isAdded) return@addOnSuccessListener
                        Toast.makeText(context, getString(R.string.msg_entrada_eliminada), Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                    .addOnFailureListener {
                        if (!isAdded) return@addOnFailureListener
                        Toast.makeText(context, getString(R.string.msg_error_eliminar), Toast.LENGTH_SHORT).show()
                    }
            }
        }

        return binding.root
    }

    /**
     * Cargamos los detalles de la tarea desde Firestore y los mostramos en la interfaz.
     */
    private fun cargarDetallesTarea() {
        entradaId?.let { id ->
            FirebaseFirestore.getInstance()
                .collection("usuarios_datos").document(usuarioId)
                .collection("entradas").document(id)
                .get()
                .addOnSuccessListener { doc ->
                    if (!isAdded) return@addOnSuccessListener
                    val entrada = doc.toObject(Entrada::class.java)
                    if (entrada != null) {
                        // Guardamos la fecha y hora para más adelante
                        fechaBbdd = entrada.fecha
                        horaBbdd = entrada.hora

                        // Verificamos si ya pasó la hora programada
                        try {
                            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            val entryDateTime = formatter.parse("$fechaBbdd $horaBbdd")
                            if (entryDateTime != null && entryDateTime.before(Calendar.getInstance().time)) {
                                notificar = false
                            } else {
                                notificar = entrada.notificar
                            }
                        } catch (e: Exception) {
                            notificar = false
                        }

                        // Ponemos los valores en la interfaz
                        binding.iconoNotificar.setImageResource(
                            if (notificar) R.drawable.ic_notificacion_activada else R.drawable.ic_notificacion_desactivada
                        )
                        binding.campoFecha.setText(
                            FechaUtils.formatearFechaHoraCompleta(
                                requireContext(),
                                fechaStr = fechaBbdd,
                                hora = horaBbdd
                            )
                        )
                        binding.etTitulo.setText(entrada.titulo)
                        binding.checkHecho.isChecked = entrada.hecho

                        // Mostramos el contenido y ocultamos el loader
                        binding.contentLayout.visibility = View.VISIBLE
                        binding.cargarProgreso.visibility = View.GONE
                    } else {
                        Toast.makeText(context, getString(R.string.msg_entrada_no_encontrada), Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                        return@addOnSuccessListener
                    }

                    // Mostramos el selector de fecha y hora si se pulsa en el campo
                    binding.campoFecha.setOnClickListener {
                        mostrarSelectorFechaHora()
                    }
                }
                .addOnFailureListener {
                    if (!isAdded) return@addOnFailureListener
                    Toast.makeText(context, getString(R.string.msg_error_generico, it.message), Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
        }
    }

    /**
     * Guardamos los cambios realizados a la tarea, incluyendo verificación de campos y fecha.
     */
    private fun guardarCambios() {
        val nuevoTitulo = binding.etTitulo.text.toString().trim()
        val nuevoHecho = binding.checkHecho.isChecked

        if (nuevoTitulo.isEmpty()) {
            Toast.makeText(context, getString(R.string.msg_titulo_vacio), Toast.LENGTH_SHORT).show()
            return
        }

        entradaId?.let { id ->
            try {
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val selectedTime = formatter.parse("$fechaBbdd $horaBbdd")
                if (notificar && selectedTime != null && selectedTime.before(Calendar.getInstance().time)) {
                    notificar = false
                }
            } catch (e: Exception) {
                notificar = false
            }

            // Creamos el mapa de actualizaciones
            val actualizaciones = mapOf(
                "titulo" to nuevoTitulo,
                "hecho" to nuevoHecho,
                "fecha" to fechaBbdd,
                "hora" to horaBbdd,
                "notificar" to notificar,
                "actualizado" to System.currentTimeMillis()
            )

            // Actualizamos en Firestore
            FirebaseFirestore.getInstance()
                .collection("usuarios_datos").document(usuarioId)
                .collection("entradas").document(id)
                .update(actualizaciones)
                .addOnSuccessListener {
                    if (!isAdded) return@addOnSuccessListener

                    // Si hay notificación activa, la programamos
                    if (notificar && fechaBbdd.isNotBlank() && horaBbdd.isNotBlank()) {
                        programarNotificacion(id, nuevoTitulo)
                    }

                    Toast.makeText(context, getString(R.string.msg_entrada_actualizada), Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                .addOnFailureListener {
                    if (!isAdded) return@addOnFailureListener
                    Toast.makeText(context, getString(R.string.msg_error_actualizar), Toast.LENGTH_SHORT).show()
                }
        }
    }

    /**
     * Programamos una notificación en el sistema usando AlarmManager.
     */
    private fun programarNotificacion(id: String, titulo: String) {
        val intent = Intent(requireContext(), com.example.nualia3.utils.ReceptorNotificacion::class.java).apply {
            putExtra("titulo", titulo)
            putExtra("mensaje", getString(R.string.recordatorio_tarea))
            putExtra("entradaId", id)
            putExtra("usuarioId", usuarioId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)

        val formateador = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val calendario = Calendar.getInstance().apply {
            time = formateador.parse("$fechaBbdd $horaBbdd")!!
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendario.timeInMillis, pendingIntent)
            } else {
                Toast.makeText(context, getString(R.string.msg_permiso_alarmas), Toast.LENGTH_LONG).show()
                val intentSettings = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intentSettings)
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendario.timeInMillis, pendingIntent)
        }
    }

    /**
     * Mostramos un DatePicker seguido de un TimePicker para que el usuario seleccione fecha y hora.
     */
    private fun mostrarSelectorFechaHora() {
        val calendario = Calendar.getInstance()
        android.app.DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                android.app.TimePickerDialog(requireContext(), { _, hour, minute ->
                    calendario.set(year, month, day, hour, minute)
                    fechaBbdd = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendario.time)
                    horaBbdd = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendario.time)
                    binding.campoFecha.setText(
                        FechaUtils.formatearFechaHoraCompleta(
                            requireContext(),
                            fechaStr = fechaBbdd,
                            hora = horaBbdd
                        )
                    )
                }, calendario.get(Calendar.HOUR_OF_DAY), calendario.get(Calendar.MINUTE), true).show()
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}