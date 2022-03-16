/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.danielgabbay.invoicify22.ui

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.core.net.toUri
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.danielgabbay.invoicify22.Global.cur_user
import com.danielgabbay.invoicify22.Global.newInvoice
import com.danielgabbay.invoicify22.R
import com.danielgabbay.invoicify22.databinding.FragmentPdfViewerBinding
import com.danielgabbay.invoicify22.model.Invoice
import com.danielgabbay.invoicify22.ui.add_invoice.AddInvoiceFragment
import com.danielgabbay.invoicify22.ui.welcome.WelcomeFragment
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.IOException


private const val OPEN_CAMERA_REQUEST_CODE = 0x34

/**
 * Key string for saving the state of current page index.
 */
private const val CURRENT_PAGE_INDEX_KEY =
    "com.example.android.actionopendocument.state.CURRENT_PAGE_INDEX_KEY"

const val TAG = "ActionOpenDocumentFragment"
private const val INITIAL_PAGE_INDEX = 0

/**
 * This fragment has a big [ImageView] that shows PDF pages, and 2 [Button]s to move between pages.
 * We use a [PdfRenderer] to render PDF pages as [Bitmap]s.
 */
@Suppress("DEPRECATION")
class ActionOpenDocumentFragment : Fragment() {
    private var mode = "pdf"
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var pdfRenderer: PdfRenderer
    private lateinit var currentPage: PdfRenderer.Page
    private var currentPageNumber: Int = INITIAL_PAGE_INDEX

    private lateinit var pdfPageView: ImageView
    private lateinit var previousButton: Button
    private lateinit var nextButton: Button

    //mine
    private lateinit var binding: FragmentPdfViewerBinding
    var pageCount = -1
    private var disableControls: Boolean = false

    //gestures variables
    private lateinit var mDetector: GestureDetectorCompat

    private lateinit var srcActivity: Activity


