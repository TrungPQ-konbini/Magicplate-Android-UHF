package com.konbini.magicplateuhf.base

open class BaseEntity {
     var syncId: Int = 0
     var syncedDate: String = ""
     var dateCreated: String = ""
     var dateUpdated: String = ""
     var dateDeleted: String = ""
     var userCreated: String = ""
     var userUpdated: String = ""
     var userDeleted: String = ""
     var isDeleted: Boolean = false
     var activated: Boolean = false
}