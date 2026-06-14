package com.example.gym

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadDashboardStatsData()
    }

    private fun loadDashboardStatsData() {
        val request = StringRequest(Request.Method.GET, urlWebService,
            { response ->
                try {
                    val jsonObject = JSONObject(response)
                    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
                        maximumFractionDigits = 0
                    }

                    binding.txtDashTotalMember.text = jsonObject.getInt("total_member_aktif").toString()
                    binding.txtDashIncomeToday.text = currencyFormat.format(jsonObject.getLong("pemasukan_hari_ini"))

                    val inflaterRow = LayoutInflater.from(requireActivity())

                    // 1. Render Member Baru
                    val containerMember = binding.containerRecentMembers
                    containerMember.removeAllViews()
                    val memberArray = jsonObject.getJSONArray("recent_members")
                    for (i in 0 until memberArray.length()) {
                        val mObj = memberArray.getJSONObject(i)
                        val rowView = inflaterRow.inflate(R.layout.item_recent_member_row, containerMember, false)
                        rowView.findViewById<TextView>(R.id.txtMbNama).text = mObj.getString("full_name")
                        rowView.findViewById<TextView>(R.id.txtMbTelepon).text = mObj.getString("phone")
                        rowView.findViewById<TextView>(R.id.txtMbTglDaftar).text = mObj.getString("tanggal_daftar")
                        containerMember.addView(rowView)
                    }

                    // 2. Render Check-In Baru
                    val containerCheckin = binding.containerRecentCheckins
                    containerCheckin.removeAllViews()
                    val checkinArray = jsonObject.getJSONArray("recent_checkins")
                    for (i in 0 until checkinArray.length()) {
                        val cObj = checkinArray.getJSONObject(i)
                        val rowView = inflaterRow.inflate(R.layout.item_recent_checkin_row, containerCheckin, false)
                        rowView.findViewById<TextView>(R.id.txtCiNama).text = cObj.getString("full_name")

                        val txtTipe = rowView.findViewById<TextView>(R.id.txtCiStatusTipe)
                        txtTipe.text = cObj.getString("status_tipe").uppercase()

                        rowView.findViewById<TextView>(R.id.txtCiJamMasuk).text = "${cObj.getString("jam_masuk")} WIB"
                        containerCheckin.addView(rowView)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            {
                Toast.makeText(requireActivity(), "Koneksi data dashboard bermasalah", Toast.LENGTH_SHORT).show()
            }
        )
        Volley.newRequestQueue(requireActivity()).add(request)
    }

    override fun onResume() {
        super.onResume()
        loadDashboardStatsData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}