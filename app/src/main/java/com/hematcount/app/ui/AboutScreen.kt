package com.hematcount.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.outlined.Biotech
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hematcount.app.BuildConfigInfo
import com.hematcount.app.model.CellType

private const val CONCEPT_PHONE = "919373133332"
private const val TECH_PHONE = "919370111449"
private const val WEBSITE = "https://carehospital.in"
private const val RND_PAGE = "https://carehospital.in/products.html"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        containerColor = scheme.surface,
        contentColor = scheme.onSurface,
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.surface, titleContentColor = scheme.onSurface, navigationIconContentColor = scheme.onSurface,
                ),
            )
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // ---------------- hero
                item {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            Modifier.fillMaxWidth()
                                .background(Brush.linearGradient(listOf(Color(0xFF0B3954), Color(0xFF0E7490))))
                                .padding(24.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    Modifier.size(84.dp).clip(CircleShape).background(Color(0x33FFFFFF)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Box(Modifier.size(66.dp).clip(CircleShape).background(Color(0xFFF4F7FB)), contentAlignment = Alignment.Center) {
                                        CellIllustration(CellType.NEUTROPHIL, Modifier.size(54.dp))
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Text("HematCount", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                                Spacer(Modifier.height(4.dp))
                                Text("Precision in Every Cell",
                                    style = MaterialTheme.typography.titleMedium, color = Color(0xFF9FF0E1), textAlign = TextAlign.Center)
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "Haematology Differential Cell Counter",
                                    style = MaterialTheme.typography.bodyMedium, color = Color(0xF2FFFFFF), textAlign = TextAlign.Center,
                                )
                                Text("Version ${BuildConfigInfo.versionName(context)}",
                                    style = MaterialTheme.typography.labelMedium, color = Color(0xCCFFFFFF))
                            }
                        }
                    }
                }

                // ---------------- dedication
                item {
                    Surface(
                        color = scheme.secondaryContainer, contentColor = scheme.onSecondaryContainer,
                        shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("DEDICATION", style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSecondaryContainer.copy(alpha = 0.8f))
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Thoughtfully conceived and presented by Dr. Imran Khan as a complimentary resource " +
                                    "for medical professionals and students.",
                                style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                // ---------------- concept & distribution
                item {
                    PersonCard(
                        icon = Icons.Outlined.School,
                        role = "Concept & Distribution",
                        name = "Dr. Imran Khan",
                        lines = listOf(
                            "Department of Pathology",
                            "B. J. Government Medical College, Pune",
                        ),
                        phoneDisplay = "+91 93731 33332",
                        phoneDigits = CONCEPT_PHONE,
                        website = null,
                    )
                }

                // ---------------- technical team
                item {
                    PersonCard(
                        icon = Icons.Outlined.Engineering,
                        role = "Technical Development",
                        name = "Dr. Abrar Khan",
                        lines = listOf("Technical Team", "Care Hospital, Chikhli"),
                        phoneDisplay = "+91 93701 11449",
                        phoneDigits = TECH_PHONE,
                        website = WEBSITE,
                    )
                }

                // ---------------- R&D
                item {
                    SectionCard("Explore Our Portfolio") {
                        Row(verticalAlignment = Alignment.Top) {
                            IconBadge(Icons.Filled.Science)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Discover our complete range of clinical and healthcare solutions in the " +
                                    "Research & Development section at carehospital.in.",
                                style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { openUrl(context, RND_PAGE) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.Biotech, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                            Text("Explore Research & Development")
                        }
                    }
                }

                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Crafted for the laboratory. Built for learning.",
                            style = MaterialTheme.typography.titleSmall, color = scheme.primary, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(4.dp))
                        Text("© 2026 HematCount. All rights reserved.", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonCard(
    icon: ImageVector,
    role: String,
    name: String,
    lines: List<String>,
    phoneDisplay: String,
    phoneDigits: String,
    website: String?,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    SectionCard(role) {
        Row(verticalAlignment = Alignment.Top) {
            IconBadge(icon)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleLarge, color = scheme.onSurface)
                lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant) }
                Spacer(Modifier.height(4.dp))
                Text(phoneDisplay, style = MaterialTheme.typography.bodyLarge, color = scheme.primary)
                if (website != null) Text(website.removePrefix("https://"), style = MaterialTheme.typography.bodyMedium, color = scheme.primary)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { openUrl(context, "tel:+$phoneDigits") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Call, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Call")
            }
            FilledTonalButton(onClick = { openUrl(context, "https://wa.me/$phoneDigits") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Forum, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("WhatsApp")
            }
            if (website != null) {
                OutlinedButton(onClick = { openUrl(context, website) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Language, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Website")
                }
            }
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(scheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(22.dp)) }
}

private fun openUrl(context: Context, url: String) {
    val intent = if (url.startsWith("tel:")) Intent(Intent.ACTION_DIAL, Uri.parse(url))
    else Intent(Intent.ACTION_VIEW, Uri.parse(url))
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app available to open this", Toast.LENGTH_SHORT).show()
    }
}
