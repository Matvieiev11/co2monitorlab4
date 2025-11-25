package com.example.co2monitor.util

import android.content.Context
import android.os.Build
import android.provider.Settings

object DeviceInfoProvider {

    fun getDeviceId(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    fun getDeviceName(): String =
        "${Build.MANUFACTURER} ${Build.MODEL}"

    fun getOsVersion(): String =
        Build.VERSION.RELEASE
}