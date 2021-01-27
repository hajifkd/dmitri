package tokyo.theta.dmitri.data.model.webapi

import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import java.io.IOException

sealed class TokenResult

data class AccessToken(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("token_type")
    val tokenType: String,
    @SerializedName("expires_in")
    val expiresIn: Int
): TokenResult()

sealed class TokenError: TokenResult()

data class JsonError(val error: JsonSyntaxException): TokenError()

data class AuthError(
    val error: String?,
    @SerializedName("error_description")
    val errorDescription: String?
): TokenError()

data class NetworkError(val error: IOException): TokenError()