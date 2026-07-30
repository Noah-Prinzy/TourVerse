package com.tourverse.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.util.UUID

// Provides shared uuid serializer behavior without requiring an instance.
object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    // Performs the serialize operation for this part of the application.
    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    // Performs the deserialize operation for this part of the application.
    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}

// Provides shared instant serializer behavior without requiring an instance.
object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    // Performs the serialize operation for this part of the application.
    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }

    // Performs the deserialize operation for this part of the application.
    override fun deserialize(decoder: Decoder): Instant {
        return Instant.parse(decoder.decodeString())
    }
}

// Provides shared local date serializer behavior without requiring an instance.
object LocalDateSerializer : KSerializer<java.time.LocalDate> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    // Performs the serialize operation for this part of the application.
    override fun serialize(encoder: Encoder, value: java.time.LocalDate) {
        encoder.encodeString(value.toString())
    }

    // Performs the deserialize operation for this part of the application.
    override fun deserialize(decoder: Decoder): java.time.LocalDate {
        return java.time.LocalDate.parse(decoder.decodeString())
    }
}
