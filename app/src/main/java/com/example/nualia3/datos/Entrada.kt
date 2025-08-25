package com.example.nualia3.datos

data class Entrada(
    val id: String = "",
    val usuarioId: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val tipo: String = "",
    val fecha: String = "",
    val hora: String = "",
    val emocion: String = "",
    val actualizado: Long = System.currentTimeMillis(),
    val hecho: Boolean = false,
    val imagenUrl: String = "",
    val notificar: Boolean = false // Nuevo campo

)