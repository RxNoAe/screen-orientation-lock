package com.orientlock.domain

/**
 * 当前接入的输入外设能力快照。
 *
 * 由系统层 [com.orientlock.system.InputDeviceWatcher] 扫描 InputManager 得到,
 * 这里只保留判定需要的两个布尔值,好让「要不要自动横屏」这段逻辑能纯 JVM 单测。
 *
 * @param hasKeyboard 是否插着(或接着)实体键盘
 * @param hasMouse 是否接着鼠标 / 触控板
 */
data class InputCapabilities(
    val hasKeyboard: Boolean = false,
    val hasMouse: Boolean = false,
) {
    companion object {
        /** 什么都没接。功能关闭或还没扫描时用它 */
        val NONE = InputCapabilities()
    }
}

/**
 * 单个输入设备该不该被当成「键盘 / 鼠标」。
 *
 * 只吃原始值(sources 位掩码、keyboardType、是否虚拟设备),不引用
 * `android.view.InputDevice`——常量写成字面量,于是本类可以在纯 JVM 测试里直接跑。
 *
 * 为什么不直接判 `sources and SOURCE_KEYBOARD`:电源键、音量键那类 "gpio-keys"
 * 设备在部分 ROM 上带 SOURCE_KEYBOARD 位,但它们不是键盘。`keyboardType` 描述的是
 * 「能不能输入字母数字」,gpio-keys 与耳机线控都是 [KEYBOARD_TYPE_NONE],
 * 所以用它当判据能自然把这些误报挡掉。
 */
object InputDeviceRules {

    /** 与 android.view.InputDevice.KEYBOARD_TYPE_NONE 一致 */
    const val KEYBOARD_TYPE_NONE = 0

    /** 与 android.view.InputDevice.SOURCE_MOUSE 一致 */
    private const val SOURCE_MOUSE = 0x00002002

    /** 与 android.view.InputDevice.SOURCE_TOUCHPAD 一致 */
    private const val SOURCE_TOUCHPAD = 0x00100008

    /**
     * @param keyboardType `InputDevice.getKeyboardType()`
     * @param isVirtual `InputDevice.isVirtual()`;API < 29 上拿不到,调用方传 false
     */
    fun isKeyboard(keyboardType: Int, isVirtual: Boolean): Boolean =
        !isVirtual && keyboardType != KEYBOARD_TYPE_NONE

    /**
     * 鼠标与触控板都算「鼠标」:对用户来说都是指哪儿点哪儿的定位设备。
     *
     * 触摸屏不会命中——SOURCE_TOUCHSCREEN 的位与 SOURCE_MOUSE 不同,按位与结果不等于
     * SOURCE_MOUSE。
     *
     * @param sources `InputDevice.getSources()`
     * @param isVirtual `InputDevice.isVirtual()`;API < 29 上拿不到,调用方传 false
     */
    fun isMouse(sources: Int, isVirtual: Boolean): Boolean {
        if (isVirtual) return false
        return (sources and SOURCE_MOUSE) == SOURCE_MOUSE ||
            (sources and SOURCE_TOUCHPAD) == SOURCE_TOUCHPAD
    }
}

/**
 * 接入键盘 / 鼠标时是否应自动切到横屏。
 *
 * 三条门都要过:总开关打开、对应设备类型被允许、且该类型确实接着。
 * 完全无副作用,由 [com.orientlock.system.OrientationService] 每收到一次设置变更
 * 或设备插拔回调就重算一次。
 */
fun shouldAutoLandscape(settings: AppSettings, caps: InputCapabilities): Boolean {
    if (!settings.inputAutoLandscapeEnabled) return false
    if (settings.inputAutoLandscapeKeyboard && caps.hasKeyboard) return true
    if (settings.inputAutoLandscapeMouse && caps.hasMouse) return true
    return false
}

/**
 * 自动横屏状态机走一步后的结果。
 *
 * @param engaged 处理后的接管状态
 * @param modeBefore 处理后的「接管前模式」;未接管时为 null
 * @param switchTo 需要切换到的方向;null 表示保持当前方向不动
 */
data class InputAutoLandscapeStep(
    val engaged: Boolean,
    val modeBefore: OrientationMode?,
    val switchTo: OrientationMode? = null,
)

/**
 * 自动横屏的边缘触发状态机。
 *
 * 只在两个跳变点产生动作:
 * - 该横屏且尚未接管 → 切到横屏(已经是横屏就不用写),并记下当前模式;
 * - 不该横屏且正在接管 → 若用户开了「恢复」,切回记下的模式。
 *
 * 接管期间用户手动改成别的方向时,只把新方向记成恢复目标,不强行改回横屏——
 * 自动功能不该跟用户的手动选择打架。
 *
 * 纯函数:调用方(服务)保存 [engaged] / [modeBefore] 并在每次设置变更或设备
 * 插拔后调用本函数。**它是幂等的**:同一状态重复调用不会重复产生动作,
 * 这也挡住了「我们自己切横屏 → 设置流再次回调」的自激循环。
 */
fun stepInputAutoLandscape(
    settings: AppSettings,
    caps: InputCapabilities,
    engaged: Boolean,
    modeBefore: OrientationMode?,
): InputAutoLandscapeStep {
    if (shouldAutoLandscape(settings, caps)) {
        if (!engaged) {
            return InputAutoLandscapeStep(
                engaged = true,
                modeBefore = settings.mode,
                switchTo = OrientationMode.LANDSCAPE
                    .takeIf { settings.mode != OrientationMode.LANDSCAPE },
            )
        }
        return InputAutoLandscapeStep(
            engaged = true,
            modeBefore = if (settings.mode != OrientationMode.LANDSCAPE) {
                settings.mode
            } else {
                modeBefore
            },
        )
    }

    if (!engaged) {
        return InputAutoLandscapeStep(engaged = false, modeBefore = null)
    }
    return InputAutoLandscapeStep(
        engaged = false,
        modeBefore = null,
        switchTo = modeBefore?.takeIf {
            settings.inputAutoLandscapeRestore && it != settings.mode
        },
    )
}
