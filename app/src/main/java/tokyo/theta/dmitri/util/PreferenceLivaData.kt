package tokyo.theta.dmitri.util

import android.content.SharedPreferences
import androidx.lifecycle.LiveData

abstract class PreferenceLivaData<T>(private val pref: SharedPreferences, private val key: String) :
    LiveData<T>() {
    abstract fun getValueFromPreference(): T
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, k ->
        if (k == key) {
            value = getValueFromPreference()
        }
    }

    init {
        value = getValueFromPreference()
    }
}