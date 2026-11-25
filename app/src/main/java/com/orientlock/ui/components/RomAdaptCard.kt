package com.orientlock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.orientlock.R
import com.orientlock.ui.theme.GlassBorder
import com.orientlock.ui.theme.GlassFill
import com.orientlock.ui.theme.TextSecondary
import com.orientlock.ui.theme.TextTertiary

/**
 * 国产 ROM 适配引导。
 *
 * 小米 / 华为 / OPPO / vivo 会杀掉后台服务，需要在各自的自启动管理里放行。
 */
@Composable
fun RomAdaptCard(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(GlassFill, GlassFill)))
            .border(1.dp, GlassBorder, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_rom),
                contentDescription = "System compatibility",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "System compatibility",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = "MIUI, Huawei, OPPO, vivo, and similar systems restrict apps from auto-starting " +
                "in the background. If the orientation isn't restored automatically after a reboot, " +
                "allow this app to auto-start in the system's \"Autostart\" or \"App launch\" manager, and remove its background restrictions in battery settings.",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
        )
        OutlinedButton(
            onClick = onOpenSettings,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = TextSecondary,
            ),
            border = BorderStroke(1.dp, GlassBorder),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Open settings")
        }
    }
}
