package com.example.parallaxlive.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.parallaxlive.R
import com.example.parallaxlive.utils.DatabaseHelper
import com.example.parallaxlive.utils.FirebaseAuthHelper
import kotlinx.coroutines.launch

/**
 * Welcome screen with app logo and sign-in button
 */
class WelcomeActivity : AppCompatActivity() {

    private lateinit var firebaseAuthHelper: FirebaseAuthHelper
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var credentialManager: CredentialManager

    companion object {
        private const val TAG = "WelcomeActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Initialize helpers
        firebaseAuthHelper = FirebaseAuthHelper(this)
        databaseHelper = DatabaseHelper()
        credentialManager = CredentialManager.create(this)

        // Check if already signed in
        if (firebaseAuthHelper.isUserSignedIn()) {
            navigateToConfigScreen()
            return
        }

        // Set up sign-in button
        val signInButton = findViewById<Button>(R.id.btn_sign_in)
        signInButton.text = getString(R.string.sign_in_with_google)
        signInButton.setOnClickListener {
            signIn()
        }
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly
        val currentUser = firebaseAuthHelper.getCurrentFirebaseUser()
        updateUI(currentUser != null)
    }

    private fun signIn() {
        lifecycleScope.launch {
            try {
                val request = firebaseAuthHelper.buildGoogleSignInRequest()
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@WelcomeActivity
                )

                // Handle the credential
                result.credential.let { credential ->
                    firebaseAuthHelper.handleCredential(credential) { success, error ->
                        if (success) {
                            // Save user data to database
                            databaseHelper.saveUserData()

                            // Navigate to configuration screen
                            navigateToConfigScreen()
                        } else {
                            Toast.makeText(
                                this@WelcomeActivity,
                                "Authentication failed: $error",
                                Toast.LENGTH_SHORT
                            ).show()
                            updateUI(false)
                        }
                    }
                }
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Error getting credential", e)
                Toast.makeText(
                    this@WelcomeActivity,
                    "Sign-in failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                updateUI(false)
            }
        }
    }

    private fun updateUI(isSignedIn: Boolean) {
        val signInButton = findViewById<Button>(R.id.btn_sign_in)
        signInButton.isEnabled = !isSignedIn

        if (isSignedIn) {
            navigateToConfigScreen()
        }
    }

    private fun navigateToConfigScreen() {
        val intent = Intent(this, ConfigurationActivity::class.java)
        startActivity(intent)
        finish()
    }
}