package tokyo.theta.dmitri.data.model.webapi

import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
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

    @DELETE("/files/{file_id}")
    suspend fun deleteFile(@Path("file_id") fileId: String): Response<ResponseBody>

    @POST("/files")
    suspend fun uploadFile(
        @Header("Link") docInfo: String,
        @Header("Content-Disposition") fileInfo: String,
        @Header("Content-Type") mime: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    @GET
    suspend fun additionalData(@Url url: String): Response<ResponseBody>
}

private fun docInfo(documentId: String): String =
    "<https://api.mendeley.com/documents/$documentId>; rel=\"document\""

private fun fileInfo(fileName: String): String = "attachment; filename=\"$fileName\""

suspend fun uploadFile(service: ApiService, documentId: String, file: java.io.File): String? {
    val resp = service.uploadFile(
        docInfo(documentId),
        fileInfo(file.name),
        MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(file.extension) ?: "application/octet-stream", file.asRequestBody()
    )
    return resp.headers()["Location"]?.let { Uri.parse(it).lastPathSegment }
}

suspend inline fun <S, reified T : List<S>> paginates(
    service: ApiService,
    resp: Response<T>
): List<S>? {
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