package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun saveUserProfile(displayName: String, about: String, photoUrl: String) {
        val uid = requireUserId()
        val user = User(
            userId = uid,
            displayName = displayName,
            email = auth.currentUser?.email ?: "",
            phoneNumber = auth.currentUser?.phoneNumber ?: "",
            about = about,
            photoUrl = photoUrl,
            online = true,
            createdAt = Timestamp.now()
        )
        db.collection("users").document(uid).set(user).await()
    }

    suspend fun getUserProfile(userId: String): User? {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            doc.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun observeAllUsers(): Flow<List<User>> = callbackFlow {
        val uid = requireUserId()
        val listener = db.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val users = snapshot.toObjects(User::class.java).filter { it.userId != uid }
                    trySend(users)
                }
            }
        awaitClose { listener.remove() }
    }

    fun observeChats(): Flow<List<Chat>> = callbackFlow {
        val uid = requireUserId()
        val listener = db.collection("chats")
            .whereArrayContains("participantIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val chats = snapshot.toObjects(Chat::class.java)
                        .sortedByDescending { it.lastMessageTime?.seconds ?: 0L }
                    trySend(chats)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun getOrCreateChat(otherUserId: String, otherName: String, myName: String): String {
        val uid = requireUserId()
        val query = db.collection("chats")
            .whereArrayContains("participantIds", uid)
            .get()
            .await()

        for (doc in query.documents) {
            val participants = doc.get("participantIds") as? List<*>
            if (participants != null && participants.contains(otherUserId)) {
                return doc.id
            }
        }

        val chatRef = db.collection("chats").document()
        val chat = Chat(
            chatId = chatRef.id,
            participantIds = listOf(uid, otherUserId),
            participantNames = mapOf(uid to myName, otherUserId to otherName),
            lastMessage = "Chat created",
            lastMessageTime = Timestamp.now(),
            createdAt = Timestamp.now()
        )
        chatRef.set(chat).await()
        return chatRef.id
    }

    fun observeMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.toObjects(ChatMessage::class.java)
                    trySend(messages)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(chatId: String, text: String) {
        val uid = requireUserId()
        val encryptedRes = EncryptionHelper.encrypt(text)
        val msgRef = db.collection("chats").document(chatId).collection("messages").document()
        val message = ChatMessage(
            messageId = msgRef.id,
            senderId = uid,
            text = encryptedRes.cipherText,
            iv = encryptedRes.iv,
            timestamp = Timestamp.now(),
            status = "sent",
            encrypted = true
        )
        msgRef.set(message).await()

        db.collection("chats").document(chatId).update(
            mapOf(
                "lastMessage" to text,
                "lastMessageTime" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun markMessagesAsRead(chatId: String) {
        val uid = requireUserId()
        try {
            val unreadDocs = db.collection("chats").document(chatId)
                .collection("messages")
                .whereNotEqualTo("senderId", uid)
                .whereEqualTo("read", false)
                .get()
                .await()

            for (doc in unreadDocs.documents) {
                doc.reference.update(mapOf("read" to true, "status" to "read")).await()
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Failed to mark messages as read", e)
        }
    }

    suspend fun startCall(receiverId: String, receiverName: String, myName: String): String {
        val uid = requireUserId()
        val callRef = db.collection("calls").document()
        val call = CallLog(
            callId = callRef.id,
            callerId = uid,
            callerName = myName,
            receiverId = receiverId,
            receiverName = receiverName,
            status = "ringing",
            timestamp = Timestamp.now()
        )
        callRef.set(call).await()
        return callRef.id
    }

    fun observeIncomingCalls(): Flow<List<CallLog>> = callbackFlow {
        val uid = requireUserId()
        val listener = db.collection("calls")
            .whereEqualTo("receiverId", uid)
            .whereEqualTo("status", "ringing")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(CallLog::class.java))
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateCallStatus(callId: String, status: String) {
        try {
            db.collection("calls").document(callId).update("status", status).await()
        } catch (e: Exception) {
            Log.e("ChatRepository", "Failed to update call status", e)
        }
    }

    fun observeCallHistory(): Flow<List<CallLog>> = callbackFlow {
        val uid = requireUserId()
        val listener = db.collection("calls")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val calls = snapshot.toObjects(CallLog::class.java)
                        .filter { it.callerId == uid || it.receiverId == uid }
                        .sortedByDescending { it.timestamp?.seconds ?: 0L }
                    trySend(calls)
                }
            }
        awaitClose { listener.remove() }
    }
}
