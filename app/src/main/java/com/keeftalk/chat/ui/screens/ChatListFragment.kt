package com.keeftalk.chat.ui.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import android.util.Log
import androidx.recyclerview.widget.ListAdapter
import androidx.paging.PagingDataAdapter
import androidx.paging.PagingData
import androidx.paging.map
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.keeftalk.chat.R
import com.keeftalk.chat.di.AppModule
import com.keeftalk.chat.domain.model.ChatListItemUiModel
import com.keeftalk.chat.util.CachedInflater
import com.keeftalk.chat.util.PerformanceProfiler

class ChatListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatPagingAdapter
    private var onChatClick: ((String) -> Unit)? = null
    private var onChatLongClick: ((ChatListItemUiModel) -> Unit)? = null
    private var currentFilter = "all"
    private var pagingJob: Job? = null

    private val mainViewModel: com.keeftalk.chat.ui.MainViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(requireActivity())[com.keeftalk.chat.ui.MainViewModel::class.java]
    }

    fun setOnChatClickListener(listener: (String) -> Unit) {
        onChatClick = listener
    }

    fun setOnChatLongClickListener(listener: (ChatListItemUiModel) -> Unit) {
        onChatLongClick = listener
    }

    fun setFilter(filter: String) {
        if (currentFilter != filter) {
            currentFilter = filter
            observePagingData()
        }
    }

    // Shared ViewPool for all fragments to maximize reuse
    companion object {
        private val sharedPool = RecyclerView.RecycledViewPool().apply {
            setMaxRecycledViews(0, 20)
        }
        
        fun warmViewPool(context: android.content.Context) {
            val inflater = LayoutInflater.from(context)
            repeat(10) {
                val view = CachedInflater.inflate(inflater, android.widget.FrameLayout(context))
                sharedPool.putRecycledView(ChatViewHolder(view, {}, {}, ""))
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        recyclerView = RecyclerView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            setRecycledViewPool(sharedPool)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            clipToPadding = false
            
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
                val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                val topOffset = (112 * resources.displayMetrics.density).toInt()
                v.setPadding(0, systemBars.top + topOffset, 0, systemBars.bottom)
                insets
            }
        }
        return recyclerView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentUserId = mainViewModel.userPreferences.value.userId
        
        // Phase 0: Use standard adapter for the very first frame (Lightweight)
        val skeletonAdapter = ChatListAdapter(
            { 
                PerformanceProfiler.startSession("CHAT_OPEN", PerformanceProfiler.Category.UI)
                PerformanceProfiler.startStage("Navigation started")
                onChatClick?.invoke(it) 
            }, 
            { onChatLongClick?.invoke(it) }, 
            currentUserId,
            mainViewModel.userPreferences.value.appCustomization.chatSpacingMultiplier
        )
        recyclerView.adapter = skeletonAdapter

        // Phase 1: FAST PATH (Core Architecture)
        // Bind directly from the synchronous Singleton cache
        val fastChats = com.keeftalk.chat.util.FastPathInbox.chats.value
        if (fastChats.isNotEmpty()) {
            skeletonAdapter.submitList(fastChats)
            (activity as? OnChatListReadyListener)?.onChatListReady()
            PerformanceProfiler.logEvent("FastPathInbox: Rendered synchronously via ListAdapter", category = PerformanceProfiler.Category.UI)
        }

        // Phase 2: Reactive updates for cache changes
        viewLifecycleOwner.lifecycleScope.launch {
            com.keeftalk.chat.util.FastPathInbox.chats.collect { chats ->
                if (chats.isNotEmpty() && recyclerView.adapter === skeletonAdapter) {
                    skeletonAdapter.submitList(chats)
                    (activity as? OnChatListReadyListener)?.onChatListReady()
                    PerformanceProfiler.logEvent("FastPathInbox: Updated via ListAdapter", category = PerformanceProfiler.Category.UI)
                }
            }
        }
        
        // Phase 3: Switch to Paging for full list
        observePagingData()
    }

    private fun observePagingData() {
        pagingJob?.cancel()
        pagingJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val pager = withContext(Dispatchers.IO) {
                    val db = AppModule.provideDatabase(requireContext())
                    
                    androidx.paging.Pager(
                        config = androidx.paging.PagingConfig(
                            pageSize = 30,
                            prefetchDistance = 10,
                            enablePlaceholders = false
                        ),
                        pagingSourceFactory = { 
                            when (currentFilter) {
                                "unread" -> db.chatDao().getUnreadChatsPaging()
                                "groups" -> db.chatDao().getGroupChatsPaging()
                                "archived" -> db.chatDao().getArchivedChatsPaging()
                                "favorites" -> db.chatDao().getFavoriteChatsPaging()
                                else -> db.chatDao().getActiveChatsPaging()
                            }
                        }
                    ).flow.map { pagingData: androidx.paging.PagingData<com.keeftalk.chat.data.local.entities.ChatEntity> ->
                        val userId = mainViewModel.userPreferences.value.userId
                        pagingData.map { it.toUiModel(userId) }
                    }.cachedIn(viewLifecycleOwner.lifecycleScope)
                }

                pager.collectLatest { pagingData ->
                    // Switch to paging adapter on first real data arrival
                    if (recyclerView.adapter !is ChatPagingAdapter) {
                        val currentUserId = mainViewModel.userPreferences.value.userId
                        adapter = ChatPagingAdapter(
                            { 
                                PerformanceProfiler.startSession("CHAT_OPEN", PerformanceProfiler.Category.UI)
                                PerformanceProfiler.startStage("Navigation started")
                                onChatClick?.invoke(it) 
                            }, 
                            { onChatLongClick?.invoke(it) }, 
                            currentUserId,
                            mainViewModel.userPreferences.value.appCustomization.chatSpacingMultiplier
                        )
                        recyclerView.adapter = adapter
                        PerformanceProfiler.logEvent("Switched to ChatPagingAdapter", category = PerformanceProfiler.Category.UI)
                    }
                    adapter.submitData(pagingData)
                    // Also notify ready just in case cache was empty
                    (activity as? OnChatListReadyListener)?.onChatListReady()
                }
            } catch (e: Throwable) {
                Log.e("ChatListFragment", "Paging failed: ${e.message}", e)
            }
        }
    }

    interface OnChatListReadyListener {
        fun onChatListReady()
    }

    private fun com.keeftalk.chat.data.local.entities.ChatEntity.toUiModel(currentUserId: String?): ChatListItemUiModel {
        val name = if (this.name == null || this.name == "Unknown" || this.name.equals("Direct Chat", ignoreCase = true)) {
            this.lastDecryptedMessage ?: "Unknown"
        } else {
            this.name
        }
        
        return ChatListItemUiModel(
            id = id,
            name = name,
            avatarUrl = avatarUrl,
            initials = com.keeftalk.chat.util.AvatarUtils.getInitials(name),
            lastMessage = lastDecryptedMessage ?: lastMessage,
            lastTimestamp = lastTimestamp,
            formattedTimestamp = formatTimestamp(lastTimestamp),
            unreadCount = unreadCount,
            isPinned = isPinned,
            isMuted = isMuted,
            lastMessageStatus = try { lastMessageStatus?.let { com.keeftalk.chat.domain.model.MessageStatus.valueOf(it) } } catch(_: Exception) { null },
            lastMessageSenderId = lastMessageSenderId,
            isArchived = isArchived,
            isFavorite = isFavorite,
            peerId = peerId,
            type = if (type == "GROUP") com.keeftalk.chat.domain.model.ChatType.GROUP else com.keeftalk.chat.domain.model.ChatType.ONE_TO_ONE,
            isTyping = false,
            snippetType = snippetType,
            snippetUri = snippetUri,
            latestAuthorName = null // Would need to parse metadata if needed
        )
    }

    class ChatPagingAdapter(
        private val onClick: (String) -> Unit, 
        private val onLongClick: (ChatListItemUiModel) -> Unit, 
        private val currentUserId: String,
        private val spacingMultiplier: Float = 0.5f
    ) : PagingDataAdapter<ChatListItemUiModel, ChatViewHolder>(ChatDiffCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
            val view = CachedInflater.inflate(LayoutInflater.from(parent.context), parent)
            return ChatViewHolder(view, onClick, onLongClick, currentUserId, spacingMultiplier)
        }

        override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
            getItem(position)?.let { holder.bind(it) }
        }
    }

    class ChatListAdapter(
        private val onClick: (String) -> Unit, 
        private val onLongClick: (ChatListItemUiModel) -> Unit, 
        private val currentUserId: String,
        private val spacingMultiplier: Float = 0.5f
    ) : ListAdapter<ChatListItemUiModel, ChatViewHolder>(ChatDiffCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
            val view = CachedInflater.inflate(LayoutInflater.from(parent.context), parent)
            return ChatViewHolder(view, onClick, onLongClick, currentUserId, spacingMultiplier)
        }

        override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    class ChatViewHolder(
        view: View, 
        private val onClick: (String) -> Unit, 
        private val onLongClick: (ChatListItemUiModel) -> Unit, 
        private val currentUserId: String,
        private val spacingMultiplier: Float = 0.5f
    ) : RecyclerView.ViewHolder(view) {
        private val avatar = view.findViewById<ImageView>(R.id.avatar)
        private val avatarInitials = view.findViewById<TextView>(R.id.avatar_initials)
        private val onlineIndicator = view.findViewById<View>(R.id.online_indicator)
        private val statusIndicator = view.findViewById<ImageView>(R.id.status_indicator)
        private val name = view.findViewById<TextView>(R.id.name)
        private val snippet = view.findViewById<TextView>(R.id.snippet)
        private val time = view.findViewById<TextView>(R.id.time)
        private val unreadBadge = view.findViewById<TextView>(R.id.unread_badge)
        private val statusContainer = view.findViewById<View>(R.id.status_container)
        private val statusInitials = view.findViewById<TextView>(R.id.status_initials)
        private val mediaPreview = view.findViewById<ImageView>(R.id.media_preview)

        fun bind(chat: ChatListItemUiModel) {
            // Apply Spacing
            val density = itemView.resources.displayMetrics.density
            val baseHeight = (80 * density).toInt()
            val basePadding = (12 * density).toInt()
            val scaledPadding = (basePadding * spacingMultiplier).toInt()
            val scaledHeight = baseHeight - (basePadding * 2) + (scaledPadding * 2)
            
            val params = itemView.layoutParams
            if (params != null) {
                params.height = scaledHeight
                itemView.layoutParams = params
            } else {
                itemView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 
                    scaledHeight
                )
            }
            itemView.setPadding(itemView.paddingLeft, scaledPadding, itemView.paddingRight, scaledPadding)

            itemView.setOnClickListener { onClick(chat.id) }
            itemView.setOnLongClickListener { 
                onLongClick(chat)
                true 
            }
            name.text = chat.name
            snippet.text = chat.lastMessage
            time.text = chat.formattedTimestamp
            
            // Media Preview Handling
            if (!chat.snippetUri.isNullOrBlank() && (chat.snippetType == "IMAGE" || chat.snippetType == "VIDEO")) {
                mediaPreview.visibility = View.VISIBLE
                mediaPreview.load(chat.snippetUri) {
                    crossfade(true)
                    transformations(coil.transform.RoundedCornersTransformation(12f))
                }
            } else {
                mediaPreview.visibility = View.GONE
                mediaPreview.setImageDrawable(null)
            }

            if (chat.unreadCount > 0) {
                unreadBadge.visibility = View.VISIBLE
                unreadBadge.text = chat.unreadCount.toString()
            } else {
                unreadBadge.visibility = View.GONE
            }

            val avatarIdentifier = chat.peerId ?: chat.id
            val localAvatar = com.keeftalk.chat.util.PersistentAvatarManager.getLocalAvatarFile(itemView.context, avatarIdentifier)
            
            if (localAvatar != null) {
                avatarInitials.visibility = View.GONE
                avatar.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                avatar.load(localAvatar) {
                    transformations(coil.transform.CircleCropTransformation())
                    crossfade(true)
                }
            } else if (chat.avatarUrl.isNullOrBlank()) {
                avatar.setImageDrawable(null)
                avatar.setBackgroundColor(com.keeftalk.chat.util.AvatarUtils.getAvatarColorInt(chat.id))
                avatarInitials.text = chat.initials
                avatarInitials.visibility = View.VISIBLE
            } else {
                avatarInitials.visibility = View.GONE
                avatar.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                avatar.load(chat.avatarUrl) {
                    transformations(coil.transform.CircleCropTransformation())
                    crossfade(true)
                    listener(onSuccess = { _, result ->
                        if (result.dataSource == coil.decode.DataSource.NETWORK) {
                            val drawable = result.drawable
                            if (drawable is android.graphics.drawable.BitmapDrawable) {
                                val bitmap = drawable.bitmap
                                val stream = java.io.ByteArrayOutputStream()
                                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
                                com.keeftalk.chat.util.PersistentAvatarManager.saveAvatar(itemView.context, avatarIdentifier, stream.toByteArray())
                            }
                        }
                    })
                }
            }

            onlineIndicator.visibility = if (chat.unreadCount > 0) View.VISIBLE else View.GONE
            
            val isFromMe = chat.lastMessageSenderId == currentUserId
            if (isFromMe && chat.lastMessageStatus != null) {
                statusContainer.visibility = View.VISIBLE
                if (chat.lastMessageStatus == com.keeftalk.chat.domain.model.MessageStatus.SEEN) {
                    if (localAvatar != null) {
                        statusInitials.visibility = View.GONE
                        statusIndicator.load(localAvatar) {
                            transformations(coil.transform.CircleCropTransformation())
                        }
                    } else if (!chat.avatarUrl.isNullOrBlank()) {
                        statusInitials.visibility = View.GONE
                        statusIndicator.load(chat.avatarUrl) {
                            transformations(coil.transform.CircleCropTransformation())
                        }
                    } else {
                        statusIndicator.setImageDrawable(android.graphics.drawable.ColorDrawable(com.keeftalk.chat.util.AvatarUtils.getAvatarColorInt(chat.id)))
                        statusInitials.text = chat.initials
                        statusInitials.visibility = View.VISIBLE
                    }
                } else {
                    statusInitials.visibility = View.GONE
                    statusIndicator.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    val (iconRes, tint) = when (chat.lastMessageStatus) {
                        com.keeftalk.chat.domain.model.MessageStatus.SENDING -> R.drawable.ic_status_sent_eye to android.graphics.Color.GRAY
                        com.keeftalk.chat.domain.model.MessageStatus.SENT -> R.drawable.ic_status_sent_eye to android.graphics.Color.GRAY
                        com.keeftalk.chat.domain.model.MessageStatus.DELIVERED -> R.drawable.ic_status_delivered_eye to android.graphics.Color.GRAY
                        else -> R.drawable.ic_status_sent_eye to android.graphics.Color.GRAY
                    }
                    statusIndicator.setImageResource(iconRes)
                    statusIndicator.setColorFilter(tint)
                }
            } else {
                statusContainer.visibility = View.GONE
            }
        }
    }

    class ChatDiffCallback : DiffUtil.ItemCallback<ChatListItemUiModel>() {
        override fun areItemsTheSame(oldItem: ChatListItemUiModel, newItem: ChatListItemUiModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatListItemUiModel, newItem: ChatListItemUiModel): Boolean {
            return oldItem == newItem
        }
    }
}
