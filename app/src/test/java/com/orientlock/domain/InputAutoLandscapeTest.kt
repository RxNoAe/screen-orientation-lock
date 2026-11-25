package com.orientlock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 自动横屏的判定逻辑是纯函数,这里全在 JVM 上验证,不需要 Android 环境。
 */
class InputAutoLandscapeTest {

    private fun settings(
        enabled: Boolean = true,
        keyboard: Boolean = true,
        mouse: Boolean = true,
    ) = AppSettings(
        inputAutoLandscapeEnabled = enabled,
        inputAutoLandscapeKeyboard = keyboard,
        inputAutoLandscapeMouse = mouse,
    )

    @Test
    fun `总开关关闭时不触发_即使接着键盘鼠标`() {
        assertFalse(
            shouldAutoLandscape(
                settings(enabled = false),
                InputCapabilities(hasKeyboard = true, hasMouse = true),
            )
        )
    }

    @Test
    fun `接键盘且键盘子项开着时触发`() {
        assertTrue(
            shouldAutoLandscape(
                settings(),
                InputCapabilities(hasKeyboard = true, hasMouse = false),
            )
        )
    }

    @Test
    fun `接鼠标且鼠标子项开着时触发`() {
        assertTrue(
            shouldAutoLandscape(
                settings(),
                InputCapabilities(hasKeyboard = false, hasMouse = true),
            )
        )
    }

    @Test
    fun `键盘子项关闭时接键盘不触发`() {
        assertFalse(
            shouldAutoLandscape(
                settings(keyboard = false, mouse = false),
                InputCapabilities(hasKeyboard = true, hasMouse = false),
            )
        )
    }

    @Test
    fun `鼠标子项关闭时接鼠标不触发`() {
        assertFalse(
            shouldAutoLandscape(
                settings(keyboard = false, mouse = false),
                InputCapabilities(hasKeyboard = false, hasMouse = true),
            )
        )
    }

    @Test
    fun `键盘子项关闭不影响鼠标触发`() {
        // 键盘被禁用,但鼠标子项仍开着且接着鼠标 → 仍应横屏
        assertTrue(
            shouldAutoLandscape(
                settings(keyboard = false),
                InputCapabilities(hasKeyboard = true, hasMouse = true),
            )
        )
    }

    @Test
    fun `没有外设时不触发`() {
        assertFalse(shouldAutoLandscape(settings(), InputCapabilities.NONE))
    }

    // ---- stepInputAutoLandscape(状态机) ----

    @Test
    fun `未接外设时状态机不动作`() {
        val step = stepInputAutoLandscape(
            settings = settings(enabled = false, keyboard = false, mouse = false),
            caps = InputCapabilities.NONE,
            engaged = false,
            modeBefore = null,
        )

        assertFalse(step.engaged)
        assertEquals(null, step.modeBefore)
        assertEquals(null, step.switchTo)
    }

    @Test
    fun `首次接键盘时切横屏并记住原模式`() {
        val step = stepInputAutoLandscape(
            settings = settings().copy(mode = OrientationMode.PORTRAIT),
            caps = InputCapabilities(hasKeyboard = true),
            engaged = false,
            modeBefore = null,
        )

        assertTrue(step.engaged)
        assertEquals(OrientationMode.PORTRAIT, step.modeBefore)
        assertEquals(OrientationMode.LANDSCAPE, step.switchTo)
    }

    @Test
    fun `已经是横屏时首次接键盘不再重复写入`() {
        val step = stepInputAutoLandscape(
            settings = settings().copy(mode = OrientationMode.LANDSCAPE),
            caps = InputCapabilities(hasKeyboard = true),
            engaged = false,
            modeBefore = null,
        )

        assertTrue(step.engaged)
        assertEquals(OrientationMode.LANDSCAPE, step.modeBefore)
        assertEquals(null, step.switchTo)
    }

    @Test
    fun `接管中重复调用保持幂等`() {
        // 我们自己 setMode(LANDSCAPE) 会把设置流再打回来,必须不产生第二个动作
        val step = stepInputAutoLandscape(
            settings = settings().copy(mode = OrientationMode.LANDSCAPE),
            caps = InputCapabilities(hasKeyboard = true),
            engaged = true,
            modeBefore = OrientationMode.PORTRAIT,
        )

        assertTrue(step.engaged)
        assertEquals(OrientationMode.PORTRAIT, step.modeBefore)
        assertEquals(null, step.switchTo)
    }

