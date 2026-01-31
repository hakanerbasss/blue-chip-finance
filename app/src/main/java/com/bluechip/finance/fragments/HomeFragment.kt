package com.bluechip.finance.fragments

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.fragment.app.Fragment
import com.bluechip.finance.R
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {
    private lateinit var priceUsd: TextView
    private lateinit var priceEur: TextView
    private lateinit var priceGold: TextView
    private lateinit var priceSilver: TextView
    private lateinit var priceBtc: TextView
    private lateinit var priceEth: TextView
    private lateinit var priceUpdateTime: TextView
    private lateinit var btnRefresh: Button
    private lateinit var newsContainer: LinearLayout
    private lateinit var currencySwitch: Switch
    private lateinit var silverInfo: TextView
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var usdRate = 32.5
    private var eurRate = 35.2
    private var goldGramTL = 2450.0
    private var btcUSD = 64200.0
    private var ethUSD = 3100.0
    private var isTL = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        
        priceUsd = view.findViewById(R.id.price_usd)
        priceEur = view.findViewById(R.id.price_eur)
        priceGold = view.findViewById(R.id.price_gold)
        priceSilver = view.findViewById(R.id.price_silver)
        priceBtc = view.findViewById(R.id.price_btc)
        priceEth = view.findViewById(R.id.price_eth)
        priceUpdateTime = view.findViewById(R.id.price_update_time)
        btnRefresh = view.findViewById(R.id.btn_refresh_prices)
        newsContainer = view.findViewById(R.id.news_container)
        currencySwitch = view.findViewById(R.id.currency_switch)
        silverInfo = view.findViewById(R.id.silver_info)
        
        // Modül kartları
        view.findViewById<MaterialCardView>(R.id.card_overtime).setOnClickListener {
            navigateToOvertime()
        }
        
        view.findViewById<MaterialCardView>(R.id.card_agi).setOnClickListener {
            Toast.makeText(context, "AGİ modülü yakında eklenecek", Toast.LENGTH_SHORT).show()
        }
        
        view.findViewById<MaterialCardView>(R.id.card_tax).setOnClickListener {
            Toast.makeText(context, "Vergi modülü yakında eklenecek", Toast.LENGTH_SHORT).show()
        }
        
        view.findViewById<MaterialCardView>(R.id.card_severance).setOnClickListener {
            Toast.makeText(context, "Kıdem modülü yakında eklenecek", Toast.LENGTH_SHORT).show()
        }
        
        btnRefresh.setOnClickListener {
            loadPrices()
            loadNews()
        }
        
        currencySwitch.setOnCheckedChangeListener { _, isChecked ->
            isTL = !isChecked
            updatePriceDisplay()
        }
        
        silverInfo.setOnClickListener {
            showSilverInfo()
        }
        
        loadPrices()
        loadNews()
        
        return view
    }
    
    private fun navigateToOvertime() {
        val fragment = OvertimeFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
    
    private fun loadPrices() {
        scope.launch {
            try {
                btnRefresh.isEnabled = false
                
                // TCMB API
                val tcmbData = withContext(Dispatchers.IO) {
                    try {
                        URL("https://www.tcmb.gov.tr/kurlar/today.xml").readText()
                    } catch (e: Exception) {
                        null
                    }
                }
                
                if (tcmbData != null) {
                    parseTCMBData(tcmbData)
                }
                
                // CoinGecko API
                val cryptoData = withContext(Dispatchers.IO) {
                    try {
                        URL("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum&vs_currencies=usd").readText()
                    } catch (e: Exception) {
                        null
                    }
                }
                
                if (cryptoData != null) {
                    val json = JSONObject(cryptoData)
                    btcUSD = json.getJSONObject("bitcoin").getDouble("usd")
                    ethUSD = json.getJSONObject("ethereum").getDouble("usd")
                }
                
                updatePriceDisplay()
                
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                priceUpdateTime.text = "Son güncelleme: $time"
                
            } catch (e: Exception) {
                Toast.makeText(context, "Fiyatlar yüklenemedi", Toast.LENGTH_SHORT).show()
            } finally {
                btnRefresh.isEnabled = true
            }
        }
    }
    
    private fun parseTCMBData(xml: String) {
        try {
            val usdMatch = Regex("<Currency[^>]*CurrencyCode=\"USD\"[^>]*>.*?<ForexSelling>([0-9.]+)</ForexSelling>", RegexOption.DOT_MATCHES_ALL).find(xml)
            if (usdMatch != null) {
                usdRate = usdMatch.groupValues[1].toDouble()
            }
            
            val eurMatch = Regex("<Currency[^>]*CurrencyCode=\"EUR\"[^>]*>.*?<ForexSelling>([0-9.]+)</ForexSelling>", RegexOption.DOT_MATCHES_ALL).find(xml)
            if (eurMatch != null) {
                eurRate = eurMatch.groupValues[1].toDouble()
            }
            
            val goldMatch = Regex("<Currency[^>]*CurrencyCode=\"XAU\"[^>]*>.*?<ForexSelling>([0-9.]+)</ForexSelling>", RegexOption.DOT_MATCHES_ALL).find(xml)
            if (goldMatch != null) {
                val goldOunce = goldMatch.groupValues[1].toDouble()
                goldGramTL = goldOunce / 31.1035
            }
        } catch (e: Exception) {
            // Varsayılan değerler
        }
    }
    
    private fun updatePriceDisplay() {
        if (isTL) {
            priceUsd.text = "${formatMoney(usdRate)}₺"
            priceEur.text = "${formatMoney(eurRate)}₺"
            priceGold.text = "${formatMoney(goldGramTL)}₺"
            priceSilver.text = "${formatMoney(goldGramTL / 80)}₺"
            priceBtc.text = "${formatMoney(btcUSD * usdRate)}₺"
            priceEth.text = "${formatMoney(ethUSD * usdRate)}₺"
        } else {
            priceUsd.text = "1.00$"
            priceEur.text = "${formatMoney(eurRate / usdRate)}$"
            priceGold.text = "${formatMoney(goldGramTL / usdRate)}$"
            priceSilver.text = "${formatMoney(goldGramTL / 80 / usdRate)}$"
            priceBtc.text = "${formatNumber(btcUSD)}$"
            priceEth.text = "${formatNumber(ethUSD)}$"
        }
    }
    
    private fun showSilverInfo() {
        AlertDialog.Builder(requireContext())
            .setTitle("Gümüş Fiyatı Hakkında")
            .setMessage("Gümüş fiyatı, API maliyet kısıtlaması nedeniyle altın/gümüş oranı (1:80) kullanılarak hesaplanmaktadır. Gerçek zamanlı fiyat için lütfen aşağıdaki siteyi ziyaret edin.")
            .setPositiveButton("Tamam", null)
            .setNeutralButton("Siteye Git") { _, _ ->
                openUrl("https://kuyumcukur.com/canli-altin-fiyatlari")
            }
            .show()
    }
    
    private fun loadNews() {
        scope.launch {
            newsContainer.removeAllViews()
            
            val news = listOf(
                NewsItem("Asgari ücret 2025 zam oranı belli oldu", "https://www.csgb.gov.tr", "2 saat önce"),
                NewsItem("Kıdem tazminatı tavanı güncellendi", "https://www.sgk.gov.tr", "5 saat önce"),
                NewsItem("Fazla mesai düzenlemesi TBMM'de", "https://www.iskur.gov.tr", "1 gün önce")
            )
            
            news.forEach { item ->
                val itemView = layoutInflater.inflate(R.layout.news_item, newsContainer, false)
                itemView.findViewById<TextView>(R.id.news_title).text = item.title
                itemView.findViewById<TextView>(R.id.news_time).text = item.time
                itemView.setOnClickListener {
                    showNewsDialog(item.title, item.url)
                }
                newsContainer.addView(itemView)
            }
            
            val moreBtn = Button(context).apply {
                text = "DAHA FAZLA HABER →"
                setOnClickListener {
                    showNewsDialog("Tüm Haberler", "https://www.csgb.gov.tr/haberler")
                }
            }
            newsContainer.addView(moreBtn)
        }
    }
    
    private fun showNewsDialog(title: String, url: String) {
        val webView = WebView(requireContext()).apply {
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
            loadUrl(url)
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(webView)
            .setPositiveButton("Kapat", null)
            .setNeutralButton("Tarayıcıda Aç") { _, _ ->
                openUrl(url)
            }
            .show()
    }
    
    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Tarayıcı açılamadı", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun formatMoney(amount: Double): String {
        return String.format("%.2f", amount).replace('.', ',')
    }
    
    private fun formatNumber(amount: Double): String {
        return String.format("%,.0f", amount).replace(',', '.')
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
    
    data class NewsItem(val title: String, val url: String, val time: String)
}
