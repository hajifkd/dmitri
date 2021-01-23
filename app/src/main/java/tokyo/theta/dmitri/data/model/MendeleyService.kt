package tokyo.theta.dmitri.data.model

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*


interface AuthService {
    @FormUrlEncoded
    @POST("/oauth/token")
    suspend fun requestToken(
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String,

        @Field("grant_type") grantType: String = AUTHORIZATION_CODE
    ): Response<AccessToken>

    @FormUrlEncoded
    @POST("/oauth/token")
    suspend fun refreshToken(
        @Field("refresh_token") refreshToken: String,
        @Field("redirect_uri") redirectUri: String,

        @Field("grant_type") grantType: String = REFRESH_TOKEN
    ): Response<AccessToken>

    companion object {
        const val AUTHORIZATION_CODE = "authorization_code"
        const val REFRESH_TOKEN = "refresh_token"
    }
}

interface ApiService {
    @GET("/profiles/me")
    suspend fun profile(): Response<UserProfile>

    @GET
    suspend fun download(@Url fileUrl: String): ResponseBody
}