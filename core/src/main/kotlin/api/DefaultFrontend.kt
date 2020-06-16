@file: Suppress("TooManyFunctions", "MatchingDeclarationName")
@file: JvmName("DefaultFrontend")
package edu.illinois.cs.cs125.answerable.api

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import edu.illinois.cs.cs125.answerable.classdesignanalysis.OssifiedField
import java.lang.UnsupportedOperationException

val serializer = Moshi.Builder()
    .add(OssifiedFieldAdapter())
    .add(KotlinJsonAdapterFactory())
    .build()

fun serialize(obj: Any) = (serializer.adapter(obj::class.java) as JsonAdapter<Any>).toJson(obj)

/*
 * ADAPTERS:
 *
 * Moshi needs to be able to do all sorts of implicit reflection shenanigans with fields and parameters that
 * isn't possible for some of our objects. For these, we must provide custom adapters.
 *
 * Unfortunately, in many cases, the default _serialization_ would be fine, and Moshi's issue is with
 * deserialization. As far as I can tell, there's no way to use Moshi's serializer but change the deserializer :(
 */

class OssifiedFieldAdapter {
    @FromJson
    fun fromJson(unused: String): OssifiedField =
        throw UnsupportedOperationException("Can't deserialize OssifiedFields")

    class SerializableOssifiedField(
        val modifiers: List<String>,
        val type: String,
        val name: String,
        val answerableName: String
    ) {
        internal constructor(ofield: OssifiedField) : this (
            ofield.modifiers,
            ofield.type,
            ofield.name,
            ofield.answerableName
        )
    }
    @ToJson
    fun toJson(ofield: OssifiedField): SerializableOssifiedField =
        SerializableOssifiedField(ofield)
}