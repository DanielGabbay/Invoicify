package com.danielgabbay.invoicify22.ui.invoice_viewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.danielgabbay.invoicify22.Global.filterMonthYearInvoices
import com.danielgabbay.invoicify22.Global.getMonthYearKey
import com.danielgabbay.invoicify22.databinding.FragmentInvoicesViewerBinding
import com.danielgabbay.invoicify22.model.Invoice
import com.danielgabbay.invoicify22.ui.LoadingDialog
import java.text.DateFormatSymbols
import java.util.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


/**
 * A simple [Fragment] subclass.
 * Use the [InvoicesViewerFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class InvoicesViewerFragment(var year: Int?, var month: Int?) : Fragment() {
    //
    lateinit var binding: FragmentInvoicesViewerBinding
    var invoices: ArrayList<Invoice?> = arrayListOf()
    private lateinit var loadingDialog: LoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentInvoicesViewerBinding.inflate(inflater, container, false)
        loadingDialog = LoadingDialog(requireActivity())

        if (month != 0 && year != 0) {
            val monthName = DateFormatSymbols(Locale.US).months[month?.minus(1)!!];
            binding.dateRangeText.text = "$monthName $year"
        }
        //פילטור החשבוניות של החודש שנבחר
        var key = getMonthYearKey((month?.minus(1))!!, year!!)
        invoices = filterMonthYearInvoices(key)
        //אתחול הנתונים בליסט
        if (invoices.size != 0) {
            val invoicesViewerAdapter =
                InvoicesViewerAdapter(invoices, requireActivity(), loadingDialog)
            binding.recyclerview2.adapter = invoicesViewerAdapter
            binding.recyclerview2.layoutManager = LinearLayoutManager(context)
        }
        return binding.root
    }

}