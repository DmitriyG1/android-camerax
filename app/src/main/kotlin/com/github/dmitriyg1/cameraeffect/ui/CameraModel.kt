package com.github.dmitriyg1.cameraeffect.ui

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraX
import com.github.dmitriyg1.cameraeffect.utils.getCameraResolution


class CameraModel(private val context: Context) {
  fun getCameraResolution(lensFacing: CameraX.LensFacing): Size? {
    return getCameraResolution(context, lensFacing)
  }

  fun hasCameraWithLensFacing(lensFacing: CameraX.LensFacing): Boolean {
    return CameraX.hasCameraWithLensFacing(lensFacing)
  }
}