    companion object {
        private const val DOCUMENT_URI_ARGUMENT =
            "com.example.android.actionopendocument.args.DOCUMENT_URI_ARGUMENT"

        //for images rendering
        private lateinit var imagesToView: ArrayList<Bitmap>
        private var pageToView: Int = -1

        fun newInstance(
            documentUri: Uri,
            disableControls: Boolean = false,
            srcActivity: Activity?,
            mode: String?
        ): ActionOpenDocumentFragment {
            return ActionOpenDocumentFragment().apply {
                this.disableControls = disableControls
                if (srcActivity != null) {
                    this.srcActivity = srcActivity
                }
                if (mode != null) {
                    this.mode = mode
                }
                arguments = Bundle().apply {
                    putString(DOCUMENT_URI_ARGUMENT, documentUri.toString())
                }
            }
        }

        fun newInstanceForImages(
            images: ArrayList<Bitmap>,
            pageNumber: Int,
            srcActivity: Activity?
        ): ActionOpenDocumentFragment {
            return ActionOpenDocumentFragment().apply {
                this.mode = "images"
                imagesToView = images
                pageToView = pageNumber
                if (srcActivity != null) {
                    this.srcActivity = srcActivity
                }
                arguments = Bundle().apply {

                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val gesture = GestureDetector(
            activity,
            object : SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean {
                    return true
                }

                override fun onFling(
                    e1: MotionEvent, e2: MotionEvent, velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    Log.i(TAG, "onFling has been called!")
                    val SWIPE_MIN_DISTANCE = 120
                    val SWIPE_MAX_OFF_PATH = 250
                    val SWIPE_THRESHOLD_VELOCITY = 200
                    try {
                        if (Math.abs(e1.y - e2.y) > SWIPE_MAX_OFF_PATH) return false
                        if (e1.x - e2.x > SWIPE_MIN_DISTANCE
                            && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY
                        ) {
                            Log.i(TAG, "Right to Left")
                            if (mode == "images") {
                                pageCount = imagesToView.size

                                if (pageToView < pageCount)
                                    pageToView += 1
                                binding.image.setImageBitmap(imagesToView[pageToView])
                            } else
                                showPage(currentPage.index + 1)

                        } else if (e2.x - e1.x > SWIPE_MIN_DISTANCE
                            && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY
                        ) {
                            Log.i(TAG, "Left to Right")
                            if (mode == "images") {
                                pageCount = imagesToView.size

                                if (pageToView > 0)
                                    pageToView -= 1
                                binding.image.setImageBitmap(imagesToView[pageToView])
                            } else
                                showPage(currentPage.index - 1)

                        }
                    } catch (e: Exception) {
                        // nothing
                    }
                    return super.onFling(e1, e2, velocityX, velocityY)
                }
            })

        // Inflate the layout for this fragment
        binding = FragmentPdfViewerBinding.inflate(inflater, container, false)
        if (this.mode != "view") {
            if (this.mode == "images") {
                binding.ViewBtnsBar.visibility = View.GONE
                binding.imageViewBtnsBar.visibility = View.VISIBLE
                //
                binding.addImage.setOnClickListener {
                    //בדוק אם יש תמונות במערך התמונות
                    //הפוך מערך לקובץ פידיאף
                    //העלה לענן
                    dispatchTakePictureIntent()
                    pageToView = pageToView.plus(1)
                    pageCount = imagesToView.size
                    if (pageCount >= 0) {
                        binding.dropImage.isClickable = true
                    }
                }
                binding.dropImage.setOnClickListener {
                    //איפוס משתנים ומערך תמונות
                    //חזרה לדף הבית
                    imagesToView.removeAt(pageToView)
                    val imagesCount = imagesToView.size
                    if (imagesCount == 0) {
                        pageToView = -1
                        binding.image.setImageBitmap(null)
                        binding.dropImage.isClickable = false
                    } else {
                        try {
                            pageToView += 1
                            binding.image.setImageBitmap(imagesToView[pageToView])
                        } catch (e: IndexOutOfBoundsException) {
                            try {
                                pageToView -= 1
                                binding.image.setImageBitmap(imagesToView[pageToView])
                            } catch (e: IndexOutOfBoundsException) {
                                try {
                                    pageToView -= 1
                                    binding.image.setImageBitmap(imagesToView[pageToView])
                                } catch (e: IndexOutOfBoundsException) {
                                    pageToView = -1
                                    binding.image.setImageBitmap(null)
                                    binding.dropImage.isClickable = false
                                    binding.image.setImageBitmap(imagesToView[pageToView])
                                }
                            }
                        }
                    }
                }

                binding.cancelBtn.setOnClickListener {
                    imagesToView = arrayListOf()
                    pageToView = -1
                    val transaction: FragmentTransaction =
                        requireActivity().supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.container, AddInvoiceFragment())
                    transaction.addToBackStack(null)
                    transaction.commit()
                }


                binding.okBtn.setOnClickListener {
                    this.loadingDialog = LoadingDialog(srcActivity)
                    this.loadingDialog.startLoadingDialog("Your Invoice is Uploading To Database ...")
                    //הפיכת התמונות שבמערך לpdf
                    val document: PdfDocument = PdfDocument()

                    for (i in 0 until imagesToView.size) {
                        val im = imagesToView[i]
                        val pageInfo: PdfDocument.PageInfo =
                            PdfDocument.PageInfo.Builder(im.width, im.height, i + 1).create()
                        val page: PdfDocument.Page = document.startPage(pageInfo)
                        // Draw the bitmap onto the page
                        val canvas: Canvas = page.canvas
                        canvas.drawBitmap(im, 0f, 0f, null)
                        document.finishPage(page)
                    }
                    if (checkSelfPermission(
                            requireContext(),
                            WRITE_EXTERNAL_STORAGE
                        ) != PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            requireActivity(),
                            arrayOf(PERMISSION_GRANTED.toString()), 1
                        );
                    }
                    // Write the PDF file to a file
                    val directoryPath: String =
                        android.os.Environment.getExternalStorageDirectory().absolutePath.toString() + "/Documents"
                    val dir = directoryPath + "/${cur_user?.id}"
                    var myPath = File(dir)
                    myPath.mkdirs()
                    myPath = File(dir, "I${cur_user?.user_invoices?.count}.pdf")
                    document.writeTo(FileOutputStream(myPath))
                    document.close()

                    val storageRef = Firebase.storage.reference
                    val invoiceFilePath = newInvoice?.invoice_path

                    val newUserInvoiceRef = storageRef.child(invoiceFilePath!!)
                    val documentUri = Uri.fromFile(myPath)
                    val uploadTask = newUserInvoiceRef.putFile(documentUri)

                    // Register observers to listen for when the download is done or if it fails
                    uploadTask.addOnFailureListener {
                        // Handle unsuccessful uploads
                        loadingDialog.dismissLoadingDialog()
                        Toast.makeText(
                            context,
                            "Failed Invoice Uploading ❌ ...",
                            Toast.LENGTH_LONG
                        ).show()
                    }.addOnSuccessListener {
                        loadingDialog.dismissLoadingDialog()//סגירת דיאלוג טעינה
                        newInvoice = Invoice()//איפוס משתנים גלובליים

                        Toast.makeText(
                            context,
                            "Success Uploading \uD83E\uDDFE...",
                            Toast.LENGTH_LONG
                        ).show()

                        //חזרה למסך הבית לאחר שהחשבונית עלתה בהצלחה
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, WelcomeFragment())
                            .commit()
                    }
                }
            } else {
                if (disableControls) {
                    binding.ViewBtnsBar.visibility = View.GONE
                } else {
                    binding.okBtn.setOnClickListener {
                        this.loadingDialog = LoadingDialog(srcActivity)
                        this.loadingDialog.startLoadingDialog("Your Invoice is Uploading To Database ...")
                        
                        val documentUri = arguments?.getString(DOCUMENT_URI_ARGUMENT)?.toUri()

                        val storageRef = Firebase.storage.reference
                        val invoiceFilePath = newInvoice?.invoice_path

                        val newUserInvoiceRef = storageRef.child(invoiceFilePath!!)
                        val uploadTask = newUserInvoiceRef.putFile(documentUri!!)
                        // Register observers to listen for when the download is done or if it fails
                        uploadTask.addOnFailureListener {
                            // Handle unsuccessful uploads
                            this.loadingDialog.dismissLoadingDialog()
                            Toast.makeText(
                                context,
                                "Failed Invoice Uploading ❌ ...",
                                Toast.LENGTH_LONG
                            ).show()
                        }.addOnSuccessListener {
                            this.loadingDialog.dismissLoadingDialog()
                            newInvoice = Invoice()//איפוס משתנים גלובליים

                            //חזרה למסך הבית לאחר שהחשבונית עלתה בהצלחה
                            requireActivity().supportFragmentManager.beginTransaction()
                                .replace(R.id.fragmentContainer, WelcomeFragment())
                                .commit()
                        }

                    }

                    binding.cancelBtn.setOnClickListener {
                        val transaction: FragmentTransaction =
                            requireActivity().supportFragmentManager.beginTransaction()
                        transaction.replace(R.id.container, AddInvoiceFragment())
                        transaction.addToBackStack(null)
                        transaction.commit()
                    }
                }
            }
        }

