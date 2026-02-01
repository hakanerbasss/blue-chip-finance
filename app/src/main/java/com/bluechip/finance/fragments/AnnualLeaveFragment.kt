package com.bluechip.finance.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import com.bluechip.finance.R
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class AnnualLeaveFragment : Fragment() {
    private lateinit var scrollView: ScrollView
    private lateinit var btnStartDate: Button
    private lateinit var btnCalcDate: Button
    private lateinit var inputAge: EditText
    private lateinit var checkUnderground: CheckBox
    private lateinit var btnCalculate: Button
    private lateinit var btnReset: Button
    private lateinit var infoIcon: TextView
    private lateinit var resultCard: MaterialCardView
    private lateinit var resultDuration: TextView
    private lateinit var resultLeave: TextView
    private lateinit var resultInfo: TextView
    private lateinit var btnShare: Button
    
    private var startDate: Calendar? = null
    private var calcDate: Calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_annual_leave, container, false)
        
        scrollView = view.findViewById(R.id.scroll_view)
        btnStartDate = view.findViewById(R.id.btn_start_date)
        btnCalcDate = view.findViewById(R.id.btn_calc_date)
        inputAge = view.findViewById(R.id.input_age)
        checkUnderground = view.findViewById(R.id.check_underground)
        btnCalculate = view.findViewById(R.id.btn_calculate)
        btnReset = view.findViewById(R.id.btn_reset)
        infoIcon = view.findViewById(R.id.info_icon)
        resultCard = view.findViewById(R.id.result_card)
        resultDuration = view.findViewById(R.id.result_duration)
        resultLeave = view.findViewById(R.id.result_leave)
        resultInfo = view.findViewById(R.id.result_info)
        btnShare = view.findViewById(R.id.btn_share)
        
        btnCalcDate.text = dateFormat.format(calcDate.time)
        
        setupListeners()
        
        return view
    }
    
    private fun setupListeners() {
        btnStartDate.setOnClickListener { showDatePicker(true) }
        btnCalcDate.setOnClickListener { showDatePicker(false) }
        infoIcon.setOnClickListener { showInfoDialog() }
        btnCalculate.setOnClickListener {
            hideKeyboard()
            calculate()
        }
        btnReset.setOnClickListener { reset() }
        btnShare.setOnClickListener { share() }
    }
    
    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = if (isStartDate) Calendar.getInstance() else calcDate
        DatePickerDialog(requireContext(), { _, year, month, day ->
            val selected = Calendar.getInstance()
            selected.set(year, month, day)
            if (isStartDate) {
                startDate = selected
                btnStartDate.text = dateFormat.format(selected.time)
            } else {
                calcDate = selected
                btnCalcDate.text = dateFormat.format(selected.time)
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
    
    private fun showInfoDialog() {
        val message = """
📋 İZİN TÜRLERİ REHBERİ

━━━━━━━━━━━━━━━━━━━━━
1️⃣ YILLIK ÜCRETLİ İZİN
━━━━━━━━━━━━━━━━━━━━━

📜 4857 İş Kanunu Mad. 53
🔗 mevzuat.gov.tr/mevzuat?MevzuatNo=4857

• 1 yıl çalıştıktan sonra hak kazanılır
• 0-5 yıl: 14 gün
• 5-15 yıl: 20 gün
• 15+ yıl: 26 gün
• 18 yaş altı: 20 gün
• 50+ yaş: 20 gün
• Yer altı işçisi: +4 gün

━━━━━━━━━━━━━━━━━━━━━
2️⃣ DOĞUM İZNİ (Kadınlar)
━━━━━━━━━━━━━━━━━━━━━

📜 4857 İş Kanunu Mad. 74

ÜCRETLİ:
• Toplam: 16 hafta (112 gün)
• Doğum öncesi: 8 hafta
• Doğum sonrası: 8 hafta
• Çoğul: +2 hafta

ÜCRETSİZ:
• 24 aya kadar

━━━━━━━━━━━━━━━━━━━━━
3️⃣ BABALIK İZNİ
━━━━━━━━━━━━━━━━━━━━━

📜 4857 İş Kanunu Ek Mad. 2

• Özel sektör: 5 gün
• Kamu: 10 gün
• Ücretli, yıllık izinden düşmez

━━━━━━━━━━━━━━━━━━━━━
4️⃣ MAZERET İZNİ
━━━━━━━━━━━━━━━━━━━━━

📜 4857 İş Kanunu Ek Mad. 2

• Evlenme: 3 gün
• Yakın ölümü: 3 gün
• Evlat edinme: 3 gün
• Engelli çocuk: 10 gün/yıl

⚠️ Yıllık izinden DÜŞMEZ

━━━━━━━━━━━━━━━━━━━━━
5️⃣ HASTALIK/RAPOR İZNİ
━━━━━━━━━━━━━━━━━━━━━

📜 5510 SGK Kanunu Mad. 18

ÖZEL SEKTÖR:
• İlk 2 gün: İşveren
• 3. gün+: SGK ödeme
• Normal: Brüt ücretin 2/3'ü
• İş kazası: Tam ücret

MEMUR (657 DMK m.105):
• 7 gün: Tam maaş
• 7+ gün: %40 kesinti
• İSTİSNA (kesinti yok):
  Sağlık kurulu, kanser,
  verem, hastane yatış

━━━━━━━━━━━━━━━━━━━━━
6️⃣ SÜT İZNİ (Kadınlar)
━━━━━━━━━━━━━━━━━━━━━

📜 4857 İş Kanunu Mad. 74

• 1 yıl sürer
• Günde 1.5 saat
• Ücretli

━━━━━━━━━━━━━━━━━━━━━
⚠️ ÖNEMLİ
━━━━━━━━━━━━━━━━━━━━━

✓ Mazeret izinleri yıllık izinden DÜŞMEZ
✓ Rapor yıllık izinden DÜŞMEZ
✓ Hafta sonu izne DAHİLDİR
✓ TAKVİM GÜNÜ hesaplanır
        """.trimIndent()
        
        val textView = TextView(requireContext()).apply {
            text = message
            setPadding(40, 20, 40, 20)
            textSize = 13f
            autoLinkMask = Linkify.WEB_URLS
            movementMethod = LinkMovementMethod.getInstance()
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("İZİN TÜRLERİ REHBERİ")
            .setView(textView)
            .setPositiveButton("Tamam", null)
            .show()
    }
    
    private fun calculate() {
        if (startDate == null) {
            Toast.makeText(context, "Lütfen işe başlama tarihini seçin", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (calcDate.before(startDate)) {
            Toast.makeText(context, "Hesaplama tarihi başlama tarihinden önce olamaz", Toast.LENGTH_SHORT).show()
            return
        }
        
        val diffMillis = calcDate.timeInMillis - startDate!!.timeInMillis + 86400000
        val totalDays = (diffMillis / 86400000).toInt()
        val years = totalDays / 365
        val remainingDays = totalDays % 365
        val months = remainingDays / 30
        val days = remainingDays % 30
        
        val ageText = inputAge.text.toString()
        val age = ageText.toIntOrNull()
        
        var leaveDays = when {
            age != null && age < 18 -> 20
            age != null && age >= 50 -> 20
            years < 5 -> 14
            years < 15 -> 20
            else -> 26
        }
        
        if (checkUnderground.isChecked) {
            leaveDays += 4
        }
        
        resultDuration.text = "Çalışma Süresi: $years yıl $months ay $days gün"
        resultLeave.text = "Yıllık İzin Hakkı: $leaveDays gün"
        
        val infoText = buildString {
            when {
                age != null && age < 18 -> append("ℹ️ 18 yaş altı: 20 gün (İş Kanunu Mad. 53)\n")
                age != null && age >= 50 -> append("ℹ️ 50+ yaş: 20 gün (İş Kanunu Mad. 53)\n")
                years < 5 -> append("ℹ️ 0-5 yıl kıdem: 14 gün\n")
                years < 15 -> append("ℹ️ 5-15 yıl kıdem: 20 gün\n")
                else -> append("ℹ️ 15+ yıl kıdem: 26 gün\n")
            }
            if (checkUnderground.isChecked) {
                append("ℹ️ Yer altı işçisi: +4 gün ek")
            }
        }
        resultInfo.text = infoText.trim()
        
        resultCard.visibility = View.VISIBLE
        scrollView.post {
            scrollView.smoothScrollTo(0, resultCard.top)
        }
    }
    
    private fun share() {
        val text = buildString {
            append("📅 YILLIK İZİN HESAPLAMA\n\n")
            append("${resultDuration.text}\n")
            append("${resultLeave.text}\n\n")
            if (resultInfo.text.isNotEmpty()) {
                append("${resultInfo.text}\n\n")
            }
            append("📱 Blue Chip Finance ile hesaplandı")
        }
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Sonuçları Paylaş"))
    }
    
    private fun reset() {
        startDate = null
        calcDate = Calendar.getInstance()
        btnStartDate.text = "Tarih Seç"
        btnCalcDate.text = dateFormat.format(calcDate.time)
        inputAge.text.clear()
        checkUnderground.isChecked = false
        resultCard.visibility = View.GONE
        hideKeyboard()
    }
    
    private fun hideKeyboard() {
        val imm = getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
    }
}
