package com.danielgabbay.invoicify22.login

import android.Manifest
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.danielgabbay.invoicify22.Constants.user_path
import com.danielgabbay.invoicify22.Global.cur_user
import com.danielgabbay.invoicify22.Global.current_fb_user
import com.danielgabbay.invoicify22.Global.current_user_id
import com.danielgabbay.invoicify22.Global.googleSignInClient
import com.danielgabbay.invoicify22.Global.initCurrentUser
import com.danielgabbay.invoicify22.Global.mDatabase
import com.danielgabbay.invoicify22.Global.storageRef
import com.danielgabbay.invoicify22.MainContainer
import com.danielgabbay.invoicify22.R
import com.danielgabbay.invoicify22.databinding.ActivityLoginBinding
import com.danielgabbay.invoicify22.model.Invoice
import com.danielgabbay.invoicify22.model.User
import com.danielgabbay.invoicify22.ui.LoadingDialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.BaseMultiplePermissionsListener
@Suppress("DEPRECATION","UNCHECKED_CAST")
class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding
    private val SIGN_IN_REQ_CODE = 10
    private lateinit var loadingDialog: LoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Dexter.withContext(this)
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

        binding = ActivityLoginBinding.inflate(layoutInflater)

        //אתחול loadingDialog
        loadingDialog = LoadingDialog(this)

        // בר טעינה כאשר מתבצע תהליך ההתחברות ברקע - בהתחלה מוסתר ומוצג רק כאשר מתבצעת פעולת טעינה של נתונים או בדיקה מול הד״ב
        binding.progressBar.visibility = View.GONE

        //אתחול והתממשקות ראשונית לפיירבייס
        FirebaseApp.initializeApp(this)

        // הגדרת האופציה ללוגין דרך חשבון גוגל
        val gso = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id_))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        //*****************************  הגדרת אינסטנסים לפיירבייס   ********************************

        //קבלת אינסטנס להתחברות דרך פיירבייס
        auth = Firebase.auth

        //אתחול אינסטנס לד״ב
        mDatabase = FirebaseDatabase.getInstance().reference;

        //אתחול אינסטנס לאחסון קבצים בענן
        storageRef = Firebase.storage


        //*****************************  הגדרת המאזינים ללחצנים   ********************************
        binding.signWithGoogleBtn.setOnClickListener {
            signInWithGoogle()
        }

        setContentView(binding.root)
    }

    private fun signInWithGoogle() {
        loadingDialog.startLoadingDialog("Try To Logging In With Your Google Account...")
        val signInIntent: Intent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, SIGN_IN_REQ_CODE)
    }

    // onActivityResult() function : this is where
    // we provide the task and data for the Google Account
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == SIGN_IN_REQ_CODE) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d(ContentValues.TAG, "firebaseAuthWithGoogle:" + account.id)

                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(ContentValues.TAG, "Google sign in failed", e)
                loadingDialog.dismissLoadingDialog()
                Toast.makeText(this, "Google sign in failed ...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    current_fb_user = auth.currentUser
                    //הוספת המשתמש החדש לד״ב
                    if (current_fb_user != null) {
                        //create parsed user object to send to Firebase
                        initCurrentUser()
                        current_user_id = "U_" + FirebaseAuth.getInstance().currentUser!!.uid
                        user_path = "users/$current_user_id"

                        loadingDialog.startLoadingDialog("Loading your data from cloud ☁️ ...")
                        val mUser = mDatabase.child(user_path!!)

                        mUser.get().addOnSuccessListener { it ->
                            if (it.exists()) {
                                if (it.child("user_invoices/count").exists()) {
                                    val count =
                                        it.child("user_invoices/count").getValue(Int::class.java)
                                    cur_user?.user_invoices?.count = count!!

                                    if (count > 0) {
                                        val invs: Map<*, *> =
                                            it.child("user_invoices/invoices_data").value as Map<*, *>

                                        for (key in invs.keys) {
                                            var invoices: ArrayList<Invoice?> = arrayListOf()
                                            (invs[key] as ArrayList<HashMap<*, *>>).forEach { it1 ->
                                                var inv = Invoice()
                                                inv.id = it1["id"].toString()
                                                inv.seller_name = it1["seller_name"].toString()
                                                inv.totalAmount =
                                                    (it1["totalAmount"].toString()).toDouble()
                                                inv.creationDate = it1["creationDate"].toString()
                                                inv.month = it1["month"].toString().toInt()
                                                inv.year = it1["year"].toString().toInt()
                                                inv.invoice_path = it1["invoice_path"].toString()
                                                invoices.add(inv)
                                            }
                                            cur_user?.user_invoices?.invoices_data?.put(
                                                key as String,
                                                invoices
                                            )
                                        }
                                    }
                                }
                                loadingDialog.dismissLoadingDialog()
                                updateUI(cur_user)
                            } else {
                                initCurrentUser()
                                //send to Firebase db
                                user_path = "users/${cur_user!!.id}"
                                val dbUsers = mDatabase.child(user_path!!)
                                dbUsers.setValue(cur_user).addOnSuccessListener {
                                    loadingDialog.dismissLoadingDialog()
                                    Toast.makeText(
                                        this,
                                        "Google sign in Success :)",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                    updateUI(cur_user)
                                }
                            }
                        }
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    loadingDialog.dismissLoadingDialog()
                    Toast.makeText(this, "Google sign in failed ...", Toast.LENGTH_SHORT).show()
                    updateUI(null)

                }
            }
    }


    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        current_fb_user = auth.currentUser

        if (current_fb_user != null) {
            initCurrentUser()
            current_user_id = "U_" + FirebaseAuth.getInstance().currentUser!!.uid
            user_path = "users/$current_user_id"

            loadingDialog.startLoadingDialog("Loading your data from cloud ☁️ ...")
            val mUser = mDatabase.child(user_path!!)

            mUser.get().addOnSuccessListener { it ->
                if (it.exists()) {
                    if (it.child("user_invoices/count").exists()) {
                        val count = it.child("user_invoices/count").getValue(Int::class.java)
                        cur_user?.user_invoices?.count = count!!

                        if (count > 0) {
                            val invs: Map<*, *> =
                                it.child("user_invoices/invoices_data").value as Map<*, *>

                            for (key in invs.keys) {
                                var invoices: ArrayList<Invoice?> = arrayListOf()
                                (invs[key] as ArrayList<HashMap<*, *>>).forEach { it1 ->
                                    var inv = Invoice()
                                    inv.id = it1["id"].toString()
                                    inv.seller_name = it1["seller_name"].toString()
                                    inv.totalAmount = (it1["totalAmount"].toString()).toDouble()
                                    inv.creationDate = it1["creationDate"].toString()
                                    inv.month = it1["month"].toString().toInt()
                                    inv.year = it1["year"].toString().toInt()
                                    inv.invoice_path = it1["invoice_path"].toString()
                                    invoices.add(inv)
                                }
                                cur_user?.user_invoices?.invoices_data?.put(key as String, invoices)
                            }
                        }
                    }
                    loadingDialog.dismissLoadingDialog()
                    updateUI(cur_user)
                } else {
                    //  אם לא קיים יוזר בד״ב - הודעת שגיאה והישארות במסך הלוגין
                    loadingDialog.dismissLoadingDialog()
                    Toast.makeText(this@LoginActivity, "Failed Login ...", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateUI(user: User?) {
        if (user != null) {
            // Go to Dashboard activity
            val intent = Intent(this@LoginActivity, MainContainer::class.java)
            startActivity(intent)
        }
    }
}