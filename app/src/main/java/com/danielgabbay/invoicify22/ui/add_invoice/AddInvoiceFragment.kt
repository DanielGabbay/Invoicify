package com.danielgabbay.invoicify22.ui.add_invoice

import android.app.Activity
import android.content.*
import android.content.ContentValues.TAG
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.danielgabbay.invoicify22.Global
import com.danielgabbay.invoicify22.Global.cur_user
import com.danielgabbay.invoicify22.Global.customFormatDate
import com.danielgabbay.invoicify22.Global.getInvoicesCountByMonthKey
import com.danielgabbay.invoicify22.Global.newInvoice
import com.danielgabbay.invoicify22.Global.parseToDouble
import com.danielgabbay.invoicify22.Global.selected_month
import com.danielgabbay.invoicify22.Global.selected_year
import com.danielgabbay.invoicify22.R
import com.danielgabbay.invoicify22.databinding.FragmentAddInvoiceBinding
import com.danielgabbay.invoicify22.model.Invoice
import com.danielgabbay.invoicify22.ui.ActionOpenDocumentFragment
import com.danielgabbay.invoicify22.ui.LoadingDialog
import java.lang.Integer.parseInt
import java.time.LocalDateTime
import java.util.*

private const val OPEN_DOCUMENT_REQUEST_CODE = 0x33
private const val OPEN_CAMERA_REQUEST_CODE = 0x34

const val DOCUMENT_FRAGMENT_TAG = "com.danielgabbay.invoicify22.tags.DOCUMENT_FRAGMENT"
private const val LAST_OPENED_URI_KEY = "com.danielgabbay.invoicify22.pref.LAST_OPENED_URI_KEY"

