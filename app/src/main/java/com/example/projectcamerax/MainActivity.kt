package com.example.projectcamerax


import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import android.util.Log
import android.view.Display
import android.view.Surface
import android.widget.Button
import androidx.camera.core.Camera
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.VideoRecordEvent
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.PermissionChecker
import com.example.projectcamerax.databinding.ActivityMainBinding
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale

typealias LumaListener = (luma: Double) -> Unit

enum class CameraMode { PHOTO, VIDEO }
enum class MyCameraSelector { BACK, FRONT }

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    private var imageCapture: ImageCapture? = null

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService

    // Selecto photo mode as default
    private var cameraMode = CameraMode.PHOTO

    // Select back camera as a default
    private var myCameraSelector = MyCameraSelector.BACK

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && it.value == false)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)

//        Log.d(TAG,"CURRENT ORIENTATION: $requestedOrientation || ${requestedOrientation::class.simpleName}")
//        if(requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT)
//            adjustReverseLayout()
        setContentView(viewBinding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            // Sets the listener for the photo/video chip
            viewBinding.chip.setOnClickListener { swapCameraMode() }

            // Sets the listener for the back/front camera chip
            viewBinding.chip2.setOnClickListener{ swapCameraSelector() }

            startCamera()
        } else {
            requestPermissions()
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    //TODO: Function needs to be completed.
    //TODO: Find a way to detect reverse landscape.
//    private fun adjustReverseLayout() {
//        val mySet = ConstraintSet()
//        mySet.clone(viewBinding.root)
//        mySet.connect(viewBinding.captureButton.id,ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
//        mySet.setHorizontalBias(viewBinding.captureButton.id,0.0F)
//        mySet.setMargin(viewBinding.captureButton.id,ConstraintSet.LEFT,16)
//        mySet.connect(viewBinding.captureButton.id,ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)
//        Log.d(TAG,"ADJUSTED REVERSE LANDSCAPE ROTATION.")
//    }

    private fun swapCameraMode(){
        // Swaps between PHOTO and VIDEO, iterating the enum values.
        cameraMode = CameraMode.values()[ (cameraMode.ordinal+1) % CameraMode.values().size ]

        startCamera()
    }

    private fun swapCameraSelector(){
        // Swaps between BACK and FRONT, iterating the enum values.
        myCameraSelector = MyCameraSelector.values()[( myCameraSelector.ordinal+1) % MyCameraSelector.values().size ]

        startCamera()
    }
    private fun getCameraSelector(selectedCamera: MyCameraSelector): CameraSelector {
        when(selectedCamera){
            MyCameraSelector.BACK -> return CameraSelector.DEFAULT_BACK_CAMERA
            MyCameraSelector.FRONT -> return CameraSelector.DEFAULT_FRONT_CAMERA
        }
    }



    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }

    // Implements VideoCapture use case, including start and stop capturing.
    private fun captureVideo() {
        val videoCapture = this.videoCapture ?: return

        viewBinding.captureButton.isEnabled = false

        val curRecording = recording
        if (curRecording != null) {
            // Stop the current recording session.
            curRecording.stop()
            recording = null
            return
        }

        // create and start a new recording session
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .apply {
                if (PermissionChecker.checkSelfPermission(this@MainActivity,
                        Manifest.permission.RECORD_AUDIO) ==
                    PermissionChecker.PERMISSION_GRANTED)
                {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
                        viewBinding.captureButton.apply {
                            text = getString(R.string.stop_capture)
                            isEnabled = true
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = "Video capture succeeded: " +
                                    "${recordEvent.outputResults.outputUri}"
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT)
                                .show()
                            Log.d(TAG, msg)
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(TAG, "Video capture ends with error: " +
                                    "${recordEvent.error}")
                        }
                        viewBinding.captureButton.apply {
                            text = getString(R.string.start_capture)
                            isEnabled = true
                        }
                    }
                }
            }
    }

    private fun startCamera() {

        // Sets text to the camera mode chip.
        viewBinding.chip.text = "$cameraMode MODE"

        // Sets text to the camera selection chip.
        viewBinding.chip2.text = "$myCameraSelector"

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            val cameraSelector = getCameraSelector(myCameraSelector)

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                when(cameraMode) {
                    CameraMode.PHOTO -> {

                        // Sets the listener for the button.
                        viewBinding.captureButton.setOnClickListener { takePhoto() }

                        // Edit name of the button.
                        viewBinding.captureButton.text = getText(R.string.take_photo)
                        // Start ImageCapture instance.
                        imageCapture = ImageCapture.Builder().build()

                        // Start ImageAnalyzer instance.
                        val imageAnalyzer = ImageAnalysis.Builder()
                            .build()
//                            .also {
//                                it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
//                                    Log.d(TAG, "Average luminosity: $luma")
//                                })
//                            }
                        // Bind use cases to camera
                        cameraProvider.bindToLifecycle(
                            this, cameraSelector, preview, imageCapture, imageAnalyzer)
                    }
                    CameraMode.VIDEO -> {

                        // Sets the listener for the button.
                        viewBinding.captureButton.setOnClickListener { captureVideo() }
                        // Edit name of the button.
                        viewBinding.captureButton.text = getText(R.string.start_capture)

                        // Start VideoCapture instance.
                        val recorder = Recorder.Builder()
                            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                            .build()
                        videoCapture = VideoCapture.withOutput(recorder)

                        // Bind use cases to camera
                        cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture)
                    }
                }

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))

    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Saves Camera Mode enum
        outState.putInt(CameraMode::class.simpleName,cameraMode.ordinal)
        Log.d(TAG,"Saved key: ${CameraMode::class.simpleName} \t value: ${cameraMode.ordinal}")
        // Saves Camera Selector enum
        outState.putInt(MyCameraSelector::class.simpleName,myCameraSelector.ordinal)
        Log.d(TAG,"Saved key: ${MyCameraSelector::class.simpleName} \t value: ${myCameraSelector.ordinal}")

        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        // Restores Camera Mode
        cameraMode = CameraMode.values()[ savedInstanceState.getInt(CameraMode::class.simpleName) ]
        // Restores Camera Selector
        myCameraSelector = MyCameraSelector.values() [ savedInstanceState.getInt(MyCameraSelector::class.simpleName) ]

        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "Project CameraX"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    private inner class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        override fun analyze(image: ImageProxy) {

            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()
            val pixels = data.map { it.toInt() and 0xFF }
            val luma = pixels.average()

            listener(luma)

            image.close()
        }
    }
}