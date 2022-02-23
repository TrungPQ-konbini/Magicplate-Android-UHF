package com.konbini.magicplateuhf.data.remote.product.response

import com.google.gson.annotations.SerializedName

data class ProductResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("slug")
    val slug: String?,
    @SerializedName("permalink")
    val permalink: String?,
    @SerializedName("date_created")
    val dateCreated: String?,
    @SerializedName("date_created_gmt")
    val dateCreatedGmt: String?,
    @SerializedName("date_modified")
    val dateModified: String?,
    @SerializedName("date_modified_gmt")
    val dateModifiedGmt: String?,
    @SerializedName("type")
    val type: String?,
    @SerializedName("status")
    val status: String?,
    @SerializedName("featured")
    val featured: Boolean = false,
    @SerializedName("catalog_visibility")
    val catalogVisibility: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("short_description")
    val shortDescription: String?,
    @SerializedName("sku")
    val sku: String?,
    @SerializedName("price")
    val price: String?,
    @SerializedName("regular_price")
    val regularPrice: String?,
    @SerializedName("sale_price")
    val salePrice: String?,
    @SerializedName("on_sale")
    val onSale: Boolean = false,
    @SerializedName("purchasable")
    val purchasable: Boolean = false,
    @SerializedName("total_sales")
    val totalSales: Int?,
    @SerializedName("parent_id")
    val parentId: Int?,
    val categories: List<CategoryProduct>? = null,
    val images: List<Image>? = null,
    @SerializedName("ean_code")
    var eanCode: String?,
    var barcode: String?,
    val options: String?,
    @SerializedName("menu_order")
    val menuOrder: Int?,
    @SerializedName("meta_data")
    var metaData: List<MetaData>? = null
)
