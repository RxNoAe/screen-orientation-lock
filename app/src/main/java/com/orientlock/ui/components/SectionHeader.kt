package com.orientlock.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orientlock.ui.theme.TextSecondary

/**
 * 设置分组的小标题。
 *
 * 卡片本身不画标题,原来几张卡片直接叠在一起,用户分不清哪张管方向、哪张管输入设备。
 * 上方的留白由调用方给,下边只留 8dp——靠「上宽下窄」把标题和它那张卡片锁成一组。
 */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = TextSecondary,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, bottom = 8.dp),
    )
}