    @Test
    fun `接管期间用户手动改方向只更新恢复目标`() {
        val step = stepInputAutoLandscape(
            settings = settings().copy(mode = OrientationMode.PORTRAIT_REVERSE),
            caps = InputCapabilities(hasKeyboard = true),
            engaged = true,
            modeBefore = OrientationMode.AUTO,
        )

        assertTrue("仍处于接管中", step.engaged)
        assertEquals(OrientationMode.PORTRAIT_REVERSE, step.modeBefore)
        // 不强行改回横屏
        assertEquals(null, step.switchTo)
    }

    @Test
    fun `拔掉设备后恢复原模式`() {
        val step = stepInputAutoLandscape(
            settings = settings().copy(
                mode = OrientationMode.LANDSCAPE,
                inputAutoLandscapeRestore = true,
            ),
            caps = InputCapabilities.NONE,
            engaged = true,
            modeBefore = OrientationMode.PORTRAIT,
        )

        assertFalse(step.engaged)
        assertEquals(null, step.modeBefore)
        assertEquals(OrientationMode.PORTRAIT, step.switchTo)
    }

    @Test
    fun `关闭恢复时拔掉设备不切回`() {
        val step = stepInputAutoLandscape(
            settings = settings().copy(
                mode = OrientationMode.LANDSCAPE,
                inputAutoLandscapeRestore = false,
            ),
            caps = InputCapabilities.NONE,
            engaged = true,
            modeBefore = OrientationMode.PORTRAIT,
        )

        assertFalse(step.engaged)
        assertEquals(null, step.switchTo)
    }

    @Test
    fun `拔掉时当前已经是恢复目标则不切换`() {
        val step = stepInputAutoLandscape(
            settings = settings().copy(
                mode = OrientationMode.LANDSCAPE,
                inputAutoLandscapeRestore = true,
            ),
            caps = InputCapabilities.NONE,
            engaged = true,
            modeBefore = OrientationMode.LANDSCAPE,
        )

        assertEquals(null, step.switchTo)
    }

    @Test
    fun `鼠标单独接入也能触发`() {
        val step = stepInputAutoLandscape(
            settings = settings().copy(mode = OrientationMode.PORTRAIT),
            caps = InputCapabilities(hasMouse = true),
            engaged = false,
            modeBefore = null,
        )

        assertEquals(OrientationMode.LANDSCAPE, step.switchTo)
    }

    // ---- InputDeviceRules ----

    @Test
    fun `alphabet 键盘类型算键盘`() {
        assertTrue(InputDeviceRules.isKeyboard(KEYBOARD_TYPE_ALPHABETIC, isVirtual = false))
    }

    @Test
    fun `keyboardType 为 NONE 的按键设备不算键盘`() {
        // 电源键 / 音量键那类 gpio-keys 不是键盘,避免常驻误触发
        assertFalse(InputDeviceRules.isKeyboard(InputDeviceRules.KEYBOARD_TYPE_NONE, isVirtual = false))
    }

    @Test
    fun `虚拟键盘不算键盘`() {
        assertFalse(InputDeviceRules.isKeyboard(KEYBOARD_TYPE_ALPHABETIC, isVirtual = true))
    }

    @Test
    fun `鼠标源算鼠标`() {
        assertTrue(InputDeviceRules.isMouse(SOURCE_MOUSE, isVirtual = false))
    }

    @Test
    fun `触控板算鼠标`() {
        assertTrue(InputDeviceRules.isMouse(SOURCE_TOUCHPAD, isVirtual = false))
    }

    @Test
    fun `触摸屏不算鼠标`() {
        assertFalse(InputDeviceRules.isMouse(SOURCE_TOUCHSCREEN, isVirtual = false))
    }

    @Test
    fun `虚拟鼠标不算鼠标`() {
        assertFalse(InputDeviceRules.isMouse(SOURCE_MOUSE, isVirtual = true))
    }

    /** 键盘带指针位(部分二合一键盘)时,鼠标判定仍只认鼠标源 */
    @Test
    fun `键盘源不算鼠标`() {
        assertFalse(InputDeviceRules.isMouse(SOURCE_KEYBOARD, isVirtual = false))
    }

    private companion object {
        const val KEYBOARD_TYPE_ALPHABETIC = 1
        const val SOURCE_KEYBOARD = 0x00000101
        const val SOURCE_TOUCHSCREEN = 0x00001002
        const val SOURCE_MOUSE = 0x00002002
        const val SOURCE_TOUCHPAD = 0x00100008
    }
}
