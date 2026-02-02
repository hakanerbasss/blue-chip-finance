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
    private lateinit var priceBtc: TextView
    private lateinit var priceEth: TextView
    private lateinit var priceUpdateTime: TextView
    private lateinit var btnRefresh: Button
    private lateinit var newsContainer: LinearLayout
    private lateinit var currencySwitch: Switch
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var usdRate = 32.5
    private var eurRate = 35.2
    private var goldOunceUSD = 2650.0
    private var btcUSD = 64200.0
    private var ethUSD = 3100.0
    private var isTL = true
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        
        priceUsd = view.findViewById(R.id.price_usd)
        priceEur = view.findViewById(R.id.price_eur)
        priceGold = view.findViewById(R.id.price_gold)
        priceBtc = view.findViewById(R.id.price_btc)
        priceEth = view.findViewById(R.id.price_eth)
        priceUpdateTime = view.findViewById(R.id.price_update_time)
        btnRefresh = view.findViewById(R.id.btn_refresh_prices)
        newsContainer = view.findViewById(R.id.news_container)
        currencySwitch = view.findViewById(R.id.currency_switch)
        
        view.findViewById<MaterialCardView>(R.id.card_overtime).setOnClickListener {
            navigateToOvertime()
        }
        
        view.findViewById<MaterialCardView>(R.id.card_agi).setOnClickListener {
            navigateToSeverance()
        }
        
        view.findViewById<MaterialCardView>(R.id.card_tax).setOnClickListener {
            Toast.makeText(context, "navigateToTax()", Toast.LENGTH_SHORT).show()
        }
        
        view.findViewById<MaterialCardView>(R.id.card_severance).setOnClickListener {
            navigateToAnnualLeave()
        }
        
        btnRefresh.setOnClickListener {
            loadPrices()
            loadNews()
        }
        
        currencySwitch.setOnCheckedChangeListener { _, isChecked ->
            isTL = !isChecked
            updatePriceDisplay()
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
    
    private fun navigateToSeverance() {
        val fragment = SeveranceFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
    
    private fun navigateToAnnualLeave() {
    
    private fun navigateToTax() {
        val fragment = TaxFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
        val fragment = AnnualLeaveFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
    
    private fun loadPrices() {
        scope.launch {
            try {
                btnRefresh.isEnabled = false
                
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
                
                val cryptoData = withContext(Dispatchers.IO) {
                    try {
                        URL("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,tether-gold&vs_currencies=usd").readText()
                    } catch (e: Exception) {
                        null
                    }
                }
                
                if (cryptoData != null) {
                    val json = JSONObject(cryptoData)
                    btcUSD = json.getJSONObject("bitcoin").getDouble("usd")
                    ethUSD = json.getJSONObject("ethereum").getDouble("usd")
                    goldOunceUSD = json.getJSONObject("tether-gold").getDouble("usd")
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
        } catch (e: Exception) {
            // Varsayılan değerler
        }
    }
    
    private fun updatePriceDisplay() {
        if (isTL) {
            priceUsd.text = "${formatMoney(usdRate)}₺"
            priceEur.text = "${formatMoney(eurRate)}₺"
            priceGold.text = "${formatMoney(goldOunceUSD * usdRate)}₺"
            priceBtc.text = "${formatMoney(btcUSD * usdRate)}₺"
            priceEth.text = "${formatMoney(ethUSD * usdRate)}₺"
        } else {
            priceUsd.text = "1.00$"
            priceEur.text = "${formatMoney(eurRate / usdRate)}$"
            priceGold.text = "${formatMoney(goldOunceUSD)}$"
            priceBtc.text = "${formatNumber(btcUSD)}$"
            priceEth.text = "${formatNumber(ethUSD)}$"
        }
    }
    
    private fun loadNews() {
        scope.launch {
            newsContainer.removeAllViews()
            
            val news = withContext(Dispatchers.IO) {
                try {
                    val url = "https://news.google.com/rss/topics/CAAqKggKIiRDQkFTRlFvSUwyMHZNRGx6TVdZU0JYUnlMVlJTR2dKVVVpZ0FQAQ?hl=tr&gl=TR&ceid=TR:tr"
                    val rss = URL(url).readText()
                    
                    val newsList = mutableListOf<NewsItem>()
                    val itemMatches = Regex("<item>.*?</item>", RegexOption.DOT_MATCHES_ALL).findAll(rss)
                    
                    itemMatches.take(3).forEach { match ->
                        val itemXml = match.value
                        val titleMatch = Regex("<title><!\\[CDATA\\[(.+?)\\]\\]></title>").find(itemXml)
                        val linkMatch = Regex("<link>(.+?)</link>").find(itemXml)
                        val pubDateMatch = Regex("<pubDate>(.+?)</pubDate>").find(itemXml)
                        
                        if (titleMatch != null && linkMatch != null) {
                            val title = titleMatch.groupValues[1]
                            val link = linkMatch.groupValues[1]
                            
                            val time = try {
                                val pubDate = pubDateMatch?.groupValues?.get(1) ?: ""
                                val parser = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
                                val date = parser.parse(pubDate)
                                val now = Date()
                                val diff = (now.time - date.time) / 1000 / 60
                                when {
                                    diff < 60 -> "${diff.toInt()} dakika önce"
                                    diff < 1440 -> "${(diff / 60).toInt()} saat önce"
                                    else -> "${(diff / 1440).toInt()} gün önce"
                                }
                            } catch (e: Exception) {
                                "Bugün"
                            }
                            
                            newsList.add(NewsItem(title, link, time))
                        }
                    }
                    newsList
                } catch (e: Exception) {
                    listOf(NewsItem("Haberler yüklenemedi", "", "İnternet bağlantınızı kontrol edin"))
                }
            }
            
            if (news.isEmpty()) {
                val emptyText = TextView(context).apply {
                    text = "Haberler yüklenemedi"
                    textSize = 14f
                    setTextColor(context.getColor(android.R.color.darker_gray))
                }
                newsContainer.addView(emptyText)
            } else {
                news.forEach { item ->
                    val itemView = layoutInflater.inflate(R.layout.news_item, newsContainer, false)
                    itemView.findViewById<TextView>(R.id.news_title).text = item.title
                    itemView.findViewById<TextView>(R.id.news_time).text = item.time
                    if (item.url.isNotEmpty()) {
                        itemView.setOnClickListener {
                            showNewsDialog(item.title, item.url)
                        }
                    }
                    newsContainer.addView(itemView)
                }
            }
            
            val moreBtn = Button(context).apply {
                text = "DAHA FAZLA HABER →"
                setOnClickListener {
                    showNewsDialog("Ekonomi Haberleri", "https://news.google.com/topics/CAAqKggKIiRDQkFTRlFvSUwyMHZNRGx6TVdZU0JYUnlMVlJTR2dKVVVpZ0FQAQ?hl=tr&gl=TR&ceid=TR:tr")
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
