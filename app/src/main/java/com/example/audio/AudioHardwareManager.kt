package com.example.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DetectedAudioDevice(
    val id: Int,
    val name: String,
    val type: Int,
    val typeName: String,
    val isSource: Boolean, // input
    val isSink: Boolean    // output
)

data class AudioRoutingStatus(
    val primaryInputDevice: String,
    val primaryOutputDevice: String,
    val isDirectInjectionSupported: Boolean,
    val injectionStatusMessage: String,
    val hasWiredHeadset: Boolean,
    val hasUsbAudio: Boolean,
    val hasBluetoothAudio: Boolean,
    val recommendedSetup: String,
    val activeDevices: List<DetectedAudioDevice>
)

class AudioHardwareManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _routingStatus = MutableStateFlow(evaluateCurrentAudioState())
    val routingStatus: StateFlow<AudioRoutingStatus> = _routingStatus.asStateFlow()

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            _routingStatus.value = evaluateCurrentAudioState()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            _routingStatus.value = evaluateCurrentAudioState()
        }
    }

    init {
        audioManager.registerAudioDeviceCallback(deviceCallback, null)
    }

    fun refresh() {
        _routingStatus.value = evaluateCurrentAudioState()
    }

    private fun evaluateCurrentAudioState(): AudioRoutingStatus {
        val inputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        val outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        val allDevices = mutableListOf<DetectedAudioDevice>()
        var hasWired = false
        var hasUsb = false
        var hasBt = false
        var primaryInput = "Built-in Microphone"
        var primaryOutput = "Built-in Speaker"

        for (dev in inputDevices) {
            val typeName = getDeviceTypeName(dev.type)
            val name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !dev.productName.isNullOrBlank()) {
                dev.productName.toString()
            } else {
                typeName
            }
            if (dev.type == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
                hasWired = true
                primaryInput = "Wired Headset Mic ($name)"
            }
            if (dev.type == AudioDeviceInfo.TYPE_USB_DEVICE || dev.type == AudioDeviceInfo.TYPE_USB_HEADSET) {
                hasUsb = true
                primaryInput = "USB Audio Interface Mic ($name)"
            }
            if (dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                hasBt = true
                primaryInput = "Bluetooth Headset Mic ($name)"
            }

            allDevices.add(
                DetectedAudioDevice(
                    id = dev.id,
                    name = name,
                    type = dev.type,
                    typeName = typeName,
                    isSource = true,
                    isSink = false
                )
            )
        }

        for (dev in outputDevices) {
            val typeName = getDeviceTypeName(dev.type)
            val name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !dev.productName.isNullOrBlank()) {
                dev.productName.toString()
            } else {
                typeName
            }
            if (dev.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || dev.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) {
                hasWired = true
                primaryOutput = "Wired Headphones ($name)"
            }
            if (dev.type == AudioDeviceInfo.TYPE_USB_DEVICE || dev.type == AudioDeviceInfo.TYPE_USB_HEADSET) {
                hasUsb = true
                primaryOutput = "USB Audio Output ($name)"
            }
            if (dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                hasBt = true
                primaryOutput = "Bluetooth Audio ($name)"
            }

            allDevices.add(
                DetectedAudioDevice(
                    id = dev.id,
                    name = name,
                    type = dev.type,
                    typeName = typeName,
                    isSource = false,
                    isSink = true
                )
            )
        }

        // Direct Virtual Mic Injection evaluation
        // Standard Android OS isolates AudioRecord buffers per application UID for privacy.
        // Third-party applications cannot override or inject audio into another app's microphone input.
        val isDirectSupported = false
        val injectionMsg = "Direct game microphone injection is not supported on this device. " +
                "Android security policy prohibits 3rd-party apps from overriding another application's microphone input."

        val recommended = when {
            hasUsb -> "USB Audio Interface / OTG Sound Card (Hardware Mixing)"
            hasWired -> "TRRS 4-Pole Audio Splitter (Gaming Headset + Line-In)"
            hasBt -> "Bluetooth Receiver + 3.5mm Aux Splitter"
            else -> "Connect a TRRS Audio Splitter or USB Sound Card to route mixed meme audio into Free Fire voice chat."
        }

        return AudioRoutingStatus(
            primaryInputDevice = primaryInput,
            primaryOutputDevice = primaryOutput,
            isDirectInjectionSupported = isDirectSupported,
            injectionStatusMessage = injectionMsg,
            hasWiredHeadset = hasWired,
            hasUsbAudio = hasUsb,
            hasBluetoothAudio = hasBt,
            recommendedSetup = recommended,
            activeDevices = allDevices
        )
    }

    private fun getDeviceTypeName(type: Int): String {
        return when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Built-in Microphone"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Built-in Speaker"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset (Mic+Audio)"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphones"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth Voice Headset (SCO)"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth Stereo Audio (A2DP)"
            AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Audio Device / DAC"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
            AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB Audio Accessory"
            AudioDeviceInfo.TYPE_LINE_ANALOG -> "3.5mm Line In/Out"
            AudioDeviceInfo.TYPE_LINE_DIGITAL -> "Digital Line"
            else -> "Audio Device (Code $type)"
        }
    }

    fun unregister() {
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
    }
}
