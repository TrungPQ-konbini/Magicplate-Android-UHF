package com.konbini.magicplateuhf.ui.registerTags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.konbini.magicplateuhf.AppContainer
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.MainApplication
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.data.entities.PlateModelEntity
import com.konbini.magicplateuhf.data.remote.plateModel.request.Data
import com.konbini.magicplateuhf.data.remote.plateModel.request.SetPlateModelRequest
import com.konbini.magicplateuhf.data.remote.plateModel.response.PlateModel
import com.konbini.magicplateuhf.data.repository.PlateModelRepository
import com.konbini.magicplateuhf.utils.LogUtils
import com.konbini.magicplateuhf.utils.Resource
import com.konbini.magicplateuhf.utils.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class RegisterTagsViewModel @Inject constructor(
    private val plateModelRepository: PlateModelRepository
) : ViewModel() {
    companion object {
        const val TAG = "RegisterTagsViewModel"
    }

    private val gson = Gson()
    private val resources = MainApplication.shared()

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private var listPlateModels: MutableList<PlateModelEntity> = mutableListOf()

    fun syncPlateModels() {
        viewModelScope.launch {
            val url = AppSettings.Cloud.Host

            _state.value = State(
                status = Resource.Status.LOADING
            )

            val syncPlateModels =
                withContext(Dispatchers.Default) {
                    plateModelRepository.syncPlateModels(url)
                }

            if (syncPlateModels.status == Resource.Status.SUCCESS) {
                syncPlateModels.data?.let { response ->
                    // Delete old Plates Model
                    plateModelRepository.deleteAll()

                    // Save new Plates Model
                    listPlateModels.clear()
                    response.results.forEach { _plateModelEntity ->
                        val plateModelEntity = formatPlateModel(_plateModelEntity)
                        listPlateModels.add(plateModelEntity)
                        plateModelRepository.insert(plateModelEntity)
                    }

                    _state.value = State(
                        status = Resource.Status.SUCCESS,
                        message = resources.getString(R.string.message_success_sync),
                        data = listPlateModels
                    )
                }
            } else {
                _state.value = State(
                    status = Resource.Status.ERROR,
                    message = String.format(
                        resources.getString(R.string.message_error_sync_item),
                        "Plate Models"
                    )
                )
            }
        }
    }

    fun setPlateModelData(data: ArrayList<Data>) {
        viewModelScope.launch {
            val url = AppSettings.Cloud.Host

            val bodyRequest = SetPlateModelRequest(
                accessToken = AppContainer.GlobalVariable.currentToken,
                data = data
            )

            val setPlateModelData =
                withContext(Dispatchers.Default) {
                    plateModelRepository.setPlateModels(url, bodyRequest)
                }

            if (setPlateModelData.status == Resource.Status.SUCCESS) {
                setPlateModelData.data?.let { response ->

                    LogUtils.logInfo("Set Plate Model Data Success!!!")
                    LogUtils.logInfo("[Set Plate Model Data] ${gson.toJson(response.data)}")
//                    _state.value = State(
//                        status = Resource.Status.SUCCESS,
//                        message = resources.getString(R.string.message_success_register),
//                        data = response.data,
//                        isFinish = true
//                    )
                }
            } else {
//                _state.value = State(
//                    status = Resource.Status.ERROR,
//                    message = resources.getString(R.string.message_error_register)
//                )
            }
        }
    }

    private fun formatPlateModel(_plateModel: PlateModel): PlateModelEntity {
        return PlateModelEntity(
            id = _plateModel.plateModelId.toInt(),
            plateModelId = _plateModel.plateModelId,
            plateModelCode = _plateModel.plateModelCode,
            plateModelTitle = _plateModel.plateModelTitle,
            lastPlateSerial = _plateModel.lastPlateSerial ?: "000001"
        )
    }
}