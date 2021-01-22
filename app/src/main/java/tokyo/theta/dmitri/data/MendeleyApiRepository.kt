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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tokyo.theta.dmitri.R
import tokyo.theta.dmitri.data.model.*

class MendeleyApiRepository(val context: Context) {

    private fun webService(): AuthService {
        return Retrofit.Builder().apply {
            baseUrl("https://api.mendeley.com/")
            addConverterFactory(GsonConverterFactory.create())
            client(
                OkHttpClient.Builder()
                    .addInterceptor(Interceptor { chain ->
                        chain.proceed(
                            chain.request().newBuilder().addHeader(
                                "Authorization",
                                Credentials.basic(
                                    context.getString(R.string.mendeley_client_id),
                                    context.getString(R.string.mendeley_secret)
                                )
                            ).build()
                        )
                    })
                    .addInterceptor(HttpLoggingInterceptor().apply { setLevel(HttpLoggingInterceptor.Level.BODY) })
                    .build()
            )
        }.build().create(AuthService::class.java)
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

    suspend fun getToken(authCode: String) {
        withContext(Dispatchers.IO) {
            val service = webService()
            val response =
                service.requestToken(AuthService.AUTHORIZATION_CODE, authCode, redirectUri)
                    .execute()

            // TODO check network error
            val result = if (response.isSuccessful) {
                response.body()
            } else {
                try {
                    response.errorBody()?.run {
                        GsonBuilder().create()
                            .fromJson(string(), AuthError::class.java)
                    }
                } catch (e: JsonSyntaxException) {
                    JsonError(e)
                }
            }

            // TODO add result to sharedpref
            // observe sharedpreflivedata

            Log.d("aaaaaaaa", "result: ${result}")
        }
    }
}