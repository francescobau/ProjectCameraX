package com.example.projectcamerax


import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import android.webkit.MimeTypeMap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.PermissionChecker
import com.example.projectcamerax.databinding.ActivityMainBinding
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale

typealias LumaListener = (luma: Double) -> Unit

enum class CameraMode { PHOTO, VIDEO }
enum class MyCameraSelector { BACK, FRONT }

data class MediaInfo(val title: String, val mimeType: String, val fullPath: String)

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
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
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

            val datasource = Datasource()
            // Sets the listener for the click on the image icon.
            viewBinding.imageButton.setOnClickListener{
                val mediaList = datasource.getMediaList()
                Log.d(TAG,"SIZE: ${mediaList.size}")
                val intent = Intent(this,MediaPickerActivity::class.java)
                var index = 0
                for (media in mediaList) {
                    Log.d(TAG, "[$index] $media")
                    index = index.inc()
                }
                startActivity(intent)
            }

            startCamera()
        } else {
            requestPermissions()
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }


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
        return when(selectedCamera){
            MyCameraSelector.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
            MyCameraSelector.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
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
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$LOCAL_PHOTO_DIRECTORY")
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
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$LOCAL_VIDEO_DIRECTORY")
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
        viewBinding.chip.text = when(cameraMode) {
            CameraMode.PHOTO -> getString(R.string.camera_mode_PHOTO)
            CameraMode.VIDEO -> getString(R.string.camera_mode_VIDEO)
        }

        // Sets text to the camera selection chip.
        viewBinding.chip2.text = when (myCameraSelector) {
            MyCameraSelector.BACK  -> getString(R.string.camera_selector_BACK)
            MyCameraSelector.FRONT -> getString(R.string.camera_selector_FRONT)
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()


            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
//                    val surfaceProvider = viewBinding.viewFinder.surfaceProvider as SurfaceRequest
//                    surfaceProvider.resolution
                    //TODO
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
                            .also {
                                it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                                    Log.d(TAG, "Average luminosity: $luma")
                                })
                            }
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
        const val LOCAL_PHOTO_DIRECTORY = "CameraX-Image"
        const val LOCAL_VIDEO_DIRECTORY = "CameraX-Video"
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

    private inner class LuminosityAnalyzer(private val ignoredListener: LumaListener) : ImageAnalysis.Analyzer {

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

            ignoredListener(luma)

            image.close()
        }
    }
}

class Datasource {

    fun getMediaList(): MutableList<MediaInfo> {
        val mediaList = mutableListOf<MediaInfo>()

        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val cameraXImageDir = File(picturesDir, MainActivity.LOCAL_PHOTO_DIRECTORY)

        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val cameraXVideoDir = File(moviesDir, MainActivity.LOCAL_VIDEO_DIRECTORY)

        val mediaDirectories = arrayOf(cameraXImageDir, cameraXVideoDir)

        for (mediaDir in mediaDirectories) {
            if (mediaDir.exists()) {
                val files = mediaDir.listFiles()
                files?.forEach { file ->
                    if (file.isFile) {
                        val mediaInfo = MediaInfo(file.name, getMimeType(file), file.absolutePath)
                        mediaList.add(mediaInfo)

                    }
                }
            }
        }

        return mediaList
    }

    private fun getMimeType(file: File): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(file.path)
        Log.d(Datasource::class.simpleName, "File Extension: $extension")
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "unknown"
    }

}