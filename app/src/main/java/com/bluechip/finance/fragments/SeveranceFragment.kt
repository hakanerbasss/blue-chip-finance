package com.bluechip.finance.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import com.bluechip.finance.R
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class SeveranceFragment : Fragment() {
    private lateinit var scrollView: ScrollView
    private lateinit var btnStartDate: Button
    private lateinit var btnEndDate: Button
    private lateinit var inputSalary: EditText
    private lateinit var btnReason: Button
    private lateinit var btnNotice: Button
    private lateinit var btnCalculate: Button
    private lateinit var btnReset: Button
    private lateinit var infoIcon: TextView
    private lateinit var salaryInfo: TextView
    private lateinit var resultCard: MaterialCardView
    private lateinit var resultDuration: TextView
    private lateinit var resultSeveranceGross: TextView
    private lateinit var resultSeveranceTax: TextView
    private lateinit var resultSeveranceNet: TextView
    private lateinit var resultNoticePeriod: TextView
    private lateinit var resultNoticeGross: TextView
    private lateinit var resultNoticeTax: TextView
    private lateinit var resultNoticeNet: TextView
    private lateinit var resultTotal: TextView
    private lateinit var resultInfo: TextView
    private lateinit var btnShare: Button
    private lateinit var refreshIcon: TextView
    private lateinit var updateStatus: TextView
    private lateinit var warningText: TextView
    
    private var startDate: Calendar? = null
    private var endDate: Calendar? = null
    private var noticeGiven = false
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    
    private var severanceCeiling = 64948.77
    private var stampTaxRate = 0.00759
    private var lastUpdate = "2026-02-02"
    
    private val reasons = listOf(
        "İşveren Feshi",
        "İşçi Haklı Feshi",
        "Emeklilik",
        "Askerlik",
        "Evlilik (Kadın, 1 Yıl İçinde)",
        "İşçi Haksız Feshi",
        "Deneme Süresi Feshi"
    )
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_severance, container, false)
        
        scrollView = view.findViewById(R.id.scroll_view)
        btnStartDate = view.findViewById(R.id.btn_start_date)
        btnEndDate = view.findViewById(R.id.btn_end_date)
        inputSalary = view.findViewById(R.id.input_salary)
        btnReason = view.findViewById(R.id.btn_reason)
        btnNotice = view.findViewById(R.id.btn_notice)
        btnCalculate = view.findViewById(R.id.btn_calculate)
        btnReset = view.findViewById(R.id.btn_reset)
        infoIcon = view.findViewById(R.id.info_icon)
        salaryInfo = view.findViewById(R.id.salary_info)
        resultCard = view.findViewById(R.id.result_card)
        resultDuration = view.findViewById(R.id.result_duration)
        resultSeveranceGross = view.findViewById(R.id.result_severance_gross)
        resultSeveranceTax = view.findViewById(R.id.result_severance_tax)
        resultSeveranceNet = view.findViewById(R.id.result_severance_net)
        resultNoticePeriod = view.findViewById(R.id.result_notice_period)
        resultNoticeGross = view.findViewById(R.id.result_notice_gross)
        resultNoticeTax = view.findViewById(R.id.result_notice_tax)
        resultNoticeNet = view.findViewById(R.id.result_notice_net)
        resultTotal = view.findViewById(R.id.result_total)
        resultInfo = view.findViewById(R.id.result_info)
        btnShare = view.findViewById(R.id.btn_share)
        refreshIcon = view.findViewById(R.id.refresh_icon)
        updateStatus = view.findViewById(R.id.update_status)
        warningText = view.findViewById(R.id.warning_text)

        // JSON parametrelerini yükle
        loadParameters()
        fetchParameters()
        
        setupListeners()
        
        return view
    }
    
    private fun setupListeners() {
        btnStartDate.setOnClickListener { showDatePicker(true) }
        btnEndDate.setOnClickListener { showDatePicker(false) }
        btnReason.setOnClickListener { showReasonDialog() }
        btnNotice.setOnClickListener { showNoticeDialog() }
        infoIcon.setOnClickListener { showInfoDialog() }
        refreshIcon.setOnClickListener {
            fetchParameters()
            Toast.makeText(requireContext(), "Parametreler güncelleniyor...", Toast.LENGTH_SHORT).show()
        }
        salaryInfo.setOnClickListener { showSalaryInfoDialog() }
        btnCalculate.setOnClickListener {
            hideKeyboard()
            calculate()
        }
        btnReset.setOnClickListener { reset() }
        btnShare.setOnClickListener { share() }
    }
    
    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            val selected = Calendar.getInstance()
            selected.set(year, month, day)
            if (isStartDate) {
                startDate = selected
                btnStartDate.text = dateFormat.format(selected.time)
            } else {
                endDate = selected
                btnEndDate.text = dateFormat.format(selected.time)
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
    
    private fun showReasonDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Çıkış Nedeni Seçin")
            .setSingleChoiceItems(reasons.toTypedArray(), reasons.indexOf(btnReason.text.toString())) { dialog, which ->
                btnReason.text = reasons[which]
                dialog.dismiss()
            }
            .setNegativeButton("İptal", null)
            .show()
    }
    
    private fun showNoticeDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("İhbar Verildi mi?")
            .setSingleChoiceItems(arrayOf("Hayır (İhbar verilmedi)", "Evet (İhbar verildi)"), if (noticeGiven) 1 else 0) { dialog, which ->
                noticeGiven = (which == 1)
                btnNotice.text = if (noticeGiven) "Evet (İhbar verildi)" else "Hayır (İhbar verilmedi)"
                dialog.dismiss()
            }
            .setNegativeButton("İptal", null)
            .show()
    }
    
    private fun showInfoDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("KIDEM & İHBAR TAZMİNATI NEDİR?")
            .setMessage("""
                🎁 KIDEM TAZMİNATI (İş Kanunu Mad. 120):
                İş sözleşmesi belirli hallerde sona erdiğinde, 1 yıl ve üzeri çalışan işçiye ödenir.
                
                HAK KAZANMA:
                ✓ İşveren feshi
                ✓ İşçi haklı feshi
                ✓ Emeklilik
                ✓ Askerlik
                ✓ Evlilik (Kadın, 1 yıl içinde)
                
                HAK KAZANILAMAZ:
                ✗ İşçi haksız feshi
                ✗ Deneme süresi feshi
                
                HESAPLAMA:
                • Her tam yıl için 30 günlük brüt ücret
                • Tavan: ${String.format("%,.2f", severanceCeiling)}₺ (${lastUpdate.substring(0,4)})
                • Kesinti: Damga vergisi (%${String.format("%.3f", stampTaxRate * 100)})
                
                ⚖️ İHBAR TAZMİNATI (İş Kanunu Mad. 17):
                İşten çıkarken önceden bildirim (ihbar) verilmezse ödenir.
                
                İHBAR SÜRELERİ:
                • 6 aydan az: 2 hafta
                • 6 ay - 1.5 yıl: 4 hafta
                • 1.5 - 3 yıl: 6 hafta
                • 3 yıl+: 8 hafta
                
                HESAPLAMA:
                • İhbar süresi × Günlük brüt maaş
                • Tavan yok
                • Kesinti: Damga vergisi (%${String.format("%.3f", stampTaxRate * 100)})
            """.trimIndent())
            .setPositiveButton("Tamam", null)
            .show()
    }
    
    private fun showSalaryInfoDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("BRÜT MAAŞ NEDİR?")
            .setMessage("""
                📊 BRÜT vs NET MAAŞ:
                
                Brüt Maaş: Kesintilerden ÖNCE ücret
                Net Maaş: Elinize geçen para
                
                BRÜT MAAŞINIZI NASIL BULURSUNUZ?
                
                1️⃣ Son bordronuza bakın
                2️⃣ "Brüt Ücret" satırını bulun
                3️⃣ Düzenli fazla mesai varsa ekleyin
                
                📌 ÖRNEK:
                Bordrodaki Brüt: 40.000₺
                Düzenli Fazla Mesai: 10.000₺
                ─────────────────────────
                Kıdem için Brüt: 50.000₺
                
                ⚠️ Fazla mesai düzenli değilse (ayda 1-2 kez), eklemeyin.
            """.trimIndent())
            .setPositiveButton("Tamam", null)
            .show()
    }
    
    private fun calculate() {
        if (startDate == null || endDate == null) {
            Toast.makeText(context, "Lütfen tarihleri seçin", Toast.LENGTH_SHORT).show()
            return
        }
        
        val salaryText = inputSalary.text.toString()
        if (salaryText.isEmpty()) {
            Toast.makeText(context, "Lütfen brüt maaşı girin", Toast.LENGTH_SHORT).show()
            return
        }
        
        val salary = salaryText.toDoubleOrNull()
        if (salary == null || salary <= 0) {
            Toast.makeText(context, "Geçerli bir maaş girin", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (endDate!!.before(startDate)) {
            Toast.makeText(context, "Çıkış tarihi giriş tarihinden önce olamaz", Toast.LENGTH_SHORT).show()
            return
        }
        
        val reason = btnReason.text.toString()
        val severanceEligible = !reason.contains("Haksız") && !reason.contains("Deneme")
        
        val diffMillis = endDate!!.timeInMillis - startDate!!.timeInMillis + 86400000
        val totalDays = (diffMillis / 86400000).toInt()
        val years = totalDays / 365
        val remainingDays = totalDays % 365
        val months = remainingDays / 30
        val days = remainingDays % 30
        
        resultDuration.text = "Çalışma Süresi: $years yıl $months ay $days gün"
        
        var severanceGross = 0.0
        var severanceTax = 0.0
        var severanceNet = 0.0
        
        if (severanceEligible) {
            val yearlyAmount = minOf(salary, severanceCeiling)
            severanceGross = yearlyAmount * (totalDays / 365.0)
            severanceTax = severanceGross * stampTaxRate
            severanceNet = severanceGross - severanceTax
        }
        
        resultSeveranceGross.text = "Brüt Kıdem: ${formatMoney(severanceGross)}₺"
        resultSeveranceTax.text = "Damga Vergisi: ${formatMoney(severanceTax)}₺ (%0.759)"
        resultSeveranceNet.text = "Net Kıdem: ${formatMoney(severanceNet)}₺"
        
        val noticeDays = when {
            totalDays < 180 -> 14
            totalDays < 547 -> 28
            totalDays < 1095 -> 42
            else -> 56
        }
        
        var noticeGross = 0.0
        var noticeTax = 0.0
        var noticeNet = 0.0
        
        if (!noticeGiven) {
            val dailySalary = salary / 30
            noticeGross = dailySalary * noticeDays
            noticeTax = noticeGross * stampTaxRate
            noticeNet = noticeGross - noticeTax
        }
        
        val noticeWeeks = noticeDays / 7
        resultNoticePeriod.text = if (noticeGiven) {
            "İhbar Süresi: $noticeWeeks hafta ($noticeDays gün) - Verildi"
        } else {
            "İhbar Süresi: $noticeWeeks hafta ($noticeDays gün)"
        }
        resultNoticeGross.text = "Brüt İhbar: ${formatMoney(noticeGross)}₺"
        resultNoticeTax.text = "Damga Vergisi: ${formatMoney(noticeTax)}₺ (%0.759)"
        resultNoticeNet.text = "Net İhbar: ${formatMoney(noticeNet)}₺"
        
        val totalNet = severanceNet + noticeNet
        resultTotal.text = "TOPLAM NET: ${formatMoney(totalNet)}₺"
        
        val infoText = buildString {
            if (!severanceEligible) {
                append("⚠️ $reason durumunda kıdem tazminatı hakkı yoktur.\n")
            } else if (salary > severanceCeiling) {
                append("ℹ️ Maaş tavandan yüksek, kıdem tavan (${formatMoney(severanceCeiling)}₺) üzerinden hesaplandı.\n")
            }
            if (noticeGiven) {
                append("✓ İhbar verildiği için ihbar tazminatı yok.")
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
            append("🎁 KIDEM & İHBAR TAZMİNATI\n\n")
            append("${resultDuration.text}\n\n")
            append("💰 KIDEM TAZMİNATI:\n")
            append("${resultSeveranceGross.text}\n")
            append("${resultSeveranceTax.text}\n")
            append("${resultSeveranceNet.text}\n\n")
            append("⚖️ İHBAR TAZMİNATI:\n")
            append("${resultNoticePeriod.text}\n")
            append("${resultNoticeGross.text}\n")
            append("${resultNoticeTax.text}\n")
            append("${resultNoticeNet.text}\n\n")
            append("${resultTotal.text}\n\n")
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
        endDate = null
        noticeGiven = false
        btnStartDate.text = "Tarih Seç"
        btnEndDate.text = "Tarih Seç"
        inputSalary.text.clear()
        btnReason.text = "İşveren Feshi"
        btnNotice.text = "Hayır (İhbar verilmedi)"
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

    private fun loadParameters() {
        val prefs = requireContext().getSharedPreferences("work_params", Context.MODE_PRIVATE)
        severanceCeiling = prefs.getFloat("severance_ceiling", 64948.77f).toDouble()
        stampTaxRate = prefs.getFloat("stamp_tax_rate", 0.00759f).toDouble()
        lastUpdate = prefs.getString("last_update", "2026-02-02") ?: "2026-02-02"
        updateStatusText(true)
    }

    private fun fetchParameters() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://raw.githubusercontent.com/hakanerbasss/blue-chip-finance/main/tax_parameters.json")
                val json = url.readText()
                val jsonObject = JSONObject(json)

                val newCeiling = jsonObject.getDouble("severance_ceiling")
                val newStampTax = jsonObject.getDouble("stamp_tax_rate")
                val newUpdate = jsonObject.getString("last_update")

                withContext(Dispatchers.Main) {
                    severanceCeiling = newCeiling
                    stampTaxRate = newStampTax
                    lastUpdate = newUpdate

                    val prefs = requireContext().getSharedPreferences("work_params", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putFloat("severance_ceiling", newCeiling.toFloat())
                        .putFloat("stamp_tax_rate", newStampTax.toFloat())
                        .putString("last_update", newUpdate)
                        .apply()

                    updateStatusText(true)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateStatusText(false)
                }
            }
        }
    }

    private fun updateStatusText(online: Boolean) {
        if (online) {
            updateStatus.text = "📅 Son güncelleme: $lastUpdate"
            updateStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            updateStatus.visibility = View.VISIBLE
            warningText.visibility = View.GONE
        } else {
            updateStatus.visibility = View.GONE
            warningText.visibility = View.VISIBLE
        }
    }
}
