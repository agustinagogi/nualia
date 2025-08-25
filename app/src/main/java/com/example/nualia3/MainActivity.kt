package com.example.nualia3

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.example.nualia3.utils.IdiomaHelper
import com.google.android.material.navigation.NavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
/**
 * Actividad principal de la aplicación Nualia.
 *
 * Esta actividad se encarga de:
 * - Inicializar Firebase y AppCheck.
 * - Gestionar el idioma guardado del usuario.
 * - Configurar la navegación con DrawerLayout.
 * - Mostrar u ocultar el menú lateral dependiendo del destino.
 * - Pedir permisos para notificaciones (Android 13+).
 * - Cargar y mostrar los datos del usuario autenticado en el menú lateral.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController
    private lateinit var navigationView: NavigationView
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var authListener: FirebaseAuth.AuthStateListener

    /**
     * Ponemos el idioma guardado antes de crear la actividad.
     */
    override fun attachBaseContext(newBase: Context) {
        val idioma = IdiomaHelper.getIdiomaGuardado(newBase)
        val context = IdiomaHelper.establecerIdioma(newBase, idioma)
        super.attachBaseContext(context)
    }

    /**
     * Cargamos la interfaz, inicializamos Firebase y configuramos la navegación.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Nualia3) // ← importante para mostrar bien el splash
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializamos Firebase
        FirebaseApp.initializeApp(this)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        // Configuramos menú lateral
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Configuramos navegación con fragments
        navController = findNavController(R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.fragmentListaDiario,
                R.id.fragmentVistaSemanal,
                R.id.fragmentEstadisticas,
                R.id.fragmentAjustes,
                R.id.fragmentListaNotas
            ),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navigationView.setupWithNavController(navController)

        // Ocultamos o mostramos el menú lateral según el destino
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment, R.id.fragmentRegistro -> {
                    supportActionBar?.hide()
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                }
                else -> {
                    supportActionBar?.show()
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                }
            }
        }

        // Pedimos permiso para notificaciones en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        // Escuchamos el estado de autenticación
        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val usuarioActual = firebaseAuth.currentUser
            val headerView = navigationView.getHeaderView(0)
            val tvNombreUsuario = headerView.findViewById<TextView>(R.id.tvNombreUsuario)
            val tvUserEmail = headerView.findViewById<TextView>(R.id.tvEmail)
            val imgPerfil = headerView.findViewById<ImageView>(R.id.imgUsuario)

            if (usuarioActual != null) {
                tvUserEmail.text = usuarioActual.email

                // Cargamos nombre y foto del cache
                val prefs = getSharedPreferences("usuario", Context.MODE_PRIVATE)
                val nombreCache =
                    prefs.getString("nombre", getString(R.string.nombre_no_disponible))
                val fotoCache = prefs.getString("fotoUrl", "")

                tvNombreUsuario.text = nombreCache
                Glide.with(this)
                    .load(if (!fotoCache.isNullOrEmpty()) fotoCache else R.drawable.usuario_por_defecto)
                    .circleCrop()
                    .into(imgPerfil)

                // Actualizamos en segundo plano
                val uid = usuarioActual.uid
                FirebaseFirestore.getInstance().collection("usuarios_datos").document(uid)
                    .get()
                    .addOnSuccessListener { doc ->
                        val nombre =
                            doc.getString("nombre") ?: getString(R.string.nombre_no_disponible)
                        val fotoUrl = doc.getString("fotoUrl") ?: ""

                        tvNombreUsuario.text = nombre
                        Glide.with(this)
                            .load(if (fotoUrl.isNotEmpty()) fotoUrl else R.drawable.usuario_por_defecto)
                            .circleCrop()
                            .into(imgPerfil)

                        prefs.edit().apply {
                            putString("nombre", nombre)
                            putString("fotoUrl", fotoUrl)
                            apply()
                        }
                    }
            } else {
                tvNombreUsuario.text = getString(R.string.usuario_no_autenticado)
                tvUserEmail.text = ""
                imgPerfil.setImageResource(R.drawable.usuario_por_defecto)

                // 🚨 Redirigimos al login si no hay usuario autenticado
                val destinoActual = navController.currentDestination?.id
                if (destinoActual != R.id.loginFragment && destinoActual != R.id.fragmentRegistro) {
                    navController.navigate(R.id.loginFragment)
                }
            }
        }
    }

    /**
     * Activamos el listener de autenticación al iniciar.
     */
    override fun onStart() {
        super.onStart()
        FirebaseAuth.getInstance().addAuthStateListener(authListener)
    }

    /**
     * Quitamos el listener de autenticación al parar.
     */
    override fun onStop() {
        super.onStop()
        FirebaseAuth.getInstance().removeAuthStateListener(authListener)
    }

    /**
     * Permitimos navegación al pulsar la flecha de volver.
     */
    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }

    /**
     * Inflamos el menú superior.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return true
    }

    /**
     * Gestionamos las opciones del menú superior.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_cerrarSesion -> {
                val viewModel = androidx.lifecycle.ViewModelProvider(this)[com.example.nualia3.ui.home.HomeViewModel::class.java]
                viewModel.limpiarListeners()

                FirebaseAuth.getInstance().signOut()
                navController.navigate(R.id.loginFragment)
                true
            }
            R.id.action_busqueda -> {
                navController.navigate(R.id.fragmentBuscar)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Método auxiliar para volver a cargar los datos del usuario en el menú lateral.
     */
    fun cargarDatosUsuarioEnDrawer() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val headerView = navigationView.getHeaderView(0)
        val tvNombreUsuario = headerView.findViewById<TextView>(R.id.tvNombreUsuario)
        val tvUserEmail = headerView.findViewById<TextView>(R.id.tvEmail)
        val imgPerfil = headerView.findViewById<ImageView>(R.id.imgUsuario)

        db.collection("usuarios_datos").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val nombre = snapshot.getString("nombre") ?: "Usuario"
                val email = snapshot.getString("email") ?: ""
                val fotoUrl = snapshot.getString("fotoUrl") ?: ""

                tvNombreUsuario.text = nombre
                tvUserEmail.text = email

                if (fotoUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(fotoUrl)
                        .placeholder(R.drawable.usuario_por_defecto)
                        .circleCrop()
                        .into(imgPerfil)
                } else {
                    imgPerfil.setImageResource(R.drawable.usuario_por_defecto)
                }

                // Guardamos en caché local
                getSharedPreferences("usuario", Context.MODE_PRIVATE).edit().apply {
                    putString("nombre", nombre)
                    putString("fotoUrl", fotoUrl)
                    apply()
                }
            }
    }

}