/**
 * A simple [Fragment] subclass.
 * Use the [AddInvoiceFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@Suppress("DEPRECATION")
class AddInvoiceFragment : Fragment() {
    private lateinit var binding: FragmentAddInvoiceBinding
    private lateinit var noDocumentView: ViewGroup
    private var creationDateString: String? = null
    private lateinit var loadingDialog: LoadingDialog
    var bitmapArray: ArrayList<Bitmap> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentAddInvoiceBinding.inflate(inflater, container, false)
        loadingDialog = LoadingDialog(requireActivity())

        if (newInvoice != null) {
            binding.sellerNameText.setText(newInvoice!!.seller_name)
            binding.totalAmountText.setText(newInvoice!!.totalAmount.toString())
        }
        noDocumentView = binding.noDocumentView

        //מאזינים לui
        binding.uploadInvoiceFs.setOnClickListener {
            saveNewInvoiceForm()
            chooseFile()
        }

        binding.captureInvoiceBtn.setOnClickListener {
            saveNewInvoiceForm()
            dispatchTakePictureIntent()
        }

        // Set date change listener on calenderView.
        // Callback notified when user select a date from CalenderView on UI.
        binding.creationDateField.setOnDateChangeListener { calView: CalendarView, year: Int, month: Int, dayOfMonth: Int ->

            // Create calender object with which will have system date time.
            val calender: Calendar = Calendar.getInstance()

            // Set attributes in calender object as per selected date.
            calender.set(year, month, dayOfMonth)

            // Now set calenderView with this calender object to highlight selected date on UI.
            calView.setDate(calender.timeInMillis, true, true)

            creationDateString = "$dayOfMonth/${month + 1}/$year"
            selected_year = year
            selected_month = month
        }

        return binding.root
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                OPEN_DOCUMENT_REQUEST_CODE -> {
                    resultData?.data?.also { documentUri ->
                        context?.contentResolver?.takePersistableUriPermission(
                            documentUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        openDocument(documentUri)
//                        uploadFile(
//                            documentUri,
//                            newInvoice?.invoice_path!!,
//                            loadingDialog,
//                            this@AddInvoiceFragment
//                        )
                    }
                }
                OPEN_CAMERA_REQUEST_CODE -> {
                    val imageBitmap = resultData?.extras?.get("data") as Bitmap
                    bitmapArray.add(imageBitmap)
                    openImageInViewer()
                }
            }
        }
    }

    private fun saveNewInvoiceForm() {
        newInvoice = Invoice()
        if (selected_month == -1) {
            selected_month = parseInt(customFormatDate(LocalDateTime.now(), "MM")) - 1
        }
        if (selected_year == -1) {
            selected_year = parseInt(customFormatDate(LocalDateTime.now(), "yyyy"))
        }
        newInvoice?.month_year_key = Global.getMonthYearKey(selected_month, selected_year)
        newInvoice?.month = selected_month
        newInvoice?.year = selected_year
        newInvoice?.seller_name = binding.sellerNameText.text.toString()
        newInvoice?.totalAmount = binding.totalAmountText.text.parseToDouble()
        if (creationDateString != null)
            newInvoice?.creationDate = creationDateString

        var monthInvoices: ArrayList<Invoice?>
        if (cur_user?.user_invoices?.invoices_data?.containsKey(newInvoice?.month_year_key) == true) {
            monthInvoices =
                cur_user?.user_invoices?.invoices_data?.get(newInvoice?.month_year_key)!!
        } else {
            monthInvoices = arrayListOf()
        }
        monthInvoices.add(newInvoice)
        cur_user?.user_invoices?.invoices_data?.set(newInvoice?.month_year_key!!, monthInvoices)
        cur_user?.user_invoices?.count = cur_user?.user_invoices?.count?.plus(1)!!

        val count: Int? = getInvoicesCountByMonthKey(Global.getMonthYearKey(0, 0))
        newInvoice?.setPath(cur_user?.id!!, newInvoice?.month_year_key!!, count!!)

        val dbUser = Global.mDatabase.child("users").child(cur_user?.id.toString())
        dbUser.setValue(cur_user).addOnSuccessListener {
            Log.d(TAG, "123")
        }

    }

    private fun chooseFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, OPEN_DOCUMENT_REQUEST_CODE)
    }

    private fun openDocumentPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            /**
             * It's possible to limit the types of files by mime-type. Since this
             * app displays pages from a PDF file, we'll specify `application/pdf`
             * in `type`.
             * See [Intent.setType] for more details.
             */
            type = "application/pdf"

            /**
             * Because we'll want to use [ContentResolver.openFileDescriptor] to read
             * the data of whatever file is picked, we set [Intent.CATEGORY_OPENABLE]
             * to ensure this will succeed.
             */
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, OPEN_DOCUMENT_REQUEST_CODE)
    }

    private fun openImageInViewer() {
        val fragment = ActionOpenDocumentFragment.newInstanceForImages(bitmapArray, 0, activity)

        activity?.supportFragmentManager?.beginTransaction()
            ?.add(R.id.container, fragment, DOCUMENT_FRAGMENT_TAG)
            ?.addToBackStack(null)
            ?.commit();
    }

    private fun openDocument(documentUri: Uri) {
        /**
         * Save the document to [SharedPreferences]. We're able to do this, and use the
         * uri saved indefinitely, because we called [ContentResolver.takePersistableUriPermission]
         * up in [onActivityResult].
         */
        context?.getSharedPreferences(TAG, Context.MODE_PRIVATE)?.edit {
            putString(LAST_OPENED_URI_KEY, documentUri.toString())
        }

        val fragment =
            ActionOpenDocumentFragment.newInstance(documentUri, false, requireActivity(), null)

        activity?.supportFragmentManager?.beginTransaction()
            ?.add(R.id.container, fragment, DOCUMENT_FRAGMENT_TAG)
            ?.addToBackStack(null)
            ?.commit();

        // Document is open, so get rid of the call to action view.
        noDocumentView.visibility = View.GONE
    }


    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(takePictureIntent, OPEN_CAMERA_REQUEST_CODE)
        } catch (e: ActivityNotFoundException) {
            // display error state to the user
        }
    }
}
