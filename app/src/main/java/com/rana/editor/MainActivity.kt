package com.rana.editor

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RanaEditorApp() }
    }
}

private data class Look(val name: String, val saturation: Float = 1f, val warmth: Float = 0f, val contrast: Float = 1f)

private val looks = listOf(
    Look("Original"), Look("Vivid", 1.45f, 0f, 1.12f), Look("Soft", 0.82f, 0f, 0.94f),
    Look("Warm", 1.12f, 0.18f, 1.04f), Look("Cool", 1.08f, -0.16f, 1.04f),
    Look("Mono", 0f), Look("Vintage", 0.72f, 0.12f, 0.88f), Look("Cinema", 1.18f, -0.04f, 1.22f),
    Look("Fade", 0.78f, 0.04f, 0.82f), Look("Bright", 1.08f, 0.02f, 1.08f),
    Look("Portrait", 1.12f, 0.08f, 0.98f), Look("Dramatic", 1.32f, -0.02f, 1.38f)
)

class EditorValues(val brightness: Float, val contrast: Float, val saturation: Float, val look: Look) {
    fun matrix(): ComposeColorMatrix {
        val s = look.saturation * saturation
        val c = contrast * look.contrast
        val b = (brightness + look.warmth * 0.15f) * 255f
        val t = (1f - c) * 128f + b
        val base = floatArrayOf(
            c * (0.213f + 0.787f*s), c * (0.715f - 0.715f*s), c * (0.072f - 0.072f*s), 0f, t + look.warmth*18f,
            c * (0.213f - 0.213f*s), c * (0.715f + 0.285f*s), c * (0.072f - 0.072f*s), 0f, t,
            c * (0.213f - 0.213f*s), c * (0.715f - 0.715f*s), c * (0.072f + 0.928f*s), 0f, t - look.warmth*18f,
            0f, 0f, 0f, 1f, 0f
        )
        return ComposeColorMatrix(base)
    }
}

@Composable
fun RanaEditorApp() {
    var selectedMedia by remember { mutableStateOf<Uri?>(null) }
    var activeTab by remember { mutableStateOf("Edit") }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var selectedLook by remember { mutableStateOf(looks.first()) }
    var message by remember { mutableStateOf("Photo import karein aur editing shuru karein") }
    var exporting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        selectedMedia = uri
        message = if (uri == null) "Koi photo select nahi hui" else "Photo ready — Adjust ya Filters tab use karein"
    }
    val values = EditorValues(brightness, contrast, saturation, selectedLook)
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFB59CFF), background = Color(0xFF101018), surface = Color(0xFF1B1B28))) {
        Column(Modifier.fillMaxSize().background(Color(0xFF101018)).verticalScroll(rememberScrollState()).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("RANA EDITOR", color = Color.White, fontSize = 28.sp)
            Text("Developed by Rana Haseeb", color = Color(0xFFB59CFF), fontSize = 13.sp)
            Spacer(Modifier.height(18.dp))
            Button(onClick = { picker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("＋ Import Photo") }
            Spacer(Modifier.height(12.dp))
            if (selectedMedia != null) {
                AsyncImage(model = selectedMedia, contentDescription = "Selected photo", modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 340.dp), colorFilter = ColorFilter.colorMatrix(values.matrix()))
            } else {
                Box(Modifier.fillMaxWidth().height(210.dp).background(Color(0xFF202030), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                    Text("Your photo preview will appear here", color = Color.LightGray)
                }
            }
            Text(message, color = Color.LightGray, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Edit", "Filters", "Adjust", "Enhance").forEach { tab ->
                    FilterChip(selected = activeTab == tab, onClick = { activeTab = tab }, label = { Text(tab) })
                }
            }
            Spacer(Modifier.height(14.dp))
            when (activeTab) {
                "Edit" -> {
                    Text("Photo tools", color = Color.White, fontSize = 20.sp)
                    Text("Import • Live preview • Reset • Save edited photo", color = Color.LightGray)
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = {
                        brightness = 0f; contrast = 1f; saturation = 1f; selectedLook = looks.first()
                        message = "Adjustments reset"
                    }) { Text("Reset all adjustments") }
                    Button(enabled = selectedMedia != null && !exporting, onClick = {
                        val uri = selectedMedia ?: return@Button
                        exporting = true
                        scope.launch {
                            val result = withContext(Dispatchers.IO) { saveEditedPhoto(context, uri, values) }
                            exporting = false
                            message = if (result != null) "Saved to Pictures/RANA EDITOR" else "Save failed — try another photo"
                        }
                    }) { Text(if (exporting) "Saving..." else "Save edited photo") }
                }
                "Filters" -> {
                    Text("RANA Looks", color = Color.White, fontSize = 20.sp)
                    Text("12 live preview looks — tap to apply", color = Color.LightGray, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    looks.chunked(3).forEach { line ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            line.forEach { look ->
                                FilterChip(selected = selectedLook == look, onClick = { selectedLook = look }, label = { Text(look.name, fontSize = 11.sp) })
                            }
                        }
                    }
                    Text("500-filter collection is planned; these 12 looks currently render real preview effects.", color = Color(0xFFFFCC80), fontSize = 11.sp)
                }
                "Adjust" -> {
                    Text("Live Adjustments", color = Color.White, fontSize = 20.sp)
                    Text("Brightness  %.2f".format(brightness), color = Color.White)
                    Slider(value = brightness, onValueChange = { brightness = it }, valueRange = -1f..1f)
                    Text("Contrast  %.2f".format(contrast), color = Color.White)
                    Slider(value = contrast, onValueChange = { contrast = it }, valueRange = 0f..2f)
                    Text("Saturation  %.2f".format(saturation), color = Color.White)
                    Slider(value = saturation, onValueChange = { saturation = it }, valueRange = 0f..2f)
                }
                "Enhance" -> {
                    Text("Quality Enhance", color = Color.White, fontSize = 20.sp)
                    Text("AI upscaling model abhi integrate nahi hua. Is build mein Remini-level enhancement ka dawa nahi hai.", color = Color.LightGray)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("RANA EDITOR • Offline photo editing", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

private fun saveEditedPhoto(context: android.content.Context, uri: Uri, values: EditorValues): Uri? {
    return try {
        val source = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val matrix = ColorMatrix()
        matrix.set(values.matrix().values)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(matrix) }
        canvas.drawBitmap(source, 0f, 0f, paint)
        val name = "RANA_EDITED_${System.currentTimeMillis()}.jpg"
        val content = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/RANA EDITOR")
        }
        val saved = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, content) ?: return null
        val ok = context.contentResolver.openOutputStream(saved)?.use { output.compress(Bitmap.CompressFormat.JPEG, 95, it) } ?: false
        source.recycle()
        output.recycle()
        if (ok) saved else null
    } catch (_: Exception) { null }
}
