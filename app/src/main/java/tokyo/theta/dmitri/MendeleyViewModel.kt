package tokyo.theta.dmitri

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager

class MendeleyViewModel(application: Application) : AndroidViewModel(application) {
    val authCode: MutableLiveData<String> = application.run {
        MutableLiveData<String>().apply {
            val preferences = PreferenceManager.getDefaultSharedPreferences(application)
            preferences.getString(getString(R.string.pref_auth_code), null)?.let { value = it }
            observeForever { preferences.edit().putString(getString(R.string.pref_auth_code), it).apply() }
        }
    }

    val authState: String? = null
}