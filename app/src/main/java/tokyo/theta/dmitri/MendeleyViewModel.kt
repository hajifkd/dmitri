package tokyo.theta.dmitri

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import tokyo.theta.dmitri.data.LoginResult
import tokyo.theta.dmitri.data.MendeleyApiRepository
import tokyo.theta.dmitri.data.MendeleyDataRepository
import tokyo.theta.dmitri.data.PrefRepository
import tokyo.theta.dmitri.data.model.db.Document
import tokyo.theta.dmitri.data.model.db.File as DbFile
import tokyo.theta.dmitri.data.model.db.FolderContent
import tokyo.theta.dmitri.data.model.webapi.AccessToken
import tokyo.theta.dmitri.data.model.webapi.NetworkError
import java.io.File
import java.lang.Exception

class MendeleyViewModel(private val app: Application) : AndroidViewModel(app) {
    val prefRepository = PrefRepository(app)
    val apiRepository = MendeleyApiRepository(app)
    val dataRepository = MendeleyDataRepository(app)

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult>
        get() = _loginResult

    private val _profilePhoto = MutableLiveData<File>()
    val profilePhoto: LiveData<File>
        get() = _profilePhoto

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String>
        get() = _userName

    // db data
    val folders: LiveData<List<FolderContent>> = dataRepository.database.getFolderDao().folders()

    suspend fun findFilesForDocument(document: Document): List<DbFile> {
        return dataRepository.database.getFileDao().findByDocumentId(document.id)
    }

    private fun saveUserName(v: String) {
        prefRepository.setStringPreference(app.getString(R.string.pref_user_name), v)
    }

    private fun loadUserName(): String? =
        prefRepository.getStringPreference(app.getString(R.string.pref_user_name))

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

    fun buildOAuthUri(): Uri {
        val state = (0..MendeleyViewModel.AUTH_STATE_LENGTH).map { ('a'..'z').random() }
            .joinToString("")
        authState = state
        return apiRepository.buildOAuthUri(state)
    }

    fun login() {
        val authCode = authCode
        if (authCode == null) {
            _loginResult.value = LoginResult.Failed
            return
        }


        viewModelScope.launch {
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
                            _userName.value = prof.displayName
                            Log.d("User Name", "$_userName.value")
                            _loginResult.value = LoginResult.Successful
                            prof.photo?.standard?.let { apiRepository.downloadProfilePhoto(it) }
                                ?.let {
                                    _profilePhoto.value = it
                                    saveUserName(prof.displayName)
                                }
                        } else {
                            Log.e("Profile", "failed to get the user data")
                            _loginResult.postValue(LoginResult.Failed)
                        }
                    } catch (e: Exception) {
                        Log.e("Profile", "failed to get the user data")
                        _loginResult.postValue(LoginResult.Failed)
                    }
                }
                is NetworkError -> {
                    val u = loadUserName()
                    if (u != null) {
                        _userName.value = u
                        _profilePhoto.value = apiRepository.profileFile()
                        _loginResult.postValue(LoginResult.Offline)
                    } else {
                        _loginResult.postValue(LoginResult.OfflineWithoutData)
                    }
                }
                else -> {
                    _loginResult.postValue(LoginResult.Failed)
                }
            }
        }
    }

    fun updateData() {
        viewModelScope.launch {
            val folders = accessToken?.let { apiRepository.listFolders(it) } ?: listOf()
            val folderContents = folders.mapNotNull { folder ->
                accessToken?.let { token ->
                    apiRepository.documentIdsInFolder(
                        token,
                        folder
                    )?.let { Pair(folder, it) }
                }
            }.toMap()
            val documents = accessToken?.let { apiRepository.listDocuments(it) } ?: listOf()
            val files = accessToken?.let { apiRepository.listFiles(it) } ?: listOf()

            dataRepository.updateData(folders, folderContents, documents, files)
        }
    }

    companion object {
        const val AUTH_STATE_LENGTH = 32
    }
}