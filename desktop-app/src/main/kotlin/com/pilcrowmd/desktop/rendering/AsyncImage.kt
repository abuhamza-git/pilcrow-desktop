package com.pilcrowmd.desktop.rendering

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import javax.imageio.ImageIO
import java.io.File
import java.nio.file.Path

@Composable
fun AsyncMarkdownImage(url: String, modifier: Modifier = Modifier) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    var error by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        withContext(Dispatchers.IO) {
            try {
                val image = if (url.startsWith("http://") || url.startsWith("https://")) {
                    ImageIO.read(URL(url))
                } else if (url.startsWith("file://")) {
                    ImageIO.read(File(java.net.URI(url)))
                } else {
                    ImageIO.read(File(url))
                }
                
                if (image != null) {
                    bitmap = image.toComposeImageBitmap()
                } else {
                    error = true
                }
            } catch (e: Exception) {
                error = true
            }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = "Markdown Image",
                contentScale = ContentScale.Inside
            )
        } else if (error) {
            Text("[Failed to load image: $url]", color = MaterialTheme.colorScheme.error)
        } else {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
    }
}
