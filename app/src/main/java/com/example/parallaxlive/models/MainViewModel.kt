package com.example.parallaxlive.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parallaxlive.models.LiveConfig
import com.example.parallaxlive.utils.ClaudeRepository
import kotlinx.coroutines.launch

class MainViewModel(private val repository: ClaudeRepository) : ViewModel() {
    val commentLiveData: MutableLiveData<String> = MutableLiveData()
    val errorLiveData: MutableLiveData<String> = MutableLiveData()

    fun generateComment(config: LiveConfig) {
        viewModelScope.launch {
            try {
                val generatedMessage = repository.generateMessage(config)
                commentLiveData.value = generatedMessage
            } catch (e: Exception) {
                errorLiveData.value = "Error generating comment: ${e.message}"
            }
        }
    }
}