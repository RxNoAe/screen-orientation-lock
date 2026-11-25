package com.orientlock.system

import android.content.Context
import android.hardware.input.InputManager
import android.os.Build
import android.view.InputDevice
import com.orientlock.domain.InputCapabilities
import com.orientlock.domain.InputDeviceRules

/**
 * 监听输入设备的插拔,汇报当前是否接着键盘 / 鼠标。
 *
 * 只在总开关打开时由 [OrientationService] 注册,关掉即注销——列表常驻监听没有必要。
 * 注册时传 null handler:回调落在注册线程(服务的生命周期方法都在主线程)的 Looper 上。
 *
 * 判定规则不在这里,而在纯 Kotlin 的 [InputDeviceRules];本类只负责把系统设备列表
 * 翻译成 [InputCapabilities]。
 */
class InputDeviceWatcher(
    context: Context,
    private val onCapabilitiesChanged: (InputCapabilities) -> Unit,
) : InputManager.InputDeviceListener {

    private val inputManager =
        context.getSystemService(Context.INPUT_SERVICE) as InputManager

    /** 注册监听,并立刻回报一次当前状态 */
    fun start() {
        inputManager.registerInputDeviceListener(this, null)
        onCapabilitiesChanged(readCapabilities())
    }

    fun stop() {
        inputManager.unregisterInputDeviceListener(this)
    }

    override fun onInputDeviceAdded(deviceId: Int) = report()

    override fun onInputDeviceRemoved(deviceId: Int) = report()

    override fun onInputDeviceChanged(deviceId: Int) = report()

    private fun report() {
        onCapabilitiesChanged(readCapabilities())
    }

    private fun readCapabilities(): InputCapabilities {
        var keyboard = false
        var mouse = false
        for (id in inputManager.inputDeviceIds) {
            val device = inputManager.getInputDevice(id) ?: continue
            val virtual = isVirtual(device)
            if (InputDeviceRules.isKeyboard(device.keyboardType, virtual)) keyboard = true
            if (InputDeviceRules.isMouse(device.sources, virtual)) mouse = true
            if (keyboard && mouse) break
        }
        return InputCapabilities(hasKeyboard = keyboard, hasMouse = mouse)
    }

    /**
     * `InputDevice.isVirtual` 是 API 29 才有的。更低的版本拿不到这个信息,
     * 只能保守地当作「非虚拟」——误判风险由 [InputDeviceRules] 的键盘判据(keyboardType)
     * 兜底。
     */
    private fun isVirtual(device: InputDevice): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && device.isVirtual
}
