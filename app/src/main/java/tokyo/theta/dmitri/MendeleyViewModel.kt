package tokyo.theta.dmitri

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.preference.PreferenceManager
import tokyo.theta.dmitri.data.MendeleyRepository

class MendeleyViewModel(private val app: Application) : AndroidViewModel(app) {
    val repository = MendeleyRepository(app)
    var authCode: String?
        get() = repository.getStringPreference(app.getString(R.string.pref_auth_code))
        set(value) = repository.setStringPreference(
            app.getString(R.string.pref_auth_code),
            value
        )

    var authState: String?
        get() = repository.getStringPreference(app.getString(R.string.pref_auth_state))
        set(value) = repository.setStringPreference(
            app.getString(R.string.pref_auth_state),
            value
        )

    companion object {
        const val AUTH_STATE_LENGTH = 32
    }
}