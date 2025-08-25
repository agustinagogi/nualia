package com.example.nualia3.ui.entradas

import android.app.Activity
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.nualia3.utils.IdiomaHelper
import com.example.nualia3.R
import com.example.nualia3.databinding.FragmentAnadirEntradaBinding
import com.example.nualia3.datos.Entrada
import com.example.nualia3.ui.home.HomeViewModel
import com.example.nualia3.utils.FechaUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import android.provider.Settings


/**
 * Fragment que permite al usuario crear una entrada en la app Nualia
 *
 * Soporta estas entradas:
 * - Tarea: con fecha, hora y opción de notificación
 * - Evento: con descripcion, fecha, hora y notificación
 * - Nota: con título y descripción
 * - Diario: con descripción, emoción e imagen
 *
 * Funcionalidades:
 * - Gestión del formulario según el tipo seleccionado
 * - Validación de campos obligatorios
 * - Almacenamiento de datos en Firestore
 * - Subida de imagen a Firebase Storage si es un diario
 * - Programación de notificación con AlarmManager
 * - Edición si se recibe entradaId en los argumentos
 *
 * Navegación:
 * - Usa Navigation Component para volver después de guardar
 * - Usa ViewModel compartido para coger la fecha seleccionada del Home
 */
class FragmentAnadirEntrada : Fragment() {
    // ViewBinding
    private lateinit var binding: FragmentAnadirEntradaBinding

    // Variables de la entrada
    private var calendarioSeleccionado = Calendar.getInstance()
    private var fechaBbdd = ""
    private var horaBbdd = ""
    private var emocionSeleccionada: String = ""
    private var entradaId: String? = null
    private var uriImagenSeleccionada: Uri? = null
    private var notificar = false

    // Lanzador para seleccionar imagen de la galería
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    /**
     * Infla la vista del fragment, configura los listeners de tipo, imagen, fecha y guardar
     */

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAnadirEntradaBinding.inflate(inflater, container, false)

        // Escucha cambios en el tipo de entrada
        binding.rgTipo.setOnCheckedChangeListener {_, checkedId ->
            when (checkedId) {
                R.id.rbTarea -> { mostrarCamposTarea(); binding.tvPantallaTitulo.setText(R.string.anadir_tarea) }
                R.id.rbNota -> { mostrarCamposNota(); binding.tvPantallaTitulo.setText(R.string.anadir_nota) }
                R.id.rbEvento -> { mostrarCamposEvento(); binding.tvPantallaTitulo.setText(R.string.anadir_evento) }
                R.id.rbDiario -> { mostrarCamposDiario(); binding.tvPantallaTitulo.setText(R.string.anadir_diario) }
            }

        }

        // Fecha/Hora
        binding.campoFecha.setOnClickListener { mostrarSelectorFechaHora() }

        // Notificación
        binding.iconoNotificar.setOnClickListener {
            notificar = !notificar
            binding.iconoNotificar.setImageResource(if (notificar) R.drawable.ic_notificacion_activada else R.drawable.ic_notificacion_desactivada)
        }

        // Guardar entrada
        binding.btnGuardar.setOnClickListener { guardarEntrada() }

