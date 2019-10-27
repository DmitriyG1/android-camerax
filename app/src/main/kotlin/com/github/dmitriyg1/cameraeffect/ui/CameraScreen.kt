package com.github.dmitriyg1.cameraeffect.ui

import android.graphics.Rect
import android.util.Size
import androidx.camera.core.CameraX


interface CameraScreen {
  fun render(state: ViewState)

  data class ViewState(
    val lensFacing: CameraX.LensFacing,
    val resolution: Size,
    val zoom: Float,
    val zoomRect: Rect,
    val buttonStyle: ButtonStyle
  )

  enum class ButtonStyle {
    White,
    Red,
    Green,
    Blue
  }
}
