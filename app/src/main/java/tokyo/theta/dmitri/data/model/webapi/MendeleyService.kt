package tokyo.theta.dmitri.data.model.webapi

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*
import tokyo.theta.dmitri.util.getType


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

    @GET("/folders/{folder_id}/documents")
    suspend fun documentIdsInFolder(@Path("folder_id") folderId: String): Response<List<DocumentId>>

    @GET("/documents")
    suspend fun listDocuments(): Response<List<Document>>

    @GET("/files")
    suspend fun listFiles(): Response<List<File>>

    @GET
    suspend fun download(@Url fileUrl: String): ResponseBody

    @GET("/files/{file_id}")
    suspend fun downloadFile(@Path("file_id") fileId: String): ResponseBody

    @GET
    suspend fun additionalData(@Url fileUrl: String): Response<ResponseBody>
}

suspend inline fun <S, reified T: List<S>> paginates(service: ApiService, resp: Response<T>): List<S>? {
    var headers = resp.headers()
    val result = resp.body()?.toMutableList()
    val gson = Gson()
    val targetType = getType<T>()
    Log.d("type", getType<T>().toString())

    while (true) {
        var nextUrl: String? = null
        for ((name, header) in headers) {
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

        val resp = service.additionalData(nextUrl)
        withContext(Dispatchers.IO) {
            resp.body()?.let {
                result?.addAll(gson.fromJson(it.string(), targetType))
            }
        }
        headers = resp.headers()
    }

    return result
}