package com.danielgabbay.invoicify22

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.text.Editable
import android.util.Base64
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.danielgabbay.invoicify22.model.Invoice
import com.danielgabbay.invoicify22.model.User
import com.danielgabbay.invoicify22.model.UserInvoices
import com.danielgabbay.invoicify22.ui.LoadingDialog
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


object Global {
    //global constants
    const val FIVE_MEGABYTES: Long = 5 * 1024 * 1024

    lateinit var mDatabase: DatabaseReference
    lateinit var storageRef: FirebaseStorage
    lateinit var googleSignInClient: GoogleSignInClient

    //user info
    var current_user_id: String? = null
    var current_fb_user: FirebaseUser? = null
    var cur_user: User? = null

    //user filter month selected
    var selected_year: Int = -1
    var selected_month: Int = -1

    //temp globals
    var newInvoice: Invoice? = null

    fun customFormatDate(date: LocalDateTime, dateTimePattern: String): String {
//        val dateTimePattern = "dd/MM/yy"
        val formatter = DateTimeFormatter.ofPattern(dateTimePattern)
        return date.format(formatter)
    }

    fun Editable.parseToDouble() = when (isNotEmpty()) {
        true -> trim().toString().toDoubleOrNull() ?: 0.0
        else -> 0.0
    }

    fun initCurrentUser() {
        if (current_fb_user != null) {
            cur_user = User()
            cur_user!!.id = current_fb_user!!.uid
            cur_user!!.display_name = current_fb_user!!.displayName
            cur_user!!.email = current_fb_user!!.email
            cur_user!!.permissions_level = 1
            cur_user!!.group_id = null
            cur_user!!.user_invoices = UserInvoices(0, mutableMapOf())
        }
    }

    fun getMonthYearKey(month: Int, year: Int): String {
        if (month == 0 && year == 0) {
            val m = Integer.parseInt(customFormatDate(LocalDateTime.now(), "MM")) - 1
            val y = Integer.parseInt(customFormatDate(LocalDateTime.now(), "yyyy"))
            return "$m$y"
        }
        return "$month$year"
    }

    @Throws(IOException::class)
    fun decodeToBitmap(image: String?): Bitmap? {
        val decodedByteArray = Base64.decode(image, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedByteArray, 0, decodedByteArray.size)
    }

    fun filterMonthYearInvoices(month_year_key: String): ArrayList<Invoice?> {
        if (month_year_key == "") {
            var res = ArrayList<Invoice?>()
            val keys = cur_user?.user_invoices?.invoices_data?.keys
            if (keys?.isNotEmpty() == true) {
                keys.forEach { it1 ->
                    if (cur_user?.user_invoices?.invoices_data?.containsKey(it1) == true) {
                        val invMonth = cur_user?.user_invoices?.invoices_data?.get(it1)
                        invMonth?.forEach {
                            res.add(it)
                        }
                    }
                }
                return res
            }
            return arrayListOf()
        } else {
            if (cur_user?.user_invoices?.invoices_data?.isNotEmpty() == true) {
                if (cur_user?.user_invoices?.invoices_data?.containsKey(month_year_key) == true) {
                    return cur_user?.user_invoices?.invoices_data?.get(month_year_key)!!
                }
            }
            return arrayListOf()
        }
    }

    fun getInvoicesCountByMonthKey(month_year_key: String): Int? {
        if (cur_user?.user_invoices?.invoices_data?.containsKey(month_year_key) == true) {
            var monthInvoicesArray: ArrayList<Invoice?>? =
                cur_user?.user_invoices?.invoices_data?.get(month_year_key)
            return monthInvoicesArray?.count()
        }
        return -1
    }

    fun convertBytearrayToPdf(context: Context, bytes: ByteArray): File {
        val path = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        var file = File.createTempFile("my_file", ".pdf", path)
        var os = FileOutputStream(file);
        os.write(bytes);
        os.close();

        return file
    }

}


object Constants {
    var user_path: String? = null
}

