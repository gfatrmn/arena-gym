package com.example.gym

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.gym.databinding.ActivityDashboardBinding
import org.json.JSONObject
import java.text.NumberFormat
import java.util.*

class DashboardFragment : Fragment() {

    private var _binding: ActivityDashboardBinding? = null
    private val binding get() = _binding!!
    private val urlWebService = "http://192.168.1.6/mobile/get_dashboard_data.php"

    // Default filter adalah Hari Ini
    private var activeFilter = "hari_ini"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Inisialisasi Dropdown Spinner
        val optionsList = arrayOf("Hari Ini", "Minggu Ini", "Bulan Ini")
        val spinnerAdapter = ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_item, optionsList)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDashboardFilter.adapter = spinnerAdapter

        // 2. Set listener saat pilihan dropdown diubah oleh user
        binding.spinnerDashboardFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                activeFilter = when (position) {
                    0 -> "hari_ini"
                    1 -> "minggu_ini"
                    2 -> "bulan_ini"
                    else -> "hari_ini"
                }
                loadDashboardStatsData(activeFilter)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadDashboardStatsData(filterRange: String) {
        // Menggunakan POST agar bisa mengirimkan parameter filter_range
        val request = object : StringRequest(Request.Method.POST, urlWebService,
            { response ->
                try {
                    val jsonObject = JSONObject(response)
                    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
                        maximumFractionDigits = 0
                    }

                    binding.txtDashTotalMember.text = jsonObject.optInt("total_member_aktif", 0).toString()
                    binding.txtDashIncomeToday.text = currencyFormat.format(jsonObject.optLong("pemasukan_hari_ini", 0))

                    val inflaterRow = LayoutInflater.from(requireActivity())

                    // 1. Render Member Baru
                    val containerMember = binding.containerRecentMembers
                    containerMember.removeAllViews()
                    val memberArray = jsonObject.optJSONArray("recent_members")
                    if (memberArray != null) {
                        for (i in 0 until memberArray.length()) {
                            val mObj = memberArray.getJSONObject(i)
                            val rowView = inflaterRow.inflate(R.layout.item_recent_member_row, containerMember, false)
                            rowView.findViewById<TextView>(R.id.txtMbNama).text = mObj.optString("full_name", "-")
                            rowView.findViewById<TextView>(R.id.txtMbTelepon).text = mObj.optString("phone", "-")
                            rowView.findViewById<TextView>(R.id.txtMbTglDaftar).text = mObj.optString("tanggal_daftar", "-")
                            containerMember.addView(rowView)
                        }
                    }

                    // 2. Render Check-In Baru
                    val containerCheckin = binding.containerRecentCheckins
                    containerCheckin.removeAllViews()
                    val checkinArray = jsonObject.optJSONArray("recent_checkins")
                    if (checkinArray != null) {
                        for (i in 0 until checkinArray.length()) {
                            val cObj = checkinArray.getJSONObject(i)
                            val rowView = inflaterRow.inflate(R.layout.item_recent_checkin_row, containerCheckin, false)
                            rowView.findViewById<TextView>(R.id.txtCiNama).text = cObj.optString("full_name", "-")

                            val txtTipe = rowView.findViewById<TextView>(R.id.txtCiStatusTipe)
                            txtTipe.text = cObj.optString("status_tipe", "MEMBER").uppercase()

                            rowView.findViewById<TextView>(R.id.txtCiJamMasuk).text = "${cObj.optString("jam_masuk", "00:00")} WIB"
                            containerCheckin.addView(rowView)
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            {
                Toast.makeText(requireActivity(), "Koneksi data dashboard bermasalah", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["filter_range"] = filterRange
                return params
            }
        }
        Volley.newRequestQueue(requireActivity()).add(request)
    }

    override fun onResume() {
        super.onResume()
        loadDashboardStatsData(activeFilter)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}