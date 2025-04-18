package com.example.parallaxlive.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents an Instagram user
 */
@Parcelize
data class User(
    val id: String,
    val username: String,
    val fullName: String = "",
    val profilePicUrl: String = "",
    val accessToken: String
) : Parcelable {

    /**
     * Checks if the user has a valid access token
     */
    fun isAuthenticated(): Boolean {
        return accessToken.isNotBlank()
    }

    companion object {
        /**
         * Creates a mock user for testing
         */
        fun createMockUser(): User {
            return User(
                id = "12345678",
                username = "parallax_user",
                fullName = "Parallax Test User",
                profilePicUrl = "",
                accessToken = "IGQWRPZADZAkU0hqeTJ0WFlrOFZArd3BVMVZAIREZAWZA3TmxMOWtVU19yX0RqbEE5dGJCOUtiUTBidWI2aDZA"
            )
        }
    }
}