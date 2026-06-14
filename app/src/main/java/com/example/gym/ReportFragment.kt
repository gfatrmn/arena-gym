package com.example.gym

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.gym.databinding.ActivityReportBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import org.json.JSONObject
import java.text.NumberFormat
import java.util.*

class ReportFragment : Fragment() {

    private var _binding: ActivityReportBinding? = null
    private val binding get() = _binding!!

    private val urlWebService = "http://192.168.1.6/mobile/get_report_data.php"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ambil data laporan keuangan dari server saat fragmen dimuat
        fetchReportDataServer()
    }

    private fun fetchReportDataServer() {
        val request = StringRequest(Request.Method.GET, urlWebService,
            { response ->
                try {
                    val json = JSONObject(response)
                    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
                        maximumFractionDigits = 0
                    }

                    // 1. Tempel Teks Statistik Atas
                    binding.txtRepTotalMember.text = json.getInt("total_member").toString()
                    binding.txtRepTotalCheckIn.text = json.getInt("total_checkin").toString()
                    binding.txtRepIncomeToday.text = currencyFormat.format(json.getLong("pemasukan_hari_ini"))
                    binding.txtRepIncomeAll.text = currencyFormat.format(json.getLong("total_pemasukan"))

                    // 2. Olah Data Grafik Bulanan
                    val grafikArray = json.getJSONArray("grafik")
                    val entries = ArrayList<BarEntry>()
                    val labels = ArrayList<String>()

                    for (i in 0 until grafikArray.length()) {
                        val obj = grafikArray.getJSONObject(i)
                        labels.add(obj.getString("bulan"))
                        // Masukkan index X dan Nilai Uang Y
                        entries.add(BarEntry(i.toFloat(), obj.getLong("total").toFloat()))
                    }

                    setupBarChartVisualization(entries, labels)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { Toast.makeText(requireActivity(), "Gagal sinkronisasi data laporan keuangan", Toast.LENGTH_SHORT).show() }
        )
        Volley.newRequestQueue(requireActivity()).add(request)
    }

    private fun setupBarChartVisualization(entries: ArrayList<BarEntry>, labels: ArrayList<String>) {
        val dataSet = BarDataSet(entries, "Total Omzet (Rp)").apply {
            color = Color.parseColor("#FF1E27") // Warna batang Merah Arena Gym
            valueTextColor = Color.WHITE
            valueTextSize = 10f
        }

        val barData = BarData(dataSet).apply {
            barWidth = 0.5f
        }

        binding.barChartReport.apply {
            data = barData
            description.isEnabled = false
            legend.textColor = Color.WHITE

            // Konfigurasi Sumbu X (Bulan)
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.WHITE
                granularity = 1f
                isGranularityEnabled = true
                setDrawGridLines(false)
            }

            // Konfigurasi Sumbu Y Kiri
            axisLeft.apply {
                textColor = Color.WHITE
                axisMinimum = 0f
            }

            // Hilangkan Sumbu Y Kanan agar rapi tidak numpuk
            axisRight.isEnabled = false

            animateY(1000) // Efek animasi batang naik saat dibuka
            invalidate() // Refresh tampilan grafik
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}