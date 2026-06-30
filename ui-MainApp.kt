package com.automod.ai.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val navController = rememberNavController()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "AUTOMOD AI", 
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Home") },
                    selected = true,
                    onClick = { navController.navigate("home") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Analytics, contentDescription = null) },
                    label = { Text("Analyze") },
                    selected = false,
                    onClick = { navController.navigate("analyze") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Build, contentDescription = null) },
                    label = { Text("Build") },
                    selected = false,
                    onClick = { navController.navigate("build") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = { navController.navigate("settings") }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") { HomeScreen() }
            composable("analyze") { AnalyzeScreen() }
            composable("build") { BuildScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}

@Composable
fun HomeScreen() {
    var selectedPackage by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status karta
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "SYSTEM STATUS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                StatusRow("AI Engine", "ACTIVE", Color(0xFF00FF00))
                StatusRow("R2Frida", "CONNECTED", Color(0xFF00FF00))
                StatusRow("R2Ghidra", "READY", Color(0xFF00FF00))
                StatusRow("APKTool", "INSTALLED", Color(0xFF00FF00))
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Hlavní akce
        OutlinedTextField(
            value = selectedPackage,
            onValueChange = { selectedPackage = it },
            label = { Text("Package name or APK path") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
        )
        
        Spacer(Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { /* start analysis */ },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Analytics, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("AUTO ANALYZE")
            }
            
            OutlinedButton(
                onClick = { /* select APK file */ },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.FileUpload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("BROWSE")
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Rychlé akce
        Text(
            "QUICK ACTIONS",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(Modifier.height(8.dp))
        
        QuickActionCard("SSL Pinning Bypass", Icons.Filled.Lock, Color(0xFF00E5FF))
        QuickActionCard("License Removal", Icons.Filled.Verified, Color(0xFF00C853))
        QuickActionCard("Native Analysis", Icons.Filled.Memory, Color(0xFFD500F9))
        QuickActionCard("Memory Editor", Icons.Filled.Edit, Color(0xFFFF6D00))
    }
}

@Composable
fun StatusRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            value,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = color
        )
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
fun QuickActionCard(title: String, icon: ImageVector, color: Color) {
    Card(
        onClick = { /* action */ },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(Modifier.width(16.dp))
            Text(
                title,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        }
    }
}
