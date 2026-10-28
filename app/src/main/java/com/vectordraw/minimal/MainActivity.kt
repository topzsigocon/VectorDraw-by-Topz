package com.vectordraw.minimal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import java.io.File

// --- MODELS - PURE VECTOR, HINDI RASTER ---
enum class ToolType { PICK, PEN, RECT, ELLIPSE }
data class VectorObject(val id: Int, val type: ToolType, val x: Float, val y: Float, val w: Float, val h: Float, val fill: Color = Color(0xFF00E676))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var selectedTool by remember { mutableStateOf(ToolType.PICK) }
            var objects by remember { mutableStateOf(listOf<VectorObject>()) }
            var currentId by remember { mutableStateOf(0) }
            var docUnit by remember { mutableStateOf("inch") }

            MaterialTheme(colorScheme = darkColorScheme()) {
                Column(Modifier.fillMaxSize().background(Color(0xFF121212))) {
                    // TOP PROPERTY BAR
                    Row(Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Doc: 10 x 8 $docUnit | Zoom: 100% | ${objects.size} objects", color = Color.White)
                        Button(onClick = {
                            // SAVE AS VECTOR JSON - HINDI RASTER
                            val json = objects.joinToString(prefix="{\"unit\":\"$docUnit\",\"objects\":[", postfix="]}") { 
                                "{\"type\":\"${it.type}\",\"x\":${it.x},\"y\":${it.y},\"w\":${it.w},\"h\":${it.h}}" 
                            }
                            val dir = File(getExternalFilesDir(null), "projects")
                            dir.mkdirs()
                            File(dir, "vectordraw_${System.currentTimeMillis()}.vdproj").writeText(json)
                        }) { Text("Save .vdproj") }
                    }

                    Row(Modifier.fillMaxSize()) {
                        // LEFT - MINIMALIST TOOL DOCK
                        Column(Modifier.width(64.dp).fillMaxHeight().background(Color(0xFF1E1E1E)).padding(8.dp)) {
                            ToolButton(ToolType.PICK, selectedTool) { selectedTool = it }
                            ToolButton(ToolType.RECT, selectedTool) { selectedTool = it }
                            ToolButton(ToolType.ELLIPSE, selectedTool) { selectedTool = it }
                            ToolButton(ToolType.PEN, selectedTool) { selectedTool = it }
                            Divider(Modifier.padding(vertical = 12.dp), color = Color.Gray)
                            // Unit Switcher - Actual Size Feature
                            listOf("inch","cm").forEach { u ->
                                FilterChip(selected = docUnit==u, onClick = { docUnit=u }, label = { Text(u, color=Color.White) }, modifier = Modifier.padding(bottom=4.dp))
                            }
                        }

                        // CENTER - INFINITE CANVAS
                        var startPos by remember { mutableStateOf(Offset.Zero) }
                        Canvas(modifier = Modifier.fillMaxSize().background(Color(0xFF2A2A2A))
                            .pointerInput(selectedTool) {
                                detectDragGestures(
                                    onDragStart = { startPos = it },
                                    onDragEnd = {
                                        if(selectedTool == ToolType.RECT || selectedTool == ToolType.ELLIPSE) {
                                            // wag gumawa pag sobrang liit
                                        }
                                    }
                                ) { change, dragAmount ->
                                    if (selectedTool == ToolType.RECT || selectedTool == ToolType.ELLIPSE) {
                                        // preview lang, sa drag end natin finalize
                                    }
                                }
                            }
                            .pointerInput(selectedTool) {
                                awaitPointerEventScope {
                                    while(true) {
                                        val event = awaitPointerEvent()
                                        if(event.type == androidx.compose.ui.input.pointer.PointerEventType.Release) {
                                            val end = event.changes.first().position
                                            val w = end.x - startPos.x
                                            val h = end.y - startPos.y
                                            if (kotlin.math.abs(w) > 20 && kotlin.math.abs(h) > 20 && (selectedTool == ToolType.RECT || selectedTool == ToolType.ELLIPSE)) {
                                                objects = objects + VectorObject(currentId++, selectedTool, startPos.x, startPos.y, w, h)
                                            }
                                        }
                                    }
                                }
                            }
                        ) {
                            // RULER LINES (simple)
                            drawRect(Color(0xFF121212), topLeft = Offset(0f,0f), size = Size(size.width, 20f))
                            drawRect(Color(0xFF121212), topLeft = Offset(0f,0f), size = Size(20f, size.height))
                            
                            // DRAW ALL VECTOR OBJECTS - HINDI RASTER
                            objects.forEach { obj ->
                                when(obj.type) {
                                    ToolType.RECT -> drawRect(color = obj.fill, topLeft = Offset(obj.x, obj.y), size = Size(obj.w, obj.h), style = Stroke(2f))
                                    ToolType.ELLIPSE -> drawOval(color = obj.fill, topLeft = Offset(obj.x, obj.y), size = Size(obj.w, obj.h), style = Stroke(2f))
                                    else -> {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolButton(type: ToolType, selected: ToolType, onSelect: (ToolType)->Unit) {
    val isSelected = type == selected
    Button(
        onClick = { onSelect(type) },
        colors = ButtonDefaults.buttonColors(containerColor = if(isSelected) Color(0xFF00E676) else Color.Transparent),
        modifier = Modifier.fillMaxWidth().padding(bottom=8.dp).height(48.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Canvas(Modifier.size(24.dp)) {
            // MINIMALIST LINE ICONS - 2px stroke, white lines
            when(type) {
                ToolType.PICK -> { // Arrow icon
                    drawLine(Color.White, Offset(4f,4f), Offset(20f,12f), strokeWidth=2f)
                    drawLine(Color.White, Offset(20f,12f), Offset(12f,12f), strokeWidth=2f)
                    drawLine(Color.White, Offset(12f,12f), Offset(12f,20f), strokeWidth=2f)
                }
                ToolType.RECT -> drawRect(Color.White, style=Stroke(2f), size=Size(18f,18f), topLeft=Offset(3f,3f))
                ToolType.ELLIPSE -> drawOval(Color.White, style=Stroke(2f), size=Size(18f,18f), topLeft=Offset(3f,3f))
                ToolType.PEN -> { // Pen bezier
                    drawLine(Color.White, Offset(4f,20f), Offset(12f,4f), strokeWidth=2f)
                    drawLine(Color.White, Offset(12f,4f), Offset(20f,12f), strokeWidth=2f)
                }
            }
        }
    }
}
