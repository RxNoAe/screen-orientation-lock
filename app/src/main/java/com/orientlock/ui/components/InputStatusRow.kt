package com.orientlock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.orientlock.domain.InputCapabilities
import com.orientlock.ui.theme.LockedGreen
import com.orientlock.ui.theme.TextSecondary
import com.orientlock.ui.theme.TextTertiary

/**
 * 输入卡片的实时状态行:系统此刻到底认到了什么。
 *
 * 没有它,「总开关打开了却什么都不发生」和「设备其实没被系统识别」在界面上长得
 * 一模一样,用户没法判断是功能坏了还是设备没接上。这一行把 [InputCapabilities]
 * 直接摊开,配合下面的子开关,一眼能看出是判据问题还是设备问题。
 */
@Composable
fun InputStatusRow(
    capabilities: InputCapabilities,
    modifier: Modifier = Modifier,
) {
    val detected = buildList {
        if (capabilities.hasKeyboard) add("Keyboard")
        if (capabilities.hasMouse) add("Mouse")
    }
    val anyDetected = detected.isNotEmpty()
    val accent = if (anyDetected) LockedGreen else TextTertiary
    val label = if (anyDetected) {
        detected.joinToString(" + ") + " connected"
    } else {
        "No keyboard or mouse connected"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            // 66dp = SettingRow 正文的左缘(16 容器 + 38 图标 + 12 内边距),与标题对齐
            .padding(start = 66.dp, end = 16.dp, top = 2.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (anyDetected) TextSecondary else TextTertiary,
        )
    }
}
