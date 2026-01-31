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
import android.widget.*
import android.graphics.drawable.ColorStateList
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
        setupInfoIcon()
        setupSwitchColors()
        return view
    }
    private fun setupInfoIcon() {
        val colorFilter = android.graphics.PorterDuffColorFilter(Color.parseColor("#1976D2"), android.graphics.PorterDuff.Mode.SRC_IN)
        infoButton.drawable?.colorFilter = colorFilter
    }
    private fun setupSwitchColors() {
        val context = requireContext()
        val isDark = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        if (isDark) {
            methodSwitch.trackTintList = trackColor
            methodSwitch.thumbTintList = thumbColor
        }
    }
    private fun setupSpinner() {
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, overtimeTypes.map { "${it.percentage} - ${it.name}" })
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
            Toast.makeText(context, "Lutfen net maas giriniz", Toast.LENGTH_SHORT).show()
            return
        }
        val salary = salaryText.toDoubleOrNull()
        if (salary == null || salary <= 0) {
            Toast.makeText(context, "Gecerli bir net maas giriniz", Toast.LENGTH_SHORT).show()
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
        r.append("━━━━━━━━━━━━━━━━━━━━\n")
        r.append("💰  NET MAAS: ${formatMoney(data.salary)} TL\n")
        r.append("⚙️  Hesaplama: ${data.method} saat\n")
        r.append("━━━━━━━━━━━━━━━━━━━━\n\n")
        r.append("💵  Birim Ucret\n")
        r.append("    ${formatMoney(data.baseRate)} TL / saat\n\n")
        r.append("📌  ${data.type.percentage} - ${data.type.name}\n")
        r.append("    İş Kanunu ${data.type.law}\n")
        r.append("    ${data.type.description}\n\n")
        if (data.type.percentage == "%75") {
            r.append("📊  Hesaplama Detayı:\n")
            r.append("    • Gece  (%25) : +${formatMoney(data.baseRate * 0.25)} TL\n")
            r.append("    • Fazla (%50) : +${formatMoney(data.baseRate * 0.5)} TL\n")
            r.append("    • Toplam      : %75 fazla\n\n")
        } else if (data.type.percentage == "%125") {
            r.append("📊  Hesaplama Detayı:\n")
            r.append("    • Gece  (%25)  : +${formatMoney(data.baseRate * 0.25)} TL\n")
            r.append("    • Tatil (%100) : +${formatMoney(data.baseRate * 1.0)} TL\n")
            r.append("    • Toplam       : %125 fazla\n\n")
        }
        r.append("━━━━━━━━━━━━━━━━━━━━\n")
        r.append("💎  Saatlik Ücret\n")
        r.append("    ${formatMoney(data.overtimeRate)} TL / saat\n\n")
        if (data.isExampleHours) { r.append("📈  Örnek (${data.hours.toInt()} saat)\n") }
        else { r.append("📈  Toplam (${data.hours.toInt()} saat)\n") }
        r.append("    ${formatMoney(data.totalAmount)} TL\n")
        r.append("━━━━━━━━━━━━━━━━━━━━")
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
╔══════════════════════════════╗
║   💰 FAZLA MESAİ HESABIM    ║
╠══════════════════════════════╣
║  Net Maaş   : ${padEnd(formatMoney(data.salary) + " TL", 14)}║
║  Yöntem     : ${padEnd("${data.method} saat", 14)}║
║  Tür        : ${padEnd("${data.type.percentage} ${data.type.name}", 14)}║
║  İş Kanunu  : ${padEnd(data.type.law, 14)}║
╠══════════════════════════════╣
║  Birim Ücret: ${padEnd(formatMoney(data.baseRate) + " TL", 14)}║
║  Saatlik    : ${padEnd(formatMoney(data.overtimeRate) + " TL", 14)}║
║  ${if (data.isExampleHours) "Örnek" else "Toplam"} (${data.hours.toInt()}h): ${padEnd(formatMoney(data.totalAmount) + " TL", 13)}║
╠══════════════════════════════╣
║  📱 Blue Chip Finance        ║
║  play.google.com/store/      ║
║  apps/details?id=            ║
║  com.bluechip.finance        ║
╚══════════════════════════════╝
        """.trimIndent()
    }
    private fun padEnd(text: String, length: Int): String {
        return if (text.length >= length) text else text + " ".repeat(length - text.length)
    }
    private fun createImageShare(data: CalculationData): Uri? {
        try {
            val width = 1080
            val height = 1350
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val bgPaint = Paint().apply {
                shader = android.graphics.LinearGradient(0f, 0f, 0f, height.toFloat(), Color.parseColor("#0D47A1"), Color.parseColor("#1565C0"), android.graphics.Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
            val cardPaint = Paint().apply { color = Color.WHITE; maskFilter = android.graphics.BlurMaskFilter(30f, android.graphics.BlurMaskFilter.Blur.NORMAL) }
            canvas.drawRoundRect(60f, 80f, 1020f, 680f, 40f, 40f, cardPaint)
            cardPaint.maskFilter = null; cardPaint.color = Color.WHITE
            canvas.drawRoundRect(60f, 80f, 1020f, 680f, 40f, 40f, cardPaint)
            val cardPaint2 = Paint().apply { color = Color.WHITE; maskFilter = android.graphics.BlurMaskFilter(30f, android.graphics.BlurMaskFilter.Blur.NORMAL) }
            canvas.drawRoundRect(60f, 720f, 1020f, 1180f, 40f, 40f, cardPaint2)
            cardPaint2.maskFilter = null; cardPaint2.color = Color.WHITE
            canvas.drawRoundRect(60f, 720f, 1020f, 1180f, 40f, 40f, cardPaint2)
            val titlePaint = Paint().apply { color = Color.parseColor("#0D47A1"); textSize = 72f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER }
            canvas.drawText("FAZLA MESAİ HESABIM", 540f, 170f, titlePaint)
            val linePaint = Paint().apply { color = Color.parseColor("#1976D2"); strokeWidth = 6f }
            canvas.drawLine(200f, 200f, 880f, 200f, linePaint)
            val labelPaint = Paint().apply { color = Color.parseColor("#757575"); textSize = 40f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); textAlign = Paint.Align.LEFT }
            val valuePaint = Paint().apply { color = Color.parseColor("#212121"); textSize = 44f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.LEFT }
            var y = 290f
            canvas.drawText("NET MAAŞ", 140f, y, labelPaint); y += 50f
            canvas.drawText("${formatMoney(data.salary)} TL", 140f, y, valuePaint); y += 70f
            canvas.drawText("HESAPLAMA YÖNTEMI", 140f, y, labelPaint); y += 50f
            canvas.drawText("${data.method} saat", 140f, y, valuePaint); y += 70f
            canvas.drawText("FAZLA MESAİ TÜRÜ", 140f, y, labelPaint); y += 50f
            canvas.drawText("${data.type.percentage} - ${data.type.name}", 140f, y, valuePaint); y += 50f
            canvas.drawText("İş Kanunu ${data.type.law}", 140f, y, labelPaint)
            y = 810f
            canvas.drawText("BİRİM ÜCRET", 140f, y, labelPaint); y += 50f
            canvas.drawText("${formatMoney(data.baseRate)} TL / saat", 140f, y, valuePaint); y += 80f
            canvas.drawText("SAATLIK ÜCRET", 140f, y, labelPaint); y += 50f
            val saatlikPaint = Paint().apply { color = Color.parseColor("#1976D2"); textSize = 56f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.LEFT }
            canvas.drawText("${formatMoney(data.overtimeRate)} TL / saat", 140f, y, saatlikPaint); y += 80f
            canvas.drawText("${if (data.isExampleHours) "ÖRNEK" else "TOPLAM"} (${data.hours.toInt()} SAAT)", 140f, y, labelPaint); y += 55f
            val totalPaint = Paint().apply { color = Color.parseColor("#0D47A1"); textSize = 64f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.LEFT }
            canvas.drawText("${formatMoney(data.totalAmount)} TL", 140f, y, totalPaint)
            val footerPaint = Paint().apply { color = Color.WHITE; textSize = 38f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER }
            canvas.drawText("Blue Chip Finance", 540f, 1260f, footerPaint)
            val footerPaint2 = Paint().apply { color = Color.parseColor("#BBDEFB"); textSize = 32f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); textAlign = Paint.Align.CENTER }
            canvas.drawText("play.google.com/store/apps/details?id=com.bluechip.finance", 540f, 1305f, footerPaint2)
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
        val inflater = android.view.LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.info_dialog_layout, null)
        AlertDialog.Builder(requireContext())
            .setTitle("Fazla Mesai Bilgileri")
            .setView(view)
            .setPositiveButton("Tamam", null)
            .setNeutralButton("İş Kanunu") { _, _ -> openLawInBrowser() }
            .show()
    }
    private fun openLawInBrowser() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.mevzuat.gov.tr/MevzuatMetin/1.5.4857.pdf"))
        startActivity(intent)
    }
    private fun formatMoney(amount: Double): String {
        return String.format("%,.2f", amount).replace(',', 'X').replace('.', ',').replace('X', '.')
    }
    data class OvertimeType(val percentage: String, val name: String, val multiplier: Double, val law: String, val description: String)
    data class CalculationData(val salary: Double, val method: Int, val type: OvertimeType, val baseRate: Double, val overtimeRate: Double, val hours: Double, val isExampleHours: Boolean, val totalAmount: Double)
}
