package com.github.dmitriyg1.cameraeffect.ui

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.dmitriyg1.cameraeffect.R
import com.github.dmitriyg1.cameraeffect.ui.CameraScreen.ButtonStyle
import com.github.dmitriyg1.cameraeffect.ui.CameraScreen.ViewState
import com.github.dmitriyg1.cameraeffect.utils.AutoFitPreviewBuilder
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.abs


class CameraActivity : AppCompatActivity(),
  ScaleGestureDetector.OnScaleGestureListener,
  GestureDetector.OnGestureListener,
  CameraScreen {
  private val executor = Executors.newSingleThreadExecutor()
  private lateinit var viewFinder: TextureView
  private var preview: Preview? = null
  private lateinit var captureButton: View
  private lateinit var zoomView: TextView

  private lateinit var scaleGestureDetector: ScaleGestureDetector
  private lateinit var gestureDetector: GestureDetector

  private var previousViewState: ViewState? = null
  private val presenter = CameraPresenter(this, CameraModel(this))

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_camera)

    viewFinder = findViewById(R.id.view_finder)
    captureButton = findViewById(R.id.capture_button)
    zoomView = findViewById(R.id.zoom)

    // Request camera permissions
    if (allPermissionsGranted()) {
      viewFinder.post {
        presenter.onViewInitialized()
      }
    } else {
      ActivityCompat.requestPermissions(
        this,
        REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
      )
    }

    scaleGestureDetector = ScaleGestureDetector(this, this)
    gestureDetector = GestureDetector(this, this)
  }

  override fun onTouchEvent(event: MotionEvent?): Boolean {
    val scaleGestureResult = scaleGestureDetector.onTouchEvent(event)
    val gestureResult = gestureDetector.onTouchEvent(event)

    if (scaleGestureResult || gestureResult) {
      return true
    }

    return super.onTouchEvent(event)
  }

  private fun startCamera(viewState: ViewState) {
    CameraX.unbindAll()
    // Bind use cases to lifecycle
    // If Android Studio complains about "this" being not a LifecycleOwner
    // try rebuilding the project or updating the appcompat dependency to
    // version 1.1.0 or higher.
    CameraX.bindToLifecycle(
      this,
      previewUseCase(viewState),
      imageCaptureUseCase(viewState)
    )
  }

  private fun previewUseCase(viewState: ViewState): UseCase {
    val previewConfig = PreviewConfig.Builder().apply {
      setLensFacing(viewState.lensFacing)
      setTargetRotation(viewFinder.display.rotation)
    }.build()

    preview = AutoFitPreviewBuilder.build(previewConfig, viewFinder)
    return preview!!
  }

  private fun imageCaptureUseCase(viewState: ViewState): UseCase {
    // Create configuration object for the image capture use case
    val imageCaptureConfig = ImageCaptureConfig.Builder()
      .apply {
        setLensFacing(viewState.lensFacing)
        setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
        setTargetRotation(viewFinder.display.rotation)
      }.build()

    // Build the image capture use case and attach button click listener
    val imageCapture = ImageCapture(imageCaptureConfig)
    captureButton.setOnClickListener {
      imageCapture.takePicture(
        getImageFile(),
        executor,
        object : ImageCapture.OnImageSavedListener {
          override fun onError(
            imageCaptureError: ImageCapture.ImageCaptureError,
            message: String,
            exc: Throwable?
          ) {
            val msg = "Photo capture failed: $message"
            Log.e("CameraXApp", msg, exc)
            viewFinder.post {
              Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            }
          }

          override fun onImageSaved(file: File) {
            val msg = "Photo capture succeeded: ${file.absolutePath}"
            Log.d("CameraXApp", msg)
            viewFinder.post {
              Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            }
          }
        }
      )
    }

    return imageCapture
  }

  private fun getImageFile(): File {
    val fileName = "${System.currentTimeMillis()}.jpg"
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
      File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
        fileName
      )
    } else {
      val resolver = this.contentResolver
      val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
      }

      val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
      // Uri scheme is "content", "content://media/external/images/media/43"
      uri!!.toFile(this)!!
    }
  }

  /**
   * Process result from permission request dialog box, has the request
   * been granted? If yes, start Camera. Otherwise display a toast
   */
  override fun onRequestPermissionsResult(
    requestCode: Int, permissions: Array<String>, grantResults: IntArray
  ) {
    if (requestCode == REQUEST_CODE_PERMISSIONS) {
      if (allPermissionsGranted()) {
        viewFinder.post {
          presenter.onViewInitialized()
        }
      } else {
        Toast.makeText(
          this,
          "Permissions not granted by the user.",
          Toast.LENGTH_SHORT
        ).show()
        finish()
      }
    }
  }

  /**
   * Check if all permission specified in the manifest have been granted
   */
  private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
    ContextCompat.checkSelfPermission(
      baseContext, it
    ) == PackageManager.PERMISSION_GRANTED
  }

  override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
    return true
  }

  override fun onScaleEnd(detector: ScaleGestureDetector) {
  }

  override fun onScale(detector: ScaleGestureDetector): Boolean {
    presenter.changeZoomIntent(scaleGestureDetector.scaleFactor)
    return true
  }

  override fun onShowPress(p0: MotionEvent?) {

  }

  override fun onSingleTapUp(p0: MotionEvent?): Boolean {
    presenter.changeLensFacingIntent()

    return true
  }

  override fun onDown(p0: MotionEvent?): Boolean {
    return true
  }

  override fun onFling(
    startEvent: MotionEvent,
    currentEvent: MotionEvent,
    velocityX: Float,
    velocityY: Float
  ): Boolean {
    if (abs(velocityX) > abs(velocityY)) {
      val direction = if (velocityX > 0) -1 else 1
      presenter.changeButtonStyleIntent(direction)
    }
    return true
  }

  override fun onScroll(
    startEvent: MotionEvent,
    currentEvent: MotionEvent,
    distanceX: Float,
    distanceY: Float
  ): Boolean {

    return true
  }

  override fun onLongPress(p0: MotionEvent?) {
  }

  override fun render(state: ViewState) {
    if (previousViewState?.lensFacing != state.lensFacing) {
      startCamera(state)
    }

    if (previousViewState?.zoomRect != state.zoomRect) {
      renderZoom(state.zoom, state.zoomRect)
    }

    if (previousViewState?.buttonStyle != state.buttonStyle) {
      renderButton(state.buttonStyle)
    }

    previousViewState = state
  }

  private fun renderButton(buttonStyle: ButtonStyle) {
    captureButton.backgroundTintList = ContextCompat.getColorStateList(
      this,
      buttonStyle.color
    )
  }

  private fun renderZoom(zoom: Float, zoomRect: Rect) {
    preview?.zoom(zoomRect)
    zoomView.text = if (zoom > 1) "x %.1f".format(zoom) else null
  }
}

// This is an arbitrary number we are using to keep track of the permission
// request. Where an app has multiple context for requesting permission,
// this can help differentiate the different contexts.
private const val REQUEST_CODE_PERMISSIONS = 10

// This is an array of all the permission specified in the manifest.
private val REQUIRED_PERMISSIONS = arrayOf(
  Manifest.permission.CAMERA,
  Manifest.permission.WRITE_EXTERNAL_STORAGE,
  Manifest.permission.READ_EXTERNAL_STORAGE
)

private val ButtonStyle.color: Int
  get() {
    return when (this) {
      ButtonStyle.White -> android.R.color.white
      ButtonStyle.Red -> android.R.color.holo_red_light
      ButtonStyle.Green -> android.R.color.holo_green_light
      ButtonStyle.Blue -> android.R.color.holo_blue_light
    }
  }

/**
 * Uri "content:/media"
 */
private fun Uri.toFile(context: Context): File? {
  return getPath(context, this)?.let { File(it) }
}

private fun getPath(context: Context, uri: Uri): String? {
  val projection = arrayOf(MediaStore.Images.Media.DATA)
  return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
    val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
    cursor.moveToFirst()
    cursor.getString(columnIndex)
  }
}
