package com.bluechip.finance.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bluechip.finance.data.Article
import com.bluechip.finance.data.NewsApiService
import com.bluechip.finance.ui.components.formatMoney
import com.bluechip.finance.ui.components.formatNumber
import com.bluechip.finance.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val colors = LocalAppColors.current

    var usdRate by remember { mutableDoubleStateOf(0.0) }
    var eurRate by remember { mutableDoubleStateOf(0.0) }
    var goldUSD by remember { mutableDoubleStateOf(0.0) }
    var btcUSD by remember { mutableDoubleStateOf(0.0) }
    var ethUSD by remember { mutableDoubleStateOf(0.0) }
    var isTL by remember { mutableStateOf(true) }
    var updateTime by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    var articles by remember { mutableStateOf<List<Article>>(emptyList()) }
    var newsStatus by remember { mutableStateOf("Haberler yükleniyor...") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val tcmb = URL("https://www.tcmb.gov.tr/kurlar/today.xml").readText()
                val usdMatch = Regex("<Currency[^>]*CurrencyCode=\"USD\"[^>]*>.*?<ForexSelling>([0-9.]+)</ForexSelling>", RegexOption.DOT_MATCHES_ALL).find(tcmb)
                val eurMatch = Regex("<Currency[^>]*CurrencyCode=\"EUR\"[^>]*>.*?<ForexSelling>([0-9.]+)</ForexSelling>", RegexOption.DOT_MATCHES_ALL).find(tcmb)
                if (usdMatch != null) usdRate = usdMatch.groupValues[1].toDouble()
                if (eurMatch != null) eurRate = eurMatch.groupValues[1].toDouble()
            } catch (_: Exception) { usdRate = 32.5; eurRate = 35.2 }
            try {
                val crypto = URL("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,tether-gold&vs_currencies=usd").readText()
                val json = JSONObject(crypto)
                btcUSD = json.getJSONObject("bitcoin").getDouble("usd")
                ethUSD = json.getJSONObject("ethereum").getDouble("usd")
                goldUSD = json.getJSONObject("tether-gold").getDouble("usd")
            } catch (_: Exception) { btcUSD = 64200.0; ethUSD = 3100.0; goldUSD = 2650.0 }
            isLoading = false
            updateTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        }
        withContext(Dispatchers.IO) {
            try {
                val response = NewsApiService.create().getNews()
                articles = response.articles.take(3)
                newsStatus = "Son ${articles.size} haber (${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())})"
            } catch (e: Exception) { newsStatus = "Haber yüklenemedi" }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Piyasa Verileri
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBg)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Piyasa Verileri", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Text(if (isTL) "₺ TL" else "$ USD", fontSize = 12.sp, color = colors.textSecondary)
                    Switch(checked = !isTL, onCheckedChange = { isTL = !it }, modifier = Modifier.padding(start = 4.dp))
                }
                Spacer(Modifier.height(12.dp))
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    val s = if (isTL) "₺" else "$"
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        PriceChip("USD", "${formatMoney(if (isTL) usdRate else 1.0)}$s", Color(0xFF4CAF50))
                        PriceChip("EUR", "${formatMoney(if (isTL) eurRate else eurRate/usdRate)}$s", Color(0xFF2196F3))
                        PriceChip("Altın", "${formatMoney(if (isTL) goldUSD*usdRate else goldUSD)}$s", Color(0xFFFF9800))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        PriceChip("BTC", "${formatNumber(if (isTL) btcUSD*usdRate else btcUSD)}$s", Color(0xFFF7931A))
                        PriceChip("ETH", "${formatNumber(if (isTL) ethUSD*usdRate else ethUSD)}$s", Color(0xFF627EEA))
                    }
                }
                if (updateTime.isNotEmpty()) {
                    Text("Son güncelleme: $updateTime", fontSize = 11.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        // Hesaplama Kartları - DÜZELTME: Kenarlıklar küçültüldü
        Text("Hesaplamalar", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalcCard("Fazla Mesai", Icons.Default.AccessTime, Color(0xFF1565C0), Modifier.weight(1f)) { onNavigate("overtime") }
            CalcCard("Kıdem/İhbar", Icons.Default.AccountBalance, Color(0xFF2E7D32), Modifier.weight(1f)) { onNavigate("severance") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalcCard("Vergi Dilimi", Icons.Default.Receipt, Color(0xFFE65100), Modifier.weight(1f)) { onNavigate("tax") }
            CalcCard("Yıllık İzin", Icons.Default.DateRange, Color(0xFF6A1B9A), Modifier.weight(1f)) { onNavigate("leave") }
        }

        // Haberler
        Text("Ekonomi Haberleri", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
        Text(newsStatus, fontSize = 12.sp, color = colors.textSecondary)

        articles.forEach { article ->
            NewsCard(article = article, onClick = {
                // DÜZELTME: Tam ekran browser aç (dialog yerine)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                context.startActivity(intent)
            })
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PriceChip(name: String, price: String, color: Color) {
    val colors = LocalAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, fontSize = 11.sp, color = colors.textSecondary)
        Text(price, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun CalcCard(title: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val appColors = LocalAppColors.current
    val bgColor = if (appColors.isDark) color.copy(alpha = 0.15f) else color.copy(alpha = 0.08f)

    Card(
        modifier = modifier.height(80.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}

@Composable
private fun NewsCard(article: Article, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBg)
    ) {
        Column {
            if (!article.urlToImage.isNullOrEmpty() && article.urlToImage != "null") {
                AsyncImage(
                    model = article.urlToImage, contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(article.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(article.description ?: "Detaylar için tıklayın", fontSize = 12.sp, color = colors.info, maxLines = 2)
            }
        }
    }
}
