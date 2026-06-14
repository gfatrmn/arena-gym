package com.example.gym

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
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
import android.graphics.Color
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

        loadProductsData("")

        binding.edSearchProduct.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                loadProductsData(s.toString().trim())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

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
                        val id = obj.optString("id", "")
                        val name = obj.optString("name", "-")
                        val stockVal = obj.optInt("stock", 0)
                        val unitStr = obj.optString("unit", "pcs")

                        val row = inflaterRow.inflate(R.layout.item_product_row, binding.containerProducts, false)
                        row.findViewById<TextView>(R.id.txtProdName).text = name
                        row.findViewById<TextView>(R.id.txtProdCatBrand).text = "${obj.optString("category", "-")} • ${obj.optString("brand", "-")}"
                        row.findViewById<TextView>(R.id.txtProdPrice).text = currencyFormat.format(obj.optLong("price", 0))

                        // FIX: KONDISI WARNA KUNING UNTUK STOK < 5
                        val txtStock = row.findViewById<TextView>(R.id.txtProdStock)
                        txtStock.text = "Stok: $stockVal $unitStr"
                        if (stockVal < 5) {
                            txtStock.setTextColor(Color.parseColor("#FFD600")) // Kuning Warning
                        } else {
                            txtStock.setTextColor(Color.parseColor("#FFFFFF")) // Putih Normal
                        }

                        row.findViewById<TextView>(R.id.btnProdEdit).setOnClickListener {
                            showProductModal(obj)
                        }

                        row.findViewById<TextView>(R.id.btnProdDelete).setOnClickListener {
                            AlertDialog.Builder(requireActivity())
                                .setTitle("Hapus Produk")
                                .setMessage("Apakah Anda yakin ingin menghapus produk '$name'?")
                                .setPositiveButton("Hapus") { dialog, _ ->
                                    deleteProductData(id)
                                    dialog.dismiss()
                                }
                                .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
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

    private fun showProductModal(productObject: JSONObject?) {
        val dialogView = LayoutInflater.from(requireActivity()).inflate(R.layout.dialog_add_product, null)

        val txtTitle = dialogView.findViewById<TextView>(R.id.txtDialogTitle)
        val edName = dialogView.findViewById<EditText>(R.id.dialogProductName)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.dialogProductCategory)
        val edPrice = dialogView.findViewById<EditText>(R.id.dialogProductPrice)
        val edStock = dialogView.findViewById<EditText>(R.id.dialogProductStock)
        val edUnit = dialogView.findViewById<EditText>(R.id.dialogProductUnit)
        val edBrand = dialogView.findViewById<EditText>(R.id.dialogProductBrand)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnDialogCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnDialogSave)

        // FIX TANPA FILE BARU: Override getView & getDropDownView untuk memaksa teks menjadi Putih
        val categoriesList = arrayOf("Suplemen", "Minuman Sehat", "Aksesoris Gym", "Sewa Loker", "Merchandise")
        val adapter = object : ArrayAdapter<String>(requireActivity(), android.R.layout.simple_spinner_item, categoriesList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as? TextView)?.setTextColor(Color.WHITE) // Teks utama putih
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                view.setBackgroundColor(Color.parseColor("#1E1E1E")) // Background drop-down gelap
                (view as? TextView)?.setTextColor(Color.WHITE) // Teks drop-down putih
                return view
            }
        }

        spinnerCategory.adapter = adapter

        val alertDialog = AlertDialog.Builder(requireActivity())
            .setView(dialogView)
            .create()

        alertDialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        if (productObject != null) {
            txtTitle.text = "Edit Detail Produk"
            edName.setText(productObject.optString("name", ""))
            edPrice.setText(productObject.optString("price", ""))
            edStock.setText(productObject.optString("stock", ""))
            edUnit.setText(productObject.optString("unit", "pcs"))
            edBrand.setText(productObject.optString("brand", ""))
            btnSave.text = "PERBARUI"

            val currentCategory = productObject.optString("category", "")
            val spinnerPosition = adapter.getPosition(currentCategory)
            if (spinnerPosition >= 0) {
                spinnerCategory.setSelection(spinnerPosition)
            }
        }

        btnCancel.setOnClickListener { alertDialog.dismiss() }

        btnSave.setOnClickListener {
            val name = edName.text.toString().trim()
            if (name.isEmpty()) {
                edName.error = "Nama produk wajib diisi!"
                return@setOnClickListener
            }

            val selectedCategory = spinnerCategory.selectedItem.toString()
            val params = HashMap<String, String>()
            params["name"] = name
            params["category"] = selectedCategory
            params["price"] = edPrice.text.toString().trim().ifEmpty { "0" }
            params["stock"] = edStock.text.toString().trim().ifEmpty { "0" }
            params["unit"] = edUnit.text.toString().trim().ifEmpty { "pcs" }
            params["brand"] = edBrand.text.toString().trim()

            if (productObject != null) {
                params["mode"] = "update"
                params["id"] = productObject.optString("id", "")
            } else {
                params["mode"] = "insert"
            }

            val request = object : StringRequest(Method.POST, urlWebService,
                { response ->
                    try {
                        val res = JSONObject(response)
                        Toast.makeText(requireActivity(), res.optString("message", "Selesai"), Toast.LENGTH_SHORT).show()
                        if (res.optString("status") == "success") {
                            alertDialog.dismiss()
                            loadProductsData("")
                        }
                    } catch (e: Exception) { e.printStackTrace() }
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
                try {
                    val res = JSONObject(response)
                    Toast.makeText(requireActivity(), res.optString("message", "Selesai"), Toast.LENGTH_SHORT).show()
                    loadProductsData("")
                } catch (e: Exception) { e.printStackTrace() }
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