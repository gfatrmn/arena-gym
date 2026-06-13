package com.example.gym

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class MemberCrudActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMemberCrudBinding
    private var imStr = ""
    private val urlWebService = "http://192.168.1.6/mobile/crud_gym_members.php"

    private var currentModalDialog: BottomSheetDialog? = null
    private var modalImageViewPointer: ShapeableImageView? = null
    private var currentStatusFilter = "all"

    // Request Code internal mandiri tanpa MediaHelper
    private val REQ_CAMERA = 100
    private val REQ_GALLERY = 101

    private var imageUri: Uri? = null
    private var cameraFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemberCrudBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set Kapsul Search Bar
        val searchParentView = binding.edSearch.parent as? View
        searchParentView?.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#1E1E1E"))
            cornerRadius = 100f
        }

        // Set Kapsul Radio Button
        for (i in 0 until binding.rgStatusFilter.childCount) {
            val rb = binding.rgStatusFilter.getChildAt(i) as RadioButton
            rb.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 100f
            }
        }

        // Pastikan indikator ikon Members menyala saat halaman ini aktif
        binding.bottomNavigation.selectedItemId = R.id.nav_members

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }

                R.id.nav_products -> {
                    startActivity(Intent(this, ProductCrudActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }

                // JALUR NAVIGASI BARU: Tambahkan ini agar tombol Check-In bisa merespon!
                R.id.nav_checkin -> {
                    startActivity(Intent(this, CheckInActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }

                R.id.nav_reports -> {
                    startActivity(Intent(this, ReportActivity::class.java))
                    overridePendingTransition(0, 0); finish(); true
                }

                R.id.nav_members -> true // Tetap di halaman kelola member
                else -> false
            }
        }

        showMembersData("", "all")

        binding.edSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                showMembersData(s.toString().trim(), currentStatusFilter)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.rgStatusFilter.setOnCheckedChangeListener { _, checkedId ->
            for (i in 0 until binding.rgStatusFilter.childCount) {
                val rb = binding.rgStatusFilter.getChildAt(i) as RadioButton
                rb.setTextColor(Color.parseColor("#888888"))
                rb.setTypeface(null, android.graphics.Typeface.NORMAL)
            }

            val activeRb = findViewById<RadioButton>(checkedId)
            activeRb.setTextColor(Color.parseColor("#FFFFFF"))
            activeRb.setTypeface(null, android.graphics.Typeface.BOLD)

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

    private fun openMemberModal(selectedData: JSONObject?) {
        val dialog = BottomSheetDialog(this)
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

        val btnSubmit = view.findViewById<Button>(R.id.modalBtnSubmit)
        val btnRenew = view.findViewById<Button>(R.id.modalBtnRenew)
        val btnDelete = view.findViewById<Button>(R.id.modalBtnDelete)

        modalImageViewPointer = modalImUpload
        imStr = ""

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
            val photoUrl = selectedData.optString("photo_url", "")
            val expiresAt = selectedData.optString("expires_at", "-")

            edId.setText(id)
            edName.setText(name)
            edEmail.setText(if (email == "null" || email.isEmpty()) "" else email)
            edPhone.setText(if (phone == "null" || phone.isEmpty()) "" else phone)
            edExpiredAt.setText(if (expiresAt == "null" || expiresAt.isEmpty()) "Belum Diatur" else expiresAt)

            val imageSource = if (photoUrl != "null" && photoUrl.isNotEmpty()) photoUrl else "https://ui-avatars.com/api/?name=$name&background=333333&color=ffffff"
            Glide.with(this).load(imageSource).circleCrop().into(modalImUpload)

            btnSubmit.setOnClickListener {
                val uName = edName.text.toString().trim()
                if (uName.isEmpty()) {
                    edName.error = "Nama wajib diisi!"
                } else {
                    executeCrud("update", id, uName, edEmail.text.toString().trim(), edPhone.text.toString().trim())
                }
            }

            btnRenew.setOnClickListener {
                AlertDialog.Builder(this)
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
                AlertDialog.Builder(this)
                    .setTitle("Hapus Member")
                    .setMessage("Apakah Anda yakin ingin menghapus Member bernama $name ?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("YA, HAPUS") { dialogInterface, _ ->
                        executeCrud("delete", id, "", "", "")
                        dialogInterface.dismiss()
                    }
                    .setNegativeButton("BATAL") { dialogInterface, _ ->
                        dialogInterface.dismiss()
                    }
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
        AlertDialog.Builder(this)
            .setTitle("Sumber Foto Profil Atlet")
            .setItems(options) { dialogInterface, which ->
                when (which) {
                    0 -> {
                        try {
                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            cameraFile = File.createTempFile("IMG_${timeStamp}_", ".jpg", cacheDir)

                            imageUri = FileProvider.getUriForFile(
                                this,
                                "$packageName.fileprovider",
                                cameraFile!!
                            )

                            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                                putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                            }
                            @SuppressWarnings("deprecation")
                            startActivityForResult(intent, REQ_CAMERA)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(this, "Gagal meluncurkan hardware kamera", Toast.LENGTH_SHORT).show()
                        }
                    }
                    1 -> {
                        val intent = Intent().apply {
                            type = "image/*"
                            action = Intent.ACTION_GET_CONTENT
                        }
                        @SuppressWarnings("deprecation")
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
        @SuppressWarnings("deprecation")
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            modalImageViewPointer?.let { imageView ->
                if (requestCode == REQ_GALLERY) {
                    data?.data?.let { uri ->
                        try {
                            Glide.with(this).load(uri).circleCrop().into(imageView)
                            val inputStream: InputStream? = contentResolver.openInputStream(uri)
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
                            Glide.with(this).load(fileToRead).circleCrop().into(imageView)

                            val bitmap = BitmapFactory.decodeFile(fileToRead.absolutePath)
                            val bytes = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bytes)

                            imStr = Base64.encodeToString(bytes.toByteArray(), Base64.DEFAULT)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this, "Gagal memproses gambar kamera", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showMembersData(searchKeyword: String, statusFilter: String) {
        val request = object : StringRequest(
            Request.Method.POST, urlWebService,
            { response ->
                try {
                    val jsonArray = JSONArray(response)
                    binding.containerMembersList.removeAllViews()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)

                        val name = obj.optString("full_name", "-")
                        val phone = obj.optString("phone", "-")
                        val status = obj.optString("status", "member").lowercase()
                        val photoUrl = obj.optString("photo_url", "")

                        // 1. Ambil data tanggal expires_at dari response server Laragon
                        val expiresAt = obj.optString("expires_at", "-")

                        val rowBinding = ItemMemberRowBinding.inflate(LayoutInflater.from(this), binding.containerMembersList, false)
                        rowBinding.txtRowName.text = name
                        rowBinding.txtRowPhone.text = if (phone == "null" || phone.isEmpty()) "-" else phone
                        rowBinding.txtRowStatus.text = status.uppercase()

                        // 2. Tempelkan teks masa berakhir anggota ke layout list row
                        rowBinding.txtRowExpired.text = if (expiresAt == "null" || expiresAt.isEmpty()) {
                            "Masa Aktif: Belum Diatur"
                        } else {
                            "Expired: $expiresAt"
                        }

                        val imageSource = if (photoUrl != "null" && photoUrl.isNotEmpty()) photoUrl else "https://ui-avatars.com/api/?name=$name&background=333333&color=ffffff"
                        Glide.with(this).load(imageSource).circleCrop().into(rowBinding.imgAthlete)

                        val dotDrawable = GradientDrawable().apply { shape = GradientDrawable.OVAL }
                        if (status == "active" || status == "aktif" || status == "member") {
                            dotDrawable.setColor(Color.parseColor("#FF1E27"))
                            rowBinding.cardStatusBadge.setCardBackgroundColor(Color.parseColor("#331415"))
                            rowBinding.txtRowStatus.setTextColor(Color.parseColor("#FF1E27"))
                        } else {
                            dotDrawable.setColor(Color.parseColor("#555555"))
                            rowBinding.cardStatusBadge.setCardBackgroundColor(Color.parseColor("#222222"))
                            rowBinding.txtRowStatus.setTextColor(Color.parseColor("#888888"))
                        }
                        rowBinding.viewStatusDot.background = dotDrawable

                        rowBinding.root.setOnClickListener {
                            openMemberModal(obj)
                        }

                        binding.containerMembersList.addView(rowBinding.root)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { Toast.makeText(this, "Gagal sinkronisasi data server", Toast.LENGTH_SHORT).show() }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["mode"] = "show"
                params["search"] = searchKeyword
                params["status_filter"] = statusFilter
                return params
            }
        }
        Volley.newRequestQueue(this).add(request)
    }

    private fun executeCrud(mode: String, id: String, name: String, email: String, phone: String) {
        val request = object : StringRequest(
            Request.Method.POST, urlWebService,
            { response ->
                Log.d("DEBUG_GYM", "Respons Server: $response")
                try {
                    val jsonObject = JSONObject(response)
                    Toast.makeText(this, jsonObject.getString("message"), Toast.LENGTH_LONG).show()

                    if (jsonObject.optString("status") != "error") {
                        currentModalDialog?.dismiss()
                        binding.edSearch.setText("")
                        binding.rbAll.isChecked = true
                        showMembersData("", "all")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Format respon bermasalah", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(this, "Gangguan koneksi sistem server Volley", Toast.LENGTH_SHORT).show()
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

        Volley.newRequestQueue(this).add(request)
    }
}