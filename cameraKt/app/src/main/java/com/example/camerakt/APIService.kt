package com.example.camerakt

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Url

interface APIService {

    @Multipart
    @POST("Api.php?apicall=upload")
    fun uploadImage(
        @Part image:MultipartBody.Part,
        @Part("desc") desc: RequestBody
    ):Call<UploadResponse>

    companion object{
        operator fun invoke():APIService{
            return Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(APIService::class.java)
        }
    }
}