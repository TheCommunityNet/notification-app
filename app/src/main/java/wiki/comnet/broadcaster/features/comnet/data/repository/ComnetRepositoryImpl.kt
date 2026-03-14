package wiki.comnet.broadcaster.features.comnet.data.repository

import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import wiki.comnet.broadcaster.core.common.Result
import wiki.comnet.broadcaster.core.domain.repository.DeviceIdRepository
import wiki.comnet.broadcaster.features.comnet.data.exception.ApiException
import wiki.comnet.broadcaster.features.comnet.data.model.ErrorResponse
import wiki.comnet.broadcaster.features.comnet.data.model.RedeemVoucherRequest
import wiki.comnet.broadcaster.features.comnet.data.model.ReferralRequest
import wiki.comnet.broadcaster.features.comnet.data.network.ComnetApi
import wiki.comnet.broadcaster.features.comnet.domain.model.Voucher
import wiki.comnet.broadcaster.features.comnet.domain.repository.ComnetRepository
import javax.inject.Inject

class ComnetRepositoryImpl @Inject constructor(
    private val comnetApi: ComnetApi,
    private val deviceIdRepository: DeviceIdRepository,
) : ComnetRepository {
    override suspend fun activeVoucher(): Flow<Result<Voucher?>> {
        return flow {
            emit(Result.Loading)

            try {
                val voucher = comnetApi.activeVoucher()

                voucher.data?.let {
                    emit(
                        Result.Success(
                            Voucher(
                                id = it.id,
                                name = it.name,
                                expiredIn = it.expiredIn,
                                expiredAt = it.expiredAt
                            )
                        )
                    )
                } ?: emit(Result.Success(null))
            } catch (e: HttpException) {
                emit(Result.Error(parseHttpException(e)))
            } catch (e: Exception) {
                emit(Result.Error(e))
            }
        }
    }

    override suspend fun redeemVoucher(code: String): Flow<Result<Voucher>> {
        return flow {
            emit(Result.Loading)

            try {
                val voucher = comnetApi.redeemVoucher(RedeemVoucherRequest(code))

                emit(
                    Result.Success(
                        Voucher(
                            id = voucher.data.id,
                            name = voucher.data.name,
                            expiredIn = voucher.data.expiredIn,
                            expiredAt = voucher.data.expiredAt
                        )
                    )
                )
            } catch (e: HttpException) {
                emit(Result.Error(parseHttpException(e)))
            } catch (e: Exception) {
                emit(Result.Error(e))
            }
        }
    }

    override suspend fun referral(username: String): Flow<Result<Boolean>> {
        return flow {
            try {
                emit(Result.Loading)

                val data = comnetApi.referral(
                    ReferralRequest(
                        username = username,
                        deviceId = deviceIdRepository.getDeviceId()
                    )
                )

                emit(Result.Success(data.success))
            } catch (e: HttpException) {
                emit(Result.Error(parseHttpException(e)))
            } catch (e: Exception) {
                emit(Result.Error(e))
            }
        }
    }

    private fun parseHttpException(e: HttpException): ApiException {
        val code = e.code()
        val body = e.response()?.errorBody()?.string()
        val errorResponse = body?.let { raw ->
            try {
                Gson().fromJson(raw, ErrorResponse::class.java)
            } catch (_: Exception) {
                null
            }
        }
        val message = errorResponse?.message ?: e.message() ?: "Request failed"
        val errors = if (code == 422) errorResponse?.errors else null
        return ApiException(statusCode = code, message = message, errors = errors)
    }
}