package tokyo.theta.dmitri

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import tokyo.theta.dmitri.data.MendeleyApiRepository
import tokyo.theta.dmitri.data.PrefRepository
import kotlin.concurrent.thread

class MendeleyViewModel(private val app: Application) : AndroidViewModel(app) {
    val prefRepository = PrefRepository(app)
    val apiRepository = MendeleyApiRepository(app)

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

    fun buildOAuthUri(): Uri {
        val state = (0..MendeleyViewModel.AUTH_STATE_LENGTH).map { ('a'..'z').random() }
            .joinToString("")
        authState = state
        return apiRepository.buildOAuthUri(state)
    }

    fun getToken() {
        viewModelScope.launch { authCode?.let { apiRepository.getToken(it) } }
    }

    companion object {
        const val AUTH_STATE_LENGTH = 32
    }
}