package com.example.elitestay

import PropertyDetailsScreen
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
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.elitestay.model.Property
import com.example.elitestay.ui.theme.EliteStayTheme
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction

import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resumeWithException

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import shortlistedProperties


class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyBEUsccO5elkQd3fYEzICIN6VOQiInZLJU")
        }

        setContent {
            EliteStayTheme {
                HomeActivityContent()
            }
        }
    }
}

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
            composable(BottomNavItem.Home.route) { HomeScreen(navController) }
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
            composable(
                route = "property_details/{propertyId}",
                arguments = listOf(
                    navArgument("propertyId") { defaultValue = "" }
                )
            ) { backStackEntry ->
                val propertyId = backStackEntry.arguments?.getString("propertyId") ?: ""
                PropertyDetailsScreen(propertyId = propertyId, navController = navController)

            }


        }
    }
}

@Composable
fun AutoLogoutHandler(timeoutMillis: Long = 2 * 60 * 1000L, onTimeout: () -> Unit) {
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

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
fun HomeScreen(navController: NavHostController) {
    val background = AppBackgroundBrush()
    val context = LocalContext.current
    val placesClient: PlacesClient = remember { Places.createClient(context) }

    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf(listOf<AutocompletePrediction>()) }
    var selectedPlaceDetails by remember { mutableStateOf<Pair<String, String>?>(null) }

    val firestoreProperties = remember { mutableStateMapOf<String, Property>() }

    LaunchedEffect(Unit) {
        val db = Firebase.firestore
        val snapshot = db.collection("properties").get().await()
        snapshot.documents.forEach { doc ->
            val prop = doc.toObject(Property::class.java)
            if (prop != null) {
                firestoreProperties[doc.id] = prop
            }
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length > 2) {
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(searchQuery)
                .build()

            try {
                val response = withContext(Dispatchers.IO) {
                    placesClient.findAutocompletePredictions(request).await()
                }
                suggestions = response.autocompletePredictions
            } catch (e: Exception) {
                suggestions = emptyList()
            }
        } else {
            suggestions = emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(16.dp)
    ) {
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

        Spacer(modifier = Modifier.height(12.dp))

        suggestions.forEach { prediction ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable {
                        searchQuery = prediction.getFullText(null).toString()
                        suggestions = emptyList()

                        val placeId = prediction.placeId
                        val placeFields = listOf(Place.Field.NAME, Place.Field.LAT_LNG)
                        val request = FetchPlaceRequest.builder(placeId, placeFields).build()

                        placesClient.fetchPlace(request).addOnSuccessListener { response ->
                            val place = response.place
                            val name = place.name ?: ""
                            val location = place.latLng?.toString() ?: ""
                            selectedPlaceDetails = name to location
                        }
                    }
            ) {
                Text(
                    text = prediction.getFullText(null).toString(),
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp
                )
            }
        }

        selectedPlaceDetails?.let { (placeName, _) ->
            Spacer(modifier = Modifier.height(16.dp))

            val matched = firestoreProperties.filterValues {
                it.location.contains(placeName, ignoreCase = true)
            }

            Text(
                "Properties in $placeName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (matched.isEmpty()) {
                Text("No properties found in this location", color = Color.LightGray)
            } else {
                matched.forEach { (id, property) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (property.imageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = property.imageUrl,
                                    contentDescription = property.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .padding(bottom = 8.dp)
                                )
                            }
                            Text(property.name, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                            Text(property.price, color = MaterialTheme.colorScheme.primary)
                            Text("Location: ${property.location}", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    navController.navigate("property_details/$id")
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("View Details")
                            }
                        }
                    }
                }
            }
        }
    }
}






@OptIn(ExperimentalCoroutinesApi::class)
suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        addOnCompleteListener {
            if (it.isSuccessful) cont.resume(it.result, null)
            else cont.resumeWithException(it.exception ?: Exception("Unknown error"))
        }
    }





@Composable
fun ShortlistScreen() {
    val background = AppBackgroundBrush()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Shortlisted Properties", style = MaterialTheme.typography.headlineSmall, color = Color.White)

            if (shortlistedProperties.isEmpty()) {
                Text("No properties shortlisted yet.", color = Color.LightGray)
            } else {
                shortlistedProperties.forEach { property ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(property.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text("Location: ${property.location}")
                            Text("Price: ${property.price}", color = MaterialTheme.colorScheme.primary)
                        }
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

