package tokyo.theta.dmitri.data.model.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface MendeleyDao {
    @Insert
    suspend fun insertFolders(folders: List<Folder>)

    @Query("delete from Folder")
    suspend fun clearFolders()

    @Transaction
    @Query("select * from Folder")
    fun folders(): LiveData<List<FolderContent>>


    @Insert
    suspend fun insertDocuments(documents: List<Document>)

    @Query("delete from Document")
    suspend fun clearDocuments()

    @Transaction
    @Query("select * from Document")
    fun documents(): LiveData<List<DocumentWithFiles>>


    @Insert
    suspend fun insertFolderDocumentCrossRefs(documents: List<FolderDocumentCrossRef>)

    @Query("delete from FolderDocumentCrossRef")
    suspend fun clearFolderDocumentCrossRef()


    @Insert
    suspend fun insertFiles(documents: List<File>)

    @Query("delete from File")
    suspend fun clearFiles()

    @Delete
    suspend fun deleteFile(file: File)

    @Update
    suspend fun updateFile(file: File)

    suspend fun clearDb() {
        clearFiles()
        clearFolderDocumentCrossRef()
        clearDocuments()
        clearFolderDocumentCrossRef()
    }
}