package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val about: String = "Hey there! I am using ChatFlow.",
    val photoUrl: String = "",
    val online: Boolean = true,
    val lastSeen: Timestamp? = null,
    val createdAt: Timestamp? = null
)

data class Chat(
    @DocumentId val chatId: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTime: Timestamp? = null,
    val createdAt: Timestamp? = null
)

data class ChatMessage(
    @DocumentId val messageId: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null,
    val status: String = "sent", // sent, delivered, read
    val read: Boolean = false,
    val iv: String = "",
    val encrypted: Boolean = true
)

data class CallLog(
    @DocumentId val callId: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val status: String = "ringing", // ringing, accepted, rejected, ended
    val timestamp: Timestamp? = null,
    val callType: String = "voice"
)
