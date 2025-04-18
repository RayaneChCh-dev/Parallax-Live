package com.example.parallaxlive.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import com.example.parallaxlive.R
import com.example.parallaxlive.utils.FirebaseAuthHelper

/**
 * Authentication Activity
 *
 * Note: This activity is kept for backward compatibility,
 * but in the new Firebase implementation, authentication happens
 * directly in WelcomeActivity using Credential Manager.
 */
class AuthActivity : AppCompatActivity() {

    private lateinit var firebaseAuthHelper: FirebaseAuthHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth Helper
        firebaseAuthHelper = FirebaseAuthHelper(this)

        // Handle intent data if coming from authentication redirect
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        // If authentication is needed, redirect to WelcomeActivity
        // where the Firebase authentication flow is implemented
        val welcomeIntent = Intent(this, WelcomeActivity::class.java)
        startActivity(welcomeIntent)
        finish()
    }
}