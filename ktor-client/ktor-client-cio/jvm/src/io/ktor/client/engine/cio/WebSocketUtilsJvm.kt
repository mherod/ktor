/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.engine.cio

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.util.date.*
import io.ktor.utils.io.*
import kotlin.coroutines.*

internal actual fun startWebSocketSession(
    status: HttpStatusCode,
    requestTime: GMTDate,
    headers: Headers,
    version: HttpProtocolVersion,
    callContext: CoroutineContext,
    input: ByteReadChannel,
    output: ByteWriteChannel
): HttpResponseData {
    val session = RawWebSocket(input, output, masking = true, coroutineContext = callContext)
    return HttpResponseData(status, requestTime, headers, version, session, callContext)
}
