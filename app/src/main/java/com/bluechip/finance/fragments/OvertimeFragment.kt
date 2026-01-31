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
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bluechip.finance.R
import java.io.File
import java.io.FileOutputStream

class OvertimeFragment : Fragment() {
    private lateinit var salaryInput: EditText
    private lateinit var hoursInput: EditText
    private lateinit var typeButton: Button
    private lateinit var calculateButton: Button
    private lateinit var resetButton: Button
    private lateinit var resultCard: View
    private lateinit var resultText: TextView
    private lateinit var shareButton: Button
    private lateinit var infoButton: ImageButton
    private lateinit var scrollView: ScrollView
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
        typeButton = view.findViewById(R.id.type_button)
        calculateButton = view.findViewById(R.id.calculate_button)
        resetButton = view.findViewById(R.id.reset_button)
        resultCard = view.findViewById(R.id.result_card)
        resultText = view.findViewById(R.id.result_text)
        shareButton = view.findViewById(R.id.share_button)
        infoButton = view.findViewById(R.id.info_button)
        setupListeners()
        setupInfoIcon()
        return view
    }

    private fun setupInfoIcon() {
        val colorFilter = android.graphics.PorterDuffColorFilter(Color.parseColor("#1976D2"), android.graphics.PorterDuff.Mode.SRC_IN)
        infoButton.drawable?.colorFilter = colorFilter
    }

    private fun setupListeners() {
        typeButton.setOnClickListener { showTypeDialog() }
        calculateButton.setOnClickListener { calculate() }
        resetButton.setOnClickListener { reset() }
        shareButton.setOnClickListener { shareResult() }
        infoButton.setOnClickListener { showInfo() }
    }

    private fun showTypeDialog() {
        val types = overtimeTypes.map { "${it.percentage} - ${it.name}" }.toTypedArray()
        val currentText = typeButton.text.toString()
        val currentIndex = overtimeTypes.indexOfFirst { "${it.percentage} - ${it.name}" == currentText }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Fazla Mesai Türü Seçin")
            .setSingleChoiceItems(types, if (currentIndex >= 0) currentIndex else 1) { dialog, which ->
                val selected = overtimeTypes[which]
                typeButton.text = "${selected.percentage} - ${selected.name}"
                if (resultCard.visibility == View.VISIBLE) { calculate() }
                dialog.dismiss()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun calculate() {
        val salaryText = salaryInput.text.toString()
        if (salaryText.isEmpty()) {
            Toast.makeText(context, "Lütfen net maaş giriniz", Toast.LENGTH_SHORT).show()
            return
        }
        val salary = salaryText.toDoubleOrNull()
        if (salary == null || salary <= 0) {
            Toast.makeText(context, "Geçerli bir net maaş giriniz", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedText = typeButton.text.toString()
        val selectedType = overtimeTypes.firstOrNull { "${it.percentage} - ${it.name}" == selectedText }
            ?: overtimeTypes[1]

        val baseRate = salary / 225.0
        val overtimeRate = baseRate * selectedType.multiplier
        val hoursText = hoursInput.text.toString()
        val hours = hoursText.toDoubleOrNull() ?: 10.0
        val isExampleHours = hoursText.isEmpty()
        val totalAmount = overtimeRate * hours

        lastCalculatedData = CalculationData(salary, selectedType, baseRate, overtimeRate, hours, isExampleHours, totalAmount)
        displayResult()

        // Klavye kapat
        val imm = activity?.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)

        // Scroll to result
        resultCard.post { scrollView.smoothScrollTo(0, resultCard.top) }
    }

    private fun displayResult() {
        val data = lastCalculatedData ?: return
        val r = StringBuilder()
        r.append("━━━━━━━━━━━━━━━━━━━━\n")
        r.append("💰  NET MAAŞ: ${formatMoney(data.salary)} TL\n")
        r.append("⚙️  Hesaplama: 225 saat\n")
        r.append("━━━━━━━━━━━━━━━━━━━━\n\n")
        r.append("💵  Birim Ücret\n")
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
        typeButton.text = "%50 - Fazla Çalışma"
        resultCard.visibility = View.GONE
        lastCalculatedData = null
    }

    private fun shareResult() {
        val data = lastCalculatedData ?: run {
            Toast.makeText(context, "Önce hesaplama yapın", Toast.LENGTH_SHORT).show()
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
        startActivity(Intent.createChooser(shareIntent, "Sonucu Paylaş"))
    }

    private fun createTextShare(data: CalculationData): String {
        return """
╔══════════════════════════════╗
║   💰 FAZLA MESAİ HESABIM    ║
╠══════════════════════════════╣
║  Net Maaş   : ${formatMoney(data.salary)} TL
║  Yöntem     : 225 saat
║  Tür        : ${data.type.percentage} ${data.type.name}
║  İş Kanunu  : ${data.type.law}
╠══════════════════════════════╣
║  Birim Ücret: ${formatMoney(data.baseRate)} TL
║  Saatlik    : ${formatMoney(data.overtimeRate)} TL
║  ${if (data.isExampleHours) "Örnek" else "Toplam"} (${data.hours.toInt()}h): ${formatMoney(data.totalAmount)} TL
╠══════════════════════════════╣
║  📱 Blue Chip Finance        ║
║  play.google.com/store/      ║
║  apps/details?id=            ║
║  com.bluechip.finance        ║
╚══════════════════════════════╝
        """.trimIndent()
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
            canvas.drawText("225 saat", 140f, y, valuePaint); y += 70f
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
            Toast.makeText(context, "Görsel oluşturulamadı", Toast.LENGTH_SHORT).show()
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
    data class CalculationData(val salary: Double, val type: OvertimeType, val baseRate: Double, val overtimeRate: Double, val hours: Double, val isExampleHours: Boolean, val totalAmount: Double)
}
