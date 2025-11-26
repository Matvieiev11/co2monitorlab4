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

        val id = "${data.deviceId}_${data.timestamp}"

        userCollection()
            .document(id)
            .set(data)
            .await()
    }

    suspend fun downloadAll(): List<SensorData> {
        if (!requireUser()) return emptyList()

        val snapshot = userCollection().get().await()
        return snapshot.toObjects(SensorData::class.java)
    }

    suspend fun deleteOldCloudData() {
        if (!requireUser()) return

        val threshold = System.currentTimeMillis() - 24L * 60 * 60 * 1000

        val docs = userCollection().get().await()

        val batch = firestore.batch()

        for (doc in docs.documents) {
            val data = doc.toObject(SensorData::class.java) ?: continue

            if (data.timestamp < threshold) {
                batch.delete(doc.reference)
            }
        }

        batch.commit().await()
    }

    suspend fun replaceAll(list: List<SensorData>) {
        clearCloud()
        uploadAll(list)
    }

    suspend fun uploadAll(list: List<SensorData>) {
        if (!requireUser()) return

        val batch = firestore.batch()

        list.forEach { data ->
            val id = "${data.deviceId}_${data.timestamp}"
            batch.set(userCollection().document(id), data)
        }
        batch.commit().await()
    }

    suspend fun clearCloud() {
        if (!requireUser()) return

        val snapshot = userCollection().get().await()
        val batch = firestore.batch()

        for (doc in snapshot.documents) {
            batch.delete(doc.reference)
        }

        batch.commit().await()
    }
}

