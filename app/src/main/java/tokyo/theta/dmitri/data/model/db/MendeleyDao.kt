package tokyo.theta.dmitri.data.model.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface FolderDao {
    @Insert
    suspend fun insertFolders(folders: List<Folder>)

    @Query("delete from Folder")
    suspend fun clearFolders()

    @Transaction
    @Query("select * from Folder")
    fun folders(): LiveData<List<FolderContent>>
}

@Dao
interface DocumentDao {

    @Insert
    suspend fun insertDocuments(documents: List<Document>)

    @Query("delete from Document")
    suspend fun clearDocuments()

    @Transaction
    @Query("select * from Document")
    fun documents(): LiveData<List<DocumentWithFiles>>


    @Insert
    suspend fun insertFolderDocumentCrossRefs(crossRefs: List<FolderDocumentCrossRef>)

    @Query("delete from FolderDocumentCrossRef")
    suspend fun clearFolderDocumentCrossRef()

    @Query("select * from Document where id = :id limit 1")
    suspend fun findById(id: String): Document?

    @Query("select Folder.* from Folder inner join FolderDocumentCrossRef on FolderDocumentCrossRef.folderId = Folder.id where FolderDocumentCrossRef.documentId = :id limit 1")
    suspend fun findOneParentFolderById(id: String): Folder?
}

@Dao
interface FileDao {
    @Insert
    suspend fun insertFiles(files: List<File>)

    @Query("delete from File")
    suspend fun clearFiles()

    @Delete
    suspend fun deleteFile(file: File)

    @Update
    suspend fun updateFile(file: File)

    @Query("select * from File where id = :id limit 1")
    suspend fun findById(id: String): File?
}