package com.keeftalk.chat.ui.profile

enum class NodeType {
    YOU, USER, PRIVATE, TARGET
}

enum class PathHighlightType {
    NONE, // Background noise
    PATH, // Secondary path
    BEST_PATH // Shortest/highlighted path
}

data class Vector3(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
)

data class GraphNode(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val type: NodeType,
    val degree: Int,
    val isClickable: Boolean,
    val position: Vector3 = Vector3(),
    val highlightType: PathHighlightType = PathHighlightType.NONE,
    val intensity: Float = 1.0f,
    val isOnline: Boolean = false,
    val isVerified: Boolean = false,
    val relationshipInfo: String? = null
)

data class GraphEdge(
    val fromId: String,
    val toId: String,
    val degree: Int,
    val highlightType: PathHighlightType = PathHighlightType.NONE
)

data class GraphData(
    val nodes: List<GraphNode> = emptyList(),
    val edges: List<GraphEdge> = emptyList(),
    val backgroundParticles: List<Vector3> = emptyList()
)
