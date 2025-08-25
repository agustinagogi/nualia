package com.example.nualia3.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.nualia3.datos.Entrada
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ViewModel para el home
 *
 * Este ViewModel se encarga de:
 * - Cargar entradas de Firestore por fecha o tipo
 * - Escuchar cambios en tiempo real
 * - Filtrar entradas por texto
 * - Gestionar el estado de autenticación
 * - Cancelar los listeners cuando el usuario cierra sesión
 */
class HomeViewModel : ViewModel() {

    // Instancia de Firestore para acceder a la bbdd
    private val firestore = FirebaseFirestore.getInstance()

    // Instancia de autenticación para obtener el usuario actual
    private val auth = FirebaseAuth.getInstance()

    // Lista observable de entradas filtradas o por fecha
    private val _entradasLiveData = MutableLiveData<List<Entrada>>()
    val entradasLiveData: LiveData<List<Entrada>> get() = _entradasLiveData

    // Estado de autenticación del usuario (true si está autenticadio, false si no)
    private val _authState = MutableLiveData<Boolean>(true)
    val authState: LiveData<Boolean> get() = _authState

    // Fecha seleccionada por el usuario (vista calendario)
    private val _fechaSeleccionada = MutableLiveData<java.time.LocalDate>(java.time.LocalDate.now())
    val fechaSeleccionada: LiveData<java.time.LocalDate> get() = _fechaSeleccionada

    // Día seleccionado para vista semanal
    private val _diaSemSelec = MutableLiveData<java.time.LocalDate>(java.time.LocalDate.now())
    val diaSemSelec: LiveData<java.time.LocalDate> get() = _diaSemSelec

    // Lista completa de entradas en memoria (usada para búsquedas
    private var _todasLasEntradas: List<Entrada>? = null

    // Creamos una lista donde vamos a ir guardando todos los listeners que ponemos en Firestore
    // Esto nos sirve para poder cancelarlos más adelante y evitar que sigan escuchando cuando ya no hace falta
    private var listenersActivos = mutableListOf<ListenerRegistration>()

    // Texto actual del campo de búsqueda (vinculado a LiveData)
    val textoBusqueda = MutableLiveData("")

    /**
     * Establece una nueva fecha seleccionada y carga las entradas correspondientes
     * Limpia la lista anterior para evitar parpadeos
     *
     * @param fecha Fecha seleccionada
     */
    fun selecFecha(fecha: LocalDate){
        _fechaSeleccionada.value = fecha
        _entradasLiveData.postValue(emptyList())
        cargarEntradasPorFecha(fecha)
    }

    /**
     * Establece el día seleccionado para la vista semanal
     *
     * @param fecha Día a seleccionar
     */
    fun selecDiaSemana(fecha: LocalDate){
        _diaSemSelec.value = fecha
    }

    /**
     * Cargar las entradas del usuario correspondientes a una fecha concreta
     * @param fecha Fecha para filtrar (en formato LocalDate
     */
    fun cargarEntradasPorFecha(fecha: LocalDate) {
        // Sacamos el ID del usuario que esté logueado
        val usuarioId = auth.currentUser?.uid

        // Si no hay usuario (porque cerró sesión o es null), limpiamos y salimos
        if (usuarioId == null) {
            _authState.postValue(false)  // Avisamos al resto de la app que no hay sesión
            _entradasLiveData.postValue(emptyList())  // Vaciamos la lista de entradas
            return
        }

        // Formateamos la fecha que recibimos (LocalDate) a texto tipo "2025-06-08"
        val stringFecha = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(fecha)

        // Creamos una consulta a Firestore con filtros por fecha y por usuario
        val listener = firestore.collection("usuarios_datos")
            .document(usuarioId)               // Buscamos en los datos del usuario actual
            .collection("entradas")            // Accedemos a todas sus entradas
            .whereEqualTo("fecha", stringFecha) // Solo queremos las que tengan esa fecha exacta
            .whereEqualTo("usuarioId", usuarioId) // Y que sean suyas (por seguridad)
            .addSnapshotListener { snapshot, error ->

                // Si hay un error (por red, permisos, etc.), vaciamos la lista
                if (error != null) {
                    _entradasLiveData.postValue(emptyList())
                    return@addSnapshotListener
                }

                // Si todo fue bien, convertimos los documentos de Firestore a objetos Entrada
                val entradas = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Entrada::class.java)?.copy(id = doc.id) // Añadimos el ID real
                }?.sortedByDescending { it.actualizado }  // Ordenamos por fecha de modificación (más reciente primero)
                    ?: emptyList()

