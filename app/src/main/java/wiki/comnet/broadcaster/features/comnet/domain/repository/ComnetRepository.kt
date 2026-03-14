package wiki.comnet.broadcaster.features.comnet.domain.repository

import kotlinx.coroutines.flow.Flow
import wiki.comnet.broadcaster.core.common.Result
import wiki.comnet.broadcaster.features.comnet.domain.model.Voucher

interface ComnetRepository {
    suspend fun activeVoucher(): Flow<Result<Voucher?>>

    suspend fun redeemVoucher(code: String): Flow<Result<Voucher>>

    suspend fun referral(username: String): Flow<Result<Boolean>>
}