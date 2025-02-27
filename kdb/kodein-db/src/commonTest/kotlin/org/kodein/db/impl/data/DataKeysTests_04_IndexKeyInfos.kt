package org.kodein.db.impl.data

import org.kodein.db.Value
import org.kodein.db.test.utils.assertBytesEquals
import org.kodein.db.test.utils.byteArray
import org.kodein.memory.Allocation
import org.kodein.memory.native
import org.kodein.memory.use
import kotlin.test.Test

@Suppress("ClassName")
class DataKeysTests_04_IndexKeyInfos {

    @Test
    fun test00_IndexName() {
        Allocation.native(32).use { objectKey ->
            objectKey.putObjectKey("Test", Value.ofAscii("one"))
            objectKey.flip()
            Allocation.native(32).use { indexKey ->
                indexKey.putIndexKey(objectKey, "Symbols", Value.ofAscii("alpha", "beta"))
                indexKey.flip()
                assertBytesEquals(byteArray("Symbols"), getIndexKeyName(indexKey))
            }
        }
    }
}
