package com.example.gym

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import java.text.SimpleDateFormat
import java.util.*

class ReportFragment : Fragment() {

    private var _binding: ActivityReportBinding? = null
    private val binding get() = _binding!!

    private val urlWebService = "http://192.168.1.6/mobile/get_report_data.php"
    private val calendar = Calendar.getInstance()

    private var startDateStr = ""
    private var endDateStr = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Muat data default pertama kali (tanpa filter tanggal)
        fetchReportDataServer("", "")

        // 1. SETUP DATE PICKER - TANGGAL MULAI
        val startDatePicker = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            startDateStr = sdf.format(calendar.time)

            val displayFormat = SimpleDateFormat("dd MMMM yyyy", Locale("in", "ID"))
            binding.edReportStartDate.setText(displayFormat.format(calendar.time))

            checkAndTriggerFilter()
        }

        binding.edReportStartDate.setOnClickListener {
            DatePickerDialog(requireActivity(), startDatePicker,
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        // 2. SETUP DATE PICKER - TANGGAL AKHIR
        val endDatePicker = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            endDateStr = sdf.format(calendar.time)

            val displayFormat = SimpleDateFormat("dd MMMM yyyy", Locale("in", "ID"))
            binding.edReportEndDate.setText(displayFormat.format(calendar.time))

            checkAndTriggerFilter()
        }

        binding.edReportEndDate.setOnClickListener {
            DatePickerDialog(requireActivity(), endDatePicker,
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        // 3. ACTION BUTTON EXPORT EXCEL BERDASARKAN FILTER TANGGAL
        binding.btnExportExcel.setOnClickListener {
            if (startDateStr.isEmpty() || endDateStr.isEmpty()) {
                Toast.makeText(requireActivity(), "Harap tentukan rentang tanggal terlebih dahulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val urlExcelService = "http://192.168.1.6/mobile/export_excel_report.php?start_date=$startDateStr&end_date=$endDateStr"
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlExcelService))
                startActivity(intent)
                Toast.makeText(requireActivity(), "Mengekspor file rekap...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireActivity(), "Browser pengunduhan gagal dibuka", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        // 4. ACTION LOGOUT
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun checkAndTriggerFilter() {
        if (startDateStr.isNotEmpty() && endDateStr.isNotEmpty()) {
            fetchReportDataServer(startDateStr, endDateStr)
        }
    }

    private fun fetchReportDataServer(start: String, end: String) {
        val request = object : StringRequest(Request.Method.POST, urlWebService,
            { response ->
                try {
                    val json = JSONObject(response)
                    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
                        maximumFractionDigits = 0
                    }

                    binding.txtRepTotalMember.text = json.optInt("total_member", 0).toString()
                    binding.txtRepTotalCheckIn.text = json.optInt("total_checkin", 0).toString()
                    binding.txtRepIncomeToday.text = currencyFormat.format(json.optLong("pemasukan_hari_ini", 0))
                    binding.txtRepIncomeAll.text = currencyFormat.format(json.optLong("total_pemasukan", 0))

                    val grafikArray = json.getJSONArray("grafik")
                    val entries = ArrayList<BarEntry>()
                    val labels = ArrayList<String>()

                    for (i in 0 until grafikArray.length()) {
                        val obj = grafikArray.getJSONObject(i)
                        labels.add(obj.optString("bulan", "-"))
                        entries.add(BarEntry(i.toFloat(), obj.optLong("total", 0).toFloat()))
                    }

                    setupBarChartVisualization(entries, labels)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { Toast.makeText(requireActivity(), "Gagal sinkronisasi data laporan keuangan", Toast.LENGTH_SHORT).show() }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf("start_date" to start, "end_date" to end)
            }
        }
        Volley.newRequestQueue(requireActivity()).add(request)
    }

    private fun setupBarChartVisualization(entries: ArrayList<BarEntry>, labels: ArrayList<String>) {
        val dataSet = BarDataSet(entries, "Total Omzet (Rp)").apply {
            color = Color.parseColor("#FF1E27")
            valueTextColor = Color.WHITE
            valueTextSize = 10f
        }

        val barData = BarData(dataSet).apply { barWidth = 0.5f }

        binding.barChartReport.apply {
            data = barData
            description.isEnabled = false
            legend.textColor = Color.WHITE

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.WHITE
                granularity = 1f
                isGranularityEnabled = true
                setDrawGridLines(false)
            }

            axisLeft.apply {
                textColor = Color.WHITE
                axisMinimum = 0f
            }
            axisRight.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert).apply {
            setTitle("Konfirmasi Keluar")
            setMessage("Apakah Anda yakin ingin keluar dari akun Arena Gym?")
            setPositiveButton("Ya, Keluar") { _, _ -> performLogoutAction() }
            setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#FF1E27"))
                    getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#888888"))
                }
            }.show()
        }
    }

    private fun performLogoutAction() {
        val sharedPref = requireActivity().getSharedPreferences("SESSION_PREF", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()
        Toast.makeText(requireContext(), "Berhasil keluar dari sistem", Toast.LENGTH_SHORT).show()

        val intent = Intent(requireActivity(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}