/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.network.tls

import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlin.coroutines.*

internal actual suspend fun openTLSSession(
    socket: Socket,
    input: ByteReadChannel,
    output: ByteWriteChannel,
    config: TLSConfig,
    context: CoroutineContext
): Socket {
    TODO("Not yet implemented")
}
