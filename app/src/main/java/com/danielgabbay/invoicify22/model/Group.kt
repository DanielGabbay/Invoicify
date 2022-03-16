package com.danielgabbay.invoicify22.model

data class Group(
    var admin_id: String? = null,
    var users: ArrayList<String> = arrayListOf(),   // ליסט עם כל המזהים של המשתמשים בקבוצה
    var invoices: ArrayList<String> = arrayListOf(), //נתיבים למיקום בקלאוד עבור על החשבוניות של כל המשתמשים בקבוצה - האחרונים יהיו האיברים האחרונים בליסט
) {
    //מזהה הקבוצה יהיה G_ + המייל של המנהל
    var id: String? = null
        set(value) {
            id = "G_$value"
        }
}
