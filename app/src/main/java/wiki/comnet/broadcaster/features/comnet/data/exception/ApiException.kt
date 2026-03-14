package wiki.comnet.broadcaster.features.comnet.data.exception

/**
 * Exception for API HTTP errors with parsed response body.
 * @param statusCode HTTP status code (e.g. 422)
 * @param message Error message from response body
 * @param errors For 422: map of field name to list of validation error messages; null otherwise
 */
class ApiException(
    val statusCode: Int,
    override val message: String,
    val errors: Map<String, List<String>>? = null,
) : Exception(message)
