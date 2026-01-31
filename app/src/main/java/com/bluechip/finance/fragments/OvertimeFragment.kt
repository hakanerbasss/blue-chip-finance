package com.bluechip.finance.fragments
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bluechip.finance.R
class OvertimeFragment : Fragment() {
    private lateinit var salaryInput: EditText
    private lateinit var hoursInput: EditText
    private lateinit var methodSwitch: Switch
    private lateinit var methodLabel: TextView
    private lateinit var typeSpinner: Spinner
    private lateinit var calculateButton: Button
    private lateinit var resultCard: View
    private lateinit var resultText: TextView
    private lateinit var shareButton: Button
    private lateinit var infoButton: ImageButton
    private var calculationMethod = 225
    private var lastCalculatedResult = ""
    private val overtimeTypes = arrayOf(
        OvertimeType("%25", "Gece Çalışması", 1.25, "Mad. 69", "20:00-06:00 arası gece çalışması"),
        OvertimeType("%50", "Fazla Çalışma", 1.5, "Mad. 41", "Haftalık 45 saati aşan çalışma"),
        OvertimeType("%75", "Gece + Fazla", 1.75, "Mad. 41+69", "Gece saatlerinde fazla çalışma"),
        OvertimeType("%100", "Bayram/Tatil", 2.0, "Mad. 47", "Ulusal bayram ve genel tatil günleri"),
        OvertimeType("%125", "Gece + Tatil", 2.25, "Mad. 47+69", "Tatil günü gece çalışması")
    )
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_overtime, container, false)
        salaryInput = view.findViewById(R.id.salary_input)
        hoursInput = view.findViewById(R.id.hours_input)
        methodSwitch = view.findViewById(R.id.method_switch)
        methodLabel = view.findViewById(R.id.method_label)
        typeSpinner = view.findViewById(R.id.type_spinner)
        calculateButton = view.findViewById(R.id.calculate_button)
        resultCard = view.findViewById(R.id.result_card)
        resultText = view.findViewById(R.id.result_text)
        shareButton = view.findViewById(R.id.share_button)
        infoButton = view.findViewById(R.id.info_button)
        setupSpinner()
        setupListeners()
        return view
    }
    private fun setupSpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            overtimeTypes.map { "${it.percentage} - ${it.name}" }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = adapter
        typeSpinner.setSelection(1)
    }
    private fun setupListeners() {
        methodSwitch.setOnCheckedChangeListener { _, isChecked ->
            calculationMethod = if (isChecked) 226 else 225
            methodLabel.text = "$calculationMethod saat"
            if (resultCard.visibility == View.VISIBLE) {
                calculate()
            }
        }
        calculateButton.setOnClickListener {
            calculate()
        }
        shareButton.setOnClickListener {
            shareResult()
        }
        infoButton.setOnClickListener {
            showInfo()
        }
    }
    private fun calculate() {
        val salaryText = salaryInput.text.toString()
        if (salaryText.isEmpty()) {
            Toast.makeText(context, "Lutfen maas giriniz", Toast.LENGTH_SHORT).show()
            return
        }
        val salary = salaryText.toDoubleOrNull()
        if (salary == null || salary <= 0) {
            Toast.makeText(context, "Gecerli bir maas giriniz", Toast.LENGTH_SHORT).show()
            return
        }
        val selectedType = overtimeTypes[typeSpinner.selectedItemPosition]
        val baseRate = salary / calculationMethod
        val overtimeRate = baseRate * selectedType.multiplier
        val hoursText = hoursInput.text.toString()
        val hours = hoursText.toDoubleOrNull() ?: 10.0
        val isExampleHours = hoursText.isEmpty()
        val totalAmount = overtimeRate * hours
        val resultBuilder = StringBuilder()
        resultBuilder.append("SONUC\n\n")
        resultBuilder.append("Brut Maas: ${formatMoney(salary)} TL\n")
        resultBuilder.append("Hesaplama: $calculationMethod saat\n\n")
        resultBuilder.append("Birim Ucret:\n")
        resultBuilder.append("${formatMoney(baseRate)} TL/saat\n\n")
        resultBuilder.append("${selectedType.percentage} - ${selectedType.name}\n")
        resultBuilder.append("-------------------\n")
        resultBuilder.append("Is Kanunu ${selectedType.law}\n")
        resultBuilder.append("${selectedType.description}\n\n")
        if (selectedType.percentage == "%75") {
            resultBuilder.append("Hesaplama Detayi:\n")
            resultBuilder.append("• Gece (%25): +${formatMoney(baseRate * 0.25)} TL\n")
            resultBuilder.append("• Fazla (%50): +${formatMoney(baseRate * 0.5)} TL\n")
            resultBuilder.append("• Toplam: %75 fazla\n\n")
        } else if (selectedType.percentage == "%125") {
            resultBuilder.append("Hesaplama Detayi:\n")
            resultBuilder.append("• Gece (%25): +${formatMoney(baseRate * 0.25)} TL\n")
            resultBuilder.append("• Tatil (%100): +${formatMoney(baseRate * 1.0)} TL\n")
            resultBuilder.append("• Toplam: %125 fazla\n\n")
        }
        resultBuilder.append("Saatlik Ucret:\n")
        resultBuilder.append("${formatMoney(overtimeRate)} TL/saat\n\n")
        if (isExampleHours) {
            resultBuilder.append("Ornek: ${hours.toInt()} saat:\n")
        } else {
            resultBuilder.append("Toplam (${hours.toInt()} saat):\n")
        }
        resultBuilder.append("${formatMoney(totalAmount)} TL")
        resultText.text = resultBuilder.toString()
        resultCard.visibility = View.VISIBLE
        lastCalculatedResult = """
Fazla Mesai Hesabim

Brut Maas: ${formatMoney(salary)} TL
Hesaplama: $calculationMethod saat

${selectedType.percentage} - ${selectedType.name}
(Is Kanunu ${selectedType.law})

Birim Ucret: ${formatMoney(baseRate)} TL/saat
Saatlik: ${formatMoney(overtimeRate)} TL/saat
${if (isExampleHours) "Ornek " else ""}${hours.toInt()} saat: ${formatMoney(totalAmount)} TL

Blue Chip Finance ile hesaplandı
        """.trimIndent()
    }
    private fun shareResult() {
        if (lastCalculatedResult.isEmpty()) {
            Toast.makeText(context, "Önce hesaplama yapın", Toast.LENGTH_SHORT).show()
            return
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, lastCalculatedResult)
        }
        startActivity(Intent.createChooser(shareIntent, "Sonucu Paylaş"))
    }
    private fun showInfo() {
        val message = """
FAZLA MESAİ TÜRLERİ

%25 - GECE ÇALIŞMASI
--------------------
İş Kanunu Mad. 69
20:00-06:00 arası
(Maaş ÷ 225) × 1.25

%50 - FAZLA ÇALIŞMA
--------------------
İş Kanunu Mad. 41
Haftalık 45 saati aşan
(Maaş ÷ 225) × 1.5

%75 - GECE + FAZLA
--------------------
Gece saatlerinde fazla çalışma
(Maaş ÷ 225) × 1.75
(%25 gece + %50 fazla)

%100 - ULUSAL BAYRAM/TATİL
--------------------------
İş Kanunu Mad. 47
Bayram ve genel tatil günleri
(Maaş ÷ 225) × 2.0

%125 - GECE + TATİL
--------------------
Tatil günü gece çalışması
(Maaş ÷ 225) × 2.25
(%25 gece + %100 tatil)

DİKKAT:
- Net tutar için vergi ve SGK kesintileri düşülmelidir
- Toplu iş sözleşmeleri daha yüksek oranlar içerebilir
        """.trimIndent()
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Fazla Mesai Bilgileri")
            .setMessage(message)
            .setPositiveButton("Tamam", null)
            .setNeutralButton("İş Kanunu") { _, _ ->
                openLawUrl()
            }
            .create()
        dialog.show()
    }
    private fun openLawUrl() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.mevzuat.gov.tr/MevzuatMetin/1.5.4857.pdf"))
        startActivity(intent)
    }
    private fun formatMoney(amount: Double): String {
        return String.format("%,.2f", amount).replace(',', 'X').replace('.', ',').replace('X', '.')
    }
    data class OvertimeType(
        val percentage: String,
        val name: String,
        val multiplier: Double,
        val law: String,
        val description: String
    )
}
