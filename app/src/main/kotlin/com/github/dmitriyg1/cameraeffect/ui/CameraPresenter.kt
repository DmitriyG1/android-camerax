package com.github.dmitriyg1.cameraeffect.ui

import android.graphics.Rect
import android.util.Size
import androidx.camera.core.CameraX
import com.github.dmitriyg1.cameraeffect.ui.CameraScreen.ButtonStyle
import com.github.dmitriyg1.cameraeffect.ui.CameraScreen.ViewState

class CameraPresenter(
  private val view: CameraScreen,
  private val model: CameraModel
) {
  private var viewState = createInitialState()

  fun onViewInitialized() {
    updateViewState {
      val resolution = model.getCameraResolution(it.lensFacing) ?: Size(0, 0)
      it.copy(
        resolution = resolution,
        zoomRect = resolution.toRect()
      )
    }
  }

  private fun createInitialState(): ViewState {
    return ViewState(
      lensFacing = CameraX.LensFacing.BACK,
      resolution = Size(0, 0),
      zoom = 1f,
      buttonStyle = ButtonStyle.White,
      zoomRect = Rect(0, 0, 0, 0)
    )
  }

  fun changeLensFacingIntent() {
    val targetLensFacing = when (viewState.lensFacing) {
      CameraX.LensFacing.BACK -> CameraX.LensFacing.FRONT
      CameraX.LensFacing.FRONT -> CameraX.LensFacing.BACK
    }

    if (model.hasCameraWithLensFacing(targetLensFacing)) {
      updateViewState {
        val resolution = model.getCameraResolution(targetLensFacing) ?: Size(0, 0)
        // При смене камеры сбрасывается текущее состояние
        it.copy(
          lensFacing = targetLensFacing,
          resolution = resolution,
          zoom = 1f,
          zoomRect = resolution.toRect()
        )
      }
    }
  }

  fun changeZoomIntent(scaleFactor: Float) {
    val targetZoom = (viewState.zoom * scaleFactor)
      .let {
        if (it > MAX_ZOOM) MAX_ZOOM else it
      }
      .let {
        if (it < MIN_ZOOM) MIN_ZOOM else it
      }

    val zoom = viewState.zoom

    val scaledWidth = viewState.resolution.width.toFloat() / zoom
    val horizontalMargin = (viewState.resolution.width.toFloat() - scaledWidth) / 2

    val scaledHeight = viewState.resolution.height.toFloat() / zoom
    val verticalMargin = (viewState.resolution.height.toFloat() - scaledHeight) / 2

    updateViewState {
      viewState.copy(
        zoom = targetZoom,
        zoomRect = Rect(
          horizontalMargin.toInt(),
          verticalMargin.toInt(),
          (viewState.resolution.width - horizontalMargin).toInt(),
          (viewState.resolution.height - verticalMargin).toInt()
        )
      )
    }
  }

  fun changeButtonStyleIntent(direction: Int) {
    val index = (viewState.buttonStyle.ordinal + direction)
      .let {
        if (it > ButtonStyle.values().lastIndex) 0 else it
      }
      .let {
        if (it < 0) ButtonStyle.values().lastIndex else it
      }

    updateViewState {
      it.copy(
        buttonStyle = ButtonStyle.values().getOrElse(index) { ButtonStyle.White }
      )
    }
  }

  private fun updateViewState(update: (ViewState) -> ViewState) {
    viewState = update(viewState)
    view.render(viewState)
  }
}

private fun Size.toRect(): Rect {
  return Rect(
    0,
    0,
    width,
    height
  )
}

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 2f
