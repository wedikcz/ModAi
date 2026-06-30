package com.automod.ai.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildScreen() {
    var removeLicense by remember { mutableStateOf(true) }
    var bypassServer by remember { mutableStateOf(true) }
    var removeAds by remember { mutableStateOf(true) }
    var unlockPremium by remember { mutableStateOf(true) }
    var addHackMenu by remember { mutableStateOf(false) }
    
    // Hack menu features
    var godMode by remember { mutableStateOf(false) }
    var oneHitKill by remember { mutableStateOf(false) }
    var aimbot by remember { mutableStateOf(false) }
    var wallhack by remember { mutableStateOf(false) }
    var xray by remember { mutableStateOf(false) }
    var speedHack by remember { mutableStateOf(false) }
    var moneyHack by remember { mutableStateOf(true) }
    var xpMultiplier by remember { mutableStateOf("2.0") }
    
    var buildProgress by remember { mutableStateOf(0f) }
    var isBuilding by remember { mutableStateOf(false) }
    var buildStatus by remember { mutableStateOf("Ready") }
    
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Build progress
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "MOD BUILDER",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(Modifier.height(16.dp))
                
                if (isBuilding) {
                    LinearProgressIndicator(
                        progress = { buildProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        buildStatus,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    Text(
                        "READY TO BUILD",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        color = Color(0xFF00FF00)
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Mod options
        Text(
            "MOD OPTIONS",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(Modifier.height(8.dp))
        
        // Toggle switches
        ModToggle("Remove License Checks", removeLicense) { removeLicense = it }
        ModToggle("Bypass Server Validation", bypassServer) { bypassServer = it }
        ModToggle("Remove Ads", removeAds) { removeAds = it }
        ModToggle("Unlock Premium Features", unlockPremium) { unlockPremium = it }
        
        Spacer(Modifier.height(16.dp))
        
        // Hack menu section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "HACK MENU",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
            
            Switch(
                checked = addHackMenu,
                onCheckedChange = { addHackMenu = it }
            )
        }
        
        AnimatedVisibility(visible = addHackMenu) {
            Column {
                Spacer(Modifier.height(8.dp))
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "HACK FEATURES",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        
                        HackToggle("God Mode", godMode) { godMode = it }
                        HackToggle("One Hit Kill", oneHitKill) { oneHitKill = it }
                        HackToggle("Aimbot", aimbot) { aimbot = it }
                        HackToggle("Wallhack", wallhack) { wallhack = it }
                        HackToggle("X-Ray Vision", xray) { xray = it }
                        HackToggle("Speed Hack", speedHack) { speedHack = it }
                        HackToggle("Money/Currency Hack", moneyHack) { moneyHack = it }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = xpMultiplier,
                            onValueChange = { xpMultiplier = it },
                            label = { Text("XP Multiplier") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Build button
        Button(
            onClick = { /* start build process */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isBuilding,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (isBuilding) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Filled.Build, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "BUILD MOD APK",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ModToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.surfaceVariant,
        thickness = 0.5.dp
    )
}

@Composable
fun HackToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = Color(0xFF00FF00)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
