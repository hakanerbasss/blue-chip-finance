package com.bluechip.finance
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bluechip.finance.fragments.*
import com.google.android.material.bottomnavigation.BottomNavigationView
class MainActivity : AppCompatActivity() {
    private lateinit var bottomNav: BottomNavigationView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bottomNav = findViewById(R.id.bottom_navigation)
        loadFragment(CalculatorFragment())
        bottomNav.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_calculator -> { loadFragment(CalculatorFragment()); true }
                R.id.nav_overtime -> { loadFragment(OvertimeFragment()); true }
                R.id.nav_leave -> { loadFragment(LeaveFragment()); true }
                R.id.nav_notice -> { loadFragment(NoticeFragment()); true }
                R.id.nav_compensation -> { loadFragment(CompensationFragment()); true }
                else -> false
            }
        }
    }
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
