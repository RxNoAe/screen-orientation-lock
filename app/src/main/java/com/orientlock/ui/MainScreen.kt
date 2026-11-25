package com.orientlock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.orientlock.R
import com.orientlock.domain.NaturalOrientation
import com.orientlock.domain.OrientationMode
import com.orientlock.system.PermissionIntents
import com.orientlock.ui.components.ModeCard
import com.orientlock.ui.components.PermissionBanner
import com.orientlock.ui.components.PhonePreview
import com.orientlock.ui.components.RomAdaptCard
import com.orientlock.ui.components.SettingRow
import com.orientlock.ui.components.SettingsCard
import com.orientlock.ui.components.StatusPill
import com.orientlock.ui.theme.TextTertiary
import com.orientlock.ui.theme.WarningAmber
import kotlinx.coroutines.delay

private val MODE_GRID = listOf(
    OrientationMode.PORTRAIT,
    OrientationMode.PORTRAIT_REVERSE,
    OrientationMode.LANDSCAPE,
    OrientationMode.LANDSCAPE_REVERSE,
    OrientationMode.CURRENT,
    OrientationMode.AUTO,
)

/** 一次性提示停留时长 */
private const val HINT_DURATION_MS = 4_000L

@Composable
fun MainScreen(
    state: MainUiState,
    hint: String?,
    onHintShown: () -> Unit,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 从系统权限页返回时复检授权状态
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val currentMode = state.settings.mode

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Screen Orientation Lock",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Keep every screen in the orientation you want",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // 两条权限通路，提示按「缺哪条」分情况：
        // 一条都没有 = 完全锁不了；只有写设置 = 能锁跟随系统的应用，但锁不住自己
        // 声明方向的（车机桌面那类），所以推荐补上悬浮窗。
        val needsOverlay = !state.canDrawOverlays
        PermissionBanner(
            visible = state.isLoaded && needsOverlay,
            title = if (state.canWriteSettings) "Enable the overlay to lock more apps" else "Locking needs one permission",
            message = if (state.canWriteSettings) {
                "Right now only apps that follow the system orientation can be locked. Car head-unit launchers and " +
                    "some media apps declare their own orientation in code, and Android listens to them first — only the \"Display over other apps\" permission can override that."
            } else {
                "\"Display over other apps\" can override an app's own declared orientation and is the " +
                    "recommended path; \"Modify system settings\" only locks apps that follow the system orientation. If neither is granted, locking won't work."
            },
            primaryLabel = "Enable overlay",
            onPrimaryClick = {
                context.startActivity(PermissionIntents.drawOverlays(context))
            },
            secondaryLabel = if (state.canWriteSettings) null else "Modify system settings",
            onSecondaryClick = if (state.canWriteSettings) {
                null
            } else {
                { context.startActivity(PermissionIntents.writeSettings(context)) }
            },
        )

        Spacer(Modifier.height(16.dp))

        // 首帧还没拿到真实设置时先不显示状态。MainUiState() 的默认 mode 是 AUTO，
        // 直接渲染会把「未锁定」当成结果闪一帧；重启后 DataStore 里可能是竖屏。
        if (state.isLoaded) {
            StatusPill(current = currentMode)
        }

        Spacer(Modifier.height(16.dp))

        PhonePreview(
            mode = currentMode,
            natural = state.settings.naturalOrientation ?: NaturalOrientation.PORTRAIT,
            pinnedRotationDegrees = state.settings.pinnedRotation,
        )

        Text(
            text = "All apps that follow the system orientation",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.height(24.dp))

        // 一次性提示。放在方向卡片正上方——用户是点了卡片才触发的，
        // 提示出现在这里才在视野内；放页面顶部在横屏或滚动后会被滑出屏幕。
        if (hint != null) {
            LaunchedEffect(hint) {
                delay(HINT_DURATION_MS)
                onHintShown()
            }
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = WarningAmber,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(WarningAmber.copy(alpha = 0.12f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
            Spacer(Modifier.height(12.dp))
        }

        // 方向网格：2 列 3 行
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MODE_GRID.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { mode ->
                        ModeCard(
                            mode = mode,
                            selected = currentMode == mode,
                            onClick = { viewModel.selectMode(mode) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        SettingsCard {
            SettingRow(
                iconRes = R.drawable.ic_setting_boot,
                title = "Start on boot",
                subtitle = "Restore your last lock after a reboot",
                checked = state.settings.autoStartOnBoot,
                onCheckedChange = viewModel::setAutoStartOnBoot,
            )
            SettingRow(
                iconRes = R.drawable.ic_setting_notification,
                title = "Persistent notification",
                subtitle = "Switch orientation from the notification shade",
                checked = state.settings.persistentNotification,
                onCheckedChange = viewModel::setPersistentNotification,
            )
            SettingRow(
                iconRes = R.drawable.ic_setting_guard,
                title = "Guard mode",
                subtitle = "Revert the orientation when something changes it",
                checked = state.settings.guardEnabled,
                onCheckedChange = viewModel::setGuardEnabled,
            )
        }

        Spacer(Modifier.height(16.dp))

        RomAdaptCard(
            onOpenSettings = {
                val intent = PermissionIntents.romAutoStart(context)
                    ?: PermissionIntents.appDetails(context)
                runCatching { context.startActivity(intent) }
                    .onFailure { context.startActivity(PermissionIntents.appDetails(context)) }
            },
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "If an app hard-codes its own orientation, this app can't override it",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
        )

        Spacer(Modifier.height(28.dp))
    }
}
