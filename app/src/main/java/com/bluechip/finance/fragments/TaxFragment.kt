package com.bluechip.finance.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import com.bluechip.finance.R
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class TaxFragment : Fragment() {
    private lateinit var scrollView: ScrollView
    private lateinit var inputSalary: EditText
    private lateinit var spinnerMonth: Spinner
    private lateinit var btnCalculate: Button
    private lateinit var btnReset: Button
    private lateinit var infoIcon: TextView
    private lateinit var refreshIcon: TextView
    private lateinit var updateStatus: TextView
    private lateinit var warningText: TextView
    private lateinit var resultCard: MaterialCardView
    
    // Result TextViews
    private lateinit var resultGross: TextView
    private lateinit var resultSgk: TextView
    private lateinit var resultUnemployment: TextView
    private lateinit var resultBaseMatrah: TextView
    private lateinit var resultExemption: TextView
    private lateinit var resultTaxable: TextView
    private lateinit var resultCumulative: TextView
    private lateinit var resultMonthInfo: TextView
    private lateinit var resultBracket: TextView
    private lateinit var resultBracketInfo: TextView
    private lateinit var resultIncomeTax: TextView
    private lateinit var resultStampTax: TextView
    private lateinit var resultNet: TextView
    private lateinit var btnShare: Button
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    // Tax parameters (with 2026 defaults)
    private var minWageGross = 33030.0
    private var sgkRate = 0.14
    private var unemploymentRate = 0.01
    private var stampTaxRate = 0.00759
    private var taxBrackets = listOf(
        TaxBracket(190000.0, 0.15),
        TaxBracket(550000.0, 0.20),
        TaxBracket(1900000.0, 0.27),
        TaxBracket(6600000.0, 0.35),
        TaxBracket(999999999.0, 0.40)
    )
    private var lastUpdate = "2026-02-02"
    
    data class TaxBracket(val limit: Double, val rate: Double)
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_tax, container, false)
        
        scrollView = view.findViewById(R.id.scroll_view)
        inputSalary = view.findViewById(R.id.input_salary)
        spinnerMonth = view.findViewById(R.id.spinner_month)
        btnCalculate = view.findViewById(R.id.btn_calculate)
        btnReset = view.findViewById(R.id.btn_reset)
        infoIcon = view.findViewById(R.id.info_icon)
        refreshIcon = view.findViewById(R.id.refresh_icon)
        updateStatus = view.findViewById(R.id.update_status)
        warningText = view.findViewById(R.id.warning_text)
        resultCard = view.findViewById(R.id.result_card)
        
        resultGross = view.findViewById(R.id.result_gross)
        resultSgk = view.findViewById(R.id.result_sgk)
        resultUnemployment = view.findViewById(R.id.result_unemployment)
        resultBaseMatrah = view.findViewById(R.id.result_base_matrah)
        resultExemption = view.findViewById(R.id.result_exemption)
        resultTaxable = view.findViewById(R.id.result_taxable)
        resultCumulative = view.findViewById(R.id.result_cumulative)
        resultMonthInfo = view.findViewById(R.id.result_month_info)
        resultBracket = view.findViewById(R.id.result_bracket)
        resultBracketInfo = view.findViewById(R.id.result_bracket_info)
        resultIncomeTax = view.findViewById(R.id.result_income_tax)
        resultStampTax = view.findViewById(R.id.result_stamp_tax)
        resultNet = view.findViewById(R.id.result_net)
        btnShare = view.findViewById(R.id.btn_share)
        
        setupMonthSpinner()
        loadParameters()
        setupListeners()
        
        return view
    }
    
    private fun setupMonthSpinner() {
        val months = arrayOf("Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
            "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMonth.adapter = adapter
        
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        spinnerMonth.setSelection(currentMonth)
    }
    
    private fun loadParameters() {
        val prefs = requireContext().getSharedPreferences("tax_params", Context.MODE_PRIVATE)
        val savedJson = prefs.getString("params_json", null)
        
        if (savedJson != null) {
            parseParameters(savedJson)
            updateStatus.text = "📅 Son güncelleme: $lastUpdate"
            updateStatus.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
            warningText.visibility = View.GONE
        } else {
            updateStatus.text = "📅 Varsayılan: 2026 verileri"
            updateStatus.setTextColor(requireContext().getColor(android.R.color.holo_orange_dark))
            warningText.visibility = View.VISIBLE
        }
        
        // Try to fetch fresh data
        fetchParameters()
    }
    
    private fun fetchParameters() {
        scope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    URL("https://raw.githubusercontent.com/hakanerbasss/blue-chip-finance/main/tax_parameters.json").readText()
                }
                
                parseParameters(json)
                
                val prefs = requireContext().getSharedPreferences("tax_params", Context.MODE_PRIVATE)
                prefs.edit().putString("params_json", json).apply()
                
                updateStatus.text = "📅 Son güncelleme: $lastUpdate"
                updateStatus.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
                warningText.visibility = View.GONE
                
            } catch (e: Exception) {
                // Silently fail, use defaults or cached
            }
        }
    }
    
    private fun parseParameters(json: String) {
        try {
            val obj = JSONObject(json)
            lastUpdate = obj.getString("last_update")
            minWageGross = obj.getDouble("min_wage_gross")
            sgkRate = obj.getDouble("sgk_employee_rate")
            unemploymentRate = obj.getDouble("unemployment_rate")
            stampTaxRate = obj.getDouble("stamp_tax_rate")
            
            val brackets = mutableListOf<TaxBracket>()
            val bracketsArray = obj.getJSONArray("tax_brackets")
            for (i in 0 until bracketsArray.length()) {
                val bracket = bracketsArray.getJSONObject(i)
                brackets.add(TaxBracket(
                    bracket.getDouble("limit"),
                    bracket.getDouble("rate")
                ))
            }
            taxBrackets = brackets
        } catch (e: Exception) {
            Toast.makeText(context, "Parametre hatası, varsayılanlar kullanılıyor", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupListeners() {
        infoIcon.setOnClickListener { showInfoDialog() }
        refreshIcon.setOnClickListener {
            Toast.makeText(context, "Güncelleniyor...", Toast.LENGTH_SHORT).show()
            fetchParameters()
        }
        btnCalculate.setOnClickListener {
            hideKeyboard()
            calculate()
        }
        btnReset.setOnClickListener { reset() }
        btnShare.setOnClickListener { share() }
    }
    
    private fun showInfoDialog() {
        val message = """
📋 VERGİ DİLİMİ NEDİR?

Maaşınızdan kesilen gelir vergisi oranını 
belirleyen sistemdir. Türkiye'de "artan 
oranlı" vergi sistemi uygulanır.

━━━━━━━━━━━━━━━━━━━━━
📊 2026 VERGİ DİLİMLERİ
━━━━━━━━━━━━━━━━━━━━━

Kümülatif Matrah | Oran
0 - 190.000₺ → %15
190.000 - 550.000₺ → %20
550.000 - 1.900.000₺ → %27
1.900.000 - 6.600.000₺ → %35
6.600.000+ ₺ → %40

━━━━━━━━━━━━━━━━━━━━━
❓ KÜMÜLATİF MATRAH NEDİR?
━━━━━━━━━━━━━━━━━━━━━

Yıl başından itibaren biriken vergiye 
tabi kazançlarınızın toplamıdır.

Örnek:
• Ocak matrah: 30.000₺
• Şubat matrah: 30.000₺
• Kümülatif: 60.000₺ (Şubat sonu)

━━━━━━━━━━━━━━━━━━━━━
🎯 KİMLER İÇİN GEÇERLİ?
━━━━━━━━━━━━━━━━━━━━━

✅ Özel sektör çalışanları
✅ Kamu çalışanları (memur)
✅ Askerler
✅ Tüm ücretliler

❌ Asgari ücretliler muaftır
   (2026: 33.030₺ altı)

━━━━━━━━━━━━━━━━━━━━━
💡 ÖNEMLİ BİLGİLER
━━━━━━━━━━━━━━━━━━━━━

• Vergi dilimi her yıl sıfırlanır
• Ocak ayında %15'den başlar
• Yıl içinde kümülatif arttıkça dilim artar
• Asgari ücret kısmı vergiden muaftır
• Memur = Özel Sektör (fark yok!)

📜 193 Sayılı Gelir Vergisi Kanunu
🔗 mevzuat.gov.tr/mevzuat?MevzuatNo=193
        """.trimIndent()
        
        val textView = TextView(requireContext()).apply {
            text = message
            setPadding(40, 20, 40, 20)
            textSize = 13f
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("VERGİ DİLİMİ REHBERİ")
            .setView(textView)
            .setPositiveButton("Tamam", null)
            .show()
    }
    
    private fun calculate() {
        val salaryText = inputSalary.text.toString()
        if (salaryText.isEmpty()) {
            Toast.makeText(context, "Lütfen brüt maaş giriniz", Toast.LENGTH_SHORT).show()
            return
        }
        
        val grossSalary = salaryText.toDoubleOrNull() ?: 0.0
        if (grossSalary <= 0) {
            Toast.makeText(context, "Geçerli bir tutar giriniz", Toast.LENGTH_SHORT).show()
            return
        }
        
        val monthIndex = spinnerMonth.selectedItemPosition + 1
        
        // Calculate
        val sgk = grossSalary * sgkRate
        val unemployment = grossSalary * unemploymentRate
        val baseMatrah = grossSalary - sgk - unemployment
        
        // Exemption
        val exemptionAmount = minOf(baseMatrah, minWageGross - (minWageGross * sgkRate) - (minWageGross * unemploymentRate))
        val taxableMatrah = maxOf(0.0, baseMatrah - exemptionAmount)
        
        // Cumulative
        val cumulativeMatrah = taxableMatrah * monthIndex
        
        // Find bracket
        var bracketRate = 0.15
        var bracketInfo = "0 - 190.000₺ arası"
        for (i in taxBrackets.indices) {
            if (cumulativeMatrah <= taxBrackets[i].limit) {
                bracketRate = taxBrackets[i].rate
                val lowerLimit = if (i == 0) 0.0 else taxBrackets[i-1].limit
                bracketInfo = "${formatMoney(lowerLimit)} - ${formatMoney(taxBrackets[i].limit)} arası"
                break
            }
        }
        
        // Taxes
        val incomeTax = taxableMatrah * bracketRate
        val stampTax = grossSalary * stampTaxRate
        
        // Net
        val netSalary = grossSalary - sgk - unemployment - incomeTax - stampTax
        
        // Display
        resultGross.text = "Brüt Maaş: ${formatMoney(grossSalary)}₺"
        resultSgk.text = "SGK İşçi (%${(sgkRate*100).toInt()}): -${formatMoney(sgk)}₺"
        resultUnemployment.text = "İşsizlik (%${(unemploymentRate*100).toInt()}): -${formatMoney(unemployment)}₺"
        resultBaseMatrah.text = "Vergi Matrahı: ${formatMoney(baseMatrah)}₺"
        resultExemption.text = "Asgari Ücret Muafiyeti: -${formatMoney(exemptionAmount)}₺"
        resultTaxable.text = "Vergiye Tabi: ${formatMoney(taxableMatrah)}₺"
        resultCumulative.text = "Kümülatif Matrah: ${formatMoney(cumulativeMatrah)}₺"
        resultMonthInfo.text = "ℹ️ $monthIndex aylık toplam"
        resultBracket.text = "🎯 Vergi Dilimi: %${(bracketRate*100).toInt()}"
        resultBracketInfo.text = "($bracketInfo)"
        resultIncomeTax.text = "Gelir Vergisi: -${formatMoney(incomeTax)}₺"
        resultStampTax.text = "Damga Vergisi: -${formatMoney(stampTax)}₺"
        resultNet.text = "💰 Net Maaş: ${formatMoney(netSalary)}₺"
        
        resultCard.visibility = View.VISIBLE
        scrollView.post {
            scrollView.smoothScrollTo(0, resultCard.top)
        }
    }
    
    private fun share() {
        val text = buildString {
            append("💰 VERGİ DİLİMİ HESAPLAMA\n\n")
            append("${resultGross.text}\n")
            append("${resultSgk.text}\n")
            append("${resultUnemployment.text}\n")
            append("${resultBaseMatrah.text}\n")
            append("${resultExemption.text}\n")
            append("${resultTaxable.text}\n\n")
            append("${resultCumulative.text}\n")
            append("${resultMonthInfo.text}\n\n")
            append("${resultBracket.text}\n")
            append("${resultBracketInfo.text}\n\n")
            append("${resultIncomeTax.text}\n")
            append("${resultStampTax.text}\n\n")
            append("${resultNet.text}\n\n")
            append("📱 Blue Chip Finance ile hesaplandı")
        }
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Sonuçları Paylaş"))
    }
    
    private fun reset() {
        inputSalary.text.clear()
        spinnerMonth.setSelection(Calendar.getInstance().get(Calendar.MONTH))
        resultCard.visibility = View.GONE
        hideKeyboard()
    }
    
    private fun hideKeyboard() {
        val imm = getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
    }
    
    private fun formatMoney(amount: Double): String {
        return String.format("%,.2f", amount).replace(',', 'X').replace('.', ',').replace('X', '.')
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
