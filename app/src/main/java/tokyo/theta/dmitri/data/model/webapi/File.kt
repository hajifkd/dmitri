package tokyo.theta.dmitri.data.model.webapi

import com.google.gson.annotations.SerializedName

data class File(
    val id: String,
    @SerializedName("document_id")
    val documentId: String,
    @SerializedName("file_name")
    val fileName: String,
    @SerializedName("filehash")
    val fileHash: String
)
