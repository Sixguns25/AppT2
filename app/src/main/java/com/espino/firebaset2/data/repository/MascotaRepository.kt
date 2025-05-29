package com.espino.firebaset2.data.repository

import com.espino.firebaset2.data.model.Mascota
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class MascotaRepository(private val firestore: FirebaseFirestore) {

    // pets: coleccion en FireSTore
    private val petsCollection = firestore.collection("pets")

    /**
     * Añade una nueva mascota a Firestore.
     * @param pet El objeto Pet a añadir.
     * @return Un objeto Result<Boolean> que indica éxito o fracaso.
     */
    suspend fun addPet(pet: Mascota): Result<Boolean> {
        return try {
            // Firestore generará automáticamente un ID para el documento
            petsCollection.add(pet).await()
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Obtiene una lista de mascotas para un propietario específico en tiempo real.
     * Utiliza un Flow para emitir actualizaciones cada vez que los datos cambian en Firestore.
     *
     * @param ownerId El ID del propietario de las mascotas.
     * @return Un Flow de Result<List<Pet>>.
     */
    fun getPetsForOwner(ownerId: String): Flow<Result<List<Mascota>>> = callbackFlow {
        val subscription = petsCollection
            .whereEqualTo("ownerId", ownerId)
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(Result.failure(e))
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val pets = snapshot.documents.mapNotNull { document ->
                        document.toObject(Mascota::class.java)?.apply {
                            this.id = document.id
                        }
                    }
                    trySend(Result.success(pets))
                } else {
                    // Maneja el caso de lista vacía
                    trySend(Result.success(emptyList()))
                }
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Actualiza una mascota existente en Firestore.
     * @param pet El objeto Pet con los datos actualizados (el ID debe coincidir con un documento existente).
     * @return Un objeto Result<Boolean> que indica éxito o fracaso.
     */
    suspend fun updatePet(pet: Mascota): Result<Boolean> {
        return try {
            // Actualiza el documento usando el ID de la mascota
            petsCollection.document(pet.id).set(pet).await()
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Elimina una mascota de Firestore.
     * @param petId El ID de la mascota a eliminar.
     * @return Un objeto Result<Boolean> que indica éxito o fracaso.
     */
    suspend fun deletePet(petId: String): Result<Boolean> {
        return try {
            petsCollection.document(petId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

}