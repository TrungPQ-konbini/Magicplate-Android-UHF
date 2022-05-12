package com.konbini.magicplateuhf.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.konbini.magicplateuhf.AppContainer
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.MainApplication
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.data.entities.*
import com.konbini.magicplateuhf.data.remote.category.request.CategoriesRequest
import com.konbini.magicplateuhf.data.remote.category.response.Category
import com.konbini.magicplateuhf.data.remote.menu.response.MenuDetail
import com.konbini.magicplateuhf.data.remote.menu.response.Product
import com.konbini.magicplateuhf.data.remote.plateModel.response.PlateModel
import com.konbini.magicplateuhf.data.remote.product.request.ProductsRequest
import com.konbini.magicplateuhf.data.remote.product.response.Option
import com.konbini.magicplateuhf.data.remote.product.response.ProductResponse
import com.konbini.magicplateuhf.data.remote.timeBlock.response.TimeBlock
import com.konbini.magicplateuhf.data.remote.user.request.GetAllUserRequest
import com.konbini.magicplateuhf.data.repository.*
import com.konbini.magicplateuhf.utils.PrefUtil
import com.konbini.magicplateuhf.utils.Resource
import com.konbini.magicplateuhf.utils.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.lang.reflect.Type
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val productRepository: ProductRepository,
    private val plateModelRepository: PlateModelRepository,
    private val timeBlockRepository: TimeBlockRepository,
    private val menuRepository: MenuRepository,
    private val orderStatusRepository: OrderStatusRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    companion object {
        const val TAG = "SettingsViewModel"
    }

    private val gson = Gson()
    private val resources = MainApplication.shared()

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private var perPage = 100
    private var categoryPage = 1
    private var productPage = 1
    private val consumerKey = AppSettings.Cloud.ConsumerKey
    private val consumerSecret = AppSettings.Cloud.ConsumerSecret

    private var listProducts: MutableList<ProductEntity> = mutableListOf()
    private var listPlateModels: MutableList<PlateModelEntity> = mutableListOf()
    private var listTimeBlocks: MutableList<TimeBlockEntity> = mutableListOf()

    fun syncAll() {
        viewModelScope.launch {
            // Delete old Products
            listProducts.clear()
            productRepository.deleteAll()

            syncProducts(1)
        }
    }

    fun syncProducts(page: Int) {
        viewModelScope.launch {
            _state.value = State(Resource.Status.LOADING, "")
            val url = AppSettings.Cloud.Host

            val productsRequest = formatSyncProducts(page)
            val syncProducts = async {
                productRepository.syncProducts(url, productsRequest)
            }

            if (syncProducts.await().status == Resource.Status.SUCCESS) {
                syncProducts.await().data?.let { response ->
                    if (response.isNotEmpty()) {
                        response.forEach { _productResponse ->
                            val productEntity = formatProduct(_productResponse)
                            productEntity.syncId = _productResponse.id
                            listProducts.add(productEntity)
                        }
                        syncProducts(page + 1)
                    } else {
                        productRepository.insertAll(listProducts.toList())
                        syncAllWithoutProducts()
                    }
                }
            } else {
                val message = "${
                    String.format(
                        resources.getString(R.string.message_error_sync_item),
                        "Product"
                    )
                } \n ${syncProducts.await().message}"
                _state.value = State(
                    Resource.Status.ERROR,
                    message
                )
            }
        }
    }

    private fun formatSyncProducts(page: Int): ProductsRequest {
        return ProductsRequest(
            page,
            perPage,
            consumerKey,
            consumerSecret
        )
    }

    private fun formatProduct(productResponse: ProductResponse): ProductEntity {
        productResponse.barcode = productResponse.eanCode
        var options = ""
        val optionsValue = productResponse.metaData?.find { metaData ->
            metaData.key == "exwo_options" && metaData.value.toString().isNotEmpty()
        }
        if (optionsValue != null) {
            val strJson = gson.toJson(optionsValue.value)
            val collectionType: Type = object : TypeToken<Collection<Option?>?>() {}.type
            val collectionOptions: Collection<Option> = gson.fromJson(strJson, collectionType)
            options = gson.toJson(collectionOptions)
        }
        return ProductEntity(
            id = productResponse.id,
            name = productResponse.name,
            price = productResponse.price ?: "0",
            regularPrice = productResponse.regularPrice ?: "0",
            salePrice = productResponse.salePrice ?: "0",
            parentId = productResponse.parentId.toString(),
            categories = productResponse.categories!!.map { it.id }.joinToString(separator = ","),
            images = productResponse.images!!.map { it.src }.joinToString(separator = ","),
            barcode = productResponse.barcode ?: "",
            options = options,
            quantity = 1,
            menuOrder = productResponse.menuOrder ?: 0,
        )
    }

    private fun syncAllWithoutProducts() {
        productPage = 1
        viewModelScope.launch {
            val url = AppSettings.Cloud.Host

            val syncOrderStatus = async { orderStatusRepository.syncOrderStatus(url) }

            val categoriesRequest = formatSyncCategories()
            val syncCategories = async {
                categoryRepository.syncCategories(url, categoriesRequest)
            }

            val syncPlateModels = async {
                plateModelRepository.syncPlateModels(url)
            }

            val syncTimeBlocks = async {
                timeBlockRepository.syncTimeBlocks(url)
            }

            val syncMenus = async {
                menuRepository.syncMenus(url)
            }

            if (syncCategories.await().status == Resource.Status.SUCCESS &&
                syncOrderStatus.await().status == Resource.Status.SUCCESS &&
                syncPlateModels.await().status == Resource.Status.SUCCESS &&
                syncTimeBlocks.await().status == Resource.Status.SUCCESS &&
                syncMenus.await().status == Resource.Status.SUCCESS
            ) {
                syncOrderStatus.await().data?.let { response ->
                    PrefUtil.setString(
                        "AppSettings.Cloud.AllOrderStatus",
                        response.data.joinToString(separator = "|")
                    )

                    // Refresh Configuration
                    AppSettings.getAllSetting()
                }

                syncCategories.await().data?.let { response ->
                    // Delete old Categories
                    categoryRepository.deleteAll()

                    // Save new Categories
                    response.forEach { _categoryEntity ->
                        val categoryEntity = formatCategory(_categoryEntity)
                        categoryRepository.insert(categoryEntity)
                    }
                }

                syncPlateModels.await().data?.let { response ->
                    // Delete old Plates Model
                    plateModelRepository.deleteAll()

                    // Save new Plates Model
                    listPlateModels.clear()
                    response.results.forEach { _plateModelEntity ->
                        val plateModelEntity = formatPlateModel(_plateModelEntity)
                        listPlateModels.add(plateModelEntity)
                        plateModelRepository.insert(plateModelEntity)
                    }
                    AppContainer.GlobalVariable.listPlatesModel = listPlateModels
                }

                syncTimeBlocks.await().data?.let { response ->
                    // Delete old Time Blocks
                    timeBlockRepository.deleteAll()

                    // Save new Time Blocks
                    listTimeBlocks.clear()
                    response.results.forEach { _timeBlock ->
                        val timeBlockEntity = formatTimeBlock(_timeBlock)
                        listTimeBlocks.add(timeBlockEntity)
                        timeBlockRepository.insert(timeBlockEntity)
                    }
                }

                syncMenus.await().data?.let { _response ->
                    // Delete old Menus
                    menuRepository.deleteAll()

                    // Save new Menus
                    _response.results.forEach { _menu ->
                        val menuDate = _menu.date
                        for (j in _menu.menus.indices) {
                            for (k in _menu.menus[j].products.indices) {
                                val plateModelEntity =
                                    getPlateModelById(_menu.menus[j].products[k].plateModelId ?: "")
                                val productEntity =
                                    listProducts.find { productEntity -> productEntity.syncId.toString() == _menu.menus[j].products[k].productId }
                                if (productEntity != null) {
                                    val menuEntity = formatMenu(
                                        plateModelEntity,
                                        productEntity,
                                        menuDate ?: "",
                                        _menu.menus[j],
                                        _menu.menus[j].products[k]
                                    )
                                    menuRepository.insert(menuEntity)
                                }
                            }
                        }
                    }
                }

                _state.value = State(
                    Resource.Status.SUCCESS,
                    resources.getString(R.string.message_success_sync)
                )
            } else {
                var message = ""
                if (syncCategories.await().status != Resource.Status.SUCCESS) {
                    message += "${
                        String.format(
                            resources.getString(R.string.message_error_sync_item),
                            "Categories"
                        )
                    } ${syncCategories.await().message}\n"
                }
                if (syncOrderStatus.await().status != Resource.Status.SUCCESS) {
                    message += "${
                        String.format(
                            resources.getString(R.string.message_error_sync_item),
                            "Order Status"
                        )
                    } ${syncOrderStatus.await().message}\n"
                }
                if (syncPlateModels.await().status != Resource.Status.SUCCESS) {
                    message += "${
                        String.format(
                            resources.getString(R.string.message_error_sync_item),
                            "Plate Models"
                        )
                    } ${syncPlateModels.await().message}\n"
                }
                if (syncTimeBlocks.await().status != Resource.Status.SUCCESS) {
                    message += "${
                        String.format(
                            resources.getString(R.string.message_error_sync_item),
                            "Time Blocks"
                        )
                    } ${syncTimeBlocks.await().message}\n"
                }
                if (syncMenus.await().status != Resource.Status.SUCCESS) {
                    message += "${
                        String.format(
                            resources.getString(R.string.message_error_sync_item),
                            "Menus"
                        )
                    } ${syncMenus.await().message}\n"
                }
                _state.value = State(
                    Resource.Status.ERROR,
                    message
                )
            }

            // Sync All User
            val getAllUsersRequest = formatGetAllUsers()
            Log.e(TAG, gson.toJson(getAllUsersRequest))
            val syncUsers = async {
                userRepository.getAllUsers(url, getAllUsersRequest)
            }

            Log.e(TAG, syncUsers.await().status.toString())
            Log.e(TAG, gson.toJson(syncUsers.await().data))
            if (syncUsers.await().status == Resource.Status.SUCCESS) {
                syncUsers.await().data?.let { response ->
                    if (response.success) {
                        // Delete old Users
                        userRepository.deleteAll()

                        // Save new Users
                        val listUsers: MutableList<UserEntity> = mutableListOf()
                        val listRoles: MutableList<String> = mutableListOf()
                        response.data.forEach { _userResponse ->
                            val userEntity = UserEntity(
                                id = _userResponse.id.toInt(),
                                displayName = _userResponse.displayName,
                                roles = if (_userResponse.roles.isNotEmpty()) _userResponse.roles.joinToString(separator = ", ") else "",
                                ccwId1 = if (_userResponse.ccwId1.isEmpty()) "" else _userResponse.ccwId1,
                                ccwId2 = if (_userResponse.ccwId2.isEmpty()) "" else _userResponse.ccwId2,
                                ccwId3 = if (_userResponse.ccwId3.isEmpty()) "" else _userResponse.ccwId3
                            )
                            listUsers.add(userEntity)

                            if (_userResponse.roles.isNotEmpty()) {
                                _userResponse.roles.forEach { _role ->
                                    if (!listRoles.contains(_role)) {
                                        listRoles.add(_role)
                                    }
                                }
                            }
                        }
                        if (listUsers.isNotEmpty())
                            userRepository.insertAll(listUsers.toList())

                        if (listRoles.isNotEmpty()) {
                            PrefUtil.setString("AppSettings.Options.RolesList", listRoles.joinToString(separator = ","))

                            // Refresh Configuration
                            AppSettings.getAllSetting()
                        }
                    }
                }
            } else {
                Log.e(TAG, syncUsers.await().message.toString())
            }
        }
    }

    private fun formatGetAllUsers(): GetAllUserRequest {
        return GetAllUserRequest(
            AppContainer.GlobalVariable.currentToken
        )
    }

    private fun formatSyncCategories(): CategoriesRequest {
        return CategoriesRequest(
            categoryPage,
            perPage,
            consumerKey,
            consumerSecret
        )
    }

    private fun formatCategory(_category: Category): CategoryEntity {
        return CategoryEntity(
            id = _category.id ?: 0,
            name = _category.name ?: "",
            parent = _category.parent ?: 0,
            menuOrder = _category.menuOrder ?: 0
        )
    }

    private fun formatPlateModel(_plateModel: PlateModel): PlateModelEntity {
        return PlateModelEntity(
            id = _plateModel.plateModelId.toInt(),
            plateModelId = _plateModel.plateModelId,
            plateModelCode = _plateModel.plateModelCode,
            plateModelTitle = _plateModel.plateModelTitle,
            lastPlateSerial = _plateModel.lastPlateSerial ?: ""
        )
    }

    private fun formatTimeBlock(timeBlock: TimeBlock): TimeBlockEntity {
        return TimeBlockEntity(
            id = timeBlock.timeBlockId?.toInt() ?: 0,
            timeBlockId = timeBlock.timeBlockId ?: "",
            fromHour = timeBlock.fromHour ?: "",
            timeBlockTitle = timeBlock.timeBlockTitle ?: "",
            toHour = timeBlock.toHour ?: ""
        )
    }

    private fun formatMenu(
        plateModelEntity: PlateModelEntity?,
        productEntity: ProductEntity?,
        menuDate: String,
        menuDetail: MenuDetail,
        product: Product
    ): MenuEntity {
        return MenuEntity(
            id = 0,
            menuDate = menuDate,
            timeBlockId = menuDetail.timeBlockId ?: "",
            productId = product.productId.toString(),
            plateModelId = product.plateModelId ?: "",
            price = product.price ?: "0",
            productName = getProductNameById(product.productId.toString()),
            plateModelName = plateModelEntity?.plateModelTitle ?: "",
            plateModelCode = plateModelEntity?.plateModelCode ?: "",
            timeBlockTitle = getTimeBlockTitleById(menuDetail.timeBlockId),
            quantity = 1,
            options = productEntity?.options ?: ""
        )
    }

    private fun getProductNameById(productId: String): String {
        var productName = ""
        listProducts.forEach { productEntity ->
            if (productId == productEntity.syncId.toString()) {
                productName = productEntity.name
                return productName
            }
        }

        return productName
    }

    private fun getPlateModelById(plateModelId: String): PlateModelEntity? {
        listPlateModels.forEach { plateModelEntity ->
            if (plateModelEntity.plateModelId == plateModelId) {
                return plateModelEntity
            }
        }
        return null
    }

    private fun getTimeBlockTitleById(timeBlockId: String?): String {
        var timeBlockTitle = ""
        listTimeBlocks.forEach { timeBlockEntity ->
            if (timeBlockId == timeBlockEntity.timeBlockId) {
                timeBlockTitle = timeBlockEntity.timeBlockTitle
                return timeBlockTitle
            }
        }

        return timeBlockTitle
    }
}