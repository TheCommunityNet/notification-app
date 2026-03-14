package wiki.comnet.broadcaster.features.comnet.domain.model


data class Voucher(
    val id: String,
    val name: String,
    val expiredIn: Int,
    val expiredAt: String,
)