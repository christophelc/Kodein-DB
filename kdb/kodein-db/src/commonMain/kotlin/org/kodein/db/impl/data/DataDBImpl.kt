package org.kodein.db.impl.data

import org.kodein.db.*
import org.kodein.db.data.DataDB
import org.kodein.db.data.DataOptions
import org.kodein.db.data.DataWrite
import org.kodein.db.impl.utils.putBody
import org.kodein.db.leveldb.LevelDB
import org.kodein.memory.*
import org.kodein.memory.concurent.Lock
import org.kodein.memory.concurent.withLock
import org.kodein.memory.model.Sized

internal class DataDBImpl(override val ldb: LevelDB) : BaseDataRead, DataDB {

    override val snapshot: LevelDB.Snapshot? get() = null

    internal val indexesLock = Lock()

    companion object {
        internal const val DEFAULT_CAPACITY = 16384

        internal fun toLdb(options: Array<out Options.Write>): LevelDB.WriteOptions {
            val wo: DataOptions.Write = options() ?: return LevelDB.WriteOptions.DEFAULT
            return LevelDB.WriteOptions(
                    sync = wo.sync
            )
        }
    }

    internal fun deleteIndexesInBatch(batch: LevelDB.WriteBatch, refKey: ReadBuffer) {
        val indexes = ldb.get(refKey) ?: return

        indexes.use {
            while (indexes.hasRemaining()) {
                val len = indexes.readInt()
                val indexKey = indexes.slice(indexes.position, len)
                batch.delete(indexKey)
                indexes.skip(len)
            }
        }

        batch.delete(refKey)
    }

    internal fun putIndexesInBatch(sb: SliceBuilder, batch: LevelDB.WriteBatch, key: ReadBuffer, refKey: ReadBuffer, indexes: Set<Index>) {
        if (indexes.isEmpty())
            return

        val ref = sb.newSlice {
            for (index in indexes) {
                val indexKeySize = getIndexKeySize(key, index.name, index.value)
                putInt(indexKeySize)
                val indexKey = subSlice { putIndexKey(key, index.name, index.value) }
                batch.put(indexKey, key)
            }
        }

        batch.put(refKey, ref)
    }

    private fun putInBatch(sb: SliceBuilder, batch: LevelDB.WriteBatch, key: ReadBuffer, body: Body, indexes: Set<Index>): Int {
        val refKey = sb.newSlice {
            putRefKeyFromObjectKey(key)
        }

        deleteIndexesInBatch(batch, refKey)
        putIndexesInBatch(sb, batch, key, refKey, indexes)

        val value = sb.newSlice { putBody(body) }
        batch.put(key, value)

        return value.remaining
    }

    override fun put(type: String, primaryKey: Value, body: Body, indexes: Set<Index>, vararg options: Options.Write): Int {
        SliceBuilder.native(DEFAULT_CAPACITY).use {
            val key = it.newSlice { putObjectKey(type, primaryKey) }
            ldb.newWriteBatch().use { batch ->
                indexesLock.withLock {
                    val length = putInBatch(it, batch, key, body, indexes)
                    ldb.write(batch, toLdb(options))
                    return length
                }
            }
        }
    }

    private fun putAndSetKey(type: String, primaryKey: Value, body: Body, indexes: Set<Index>, key: KBuffer, options: Array<out Options.Write>): Int {
        key.putObjectKey(type, primaryKey)
        key.flip()

        ldb.newWriteBatch().use { batch ->
            SliceBuilder.native(DEFAULT_CAPACITY).use {
                indexesLock.withLock {
                    val length = putInBatch(it, batch, key, body, indexes)
                    ldb.write(batch, toLdb(options))
                    return length
                }
            }
        }
    }

    override fun putAndGetHeapKey(type: String, primaryKey: Value, body: Body, indexes: Set<Index>, vararg options: Options.Write): Sized<KBuffer> {
        val key = KBuffer.array(getObjectKeySize(type, primaryKey))
        val length = putAndSetKey(type, primaryKey, body, indexes, key, options)
        return Sized(key, length)
    }

    override fun putAndGetNativeKey(type: String, primaryKey: Value, body: Body, indexes: Set<Index>, vararg options: Options.Write): Sized<Allocation> {
        val key = Allocation.native(getObjectKeySize(type, primaryKey))
        val length = putAndSetKey(type, primaryKey, body, indexes, key, options)
        return Sized(key, length)
    }

    private fun deleteInBatch(sb: SliceBuilder, batch: LevelDB.WriteBatch, key: ReadBuffer) {
        val refKey = sb.newSlice { putRefKeyFromObjectKey(key) }

        deleteIndexesInBatch(batch, refKey)
        batch.delete(key)
    }

    override fun delete(key: ReadBuffer, vararg options: Options.Write) {
        ldb.newWriteBatch().use { batch ->
            SliceBuilder.native(DEFAULT_CAPACITY).use {
                indexesLock.withLock {
                    deleteInBatch(it, batch, key)
                    ldb.write(batch, toLdb(options))
                }
            }
        }
    }

    override fun newBatch(): DataDB.Batch = DataBatchImpl(this)

    override fun newSnapshot(): DataDB.Snapshot = DataSnapshotImpl(ldb, ldb.newSnapshot())

    override fun close() {
        ldb.close()
    }
}