        // Seleccionar imagen
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                uriImagenSeleccionada = result.data?.data
                binding.ivDiario.setImageURI(uriImagenSeleccionada)
            }
        }

        return binding.root

    }

    /**
     * Extrae el ID de la entrada (si existe) y carga sus datos en caso de edición
     * Si es nueva, configura un tipo por defecto y obtiene la fecha desde el HomeViewModel
     */
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        entradaId = arguments?.getString("entradaId")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Si la entrada existe
        if (entradaId != null) {
            cargarEntradaParaEditar(entradaId!!)
        } else {
            // Obtiene el ViewModel del fragmento Home (compartido con este)
            val homeViewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)
            // Intenta tomar la fecha seleccionada en el calendario del Home
            homeViewModel.fechaSeleccionada.value?.let { fecha ->
                // Convierte la fecha LocalDate del ViewModel a Calendar
                val calendar = Calendar.getInstance().apply {
                    time = Date.from(fecha.atStartOfDay(ZoneId.systemDefault()).toInstant())
                }
                // Asigna esa fecha al calendario actual del fragmento
                calendarioSeleccionado = Calendar.getInstance().apply { time = calendar.time }

                // Formatea y guarda la fecha y hora en variables para Firestore
                fechaBbdd = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                val ahora = Calendar.getInstance()
                horaBbdd = SimpleDateFormat("HH:mm", Locale.getDefault()).format(ahora.time)
            }

            binding.rgTipo.check(R.id.rbTarea)
            mostrarCamposTarea()
        }
    }

    /**
     * Cargar una entrada ya existente desde Firestore y rellena el formualrio para edición
     */
    private fun cargarEntradaParaEditar(id: String) {
        // Obtenemos el ID del usuario actualmente autenticado en Firebase.
        // Si no hay usuario (no está autenticado), se interrumpe la función inmediatamente.
        val usuarioId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        // Accedemos a Firestore: usuarios_datos/{usuarioId}/entradas/{id}
        FirebaseFirestore.getInstance()
            .collection("usuarios_datos")
            .document(usuarioId)
            .collection("entradas")
            .document(id)
            .get()
            .addOnSuccessListener { doc ->
                // Convertimos el documento Firestore a un objeto Entrada
                val entrada = doc.toObject(Entrada::class.java)
                // Si la conversión fue exitosa y la entrada existe:
                if (entrada != null) {
                    // Rellenamos los campos del formulario con los datos de la entrada
                    binding.etTitulo.setText(entrada.titulo)
                    binding.etDescripcion.setText(entrada.descripcion)
                    // Almacenamos la fecha y hora en variables internas para uso posterior (guardado, validación)
                    fechaBbdd = entrada.fecha
                    horaBbdd = entrada.hora

                    // Cargamos el estado de notificación
                    notificar = entrada.notificar

                    // Si la fecha ya ha pasado, se desactiva automáticamente la opción de notificar
                    try {
                        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        val entryDateTime = formatter.parse("${entrada.fecha} ${entrada.hora}")
                        val now = java.util.Calendar.getInstance().time
                        if (entryDateTime != null && entryDateTime.before(now)) {
                            notificar = false
                        }
                    } catch (e: Exception) {
                        // Si ocurre un error al parsear la fecha, desactiva notificaciones como precaución
                        notificar = false
                    }

                    // Actualizamos el icono visualmente
                    binding.iconoNotificar.setImageResource(
                        if (notificar) R.drawable.ic_notificacion_activada else R.drawable.ic_notificacion_desactivada
                    )


                    // Marcamos el tipo de entrada en el selector (radio buttons) y muestra los campos correspondientes
                    when (entrada.tipo) {
                        "tarea" -> {
                            binding.rgTipo.check(R.id.rbTarea)
                            mostrarCamposTarea()
                        }
                        "nota" -> {
                            binding.rgTipo.check(R.id.rbNota)
                            mostrarCamposNota()
                        }
                        "evento" -> {
                            binding.rgTipo.check(R.id.rbEvento)
                            mostrarCamposEvento()
                        }
                        "diario" -> {
                            binding.rgTipo.check(R.id.rbDiario)
                            mostrarCamposDiario()
                        }
                    }
                    // Cargamos y resaltamos la emoción seleccionada antes (para entradas de tipo diario)
                    emocionSeleccionada = entrada.emocion
                    resaltarEmocionSeleccionada(entrada.emocion)
                }
            }
    }

    /**
     * Guarda una nueva entrada o actualiza una existente
     * Gestiona validaciones, subida de imagen (si es una entrada de diario), y programación de notificaciones
     */
    private fun guardarEntrada(){
        // Obtenemos y limpiamos el título introducido
        val titulo = binding.etTitulo.text.toString().trim()
        val descripcion = binding.etDescripcion.text.toString().trim()

        // Determinamos el tipo de entrada seleccionada en los radio buttons
        val tipo = when (binding.rgTipo.checkedRadioButtonId){
            R.id.rbTarea -> "tarea"
            R.id.rbNota -> "nota"
            R.id.rbEvento -> "evento"
            R.id.rbDiario -> "diario"
            else -> "nota"
        }

        // Validamos que si es un diario, se haya seleccionado una emoción
        if (tipo == "diario" && emocionSeleccionada.isBlank()) {
            Toast.makeText(context, getString(R.string.msg_emocion_requerida), Toast.LENGTH_SHORT).show()
            return
        }

        // Si no es una nota, debe tener fecha y hora (ej. tareas y eventos)
        if (tipo != "nota" && (fechaBbdd.isEmpty() || horaBbdd.isEmpty())) {
            Toast.makeText(context, getString(R.string.msg_seleccionar_fecha_hora), Toast.LENGTH_LONG).show()
            return
        }

        // Verificamos que el título no esté vacío
        if (titulo.isEmpty()) {
            Toast.makeText(context, getString(R.string.msg_titulo_vacio), Toast.LENGTH_SHORT).show()
            return
        }
        // Obtenemos el ID del usuario actual
        val usuarioId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        // Referenciamos a la colección Firestore donde se guardan las entradas
        val entriesRef = FirebaseFirestore.getInstance()
            .collection("usuarios_datos")
            .document(usuarioId)
            .collection("entradas")

        // Si entradaId ya existe, se trata de una edición
        if (entradaId != null) {
            val docRef = entriesRef.document(entradaId!!)
            docRef.get().addOnSuccessListener { docSnapshot ->
                val entradaExistente = docSnapshot.toObject(Entrada::class.java)
                val imagenActual = entradaExistente?.imagenUrl ?: ""
                val hechoActual = entradaExistente?.hecho ?: false

                // Si es tarea o evento con notificación, validamos que la hora no esté en el pasado
            if ((tipo == "tarea" || tipo == "evento") && notificar) {
                try {
                    val formateador = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    val horaSeleccionada = formateador.parse("$fechaBbdd $horaBbdd")
                    if (horaSeleccionada != null && horaSeleccionada.time <= System.currentTimeMillis()) {
                        notificar = false
                    }
                } catch (e: Exception) {
                    notificar = false
                }
            }

            // Creamos el objeto Entrada con los datos actualizados
            val entrada = Entrada(
                id = entradaId!!,
                usuarioId = usuarioId,
                titulo = titulo,
                descripcion = descripcion,
                tipo = tipo,
                fecha = fechaBbdd,
                hora = horaBbdd,
                emocion = if (tipo=="diario") emocionSeleccionada else "",
                actualizado = System.currentTimeMillis(),
                imagenUrl = imagenActual,
                hecho = hechoActual,
                notificar = notificar
            )
                // Guardamos la entrada en Firestore
                docRef.set(entrada).addOnSuccessListener {
                    Toast.makeText(context, getString(R.string.msg_entrada_actualizada), Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }.addOnFailureListener {
                    Toast.makeText(context, getString(R.string.msg_error_actualizar, it.message), Toast.LENGTH_LONG).show()
                }
            }
        } else {
            val nuevoDoc = entriesRef.document()
            val idGenerado = nuevoDoc.id
            // Si es nueva entrada y es tipo diario con imagen seleccionada, primero subimos la imagen
            if (tipo == "diario" && uriImagenSeleccionada != null) {
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("diarios/$usuarioId/${System.currentTimeMillis()}.jpg")
                // Subimos la imagen a Firebase Storage
                storageRef.putFile(uriImagenSeleccionada!!)
                    .addOnSuccessListener {
                        // Si la subida fue exitosa, obtenemos la URL de descarga
                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                            guardarNuevaEntrada(nuevoDoc, idGenerado, usuarioId, titulo, descripcion, tipo, fechaBbdd, horaBbdd, emocionSeleccionada, uri.toString())
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, getString(R.string.msg_error_subir_imagen, it.message), Toast.LENGTH_LONG).show()
                    }
            } else {
                // Si no es tipo diario o no tiene imagen, guardamos directamente
                guardarNuevaEntrada(nuevoDoc, idGenerado, usuarioId, titulo, descripcion, tipo, fechaBbdd, horaBbdd, emocionSeleccionada, "")
            }
        }
    }

    /**
     * Guarda una nueva entrada en Firestore y programa una notificación si es necesario.
     *
     * @param docRef Referencia al documento Firestore listo para usar
     * @param entradaId ID único generado para esta entrada
     * @param usuarioId ID del usuario autenticado
     * @param titulo Título de la entrada
     * @param descripcion Contenido o descripción opcional
     * @param tipo Tipo de entrada: tarea, evento, nota o diario
     * @param fecha Fecha en formato yyyy-MM-dd
     * @param hora Hora en formato HH:mm
     * @param emocion Emoción seleccionada (sólo aplica a diarios)
     * @param imagenUrl URL de imagen si es un diario (puede estar vacía)
     */
    private fun guardarNuevaEntrada(
        // Creamos la entrada con todos los datos que queremos guardar
        docRef: com.google.firebase.firestore.DocumentReference,
        entradaId: String,
        usuarioId: String,
        titulo: String,
        descripcion: String,
        tipo: String,
        fecha: String,
        hora: String,
        emocion: String,
        imagenUrl: String
    ) {
        val entrada = Entrada(
            id = entradaId,
            usuarioId = usuarioId,
            titulo = titulo,
            descripcion = descripcion,
            tipo = tipo,
            fecha = fecha,
            hora = hora,
            emocion = if (tipo == "diario") emocion else "", // solo guardamos la emoción si es diario
            actualizado = System.currentTimeMillis(),        // guardamos la fecha actual como "última modificación"
            imagenUrl = imagenUrl,
            hecho = false,                                   // una entrada nueva aún no debe estar marcada como hecha
            notificar = this.notificar
        )

        // Si el usuario quiere una notificación y es una tarea o evento, la programamos
        if ((tipo == "tarea" || tipo == "evento") && notificar) {
            try {
                // Parseamos la fecha y hora para convertirlo a milisegundos
                val formato = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val fechaHora = formato.parse("$fecha $hora") ?: throw IllegalArgumentException("Fecha u hora inválidas")
                // Creamos un calendario con esa fecha/hora y lo ajustamos si ya pasó
                val tiempoAlarma = java.util.Calendar.getInstance().apply {
                    time = fechaHora
                    if (timeInMillis <= System.currentTimeMillis()) add(Calendar.MINUTE, 1)
                }.timeInMillis

                // Creamos el intent que se enviará cuando suene la alarma
                val intent = Intent(requireContext(), com.example.nualia3.utils.ReceptorNotificacion::class.java).apply {
                    putExtra("titulo", titulo)
                    putExtra("mensaje", if (tipo == "tarea") getString(R.string.recordatorio_tarea) else descripcion)
                    putExtra("entradaId", entradaId)
                    putExtra("usuarioId", usuarioId)
                }

                // Preparamos el PendingIntent (obligatoriamente con FLAG_IMMUTABLE)
                val pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    entradaId.hashCode(),
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
                )

                // Pedimos el AlarmManager al sistema
                val alarmManager = requireContext().getSystemService(AlarmManager::class.java)

                // Si estamos en Android 12+ y el sistema permite alarmas exactas, la programamos
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, tiempoAlarma, pendingIntent)
                    } else {
                        // Si no puede, pedimos permiso al usuario
                        Toast.makeText(requireContext(), getString(R.string.msg_permiso_alarmas), Toast.LENGTH_LONG).show()
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    }
                } else {
                    // En versiones anteriores, simplemente la programamos
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, tiempoAlarma, pendingIntent)
                }

            } catch (e: Exception) {
                // Si algo falla al programar la notificación, lo avisamos
                Toast.makeText(requireContext(), getString(R.string.msg_error_programar_alarma), Toast.LENGTH_SHORT).show()
            }
        }

        // Guardamos la entrada en Firestore
        docRef.set(entrada)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), getString(R.string.msg_nueva_entrada), Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), getString(R.string.msg_error_guardar, it.message), Toast.LENGTH_LONG).show()
            }
    }


    /**
     * Lanza los selectores de fecha y hora para el usaurio
     * El resultado se guardar en variables internas y se actualiza visualmente
     */
    private fun mostrarSelectorFechaHora() {
        // Partimos del calendario actual o previamente seleccionado
        val calendario = calendarioSeleccionado  // Usa la fecha ya elegida

        // Lanzamos un DatePickerDialog para que el usuario seleccione la fecha
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                // Una vez seleccionada la fecha, se lanzamos un TimePickerDialog para la hora
                TimePickerDialog(
                    requireContext(),
                    { _, hour, minute ->
                        // Actualizamos el calendario con la fecha y hora seleccionadas
                        calendarioSeleccionado.set(year, month, day, hour, minute)

                        // Formateamos y guardamos la fecha en formato "yyyy-MM-dd" para Firestore
                        fechaBbdd = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendarioSeleccionado.time)
                        // Formateamos y guardamos la hora en formato "HH:mm" para Firestore
                        horaBbdd = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendarioSeleccionado.time)

                        // Actualizamos el campo visual de fecha con un formato más legible
                        binding.campoFecha.setText(
                            FechaUtils.formatearFechaHoraCompleta(
                                context = requireContext(),
                                fechaStr = fechaBbdd,
                                hora = horaBbdd
                            )
                        )
                    },
                    calendario.get(Calendar.HOUR_OF_DAY), // hora preseleccionada
                    calendario.get(Calendar.MINUTE),       // minuto preseleccionado
                    true                                   // formato 24h
                ).show()
            },
            calendario.get(Calendar.YEAR),   // año preseleccionado
            calendario.get(Calendar.MONTH),  // mes preseleccionado
            calendario.get(Calendar.DAY_OF_MONTH) // día preseleccionado
        ).show()
    }

    /**
     * Muestra los campos que se necesitan para el tipo tarea
     */
    private fun mostrarCamposTarea(){
        ocultarTodosLosCamposOpcionales()
        binding.campoFecha.visibility = View.VISIBLE
        binding.campoFecha.isEnabled = true
        binding.etDescripcion.visibility = View.GONE
        binding.layoutNotificacion.visibility = View.VISIBLE

        // Obtenemos el idioma guardado para formatear fecha y hora en su localización correcta
        val idioma = IdiomaHelper.getIdiomaGuardado(requireContext())
        val locale = Locale(idioma)

        // Si no hay fecha/hora almacenadas (entrada nueva), se asigna la actual
        if (fechaBbdd.isEmpty() || horaBbdd.isEmpty()){
            val ahora = Calendar.getInstance()
            fechaBbdd = SimpleDateFormat("yyyy-MM-dd", locale).format(ahora.time)
            horaBbdd = SimpleDateFormat("HH:mm", locale).format(ahora.time)
            calendarioSeleccionado = ahora
        } else {
            // Si ya hay fecha/hora (por ejemplo, al editar), intentamos parsearlas y mostrarlas correctamente
            try {
                val formateador = SimpleDateFormat("yyyy-MM-dd HH:mm", locale)
                calendarioSeleccionado.time = formateador.parse("$fechaBbdd $horaBbdd")!!
            } catch (e: Exception) {
                // Si ocurre un error en el parseo, usamos la fecha actual como fallback
                calendarioSeleccionado = Calendar.getInstance()
            }
        }

        binding.campoFecha.setText(
            FechaUtils.formatearFechaHoraCompleta(
                requireContext(),
                fechaBbdd,
                horaBbdd
            )
        )

        binding.campoFecha.setOnClickListener {
            mostrarSelectorFechaHora()
        }

        // Siempre se desactiva la notificación al entrar aquí (para evitar dejarla activada sin intención)
        notificar = false
        binding.iconoNotificar.setImageResource(R.drawable.ic_notificacion_desactivada)

    }

    /**
     * Muestra los campos que se necesitan para el tipo evento
     */

    private fun mostrarCamposEvento(){
        ocultarTodosLosCamposOpcionales()
        binding.campoFecha.visibility = View.VISIBLE
        binding.campoFecha.isEnabled = true
        binding.etDescripcion.visibility = View.VISIBLE
        binding.layoutNotificacion.visibility = View.VISIBLE
    }

    /**
     * Muestra los campos que se necesitan para el tipo nota
     */
    private fun mostrarCamposNota(){
        // Para las notas solo mostraremos el título y la descripción
        ocultarTodosLosCamposOpcionales()
        binding.etDescripcion.visibility = View.VISIBLE
    }

    /**
     * Muestra los campos que se necesitan para el tipo diario
     */
    private fun mostrarCamposDiario(){
        ocultarTodosLosCamposOpcionales()
        configurarSeleccionEmocion()
        binding.campoFecha.visibility = View.VISIBLE
        binding.campoFecha.isEnabled = false

        val calendario = Calendar.getInstance()
        fechaBbdd = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendario.time)
        horaBbdd = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendario.time)

        binding.campoFecha.setText(
            FechaUtils.formatearFechaHoraCompleta(
                context = requireContext(),
                fechaStr = fechaBbdd,
                hora = horaBbdd
            )
        )

        binding.etDescripcion.visibility = View.VISIBLE
        binding.contenedorEmociones.visibility = View.VISIBLE
        binding.btnSeleccionarImagen.visibility = View.VISIBLE
        binding.ivDiario.visibility = View.VISIBLE

        binding.btnSeleccionarImagen.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            imagePickerLauncher.launch(intent)
        }
    }

    /**
     * Oculta todos los campos opcionales del formulario antes de mostrar
     * únicamente los que correspondan al tipo de entrada seleccionado.
     */
    private fun ocultarTodosLosCamposOpcionales(){
        binding.campoFecha.visibility = View.GONE
        binding.contenedorEmociones.visibility = View.GONE
        binding.etTitulo.visibility = View.VISIBLE
        binding.campoFecha.isEnabled = true
        binding.btnSeleccionarImagen.visibility = View.GONE
        binding.ivDiario.visibility = View.GONE
        binding.layoutNotificacion.visibility = View.GONE
    }

    /**
     * Marcar visualmente la emoción seleccionada (para las entradas de diario)
     */
    private fun seleccionarEmocion(emocion: String){
        emocionSeleccionada = emocion

        // Cambiarlos todos a estado normal primero
        binding.emojiFeliz.alpha = 0.5f
        binding.emojiTriste.alpha = 0.5f
        binding.emojiEnfadado.alpha = 0.5f
        binding.emojiEnfermo.alpha = 0.5f

        // Resaltamos el seleccionado
        when (emocion) {
            "feliz" -> binding.emojiFeliz.alpha = 1f
            "triste" -> binding.emojiTriste.alpha = 1f
            "enfadado" -> binding.emojiEnfadado.alpha = 1f
            "enfermo" -> binding.emojiEnfermo.alpha = 1f
        }
    }

    /**
     * Configura los listeners de selección de emociones para las entradas de tipo diario
     */
    private fun configurarSeleccionEmocion(){
        binding.emojiFeliz.setOnClickListener { seleccionarEmocion("feliz") }
        binding.emojiTriste.setOnClickListener { seleccionarEmocion("triste") }
        binding.emojiEnfadado.setOnClickListener { seleccionarEmocion("enfadado") }
        binding.emojiEnfermo.setOnClickListener { seleccionarEmocion("enfermo") }
    }

    /**
     * Marcamos la emoción previamente guardada en caso de que se edite la entrada
     */
    private fun resaltarEmocionSeleccionada(emocion: String){
        binding.emojiFeliz.alpha = if (emocion == "feliz") 1f else 0.5f
        binding.emojiTriste.alpha = if (emocion == "triste") 1f else 0.5f
        binding.emojiEnfadado.alpha = if (emocion == "enfadado") 1f else 0.5f
        binding.emojiEnfermo.alpha = if (emocion == "enfermo") 1f else 0.5f

    }

}