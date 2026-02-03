package com.bluechip.finance

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bluechip.finance.fragments.HomeFragment
import com.bluechip.finance.fragments.OvertimeFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Android 15 uçtan uca ekran desteğini aktif et
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)
        
        // --- BURASI KRİTİK: Mavi barı (AppBar) kodla zorla gizliyoruz ---
        supportActionBar?.hide()

        setContentView(R.layout.activity_main)

        // 2. İçeriğin saat/pil simgelerinin altında kalmaması için padding ekle
        val rootView = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
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
                    supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, HomeFragment())
                        .commit()
                    true
                }
                R.id.nav_overtime -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, OvertimeFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }
                R.id.nav_share -> {
                    shareApp()
                    false
                }
                else -> false
            }
        }
    }

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment is HomeFragment) {
            AlertDialog.Builder(this)
                .setTitle("Çıkış")
                .setMessage("Uygulamadan çıkmak istiyor musunuz?")
                .setPositiveButton("Evet") { _, _ ->
                    finish()
                }
                .setNegativeButton("Hayır", null)
                .show()
        } else {
            supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
            bottomNav.selectedItemId = R.id.nav_home
        }
    }

    private fun shareApp() {
        val shareText = """
🌟 Blue Chip Finance - İşçi Hakları Hesaplama

✅ Fazla Mesai Hesaplama
✅ Vergi Dilimi
✅ Kıdem Tazminatı
✅ Anlık Piyasa Fiyatları
✅ İşçi Haberleri

📱 İndir:
https://play.google.com/store/apps/details?id=com.bluechip.finance
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "Uygulamayı Paylaş"))
    }
}
