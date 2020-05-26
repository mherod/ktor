/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.engine.cio

import io.ktor.client.engine.*

private val initHook = CIO

internal actual fun addToLoader() {
    engines.append(CIO)
}
