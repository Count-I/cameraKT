package com.example.camerakt

import android.Manifest

object Constants {

    const val TAG = "camerax"
    const val FILE_NAME_FORMAT = "yy-MM-dd-HH-mm-ss_SSS"
    const val REQUEST_CODE_PERMISSIONS = 123
    const val BASE_URL = "http://10.0.2.2/ImageUploader/"
    val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)


}