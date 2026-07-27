package com.example.duralapapp.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.duralapapp.ui.theme.*
import com.example.duralapapp.ui.theme.TextMain
import com.example.duralapapp.ui.theme.TextMuted

import androidx.compose.ui.tooling.preview.Preview
import com.example.duralapapp.ui.theme.DuralapAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackToHome: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val updateMessage by viewModel.updateMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var isEditDialogVisible by remember { mutableStateOf(false) }
    var isSignOutDialogVisible by remember { mutableStateOf(false) }

    var editFullName by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }

    LaunchedEffect(updateMessage) {
        updateMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUpdateMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BgDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackToHome) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextMain)
                    }
                    Text(
                        text = "Profile Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextMain,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (uiState is ProfileUiState.Success) {
                    IconButton(onClick = {
                        val user = (uiState as ProfileUiState.Success).user
                        editFullName = user.fullName ?: ""
                        editBio = user.bio ?: ""
                        isEditDialogVisible = true
                    }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Profile", tint = PrimaryBlue)
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
                is ProfileUiState.Success -> {
                    val user = state.user
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))

                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(CardBg)
                                .border(2.dp, PrimaryBlue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(60.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = user.fullName ?: user.username,
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextMain,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "@${user.username} • ${user.email}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                        user.bio?.let { bio ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = bio,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        ProfileOptionItem(icon = Icons.Default.Person, label = "Account Information")
                        ProfileOptionItem(icon = Icons.Default.Notifications, label = "Notifications")
                        ProfileOptionItem(icon = Icons.Default.Security, label = "Privacy & Security")
                        ProfileOptionItem(icon = Icons.Default.Help, label = "Help & Support")

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = { isSignOutDialogVisible = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(28.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = "Sign Out",
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
                is ProfileUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadUserProfile() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (isEditDialogVisible) {
        AlertDialog(
            onDismissRequest = { isEditDialogVisible = false },
            containerColor = CardBg,
            title = { Text("Edit Profile", color = TextMain, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editFullName,
                        onValueChange = { editFullName = it },
                        label = { Text("Full Name", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = InputBorderColor,
                            focusedTextColor = TextMain,
                            unfocusedTextColor = TextMain
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Bio", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = InputBorderColor,
                            focusedTextColor = TextMain,
                            unfocusedTextColor = TextMain
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProfile(editFullName, editBio)
                        isEditDialogVisible = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditDialogVisible = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    if (isSignOutDialogVisible) {
        AlertDialog(
            onDismissRequest = { isSignOutDialogVisible = false },
            containerColor = CardBg,
            title = { Text("Sign Out", color = TextMain, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out of Duralap?", color = TextMuted) },
            confirmButton = {
                Button(
                    onClick = {
                        isSignOutDialogVisible = false
                        viewModel.signOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Sign Out", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { isSignOutDialogVisible = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

@Composable
fun ProfileOptionItem(icon: ImageVector, label: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrimaryBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = label, color = TextMain, fontWeight = FontWeight.Medium)
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
fun ProfileScreenPreview() {
    DuralapAppTheme {
        ProfileScreen(onBackToHome = {})
    }
}
