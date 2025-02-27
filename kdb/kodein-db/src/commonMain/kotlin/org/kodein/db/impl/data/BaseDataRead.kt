package org.kodein.db.impl.data

import org.kodein.db.Options
import org.kodein.db.Value
import org.kodein.db.ascii.readAscii
import org.kodein.db.data.DataCursor
import org.kodein.db.data.DataOptions
import org.kodein.db.data.DataRead
import org.kodein.db.invoke
import org.kodein.db.leveldb.LevelDB
import org.kodein.memory.*

internal interface BaseDataRead : BaseDataBase, DataRead {

    val ldb: LevelDB
    val snapshot: LevelDB.Snapshot?

    private fun toLdb(options: Array<out Options.Read>): LevelDB.ReadOptions {
        val ro: DataOptions.Read = options() ?: return LevelDB.ReadOptions.DEFAULT
        return LevelDB.ReadOptions(
                verifyChecksums = ro.verifyChecksums,
                fillCache = ro.fillCache,
                snapshot = snapshot
        )
    }

    override fun get(key: ReadBuffer, vararg options: Options.Read): Allocation? = ldb.get(key, toLdb(options))

    override fun findAll(vararg options: Options.Read): DataCursor = DataSimpleCursor(ldb.newCursor(toLdb(options)), objectEmptyPrefix.asManagedAllocation())

    override fun findByType(type: String, vararg options: Options.Read): DataCursor {
        val key = Allocation.native(getObjectKeySize(type, null)) { putObjectKey(type, null) }
        return DataSimpleCursor(ldb.newCursor(toLdb(options)), key)
    }

    override fun findByPrimaryKey(type: String, primaryKey: Value, isOpen: Boolean, vararg options: Options.Read): DataCursor {
        val key = Allocation.native(getObjectKeySize(type, primaryKey, isOpen)) { putObjectKey(type, primaryKey, isOpen) }
        return DataSimpleCursor(ldb.newCursor(toLdb(options)), key)
    }

    override fun findAllByIndex(type: String, name: String, vararg options: Options.Read): DataCursor {
        val key = Allocation.native(getIndexKeyStartSize(type, name, null)) { putIndexKeyStart(type, name, null) }
        val ro = toLdb(options)
        return DataIndexCursor(ldb, ldb.newCursor(ro), key, ro)
    }

    override fun findByIndex(type: String, name: String, value: Value, isOpen: Boolean, vararg options: Options.Read): DataCursor {
        val key = Allocation.native(getIndexKeyStartSize(type, name, value, isOpen)) { putIndexKeyStart(type, name, value, isOpen) }
        val ro = toLdb(options)
        return DataIndexCursor(ldb, ldb.newCursor(ro), key, ro)
    }

    override fun getIndexesOf(key: ReadBuffer, vararg options: Options.Read): List<String> {
        val indexes = SliceBuilder.native(DataDBImpl.DEFAULT_CAPACITY).use {
            val refKey = it.newSlice { putRefKeyFromObjectKey(key) }
            ldb.get(refKey, toLdb(options)) ?: return emptyList()
        }

        val list = ArrayList<String>()

        indexes.use {
            while (indexes.hasRemaining()) {
                val length = indexes.readInt()
                val indexKey = indexes.slice(indexes.position, length)
                indexes.skip(length)

                val type = getIndexKeyName(indexKey)
                list.add(type.readAscii())
            }
        }

        return list
    }

}