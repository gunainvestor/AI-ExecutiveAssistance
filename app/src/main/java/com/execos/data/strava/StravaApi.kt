package com.execos.data.strava

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface StravaApi {
    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun tokenExchange(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") code: String,
        @Field("grant_type") grantType: String = "authorization_code",
    ): StravaTokenResponse

    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun refreshToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String = "refresh_token",
    ): StravaTokenResponse

    @GET("api/v3/athlete/activities")
    suspend fun listActivities(
        @Header("Authorization") bearer: String,
        @Query("after") afterEpochSeconds: Long? = null,
        @Query("before") beforeEpochSeconds: Long? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50,
    ): List<StravaActivityDto>
}

