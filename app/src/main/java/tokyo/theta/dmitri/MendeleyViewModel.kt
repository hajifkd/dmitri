package tokyo.theta.dmitri

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tokyo.theta.dmitri.data.LoginResult
import tokyo.theta.dmitri.data.MendeleyApiRepository
import tokyo.theta.dmitri.data.PrefRepository
import tokyo.theta.dmitri.data.model.AccessToken
import tokyo.theta.dmitri.data.model.NetworkError
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.lang.Exception
import java.net.URL
import kotlin.concurrent.thread

class MendeleyViewModel(private val app: Application) : AndroidViewModel(app) {
    val prefRepository = PrefRepository(app)
    val apiRepository = MendeleyApiRepository(app)
    val loginResult = MutableLiveData<LoginResult>()
    val profilePhoto = MutableLiveData<File>()

    var authCode: String?
        get() = prefRepository.getStringPreference(app.getString(R.string.pref_auth_code))
        set(value) = prefRepository.setStringPreference(
            app.getString(R.string.pref_auth_code),
            value
        )

    var authState: String?
        get() = prefRepository.getStringPreference(app.getString(R.string.pref_auth_state))
        set(value) = prefRepository.setStringPreference(
            app.getString(R.string.pref_auth_state),
            value
        )

    var accessToken: String?
        get() = prefRepository.getStringPreference(app.getString(R.string.pref_access_token))
        set(value) = prefRepository.setStringPreference(
            app.getString(R.string.pref_access_token),
            value
        )

    private var refreshToken: String?
        get() = prefRepository.getStringPreference(app.getString(R.string.pref_refresh_token))
        set(value) = prefRepository.setStringPreference(
            app.getString(R.string.pref_refresh_token),
            value
        )

    var userName: String?
        get() = prefRepository.getStringPreference(app.getString(R.string.pref_user_name))
        set(value) = prefRepository.setStringPreference(
            app.getString(R.string.pref_user_name),
            value
        )

    fun buildOAuthUri(): Uri {
        val state = (0..MendeleyViewModel.AUTH_STATE_LENGTH).map { ('a'..'z').random() }
            .joinToString("")
        authState = state
        return apiRepository.buildOAuthUri(state)
    }

    fun login() {
        val authCode = authCode
        if (authCode == null) {
            loginResult.value = LoginResult.Failed
            return
        }


        viewModelScope.launch(Dispatchers.IO) {
            val rToken = refreshToken
            val result = if (rToken != null) {
                apiRepository.refreshToken(rToken)
            } else {
                apiRepository.getToken(authCode)
            }

            when (result) {
                is AccessToken -> {
                    accessToken = result.accessToken
                    refreshToken = result.refreshToken
                    try {
                        val prof = apiRepository.getUserProfile(result.accessToken)
                        if (prof?.displayName != null) {
                            userName = prof.displayName
                            Log.d("User Name", "$userName")
                            loginResult.postValue(LoginResult.Successful)
                            prof.photo?.standard?.let { apiRepository.downloadProfilePhoto(it) }
                                ?.let { profilePhoto.postValue(it) }
                        } else {
                            Log.e("Profile", "failed to get the user data")
                            loginResult.postValue(LoginResult.Failed)
                        }
                    } catch (e: Exception) {
                        Log.e("Profile", "failed to get the user data")
                        loginResult.postValue(LoginResult.Failed)
                    }
                }
                is NetworkError -> {
                    if (userName != null) {
                        loginResult.postValue(LoginResult.Offline)
                    } else {
                        loginResult.postValue(LoginResult.OfflineWithoutData)
                    }
                }
                else -> {
                    loginResult.postValue(LoginResult.Failed)
                }
            }
        }
    }

    companion object {
        const val AUTH_STATE_LENGTH = 32
    }
}