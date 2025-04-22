package com.example.elitestay

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.elitestay.model.Property
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun PropertyDetailsScreen(propertyId: String) {
    var property by remember { mutableStateOf<Property?>(null) }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(propertyId) {
        val doc = db.collection("properties").document(propertyId).get().await()
        property = doc.toObject(Property::class.java)
    }

    property?.let {
        Column(modifier = Modifier.padding(16.dp)) {
            AsyncImage(
                model = it.imageUrl,
                contentDescription = it.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(it.name, style = MaterialTheme.typography.headlineSmall)
            Text("Price: ${it.price}", style = MaterialTheme.typography.bodyLarge)
            Text("Location: ${it.location}", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { /* Booking logic */ }) {
                Text("Book Now")
            }
        }
    } ?: run {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}