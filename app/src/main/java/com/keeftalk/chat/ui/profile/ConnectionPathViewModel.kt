package com.keeftalk.chat.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.domain.model.Profile
import com.keeftalk.chat.domain.repository.AuthRepository
import com.keeftalk.chat.domain.repository.ChatRepository
import com.keeftalk.chat.domain.repository.RelationshipRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.*

class ConnectionPathViewModel(
    private val relationshipRepository: RelationshipRepository,
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val targetUserId: String
) : ViewModel() {

    private val _paths = MutableStateFlow<List<List<Any>>>(emptyList()) // Any can be Profile or String "masked"
    val paths: StateFlow<List<List<Any>>> = _paths.asStateFlow()

    private val _graphData = MutableStateFlow(GraphData())
    val graphData: StateFlow<GraphData> = _graphData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadPaths()
    }

    private fun loadPaths() {
        viewModelScope.launch {
            _isLoading.value = true
            relationshipRepository.getRelationshipPaths(targetUserId)
                .onSuccess { rawPaths ->
                    // Resolve profiles for all non-masked IDs
                    val allIds = rawPaths.flatten().filter { it != "masked" }.distinct()
                    val profiles = chatRepository.getSearchableProfiles().first().filter { it.id in allIds }
                        .associateBy { it.id }
                    
                    val resolvedPaths = rawPaths.map { path ->
                        path.map { id ->
                            if (id == "masked") "masked"
                            else profiles[id] ?: id // Fallback to ID if profile not found
                        }
                    }
                    _paths.value = resolvedPaths
                    buildGraph(resolvedPaths)
                }
                .onFailure {
                    _error.value = it.message
                }
            _isLoading.value = false
        }
    }

    private fun buildGraph(paths: List<List<Any>>) {
        viewModelScope.launch {
            val me = authRepository.currentUserProfile.first() ?: return@launch
            
            val nodes = mutableMapOf<String, GraphNode>()
            val edges = mutableSetOf<GraphEdge>()
            
            // Add root (Me)
            nodes[me.id] = GraphNode(
                id = me.id,
                name = "You",
                avatarUrl = me.avatarUrl,
                type = NodeType.YOU,
                degree = 0,
                isClickable = false,
                highlightType = PathHighlightType.BEST_PATH
            )

            // Find the shortest path
            val shortestPathIndex = paths.indices.minByOrNull { paths[it].size } ?: -1
            
            // ALL nodes in any path should be identified
            val bestPathIds = mutableSetOf<String>()
            if (shortestPathIndex != -1) {
                paths[shortestPathIndex].forEach { node ->
                    when (node) {
                        is Profile -> bestPathIds.add(node.id)
                        is String -> if (node != "masked") bestPathIds.add(node)
                    }
                }
            }
            
            paths.forEachIndexed { pathIndex, path ->
                val isShortestPath = pathIndex == shortestPathIndex
                var prevNodeId = me.id
                
                path.forEachIndexed { index, node ->
                    val degree = index + 1
                    val nodeId = when (node) {
                        is Profile -> node.id
                        is String -> if (node == "masked") "masked_${path.hashCode()}_$index" else node
                        else -> "unknown"
                    }
                    
                    val nodeType = when {
                        degree == path.size -> NodeType.TARGET
                        node == "masked" -> NodeType.PRIVATE
                        else -> NodeType.USER
                    }
                    
                    val currentHighlight = if (isShortestPath || bestPathIds.contains(nodeId)) PathHighlightType.BEST_PATH else PathHighlightType.PATH
                    
                    if (!nodes.containsKey(nodeId)) {
                        nodes[nodeId] = GraphNode(
                            id = nodeId,
                            name = when (node) {
                                is Profile -> node.fullName ?: node.username
                                "masked" -> "Private Connection"
                                is String -> "User $node"
                                else -> "Unknown"
                            },
                            avatarUrl = if (node is Profile) node.avatarUrl else null,
                            type = nodeType,
                            degree = degree,
                            isClickable = node is Profile || (node is String && node != "masked"),
                            highlightType = currentHighlight,
                            isOnline = node is Profile && (System.currentTimeMillis() - node.lastSeen < 300_000),
                            isVerified = node is Profile && node.isVerified,
                            relationshipInfo = when {
                                degree == 0 -> "You"
                                degree == path.size -> "Target"
                                isShortestPath -> "${degree}${getOrdinal(degree)} connection"
                                else -> null
                            }
                        )
                    } else if (isShortestPath) {
                        nodes[nodeId] = nodes[nodeId]!!.copy(
                            highlightType = PathHighlightType.BEST_PATH,
                            relationshipInfo = "${degree}${getOrdinal(degree)} connection"
                        )
                    }
                    
                    val edgeHighlight = if (isShortestPath) PathHighlightType.BEST_PATH else PathHighlightType.PATH
                    // If multiple paths share an edge, and one is shortest, make it BEST_PATH
                    val existingEdge = edges.find { it.fromId == prevNodeId && it.toId == nodeId }
                    if (existingEdge == null) {
                        edges.add(GraphEdge(prevNodeId, nodeId, degree, highlightType = edgeHighlight))
                    } else if (isShortestPath) {
                        edges.remove(existingEdge)
                        edges.add(existingEdge.copy(highlightType = PathHighlightType.BEST_PATH))
                    }
                    prevNodeId = nodeId
                }
            }
            
            // Add background network nodes
            chatRepository.getSearchableProfiles().first().take(40).forEach { profile ->
                if (!nodes.containsKey(profile.id) && profile.id != me.id) {
                    val degree = (1..5).random()
                    nodes[profile.id] = GraphNode(
                        id = profile.id,
                        name = profile.fullName ?: profile.username,
                        avatarUrl = profile.avatarUrl,
                        type = NodeType.USER,
                        degree = degree,
                        isClickable = true,
                        highlightType = PathHighlightType.NONE,
                        isOnline = (System.currentTimeMillis() - profile.lastSeen < 300_000),
                        isVerified = profile.isVerified
                    )
                    
                    val connectionCount = (1..2).random()
                    nodes.values.filter { it.id != profile.id }.shuffled().take(connectionCount).forEach { neighbor ->
                        edges.add(GraphEdge(profile.id, neighbor.id, degree, highlightType = PathHighlightType.NONE))
                    }
                }
            }
            
            // Generate atmospheric background particles
            val particles = List(200) {
                Vector3(
                    x = (Math.random().toFloat() * 6000f - 3000f),
                    y = (Math.random().toFloat() * 6000f - 3000f),
                    z = (Math.random().toFloat() * 2000f - 1500f)
                )
            }
            
            val layoutedNodes = calculateLayout(nodes.values.toList())
            _graphData.value = GraphData(layoutedNodes, edges.toList(), particles)
        }
    }

    private fun calculateLayout(nodes: List<GraphNode>): List<GraphNode> {
        val result = mutableListOf<GraphNode>()
        val nodesByDegree = nodes.groupBy { it.degree }
        
        val horizontalSpacing = 700f
        val verticalSpacing = 500f
        val zDepthPerDegree = 400f // Z-axis for depth-of-field effect
        
        nodesByDegree.forEach { (degree, degreeNodes) ->
            // Primary path nodes should be at y=0, x=degree*spacing, z=0
            // Other nodes should be distributed around them.
            
            val xBase = degree * horizontalSpacing
            val zBase = -degree * zDepthPerDegree // Distant nodes are further in Z
            
            val primaryNodes = degreeNodes.filter { it.highlightType == PathHighlightType.BEST_PATH }
            val otherNodes = degreeNodes.filter { it.highlightType != PathHighlightType.BEST_PATH }
            
            // Place primary path nodes at the center
            primaryNodes.forEach { node ->
                result.add(node.copy(position = Vector3(xBase, 0f, 0f)))
            }
            
            // Distribute other nodes organically
            val count = otherNodes.size
            if (count > 0) {
                val radius = verticalSpacing * (1 + degree * 0.2f)
                otherNodes.forEachIndexed { index, node ->
                    val angle = (index.toFloat() / count) * 2f * PI.toFloat()
                    val xOff = cos(angle) * radius * 0.3f
                    val yOff = sin(angle) * radius
                    val zOff = (Math.random().toFloat() - 0.5f) * 500f
                    
                    result.add(node.copy(position = Vector3(xBase + xOff, yOff, zBase + zOff)))
                }
            }
        }
        
        return result
    }

    private fun getOrdinal(n: Int): String = when (n) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}
