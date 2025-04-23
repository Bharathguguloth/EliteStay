import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.elitestay.model.Property
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

val shortlistedProperties = mutableStateListOf<Property>()

@Composable
fun PropertyDetailsScreen(propertyId: String, navController: NavController? = null) {
    var property by remember { mutableStateOf<Property?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }

    // Handle back action
    BackHandler(enabled = true) {
        navController?.popBackStack()
    }

    LaunchedEffect(propertyId) {
        try {
            val doc = FirebaseFirestore.getInstance()
                .collection("properties")
                .document(propertyId)
                .get()
                .await()
            property = doc.toObject(Property::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        property?.let {
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    val isShortlisted = remember { mutableStateOf(shortlistedProperties.contains(it)) }

                        //val isShortlisted = remember { mutableStateOf(shortlistedProperties.contains(it)) }

                    IconButton(onClick = {
                        if (isShortlisted.value) {
                            shortlistedProperties.remove(it)
                        } else {
                            shortlistedProperties.add(it)
                        }
                        isShortlisted.value = !isShortlisted.value
                    }) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Shortlist",
                            tint = if (isShortlisted.value) Color.Red else Color.Gray
                        )
                    }


                }

                if (it.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = it.imageUrl,
                        contentDescription = it.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(it.name, style = MaterialTheme.typography.headlineSmall)
                Text("Price: ${it.price}", style = MaterialTheme.typography.bodyLarge)
                Text("Location: ${it.location}", style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Book Now")
                }

                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        confirmButton = {
                            TextButton(onClick = { showDialog = false }) {
                                Text("OK")
                            }
                        },
                        title = { Text("Booking Confirmed") },
                        text = {
                            Text("Your booking has been confirmed!\nPlease pay after visiting.")
                        }
                    )
                }
            }
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Property not found", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
