package tokyo.theta.dmitri.data.model.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(version = 1, entities = [File::class, Folder::class, Document::class, FolderDocumentCrossRef::class])
abstract class MendeleyDatabase: RoomDatabase() {
    abstract fun getFileDao() : FileDao
    abstract fun getFolderDao() : FolderDao
    abstract fun getDocumentDao() : DocumentDao
}