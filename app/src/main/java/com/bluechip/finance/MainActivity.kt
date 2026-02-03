package com.bluechip.finance

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bluechip.finance.fragments.HomeFragment
import com.bluechip.finance.fragments.OvertimeFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge() 
        super.onCreate(savedInstanceState)
        
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        val rootView = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // DÜZELTME: Buradaki '0' yerine 'systemBars.top' yazıyoruz.
            // Bu sayede içerik saat/pil çubuğunun altına itilir.
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            
            insets
        }

        bottomNav = findViewById(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, HomeFragment())
                        .commit()
                    true
                }
                R.id.nav_overtime -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, OvertimeFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }
    }
}
