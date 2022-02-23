package com.konbini.magicplateuhf.data.remote.transaction.response

data class AccessTokenResponse(
    var access_token: String?,
    var expires_in: Int?,
    var token_type: String?,
    var scope: String?
)
