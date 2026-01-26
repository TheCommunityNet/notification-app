package wiki.comnet.broadcaster.features.ble.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BlePacket(
    val version: UByte = 1u,
    val type: UByte,
    val id: ByteArray,
    val payload: ByteArray,
) : Parcelable {
    constructor(
        type: UByte,
        id: String,
        payload: ByteArray,
    ) : this(
        version = 1u,
        type = type,
        id = id.toByteArray(),
        payload = payload,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BlePacket

        if (version != other.version) return false
        if (type != other.type) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = version.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}