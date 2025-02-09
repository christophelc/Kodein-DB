package org.kodein.db.leveldb.test

import org.kodein.db.leveldb.LevelDBFactory
import org.kodein.db.leveldb.based
import org.kodein.db.leveldb.native.LevelDBNative
import org.kodein.log.Logger
import org.kodein.log.print.printLogFilter

actual fun platformOptions() = baseOptions().copy(loggerFactory = { Logger(it, printLogFilter) })

actual val platformFactory: LevelDBFactory = LevelDBNative.based("/tmp/")
