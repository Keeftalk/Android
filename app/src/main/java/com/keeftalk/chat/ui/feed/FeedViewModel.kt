package com.keeftalk.chat.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.data.prefs.UserPreferencesRepository
import com.keeftalk.chat.domain.model.FeedArticle
import com.keeftalk.chat.domain.model.FeedSource
import com.keeftalk.chat.domain.model.WeatherInfo
import com.keeftalk.chat.domain.repository.FeedRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FeedViewModel(
    private val feedRepository: FeedRepository,
    private val userPrefsRepository: UserPreferencesRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow("For You")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val sources: StateFlow<List<FeedSource>> = feedRepository.getSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val articles: StateFlow<List<FeedArticle>> = feedRepository.getArticles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredArticles: StateFlow<List<FeedArticle>> = combine(articles, _selectedCategory) { articles, category ->
        if (category == "For You") articles
        else articles.filter { it.category == category }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = articles.map { list ->
        val cats = list.mapNotNull { it.category }.distinct().sorted()
        listOf("For You") + cats
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("For You"))

    val weather: StateFlow<WeatherInfo?> = feedRepository.getWeather()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val weatherLocationMode: StateFlow<String> = userPrefsRepository.userPreferencesFlow
        .map { it.weatherLocationMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "AUTO")

    val weatherManualLocation: StateFlow<String?> = userPrefsRepository.userPreferencesFlow
        .map { it.weatherManualLocation }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedArticle = MutableStateFlow<FeedArticle?>(null)
    val selectedArticle: StateFlow<FeedArticle?> = _selectedArticle.asStateFlow()

    private val _isArticleLoading = MutableStateFlow(false)
    val isArticleLoading: StateFlow<Boolean> = _isArticleLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        refresh()
        
        // Prefetch top articles when they change
        viewModelScope.launch {
            filteredArticles.collectLatest { articles ->
                if (articles.isNotEmpty()) {
                    val toPrefetch = articles.take(15).map { it.id }
                    feedRepository.prefetchArticles(toPrefetch)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            feedRepository.refreshAll()
            feedRepository.refreshWeather()
            _isRefreshing.value = false
        }
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setWeatherMode(mode: String) {
        viewModelScope.launch {
            userPrefsRepository.updateWeatherSettings(mode = mode)
            feedRepository.refreshWeather()
        }
    }

    fun setManualLocation(location: String) {
        viewModelScope.launch {
            userPrefsRepository.updateWeatherSettings(location = location)
            feedRepository.refreshWeather()
        }
    }

    fun addCuratedSources(feeds: List<com.keeftalk.chat.domain.model.CuratedFeed>) {
        viewModelScope.launch {
            feedRepository.addCuratedSources(feeds)
        }
    }

    fun addSource(url: String) {
        viewModelScope.launch {
            feedRepository.addSource(url)
        }
    }

    fun removeSource(sourceId: String) {
        viewModelScope.launch {
            feedRepository.removeSource(sourceId)
        }
    }

    fun selectArticle(article: FeedArticle?) {
        if (article == null) {
            _selectedArticle.value = null
            return
        }
        
        viewModelScope.launch {
            _selectedArticle.value = article
            _isArticleLoading.value = true
            val fullArticle = feedRepository.getFullArticle(article.id)
            if (fullArticle != null) {
                _selectedArticle.value = fullArticle
            }
            _isArticleLoading.value = false
        }
    }
}
