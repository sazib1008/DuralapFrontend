package com.example.duralab.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.duralab.ui.auth.AuthViewModel

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun SplashScreen(
    viewModel: AuthViewModel,
    onNavigateToDashboard: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    // Animations
    val logoScale = remember { Animatable(0.5f) }
    val contentAlpha = remember { Animatable(0f) }
    val contentOffsetY = remember { Animatable(20f) }
    val brandingAlpha = remember { Animatable(0f) }
    val brandingOffsetY = remember { Animatable(20f) }

    LaunchedEffect(Unit) {
        // Logo Animation (Bounce effect)
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
        }

        // Content Animation (Fade in + Slide up)
        launch {
            delay(500)
            launch {
                contentAlpha.animateTo(1f, tween(1000))
            }
            launch {
                contentOffsetY.animateTo(0f, tween(1000))
            }
        }

        // Branding Animation (Fade in + Slide up)
        launch {
            delay(1200)
            launch {
                brandingAlpha.animateTo(1f, tween(1000))
            }
            launch {
                brandingOffsetY.animateTo(0f, tween(1000))
            }
        }

        // Navigation delay
        delay(3500)
        if (viewModel.isLoggedIn()) {
            onNavigateToDashboard()
        } else {
            onNavigateToLogin()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Camera Hole Mockup
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 15.dp)
                .size(14.dp)
                .background(Color.Black, CircleShape)
        )

        // Center Content
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Logo
            Box(
                modifier = Modifier
                    .scale(logoScale.value)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(40.dp),
                        spotColor = MaterialTheme.colorScheme.primaryContainer,
                        ambientColor = MaterialTheme.colorScheme.primaryContainer
                    )
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(40.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Name & Tagline
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(contentAlpha.value)
                    .offset(y = contentOffsetY.value.dp)
            ) {
                Text(
                    text = "Duralap",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "MESSAGING & VIDEO CALL",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Bottom Branding
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .alpha(brandingAlpha.value)
                .offset(y = brandingOffsetY.value.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "FROM",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.outline,
                letterSpacing = 4.sp
            )
            Text(
                text = "SAZIB TECH",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Loading Indicator
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp
            )
        }

        // Navigation Bar Mockup (Android Pill)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .size(width = 112.dp, height = 4.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp))
        )
    }
}

