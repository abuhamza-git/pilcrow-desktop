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
import kotlinx.coroutines.withTimeout
import java.net.URL
import javax.imageio.ImageIO
import java.io.File
import java.nio.file.Path

@Composable
fun AsyncMarkdownImage(url: String, modifier: Modifier = Modifier, basePath: Path? = null) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    var error by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        withContext(Dispatchers.IO) {
            try {
                val image = withTimeout(10_000L) {
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        val connection = URL(url).openConnection()
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000
                        ImageIO.read(connection.getInputStream())
                    } else if (url.startsWith("file://")) {
                        ImageIO.read(File(java.net.URI(url)))
                    } else {
                        val file = if (basePath != null && !File(url).isAbsolute) {
                            basePath.resolve(url).normalize().toFile()
                        } else {
                            File(url)
                        }
                        ImageIO.read(file)
                    }
                }
                
                if (image != null) {
                    bitmap = image.toComposeImageBitmap()
                } else {
                    error = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
