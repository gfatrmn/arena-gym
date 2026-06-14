package com.example.gym

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.gym.databinding.ActivityCheckInBinding
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class CheckInFragment : Fragment() {

    private var _binding: ActivityCheckInBinding? = null
    private val binding get() = _binding!!

    private val urlWebService = "http://192.168.1.6/mobile/process_checkin.php"

    private var memberListNames = ArrayList<String>()
    private var memberListData = HashMap<String, String>()
    private var selectedCheckInType = "member"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityCheckInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set Background Group Radio Button
        binding.rgCheckInType.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#1E1E1E"))
            cornerRadius = 100f
        }
        setupRadioButtonsStyle()

        // Listener pergantian tipe check-in (Member / Tamu Non-Member)
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

        // Muat data drop-down pilihan member dan log harian
        fetchMemberSelectionData()
        loadCheckInLogs()

        // Listener tombol submit check-in gerbang depan
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

                    // Ganti 'this' menjadi 'requireActivity()'
                    val adapter = ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_dropdown_item, memberListNames)
                    binding.autoCompleteMemberAuto.setAdapter(adapter)

                    binding.autoCompleteMemberAuto.setDropDownBackgroundDrawable(
                        android.graphics.drawable.ColorDrawable(Color.parseColor("#1E1E1E"))
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            {
                Toast.makeText(requireActivity(), "Gagal sinkronisasi data pilihan member", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): Map<String, String> = mapOf("mode" to "get_members")
        }
        Volley.newRequestQueue(requireActivity()).add(request)
    }

    private fun loadCheckInLogs() {
        val request = object : StringRequest(Method.POST, urlWebService,
            { response ->
                try {
                    binding.containerCheckInLogs.removeAllViews()
                    val jsonArray = JSONArray(response)
                    val inflaterRow = LayoutInflater.from(requireActivity())

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)

                        // Ganti 'this' menjadi 'requireActivity()' via inflaterRow
                        val row = inflaterRow.inflate(R.layout.item_checkin_log_row, binding.containerCheckInLogs, false)

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
                Toast.makeText(requireActivity(), "Gagal memuat log harian", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): Map<String, String> = mapOf("mode" to "get_logs")
        }
        Volley.newRequestQueue(requireActivity()).add(request)
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
                    Toast.makeText(requireActivity(), res.getString("message"), Toast.LENGTH_LONG).show()
                    if (res.getString("status") == "success") {
                        binding.autoCompleteMemberAuto.setText("")
                        binding.edNonMemberName.setText("")
                        loadCheckInLogs()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireActivity(), "Format respon backend bermasalah", Toast.LENGTH_SHORT).show()
                }
            },
            {
                Toast.makeText(requireActivity(), "Gagal memproses pengiriman check-in", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): Map<String, String> = params
        }
        Volley.newRequestQueue(requireActivity()).add(request)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}