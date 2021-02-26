package tokyo.theta.dmitri.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tokyo.theta.dmitri.R
import tokyo.theta.dmitri.data.model.webapi.*
import java.io.File
import java.io.IOException

class MendeleyApiRepository(val context: Context) {

    private fun getClient(
        credential: String?,
        httpLogLevel: HttpLoggingInterceptor.Level
    ): Retrofit {
        return Retrofit.Builder().apply {
            baseUrl("https://api.mendeley.com/")
            addConverterFactory(GsonConverterFactory.create())
            client(
                OkHttpClient.Builder()
                    .addInterceptor(Interceptor { chain ->
                        chain.proceed(
                            chain.request().newBuilder().apply {
                                credential?.let {
                                    addHeader("Authorization", it)
                                }
                            }.build()
                        )
                    })
                    .addInterceptor(HttpLoggingInterceptor().apply { setLevel(httpLogLevel) })
                    .build()
            )
        }.build()
    }

    private fun authService(): AuthService {
        return getClient(
            Credentials.basic(
                context.getString(R.string.mendeley_client_id),
                context.getString(R.string.mendeley_secret)
            ), HttpLoggingInterceptor.Level.BODY
        ).create(AuthService::class.java)
    }

    private fun apiService(
        accessToken: String,
        httpLogLevel: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.BODY
    ): ApiService {
        return getClient("Bearer $accessToken", httpLogLevel).create(ApiService::class.java)
    }

    private val redirectUri =
        context.run { "${getString(R.string.app_name)}://${getString(R.string.mendeley_auth)}/" }

    fun buildOAuthUri(authState: String): Uri = context.run {
        // e.g. https://api.mendeley.com/oauth/authorize?client_id=773&redirect_uri=http:%2F%2Flocalhost%2Fmendeley%2Fserver_sample.php&response_type=code&scope=all&state=213653957730.97845
        Uri.Builder().scheme("https").authority("api.mendeley.com").appendPath("oauth")
            .appendPath("authorize")
            .appendQueryParameter("client_id", getString(R.string.mendeley_client_id))
            .appendQueryParameter(
                "redirect_uri",
                "${getString(R.string.app_name)}://${getString(R.string.mendeley_auth)}/"
            )
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", "all")
            .appendQueryParameter("state", authState).build()
    }

    private suspend fun tokenApi(f: suspend (AuthService) -> Response<AccessToken>): TokenResult? {
        val service = authService()
        val response = try {
            f(service)
        } catch (e: IOException) {
            return NetworkError(e)
        }

        // TODO check network error
        val result = if (response.isSuccessful) {
            response.body()
        } else {
            try {
                response.errorBody()?.run {
                    GsonBuilder().create()
                        .fromJson(withContext(Dispatchers.IO) { string() }, AuthError::class.java)
                }
            } catch (e: JsonSyntaxException) {
                JsonError(e)
            }
        }

        // TODO add result to sharedpref
        // observe sharedpreflivedata

        return result
    }

    suspend fun getToken(authCode: String): TokenResult? {
        return tokenApi { it.requestToken(authCode, redirectUri) }
    }

    suspend fun refreshToken(refreshCode: String): TokenResult? {
        return tokenApi { it.refreshToken(refreshCode, redirectUri) }
    }

    suspend fun getUserProfile(accessToken: String): UserProfile? {
        return apiService(accessToken).profile().body()
    }

    suspend fun listFolders(accessToken: String): List<Folder>? {
        val service = apiService(accessToken)
        return paginates(service, service.listFolders())
    }

    suspend fun listDocuments(accessToken: String): List<Document>? {
        val service = apiService(accessToken)
        return paginates(service, service.listDocuments())
    }

    suspend fun documentIdsInFolder(accessToken: String, folder: Folder): List<DocumentId>? {
        val service = apiService(accessToken)
        return paginates(service, service.documentIdsInFolder(folder.id))
    }

    suspend fun listFiles(accessToken: String): List<tokyo.theta.dmitri.data.model.webapi.File>? {
        val service = apiService(accessToken)
        return paginates(service, service.listFiles())
    }

    suspend fun downloadFile(accessToken: String, fileId: String): ByteArray {
        val service = apiService(accessToken, HttpLoggingInterceptor.Level.HEADERS)
        return withContext(Dispatchers.IO) {
            service.downloadFile(fileId).bytes()
        }
    }

    suspend fun deleteFile(accessToken: String, fileId: String) {
        val service = apiService(accessToken)
        withContext(Dispatchers.IO) {
            service.deleteFile(fileId)
        }
    }

    suspend fun uploadFile(accessToken: String, documentId: String, file: File): String? {
        val service = apiService(accessToken, HttpLoggingInterceptor.Level.HEADERS)
        return uploadFile(service, documentId, file)
    }

    fun profileFile(): File = File(context.filesDir, "profile")

    suspend fun downloadProfilePhoto(url: String): File? {
        val file = profileFile()
        val service =
            getClient(null, HttpLoggingInterceptor.Level.HEADERS).create(ApiService::class.java)
        return try {
            val body = service.download(url)
            withContext(Dispatchers.IO) {
                file.writeBytes(body.bytes())
            }
            Log.d("profile", file.toString())
            file
        } catch (e: IOException) {
            Log.e("error", e.toString())
            null
        }
    }
}