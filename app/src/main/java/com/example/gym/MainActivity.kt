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

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE

        // Ambil data dari SharedPreferences cache sesion
        val sharedPref = getSharedPreferences("SESSION_PREF", Context.MODE_PRIVATE)
        val userRole = sharedPref.getString("role", "kasir") ?: "kasir"
        val userName = sharedPref.getString("full_name", "Petugas")


        if (savedInstanceState == null) {
            replaceFragment(DashboardFragment())
        }

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