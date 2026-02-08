package com.bluechip.finance.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluechip.finance.ui.components.*
import com.bluechip.finance.ui.theme.*
import kotlinx.coroutines.launch

data class OvertimeType(val pct: String, val name: String, val mult: Double, val law: String, val desc: String)

@Composable
fun OvertimeScreen() {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var salary by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var showResult by remember { mutableStateOf(false) }
    var showTypeSheet by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val types = remember { listOf(
        OvertimeType("%25", "Gece Çalışması", 1.25, "Mad. 69", "20:00-06:00 gece çalışması"),
        OvertimeType("%50", "Fazla Çalışma", 1.5, "Mad. 41", "Haftalık 45 saati aşan çalışma"),
        OvertimeType("%75", "Gece + Fazla", 1.75, "Mad. 41+69", "Gece saatlerinde fazla çalışma"),
        OvertimeType("%100", "Bayram/Tatil", 2.0, "Mad. 47", "Ulusal bayram ve genel tatil"),
        OvertimeType("%125", "Gece + Tatil", 2.25, "Mad. 47+69", "Tatil günü gece çalışması")
    )}
    var selectedType by remember { mutableStateOf(types[1]) }
    var resultText by remember { mutableStateOf("") }

    fun calculate() {
        val sal = salary.toDoubleOrNull() ?: return
        val h = hours.toDoubleOrNull() ?: 10.0
        val isExample = hours.isEmpty()
        val base = sal / 225.0
        val rate = base * selectedType.mult
        val total = rate * h
        resultText = buildString {
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("💰  NET MAAŞ: ${formatMoney(sal)} TL\n")
            append("⚙️  Hesaplama: 225 saat\n")
            append("━━━━━━━━━━━━━━━━━━━━\n\n")
            append("💵  Birim Ücret: ${formatMoney(base)} TL/saat\n\n")
            append("📌  ${selectedType.pct} - ${selectedType.name}\n")
            append("    İş Kanunu ${selectedType.law}\n\n")
            append("💎  Saatlik: ${formatMoney(rate)} TL/saat\n\n")
            append(if (isExample) "📈  Örnek (${h.toInt()} saat)\n" else "📈  Toplam (${h.toInt()} saat)\n")
            append("    ${formatMoney(total)} TL\n")
            append("━━━━━━━━━━━━━━━━━━━━")
        }
        showResult = true
        scope.launch { scrollState.animateScrollTo(scrollState.maxValue) }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionHeader("FAZLA MESAİ HESAPLAMA", Icons.Default.AccessTime, onInfoClick = { showInfoDialog = true })
        CurrencyField(value = salary, onValueChange = { salary = it }, label = "Net Maaş (₺)")
        NumberField(value = hours, onValueChange = { hours = it }, label = "Fazla Mesai Saati (boş = 10 örnek)")
        SelectorButton("${selectedType.pct} - ${selectedType.name}") { showTypeSheet = true }
        ActionButtons(onCalculate = { calculate() }, onReset = { salary = ""; hours = ""; showResult = false; selectedType = types[1] })
        ResultCard(visible = showResult) {
            Text("SONUÇ", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
            Spacer(Modifier.height(8.dp))
            Text(resultText, fontSize = 13.sp, lineHeight = 20.sp, color = colors.textPrimary)
            Spacer(Modifier.height(12.dp))
            ShareButton {
                val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "💰 FAZLA MESAİ\n\n$resultText\n\n📱 Blue Chip Finance") }
                context.startActivity(Intent.createChooser(intent, "Paylaş"))
            }
        }
    }

    if (showTypeSheet) {
        AlertDialog(onDismissRequest = { showTypeSheet = false }, title = { Text("Fazla Mesai Türü") }, text = {
            Column { types.forEach { type -> Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                RadioButton(selected = selectedType == type, onClick = { selectedType = type; showTypeSheet = false })
                Spacer(Modifier.width(8.dp))
                Column { Text("${type.pct} - ${type.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(type.desc, fontSize = 12.sp, color = colors.textSecondary) }
            } } }
        }, confirmButton = { TextButton(onClick = { showTypeSheet = false }) { Text("Kapat") } })
    }

    if (showInfoDialog) {
        AlertDialog(onDismissRequest = { showInfoDialog = false }, title = { Text("Fazla Mesai Bilgileri") }, text = {
            Text("%25 Gece (Mad.69) × 1.25\n%50 Fazla (Mad.41) × 1.50\n%75 Gece+Fazla × 1.75\n%100 Bayram (Mad.47) × 2.00\n%125 Gece+Tatil × 2.25\n\n⚠️ Net tutar için vergi/SGK düşülmelidir.", fontSize = 13.sp)
        }, confirmButton = { TextButton(onClick = { showInfoDialog = false }) { Text("Tamam") } })
    }
}
