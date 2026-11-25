package com.orientlock.domain

import kotlinx.coroutines.flow.Flow

/**
 * 方向设置的数据访问接口。
 *
 * ViewModel 与 OrientationService 都只依赖这个接口，便于用假实现做单元测试。
 * 实现见 com.orientlock.data.SettingsOrientationRepository。
 */
interface OrientationRepository {

    /** 设置变更流。实现方保证这是唯一数据源。 */
    val settings: Flow<AppSettings>

    /** 同步读一次当前设置 */
    suspend fun snapshot(): AppSettings

    /**
     * 选择一个方向模式：持久化并立即写入系统。
     *
     * [OrientationMode.CURRENT] 的固定角度在方法内部抓取。
     */
    suspend fun setMode(mode: OrientationMode)

    /**
     * 守护心跳：比对系统当前状态与目标，偏离则重写。
     *
     * @return true 表示本次发生了重写
     */
    suspend fun guardTick(): Boolean

    suspend fun setAutoStartOnBoot(enabled: Boolean)

    suspend fun setPersistentNotification(enabled: Boolean)

    suspend fun setGuardEnabled(enabled: Boolean)

    /** 总开关:接键盘 / 鼠标自动切横屏 */
    suspend fun setInputAutoLandscapeEnabled(enabled: Boolean)

    /** 子项:接键盘是否触发(仅总开关打开时有意义) */
    suspend fun setInputAutoLandscapeKeyboard(enabled: Boolean)

    /** 子项:接鼠标 / 触控板是否触发(仅总开关打开时有意义) */
    suspend fun setInputAutoLandscapeMouse(enabled: Boolean)

    /** 子项:拔掉设备后是否回到切换前的方向 */
    suspend fun setInputAutoLandscapeRestore(enabled: Boolean)
}
