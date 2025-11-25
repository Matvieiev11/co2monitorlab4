package com.example.co2monitor.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private fun userId(): String? = auth.currentUser?.uid

    private fun userCollection() =
        firestore.collection("users")
            .document(userId()!!)
            .collection("sensor_data")

    private fun requireUser(): Boolean = userId() != null

    suspend fun uploadData(data: SensorData) {
        if (!requireUser()) return
        userCollection()
            .document(data.timestamp.toString())
            .set(data)
            .await()
    }

    suspend fun downloadAll(): List<SensorData> {
        if (!requireUser()) return emptyList()
        val snapshot = userCollection().get().await()
        return snapshot.toObjects(SensorData::class.java)
    }

    suspend fun clearCloud() {
        if (!requireUser()) return
        val batch = firestore.batch()
        val docs = userCollection().get().await()

        for (doc in docs.documents) {
            batch.delete(doc.reference)
        }
        batch.commit().await()
    }

    suspend fun uploadAll(list: List<SensorData>) {
        if (!requireUser()) return
        val batch = firestore.batch()

        list.forEach { data ->
            val ref = userCollection().document(data.timestamp.toString())
            batch.set(ref, data)
        }
        batch.commit().await()
    }

    suspend fun replaceAll(list: List<SensorData>) {
        clearCloud()
        uploadAll(list)
    }
}
