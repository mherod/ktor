/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client

import io.ktor.client.engine.*
import io.ktor.client.features.*
import io.ktor.util.*
import kotlin.collections.set

/**
 * Mutable configuration used by [HttpClient].
 */
@HttpClientDsl
public class HttpClientConfig<T : HttpClientEngineConfig> {
    private val features: MutableMap<AttributeKey<*>, (HttpClient) -> Unit> = mutableMapOf()
    private val featureConfigurations: MutableMap<AttributeKey<*>, Any.() -> Unit> = mutableMapOf()

    private val customInterceptors: MutableMap<String, (HttpClient) -> Unit> = mutableMapOf()

    internal var engineConfig: T.() -> Unit = {}

    /**
     * Configure engine parameters.
     */
    public fun engine(block: T.() -> Unit) {
        val oldConfig = engineConfig
        engineConfig = {
            oldConfig()
            block()
        }
    }

    /**
     * Use [HttpRedirect] feature to automatically follow redirects.
     */
    public var followRedirects: Boolean = true

    /**
     * Use [defaultTransformers] to automatically handle simple [ContentType].
     */
    public var useDefaultTransformers: Boolean = true

    /**
     * Terminate [HttpClient.responsePipeline] if status code is not success(>=300).
     */
    public var expectSuccess: Boolean = true

    /**
     * Installs a specific [feature] and optionally [configure] it.
     */
    public fun <TBuilder : Any, TFeature : Any> install(
        feature: HttpClientFeature<TBuilder, TFeature>,
        configure: TBuilder.() -> Unit = {}
    ) {
        val previousConfigBlock = featureConfigurations[feature.key]
        featureConfigurations[feature.key] = {
            previousConfigBlock?.invoke(this)

            @Suppress("UNCHECKED_CAST")
            (this as TBuilder).configure()
        }

        if (features.containsKey(feature.key)) return

        features[feature.key] = { scope ->
            val attributes = scope.attributes.computeIfAbsent(FEATURE_INSTALLED_LIST) { Attributes(concurrent = true) }
            val config = scope.config.featureConfigurations[feature.key]!!
            val featureData = feature.prepare(config)

            feature.install(featureData, scope)
            attributes.put(feature.key, featureData)
        }
    }

    /**
     * Installs an interceptor defined by [block].
     * The [key] parameter is used as a unique name, that also prevents installing duplicated interceptors.
     */
    public fun install(key: String, block: HttpClient.() -> Unit) {
        customInterceptors[key] = block
    }

    /**
     * Applies all the installed [features] and [customInterceptors] from this configuration
     * into the specified [client].
     */
    public fun install(client: HttpClient) {
        features.values.forEach { client.apply(it) }
        customInterceptors.values.forEach { client.apply(it) }
    }

    /**
     * Clones this [HttpClientConfig] duplicating all the [features] and [customInterceptors].
     */
    public fun clone(): HttpClientConfig<T> {
        val result = HttpClientConfig<T>()
        result += this
        return result
    }

    /**
     * Install features from [other] client config.
     */
    public operator fun plusAssign(other: HttpClientConfig<out T>) {
        followRedirects = other.followRedirects
        useDefaultTransformers = other.useDefaultTransformers
        expectSuccess = other.expectSuccess

        features += other.features
        featureConfigurations += other.featureConfigurations
        customInterceptors += other.customInterceptors
    }
}

/**
 * Dsl marker for [HttpClient] dsl.
 */
@DslMarker
public annotation class HttpClientDsl
