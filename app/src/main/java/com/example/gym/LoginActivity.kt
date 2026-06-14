package com.example.gym

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private val urlLoginService = "http://192.168.1.6/mobile/login.php"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // KUNCI UTAMA 1: Cek apakah user sudah login sebelumnya saat aplikasi dibuka
        val sharedPref = getSharedPreferences("SESSION_PREF", Context.MODE_PRIVATE)
        val isLogin = sharedPref.getBoolean("is_login", false)
        if (isLogin) {
            val savedRole = sharedPref.getString("role", "kasir") ?: "kasir"
            val savedName = sharedPref.getString("full_name", "Petugas")
            navigateToDashboard(savedRole, savedName)
            return // Hentikan onCreate agar layout login tidak berkedip muncul
        }

        setContentView(R.layout.activity_login)

        val edEmail = findViewById<TextInputEditText>(R.id.edLoginEmail)
        val edPassword = findViewById<TextInputEditText>(R.id.edLoginPassword)
        val btnLogin = findViewById<Button>(R.id.btnLoginSubmit)

        btnLogin.setOnClickListener {
            val email = edEmail.text.toString().trim()
            val password = edPassword.text.toString().trim()

            if (email.isEmpty()) {
                edEmail.error = "Username/Login tidak boleh kosong"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                edPassword.error = "Password tidak boleh kosong"
                return@setOnClickListener
            }

            processLogin(email, password)
        }
    }

    private fun processLogin(emailStr: String, passwordStr: String) {
        val request = object : StringRequest(
            Request.Method.POST, urlLoginService,
            { response ->
                Log.d("DEBUG_LOGIN", "Respons Server: $response")
                try {
                    val jsonObject = JSONObject(response)
                    val status = jsonObject.optString("status")
                    val message = jsonObject.optString("message")

                    Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()

                    if (status == "success") {
                        val userObj = jsonObject.getJSONObject("user")
                        val role = userObj.optString("role")
                        val name = userObj.optString("full_name")
                        val email = userObj.optString("email")

                        // KUNCI UTAMA 2: Simpan data ke SharedPreferences secara permanen
                        val sharedPref = getSharedPreferences("SESSION_PREF", Context.MODE_PRIVATE)
                        sharedPref.edit().apply {
                            putBoolean("is_login", true)
                            putString("role", role)
                            putString("full_name", name)
                            putString("email", email)
                            apply() // Terapkan penyimpanan
                        }

                        navigateToDashboard(role, name)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@LoginActivity, "Format data respons bermasalah", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(this@LoginActivity, "Gangguan koneksi ke server Volley", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["login"] = emailStr
                params["password"] = passwordStr
                return params
            }
        }
        Volley.newRequestQueue(this).add(request)
    }

    // Fungsi pembantu routing halaman agar kode tidak duplikat
    private fun navigateToDashboard(role: String, name: String?) {
        when (role.lowercase()) {
            "admin" -> {
                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                intent.putExtra("EXTRA_NAME", name)
                intent.putExtra("EXTRA_ROLE", role)
                startActivity(intent)
                finish()
            }
            "kasir" -> {
                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                intent.putExtra("EXTRA_NAME", name)
                intent.putExtra("EXTRA_ROLE", role)
                startActivity(intent)
                finish()
            }
//            "member" -> {
//                val intent = Intent(this@LoginActivity, MemberDashboardActivity::class.java)
//                intent.putExtra("EXTRA_NAME", name)
//                intent.putExtra("EXTRA_ROLE", role)
//                startActivity(intent)
//                finish()
//            }
            else -> {
                Toast.makeText(this@LoginActivity, "Role tidak dikenali sistem!", Toast.LENGTH_LONG).show()
            }
        }
    }
}