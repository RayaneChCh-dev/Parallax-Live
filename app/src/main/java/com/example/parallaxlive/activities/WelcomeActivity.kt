package com.example.parallaxlive.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.parallaxlive.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.ConnectionResult
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

class WelcomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val TAG = "WelcomeActivity"

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d(TAG, "Sign-in result: ${result.resultCode}, data: ${result.data}")
            // Handle the result of the sign-in
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                // Google Sign-In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign-In failed

                Log.w(TAG, "Google sign in failed", e)
                Toast.makeText(this, "Sign-in failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.w(TAG, "Sign-in cancelled with result code: ${result.resultCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val signInButton = findViewById<Button>(R.id.btn_sign_in)
        signInButton.setOnClickListener {
            signIn()
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser // authentifié ?
        if (currentUser != null) {
            navigateToConfigScreen() // Si oui, -> ConfigurationActivity.kt
        }
    }

    private fun signIn() {
        googleSignInClient.signOut().addOnCompleteListener {
            try {
                // instances & codes, ...
                val googleApiAvailability = GoogleApiAvailability.getInstance()
                val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
                if (resultCode != ConnectionResult.SUCCESS) {
                    Log.e(TAG, "Google Play Services not available: $resultCode")
                    googleApiAvailability.getErrorDialog(this, resultCode, 1000)?.show()
                    return@addOnCompleteListener
                }
                val signInIntent = googleSignInClient.signInIntent
                signInLauncher.launch(signInIntent)

            } catch (e: Exception) {
                Log.e(TAG, "Error starting sign-in flow: ${e.message}")
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    navigateToConfigScreen()
                    userData2DB(user)
                } else {
                    //erreur
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun userData2DB(user: FirebaseUser?) {
        if (user == null) {
            Log.w(TAG, "null user, can't save to BDD")
            return
        }

        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("users")

        val userData = HashMap<String, Any>()
        userData["uid"] = user.uid
        userData["displayName"] = user.displayName ?: ""
        userData["email"] = user.email ?: ""
        userData["lastLogin"] = ServerValue.TIMESTAMP

        usersRef.child(user.uid)
            .setValue(userData)
            .addOnSuccessListener {
                Log.d(TAG, "User ${user.displayName} (${user.uid}) success BDD save")
            }
            .addOnFailureListener { e ->
                Log.e(TAG,"Failed", e)
                Toast.makeText(this, "FailSave: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToConfigScreen() {
        val intent = Intent(this, ConfigurationActivity::class.java)
        startActivity(intent)
        finish()
    }
}