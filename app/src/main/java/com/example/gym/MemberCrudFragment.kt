package com.example.gym

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.example.gym.databinding.ActivityMemberCrudBinding
import com.example.gym.databinding.ItemMemberRowBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.google.zxing.BarcodeFormat
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class MemberCrudFragment : Fragment() {

    private var _binding: ActivityMemberCrudBinding? = null
    private val binding get() = _binding!!

    private var imStr = ""
    private val urlWebService = "http://192.168.1.6/mobile/crud_gym_members.php"

    private var currentModalDialog: BottomSheetDialog? = null
    private var modalImageViewPointer: ShapeableImageView? = null
    private var currentStatusFilter = "all"

    private val REQ_CAMERA = 100
    private val REQ_GALLERY = 101

    private var imageUri: Uri? = null
    private var cameraFile: File? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityMemberCrudBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchParentView = binding.edSearch.parent as? View
        searchParentView?.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#1E1E1E"))
            cornerRadius = 100f
        }

        setupRadioButtonsStyle()
        showMembersData("", "all")

        binding.edSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                showMembersData(s.toString().trim(), currentStatusFilter)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.rgStatusFilter.setOnCheckedChangeListener { _, checkedId ->
            setupRadioButtonsStyle()
            currentStatusFilter = when (checkedId) {
                R.id.rbActive -> "active"
                R.id.rbInactive -> "inactive"
                else -> "all"
            }
            showMembersData(binding.edSearch.text.toString().trim(), currentStatusFilter)
        }

        binding.fabAddMember.setOnClickListener {
            openMemberModal(null)
        }
    }

    private fun setupRadioButtonsStyle() {
        try {
            for (i in 0 until binding.rgStatusFilter.childCount) {
                val view = binding.rgStatusFilter.getChildAt(i)
                if (view is RadioButton) {
                    if (view.isChecked) {
                        view.background = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            setColor(Color.parseColor("#FF1E27"))
                            cornerRadius = 100f
                        }
                        view.setTextColor(Color.parseColor("#FFFFFF"))
                        view.setTypeface(null, Typeface.BOLD)
                    } else {
                        view.background = null
                        view.setTextColor(Color.parseColor("#888888"))
                        view.setTypeface(null, Typeface.NORMAL)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showQrCodeDialog(memberId: String, name: String, phone: String, expired: String) {
        val qrDialog = BottomSheetDialog(requireActivity())
        val view = layoutInflater.inflate(R.layout.layout_modal_member, null)
        qrDialog.setContentView(view)

        val txtModalTitle = view.findViewById<TextView>(R.id.txtModalTitle)
        val modalImUpload = view.findViewById<ShapeableImageView>(R.id.modalImUpload)
        val ivQrCode = view.findViewById<ImageView>(R.id.modalIvQrCode)
        val btnSubmit = view.findViewById<Button>(R.id.modalBtnSubmit)

        modalImUpload.visibility = View.GONE

        view.findViewById<View>(R.id.modalEdMemberId)?.visibility = View.GONE
        view.findViewById<View>(R.id.modalEdFullName)?.parent?.let { (it as View).visibility = View.GONE }
        view.findViewById<View>(R.id.modalEdEmail)?.parent?.let { (it as View).visibility = View.GONE }
        view.findViewById<View>(R.id.modalEdPhone)?.parent?.let { (it as View).visibility = View.GONE }
        view.findViewById<View>(R.id.modalLayoutExpired)?.visibility = View.GONE
        view.findViewById<View>(R.id.modalBtnRenew)?.visibility = View.GONE
        view.findViewById<View>(R.id.modalBtnDelete)?.visibility = View.GONE

        txtModalTitle.text = """
            IDENTITAS ATLET ARENA GYM
            
            ID : $memberId
            Nama : $name
            Telepon : $phone
            Status : $expired
        """.trimIndent()

        txtModalTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        txtModalTitle.setLineSpacing(4f, 1.1f)

        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(memberId, BarcodeFormat.QR_CODE, 550, 550)
            ivQrCode.setImageBitmap(bitmap)
            ivQrCode.visibility = View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireActivity(), "Gagal me-render matriks QR Code", Toast.LENGTH_SHORT).show()
        }

        btnSubmit.text = "KEMBALI KE DAFTAR"
        btnSubmit.setOnClickListener {
            qrDialog.dismiss()
        }

        qrDialog.show()
    }

    private fun openMemberModal(selectedData: JSONObject?) {
        val dialog = BottomSheetDialog(requireActivity())
        val view = layoutInflater.inflate(R.layout.layout_modal_member, null)
        dialog.setContentView(view)

        currentModalDialog = dialog

        val txtModalTitle = view.findViewById<TextView>(R.id.txtModalTitle)
        val modalImUpload = view.findViewById<ShapeableImageView>(R.id.modalImUpload)
        val edId = view.findViewById<android.widget.EditText>(R.id.modalEdMemberId)
        val edName = view.findViewById<TextInputEditText>(R.id.modalEdFullName)
        val edEmail = view.findViewById<TextInputEditText>(R.id.modalEdEmail)
        val edPhone = view.findViewById<TextInputEditText>(R.id.modalEdPhone)

        val layoutExpired = view.findViewById<TextInputLayout>(R.id.modalLayoutExpired)
        val edExpiredAt = view.findViewById<TextInputEditText>(R.id.modalEdExpiredAt)
        val ivQrCode = view.findViewById<ImageView>(R.id.modalIvQrCode)

        val btnSubmit = view.findViewById<Button>(R.id.modalBtnSubmit)
        val btnRenew = view.findViewById<Button>(R.id.modalBtnRenew)
        val btnDelete = view.findViewById<Button>(R.id.modalBtnDelete)

        modalImageViewPointer = modalImUpload
        imStr = ""

        ivQrCode.visibility = View.GONE

        modalImUpload.setOnClickListener {
            showImageSourceChooser()
        }

        if (selectedData == null) {
            txtModalTitle.text = "Tambah Member Baru"
            btnSubmit.text = "Tambah Member"
            layoutExpired.visibility = View.GONE
            btnRenew.visibility = View.GONE
            btnDelete.visibility = View.GONE

            btnSubmit.setOnClickListener {
                val name = edName.text.toString().trim()
                if (name.isEmpty()) {
                    edName.error = "Nama wajib diisi!"
                } else {
                    executeCrud("insert", "", name, edEmail.text.toString().trim(), edPhone.text.toString().trim())
                }
            }
        } else {
            txtModalTitle.text = "Edit Data Member"
            btnSubmit.text = "SIMPAN"
            layoutExpired.visibility = View.VISIBLE
            btnRenew.visibility = View.VISIBLE
            btnDelete.visibility = View.VISIBLE

            val id = selectedData.optString("id", "")
            val name = selectedData.optString("full_name", "")
            val email = selectedData.optString("email", "")
            val phone = selectedData.optString("phone", "")
            val photoUrl = selectedData.optString("profile_photo_path", "")
            val expiresAt = selectedData.optString("expires_at", "-")

            edId.setText(id)
            edName.setText(name)
            edEmail.setText(if (email == "null" || email.isEmpty()) "" else email)
            edPhone.setText(if (phone == "null" || phone.isEmpty()) "" else phone)
            edExpiredAt.setText(if (expiresAt == "null" || expiresAt.isEmpty()) "Belum Diatur" else expiresAt)

            val imageSource = if (photoUrl != "null" && photoUrl.isNotEmpty()) photoUrl else "https://ui-avatars.com/api/?name=$name&background=333333&color=ffffff"
            Glide.with(requireActivity()).load(imageSource).circleCrop().into(modalImUpload)

            btnSubmit.setOnClickListener {
                val uName = edName.text.toString().trim()
                if (uName.isEmpty()) {
                    edName.error = "Nama wajib diisi!"
                } else {
                    executeCrud("update", id, uName, edEmail.text.toString().trim(), edPhone.text.toString().trim())
                }
            }

            btnRenew.setOnClickListener {
                AlertDialog.Builder(requireActivity())
                    .setTitle("Perpanjang Masa Aktif")
                    .setMessage("Perpanjang masa aktif member $name selama 1 bulan? Sisa hari aktif otomatis diakumulasikan.")
                    .setPositiveButton("YA, PERPANJANG") { dInterface, _ ->
                        executeCrud("renew", id, "", "", "")
                        dInterface.dismiss()
                    }
                    .setNegativeButton("BATAL") { dInterface, _ -> dInterface.dismiss() }
                    .create().apply {
                        setOnShowListener {
                            getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#FF1E27"))
                            getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#888888"))
                        }
                    }.show()
            }

            btnDelete.setOnClickListener {
                AlertDialog.Builder(requireActivity())
                    .setTitle("Hapus Member")
                    .setMessage("Apakah Anda yakin ingin menghapus Member bernama $name ?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("YA, HAPUS") { dialogInterface, _ ->
                        executeCrud("delete", id, "", "", "")
                        dialogInterface.dismiss()
                    }
                    .setNegativeButton("BATAL") { dialogInterface, _ -> dialogInterface.dismiss() }
                    .create().apply {
                        setOnShowListener {
                            getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#FF1E27"))
                            getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#888888"))
                        }
                    }.show()
            }
        }

        dialog.show()
    }

    private fun showImageSourceChooser() {
        val options = arrayOf("Ambil Foto dari Kamera", "Pilih dari Galeri")
        AlertDialog.Builder(requireActivity())
            .setTitle("Sumber Foto Profil Atlet")
            .setItems(options) { dialogInterface, which ->
                when (which) {
                    0 -> {
                        try {
                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            cameraFile = File.createTempFile("IMG_${timeStamp}_", ".jpg", requireActivity().cacheDir)

                            imageUri = FileProvider.getUriForFile(
                                requireActivity(),
                                "${requireActivity().packageName}.fileprovider",
                                cameraFile!!
                            )

                            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                                putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                            }
                            @Suppress("DEPRECATION")
                            startActivityForResult(intent, REQ_CAMERA)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(requireActivity(), "Gagal meluncurkan hardware kamera", Toast.LENGTH_SHORT).show()
                        }
                    }
                    1 -> {
                        val intent = Intent().apply {
                            type = "image/*"
                            action = Intent.ACTION_GET_CONTENT
                        }
                        @Suppress("DEPRECATION")
                        startActivityForResult(Intent.createChooser(intent, "Pilih Foto Atlet"), REQ_GALLERY)
                    }
                }
                dialogInterface.dismiss()
            }
            .create()
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            modalImageViewPointer?.let { imageView ->
                if (requestCode == REQ_GALLERY) {
                    data?.data?.let { uri ->
                        try {
                            Glide.with(requireActivity()).load(uri).circleCrop().into(imageView)
                            val inputStream: InputStream? = requireActivity().contentResolver.openInputStream(uri)
                            val bitmap = BitmapFactory.decodeStream(inputStream)
                            val bytes = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bytes)
                            imStr = Base64.encodeToString(bytes.toByteArray(), Base64.DEFAULT)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else if (requestCode == REQ_CAMERA) {
                    try {
                        val fileToRead = cameraFile
                        if (fileToRead != null && fileToRead.exists()) {
                            Glide.with(requireActivity()).load(fileToRead).circleCrop().into(imageView)

                            val bitmap = BitmapFactory.decodeFile(fileToRead.absolutePath)
                            val bytes = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bytes)

                            imStr = Base64.encodeToString(bytes.toByteArray(), Base64.DEFAULT)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(requireActivity(), "Gagal memproses gambar kamera", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showMembersData(searchKeyword: String, statusFilter: String) {
        val request = object : StringRequest(
            Request.Method.POST, urlWebService,
            { response ->
                Log.d("DEBUG_VOLLEY", "JSON Response: $response")
                try {
                    val jsonArray = JSONArray(response)
                    binding.containerMembersList.removeAllViews()
                    val inflaterRow = LayoutInflater.from(requireActivity())

                    val sdfFull = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val sdfDateOnly = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val currentDate = Date()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)

                        val id = obj.optString("id", "")
                        val name = obj.optString("full_name", "-")
                        val phone = obj.optString("phone", "-")
                        val photoUrl = obj.optString("profile_photo_path", "")
                        val expiresAt = obj.optString("expires_at", "")

                        val rowBinding = ItemMemberRowBinding.inflate(inflaterRow, binding.containerMembersList, false)

                        rowBinding.txtRowName.text = name
                        rowBinding.txtRowPhone.text = if (phone == "null" || phone.isEmpty()) "-" else phone

                        val expString = if (expiresAt == "null" || expiresAt.isEmpty()) "Belum Diatur" else expiresAt
                        rowBinding.txtRowExpired.text = "Expired: $expString"

                        val imageSource = if (photoUrl != "null" && photoUrl.isNotEmpty()) photoUrl else "https://ui-avatars.com/api/?name=$name&background=333333&color=ffffff"
                        Glide.with(requireActivity()).load(imageSource).circleCrop().into(rowBinding.imgAthlete)

                        val dotDrawable = GradientDrawable().apply { shape = GradientDrawable.OVAL }

                        // PEWARNAAN 3 KONDISI DINAMIS (ACTIVE, WARNING TINGGAL 3 HARI, EXPIRED)
                        var memberStatus = "EXPIRED" // Default fallback
                        if (expiresAt.isNotEmpty() && expiresAt != "null") {
                            try {
                                val expiryDate = if (expiresAt.contains(":")) sdfFull.parse(expiresAt) else sdfDateOnly.parse(expiresAt)
                                if (expiryDate != null) {
                                    val diffInMillies = expiryDate.time - currentDate.time
                                    val diffInDays = diffInMillies / (1000 * 60 * 60 * 24)

                                    memberStatus = when {
                                        diffInMillies <= 0 -> "EXPIRED"
                                        diffInDays <= 3 -> "CRITICAL" // Sisa waktu <= 3 hari (Kuning)
                                        else -> "ACTIVE"              // Lebih dari 3 hari (Hijau)
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                memberStatus = "EXPIRED"
                            }
                        }

                        // Mengatur gaya warna row layout berdasarkan status kalkulasi hari di atas
                        when (memberStatus) {
                            "ACTIVE" -> {
                                rowBinding.txtRowStatus.text = "ACTIVE"
                                dotDrawable.setColor(Color.parseColor("#00E676")) // Hijau Cerah
                                rowBinding.cardStatusBadge.setCardBackgroundColor(Color.parseColor("#0A2F1D")) // Hijau Gelap
                                rowBinding.txtRowStatus.setTextColor(Color.parseColor("#00E676"))
                                rowBinding.txtRowExpired.setTextColor(Color.parseColor("#00E676"))
                            }
                            "CRITICAL" -> {
                                rowBinding.txtRowStatus.text = "WARNING"
                                dotDrawable.setColor(Color.parseColor("#FFD600")) // Kuning Cerah
                                rowBinding.cardStatusBadge.setCardBackgroundColor(Color.parseColor("#3A3200")) // Kuning Gelap
                                rowBinding.txtRowStatus.setTextColor(Color.parseColor("#FFD600"))
                                rowBinding.txtRowExpired.setTextColor(Color.parseColor("#FFD600"))
                            }
                            "EXPIRED" -> {
                                rowBinding.txtRowStatus.text = "EXPIRED"
                                dotDrawable.setColor(Color.parseColor("#FF1E27")) // Merah Cerah
                                rowBinding.cardStatusBadge.setCardBackgroundColor(Color.parseColor("#331415")) // Merah Gelap
                                rowBinding.txtRowStatus.setTextColor(Color.parseColor("#FF1E27"))
                                rowBinding.txtRowExpired.setTextColor(Color.parseColor("#FF1E27"))
                            }
                        }
                        rowBinding.viewStatusDot.background = dotDrawable

                        rowBinding.root.setOnClickListener {
                            openMemberModal(obj)
                        }

                        rowBinding.root.setOnLongClickListener {
                            val options = arrayOf("Tampilkan QR Code Member", "Edit Detail Atlet")
                            AlertDialog.Builder(requireActivity())
                                .setTitle("Aksi Member: $name")
                                .setItems(options) { dialogInterface, which ->
                                    when (which) {
                                        0 -> showQrCodeDialog(id, name, rowBinding.txtRowPhone.text.toString(), rowBinding.txtRowExpired.text.toString())
                                        1 -> openMemberModal(obj)
                                    }
                                    dialogInterface.dismiss()
                                }
                                .create().show()
                            true
                        }

                        binding.containerMembersList.addView(rowBinding.root)
                    }
                } catch (e: Exception) {
                    Log.e("DEBUG_GYM_ERROR", "Gagal memproses JSON: ${e.message}")
                    e.printStackTrace()
                }
            },
            { Toast.makeText(requireActivity(), "Gagal sinkronisasi data server", Toast.LENGTH_SHORT).show() }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["mode"] = "show"
                params["search"] = searchKeyword
                params["status_filter"] = statusFilter
                return params
            }
        }
        Volley.newRequestQueue(requireActivity()).add(request)
    }

    private fun executeCrud(mode: String, id: String, name: String, email: String, phone: String) {
        val request = object : StringRequest(
            Request.Method.POST, urlWebService,
            { response ->
                Log.d("DEBUG_GYM", "Respons Server: $response")
                try {
                    val jsonObject = JSONObject(response)
                    Toast.makeText(requireActivity(), jsonObject.getString("message"), Toast.LENGTH_LONG).show()

                    if (jsonObject.optString("status") != "error") {
                        currentModalDialog?.dismiss()
                        binding.edSearch.setText("")
                        binding.rbAll.isChecked = true
                        showMembersData("", "all")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireActivity(), "Format respon bermasalah", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(requireActivity(), "Gangguan koneksi sistem server Volley", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val fileName = "IMG_" + SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date()) + ".jpg"
                return hashMapOf(
                    "mode" to mode,
                    "id" to id,
                    "full_name" to name,
                    "email" to email,
                    "phone" to phone,
                    "image" to imStr,
                    "file" to if (imStr.isEmpty()) "" else fileName
                )
            }
        }

        request.retryPolicy = DefaultRetryPolicy(
            20000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        Volley.newRequestQueue(requireActivity()).add(request)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}