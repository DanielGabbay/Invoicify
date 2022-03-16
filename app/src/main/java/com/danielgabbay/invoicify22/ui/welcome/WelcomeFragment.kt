package com.danielgabbay.invoicify22.ui.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.danielgabbay.invoicify22.Global
import com.danielgabbay.invoicify22.Global.cur_user
import com.danielgabbay.invoicify22.Global.filterMonthYearInvoices
import com.danielgabbay.invoicify22.databinding.FragmentWelcomeBinding


/**
 * A simple [Fragment] subclass.
 * Use the [WelcomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class WelcomeFragment : Fragment() {
    private lateinit var binding: FragmentWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        //אתחול הודעת הפתיחה עבור המשתמש
        binding.helloUserText.text =
            "Welcome " + (Global.current_fb_user?.displayName ?: "User") + " \uD83D\uDE0A"

        //אתחול מאזינים

        // אתחול טבלת החשבוניות האחרונות שנוספו
        binding.recyclerview1.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
//        //הוספת הנתונים לrecyclerview

        if (cur_user?.user_invoices?.invoices_data?.isNotEmpty() == true) {
            val invoices = filterMonthYearInvoices("")
            val recentInvoicesAdapter = RecentInvoicesAdapter(invoices)
            binding.recyclerview1.adapter = recentInvoicesAdapter
        }

        return binding.root
    }

//    private fun addNewInvoice() {
//        Log.d(TAG, "onDebugClicked")
//
//        val dbUsers = Global.mDatabase.child("users").child(cur_user!!.id!!).child("invoices")
//
//
//        var inv = Invoice()
//        inv.id = "I_${generateUniqueId()}"
//        inv.creationDate = formatDate(LocalDateTime.now())
//        inv.totalAmount = Random.nextDouble(40.0, 10000.0)
//        inv.seller_name = binding.sellerNameText.text.toString()
//        inv.group_id = cur_user!!.group_id
//
//        dbUsers.child(inv.id!!).setValue(inv)
//
//    }

}