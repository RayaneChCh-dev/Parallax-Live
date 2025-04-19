package com.example.parallaxlive.utils

import android.os.Handler
import android.os.Looper
import com.example.parallaxlive.models.LiveConfig
import java.util.*
import kotlin.math.max

/**
 * Class to handle realistic viewer count behavior
 */
class ViewerCountManager(
    private val liveConfig: LiveConfig,
    private val onViewersCountUpdated: (Int) -> Unit
) {
    private var currentViewersCount = 10
    private var maxViewersReached = false
    private var handler = Handler(Looper.getMainLooper())
    private var random = Random()

    // Start time of the live stream
    private val startTime = System.currentTimeMillis()

    // Handler to run periodic updates
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateViewersCount()
            handler.postDelayed(this, 2000) // Update every 2 seconds
        }
    }

    fun start() {
        handler.post(updateRunnable)
    }

    fun stop() {
        handler.removeCallbacks(updateRunnable)
    }

    private fun updateViewersCount() {
        val elapsedMinutes = (System.currentTimeMillis() - startTime) / (1000 * 60)

        // Realistic viewer growth curve
        if (!maxViewersReached) {
            // Faster growth in beginning, slower as we approach target
            val growthRate = if (currentViewersCount < liveConfig.viewersCount * 0.3) {
                0.15 // 15% growth in early stage
            } else if (currentViewersCount < liveConfig.viewersCount * 0.7) {
                0.08 // 8% growth in middle stage
            } else {
                0.03 // 3% growth in final approach
            }

            val increment = max(1, (currentViewersCount * growthRate).toInt())
            currentViewersCount += increment

            // Cap at configured max
            if (currentViewersCount >= liveConfig.viewersCount) {
                currentViewersCount = liveConfig.viewersCount
                maxViewersReached = true
            }
        } else {
            // After peak, add random fluctuations
            if (elapsedMinutes > 5) {
                // After 5 minutes, start gradual decline
                val decline = random.nextInt(5) + 1
                currentViewersCount -= decline

                // Don't go below 50% of max
                val minViewers = (liveConfig.viewersCount * 0.5).toInt()
                if (currentViewersCount < minViewers) {
                    currentViewersCount = minViewers
                }
            } else {
                // Random fluctuations around max
                val fluctuation = random.nextInt(9) - 4 // -4 to +4
                currentViewersCount += fluctuation

                // Keep within a reasonable range
                if (currentViewersCount > liveConfig.viewersCount + 10) {
                    currentViewersCount = liveConfig.viewersCount + 10
                } else if (currentViewersCount < liveConfig.viewersCount - 10) {
                    currentViewersCount = liveConfig.viewersCount - 10
                }
            }
        }

        // Notify listener
        onViewersCountUpdated(currentViewersCount)
    }
}