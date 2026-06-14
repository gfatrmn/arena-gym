package com.example.gym

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

    private val urlLoginService = "http://192.168.1.6/mobile/login.php" // Sesuaikan IP server lokalmu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val edEmail = findViewById<TextInputEditText>(R.id.edLoginEmail)
        val edPassword = findViewById<TextInputEditText>(R.id.edLoginPassword)
        val btnLogin = findViewById<Button>(R.id.btnLoginSubmit)

        btnLogin.setOnClickListener {
            val email = edEmail.text.toString().trim()
            val password = edPassword.text.toString().trim()

            if (email.isEmpty()) {
                edEmail.error = "Email tidak boleh kosong"
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

                        // LOGIKA PERADILAN ROLE: Mengarahkan halaman berdasarkan hak akses/role
                        when (role) {
                            "admin" -> {
                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                intent.putExtra("EXTRA_NAME", name)
                                startActivity(intent)
                                finish() // Tutup LoginActivity agar tidak bisa di-back
                            }
                            "kasir" -> {
                                val intent = Intent(this@LoginActivity, MainActivity::class.java) // Atau CashierDashboardActivity
                                intent.putExtra("EXTRA_NAME", name)
                                startActivity(intent)
                                finish()
                            }
//                            "member" -> {
//                                val intent = Intent(this@LoginActivity, MemberDashboardActivity::class.java)
//                                intent.putExtra("EXTRA_NAME", name)
//                                startActivity(intent)
//                                finish()
//                            }
                            else -> {
                                Toast.makeText(this@LoginActivity, "Role pengguna tidak dikenali sistem!", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@LoginActivity, "Format data respon bermasalah", Toast.LENGTH_SHORT).show()
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
}