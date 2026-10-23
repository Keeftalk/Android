package tn.keeftalk.chat.extension.registry

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tn.keeftalk.chat.extension.model.DeclarativeCard

class FeedCardRegistry {
    private val cardsMap = mutableMapOf<String, DeclarativeCard>()
    private val _installedFeedCards = MutableStateFlow<List<DeclarativeCard>>(emptyList())
    val installedFeedCards: StateFlow<List<DeclarativeCard>> = _installedFeedCards.asStateFlow()

    fun registerCard(card: DeclarativeCard) {
        cardsMap[card.id] = card
        _installedFeedCards.value = cardsMap.values.toList()
    }

    fun unregisterCard(cardId: String) {
        cardsMap.remove(cardId)
        _installedFeedCards.value = cardsMap.values.toList()
    }
}
