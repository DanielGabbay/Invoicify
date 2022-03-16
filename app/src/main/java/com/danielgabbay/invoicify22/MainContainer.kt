package com.danielgabbay.invoicify22

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.danielgabbay.invoicify22.Global.cur_user
import com.danielgabbay.invoicify22.Global.selected_month
import com.danielgabbay.invoicify22.Global.selected_year
import com.danielgabbay.invoicify22.databinding.ActivityMainContainerBinding
import com.danielgabbay.invoicify22.login.LoginActivity
import com.danielgabbay.invoicify22.model.Invoice
import com.danielgabbay.invoicify22.ui.add_invoice.AddInvoiceFragment
import com.danielgabbay.invoicify22.ui.invoice_viewer.InvoicesViewerFragment
import com.danielgabbay.invoicify22.ui.invoice_viewer.MonthYearPickerDialog
import com.danielgabbay.invoicify22.ui.welcome.WelcomeFragment
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class MainContainer : AppCompatActivity() {
    lateinit var binding: ActivityMainContainerBinding

    // Initialise the DrawerLayout, NavigationView and ToggleBar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var navView: NavigationView
    private var isDrawerOpen = false
    private lateinit var navHeader: android.view.View

    //image
    private lateinit var imageUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainContainerBinding.inflate(layoutInflater)


        navView = binding.navView
        drawerLayout = binding.drawerLayout

        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Call syncState() on the action bar so it'll automatically change to the back button when the drawer layout is open
        toggle.syncState()

        // Display the hamburger icon to launch the drawer
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // הגדרת מאזין ללחיצות בתפריט הצד
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_welcome -> {
                    replaceFragment(WelcomeFragment())
                    true
                }
                R.id.nav_add_new_invoice -> {
                    Global.newInvoice = Invoice()
                    replaceFragment(AddInvoiceFragment())
                    true
                }
                R.id.nav_invoices_viewer -> {
                    showSelectMonthDialog()
                    true
                }
                R.id.nav_group_and_settings -> {
                    true
                }
                R.id.nav_logout -> {
                    Firebase.auth.signOut()
                    Global.googleSignInClient.signOut()
                    val intent = Intent(this@MainContainer, LoginActivity::class.java)
                    startActivity(intent)

                    true
                }
                else -> {
                    false
                }
            }
        }

        //side navigation initialization
        navHeader = this.navView.getHeaderView(0)
        navHeader.findViewById<TextView>(R.id.user_name).text = cur_user?.display_name
        navHeader.findViewById<TextView>(R.id.user_email).text = cur_user?.email
        setContentView(binding.root)
    }


    // override the onSupportNavigateUp() function to launch the Drawer when the hamburger icon is clicked
    override fun onSupportNavigateUp(): Boolean {
        isDrawerOpen = !isDrawerOpen

        when (isDrawerOpen) {
            true -> drawerLayout.openDrawer(navView)
            false -> drawerLayout.closeDrawers()
        }

        return isDrawerOpen
    }

    private fun replaceFragment(fragment: Fragment): Boolean {
        // replace a fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        // close the side menu
        drawerLayout.closeDrawers()
        return true
    }

    private fun showSelectMonthDialog() {
        MonthYearPickerDialog().apply {
            setListener { _, year, month, _ ->
                selected_year = year
                selected_month = month
                //TODO: סינון לפי החודש הנבחר ומעבר
                replaceFragment(InvoicesViewerFragment(year, month + 1))
            }
            show(supportFragmentManager, "MonthYearPickerDialog")
        }
    }

}