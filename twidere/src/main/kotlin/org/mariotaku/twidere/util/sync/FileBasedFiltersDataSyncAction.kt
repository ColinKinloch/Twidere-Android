package org.mariotaku.twidere.util.sync

import android.content.Context
import android.util.Xml
import org.mariotaku.ktextension.mapToArray
import org.mariotaku.ktextension.nullableContentEquals
import org.mariotaku.twidere.extension.bulkDelete
import org.mariotaku.twidere.extension.model.*
import org.mariotaku.twidere.model.FiltersData
import org.mariotaku.twidere.provider.TwidereDataStore.Filters
import java.io.Closeable
import java.io.File
import java.io.IOException

abstract class FileBasedFiltersDataSyncAction<DownloadSession : Closeable, UploadSession : Closeable>(
        val context: Context
) : SingleFileBasedDataSyncAction<FiltersData, File, DownloadSession, UploadSession>() {

    override fun File.loadSnapshot(): FiltersData {
        return reader().use {
            val snapshot = FiltersData()
            val parser = Xml.newPullParser()
            parser.setInput(it)
            snapshot.parse(parser)
            return@use snapshot
        }
    }

    override fun File.saveSnapshot(data: FiltersData) {
        writer().use {
            val serializer = Xml.newSerializer()
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
            serializer.setOutput(it)
            data.serialize(serializer)
        }
    }

    override var File.snapshotLastModified: Long
        get() = this.lastModified()
        set(value) {
            this.setLastModified(value)
        }

    override fun loadFromLocal(): FiltersData {
        return FiltersData().apply {
            read(context.contentResolver)
            initFields()
        }
    }

    override fun addToLocal(data: FiltersData) {
        data.write(context.contentResolver, deleteOld = false)
    }

    override fun removeFromLocal(data: FiltersData) {
        val cr = context.contentResolver
        cr.bulkDelete(Filters.Users.CONTENT_URI, Filters.Users.USER_KEY, data.users?.mapToArray { it.userKey.toString() })
        cr.bulkDelete(Filters.Keywords.CONTENT_URI, Filters.Keywords.VALUE, data.keywords?.mapToArray { it.value })
        cr.bulkDelete(Filters.Sources.CONTENT_URI, Filters.Sources.VALUE, data.sources?.mapToArray { it.value })
        cr.bulkDelete(Filters.Links.CONTENT_URI, Filters.Links.VALUE, data.links?.mapToArray { it.value })
    }

    override fun newData(): FiltersData {
        return FiltersData()
    }

    override fun FiltersData.minus(data: FiltersData): FiltersData {
        val diff = FiltersData()
        diff.addAll(this, true)
        diff.removeAllData(data)
        return diff
    }

    override fun FiltersData.addAllData(data: FiltersData): Boolean {
        return this.addAll(data, ignoreDuplicates = true)
    }

    override fun FiltersData.removeAllData(data: FiltersData): Boolean {
        return this.removeAll(data)
    }

    override fun FiltersData.isDataEmpty(): Boolean {
        return this.isEmpty()
    }

    override fun newSnapshotStore(): File {
        val syncDataDir: File = context.syncDataDir.mkdirIfNotExists() ?: throw IOException()
        return File(syncDataDir, "filters.xml")
    }

    override fun FiltersData.dataContentEquals(localData: FiltersData): Boolean {
        return this.users.nullableContentEquals(localData.users)
                && this.keywords.nullableContentEquals(localData.keywords)
                && this.sources.nullableContentEquals(localData.sources)
                && this.links.nullableContentEquals(localData.links)
    }

    override val whatData: String = "filters"
}