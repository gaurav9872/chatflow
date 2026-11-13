package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.data.ChatRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.ProfileSetupScreen
import com.example.ui.navigation.ChatFlowNav
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                RootContent()
            }
        }
    }
}

@Composable
fun RootContent() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { ChatRepository(context) }
    val scope = rememberCoroutineScope()

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var hasProfile by remember { mutableStateOf<Boolean?>(null) }
    var isLoadingProfile by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            if (currentUser == null) {
                hasProfile = null
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            isLoadingProfile = true
            scope.launch {
                val profile = repository.getUserProfile(currentUser!!.uid)
                hasProfile = profile != null && profile.displayName.isNotBlank()
                isLoadingProfile = false
            }
        }
    }

    when {
        currentUser == null -> {
            AuthScreen(
                onAuthSuccess = { currentUser = Firebase.auth.currentUser }
            )
        }
        isLoadingProfile || hasProfile == null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        hasProfile == false -> {
            ProfileSetupScreen(
                onProfileSaved = { hasProfile = true }
            )
        }
        else -> {
            key(currentUser!!.uid) {
                ChatFlowNav(
                    onSignedOut = {
                        currentUser = null
                        hasProfile = null
                    }
                )
            }
        }
    }
}
