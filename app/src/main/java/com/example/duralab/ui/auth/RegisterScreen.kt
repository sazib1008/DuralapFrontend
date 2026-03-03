package com.example.duralab.ui.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.duralab.ui.theme.*
import com.example.duralab.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val registerState by viewModel.registerState.collectAsState()
    val scrollState = rememberScrollState()

    // Animations
    val fadeAnim = remember { Animatable(0f) }
    val slideAnim = remember { Animatable(15f) }

    LaunchedEffect(Unit) {
        fadeAnim.animateTo(1f, tween(600, easing = LinearOutSlowInEasing))
        slideAnim.animateTo(0f, tween(600, easing = LinearOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF8FAFC), Color.White)
                )
            )
    ) {
        // Decorative Background Elements
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 64.dp, y = (-64).dp)
                .size(128.dp)
                .background(Blue200.copy(alpha = 0.4f), CircleShape)
                .blur(32.dp)
        )
        Box(
            modifier = Modifier
                .size(96.dp)
                .offset(x = (-48).dp, y = 160.dp)
                .background(Color(0xFFE0E7FF).copy(alpha = 0.5f), CircleShape)
                .blur(24.dp)
        )

        // Camera Hole Mockup
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 15.dp)
                .size(14.dp)
                .background(Color.Black, CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Back Button
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onNavigateToLogin() },
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Slate400,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Header
            Column(
                modifier = Modifier
                    .alpha(fadeAnim.value)
                    .offset(y = slideAnim.value.dp)
            ) {
                Text(
                    text = "Join Us",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate800,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Start your journey with Duralap",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate400,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Form Content
            Column(
                modifier = Modifier
                    .alpha(fadeAnim.value)
                    .offset(y = slideAnim.value.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar Upload Mockup
                Box(
                    modifier = Modifier.padding(bottom = 24.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color(0xFFF1F5F9))
                            .border(
                                width = 2.dp,
                                color = Color(0xFFCBD5E1),
                                shape = RoundedCornerShape(32.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .offset(x = 4.dp, y = 4.dp)
                            .size(28.dp)
                            .background(Blue500, RoundedCornerShape(10.dp))
                            .border(2.dp, Color.White, RoundedCornerShape(10.dp))
                            .clickable { /* Upload logic */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Input Fields
                RegisterTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    placeholder = "Full Name"
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                RegisterTextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = "Username"
                )

                Spacer(modifier = Modifier.height(16.dp))

                RegisterTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "Email Address",
                    keyboardType = KeyboardType.Email
                )

                Spacer(modifier = Modifier.height(16.dp))

                RegisterTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    placeholder = "Phone Number",
                    keyboardType = KeyboardType.Phone
                )

                Spacer(modifier = Modifier.height(16.dp))

                RegisterTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    placeholder = "Bio (Optional)",
                    singleLine = false,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                RegisterTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Password",
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color(0xFFCBD5E1)
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Create Account Button
                Button(
                    onClick = { viewModel.register(username, email, password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(24.dp),
                            spotColor = Blue200,
                            ambientColor = Blue200
                        ),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue500),
                    shape = RoundedCornerShape(24.dp),
                    enabled = registerState !is UiState.Loading
                ) {
                    if (registerState is UiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text(
                            text = "Create Account",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Terms & Privacy
                Text(
                    text = "By tapping Create Account, you agree to our Terms & Privacy Policy.",
                    fontSize = 10.sp,
                    color = Slate400,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Login Redirect
                Row(
                    modifier = Modifier.padding(bottom = 32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Have an account? ", fontSize = 14.sp, color = Slate500, fontWeight = FontWeight.Medium)
                    Text(
                        text = "Log In",
                        fontSize = 14.sp,
                        color = Blue500,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onNavigateToLogin() }
                    )
                }

                // Navigation Bar Mockup
                Box(
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .size(width = 96.dp, height = 6.dp)
                        .background(Slate200, RoundedCornerShape(3.dp))
                )
            }
        }

        // Handle success navigation
        if (registerState is UiState.Success) {
            LaunchedEffect(Unit) {
                onRegisterSuccess()
            }
        }

        // Error Message
        if (registerState is UiState.Error) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 100.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = (registerState as UiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Slate400, fontSize = 14.sp, fontWeight = FontWeight.Medium) },
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x1A000000)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedIndicatorColor = Blue500,
            unfocusedIndicatorColor = Color(0xFFF1F5F9),
            cursorColor = Blue500
        ),
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        maxLines = maxLines
    )
}

