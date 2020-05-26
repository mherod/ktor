package io.ktor.client.engine

import io.ktor.http.*
import io.ktor.network.util.*
import java.net.*

/**
 * Proxy configuration.
 *
 * See [ProxyBuilder] to create proxy.
 */
actual typealias ProxyConfig = Proxy

/**
 * [ProxyConfig] factory.
 */
actual object ProxyBuilder {
    /**
     * Create http proxy from [url].
     */
    actual fun http(url: Url): ProxyConfig = Proxy(Proxy.Type.HTTP, InetSocketAddress(url.host, url.port))

    /**
     * Create socks proxy from [host] and [port].
     */
    actual fun socks(host: String, port: Int): ProxyConfig = Proxy(Proxy.Type.SOCKS, InetSocketAddress(host, port))
}

/**
 * Resolve remote address of [ProxyConfig].
 *
 * This operations can block.
 */
actual fun ProxyConfig.resolveAddress(): NetworkAddress {
    return address() as NetworkAddress
}

/**
 * Type of configured proxy.
 */
actual val ProxyConfig.type: ProxyType
    get() = when (type()) {
        Proxy.Type.DIRECT -> ProxyType.SOCKS
        Proxy.Type.HTTP -> ProxyType.HTTP
        else -> ProxyType.UNKNOWN
    }
