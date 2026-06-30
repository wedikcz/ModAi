package com.automod.ai.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.automod.ai.analyzer.DynamicAnalyzer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzeScreen() {
    var isAnalyzing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var stateText by remember { mutableStateOf("Ready") }
    val findings = remember { mutableStateListOf<String>() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Progress section
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
                    "ANALYSIS ENGINE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(Modifier.height(16.dp))
                
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(120.dp),
                        strokeWidth = 8.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    
                    Text(
                        "${(progress * 100).toInt()}%",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        stateText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    Icon(
                        Icons.Filled.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    
                    Text(
                        "READY",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        color = Color(0xFF00FF00)
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Findings log
        Text(
            "FINDINGS",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(Modifier.height(8.dp))
        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(findings) { finding ->
                FindingItem(finding)
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Controls
        Button(
            onClick = { /* start/stop analysis */ },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isAnalyzing
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("START ANALYSIS")
        }
    }
}

@Composable
fun FindingItem(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.FiberManualRecord,
                contentDescription = null,
                modifier = Modifier.size(8.dp),
                tint = Color(0xFF00FF00)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Color(0xFF00FF00)
            )
        }
    }
}