                // Actualizamos el LiveData que está conectado a la interfaz
                _entradasLiveData.postValue(entradas)
            }

        // Guardamos este listener por si más adelante hay que cancelarlo (por ejemplo al cerrar sesión)
        listenersActivos.add(listener)
    }

    /**
     * Elimina una entrada del usuario
     *
     * @param entradaId ID de la entrada a eliminar
     */
    fun eliminarEntrada(entradaId: String){
        val usuarioId = auth.currentUser?.uid
        if (usuarioId == null) {
            _authState.postValue(false)
            return
        }

        firestore.collection("usuarios_datos")
            .document(usuarioId)
            .collection("entradas")
            .document(entradaId)
            .delete()
    }

    /**
     * Carga solo las entradas de tipo "diario"
     */
    fun cargarTodosLosDiarios() {
        // Obtenemos el ID del usuario que ha iniciado sesión
        val usuarioId = auth.currentUser?.uid

        // Si no hay usuario (por ejemplo, ya cerró sesión), avisamos y salimos
        if (usuarioId == null) {
            _authState.postValue(false)  // Indicamos que ya no está autenticado
            _entradasLiveData.postValue(emptyList())  // Limpiamos la lista de entradas
            return
        }

        // Creamos una consulta a Firestore que escucha solo entradas tipo "diario"
        val listener = firestore.collection("usuarios_datos")
            .document(usuarioId)                    // Entramos en el documento del usuario
            .collection("entradas")                 // Accedemos a sus entradas
            .whereEqualTo("tipo", "diario")         // Filtramos solo las de tipo diario
            .whereEqualTo("usuarioId", usuarioId)   // Nos aseguramos que sean suyas
            .addSnapshotListener { snapshot, error ->

                // Si hubo un error al consultar, devolvemos lista vacía y salimos
                if (error != null) {
                    _entradasLiveData.postValue(emptyList())
                    return@addSnapshotListener
                }

                // Convertimos los documentos de Firestore a objetos Entrada y los ordenamos
                val diarios = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Entrada::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                // Actualizamos la lista observable con los diarios encontrados
                _entradasLiveData.postValue(diarios)
            }

        // Guardamos este listener para poder cancelarlo más adelante con limpiarListeners()
        listenersActivos.add(listener)
    }

    /**
     * Función que escucha una entrada específica en Firestore en tiempo real.
     *
     * Se usa cuando queremos que una pantalla como el detalle de una entrada
     * se actualice automáticamente si alguien edita la entrada desde otro lugar.
     *
     * @param entradaId ID del documento de entrada en Firestore
     * @param callback Función que se llama cuando recibimos la entrada (o null si falla)
     */
    fun cargarEntradaPorIdTiempoReal(entradaId: String, callback: (Entrada?) -> Unit) {
        // Cogemos el ID del usuario actual (debe estar logueado)
        val usuarioId = auth.currentUser?.uid

        // Si no hay usuario logueado, devolvemos null y notificamos que no hay sesión
        if (usuarioId == null) {
            _authState.postValue(false) // Marcamos que no hay usuario
            callback(null)              // Devolvemos null como resultado
            return                      // Salimos de la función
        }

        // Creamos un listener que se queda escuchando los cambios de ese documento en Firestore
        val listener = firestore.collection("usuarios_datos")    // Colección raíz
            .document(usuarioId)                                 // Documento del usuario actual
            .collection("entradas")                              // Subcolección de entradas
            .document(entradaId)                                 // Documento específico a escuchar
            .addSnapshotListener { snapshot, error ->            // Este bloque se ejecuta si hay cambios
                // Si hay error o no hay datos o el documento no existe, devolvemos null
                if (error != null || snapshot == null || !snapshot.exists()) {
                    callback(null)
                    return@addSnapshotListener // Salimos del listener
                }

                // Si todo va bien, convertimos el snapshot en un objeto Entrada
                val entrada = snapshot.toObject(Entrada::class.java)?.copy(id = snapshot.id)
                callback(entrada) // Llamamos al callback con la entrada obtenida
            }

        // Guardamos el listener en una lista (por ejemplo, para poder cancelarlo luego)
        listenersActivos.add(listener)
    }

    /**
     * Establece el texto de búsqueda y aplica el filtrado
     *
     * @param query Texto a buscar
     */
    fun establecerTextoBusqueda (query: String) {
        textoBusqueda.value = query
        filtrarEntradas(query)
    }

    /**
     * Filtra la lista completa de entradas locales en función del texto de búsqueda
     *
     * @param query Texto que debe estar presente en el título o descripción
     */

    fun filtrarEntradas(query: String){
        val todasEntradas = _todasLasEntradas ?: return
        val filtradas = if (query.isBlank()) todasEntradas else todasEntradas.filter {
            it.titulo.contains(query, ignoreCase = true) || it.descripcion.contains(query, ignoreCase = true)
        }
        _entradasLiveData.postValue(filtradas)
    }

    /**
     * Carga todas las entradas del usuario actual en tiempo real desde Firestore.
     *
     * Guarda una copia local en `_todasLasEntradas` para poder aplicar filtros rápidamente
     * sin tener que volver a consultar la base de datos.
     *
     * Cada vez que se detecta un cambio en Firestore (añadir, borrar o modificar una entrada),
     * el listener se activa y actualiza automáticamente la lista filtrada.
     *
     * También guarda el listener en `listenersActivos` para poder eliminarlo luego si es necesario.
     */
    fun cargarTodasEntradas() {
        // Cogemos el ID del usuario actual
        val usuarioId = auth.currentUser?.uid ?: return // Si no hay usuario, salimos directamente

        // Creamos un listener en tiempo real a la colección de entradas del usuario
        val listener = firestore.collection("usuarios_datos")   // Colección principal
            .document(usuarioId)                                // Documento del usuario actual
            .collection("entradas")                             // Subcolección de entradas
            .addSnapshotListener { snapshot, error ->           // Este bloque se ejecuta cada vez que cambia algo
                // Si ocurre un error, salimos sin hacer nada
                if (error != null) return@addSnapshotListener

                // Convertimos los documentos de Firestore a objetos Entrada y los guardamos en memoria
                _todasLasEntradas = snapshot?.documents?.mapNotNull {
                    it.toObject(Entrada::class.java)?.copy(id = it.id)
                }

                // Aplicamos el filtro actual (si había texto de búsqueda)
                filtrarEntradas(textoBusqueda.value ?: "")
            }

        // Guardamos este listener en una lista para poder quitarlo más tarde (por ejemplo, al cerrar la app)
        listenersActivos.add(listener)
    }

    /**
     * Elimina todos los listeners activos de Firestore cuando el usuario cierra sesión.
     */
    fun limpiarListeners() {
        listenersActivos.forEach { it.remove() }
        listenersActivos.clear()
    }
}