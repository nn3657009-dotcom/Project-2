package com.shinnie.optimize

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.delay

private val Bg = Color(0xFF0E0F14)
private val Card = Color(0xFF181A22)
private val Accent = Color(0xFFFF6B1A)
private val AccentSoft = Color(0xFFFFA24D)
private val TextDim = Color(0xFF9AA0B0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val license = LicenseManager(this)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(primary = Accent, background = Bg, surface = Card)
            ) {
                var activated by remember { mutableStateOf(license.isActivated()) }
                Box(Modifier.fillMaxSize().background(Bg)) {
                    if (activated) Dashboard { license.deactivate(); activated = false }
                    else ActivationScreen(license) { activated = true }
                }
            }
        }
    }
}

@Composable
fun Header() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "SHINNIE OPTIMIZE",
            fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Accent, letterSpacing = 3.sp
        )
        Text("Built by Shinnie", fontSize = 12.sp, color = TextDim)
    }
}

@Composable
fun ActivationScreen(license: LicenseManager, onActivated: () -> Unit) {
    val context = LocalContext.current
    var key by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Header()
        Spacer(Modifier.height(40.dp))
        Text("Enter your activation key", color = Color.White, fontSize = 16.sp)
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = key,
            onValueChange = { key = it; error = false },
            singleLine = true,
            isError = error,
            placeholder = { Text("Enter your key") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        if (error) Text("Invalid key. Please check and try again.", color = Color(0xFFFF5C5C), fontSize = 12.sp)
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                if (license.activate(key)) {
                    Toast.makeText(context, "Activated!", Toast.LENGTH_SHORT).show()
                    onActivated()
                } else error = true
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Activate", fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun Dashboard(onLogout: () -> Unit) {
    val context = LocalContext.current
    var stats by remember { mutableStateOf(Optimizer.readStats(context)) }
    var boosting by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<String?>(null) }

    // refresh stats every 3 seconds
    LaunchedEffect(Unit) {
        while (true) { stats = Optimizer.readStats(context); delay(3000) }
    }
    // boost animation + action
    LaunchedEffect(boosting) {
        if (boosting) {
            delay(1500)
            val freed = Optimizer.boost(context)
            stats = Optimizer.readStats(context)
            lastResult = "Done! ~$freed MB freed"
            boosting = false
        }
    }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Header()
        Spacer(Modifier.height(24.dp))

        StatCard("RAM", stats.ramUsedPercent, "${stats.ramFreeMb} MB free")
        Spacer(Modifier.height(12.dp))
        StatCard("Storage", stats.storageUsedPercent, "used")
        Spacer(Modifier.height(12.dp))
        StatCard(
            "Battery", stats.batteryPercent,
            if (stats.isCharging) "charging" else "on battery"
        )

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = { if (!boosting) { lastResult = null; boosting = true } },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                Modifier.fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(Accent, AccentSoft))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (boosting) "Optimizing..." else "BOOST NOW",
                    color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp
                )
            }
        }
        lastResult?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = AccentSoft, fontSize = 13.sp)
        }

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    if (!Optimizer.launchGame(context))
                        Toast.makeText(context, "Free Fire is not installed", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Launch Game") }
            OutlinedButton(
                onClick = { Optimizer.openDndSettings(context) },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Do Not Disturb") }
        }

        Spacer(Modifier.weight(1f))
        TextButton(onClick = onLogout) { Text("Remove key", color = TextDim, fontSize = 12.sp) }
    }
}

@Composable
fun StatCard(title: String, percent: Int, subtitle: String) {
    val progress by animateFloatAsState(percent / 100f, tween(600), label = "progress")
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Card).padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("$percent%", color = Accent, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = Accent, trackColor = Color(0xFF2A2D38)
        )
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = TextDim, fontSize = 12.sp)
    }
}
