package com.bluechip.finance

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bluechip.finance.fragments.HomeFragment
import com.bluechip.finance.fragments.OvertimeFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.Intent
import android.net.Uri
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNav: BottomNavigationView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        bottomNav = findViewById(R.id.bottom_navigation)
        
        // İlk açılışta HomeFragment göster
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
                R.id.nav_share -> {
                    shareApp()
                    false
                }
                else -> false
            }
        }
    }
    
    private fun shareApp() {
        val shareText = """
🌟 Blue Chip Finance - İşçi Hakları Hesaplama

✅ Fazla Mesai Hesaplama
✅ AGİ Hesaplama
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
