package com.bluechip.finance.fragments
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bluechip.finance.R
import java.io.File
import java.io.FileOutputStream
class OvertimeFragment : Fragment() {
    private lateinit var salaryInput: EditText
    private lateinit var hoursInput: EditText
    private lateinit var methodSwitch: Switch
    private lateinit var methodLabel: TextView
    private lateinit var typeSpinner: Spinner
    private lateinit var calculateButton: Button
    private lateinit var resetButton: Button
    private lateinit var resultCard: View
    private lateinit var resultText: TextView
    private lateinit var shareButton: Button
    private lateinit var infoButton: ImageButton
    private lateinit var scrollView: ScrollView
    private var calculationMethod = 225
    private var lastCalculatedData: CalculationData? = null
    private val overtimeTypes = arrayOf(
        OvertimeType("%25", "Gece Çalışması", 1.25, "Mad. 69", "20:00-06:00 arası gece çalışması"),
        OvertimeType("%50", "Fazla Çalışma", 1.5, "Mad. 41", "Haftalık 45 saati aşan çalışma"),
        OvertimeType("%75", "Gece + Fazla", 1.75, "Mad. 41+69", "Gece saatlerinde fazla çalışma"),
        OvertimeType("%100", "Bayram/Tatil", 2.0, "Mad. 47", "Ulusal bayram ve genel tatil günleri"),
        OvertimeType("%125", "Gece + Tatil", 2.25, "Mad. 47+69", "Tatil günü gece çalışması")
    )
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_overtime, container, false)
        scrollView = view.findViewById(R.id.scroll_view)
        salaryInput = view.findViewById(R.id.salary_input)
        hoursInput = view.findViewById(R.id.hours_input)
        methodSwitch = view.findViewById(R.id.method_switch)
        methodLabel = view.findViewById(R.id.method_label)
        typeSpinner = view.findViewById(R.id.type_spinner)
        calculateButton = view.findViewById(R.id.calculate_button)
        resetButton = view.findViewById(R.id.reset_button)
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
            R.layout.spinner_item,
            overtimeTypes.map { "${it.percentage} - ${it.name}" }
        )
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        typeSpinner.adapter = adapter
        typeSpinner.setSelection(1)
    }
    private fun setupListeners() {
        methodSwitch.setOnCheckedChangeListener { _, isChecked ->
            calculationMethod = if (isChecked) 226 else 225
            methodLabel.text = "$calculationMethod saat"
            if (resultCard.visibility == View.VISIBLE) { calculate() }
        }
        calculateButton.setOnClickListener { calculate() }
        resetButton.setOnClickListener { reset() }
        shareButton.setOnClickListener { shareResult() }
        infoButton.setOnClickListener { showInfo() }
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
        lastCalculatedData = CalculationData(salary, calculationMethod, selectedType, baseRate, overtimeRate, hours, isExampleHours, totalAmount)
        displayResult()
        resultCard.post { scrollView.smoothScrollTo(0, resultCard.top) }
    }
    private fun displayResult() {
        val data = lastCalculatedData ?: return
        val r = StringBuilder()
        r.append("SONUC\n\n")
        r.append("Brut Maas: ${formatMoney(data.salary)} TL\n")
        r.append("Hesaplama: ${data.method} saat\n\n")
        r.append("Birim Ucret:\n")
        r.append("${formatMoney(data.baseRate)} TL/saat\n\n")
        r.append("${data.type.percentage} - ${data.type.name}\n")
        r.append("-------------------\n")
        r.append("Is Kanunu ${data.type.law}\n")
        r.append("${data.type.description}\n\n")
        if (data.type.percentage == "%75") {
            r.append("Hesaplama Detayi:\n")
            r.append("* Gece (%25): +${formatMoney(data.baseRate * 0.25)} TL\n")
            r.append("* Fazla (%50): +${formatMoney(data.baseRate * 0.5)} TL\n")
            r.append("* Toplam: %75 fazla\n\n")
        } else if (data.type.percentage == "%125") {
            r.append("Hesaplama Detayi:\n")
            r.append("* Gece (%25): +${formatMoney(data.baseRate * 0.25)} TL\n")
            r.append("* Tatil (%100): +${formatMoney(data.baseRate * 1.0)} TL\n")
            r.append("* Toplam: %125 fazla\n\n")
        }
        r.append("Saatlik Ucret:\n")
        r.append("${formatMoney(data.overtimeRate)} TL/saat\n\n")
        if (data.isExampleHours) { r.append("Ornek: ${data.hours.toInt()} saat:\n") }
        else { r.append("Toplam (${data.hours.toInt()} saat):\n") }
        r.append("${formatMoney(data.totalAmount)} TL")
        resultText.text = r.toString()
        resultCard.visibility = View.VISIBLE
    }
    private fun reset() {
        salaryInput.text?.clear()
        hoursInput.text?.clear()
        methodSwitch.isChecked = false
        calculationMethod = 225
        methodLabel.text = "225 saat"
        typeSpinner.setSelection(1)
        resultCard.visibility = View.GONE
        lastCalculatedData = null
    }
    private fun shareResult() {
        val data = lastCalculatedData ?: run {
            Toast.makeText(context, "Once hesaplama yapin", Toast.LENGTH_SHORT).show()
            return
        }
        val textShare = createTextShare(data)
        val imageUri = createImageShare(data)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_TEXT, textShare)
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Sonucu Paylas"))
    }
    private fun createTextShare(data: CalculationData): String {
        return """
Fazla Mesai Hesabim
Brut Maas: ${formatMoney(data.salary)} TL
Hesaplama: ${data.method} saat
${data.type.percentage} - ${data.type.name}
(Is Kanunu ${data.type.law})
Birim: ${formatMoney(data.baseRate)} TL/saat
Saatlik: ${formatMoney(data.overtimeRate)} TL/saat
${if (data.isExampleHours) "Ornek " else ""}${data.hours.toInt()} saat: ${formatMoney(data.totalAmount)} TL
Blue Chip Finance ile hesaplandı
        """.trimIndent()
    }
    private fun createImageShare(data: CalculationData): Uri? {
        try {
            val width = 1080
            val height = 1350
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint().apply {
                shader = android.graphics.LinearGradient(0f, 0f, 0f, height.toFloat(), Color.parseColor("#1976D2"), Color.parseColor("#0D47A1"), android.graphics.Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            paint.shader = null
            paint.color = Color.WHITE
            paint.textSize = 80f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("FAZLA MESAİ HESABIM", width / 2f, 150f, paint)
            paint.strokeWidth = 5f
            canvas.drawLine(100f, 200f, width - 100f, 200f, paint)
            paint.textSize = 50f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textAlign = Paint.Align.LEFT
            var y = 320f
            val lineHeight = 80f
            canvas.drawText("Brut Maas: ${formatMoney(data.salary)} TL", 100f, y, paint)
            y += lineHeight
            canvas.drawText("Hesaplama: ${data.method} saat", 100f, y, paint)
            y += lineHeight + 40f
            canvas.drawLine(100f, y, width - 100f, y, paint)
            y += 80f
            paint.textSize = 55f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${data.type.percentage} - ${data.type.name}", 100f, y, paint)
            y += 70f
            paint.textSize = 45f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Is Kanunu ${data.type.law}", 100f, y, paint)
            y += lineHeight + 40f
            canvas.drawText("Birim: ${formatMoney(data.baseRate)} TL/saat", 100f, y, paint)
            y += lineHeight
            paint.textSize = 60f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Saatlik: ${formatMoney(data.overtimeRate)} TL/saat", 100f, y, paint)
            y += lineHeight + 40f
            paint.textSize = 55f
            canvas.drawText("${data.hours.toInt()} saat = ${formatMoney(data.totalAmount)} TL", 100f, y, paint)
            y += lineHeight + 80f
            canvas.drawLine(100f, y, width - 100f, y, paint)
            y += 80f
            paint.textSize = 50f
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Blue Chip Finance", width / 2f, y, paint)
            val file = File(requireContext().cacheDir, "fazla_mesai_${System.currentTimeMillis()}.png")
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.close()
            return FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gorsel olusturulamadi", Toast.LENGTH_SHORT).show()
            return null
        }
    }
    private fun showInfo() {
        val message = """
FAZLA MESAİ TÜRLERİ

%25 - GECE ÇALIŞMASI
Is Kanunu Mad. 69
20:00-06:00 arası
(Maaş / 225) x 1.25

%50 - FAZLA ÇALIŞMA
Is Kanunu Mad. 41
Haftalık 45 saati aşan
(Maaş / 225) x 1.5

%75 - GECE + FAZLA
Gece saatlerinde fazla çalışma
(Maaş / 225) x 1.75

%100 - ULUSAL BAYRAM/TATİL
Is Kanunu Mad. 47
Bayram ve genel tatil günleri
(Maaş / 225) x 2.0

%125 - GECE + TATİL
Tatil günü gece çalışması
(Maaş / 225) x 2.25

DİKKAT: Net tutar için vergi ve SGK kesintileri düşülmelidir.
        """.trimIndent()
        AlertDialog.Builder(requireContext())
            .setTitle("Fazla Mesai Bilgileri")
            .setMessage(message)
            .setPositiveButton("Tamam", null)
            .setNeutralButton("İş Kanunu") { _, _ -> showLawWebView() }
            .show()
    }
    private fun showLawWebView() {
        val webView = WebView(requireContext()).apply {
            webViewClient = WebViewClient()
            loadUrl("https://www.mevzuat.gov.tr/MevzuatMetin/1.5.4857.pdf")
        }
        AlertDialog.Builder(requireContext())
            .setTitle("İş Kanunu")
            .setView(webView)
            .setPositiveButton("Kapat", null)
            .show()
    }
    private fun formatMoney(amount: Double): String {
        return String.format("%,.2f", amount).replace(',', 'X').replace('.', ',').replace('X', '.')
    }
    data class OvertimeType(val percentage: String, val name: String, val multiplier: Double, val law: String, val description: String)
    data class CalculationData(val salary: Double, val method: Int, val type: OvertimeType, val baseRate: Double, val overtimeRate: Double, val hours: Double, val isExampleHours: Boolean, val totalAmount: Double)
}
