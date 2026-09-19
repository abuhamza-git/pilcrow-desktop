package com.pilcrowmd.desktop.rendering

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import java.awt.image.BufferedImage
import javax.swing.JLabel

@Composable
fun LatexBlock(content: String) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val bitmap = remember(content, color) {
        try {
            val formula = TeXFormula(content)
            val icon = formula.createTeXIcon(TeXConstants.STYLE_DISPLAY, 24f)
            
            // Render to a BufferedImage
            val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_ARGB)
            val g2 = image.createGraphics()
            
            // Set the graphics hint if needed (anti-aliasing)
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
            
            val label = JLabel()
            label.foreground = java.awt.Color(
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt(),
                (color.alpha * 255).toInt()
            )
            icon.paintIcon(label, g2, 0, 0)
            g2.dispose()
            
            image.toComposeImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = "LaTeX Math")
        } else {
            // Fallback to text if math fails to parse
            Text(
                text = content,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun LatexInline(content: String) {
    val color = MaterialTheme.colorScheme.onSurface
    val bitmap = remember(content, color) {
        try {
            val formula = TeXFormula(content)
            val icon = formula.createTeXIcon(TeXConstants.STYLE_TEXT, 18f)
            
            val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_ARGB)
            val g2 = image.createGraphics()
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
            
            val label = JLabel()
            label.foreground = java.awt.Color(
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt(),
                (color.alpha * 255).toInt()
            )
            icon.paintIcon(label, g2, 0, 0)
            g2.dispose()
            
            image.toComposeImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap, 
            contentDescription = "LaTeX Math Inline",
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    } else {
        Text(
            text = content,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}