        if (mode == "view") {
            binding.controlButtonsView.visibility = View.VISIBLE
            binding.controlButtonsAdd.visibility = View.GONE

            binding.back.setOnClickListener {
                requireActivity().supportFragmentManager.popBackStack()
            }
        }

        binding.root.setOnTouchListener { _, p1 -> gesture.onTouchEvent(p1); }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        pdfPageView = view.findViewById(R.id.image)

        previousButton = view.findViewById<Button>(R.id.previous).apply {
            setOnClickListener {
                showPage(currentPage.index - 1)
            }
        }
        nextButton = view.findViewById<Button>(R.id.next).apply {
            setOnClickListener {
                showPage(currentPage.index + 1)
            }
        }

        // If there is a savedInstanceState (screen orientations, etc.), we restore the page index.
        currentPageNumber = savedInstanceState?.getInt(CURRENT_PAGE_INDEX_KEY, INITIAL_PAGE_INDEX)
            ?: INITIAL_PAGE_INDEX
    }

    override fun onStart() {
        super.onStart()

        if (mode == "images") {
            if (imagesToView.isNotEmpty() && pageToView > -1) {
                try {
                    binding.image.setImageBitmap(imagesToView[pageToView])

                } catch (e: IOException) {
                    Log.d(TAG, "Exception opening document", e)
                }
            }
        } else {
            val documentUri = arguments?.getString(DOCUMENT_URI_ARGUMENT)?.toUri() ?: return
            try {
                openRenderer(activity, documentUri)

                for (i in 0 until pdfRenderer.pageCount) {
                    currentPage.close()
                    currentPage = pdfRenderer.openPage(i)

                    // Important: the destination bitmap must be ARGB (not RGB).
                    val bitmap =
                        createBitmap(currentPage.width, currentPage.height, Bitmap.Config.ARGB_8888)

                    currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    pdfPageView.setImageBitmap(bitmap)
                }
                showPage(currentPageNumber)

            } catch (ioException: IOException) {
                Log.d(TAG, "Exception opening document", ioException)
            }
        }

    }

    override fun onStop() {
        super.onStop()
        if (mode != "images") {
            try {
                closeRenderer()
            } catch (ioException: IOException) {
                Log.d(TAG, "Exception closing document", ioException)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (mode != "images")
            outState.putInt(CURRENT_PAGE_INDEX_KEY, currentPage.index)
        super.onSaveInstanceState(outState)
    }

    /**
     * Sets up a [PdfRenderer] and related resources.
     */
    @Throws(IOException::class)
    private fun openRenderer(context: Context?, documentUri: Uri) {
        if (context == null) return

        /**
         * It may be tempting to use `use` here, but [PdfRenderer] expects to take ownership
         * of the [FileDescriptor], and, if we did use `use`, it would be auto-closed at the
         * end of the block, preventing us from rendering additional pages.
         */
        val fileDescriptor = context.contentResolver.openFileDescriptor(documentUri, "r") ?: return

        // This is the PdfRenderer we use to render the PDF.
        pdfRenderer = PdfRenderer(fileDescriptor)
        currentPage = pdfRenderer.openPage(currentPageNumber)
    }

    /**
     * Closes the [PdfRenderer] and related resources.
     *
     * @throws IOException When the PDF file cannot be closed.
     */
    @Throws(IOException::class)
    private fun closeRenderer() {
        currentPage.close()
        pdfRenderer.close()
    }

    /**
     * Shows the specified page of PDF to the screen.
     *
     * The way [PdfRenderer] works is that it allows for "opening" a page with the method
     * [PdfRenderer.openPage], which takes a (0 based) page number to open. This returns
     * a [PdfRenderer.Page] object, which represents the content of this page.
     *
     * There are two ways to render the content of a [PdfRenderer.Page].
     * [PdfRenderer.Page.RENDER_MODE_FOR_PRINT] and [PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY].
     * Since we're displaying the data on the screen of the device, we'll use the later.
     *
     * @param index The page index.
     */
    private fun showPage(index: Int) {
        if (index < 0 || index >= pdfRenderer.pageCount) return

        currentPage.close()
        currentPage = pdfRenderer.openPage(index)

        // Important: the destination bitmap must be ARGB (not RGB).
        val bitmap = createBitmap(currentPage.width, currentPage.height, Bitmap.Config.ARGB_8888)

        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        pdfPageView.setImageBitmap(bitmap)

        val pageCount = pdfRenderer.pageCount
        previousButton.isEnabled = (0 != index)
        nextButton.isEnabled = (index + 1 < pageCount)
        activity?.title = getString(R.string.app_name_with_index, index + 1, pageCount)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                OPEN_CAMERA_REQUEST_CODE -> {
                    val imageBitmap = resultData?.extras?.get("data") as Bitmap
                    imagesToView.add(imageBitmap)
                    if (imagesToView.isNotEmpty() && pageToView > -1) {
                        try {
                            binding.image.setImageBitmap(imagesToView[pageToView])

                        } catch (e: IOException) {
                            Log.d(TAG, "Exception opening document", e)
                        }
                    }
                }
            }
        }
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


