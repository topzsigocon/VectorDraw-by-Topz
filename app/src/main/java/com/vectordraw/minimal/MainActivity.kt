package com.vectordraw.minimal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

data class VectorDoc(val width: Float, val height: Float, val unit: String, val objects: List<String>)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var width by remember { mutableStateOf("10") }
            var height by remember { mutableStateOf("8") }
            var unit by remember { mutableStateOf("inch") } // inch, cm, mm
            
            MaterialTheme {
                Column(Modifier.padding(16.dp)) {
                    Text("VectorDraw Minimal - New Document", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(16.dp))
                    Row {
                        OutlinedTextField(value = width, onValueChange = { width = it }, label = { Text("Width") }, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Height") }, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    // Minimalist Unit Selector
                    Row {
                        listOf("inch", "cm", "mm").forEach { u ->
                            FilterChip(selected = unit==u, onClick = { unit = u }, label = { Text(u) }, modifier = Modifier.padding(end=8.dp))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = {
                        // SAVE WITHOUT RASTERIZING - JSON lang
                        val doc = VectorDoc(width.toFloatOrNull()?:10f, height.toFloatOrNull()?:8f, unit, emptyList())
                        val json = "{\"width\":${doc.width},\"height\":${doc.height},\"unit\":\"${doc.unit}\",\"objects\":[]}"
                        val dir = File(getExternalFilesDir(null), "projects")
                        dir.mkdirs()
                        File(dir, "project_${System.currentTimeMillis()}.vdproj").writeText(json)
                    }) {
                        Text("Create & Save Vector Project (.vdproj)")
                    }
                    Text("Note: File saved as editable vector JSON, not rasterized image.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
