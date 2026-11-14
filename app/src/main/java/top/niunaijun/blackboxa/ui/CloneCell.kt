package top.niunaijun.blackboxa.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * One launcher-style cell: app icon with a green number badge, name below.
 * Presses shrink to 0.95 with a spring, the way a launcher icon should feel.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CloneCell(
    bitmap: Bitmap?,
    label: String,
    badgeNumber: Int?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cellPress"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier.size(64.dp)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = label,
                    modifier = Modifier.size(60.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            if (badgeNumber != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(CloneBadgeGreen)
                ) {
                    Text(
                        text = badgeNumber.toString(),
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

private val CloneBadgeGreen = androidx.compose.ui.graphics.Color(0xFF22C55E)
