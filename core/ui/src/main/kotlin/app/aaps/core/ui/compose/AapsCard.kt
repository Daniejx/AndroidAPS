package app.aaps.core.ui.compose

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * AndroidAPS card component with proper elevation visibility in dark mode.
 *
 * Uses [MaterialTheme.colorScheme.surfaceContainerLow] as the default container color,
 * which provides visible elevation contrast in both light and dark themes through
 * Material 3's tonal elevation system.
 *
 * @param modifier Modifier to be applied to the card
 * @param selected Whether the card is in selected state (uses secondaryContainer color)
 * @param content The content of the card
 */
@Composable
fun AapsCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        content = content
    )
}
