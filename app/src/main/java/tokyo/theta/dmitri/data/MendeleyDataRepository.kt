package tokyo.theta.dmitri.data

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tokyo.theta.dmitri.data.model.db.*
import tokyo.theta.dmitri.data.model.webapi.DocumentId
import tokyo.theta.dmitri.util.sha1Hash
import java.util.*
import tokyo.theta.dmitri.data.model.webapi.Document as wDocument
import tokyo.theta.dmitri.data.model.webapi.File as wFile
import tokyo.theta.dmitri.data.model.webapi.Folder as wFolder
import java.io.File as jFile


class MendeleyDataRepository(private val context: Context) {
    val database = Room.databaseBuilder(context, MendeleyDatabase::class.java, "dmitri.db").build()

    private suspend fun clearData() {
        database.getDocumentDao().clearFolderDocumentCrossRef()
        database.getFileDao().clearFiles()
        database.getDocumentDao().clearDocuments()
        database.getFolderDao().clearFolders()
    }

    suspend fun updateData(
        wFolders: List<wFolder>,
        folderContents: Map<wFolder, List<DocumentId>>,
        wDocuments: List<wDocument>,
        wFiles: List<wFile>
    ) {
        val downloadedFiles =
            database.getFileDao().downloadedFiles().map { Pair(it.id, it) }.toMap()

        clearData()

        database.getFolderDao().insertFolders(wFolders.map { Folder(it.id, it.name) })
        database.getDocumentDao().insertDocuments(wDocuments.map { Document(it.id, it.title) })
        database.getDocumentDao()
            .insertFolderDocumentCrossRefs(folderContents.flatMap { (folder, documentIds) ->
                documentIds.map {
                    FolderDocumentCrossRef(
                        folder.id,
                        it.id
                    )
                }
            })

        database.getFileDao().insertFiles(wFiles.map {
            downloadedFiles[it.id] ?: File(
                it.id,
                it.documentId,
                it.fileHash,
                it.fileName,
                null,
                false
            )

        })
    }

    suspend fun dirtyFiles(): List<File> = database.getFileDao().downloadedFiles()
        .filter {
            Log.d("saved hash", "${it}")
            it.fileHash.toLowerCase(Locale.ENGLISH) != filePath(it)?.let {
                withContext(Dispatchers.Default) {
                    val hash = sha1Hash(it)
                    Log.d("local hash", hash)
                    hash
                }
            }
        }


    fun filePath(file: File): jFile? {
        if (file.localFileName == null) {
            return null
        }

        return jFile(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            file.localFileName!!
        )

    }

    fun addPrefix(file: jFile): jFile {
        return jFile(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            file.path
        )

    }

    suspend fun updateFileId(file: File, newId: String) {
        val hash = withContext(Dispatchers.Default) {
            filePath(file)?.let { sha1Hash(it) }
        } ?: return
        val newFile = file.copy(id = newId, fileHash = hash)
        database.getFileDao().apply {
            deleteFile(file)
            insertFile(newFile)
        }
    }

    suspend fun saveAsNewFile(file: File, newId: String) {
        val hash = withContext(Dispatchers.Default) {
            filePath(file)?.let { sha1Hash(it) }
        } ?: return
        val oldFile = file.copy(localFileName = null, isDownloaded = false)
        val newFile = file.copy(id = newId, fileHash = hash)
        database.getFileDao().apply {
            insertFile(newFile)
            updateFile(oldFile)
        }
    }

    suspend fun saveFile(file: File, data: ByteArray): Boolean {
        Log.d("saveFile", "$file")
        file.isDownloaded = true
        if (file.localFileName == null) {
            val document = database.getDocumentDao().findById(file.documentId)
            val folder =
                document?.let { database.getDocumentDao().findOneParentFolderById(it.id) }
            var f = document?.let { d ->
                folder?.let { f -> jFile(f.name) }?.let { jFile(it, d.title) }
            }?.let { jFile(it, file.name) } ?: return false

            var i = 1
            while (addPrefix(f).exists()) {
                f = jFile(f.parent, "${f.nameWithoutExtension} ($i).${f.extension}")
                i += 1
            }

            file.localFileName = f.path
        }

        val fos = filePath(file)?.run {
            Log.d("saveFile", "save file to ${this}.")
            parentFile?.run { mkdirs() }
            withContext(Dispatchers.IO) {
                createNewFile()
            }
            outputStream()
        }

        if (fos == null || runCatching { fos.write(data) }.isFailure) {
            return false // Handle?
        }

        database.getFileDao().updateFile(file)

        return true
    }
}