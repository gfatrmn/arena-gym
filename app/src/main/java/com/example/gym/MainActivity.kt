package com.example.gym

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.gym.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Mengamankan status bar agar tidak tumpang tindih
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE

        // 1. TANGKAP DATA SESI LOGIN (Nama & Role)
        // Membaca data yang dilempar dari Intent LoginActivity atau dari SharedPreferences
        val sharedPref = getSharedPreferences("SESSION_PREF", Context.MODE_PRIVATE)

        // Mengambil role, jika lewat intent kosong, dia akan mengambil dari cache session storage
        val userRole = intent.getStringExtra("EXTRA_ROLE") ?: sharedPref.getString("role", "kasir") ?: "kasir"
        val userName = intent.getStringExtra("EXTRA_NAME") ?: sharedPref.getString("full_name", "Petugas") ?: "Petugas"

        // Menyapa pengguna yang berhasil masuk ke sistem Arena Gym
        Toast.makeText(this, "Login Sukses: Selamat Bekerja $userName!", Toast.LENGTH_SHORT).show()

        // Set halaman pertama yang muncul saat aplikasi dibuka (DashboardFragment)
        if (savedInstanceState == null) {
            replaceFragment(DashboardFragment())
        }

        // Navigasi Bottom Menu murni dengan sistem Fragment Swap
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> { replaceFragment(DashboardFragment()); true }
                R.id.nav_members -> { replaceFragment(MemberCrudFragment()); true }
                R.id.nav_checkin -> { replaceFragment(CheckInFragment()); true }
                R.id.nav_products -> { replaceFragment(ProductCrudFragment()); true }
                R.id.nav_reports -> { replaceFragment(ReportFragment()); true }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}