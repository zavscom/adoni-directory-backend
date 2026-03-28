package com.zavscom.adonidirectory.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zavscom.adonidirectory.data.local.entity.BusinessEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessDetailScreen(
    business: BusinessEntity?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Business") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { inner ->
        when {
            business == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        business.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        business.category,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    business.subCategory?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(business.address, style = MaterialTheme.typography.bodyLarge)
                    val loc = listOf(business.area, business.pincode, business.city, business.state)
                        .filter { it.isNotBlank() }
                        .joinToString(", ")
                    if (loc.isNotEmpty()) {
                        Text(loc, style = MaterialTheme.typography.bodyMedium)
                    }

                    business.phone?.let { phone ->
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Call") }
                    }
                    business.whatsapp?.let { wa ->
                        Button(
                            onClick = {
                                val digits = wa.filter { it.isDigit() }
                                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$digits")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("WhatsApp") }
                    }
                    business.website?.let { url ->
                        val fixed = if (url.startsWith("http://", true) || url.startsWith("https://", true)) {
                            url
                        } else {
                            "https://$url"
                        }
                        Button(
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fixed)))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Website") }
                    }
                }
            }
        }
    }
}
