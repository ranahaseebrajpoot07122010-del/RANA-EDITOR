package com.rana.editor

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RanaEditorApp() }
    }
}

@Composable
fun RanaEditorApp() {
    var selectedMedia by remember { mutableStateOf<Uri?>(null) }
    var activeTab by remember { mutableStateOf("Edit") }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        selectedMedia = uri
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFB59CFF), background = Color(0xFF101018), surface = Color(0xFF1B1B28))) {
        Column(Modifier.fillMaxSize().background(Color(0xFF101018)).verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("RANA EDITOR", color = Color.White, fontSize = 29.sp)
            Text("Developed by Rana Haseeb", color = Color(0xFFB59CFF), fontSize = 13.sp)
            Spacer(Modifier.height(24.dp))
            Button(onClick = { picker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }) { Text("Import Photo / Video") }
            Spacer(Modifier.height(12.dp))
            Text(if (selectedMedia == null) "Apna photo ya video select karein" else "Media selected", color = Color.LightGray)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Edit", "Filters", "Adjust", "Enhance").forEach { tab ->
                    FilterChip(selected = activeTab == tab, onClick = { activeTab = tab }, label = { Text(tab) })
                }
            }
            Spacer(Modifier.height(18.dp))
            when (activeTab) {
                "Edit" -> {
                    Text("Editing tools", color = Color.White, fontSize = 20.sp)
                    Text("Trim • Split • Crop • Rotate • Speed • Text • Audio", color = Color.LightGray)
                    Text("Editing/export engine abhi development mein hai.", color = Color(0xFFFFCC80))
                }
                "Filters" -> {
                    Text("500 Filter Collection", color = Color.White, fontSize = 20.sp)
                    Text("Filter catalogue planned hai. Actual image/video filter rendering abhi connect nahi hui.", color = Color.LightGray)
                }
                "Adjust" -> {
                    Text("Adjust", color = Color.White, fontSize = 20.sp)
                    Text("Brightness: " + "%.1f".format(brightness), color = Color.White)
                    Slider(value = brightness, onValueChange = { brightness = it }, valueRange = -1f..1f)
                    Text("Contrast: " + "%.1f".format(contrast), color = Color.White)
                    Slider(value = contrast, onValueChange = { contrast = it }, valueRange = 0f..2f)
                    Text("Saturation: " + "%.1f".format(saturation), color = Color.White)
                    Slider(value = saturation, onValueChange = { saturation = it }, valueRange = 0f..2f)
                    Text("Controls UI ready; export processing abhi connected nahi.", color = Color(0xFFFFCC80))
                }
                "Enhance" -> {
                    Text("Quality Enhance", color = Color.White, fontSize = 20.sp)
                    Text("On-device AI enhancement model abhi add nahi hua. Is version mein Remini jaisi enhancement available nahi.", color = Color.LightGray)
                }
            }
            Spacer(Modifier.height(28.dp))
            Text("RANA EDITOR • Offline-first Android app", color = Color.Gray, fontSize = 12.sp)
        }
    }
}
