package tokyo.theta.dmitri.data.model.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [ForeignKey(
        entity = Document::class,
        parentColumns = ["id"],
        childColumns = ["documentId"]
    )]
    // indices = [Index(value = ["fileHash"], unique = true)]
    // Oh, files does not need to be all different!
)
data class File(
    @PrimaryKey
    val id: String,
    val documentId: String,
    val fileHash: String,
    val name: String,
    var localFileName: String?,
    var isDownloaded: Boolean
)
