package com.danielgabbay.invoicify22.model

data class Invoice(
    var id: String? = null,
    var group_id: String? = null,
    var seller_name: String? = null,
    var creationDate: String? = null,
    var totalAmount: Double? = 0.0,
    //invoice path in firebase storage
    var invoice_path: String? = null
) {
    var month_year_key: String? = null
    var month: Int = 0
    var year: Int = 0

    fun setPath(current_user_id: String, month_year_key: String, count: Int) {
        invoice_path = "$current_user_id/$month_year_key/$count"
    }
}

