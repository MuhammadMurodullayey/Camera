package uz.gita.camera

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import by.kirich1409.viewbindingdelegate.viewBinding
import uz.gita.camera.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val binding by viewBinding(ActivityMainBinding::bind)
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var savedUri : Uri
    private lateinit var photoFile : File
    private lateinit var cameraSelector1 : CameraSelector
    private lateinit var cameraExecutor  : ExecutorService
    private lateinit var camera : Camera
    private var isflashlight = ImageCapture.FLASH_MODE_OFF
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding.image.visibility = View.INVISIBLE
        outputDirectory = getOutputDirectory()
        cameraExecutor =  Executors.newSingleThreadExecutor()



        if (allPermissionGranted()) {
           cameraSelector1 =  CameraSelector.DEFAULT_BACK_CAMERA
            startCamera(cameraSelector1)
        } else {
            ActivityCompat.requestPermissions(this, Constants.REQUIRED_PERMISSIONS, Constants.REQUEST_CODE_PERMISSIONS)
        }
        binding.btnCamera.setOnClickListener {
            takePhoto()
        }
        binding.image.setOnClickListener {
            openImage()
        }
        binding.flashlight.setOnClickListener {
            when (isflashlight){
                ImageCapture.FLASH_MODE_OFF ->{
                    binding.flashlight.setImageResource(R.drawable.flash)
                    isflashlight = ImageCapture.FLASH_MODE_ON
                    imageCapture?.flashMode = isflashlight
                }
                ImageCapture.FLASH_MODE_ON ->{
                    isflashlight = ImageCapture.FLASH_MODE_AUTO
                    binding.flashlight.setImageResource(R.drawable.auto_flash)
                    imageCapture?.flashMode = isflashlight
                }
                ImageCapture.FLASH_MODE_AUTO ->{
                    binding.flashlight.setImageResource(R.drawable.no_flash)
                    isflashlight = ImageCapture.FLASH_MODE_OFF
                    imageCapture?.flashMode = isflashlight
                }


            }

        }

        binding.imageRepleace.setOnClickListener {
            if (cameraSelector1 == CameraSelector.DEFAULT_BACK_CAMERA){
                cameraSelector1 = CameraSelector.DEFAULT_FRONT_CAMERA
            }else{
                cameraSelector1 = CameraSelector.DEFAULT_BACK_CAMERA
            }

            startCamera(cameraSelector1)
        }
    }
    private fun openImage(){
        var uri = savedUri

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            uri = FileProvider.getUriForFile(this, uz.gita.camera.BuildConfig.APPLICATION_ID + ".provider",photoFile)
        }
        val intent  =Intent(Intent.ACTION_VIEW,uri).apply {


        }
        startActivity(intent)

        /*Intent intent = new Intent(Intent.ACTION_VIEW, uri);
  startActivity(intent);*/
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let { mFile ->
            File(mFile, resources.getString(R.string.app_name)).apply {
                mkdirs()
            }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir
        else filesDir
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
         photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                Constants.FILE_NAME_FORMAT,
                Locale.getDefault()
            )
                .format(System.currentTimeMillis()) + ".jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(photoFile)
            .build()
        imageCapture.takePicture(outputOptions,
            ContextCompat.getMainExecutor(this),
            object  : ImageCapture.OnImageSavedCallback{
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo saved"
                    val cameraIntent  = Intent()
                    val image = BitmapFactory.decodeFile(photoFile.absolutePath)
                    binding.image.setImageBitmap(image)
                    binding.image.scaleType = ImageView.ScaleType.CENTER_CROP
                    binding.image.visibility = View.VISIBLE

                    /*Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);*/
                 /*   Toast.makeText(this@MainActivity,
                        "$msg ,$savedUri",Toast.LENGTH_LONG).show()*/
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(Constants.TAG, "onError: ${exception.message}", exception)
                }

            }
        )



    }

    private fun startCamera(cameraSelector  : CameraSelector) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preView = Preview.Builder()
                .build()
                .also { mPrewiew ->
                    mPrewiew.setSurfaceProvider(
                        binding.viewFinder.surfaceProvider
                    )

                }
            imageCapture = ImageCapture.Builder()
                .setFlashMode(isflashlight)
                .build()
            val cameraSelector = cameraSelector

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preView, imageCapture)
            } catch (e: Exception) {
                Log.d(Constants.TAG, "startCamera fail:", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == Constants.REQUEST_CODE_PERMISSIONS) {
            startCamera(CameraSelector.DEFAULT_BACK_CAMERA)
        } else {
            Toast.makeText(this, "Permissions not granted by user", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun allPermissionGranted() = Constants.REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }
    @SuppressLint("MissingPermission")


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == Constants.REQUEST_CODE_PERMISSIONS && resultCode == Activity.RESULT_OK){
//             val photo : Bitmap = data?.extras?.get("data") as Bitmap
//            binding.image.setImageBitmap(photo)
//      }
//    }
}


/* @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK)
        {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(photo);
        }
    } */