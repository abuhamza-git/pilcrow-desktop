package com.pilcrowmd.desktop.rendering

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import kotlin.math.max

@Composable
fun SimpleTableLayout(
    columnCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val measurables = subcompose("main", content)
        if (measurables.isEmpty() || columnCount == 0) {
            return@SubcomposeLayout layout(0, 0) {}
        }
        
        val rowCount = (measurables.size + columnCount - 1) / columnCount
        val columnWidths = IntArray(columnCount) { 0 }
        val rowHeights = IntArray(rowCount) { 0 }
        
        // 1. Calculate ideal max width for each column (no wrapping)
        val maxIntrinsicWidths = IntArray(columnCount) { 0 }
        measurables.forEachIndexed { index, measurable ->
            val col = index % columnCount
            maxIntrinsicWidths[col] = max(maxIntrinsicWidths[col], measurable.maxIntrinsicWidth(Constraints.Infinity))
        }
        
        val totalMaxWidth = maxIntrinsicWidths.sum()
        val availableWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else totalMaxWidth
        
        // 2. Assign column widths proportionally if they exceed screen space
        if (totalMaxWidth <= availableWidth) {
            for (i in 0 until columnCount) columnWidths[i] = maxIntrinsicWidths[i]
        } else {
            // Allocate space proportionally to how much text they contain
            var remainingWidth = availableWidth
            for (i in 0 until columnCount - 1) {
                val assigned = (maxIntrinsicWidths[i].toFloat() / totalMaxWidth * availableWidth).toInt()
                columnWidths[i] = assigned
                remainingWidth -= assigned
            }
            columnWidths[columnCount - 1] = max(0, remainingWidth) // Give whatever is left to the last column
        }
        
        // 3. Find height needed for each row based on these wrapped column widths
        measurables.forEachIndexed { index, measurable ->
            val col = index % columnCount
            val row = index / columnCount
            val height = measurable.maxIntrinsicHeight(columnWidths[col])
            rowHeights[row] = max(rowHeights[row], height)
        }
        
        // 4. Measure exactly
        val placeables = measurables.mapIndexed { index, measurable ->
            val col = index % columnCount
            val row = index / columnCount
            measurable.measure(Constraints.fixed(columnWidths[col], rowHeights[row]))
        }
        
        val tableWidth = columnWidths.sum()
        val tableHeight = rowHeights.sum()
        
        layout(tableWidth, tableHeight) {
            var y = 0
            for (row in 0 until rowCount) {
                var x = 0
                for (col in 0 until columnCount) {
                    val index = row * columnCount + col
                    if (index < placeables.size) {
                        placeables[index].placeRelative(x, y)
                    }
                    x += columnWidths[col]
                }
                y += rowHeights[row]
            }
        }
    }
}
