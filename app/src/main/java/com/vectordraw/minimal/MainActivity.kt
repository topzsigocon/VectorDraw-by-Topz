package com.vectordraw.minimal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectDragGestures
import java.io.File

enum class ToolType { PICK, NODE, PEN, RECT, ELLIPSE }
enum class FillType { SOLID, GRADIENT }
data class Node(val x: Float, val y: Float, var hx1: Float=0f, var hy1: Float=0f, var hx2: Float=0f, var hy2: Float=0f)
data class VectorObject(
    val id: Int, val type: ToolType, var x: Float, var y: Float, var w: Float, var h: Float,
    var fill: Color = Color(0xFF00E676), var fill2: Color = Color(0xFF121212),
    var fillType: FillType = FillType.SOLID, var nodes: List<Node> = emptyList(),
    var selectedNode: Int = -1
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var selectedTool by remember { mutableStateOf(ToolType.PICK) }
            var objects by remember { mutableStateOf(listOf<VectorObject>()) }
            var selectedId by remember { mutableStateOf(-1) }
            var docUnit by remember { mutableStateOf("inch") }
            var fillType by remember { mutableStateOf(FillType.SOLID) }
            var curId by remember { mutableStateOf(0) }

            MaterialTheme(colorScheme = darkColorScheme()) {
                Column(Modifier.fillMaxSize().background(Color(0xFF121212))) {
                    // TOP BAR
                    Row(Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).padding(8.dp), Arrangement.SpaceBetween) {
                        Text("VectorDraw by Topz | ${docUnit} | Sel: $selectedId", color=Color.White)
                        Row {
                            Button(onClick = { fillType = if(fillType==FillType.SOLID) FillType.GRADIENT else FillType.SOLID }) {
                                Text(if(fillType==FillType.SOLID) "Solid" else "Gradient")
                            }
                            Spacer(Modifier.width(4.dp))
                            Button(onClick = {
                                val json = objects.joinToString(prefix="{\"unit\":\"$docUnit\",\"objects\":[", postfix="]}") {
                                    "{\"id\":${it.id},\"type\":\"${it.type}\",\"x\":${it.x},\"y\":${it.y},\"w\":${it.w},\"h\":${it.h},\"fillType\":\"${it.fillType}\"}"
                                }
                                val dir = File(getExternalFilesDir(null), "projects"); dir.mkdirs()
                                File(dir, "vd_topz_${System.currentTimeMillis()}.vdproj").writeText(json)
                            }) { Text("Save") }
                        }
                    }
                    Row(Modifier.fillMaxSize()) {
                        // LEFT TOOLS - Minimalist
                        Column(Modifier.width(64.dp).fillMaxHeight().background(Color(0xFF1E1E1E)).padding(6.dp)) {
                            listOf(ToolType.PICK, ToolType.NODE, ToolType.RECT, ToolType.ELLIPSE, ToolType.PEN).forEach { t ->
                                Button(onClick = { selectedTool = t },
                                    colors = ButtonDefaults.buttonColors(containerColor = if(selectedTool==t) Color(0xFF00E676) else Color.Transparent),
                                    modifier = Modifier.fillMaxWidth().height(44.dp).padding(bottom=6.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text(t.name.take(1), color=Color.White) }
                            }
                            Divider(color=Color.Gray, modifier=Modifier.padding(vertical=8.dp))
                            Text("Unit", color=Color.Gray); 
                            listOf("inch","cm","mm").forEach { u ->
                                FilterChip(selected=docUnit==u, onClick={docUnit=u}, label={Text(u, color=Color.White)})
                            }
                        }
                        // CANVAS WITH NODE EDITING
                        var dragStart by remember { mutableStateOf(Offset.Zero) }
                        Canvas(Modifier.fillMaxSize().background(Color(0xFF2A2A2A))
                            .pointerInput(selectedTool, selectedId) {
                                detectDragGestures(
                                    onDragStart = { dragStart = it },
                                    onDragEnd = {
                                        if(selectedTool==ToolType.NODE && selectedId!=-1) {
                                            // node editing done
                                        }
                                    }
                                ) { change, _ ->
                                    if(selectedTool==ToolType.NODE && selectedId!=-1) {
                                        // Move node or bezier handle
                                        val idx = objects.indexOfFirst { it.id==selectedId }
                                        if(idx!=-1) {
                                            val obj = objects[idx]
                                            if(obj.selectedNode!=-1) {
                                                // move bezier handle
                                                val newNodes = obj.nodes.toMutableList()
                                                val n = newNodes[obj.selectedNode]
                                                newNodes[obj.selectedNode] = n.copy(hx2 = change.position.x - n.x, hy2 = change.position.y - n.y)
                                                objects = objects.toMutableList().also { it[idx] = obj.copy(nodes=newNodes) }
                                            }
                                        }
                                    }
                                }
                            }
                        ) {
                            // Rulers
                            drawRect(Color(0xFF121212), size=Size(size.width, 18f))
                            drawRect(Color(0xFF121212), size=Size(18f, size.height))

                            objects.forEach { obj ->
                                val isSel = obj.id==selectedId
                                val brush = if(obj.fillType==FillType.GRADIENT) Brush.linearGradient(listOf(obj.fill, obj.fill2)) else null
                                
                                when(obj.type) {
                                    ToolType.RECT -> {
                                        if(brush!=null) drawRect(brush=brush, topLeft=Offset(obj.x,obj.y), size=Size(obj.w,obj.h))
                                        else drawRect(color=obj.fill, topLeft=Offset(obj.x,obj.y), size=Size(obj.w,obj.h), style=Stroke(if(isSel) 3f else 2f))
                                        if(isSel && selectedTool==ToolType.NODE) {
                                            // Draw nodes + bezier handles (Corel style)
                                            obj.nodes.forEach { node ->
                                                drawCircle(Color.White, radius=5f, center=Offset(node.x, node.y))
                                                drawCircle(Color(0xFF00E676), radius=3f, center=Offset(node.x+node.hx2, node.y+node.hy2))
                                                drawLine(Color.Gray, Offset(node.x,node.y), Offset(node.x+node.hx2, node.y+node.hy2), strokeWidth=1f)
                                            }
                                        }
                                    }
                                    ToolType.ELLIPSE -> {
                                        if(brush!=null) drawOval(brush=brush, topLeft=Offset(obj.x,obj.y), size=Size(obj.w,obj.h))
                                        else drawOval(color=obj.fill, topLeft=Offset(obj.x,obj.y), size=Size(obj.w,obj.h), style=Stroke(if(isSel) 3f else 2f))
                                    }
                                    else -> {}
                                }
                                if(isSel) drawRect(Color.White, topLeft=Offset(obj.x-4,obj.y-4), size=Size(8f,8f)) // selection handle
                            }
                        }
                    }
                }
                // Canvas tap to create
                LaunchedEffect(selectedTool) {}
            }
        }
    }
}
