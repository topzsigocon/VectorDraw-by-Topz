package com.vectordraw.minimal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.io.File
import kotlin.math.max
import kotlin.math.min

enum class ToolType { PICK, NODE, TEXT, RECT, ELLIPSE, PEN }
enum class FillType { SOLID, GRADIENT }
enum class Shaping { WELD, TRIM, INTERSECT }

data class VectorObject(
    val id: Int, var type: ToolType, var x: Float, var y: Float, var w: Float, var h: Float,
    var text: String = "VectorDraw by Topz", var fill: Color = Color(0xFF00E676),
    var fill2: Color = Color(0xFF121212), var fillType: FillType = FillType.SOLID,
    var visible: Boolean = true, var locked: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var selectedTool by remember { mutableStateOf(ToolType.PICK) }
            var objects by remember { mutableStateOf(listOf<VectorObject>()) }
            var selectedId by remember { mutableStateOf(-1) }
            var docUnit by remember { mutableStateOf("inch") }
            var textInput by remember { mutableStateOf("Topz") }
            var curId by remember { mutableStateOf(0) }

            fun weld(a: VectorObject, b: VectorObject): VectorObject {
                val nx = min(a.x, b.x); val ny = min(a.y, b.y)
                val nw = max(a.x+a.w, b.x+b.w) - nx; val nh = max(a.y+a.h, b.y+b.h) - ny
                return a.copy(x=nx, y=ny, w=nw, h=nh, id=curId++)
            }
            fun intersect(a: VectorObject, b: VectorObject): VectorObject? {
                val nx = max(a.x, b.x); val ny = max(a.y, b.y)
                val ex = min(a.x+a.w, b.x+b.w); val ey = min(a.y+a.h, b.y+b.h)
                if(ex>nx && ey>ny) return a.copy(x=nx, y=ny, w=ex-nx, h=ey-ny, id=curId++)
                return null
            }

            MaterialTheme(colorScheme = darkColorScheme()) {
                Column(Modifier.fillMaxSize().background(Color(0xFF121212))) {
                    // TOP - SHAPING BAR (Corel Style)
                    Row(Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).padding(6.dp), Arrangement.SpaceBetween) {
                        Row {
                            Button(onClick = {
                                val sel = objects.filter { it.id==selectedId || objects.indexOf(it)==0 }
                                if(sel.size>=2) { objects = listOf(weld(sel[0], sel[1])) + objects.filterNot { it.id==sel[0].id || it.id==sel[1].id } }
                            }) { Text("Weld") }
                            Spacer(Modifier.width(4.dp))
                            Button(onClick = {
                                val sel = objects.filter { it.id==selectedId }
                                if(sel.isNotEmpty() && objects.size>1) {
                                    val inter = intersect(objects[0], sel[0])
                                    if(inter!=null) objects = objects + inter
                                }
                            }) { Text("Intersect") }
                            Spacer(Modifier.width(4.dp))
                            Button(onClick = { if(selectedId!=-1) objects = objects.filterNot { it.id==selectedId }; selectedId=-1 }) { Text("Trim/Del") }
                        }
                        Button(onClick = {
                            val json = objects.joinToString(prefix="{\"unit\":\"$docUnit\",\"objects\":[", postfix="]}") {
                                "{\"id\":${it.id},\"type\":\"${it.type}\",\"x\":${it.x},\"y\":${it.y},\"w\":${it.w},\"h\":${it.h},\"text\":\"${it.text}\"}"
                            }
                            val dir = File(getExternalFilesDir(null), "projects"); dir.mkdirs()
                            File(dir, "vd4_${System.currentTimeMillis()}.vdproj").writeText(json)
                        }) { Text("Save .vdproj") }
                    }

                    Row(Modifier.fillMaxSize()) {
                        // LEFT TOOLS
                        Column(Modifier.width(60.dp).fillMaxHeight().background(Color(0xFF1E1E1E)).padding(4.dp)) {
                            listOf(ToolType.PICK, ToolType.NODE, ToolType.TEXT, ToolType.RECT, ToolType.ELLIPSE, ToolType.PEN).forEach { t ->
                                Button(onClick={selectedTool=t}, colors=ButtonDefaults.buttonColors(containerColor=if(selectedTool==t) Color(0xFF00E676) else Color.Transparent),
                                    modifier=Modifier.fillMaxWidth().height(42.dp).padding(bottom=4.dp), contentPadding=PaddingValues(0.dp)) {
                                    Text(t.name.take(2), color=Color.White, style=MaterialTheme.typography.labelSmall)
                                }
                            }
                            if(selectedTool==ToolType.TEXT) {
                                OutlinedTextField(value=textInput, onValueChange={textInput=it}, label={Text("Text", color=Color.White)}, modifier=Modifier.fillMaxWidth())
                            }
                        }

                        // CENTER CANVAS
                        Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF2A2A2A))) {
                            Canvas(Modifier.fillMaxSize()) {
                                drawRect(Color(0xFF121212), size=Size(size.width, 18f))
                                drawRect(Color(0xFF121212), size=Size(18f, size.height))
                                objects.forEach { obj ->
                                    if(!obj.visible) return@forEach
                                    val isSel = obj.id==selectedId
                                    val brush = if(obj.fillType==FillType.GRADIENT) Brush.linearGradient(listOf(obj.fill, obj.fill2)) else null
                                    when(obj.type) {
                                        ToolType.RECT -> if(brush!=null) drawRect(brush, Offset(obj.x,obj.y), Size(obj.w,obj.h)) else drawRect(obj.fill, Offset(obj.x,obj.y), Size(obj.w,obj.h), style=Stroke(if(isSel) 4f else 2f))
                                        ToolType.ELLIPSE -> if(brush!=null) drawOval(brush, Offset(obj.x,obj.y), Size(obj.w,obj.h)) else drawOval(obj.fill, Offset(obj.x,obj.y), Size(obj.w,obj.h), style=Stroke(if(isSel) 4f else 2f))
                                        ToolType.TEXT -> {
                                            drawContext.canvas.nativeCanvas.apply {
                                                drawText(obj.text, obj.x, obj.y+obj.h, android.graphics.Paint().apply { color=android.graphics.Color.WHITE; textSize=obj.h; isFakeBoldText=isSel })
                                            }
                                        }
                                        else -> {}
                                    }
                                    if(isSel) drawCircle(Color.White, 6f, Offset(obj.x, obj.y))
                                }
                            }
                            // Simple tap to create / select - using Box clickable overlay for Part 4 simplicity
                            Box(Modifier.fillMaxSize().clickable {
                                if(selectedTool==ToolType.RECT || selectedTool==ToolType.ELLIPSE) {
                                    objects = objects + VectorObject(curId++, selectedTool, 100f+curId*10f, 100f+curId*10f, 140f, 90f)
                                } else if(selectedTool==ToolType.TEXT) {
                                    objects = objects + VectorObject(curId++, ToolType.TEXT, 80f, 200f+curId*20f, 0f, 48f, text=textInput)
                                }
                            })
                        }

                        // RIGHT - LAYERS / OBJECT MANAGER (Corel Style)
                        Column(Modifier.width(130.dp).fillMaxHeight().background(Color(0xFF1E1E1E)).padding(6.dp)) {
                            Text("Objects", color=Color.White, style=MaterialTheme.typography.titleSmall)
                            Divider(color=Color.Gray, modifier=Modifier.padding(vertical=4.dp))
                            LazyColumn {
                                itemsIndexed(objects.reversed()) { idx, obj ->
                                    val realIdx = objects.size-1-idx
                                    Row(Modifier.fillMaxWidth().background(if(obj.id==selectedId) Color(0xFF333333) else Color.Transparent).clickable{selectedId=obj.id}.padding(6.dp),
                                        Arrangement.SpaceBetween) {
                                        Text("${obj.type.name.take(1)}:${obj.id}", color=if(obj.visible) Color.White else Color.Gray, style=MaterialTheme.typography.bodySmall)
                                        Row {
                                            Text(if(obj.visible) "👁" else "🚫", modifier=Modifier.clickable{
                                                objects = objects.toMutableList().also{it[realIdx]=obj.copy(visible=!obj.visible)}
                                            })
                                            Spacer(Modifier.width(4.dp))
                                            Text("↑", modifier=Modifier.clickable{
                                                if(realIdx < objects.size-1) {
                                                    val m = objects.toMutableList(); val tmp=m[realIdx]; m[realIdx]=m[realIdx+1]; m[realIdx+1]=tmp; objects=m
                                                }
                                            })
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Unit: $docUnit", color=Color.Gray, style=MaterialTheme.typography.bodySmall)
                            Text("Save keeps vector, not rasterized", color=Color(0xFF00E676), style=MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
