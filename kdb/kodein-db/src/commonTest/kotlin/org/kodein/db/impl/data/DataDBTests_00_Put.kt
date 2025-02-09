package org.kodein.db.impl.data

import org.kodein.db.Value
import org.kodein.db.indexSet
import org.kodein.db.test.utils.byteArray
import kotlin.test.Test

@Suppress("ClassName")
class DataDBTests_00_Put : DataDBTests() {

    @Test
    fun test00_PutSimpleKeyWithoutIndex() {
        ddb.put("Test", Value.ofAscii("aaa"), Value.ofAscii("ValueA1!"))

        assertDBIs(
                byteArray('o', 0, "Test", 0, "aaa", 0) to byteArray("ValueA1!")
        )
    }

    @Test
    fun test01_PutSimpleKeyWith1Index() {
        ddb.put("Test", Value.ofAscii("aaa"), Value.ofAscii("ValueA1!"), indexes = indexSet("Symbols" to Value.ofAscii("alpha", "beta")))

        assertDBIs(
                byteArray('i', 0, "Test", 0, "Symbols", 0, "alpha", 0, "beta", 0, "aaa", 0) to byteArray('o', 0, "Test", 0, "aaa", 0),
                byteArray('o', 0, "Test", 0, "aaa", 0) to byteArray("ValueA1!"),
                byteArray('r', 0, "Test", 0, "aaa", 0) to byteArray(0, 0, 0, 30, 'i', 0, "Test", 0, "Symbols", 0, "alpha", 0, "beta", 0, "aaa", 0)
        )
    }

    @Test
    fun test02_PutSimpleKeyWith2Index() {
        ddb.put("Test", Value.ofAscii("aaa"), Value.ofAscii("ValueA1!"), indexes = indexSet("Symbols" to Value.ofAscii("alpha", "beta"), "Numbers" to Value.ofAscii("forty", "two")))

        assertDBIs(
                byteArray('i', 0, "Test", 0, "Numbers", 0, "forty", 0, "two", 0, "aaa", 0) to byteArray('o', 0, "Test", 0, "aaa", 0),
                byteArray('i', 0, "Test", 0, "Symbols", 0, "alpha", 0, "beta", 0, "aaa", 0) to byteArray('o', 0, "Test", 0, "aaa", 0),
                byteArray('o', 0, "Test", 0, "aaa", 0) to byteArray("ValueA1!"),
                byteArray('r', 0, "Test", 0, "aaa", 0) to byteArray(0, 0, 0, 30, 'i', 0, "Test", 0, "Symbols", 0, "alpha", 0, "beta", 0, "aaa", 0, 0, 0, 0, 29, 'i', 0, "Test", 0, "Numbers", 0, "forty", 0, "two", 0, "aaa", 0)
        )
    }

    @Test
    fun test03_PutTwiceWithRemovedIndex() {
        ddb.put("Test", Value.ofAscii("aaa", "bbb"), Value.ofAscii("ValueAB1!"), indexes = indexSet("Symbols" to Value.ofAscii("alpha", "beta")))
        ddb.put("Test", Value.ofAscii("aaa", "bbb"), Value.ofAscii("ValueAB2!"))

        assertDBIs(
                byteArray('o', 0, "Test", 0, "aaa", 0, "bbb", 0) to byteArray("ValueAB2!")
        )
    }

    @Test
    fun test04_PutTwiceWithDifferentIndex() {
        ddb.put("Test", Value.ofAscii("aaa", "bbb"), Value.ofAscii("ValueAB1!"), indexes = indexSet("Symbols" to Value.ofAscii("alpha", "beta")))
        ddb.put("Test", Value.ofAscii("aaa", "bbb"), Value.ofAscii("ValueAB2!"), indexes = indexSet("Numbers" to Value.ofAscii("forty", "two")))

        assertDBIs(
                byteArray('i', 0, "Test", 0, "Numbers", 0, "forty", 0, "two", 0, "aaa", 0, "bbb", 0) to byteArray('o', 0, "Test", 0, "aaa", 0, "bbb", 0),
                byteArray('o', 0, "Test", 0, "aaa", 0, "bbb", 0) to byteArray("ValueAB2!"),
                byteArray('r', 0, "Test", 0, "aaa", 0, "bbb", 0) to byteArray(0, 0, 0, 33, 'i', 0, "Test", 0, "Numbers", 0, "forty", 0, "two", 0, "aaa", 0, "bbb", 0)
        )
    }

}
