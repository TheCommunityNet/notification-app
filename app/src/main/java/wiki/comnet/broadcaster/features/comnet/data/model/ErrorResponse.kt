package wiki.comnet.broadcaster.features.comnet.data.model

/**
 * API error response body.
 * - [message] is always present when the API returns an error body.
 * - [errors] is present for 422 Unprocessable Entity: map of field name to list of error messages.
 */
data class ErrorResponse(
    val message: String? = null,
    val errors: Map<String, List<String>>? = null,
)
