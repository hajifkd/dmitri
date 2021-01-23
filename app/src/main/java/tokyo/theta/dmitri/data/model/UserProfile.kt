package tokyo.theta.dmitri.data.model

import com.google.gson.annotations.SerializedName

data class UserProfile(
    @SerializedName("display_name")
    val displayName: String?,
    val email: String?,
    val photo: UserIcon?
)

data class UserIcon(val square: String?, val standard: String?)