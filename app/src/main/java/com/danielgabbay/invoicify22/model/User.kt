package com.danielgabbay.invoicify22.model

data class User(
    var display_name: String? = null,
    var email: String? = null,
    var permissions_level: Int = 0, //0 = locked (default)| 1 = simple user| 2 = admin

    //group information
    var group_id: String? = null,

    //invoices metadata
    var user_invoices: UserInvoices = UserInvoices()
) {
    var id: String? = null
        set(value) {
            field = "U_$value"
        }
}

data class UserInvoices(
    var count: Int = 0,
    var invoices_data: MutableMap<String, ArrayList<Invoice?>> = mutableMapOf()
)