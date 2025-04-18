package com.example.parallaxlive.utils

import android.util.Log
import com.example.parallaxlive.models.LiveConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

/**
 * Helper class to work with Firebase Realtime Database
 */
class DatabaseHelper {

    private val database = Firebase.database
    private val auth = FirebaseAuth.getInstance()
    private val usersRef: DatabaseReference = database.getReference("users")
    private val liveConfigsRef: DatabaseReference = database.getReference("live_configs")

    companion object {
        private const val TAG = "DatabaseHelper"
    }

    /**
     * Save user data to database after authentication
     */
    fun saveUserData() {
        val currentUser = auth.currentUser ?: return

        val userData = hashMapOf(
            "uid" to currentUser.uid,
            "email" to currentUser.email,
            "displayName" to currentUser.displayName,
            "photoUrl" to (currentUser.photoUrl?.toString() ?: ""),
            "lastLogin" to System.currentTimeMillis()
        )

        usersRef.child(currentUser.uid).setValue(userData)
            .addOnSuccessListener {
                Log.d(TAG, "User data saved successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving user data", e)
            }
    }

    /**
     * Save live configuration for the current user
     */
    fun saveLiveConfig(liveConfig: LiveConfig, onComplete: (Boolean) -> Unit) {
        val currentUser = auth.currentUser ?: return onComplete(false)

        val configData = hashMapOf(
            "userId" to currentUser.uid,
            "viewersCount" to liveConfig.viewersCount,
            "messageType" to liveConfig.messageType.toString(),
            "customMessage" to liveConfig.customMessage,
            "timestamp" to System.currentTimeMillis()
        )

        liveConfigsRef.child(currentUser.uid).setValue(configData)
            .addOnSuccessListener {
                Log.d(TAG, "Live config saved successfully")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving live config", e)
                onComplete(false)
            }
    }

    /**
     * Get the last live configuration for the current user
     */
    fun getLastLiveConfig(onComplete: (LiveConfig?) -> Unit) {
        val currentUser = auth.currentUser ?: return onComplete(null)

        liveConfigsRef.child(currentUser.uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    try {
                        val viewersCount = snapshot.child("viewersCount").getValue(Int::class.java) ?: 100
                        val messageTypeStr = snapshot.child("messageType").getValue(String::class.java) ?: "POSITIVE"
                        val customMessage = snapshot.child("customMessage").getValue(String::class.java) ?: ""

                        val messageType = try {
                            LiveConfig.MessageType.valueOf(messageTypeStr)
                        } catch (e: Exception) {
                            LiveConfig.MessageType.POSITIVE
                        }

                        val config = LiveConfig(
                            viewersCount = viewersCount,
                            messageType = messageType,
                            customMessage = customMessage
                        )

                        onComplete(config)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing live config", e)
                        onComplete(null)
                    }
                } else {
                    onComplete(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error getting live config", error.toException())
                onComplete(null)
            }
        })
    }
}