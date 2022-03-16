package com.danielgabbay.invoicify22.ui.welcome

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.danielgabbay.invoicify22.R
import com.danielgabbay.invoicify22.model.Invoice
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class RecentInvoicesAdapter(private val recentInvoices: ArrayList<Invoice?>) :
    RecyclerView.Adapter<RecentInvoicesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val v =
            LayoutInflater.from(p0.context).inflate(R.layout.recycleritem_invoice_data, p0, false)
        return ViewHolder(v);
    }

    private fun formatDate(date: LocalDateTime): String {
        val dateTimePattern = "dd/MM/yy"
        val formatter = DateTimeFormatter.ofPattern(dateTimePattern)
        return date.format(formatter)
    }

    fun Double.round(decimals: Int = 2): Double = "%.${decimals}f".format(this).toDouble()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.inv_date.text = recentInvoices[position]?.creationDate
        holder.inv_seller.text = recentInvoices[position]?.seller_name.toString()
        holder.inv_amount.text =
            (recentInvoices[position]?.totalAmount as Double).round(2).toString()
    }

    override fun getItemCount(): Int {
        return recentInvoices.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var inv_date: TextView = itemView.findViewById(R.id.inv_date)
        var inv_user: TextView = itemView.findViewById(R.id.inv_user)
        var inv_seller: TextView = itemView.findViewById(R.id.inv_seller)
        var inv_amount: TextView = itemView.findViewById(R.id.inv_amount)
    }
}