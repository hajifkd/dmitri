package tokyo.theta.dmitri.data.model.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity
data class Document(
    @PrimaryKey
    val id: String,
    val title: String
)

data class DocumentWithFiles(
    @Embedded
    val document: Document,
    @Relation(parentColumn = "id", entityColumn = "documentId")
    val files: List<File>
)