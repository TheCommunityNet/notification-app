package wiki.comnet.broadcaster.features.comnet.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import wiki.comnet.broadcaster.features.comnet.data.model.BaseResponse
import wiki.comnet.broadcaster.features.comnet.data.model.RedeemVoucherRequest
import wiki.comnet.broadcaster.features.comnet.data.model.ReferralRequest
import wiki.comnet.broadcaster.features.comnet.data.model.VoucherDto

interface ComnetApi {
    @GET("/api/v1/voucher/active")
    suspend fun activeVoucher(): BaseResponse<VoucherDto?>

    @POST("/api/v1/voucher/redeem")
    suspend fun redeemVoucher(@Body body: RedeemVoucherRequest): BaseResponse<VoucherDto>

    @POST("/api/v1/referral")
    suspend fun referral(@Body body: ReferralRequest): BaseResponse<Unit>

    companion object {
        const val BASE_URL = "https://web.comnet.wiki"
    }
}
