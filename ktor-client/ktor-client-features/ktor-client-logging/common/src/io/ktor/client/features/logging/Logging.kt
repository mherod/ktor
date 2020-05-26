/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.features.logging

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.features.observer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.*

/**
 * [HttpClient] logging feature.
 */
class Logging(
    val logger: Logger,
    val level: LogLevel,
    var filters: List<(HttpRequestBuilder) -> Boolean> = emptyList()
) {
    private val mutex = Mutex()

    /**
     * [Logging] feature configuration
     */
    class Config {
        /**
         * filters
         */
        internal var filters = mutableListOf<(HttpRequestBuilder) -> Boolean>()

        /**
         * [Logger] instance to use
         */
        var logger: Logger = Logger.DEFAULT

        /**
         * log [LogLevel]
         */
        var level: LogLevel = LogLevel.HEADERS

        /**
         * Log messages for calls matching a [predicate]
         */
        fun filter(predicate: (HttpRequestBuilder) -> Boolean) {
            filters.add(predicate)
        }
    }

    private suspend fun beginLogging() {
        mutex.lock()
    }

    private fun doneLogging() {
        mutex.unlock()
    }

    private suspend fun logRequest(request: HttpRequestBuilder): OutgoingContent? {
        if (level.info) {
            logger.log("REQUEST: ${Url(request.url)}")
            logger.log("METHOD: ${request.method}")
        }

        val content = request.body as OutgoingContent

        if (level.headers) {
            logger.log("COMMON HEADERS")
            logHeaders(request.headers.entries())

            logger.log("CONTENT HEADERS")
            logHeaders(content.headers.entries())
        }

        return if (level.body) {
            logRequestBody(content)
        } else null
    }

    private fun logResponse(response: HttpResponse) {
        if (level.info) {
            logger.log("RESPONSE: ${response.status}")
            logger.log("METHOD: ${response.call.request.method}")
            logger.log("FROM: ${response.call.request.url}")
        }

        if (level.headers) {
            logger.log("COMMON HEADERS")
            logHeaders(response.headers.entries())
        }
    }

    private suspend fun logResponseBody(contentType: ContentType?, content: ByteReadChannel): Unit = with(logger) {
        log("BODY Content-Type: $contentType")
        log("BODY START")
        val message = content.readText(contentType?.charset() ?: Charsets.UTF_8)
        log(message)
        log("BODY END")
    }

    private fun logRequestException(context: HttpRequestBuilder, cause: Throwable) {
        if (level.info) {
            logger.log("REQUEST ${Url(context.url)} failed with exception: $cause")
        }

    }

    private fun logResponseException(context: HttpClientCall, cause: Throwable) {
        if (level.info) {
            logger.log("RESPONSE ${context.request.url} failed with exception: $cause")
        }
    }

    private fun logHeaders(headers: Set<Map.Entry<String, List<String>>>) {
        val sortedHeaders = headers.toList().sortedBy { it.key }

        sortedHeaders.forEach { (key, values) ->
            logger.log("-> $key: ${values.joinToString("; ")}")
        }
    }

    private suspend fun logRequestBody(content: OutgoingContent): OutgoingContent? {
        logger.log("BODY Content-Type: ${content.contentType}")

        val charset = content.contentType?.charset() ?: Charsets.UTF_8

        val channel = ByteChannel()
        GlobalScope.launch(Dispatchers.Unconfined) {
            val text = channel.readText(charset)
            logger.log("BODY START")
            logger.log(text)
            logger.log("BODY END")
        }

        return content.observe(channel)
    }

    companion object : HttpClientFeature<Config, Logging> {
        override val key: AttributeKey<Logging> = AttributeKey("ClientLogging")

        override fun prepare(block: Config.() -> Unit): Logging {
            val config = Config().apply(block)
            return Logging(config.logger, config.level, config.filters)
        }

        override fun install(feature: Logging, scope: HttpClient) {
            scope.sendPipeline.intercept(HttpSendPipeline.Monitoring) {
                val response = if (feature.filters.isEmpty() || feature.filters.any { it(context) }) {
                    try {
                        feature.beginLogging()
                        feature.logRequest(context)
                    } catch (_: Throwable) {
                        null
                    } finally {
                        feature.doneLogging()
                    }
                } else null

                try {
                    proceedWith(response ?: subject)
                } catch (cause: Throwable) {
                    feature.logRequestException(context, cause)
                    throw cause
                } finally {
                }
            }

            scope.receivePipeline.intercept(HttpReceivePipeline.State) {
                try {
                    feature.beginLogging()
                    feature.logResponse(context.response)
                    proceedWith(subject)
                } catch (cause: Throwable) {
                    feature.logResponseException(context, cause)
                    throw cause
                } finally {
                    if (!feature.level.body) {
                        feature.doneLogging()
                    }
                }
            }

            scope.responsePipeline.intercept(HttpResponsePipeline.Receive) {
                try {
                    proceed()
                } catch (cause: Throwable) {
                    feature.logResponseException(context, cause)
                    throw cause
                }
            }

            if (!feature.level.body) {
                return
            }

            val observer: ResponseHandler = {
                try {
                    feature.logResponseBody(it.contentType(), it.content)
                } catch (_: Throwable) {
                } finally {
                    feature.doneLogging()
                }
            }

            ResponseObserver.install(ResponseObserver(observer), scope)
        }
    }
}

/**
 * Configure and install [Logging] in [HttpClient].
 */
fun HttpClientConfig<*>.Logging(block: Logging.Config.() -> Unit = {}) {
    install(Logging, block)
}

private suspend inline fun ByteReadChannel.readText(charset: Charset): String =
    readRemaining().readText(charset = charset)
