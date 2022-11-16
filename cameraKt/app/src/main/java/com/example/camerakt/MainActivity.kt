package com.example.camerakt

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.camerakt.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), UploadRequestBody.UploadCallback  {

    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture?=null
    private var  outputDirectory: File? = null
    private lateinit var cameraExecutor:ExecutorService
    //    private var selectedImage:Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        if(allPermissionsGranted()){
            startCamera()
            Toast.makeText(this,
                "We Have Permission",
                Toast.LENGTH_SHORT).show()
        }else{
            ActivityCompat.requestPermissions(
                this, Constants.REQUIRED_PERMISSIONS,
                Constants.REQUEST_CODE_PERMISSIONS
            )
        }

        binding .btnTakePhoto.setOnClickListener{
            fun main() = runBlocking {
                var cantPhotos=0
                while(cantPhotos<5){

                    //               Handler().postDelayed({
                    //
                    //               }, 1000)
                    takePhoto()
                    uploadImage()
                    cantPhotos++
                }

            }
            main()

        }
        binding.btnPost.setOnClickListener {
//            //postRequest(mUser)
//                val file = File(cacheDir, "myImage.jpg")
//                file.createNewFile()
//            Log.i("local file", "$file")
//            file.outputStream().use {
//                assets.open("image.jpg").copyTo(it)
//            }

//            uploadImage()

        }
    }

    private suspend fun uploadImage(){
        delay(2000L)
//        val parcelFileDescriptor =
//            contentResolver.openFileDescriptor(selectedImage!!, "r", null) ?: return
//        val inputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)
//        Log.i("InputStream", inputStream.toString())
        val file = path?.let { File(it) }
        Log.i("Archivo", file.toString())
//        val outputStream = FileOutputStream(file)
//        inputStream.copyTo(outputStream)

        //binding.progressBar.progress = 0
        val body = file?.let { UploadRequestBody(it, "image", this) }

        body?.let { MultipartBody.Part.createFormData("image", file.name, it) }?.let {
            APIService().uploadImage(
                it,
                RequestBody.create("multipart/form-data".toMediaTypeOrNull(), "Image From My Device")
            ).enqueue(object: Callback<UploadResponse> {
                override fun onResponse(
                    call: Call<UploadResponse>,
                    response: Response<UploadResponse>
                ) {
                    //binding.progressBar.progress = 100

                    Log.i("On good result",response.body()?.message.toString())

                }

                override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                    Log.i("On bad result", t.message!!)

                }

            })
        }
    }
    private fun getOutputDirectory(): File{
        val mediaDir = externalMediaDirs.firstOrNull()?.let { mFile->
            File(mFile, resources.getString(R.string.app_name)).apply {
                mkdirs()
            }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    private suspend fun takePhoto(){
        delay(1000L)
        val imageCapture = imageCapture?: return
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(Constants.FILE_NAME_FORMAT,
                Locale.getDefault())
                .format(System
                    .currentTimeMillis()) + ".jpg")
        val outputOption = ImageCapture
            .OutputFileOptions
            .Builder(photoFile)
            .build()

        imageCapture.takePicture(
            outputOption, ContextCompat.getMainExecutor(this),
            object :ImageCapture.OnImageSavedCallback{
                @SuppressLint("ShowToast")
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {

                    val savedUri = Uri.fromFile(photoFile)
                    Log.i("savedUri", "$savedUri")


                    path = savedUri.path


                    Log.i("the path?", "$path")
                    val msg = "Photo Saved"

                    Toast.makeText(
                        this@MainActivity,
                        "$msg $savedUri",
                        Toast.LENGTH_LONG
                    )
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(Constants.TAG,
                        "onError: ${exception.message}",
                        exception)
                }


            }
        )
    }
    companion object{
        private var path:String?= null
    }
    private fun startCamera(){
        val cameraProviderFuture = ProcessCameraProvider
            .getInstance(this)

        cameraProviderFuture.addListener({

            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also { mPreview->
                    mPreview.setSurfaceProvider(
                        binding.viewFinder.surfaceProvider
                    )
                }

            imageCapture = ImageCapture.Builder()
                .build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector,
                    preview, imageCapture
                )

            }catch (e: Exception){
                Log.d(Constants.TAG, "startCamera Fail", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.REQUEST_CODE_PERMISSIONS){

            if(allPermissionsGranted()){
                startCamera()
            }else{
                Toast.makeText(this,
                    "Permission not granted by the user",
                    Toast.LENGTH_SHORT).show()

                finish()
            }
        }
    }

    private fun allPermissionsGranted() =
        Constants.REQUIRED_PERMISSIONS.all{
            ContextCompat.checkSelfPermission(
                baseContext, it
            ) == PackageManager.PERMISSION_GRANTED
        }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onProgressUpdate(percentage: Int) {
        binding.progressBar.progress = percentage
    }

//    private fun showError(){
//        Toast.makeText(this, "Ha ocurrido un error", Toast.LENGTH_LONG).show()
//    }

}