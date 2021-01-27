package tokyo.theta.dmitri.data.model.db

import androidx.room.*

@Entity(indices = [Index(value = ["name"], unique = true)])
data class Folder(
    @PrimaryKey
    val id: String,
    val name: String
)

@Entity(
    primaryKeys = ["documentId", "folderId"],
    foreignKeys = [ForeignKey(
        entity = Document::class,
        parentColumns = ["id"],
        childColumns = ["documentId"]
    ), ForeignKey(
        entity = Folder::class,
        parentColumns = ["id"],
        childColumns = ["folderId"]
    )]
)
data class FolderDocumentCrossRef(
    val folderId: String,
    val documentId: String
)

data class FolderContent(
    @Embedded
    val folder: Folder,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            FolderDocumentCrossRef::class,
            parentColumn = "folderId",
            entityColumn = "documentId"
        )
    )
    val documents: List<Document>
)