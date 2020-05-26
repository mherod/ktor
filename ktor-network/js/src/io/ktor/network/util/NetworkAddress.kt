/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.network.util

public actual class NetworkAddress public actual constructor(
    internal val hostname: String,
    internal val port: Int
)

public actual val NetworkAddress.hostname: String
    get() = hostname

public actual val NetworkAddress.port: Int
    get() = port

public actual class UnresolvedAddressException : IllegalArgumentException()
