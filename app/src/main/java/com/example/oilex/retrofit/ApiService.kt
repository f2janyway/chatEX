package com.example.oilex.retrofit

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("/upload")
    fun postImage(@Part image:MultipartBody.Part, @Part("username") id:RequestBody): Call<ResponseBody>
//    14.4.84.182
    companion object{                                       
        val retrofit = Retrofit.Builder().baseUrl("http://14.4.84.182:3000")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}