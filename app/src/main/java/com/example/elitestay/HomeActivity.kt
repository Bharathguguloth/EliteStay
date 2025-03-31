package com.example.elitestay

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.elitestay.ui.theme.EliteStayTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EliteStayTheme {
                HomeActivityContent()
            }
        }
    }
}

// ✅ Composable-safe way to define background gradient
@Composable
fun AppBackgroundBrush(): Brush = Brush.verticalGradient(
    colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary
    )
)

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home : BottomNavItem("home", "Home", Icons.Default.Home)
    object Shortlist : BottomNavItem("shortlist", "Shortlist", Icons.Default.Favorite)
    object Profile : BottomNavItem("profile", "Profile", Icons.Default.Person)
}

@Composable
fun HomeActivityContent() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Shortlist,
        BottomNavItem.Profile
    )

    // 🔒 Auto logout if inactive for 2 minutes
    AutoLogoutHandler {
        FirebaseAuth.getInstance().signOut()
        context.startActivity(
            Intent(context, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController, items = bottomNavItems)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) { HomeScreen() }
            composable(BottomNavItem.Shortlist.route) { ShortlistScreen() }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        navController.context.startActivity(
                            Intent(navController.context, LoginActivity::class.java)
                        )
                    }
                )
            }
        }
    }
}


@Composable
fun AutoLogoutHandler(timeoutMillis: Long = 2 * 60 * 1000L, onTimeout: () -> Unit) {
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }
    val scope = rememberCoroutineScope()

    // Check inactivity every second
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            val now = System.currentTimeMillis()
            if (now - lastInteractionTime > timeoutMillis) {
                onTimeout()
                break
            }
        }
    }

    // Update on UI changes (recomposition)
    SideEffect {
        lastInteractionTime = System.currentTimeMillis()
    }
}


@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    items: List<BottomNavItem>
) {
    val currentDestination by navController.currentBackStackEntryAsState()
    val currentRoute = currentDestination?.destination?.route

    NavigationBar(containerColor = Color.White) {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}

@Composable
fun HomeScreen() {
    val background = AppBackgroundBrush()
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("EliteStay", fontSize = 24.sp, color = Color.White)
            Button(
                onClick = { /* Refer action */ },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text("Refer & Earn", color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by City, University or Property") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(30.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, shape = RoundedCornerShape(30.dp))
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Continue Your Search Journey", fontSize = 18.sp, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Chip(label = "Middlesbrough")
            Chip(label = "Teesside University")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Continue Where You Left From", fontSize = 18.sp, color = Color.White)

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterButton("Recently Viewed", selected = true)
            FilterButton("Shortlisted")
            FilterButton("Enquired")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("SOLD OUT - Property Preview")
            }
        }
    }
}

@Composable
fun Chip(label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.2f),
        modifier = Modifier
            .height(36.dp)
            .clickable { }
            .padding(horizontal = 8.dp),
        tonalElevation = 2.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(label, color = Color.White)
        }
    }
}

@Composable
fun FilterButton(text: String, selected: Boolean = false) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.background else Color.White.copy(alpha = 0.6f)
    val textColor = if (selected) MaterialTheme.colorScheme.primary else Color.DarkGray

    Surface(
        shape = RoundedCornerShape(50),
        color = backgroundColor,
        shadowElevation = 4.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(text, color = textColor, fontSize = 14.sp)
        }
    }
}

@Composable
fun ShortlistScreen() {
    val background = AppBackgroundBrush()
    val properties = listOf(
        "Elite Villa, Goa",
        "Luxury Studio, Bangalore",
        "Ocean View Apartment, Mumbai"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Shortlisted Properties", style = MaterialTheme.typography.headlineSmall, color = Color.White)

            properties.forEach { property ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(property, fontSize = 16.sp)
                        Text("View", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    fullName: String = "Bharat",
    email: String = "bharat12@gmail.com",
    onLogout: () -> Unit
) {
    val background = AppBackgroundBrush()
    var notificationsEnabled by remember { mutableStateOf(true) }
    var darkModeEnabled by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(24.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("My Profile", style = MaterialTheme.typography.headlineSmall, color = Color.White)

            // Profile Info
            ProfileItem("Full Name", fullName)
            ProfileItem("Email", email)

            Spacer(modifier = Modifier.height(16.dp))

            // Settings Section
            Text("Settings", style = MaterialTheme.typography.titleMedium, color = Color.White)

            SettingsToggleItem("Notifications", notificationsEnabled) {
                notificationsEnabled = it
            }

            SettingsToggleItem("Dark Mode", darkModeEnabled) {
                darkModeEnabled = it
            }

            SettingsClickableItem("Language", value = "English") {
                // TODO: Add language selection logic
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Logout", color = MaterialTheme.colorScheme.onError)
            }
        }
    }
}


@Composable
fun ProfileItem(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 16.sp)
        }
    }
}

@Composable
fun SettingsToggleItem(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 16.sp)
            Switch(checked = checked, onCheckedChange = onToggle)
        }
    }
}

@Composable
fun SettingsClickableItem(label: String, value: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 16.sp)
            Text(value, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

