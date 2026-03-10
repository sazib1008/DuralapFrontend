package com.example.duralab.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

// Custom colors based on Tailwind theme
private val Blue500 = Color(0xFF3B82F6)
private val Purple500 = Color(0xFFA855F7)
private val Blue600 = Color(0xFF2563EB)
private val Blue50 = Color(0xFFEFF6FF)
private val Blue100 = Color(0xFFDBEAFE)
private val Blue400 = Color(0xFF60A5FA)
private val Blue700 = Color(0xFF1D4ED8)
private val Gray900 = Color(0xFF111827)
private val Gray800 = Color(0xFF1F2937)
private val Gray600 = Color(0xFF4B5563)
private val Gray500 = Color(0xFF6B7280)
private val Gray400 = Color(0xFF9CA3AF)
private val Gray300 = Color(0xFFD1D5DB)
private val Gray100 = Color(0xFFF3F4F6)
private val Gray50 = Color(0xFFF9FAFB)
private val Green500 = Color(0xFF22C55E)
private val Green600 = Color(0xFF16A34A)
private val Green50 = Color(0xFFF0FDF4)
private val Orange600 = Color(0xFFEA580C)
private val Orange50 = Color(0xFFFFF7ED)
private val Purple600 = Color(0xFF9333EA)
private val Purple50 = Color(0xFFFAF5FF)
private val Red500 = Color(0xFFEF4444)
private val Red100 = Color(0xFFFEE2E2)
private val Red50 = Color(0xFFFEF2F2)

@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Header Area with Profile Picture
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val gradient = Brush.linearGradient(
                        colors = listOf(Purple500, Blue500),
                        start = Offset(0f, Float.POSITIVE_INFINITY),
                        end = Offset(Float.POSITIVE_INFINITY, 0f)
                    )
                    
                    Box {
                        Box(
                            modifier = Modifier
                                .size(128.dp)
                                .clip(CircleShape)
                                .background(gradient)
                                .padding(4.dp)
                        ) {
                            val profileUrl = uiState.userProfile?.profilePicture ?: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=200&h=200&auto=format&fit=crop"
                            AsyncImage(
                                model = profileUrl,
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .border(4.dp, Color.White, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        // Online indicator
                        val isOnline = uiState.userProfile?.status?.uppercase() == "ONLINE"
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 8.dp, bottom = 8.dp)
                                .size(24.dp)
                                .border(4.dp, Color.White, CircleShape)
                                .clip(CircleShape)
                                .background(if (isOnline) Green500 else Gray400)
                        )
                    }
                }
                
                // Name & verification
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.userProfile?.username ?: "Unknown User",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gray900
                        )
                        if (uiState.userProfile?.isEmailVerified == true) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = Blue500,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    Text(
                        text = "@${uiState.userProfile?.username ?: "username"}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Blue600,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    
                    Text(
                        text = "Android Developer | Tech Enthusiast | UI/UX Designer",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Gray500,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                        lineHeight = 20.sp
                    )
                }

                // Quick Stats
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier
                            .background(Blue50, RoundedCornerShape(16.dp))
                            .border(1.dp, Blue100, RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Last Seen",
                            tint = Blue600,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "LAST SEEN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Blue400,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "2 mins ago",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Blue700
                            )
                        }
                    }
                }
                
                // Contact Info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ContactItem(
                        icon = Icons.Default.Email,
                        label = "EMAIL ADDRESS",
                        value = uiState.userProfile?.email ?: "Not provided"
                    )
                    ContactItem(
                        icon = Icons.Default.Phone,
                        label = "PHONE NUMBER",
                        value = "+880 1712-345678" // Default from design
                    )
                    ContactItem(
                        icon = Icons.Default.LocationOn,
                        label = "LOCATION",
                        value = "Dhaka, Bangladesh" // Default from design
                    )
                }
                
                // Settings
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .padding(bottom = 48.dp)
                ) {
                    Text(
                        text = "Account Actions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Gray900,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    SettingsMenu()
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Logout button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Red50, RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.logout()
                                onLogout()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = Red500,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Sign Out",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Red500
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(Gray50, RoundedCornerShape(12.dp))
                .border(1.dp, Gray100, RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Gray600,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Gray400,
                letterSpacing = 0.5.sp
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Gray800
            )
        }
    }
}

@Composable
fun SettingsMenu() {
    val items = listOf(
        MenuItem(Icons.Default.Person, "Personal Info", "Update your full name and bio", Blue600, Blue50),
        MenuItem(Icons.Default.Notifications, "Notifications", "Manage your alerts", Orange600, Orange50),
        MenuItem(Icons.Default.Security, "Security", "Password and Privacy", Green600, Green50),
        MenuItem(Icons.Default.Settings, "Settings", "App preferences", Purple600, Purple50)
    )
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Gray50, RoundedCornerShape(16.dp))
                    .clickable { /* Handle click */ }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(item.bg, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = item.color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = item.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gray800
                        )
                        Text(
                            text = item.subtitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Gray400
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Forward",
                    tint = Gray300,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

data class MenuItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val color: Color,
    val bg: Color
)
