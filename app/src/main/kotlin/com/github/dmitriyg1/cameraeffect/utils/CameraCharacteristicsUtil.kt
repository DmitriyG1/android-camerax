package com.github.dmitriyg1.cameraeffect.utils

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraX

fun getCameraResolution(context: Context, lensFacing: CameraX.LensFacing): Size? {
  return getCameraResolution(context, getCameraIndex(context, lensFacing.camera2LensFacing))
}

fun getCameraResolution(context: Context, camNum: Int): Size? {
  val manager = context.cameraManager
  return try {
    val cameraId = manager.cameraIdList.getOrNull(camNum)
    if (cameraId != null) {
      val character = manager.getCameraCharacteristics(cameraId)
      character.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
    } else null
  } catch (e: CameraAccessException) {
    Log.e(LOG_TAG, e.message, e)
    null
  }
}

private fun getCameraIndex(context: Context, camera2LensFacing: Int): Int {
  val manager = context.cameraManager
  return context.cameraManager.cameraIdList.indexOfFirst { id ->
    manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == camera2LensFacing
  }
}

private val Context.cameraManager: CameraManager
  get() = getSystemService(Context.CAMERA_SERVICE) as CameraManager

private val CameraX.LensFacing.camera2LensFacing: Int
  get() {
    return when (this) {
      CameraX.LensFacing.FRONT -> CameraCharacteristics.LENS_FACING_FRONT
      CameraX.LensFacing.BACK -> CameraCharacteristics.LENS_FACING_BACK
    }
  }

private const val LOG_TAG = "CameraCharacteristicsUtil"
