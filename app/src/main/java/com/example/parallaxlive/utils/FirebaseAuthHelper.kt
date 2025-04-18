package com.example.parallaxlive.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialException
import com.example.parallaxlive.R
import com.example.parallaxlive.models.User
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * Helper class for Firebase authentication with Google
 */
class FirebaseAuthHelper(private val context: Context) {

    private val auth: FirebaseAuth = Firebase.auth
    private val credentialManager = CredentialManager.create(context)

    companion object {
        private const val TAG = "FirebaseAuthHelper"
        const val TYPE_GOOGLE_ID_TOKEN_CREDENTIAL = "com.google.android.libraries.identity.googleid.GOOGLE_ID_TOKEN_CREDENTIAL"
    }

    /**
     * Check if a user is already signed in
     */
    fun isUserSignedIn(): Boolean {
        return auth.currentUser != null
    }

    /**
     * Get the current Firebase user
     */
    fun getCurrentFirebaseUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * Get Google Sign-In credential request
     */
    fun buildGoogleSignInRequest(): GetCredentialRequest {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // <-- Allow all Google accounts
            .setServerClientId(context.getString(R.string.google_client_id))
            .build()

        return GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
    }

    /**
     * Handle Google credential
     */
    fun handleCredential(credential: Credential, onComplete: (Boolean, String?) -> Unit) {
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                firebaseAuthWithGoogle(googleIdTokenCredential.idToken, onComplete)
            } catch (e: GoogleIdTokenParsingException) {
                Log.w(TAG, "Error parsing Google ID token", e)
                onComplete(false, "Error parsing Google ID token: ${e.message}")
            }
        } else {
            Log.w(TAG, "Credential is not of type Google ID!")
            onComplete(false, "Unsupported credential type")
        }
    }

    /**
     * Authenticate with Firebase using Google ID token
     */
    private fun firebaseAuthWithGoogle(idToken: String, onComplete: (Boolean, String?) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Log.d(TAG, "signInWithCredential:success")
                    onComplete(true, null)
                } else {
                    // Sign in failed
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    onComplete(false, task.exception?.message)
                }
            }
    }

    /**
     * Convert FirebaseUser to app User model
     */
    fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null

        return User(
            id = firebaseUser.uid,
            username = firebaseUser.displayName ?: "",
            fullName = firebaseUser.displayName ?: "",
            profilePicUrl = firebaseUser.photoUrl?.toString() ?: "",
            accessToken = "" // Not needed with Firebase Auth
        )
    }

    /**
     * Get username from current user
     */
    fun getUsername(): String {
        return auth.currentUser?.displayName ?: "User"
    }

    /**
     * Sign out the current user
     */
    suspend fun signOut() {
        try {
            // Sign out from Firebase
            auth.signOut()

            // Clear credentials
            val clearRequest = ClearCredentialStateRequest()
            credentialManager.clearCredentialState(clearRequest)
        } catch (e: ClearCredentialException) {
            Log.e(TAG, "Couldn't clear user credentials: ${e.localizedMessage}")
        }
    }
}