package com.example.gym

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.gym.databinding.ActivityProductCrudBinding
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.*

class ProductCrudFragment : Fragment() {

    private var _binding: ActivityProductCrudBinding? = null
    private val binding get() = _binding!!

    private val urlWebService = "http://192.168.1.6/mobile/process_products.php"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityProductCrudBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Memuat data default produk pertama kali dibuka
        loadProductsData("")

        // Listener Real-time Search Bar Produk
        binding.edSearchProduct.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                loadProductsData(s.toString().trim())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // TRIGGER MODAL INSERT: Klik tombol FAB bulat merah untuk tambah data baru
        binding.fabAddProduct.setOnClickListener {
            showProductModal(null)
        }
    }

    private fun loadProductsData(searchKeyword: String) {
        val request = object : StringRequest(Method.POST, urlWebService,
            { response ->
                try {
                    binding.containerProducts.removeAllViews()
                    val jsonArray = JSONArray(response)
                    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
                        maximumFractionDigits = 0
                    }

                    val inflaterRow = LayoutInflater.from(requireActivity())

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val id = obj.getString("id")
                        val name = obj.getString("name")

                        // Ganti 'this' menjadi 'requireActivity()'
                        val row = inflaterRow.inflate(R.layout.item_product_row, binding.containerProducts, false)
                        row.findViewById<TextView>(R.id.txtProdName).text = name
                        row.findViewById<TextView>(R.id.txtProdCatBrand).text = "${obj.getString("category")} • ${obj.optString("brand", "-")}"
                        row.findViewById<TextView>(R.id.txtProdStock).text = "Stok: ${obj.getString("stock")} ${obj.getString("unit")}"
                        row.findViewById<TextView>(R.id.txtProdPrice).text = currencyFormat.format(obj.getLong("price"))

                        // TRIGGER MODAL EDIT
                        row.findViewById<TextView>(R.id.btnProdEdit).setOnClickListener {
                            showProductModal(obj)
                        }

                        // VALIDASI SEBELUM HAPUS: Menampilkan Dialog Konfirmasi bergaya Dark
                        row.findViewById<TextView>(R.id.btnProdDelete).setOnClickListener {
                            AlertDialog.Builder(requireActivity())
                                .setTitle("Hapus Produk")
                                .setMessage("Apakah Anda yakin ingin menghapus produk '$name'?")
                                .setPositiveButton("Hapus") { dialog, _ ->
                                    deleteProductData(id)
                                    dialog.dismiss()
                                }
                                .setNegativeButton("Batal") { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .show()
                        }

                        binding.containerProducts.addView(row)
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            { Toast.makeText(requireActivity(), "Koneksi server gagal", Toast.LENGTH_SHORT).show() }
        ) {
            override fun getParams(): Map<String, String> = mapOf("mode" to "show", "search" to searchKeyword)
        }
        Volley.newRequestQueue(requireActivity()).add(request)
    }

    // FUNGSI RENDER MODAL POP-UP DINAMIS (INSERT / UPDATE)
    private fun showProductModal(productObject: JSONObject?) {
        val dialogView = LayoutInflater.from(requireActivity()).inflate(R.layout.dialog_add_product, null)

        val txtTitle = dialogView.findViewById<TextView>(R.id.txtDialogTitle)
        val edName = dialogView.findViewById<EditText>(R.id.dialogProductName)
        val edCategory = dialogView.findViewById<EditText>(R.id.dialogProductCategory)
        val edPrice = dialogView.findViewById<EditText>(R.id.dialogProductPrice)
        val edStock = dialogView.findViewById<EditText>(R.id.dialogProductStock)
        val edUnit = dialogView.findViewById<EditText>(R.id.dialogProductUnit)
        val edBrand = dialogView.findViewById<EditText>(R.id.dialogProductBrand)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnDialogCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnDialogSave)

        val alertDialog = AlertDialog.Builder(requireActivity())
            .setView(dialogView)
            .create()

        alertDialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        if (productObject != null) {
            txtTitle.text = "Edit Detail Produk"
            edName.setText(productObject.getString("name"))
            edCategory.setText(productObject.getString("category"))
            edPrice.setText(productObject.getString("price"))
            edStock.setText(productObject.getString("stock"))
            edUnit.setText(productObject.getString("unit"))
            edBrand.setText(productObject.optString("brand", ""))
            btnSave.text = "PERBARUI"
        }

        btnCancel.setOnClickListener { alertDialog.dismiss() }

        btnSave.setOnClickListener {
            val name = edName.text.toString().trim()
            if (name.isEmpty()) {
                edName.error = "Nama produk wajib diisi!"
                return@setOnClickListener
            }

            val params = HashMap<String, String>()
            params["name"] = name
            params["category"] = edCategory.text.toString().trim()
            params["price"] = edPrice.text.toString().trim().ifEmpty { "0" }
            params["stock"] = edStock.text.toString().trim().ifEmpty { "0" }
            params["unit"] = edUnit.text.toString().trim().ifEmpty { "pcs" }
            params["brand"] = edBrand.text.toString().trim()

            if (productObject != null) {
                params["mode"] = "update"
                params["id"] = productObject.getString("id")
            } else {
                params["mode"] = "insert"
            }

            val request = object : StringRequest(Method.POST, urlWebService,
                { response ->
                    val res = JSONObject(response)
                    Toast.makeText(requireActivity(), res.getString("message"), Toast.LENGTH_SHORT).show()
                    if (res.getString("status") == "success") {
                        alertDialog.dismiss()
                        loadProductsData("")
                    }
                },
                { Toast.makeText(requireActivity(), "Gagal memproses ke server", Toast.LENGTH_SHORT).show() }
            ) {
                override fun getParams(): Map<String, String> = params
            }
            Volley.newRequestQueue(requireActivity()).add(request)
        }

        alertDialog.show()
    }

    private fun deleteProductData(productId: String) {
        val request = object : StringRequest(Method.POST, urlWebService,
            { response ->
                val res = JSONObject(response)
                Toast.makeText(requireActivity(), res.getString("message"), Toast.LENGTH_SHORT).show()
                loadProductsData("")
            },
            { Toast.makeText(requireActivity(), "Gagal menghapus", Toast.LENGTH_SHORT).show() }
        ) {
            override fun getParams(): Map<String, String> = mapOf("mode" to "delete", "id" to productId)
        }
        Volley.newRequestQueue(requireActivity()).add(request)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}