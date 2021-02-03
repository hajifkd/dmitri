package tokyo.theta.dmitri.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tokyo.theta.dmitri.R
import tokyo.theta.dmitri.data.model.db.*
import tokyo.theta.dmitri.data.model.webapi.DocumentId
import java.io.OutputStream
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
        // TODO check update
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
            File(
                it.id,
                it.documentId,
                it.fileHash,
                it.fileName,
                downloadedFiles.get(it.id)?.localFileName,
                it.id in downloadedFiles
            )
        })
    }

    private fun nameAndRelativePath(file: File): Pair<String, String>? {
        return file.localFileName?.let {
            val localFile = jFile(it)
            Pair(localFile.name, "${Environment.DIRECTORY_DOCUMENTS}/dmitri/${localFile.parent}")
        }
    }

    suspend fun fileUri(file: File): Uri? {
        val resolver = context.contentResolver
        if (file.localFileName == null) {
            return null
        }
        val (name, rPath) = nameAndRelativePath(file)!!

        return withContext(Dispatchers.IO) {
            resolver.query(
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME}=?",
                arrayOf(rPath, name),
                null
            )
        }?.let {
            if (it.count == 0) {
                // new file
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, rPath)
                    put(
                        MediaStore.MediaColumns.MIME_TYPE,
                        MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.split('.').last())
                            ?: "application/octet-stream"
                    )
                }.let {
                    context.contentResolver.run {
                        val uri =
                            insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL), it)
                        Log.d("New file uri", "$uri")
                        uri
                    }
                }
            } else {
                it.moveToFirst()
                val id = it.getLong(it.getColumnIndex(MediaStore.MediaColumns._ID))
                ContentUris.withAppendedId(
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                    id
                )
            }
        }

    }

    private suspend fun outputStream(file: File): OutputStream? {
        return withContext(Dispatchers.IO) {
            file.localFileName?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.run {
                        fileUri(file)?.let { openOutputStream(it) }
                    }
                } else {
                    val docDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    jFile(jFile(docDir, context.getString(R.string.app_name)), it).outputStream()
                }
            }
        }
    }


    // TODO check if it works
    suspend fun saveFile(file: File, data: ByteArray): Boolean {
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

            val fos = outputStream(file)

            if (fos == null || runCatching { fos.write(data) }.isFailure) {
                return false // Handle?
            }

            database.getFileDao().updateFile(file)

            true
        }
    }
}