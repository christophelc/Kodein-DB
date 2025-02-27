package org.kodein.db.impl.data

import org.kodein.db.Value
import org.kodein.db.data.DataBase
import org.kodein.memory.Allocation
import org.kodein.memory.KBuffer
import org.kodein.memory.array
import org.kodein.memory.native

interface BaseDataBase : DataBase {

    override fun getHeapKey(type: String, primaryKey: Value): KBuffer =
            KBuffer.array(getObjectKeySize(type, primaryKey)) { putObjectKey(type, primaryKey) }

    override fun getNativeKey(type: String, primaryKey: Value): Allocation =
            Allocation.native(getObjectKeySize(type, primaryKey)) { putObjectKey(type, primaryKey) }


}