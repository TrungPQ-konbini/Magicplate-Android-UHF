package com.konbini.magicplateuhf.jobs

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import com.google.gson.Gson
import com.konbini.magicplateuhf.AppContainer
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.data.remote.wallet.request.WalletTokenRequest
import com.konbini.magicplateuhf.data.repository.WalletRepository
import com.konbini.magicplateuhf.utils.LogUtils
import com.konbini.magicplateuhf.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.lang.Runnable
import javax.inject.Inject
import kotlin.coroutines.EmptyCoroutineContext

@AndroidEntryPoint
class GetTokenJobService : JobService() {

    @Inject lateinit var walletRepository: WalletRepository
    private val serviceScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)

    companion object {
        private const val TAG = "GetTokenJobService"
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d(TAG, "Job Get Token Started")
        LogUtils.logInfo("Job Get Token Started")
        doBackgroundWork(params)
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.d(TAG, "Job Get Token completion")
        return true
    }

    private fun doBackgroundWork(params: JobParameters?) {
        Thread(Runnable {
            kotlin.run {
                if (AppContainer.GlobalVariable.isGettingToken) {
                    return@Runnable
                }
                serviceScope.launch {
                    try {
                        LogUtils.logInfo("Call API: ${AppSettings.APIs.Oauth}")
                        Log.e(TAG, "Call API: ${AppSettings.APIs.Oauth}")
                        AppContainer.GlobalVariable.isGettingToken = true

                        // Get token wallet
                        val requestTokenWallet = WalletTokenRequest(
                            "client_credentials",
                            AppSettings.Cloud.ClientId,
                            AppSettings.Cloud.ClientSecret
                        )
                        val tokenWallet =
                            withContext(Dispatchers.Default) {
                                walletRepository.getAccessToken(
                                    AppSettings.Cloud.Host,
                                    requestTokenWallet
                                )
                            }

                        if (tokenWallet.status == Resource.Status.SUCCESS) {
                            tokenWallet.data?.let { _walletTokenResponse ->
                                if (_walletTokenResponse.access_token != null) {
                                    AppContainer.GlobalVariable.currentToken =
                                        _walletTokenResponse.access_token ?: ""
                                    AppContainer.GlobalVariable.isGettingToken = false
                                }
                                LogUtils.logInfo("[Token]: ${AppContainer.GlobalVariable.currentToken}")
                                Log.e(TAG, "[Token]: ${AppContainer.GlobalVariable.currentToken}")
                            }
                        } else {
                            AppContainer.GlobalVariable.isGettingToken = false
                            LogUtils.logInfo("[Token]: Error ${Gson().toJson(tokenWallet.data ?: "Can't get Token!!!")}")
                        }
                    } catch (ex: Exception) {
                        LogUtils.logError(ex)
                        AppContainer.GlobalVariable.isGettingToken = false
                    }
                }
                Thread.sleep(AppSettings.Timer.PeriodicGetToken.toLong() * 60 * 1000)
            }
            jobFinished(params, true)
        }).start()
    }
}