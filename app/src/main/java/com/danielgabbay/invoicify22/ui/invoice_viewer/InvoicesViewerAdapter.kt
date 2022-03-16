package com.danielgabbay.invoicify22.ui.invoice_viewer

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.danielgabbay.invoicify22.Global
import com.danielgabbay.invoicify22.Global.FIVE_MEGABYTES
import com.danielgabbay.invoicify22.Global.convertBytearrayToPdf
import com.danielgabbay.invoicify22.R
import com.danielgabbay.invoicify22.model.Invoice
import com.danielgabbay.invoicify22.ui.ActionOpenDocumentFragment
import com.danielgabbay.invoicify22.ui.LoadingDialog
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.BaseMultiplePermissionsListener
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

const val DOCUMENT_VIEWER_FRAGMENT_TAG = "com.danielgabbay.invoicify22.tags.DOCUMENT_VIEWER_FRAGMENT_TAG"

class InvoicesViewerAdapter(
    private var invoices: ArrayList<Invoice?>,
    private val srcActivity: FragmentActivity,
    private var loadingDialogRef: LoadingDialog
) :
    RecyclerView.Adapter<InvoicesViewerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        Dexter.withContext(srcActivity)
            .withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ).withListener(
                object : BaseMultiplePermissionsListener() {
                    override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                        super.onPermissionsChecked(p0)
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        p0: MutableList<PermissionRequest>?,
                        p1: PermissionToken?
                    ) {
                        super.onPermissionRationaleShouldBeShown(p0, p1)
                    }

                }
            )
        val v =
            LayoutInflater.from(p0.context).inflate(R.layout.recyclerview_invoices_item, p0, false)
        return ViewHolder(v);
    }

    private fun formatDate(date: LocalDateTime): String {
        val dateTimePattern = "dd/MM/yy"
        val formatter = DateTimeFormatter.ofPattern(dateTimePattern)
        return date.format(formatter)
    }

    fun Double.round(decimals: Int = 2): Double = "%.${decimals}f".format(this).toDouble()

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val inv = invoices[position]
        holder.open_inv_btn.setOnClickListener {
            //שליפת החשבונית מהדטהבייס

            loadingDialogRef.startLoadingDialog("Loading...")

            var store = Global.storageRef.reference
            var invRef = store.child(inv?.invoice_path!!)

            invRef.getBytes(FIVE_MEGABYTES).addOnSuccessListener {

                // שליחת הקובץ לתצוגה בpdf viewer
                val file = convertBytearrayToPdf(srcActivity, it)
                val uri = Uri.fromFile(file)
                val fragment = ActionOpenDocumentFragment.newInstance(uri, true, null, "view")

                srcActivity.supportFragmentManager.beginTransaction()
                    .add(R.id.invoices_list_container, fragment, DOCUMENT_VIEWER_FRAGMENT_TAG)
                    .addToBackStack(null)
                    .commit()

                loadingDialogRef.dismissLoadingDialog()


            }.addOnFailureListener {
                // Handle any errors
            }

        }

        holder.inv_detail_text.text = "Seller: ${inv?.seller_name}\n" +
                "Date: ${inv?.creationDate}\n" +
                "Total Amount: ${inv?.totalAmount}"

    }

    override fun getItemCount(): Int {
        return invoices.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var open_inv_btn: Button = itemView.findViewById(R.id.open_inv_btn)
        var inv_detail_text: TextView = itemView.findViewById(R.id.inv_detail_text)
    }
}