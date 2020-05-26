/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.util

/**
 * Specifies a key for an attribute in [Attributes]
 * @param T is type of the value stored in the attribute
 * @param name is a name of the attribute for diagnostic purposes
 */
public class AttributeKey<T>(public val name: String) {
    override fun toString(): String = if (name.isEmpty())
        super.toString()
    else
        "AttributeKey: $name"
}

/**
 * Create attributes instance suitable for the particular platform
 */
public expect fun Attributes(concurrent: Boolean = false): Attributes

/**
 * Map of attributes accessible by [AttributeKey] in a typed manner
 */
public interface Attributes {
    /**
     * Gets a value of the attribute for the specified [key], or throws an exception if an attribute doesn't exist
     */
    public operator fun <T : Any> get(key: AttributeKey<T>): T =
        getOrNull(key) ?: throw IllegalStateException("No instance for key $key")

    /**
     * Gets a value of the attribute for the specified [key], or return `null` if an attribute doesn't exist
     */
    public fun <T : Any> getOrNull(key: AttributeKey<T>): T?

    /**
     * Checks if an attribute with the specified [key] exists
     */
    public operator fun contains(key: AttributeKey<*>): Boolean

    /**
     * Creates or changes an attribute with the specified [key] using [value]
     */
    public fun <T : Any> put(key: AttributeKey<T>, value: T)

    /**
     * Removes an attribute with the specified [key]
     */
    public fun <T : Any> remove(key: AttributeKey<T>)

    /**
     * Removes an attribute with the specified [key] and returns its current value, throws an exception if an attribute doesn't exist
     */
    @KtorExperimentalAPI
    public fun <T : Any> take(key: AttributeKey<T>): T = get(key).also { remove(key) }

    /**
     * Removes an attribute with the specified [key] and returns its current value, returns `null` if an attribute doesn't exist
     */
    @KtorExperimentalAPI
    public fun <T : Any> takeOrNull(key: AttributeKey<T>): T? = getOrNull(key).also { remove(key) }

    /**
     * Gets a value of the attribute for the specified [key], or calls supplied [block] to compute its value
     */
    public fun <T : Any> computeIfAbsent(key: AttributeKey<T>, block: () -> T): T

    /**
     * Returns [List] of all [AttributeKey] instances in this map
     */
    public val allKeys: List<AttributeKey<*>>
}
