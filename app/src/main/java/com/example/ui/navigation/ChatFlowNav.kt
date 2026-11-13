package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.ChatRepository
import com.example.data.CallLog
import com.example.ui.calls.CallHistoryScreen
import com.example.ui.calls.VoiceCallScreen
import com.example.ui.chats.ChatListScreen
import com.example.ui.chats.ChatRoomScreen
import com.example.ui.chats.NewChatScreen
import com.example.ui.profile.ProfileSettingsScreen
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Chats : Screen("chats")
    object Calls : Screen("calls")
    object Profile : Screen("profile")
    object NewChat : Screen("new_chat")
    object ChatRoom : Screen("chat_room/{chatId}") {
        fun createRoute(chatId: String) = "chat_room/$chatId"
    }
    object VoiceCall : Screen("voice_call/{callId}/{userName}") {
        fun createRoute(callId: String, userName: String) = "voice_call/$callId/$userName"
    }
}

@Composable
fun ChatFlowNav(onSignedOut: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { ChatRepository(context) }
    val scope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf<String>(Screen.Chats.route) }
    var activeChatId by remember { mutableStateOf<String?>(null) }
    var activeCallId by remember { mutableStateOf<String?>(null) }
    var activeCallUserName by remember { mutableStateOf<String>("") }
    var incomingCall by remember { mutableStateOf<CallLog?>(null) }

    // Observe incoming calls globally
    LaunchedEffect(Unit) {
        repository.observeIncomingCalls().collect { calls ->
            if (calls.isNotEmpty() && activeCallId == null) {
                incomingCall = calls.first()
            }
        }
    }

    // If an incoming call dialog is ringing
    if (incomingCall != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Incoming Voice Call") },
            text = { Text("${incomingCall?.callerName ?: "Someone"} is calling you...") },
            confirmButton = {
                TextButton(onClick = {
                    val call = incomingCall!!
                    incomingCall = null
                    scope.launch {
                        repository.updateCallStatus(call.callId, "accepted")
                    }
                    activeCallId = call.callId
                    activeCallUserName = call.callerName
                }) {
                    Text("Accept")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    val call = incomingCall!!
                    incomingCall = null
                    scope.launch {
                        repository.updateCallStatus(call.callId, "rejected")
                    }
                }) {
                    Text("Decline")
                }
            }
        )
    }

    if (activeCallId != null) {
        VoiceCallScreen(
            callId = activeCallId!!,
            otherUserName = activeCallUserName,
            onEndCall = {
                activeCallId = null
            }
        )
        return
    }

    Scaffold(
        bottomBar = {
            if (currentScreen == Screen.Chats.route || currentScreen == Screen.Calls.route || currentScreen == Screen.Profile.route) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Chat, contentDescription = "Chats") },
                        label = { Text("Chats") },
                        selected = currentScreen == Screen.Chats.route,
                        onClick = { currentScreen = Screen.Chats.route }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Call, contentDescription = "Calls") },
                        label = { Text("Calls") },
                        selected = currentScreen == Screen.Calls.route,
                        onClick = { currentScreen = Screen.Calls.route }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        selected = currentScreen == Screen.Profile.route,
                        onClick = { currentScreen = Screen.Profile.route }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                currentScreen == Screen.Chats.route -> {
                    ChatListScreen(
                        onChatClick = { chatId ->
                            activeChatId = chatId
                            currentScreen = Screen.ChatRoom.route
                        },
                        onNewChatClick = {
                            currentScreen = Screen.NewChat.route
                        }
                    )
                }
                currentScreen == Screen.Calls.route -> {
                    CallHistoryScreen(
                        onStartCallClick = { receiverId, name ->
                            scope.launch {
                                val callId = repository.startCall(receiverId, name, "User")
                                activeCallId = callId
                                activeCallUserName = name
                            }
                        }
                    )
                }
                currentScreen == Screen.Profile.route -> {
                    ProfileSettingsScreen(onSignedOut = onSignedOut)
                }
                currentScreen == Screen.NewChat.route -> {
                    NewChatScreen(
                        onChatSelected = { chatId ->
                            activeChatId = chatId
                            currentScreen = Screen.ChatRoom.route
                        },
                        onBack = { currentScreen = Screen.Chats.route }
                    )
                }
                currentScreen == Screen.ChatRoom.route && activeChatId != null -> {
                    ChatRoomScreen(
                        chatId = activeChatId!!,
                        onBack = { currentScreen = Screen.Chats.route },
                        onStartCall = { chatId, name ->
                            scope.launch {
                                // Start call in background
                                val callId = repository.startCall(chatId, name, "User")
                                activeCallId = callId
                                activeCallUserName = name
                            }
                        }
                    )
                }
            }
        }
    }
}
