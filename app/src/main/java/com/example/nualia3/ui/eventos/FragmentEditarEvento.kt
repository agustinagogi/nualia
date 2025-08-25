package com.example.nualia3.ui.eventos

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.provider.Settings
import android.app.TimePickerDialog
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
import com.example.nualia3.databinding.FragmentEditarEventoBinding
import com.example.nualia3.datos.Entrada
import com.example.nualia3.utils.FechaUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
/**
 * Fragmento para editar un evento ya creado por el usuario.
 *
 * Permite visualizar y modificar los campos del evento como título, descripción, fecha, hora
 * y si se desea notificación. Los datos se cargan desde Firestore y se actualizan
 * tras la edición. También permite eliminar el evento.
 *
 * Además, si el usuario activa la notificación, se programa una alarma local en el dispositivo.
 *
 * Este fragmento se conecta con Firebase Auth, Firestore y AlarmManager para gestión de datos
 * y recordatorios.
 */
class FragmentEditarEvento : Fragment() {

    private lateinit var binding: FragmentEditarEventoBinding
    private lateinit var usuarioId: String
    private var entradaId: String? = null
    private var fechaBbdd = ""
    private var horaBbdd = ""
    private var notificar = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflamos el layout
        binding = FragmentEditarEventoBinding.inflate(inflater, container, false)

        // Obtenemos el UID del usuario autenticado
        usuarioId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Recuperamos el ID de la entrada pasada por argumentos
        entradaId = arguments?.getString("entradaId")

        // Ocultamos contenido y mostramos el cargador
        binding.contentLayout.visibility = View.GONE
        binding.cargarProgreso.visibility = View.VISIBLE

        // Si no hay ID, mostramos error y volvemos atrás
        if (entradaId.isNullOrEmpty()) {
            Toast.makeText(context, getString(R.string.msg_id_entrada_invalido), Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return binding.root
        }

        // Cargamos los datos del evento
        cargarEntrada()

        // Alternamos el estado del icono de notificación
        binding.iconoNotificar.setOnClickListener {
            notificar = !notificar
            binding.iconoNotificar.setImageResource(
                if (notificar) R.drawable.ic_notificacion_activada else R.drawable.ic_notificacion_desactivada
            )
        }

        // Abrimos selector de fecha y hora
        binding.campoFecha.setOnClickListener { mostrarSelectorFechaHora() }

        // Guardamos los cambios realizados
        binding.btnGuardar.setOnClickListener { guardarCambios() }

        // Eliminamos la entrada si se pulsa el botón eliminar
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
     * Cargamos los datos del evento desde Firestore y los colocamos en el formulario.
     * También verificamos si la notificación aún debe estar activa.
     */
    private fun cargarEntrada() {
        entradaId?.let { id ->
            FirebaseFirestore.getInstance()
                .collection("usuarios_datos").document(usuarioId)
                .collection("entradas").document(id)
                .get()
                .addOnSuccessListener { doc ->
                    if (!isAdded) return@addOnSuccessListener
                    val entrada = doc.toObject(Entrada::class.java)
                    if (entrada != null) {
                        // Verificamos si la fecha ya pasó para desactivar la notificación
                        try {
                            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            val entryDateTime = formatter.parse("${entrada.fecha} ${entrada.hora}")
                            notificar = entryDateTime?.after(Calendar.getInstance().time) == true && entrada.notificar
                        } catch (e: Exception) {
                            notificar = false
                        }

                        // Actualizamos el icono visualmente
                        binding.iconoNotificar.setImageResource(
                            if (notificar) R.drawable.ic_notificacion_activada else R.drawable.ic_notificacion_desactivada
                        )

                        // Ponemos los valores en los campos del formulario
                        binding.etTitulo.setText(entrada.titulo)
                        binding.etDescripcion.setText(entrada.descripcion)
                        fechaBbdd = entrada.fecha
                        horaBbdd = entrada.hora
                        binding.campoFecha.setText(
                            FechaUtils.formatearFechaHoraCompleta(
                                requireContext(),
                                fechaStr = fechaBbdd,
                                hora = horaBbdd
                            )
                        )


                        // Mostramos el contenido
                        binding.contentLayout.visibility = View.VISIBLE
                        binding.cargarProgreso.visibility = View.GONE
                    } else {
                        Toast.makeText(context, getString(R.string.msg_entrada_no_encontrada), Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                }
                .addOnFailureListener {
                    if (!isAdded) return@addOnFailureListener
                    Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
        }
    }

    /**
     * Validamos y guardamos los cambios hechos en el evento. También programamos notificación si aplica.
     */
    private fun guardarCambios() {
        val nuevoTitulo = binding.etTitulo.text.toString().trim()
        val nuevaDescripcion = binding.etDescripcion.text.toString().trim()

        if (nuevoTitulo.isEmpty()) {
            Toast.makeText(context, getString(R.string.msg_titulo_vacio), Toast.LENGTH_SHORT).show()
            return
        }

        if (fechaBbdd.isEmpty() || horaBbdd.isEmpty()) {
            Toast.makeText(context, getString(R.string.msg_seleccionar_fecha_hora), Toast.LENGTH_SHORT).show()
            return
        }

        entradaId?.let { id ->
            // Verificamos que no programemos notificación si la fecha ya pasó
            try {
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val selectedTime = formatter.parse("$fechaBbdd $horaBbdd")
                if (notificar && selectedTime?.before(Calendar.getInstance().time) == true) {
                    notificar = false
                }
            } catch (e: Exception) {
                notificar = false
            }

            // Creamos el mapa con los datos nuevos
            val actualizaciones = mapOf(
                "titulo" to nuevoTitulo,
                "descripcion" to nuevaDescripcion,
                "fecha" to fechaBbdd,
                "hora" to horaBbdd,
                "notificar" to notificar,
                "actualizado" to System.currentTimeMillis()
            )

            // Subimos a Firestore
            FirebaseFirestore.getInstance()
                .collection("usuarios_datos").document(usuarioId)
                .collection("entradas").document(id)
                .update(actualizaciones)
                .addOnSuccessListener {
                    if (!isAdded) return@addOnSuccessListener

                    if (notificar) {
                        // Si está activada, programamos la alarma
                        programarNotificacion(id, nuevoTitulo, nuevaDescripcion)
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
     * Programamos una notificación usando AlarmManager con la fecha y hora seleccionadas.
     */
    private fun programarNotificacion(id: String, titulo: String, mensaje: String) {
        val intent = Intent(requireContext(), com.example.nualia3.utils.ReceptorNotificacion::class.java).apply {
            putExtra("titulo", titulo)
            putExtra("mensaje", mensaje)
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
        alarmManager.cancel(pendingIntent)  // Cancelamos cualquier alarma anterior

        val formateador = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val calendario = Calendar.getInstance().apply {
            time = formateador.parse("$fechaBbdd $horaBbdd")!!
        }

        // Si estamos en Android S o superior, verificamos permisos extra
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendario.timeInMillis, pendingIntent)
            } else {
                Toast.makeText(context, getString(R.string.msg_permiso_alarmas), Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendario.timeInMillis, pendingIntent)
        }
    }

    /**
     * Mostramos un diálogo para que el usuario elija la fecha y la hora del evento.
     */
    private fun mostrarSelectorFechaHora() {
        val calendario = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                TimePickerDialog(requireContext(), { _, hour, minute ->
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
