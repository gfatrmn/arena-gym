package com.example.gym

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.gym.databinding.ActivityCheckInBinding
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class CheckInActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckInBinding
    private val urlWebService = "http://192.168.1.6/mobile/process_checkin.php"

    private var memberListNames = ArrayList<String>()
    private var memberListData = HashMap<String, String>()
    private var selectedCheckInType = "member"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rgCheckInType.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#1E1E1E"))
            cornerRadius = 100f
        }
        setupRadioButtonsStyle()

        binding.bottomNavigation.selectedItemId = R.id.nav_checkin
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_checkin -> true
                R.id.nav_members -> {
                    startActivity(Intent(this, MemberCrudActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }

                R.id.nav_products -> {
                    startActivity(Intent(this, ProductCrudActivity::class.java))
                    overridePendingTransition(0, 0); finish(); true
                }

                R.id.nav_reports -> {
                    startActivity(Intent(this, ReportActivity::class.java))
                    overridePendingTransition(0, 0); finish(); true
                }
                R.id.nav_checkin -> true
                else -> false
            }
        }

        binding.rgCheckInType.setOnCheckedChangeListener { _, checkedId ->
            setupRadioButtonsStyle()
            if (checkedId == R.id.rbTypeMember) {
                selectedCheckInType = "member"
                binding.layoutInputMember.visibility = View.VISIBLE
                binding.layoutInputNonMemberName.visibility = View.GONE
            } else {
                selectedCheckInType = "non_member"
                binding.layoutInputMember.visibility = View.GONE
                binding.layoutInputNonMemberName.visibility = View.VISIBLE
            }
        }

        fetchMemberSelectionData()
        loadCheckInLogs()

        binding.btnSubmitCheckIn.setOnClickListener {
            processCheckInSubmit()
        }
    }

    private fun setupRadioButtonsStyle() {
        try {
            for (i in 0 until binding.rgCheckInType.childCount) {
                val view = binding.rgCheckInType.getChildAt(i)
                if (view is RadioButton) {
                    if (view.isChecked) {
                        view.background = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            setColor(Color.parseColor("#FF1E27"))
                            cornerRadius = 100f
                        }
                        view.setTextColor(Color.parseColor("#FFFFFF"))
                    } else {
                        view.background = null
                        view.setTextColor(Color.parseColor("#888888"))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun fetchMemberSelectionData() {
        val request = object : StringRequest(Method.POST, urlWebService,
            { response ->
                try {
                    val jsonArray = JSONArray(response)
                    memberListNames.clear()
                    memberListData.clear()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val name = obj.getString("full_name")
                        val id = obj.getString("id")
                        memberListNames.add(name)
                        memberListData[name] = id
                    }

                    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, memberListNames)
                    binding.autoCompleteMemberAuto.setAdapter(adapter)

                    binding.autoCompleteMemberAuto.setDropDownBackgroundDrawable(
                        android.graphics.drawable.ColorDrawable(Color.parseColor("#1E1E1E"))
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            {
                Toast.makeText(this, "Gagal sinkronisasi data pilihan member", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): Map<String, String> = mapOf("mode" to "get_members")
        }
        Volley.newRequestQueue(this).add(request)
    }

    private fun loadCheckInLogs() {
        val request = object : StringRequest(Method.POST, urlWebService,
            { response ->
                try {
                    binding.containerCheckInLogs.removeAllViews()
                    val jsonArray = JSONArray(response)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val row = LayoutInflater.from(this).inflate(R.layout.item_checkin_log_row, binding.containerCheckInLogs, false)

                        row.findViewById<TextView>(R.id.txtLogName).text = obj.optString("name", "-")
                        row.findViewById<TextView>(R.id.txtLogType).text = obj.optString("type", "MEMBER").uppercase()
                        row.findViewById<TextView>(R.id.txtLogTime).text = obj.optString("checkin_time", "--:--")

                        binding.containerCheckInLogs.addView(row)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            {
                Toast.makeText(this, "Gagal memuat log harian", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): Map<String, String> = mapOf("mode" to "get_logs")
        }
        Volley.newRequestQueue(this).add(request)
    }

    private fun processCheckInSubmit() {
        val params = HashMap<String, String>()
        params["mode"] = "submit_checkin"
        params["type"] = selectedCheckInType

        if (selectedCheckInType == "member") {
            val typedName = binding.autoCompleteMemberAuto.text.toString().trim()

            if (typedName.isEmpty()) {
                binding.autoCompleteMemberAuto.error = "Ketik atau pilih salah satu nama member!"
                return
            }

            val memberId = memberListData[typedName]
            if (memberId == null) {
                binding.autoCompleteMemberAuto.error = "Nama Atlet tidak valid atau belum terdaftar!"
                return
            }
            params["member_id"] = memberId
        } else {
            val name = binding.edNonMemberName.text.toString().trim()
            if (name.isEmpty()) {
                binding.edNonMemberName.error = "Nama wajib diisi untuk tamu harian!"
                return
            }
            params["non_member_name"] = name
        }

        val request = object : StringRequest(Method.POST, urlWebService,
            { response ->
                try {
                    val res = JSONObject(response)
                    Toast.makeText(this, res.getString("message"), Toast.LENGTH_LONG).show()
                    if (res.getString("status") == "success") {
                        binding.autoCompleteMemberAuto.setText("")
                        binding.edNonMemberName.setText("")
                        loadCheckInLogs()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Format respon backend bermasalah", Toast.LENGTH_SHORT).show()
                }
            },
            {
                Toast.makeText(this, "Gagal memproses pengiriman check-in", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): Map<String, String> = params
        }
        Volley.newRequestQueue(this).add(request)
    }
}