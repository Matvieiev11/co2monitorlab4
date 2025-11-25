package com.example.co2monitor.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreService {

    private val db = FirebaseFirestore.getInstance()

    private fun userId(): String? = FirebaseAuth.getInstance().currentUser?.uid

    fun userCollection() =
        userId()?.let { db.collection("users").document(it).collection("measurements") }
}