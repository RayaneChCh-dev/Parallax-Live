package com.example.parallaxlive

import android.app.Application

/**
 * Main Application class for Parallax Live
 */
class ParallaxLiveApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize any app-wide configurations or libraries here
        initAppConfig()
    }

    private fun initAppConfig() {
        // This would be where you initialize any libraries or SDKs
        // For now we'll keep it simple, but in a real app you might:
        // - Set up crash reporting
        // - Initialize analytics
        // - Set up dependency injection
        // - Configure networking libraries
    }

    companion object {
        const val TAG = "ParallaxLiveApp"
    }
}