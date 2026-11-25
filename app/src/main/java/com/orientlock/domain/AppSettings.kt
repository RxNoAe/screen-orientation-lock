package com.orientlock.domain

/**
 * 应用的全部可持久化设置。
 *
 * @param mode 当前锁定的方向模式
 * @param pinnedRotation 选中 [OrientationMode.CURRENT] 瞬间固定的系统角度值
 * @param autoStartOnBoot 是否开机自启并恢复锁定
 * @param persistentNotification 是否显示常驻通知
 * @param guardEnabled 是否启用守护
 * @param naturalOrientation 设备的天然朝向;null 表示尚未探测
 * @param inputAutoLandscapeEnabled 是否启用「接键盘 / 鼠标自动横屏」总开关
 * @param inputAutoLandscapeKeyboard 接键盘时是否也触发(仅总开关打开时有意义)
 * @param inputAutoLandscapeMouse 接鼠标 / 触控板时是否也触发(仅总开关打开时有意义)
 * @param inputAutoLandscapeRestore 拔掉设备后是否回到切换前的方向
 */
data class AppSettings(
    val mode: OrientationMode = OrientationMode.AUTO,
    val pinnedRotation: Int = 0,
    val autoStartOnBoot: Boolean = true,
    val persistentNotification: Boolean = true,
    val guardEnabled: Boolean = true,
    val naturalOrientation: NaturalOrientation? = null,
    val inputAutoLandscapeEnabled: Boolean = false,
    val inputAutoLandscapeKeyboard: Boolean = true,
    val inputAutoLandscapeMouse: Boolean = true,
    val inputAutoLandscapeRestore: Boolean = true,
)
