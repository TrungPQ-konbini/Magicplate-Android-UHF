package com.konbini.magicplateuhf.data.remote.wallet.response

data class WalletTokenResponse(
    var access_token: String?,
    var expires_in: Int?,
    var token_type: String?,
    var scope: String?
)
