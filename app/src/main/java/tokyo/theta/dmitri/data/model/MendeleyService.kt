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

    // TODO test pagination...
    @GET("/folders")
    suspend fun listFolders(@Query("limit") limit: Int = 5): Response<List<Folder>>

    @GET("/documents")
    suspend fun listDocuments(): Response<List<Document>>

    @GET
    suspend fun download(@Url fileUrl: String): ResponseBody

    @GET
    suspend fun <T> additionalData(@Url fileUrl: String): Response<T>
}

suspend fun <T> paginates(service: ApiService, resp: Response<List<T>>): List<T>? {
    var resp = resp
    val result = resp.body()?.toMutableList()

    while (true) {
        var nextUrl: String? = null
        for ((name, header) in resp.headers()) {
            if (name == "Link") {
                val elements = header.split(";").map { it.trim() }
                if (elements.size == 2 && listOf("rel", "\"next\"") == elements[1].split("=")
                        .map { it.trim() } && elements[0].length > 2
                ) {
                    // elements[0] is the pagination data
                    nextUrl = elements[0].substring(1, elements[0].length - 1)
                }
            }
        }

        if (nextUrl == null) {
            break
        }

        resp = service.additionalData(nextUrl)
        resp.body()?.let { result?.addAll(it) }
    }

    return result
}