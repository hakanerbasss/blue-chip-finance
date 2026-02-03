package com.bluechip.finance

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    // Haber listesini ve yüklenme durumunu tutan değişkenler
    val newsList = mutableStateOf<List<Article>>(emptyList())
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    // Az önce oluşturduğumuz servisi çağırıyoruz
    private val apiService = NewsApiService.create()

    init {
        fetchNews()
    }

    // Haberleri çeken fonksiyon
    fun fetchNews() {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            try {
                val response = apiService.getNews()
                newsList.value = response.articles
            } catch (e: Exception) {
                errorMessage.value = "Hata oluştu: ${e.localizedMessage}"
                e.printStackTrace()
            } finally {
                isLoading.value = false
            }
        }
    }
}
