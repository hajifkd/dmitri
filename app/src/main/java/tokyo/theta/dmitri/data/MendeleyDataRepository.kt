package tokyo.theta.dmitri.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.room.Room
import tokyo.theta.dmitri.R
import tokyo.theta.dmitri.data.model.db.*
import java.io.FileOutputStream
import java.io.File as jFile
import tokyo.theta.dmitri.data.model.webapi.Folder as wFolder
import tokyo.theta.dmitri.data.model.webapi.Document as wDocument
import tokyo.theta.dmitri.data.model.webapi.File as wFile

class MendeleyDataRepository(val context: Context) {
    val database = Room.databaseBuilder(context, MendeleyDatabase::class.java, "dmitri.db").build()

    suspend fun saveData(
        wFolders: List<wFolder>,
        folderContents: Map<String, List<wDocument>>,
        wDocuments: List<wDocument>,
        wFiles: List<wFile>
    ) {
        database.getFolderDao().insertFolders(wFolders.map { Folder(it.id, it.name) })
        database.getDocumentDao().insertDocuments(wDocuments.map { Document(it.id, it.title) })
        database.getDocumentDao()
            .insertFolderDocumentCrossRefs(folderContents.flatMap { (folderId, documentIds) ->
                documentIds.map {
                    FolderDocumentCrossRef(
                        folderId,
                        it.id
                    )
                }
            })
        database.getFileDao().insertFiles(wFiles.map {
            File(
                it.id,
                it.documentId,
                it.fileHash,
                it.fileName,
                null,
                false
            )
        })
    }


    // TODO check if it works
    suspend fun saveFile(fileId: String, data: ByteArray): Boolean {
        var file = database.getFileDao().findById(fileId)
        return if (file == null) {
            false
        } else {
            file.isDownloaded = true
            if (file.localFileName == null) {
                val document = database.getDocumentDao().findById(file.documentId)
                val folder =
                    document?.let { database.getDocumentDao().findOneParentFolderById(it.id) }
                val f = document?.let { d ->
                    folder?.let { f -> jFile(f.name) }?.let { jFile(it, d.title) }
                }?.let { jFile(it, file.name) } ?: return false

                file.localFileName = f.path
            }

            val fos = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val localFile = jFile(file.localFileName)
                    ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, localFile.name)
                        put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            "${Environment.DIRECTORY_DOCUMENTS}/${localFile.parent}"
                        )
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")

                    }.let {
                        context.contentResolver.run {
                            val uri =
                                insert(
                                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                                    it
                                )
                            if (uri == null) {
                                return false
                            }
                            openOutputStream(uri)
                        }
                    }
                } else {
                    val docDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    val fileToSave =
                        jFile(
                            jFile(docDir, context.getString(R.string.app_name)),
                            file.localFileName
                        )
                    FileOutputStream(fileToSave)
                }
            }

            if (fos.isFailure || runCatching { fos.getOrNull()!!.write(data) }.isFailure) {
                return false // Handle?
            }

            true
        }
    }
}