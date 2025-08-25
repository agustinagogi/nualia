# Nualia | Mi Proyecto Final de Grado

## Sobre mí y este proyecto

¡Hola! Soy Agustina Tamhara González Ginares, desarrolladora de aplicaciones móviles. Nualia es el resultado de mi Proyecto de Fin de Curso para el Grado Superior de Desarrollo de Aplicaciones Multiplataforma.

Desarrollar Nualia ha sido una experiencia increíblemente significativa para mí. Mi motivación surgió al ver la necesidad de contar con herramientas que nos ayuden a gestionar el ritmo acelerado del mundo actual. Por eso, mi objetivo era crear algo más que una simple app de tareas; quería construir un espacio seguro y acogedor que combinara la organización diaria con el bienestar emocional, una herramienta que yo misma querría usar.

Este proyecto es una demostración de mi capacidad para llevar una idea desde su concepción hasta una aplicación Android nativa, funcional y completa, aplicando las mejores prácticas de la industria.

## 🚀 ¿Qué es Nualia?

Nualia es una aplicación de bienestar personal que te ayuda a organizar tu vida con un enfoque más humano. En lugar de centrarme únicamente en la productividad, he querido crear un compañero digital que te permita:

Gestionar todo en un lugar: Crear y organizar tareas, eventos, notas y entradas de diario.

Conectar con tus emociones: Registrar tu estado de ánimo, añadir imágenes a tus reflexiones y ver cómo te has sentido a lo largo del tiempo.

Planificar con calma: Utilizar un calendario y una vista semanal con una interfaz limpia y relajante, diseñada para reducir el estrés visual.

No olvidar nada importante: Gracias a un sistema de notificaciones programadas para tus tareas y eventos.

## 🛠️ Mi Stack Tecnológico y Arquitectura

Para construir Nualia, tomé decisiones técnicas orientadas a crear una base de código limpia, escalable y mantenible, siguiendo las recomendaciones de Google.

Arquitectura: Implementé una arquitectura MVVM (Model-View-ViewModel). Esta elección fue clave para separar la lógica de negocio de la interfaz de usuario, lo que me facilitó mucho las pruebas y la reutilización de componentes. La UI observa los cambios de datos a través de LiveData, garantizando que la información esté siempre actualizada y sea consciente del ciclo de vida de la app.

Lenguaje: Desarrollé toda la aplicación en Kotlin, aprovechando su sintaxis moderna y su seguridad contra nulos para escribir un código más limpio y robusto.

Navegación: Utilicé una Single-Activity Architecture, gestionando todas las pantallas como Fragments a través del Jetpack Navigation Component. Esto me permitió crear una navegación fluida y segura entre las distintas partes de la app.

Backend como Servicio (BaaS): Me apoyé completamente en Firebase para gestionar el backend:

Firebase Firestore: Como base de datos NoSQL en tiempo real. Fue un reto interesante estructurar los datos para asegurar la privacidad del usuario, donde cada persona solo puede acceder a su propia información.

Firebase Authentication: Para implementar un flujo de autenticación seguro (registro, login y recuperación de contraseña).

Firebase Storage: Para el almacenamiento de imágenes, como las fotos de perfil y las de las entradas de diario.

Librerías Clave:

MPAndroidChart: Para la visualización de datos. Me permitió transformar las emociones registradas en gráficos circulares dinámicos y estéticamente coherentes con el diseño de la app.

Glide: Para una carga y cacheo eficiente de las imágenes desde Firebase.

AlarmManager y BroadcastReceiver: Implementar las notificaciones fue uno de los desafíos más grandes, especialmente por las restricciones de las versiones modernas de Android. Logré crear un sistema fiable que funciona incluso con la app cerrada.

🌱 Lo que aprendí y futuros pasos
Nualia me ha enseñado a enfrentarme a problemas reales de desarrollo, a investigar en la documentación oficial y a refactorizar mi propio código sin miedo. Aunque estoy muy orgullosa del resultado, siempre pienso en cómo podría mejorarla. Algunas ideas que me gustaría explorar en el futuro son:

Sincronización Multiplataforma: Crear una versión de escritorio para una experiencia continua.

Modo Offline Completo: Mejorar el almacenamiento en caché para que la app sea totalmente funcional sin conexión.

Exportación de Datos: Permitir a los usuarios exportar sus entradas a PDF, dándoles control total sobre su información.

Gracias por tomarte el tiempo de revisar mi proyecto. ¡Espero que te guste!
