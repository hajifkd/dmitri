package tokyo.theta.dmitri.data

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager

class PrefRepository(val context: Context) {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun preferenceMutableLiveStringData(key: String): MutableLiveData<String> =
        MutableLiveData<String>().apply {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            preferences.getString(key, null).let { value = it }
            observeForever {
                preferences.edit().putString(key, it).apply()
            }
        }

    fun getStringPreference(key: String): String? = preferences.getString(key, null)
    fun setStringPreference(key: String, value: String?) =
        preferences.edit().putString(key, value).apply()
}