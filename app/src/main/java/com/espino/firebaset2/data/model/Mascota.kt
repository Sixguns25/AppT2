package com.espino.firebaset2.data.model

import com.google.firebase.firestore.DocumentId

data class Mascota(
    @DocumentId
    var id: String = "",
    var name: String = "",
    var type: String = "", // Ej: "Perro", "Gato"
    var age: Int = 0,
    var ownerId: String = "",
    var imageBase64: String? = null // Campo para la imagen en Base64
)