/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.network.util

import io.ktor.utils.io.*

actual class NetworkAddress constructor(
    val hostname: String,
    val port: Int,
    address: SocketAddress? = null
) {
    actual constructor(
        hostname: String, port: Int
    ) : this(hostname, port, null)

    val address: SocketAddress

    init {
        this.address = address ?: resolve().first()
        makeShared()
    }

    /**
     * Resolve current socket address.
     */
    fun resolve(): List<SocketAddress> = getAddressInfo(hostname, port)
}

actual val NetworkAddress.hostname: String get() = hostname

actual val NetworkAddress.port: Int get() = port

actual class UnresolvedAddressException : IllegalArgumentException()
