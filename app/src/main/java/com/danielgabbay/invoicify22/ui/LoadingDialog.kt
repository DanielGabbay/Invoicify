package com.danielgabbay.invoicify22.ui;

import android.app.Activity
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.danielgabbay.invoicify22.R
@Suppress("DEPRECATION")
class LoadingDialog(myActivity: Activity) {

    private var activity: Activity = myActivity
    private lateinit var dialog: AlertDialog

    fun startLoadingDialog(customLoadingMessage: String?) {
        val builder: AlertDialog.Builder? = activity.let {
            AlertDialog.Builder(it)
        }
        val inflater: LayoutInflater = activity.layoutInflater
        val v: View = inflater.inflate(R.layout.dialog_loading, null)
        builder?.setView(v)


        val msg: TextView = v.findViewById(R.id.loadingMsg)
        if (customLoadingMessage != null) {
            msg.text = customLoadingMessage.toString()
        }

        builder?.setCancelable(true)

        dialog = builder?.create()!!

        dialog.show()
    }


    fun dismissLoadingDialog() {
        dialog.dismiss()
    }


}
