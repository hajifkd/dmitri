package tokyo.theta.dmitri.data.model

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface AuthService {
    @FormUrlEncoded
    @POST("/oauth/token")
    fun requestToken(
        @Field("grant_type") grantType: String,
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String
    ): Call<AccessToken>

    @FormUrlEncoded
    @POST("/oauth/token")
    fun refreshToken(
        @Field("grant_type") grantType: String,
        @Field("refresh_token") refreshToken: String,
        @Field("redirect_uri") redirectUri: String
    ): Call<AccessToken>

    companion object {
        const val AUTHORIZATION_CODE = "authorization_code"
        const val REFRESH_TOKEN = "refresh_token"
    }
}