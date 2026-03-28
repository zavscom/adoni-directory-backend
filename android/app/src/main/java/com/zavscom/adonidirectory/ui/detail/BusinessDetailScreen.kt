package com.zavscom.adonidirectory.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
                title = { Text("Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
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
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            else -> {
                val pageScroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner)
                        .padding(horizontal = 20.dp)
                        .verticalScroll(pageScroll),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = business.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    SuggestionChip(
                        onClick = {},
                        enabled = false,
                        label = {
                            Text(
                                business.category,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            disabledLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        border = null,
                    )
                    business.subCategory?.takeIf { it.isNotBlank() }?.let { sub ->
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Address",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (business.address.isNotBlank()) {
                                Text(
                                    text = business.address,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                            val loc = listOf(business.area, business.pincode, business.city, business.state)
                                .filter { it.isNotBlank() }
                                .joinToString(", ")
                            if (loc.isNotEmpty()) {
                                Text(
                                    text = loc,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    val hasContact =
                        !business.phone.isNullOrBlank() ||
                            !business.whatsapp.isNullOrBlank() ||
                            !business.website.isNullOrBlank()
                    if (hasContact) {
                        val actionsScroll = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(actionsScroll),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            business.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                                FilledTonalButton(
                                    onClick = {
                                        val intent = Intent(
                                            Intent.ACTION_DIAL,
                                            Uri.parse("tel:${Uri.encode(phone)}"),
                                        )
                                        context.startActivity(intent)
                                    },
                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                                    shape = MaterialTheme.shapes.large,
                                ) {
                                    Text("Call")
                                }
                            }
                            business.whatsapp?.takeIf { it.isNotBlank() }?.let { wa ->
                                FilledTonalButton(
                                    onClick = {
                                        val digits = wa.filter { it.isDigit() }
                                        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$digits")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    },
                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                                    shape = MaterialTheme.shapes.large,
                                ) {
                                    Text("WhatsApp")
                                }
                            }
                            business.website?.takeIf { it.isNotBlank() }?.let { url ->
                                val fixed =
                                    if (url.startsWith("http://", true) || url.startsWith("https://", true)) {
                                        url
                                    } else {
                                        "https://$url"
                                    }
                                FilledTonalButton(
                                    onClick = {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fixed)))
                                    },
                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                                    shape = MaterialTheme.shapes.large,
                                ) {
                                    Text("Website")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
