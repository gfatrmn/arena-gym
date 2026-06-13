package com.example.gym

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.gym.databinding.ActivityDashboardBinding
import org.json.JSONObject
import java.text.NumberFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val urlWebService = "http://192.168.1.6/mobile/get_dashboard_data.php"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Sistem Navigasi Menu Bawah 5 Pintu Sinkron
        binding.bottomNavigation.selectedItemId = R.id.nav_dashboard
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true
                R.id.nav_members -> {
                    startActivity(Intent(this, MemberCrudActivity::class.java))
                    overridePendingTransition(0, 0); finish(); true
                }
                R.id.nav_checkin -> {
                    startActivity(Intent(this, CheckInActivity::class.java))
                    overridePendingTransition(0, 0); finish(); true
                }
                R.id.nav_products -> {
                    startActivity(Intent(this, ProductCrudActivity::class.java))
                    overridePendingTransition(0, 0); finish(); true
                }
                R.id.nav_reports -> {
                    startActivity(Intent(this, ReportActivity::class.java))
                    overridePendingTransition(0, 0); finish(); true
                }
                else -> false
            }
        }

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

                    // 1. Tempel data summary statistik card atas
                    binding.txtDashTotalMember.text = jsonObject.getInt("total_member_aktif").toString()
                    binding.txtDashIncomeToday.text = currencyFormat.format(jsonObject.getLong("pemasukan_hari_ini"))

                    val inflater = LayoutInflater.from(this)

                    // 2. RENDER TABEL MEMBER TERBARU (3 Kolom: item_recent_member_row.xml)
                    val containerMember = binding.containerRecentMembers
                    containerMember.removeAllViews()
                    val memberArray = jsonObject.getJSONArray("recent_members")

                    for (i in 0 until memberArray.length()) {
                        val mObj = memberArray.getJSONObject(i)
                        val rowView = inflater.inflate(R.layout.item_recent_member_row, containerMember, false)

                        rowView.findViewById<TextView>(R.id.txtMbNama).text = mObj.getString("full_name")
                        rowView.findViewById<TextView>(R.id.txtMbTelepon).text = mObj.getString("phone")

                        val txtTgl = rowView.findViewById<TextView>(R.id.txtMbTglDaftar)
                        txtTgl.text = mObj.getString("tanggal_daftar")

                        containerMember.addView(rowView)
                    }

                    // 3. RENDER TABEL RIWAYAT CHECK-IN TERBARU (3 Kolom: item_recent_checkin_row.xml)
                    val containerCheckin = binding.containerRecentCheckins
                    containerCheckin.removeAllViews()
                    val checkinArray = jsonObject.getJSONArray("recent_checkins")

                    for (i in 0 until checkinArray.length()) {
                        val cObj = checkinArray.getJSONObject(i)
                        val rowView = inflater.inflate(R.layout.item_recent_checkin_row, containerCheckin, false)

                        rowView.findViewById<TextView>(R.id.txtCiNama).text = cObj.getString("full_name")

                        val txtTipe = rowView.findViewById<TextView>(R.id.txtCiStatusTipe)
                        txtTipe.text = cObj.getString("status_tipe").uppercase()

                        val txtJam = rowView.findViewById<TextView>(R.id.txtCiJamMasuk)
                        txtJam.text = "${cObj.getString("jam_masuk")}"

                        containerCheckin.addView(rowView)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            {
                Toast.makeText(this, "Koneksi data dashboard bermasalah", Toast.LENGTH_SHORT).show()
            }
        )
        Volley.newRequestQueue(this).add(request)
    }

    override fun onResume() {
        super.onResume()
        // Otomatis refresh data ketika kasir kembali ke halaman dashboard utama
        loadDashboardStatsData()
    }
}