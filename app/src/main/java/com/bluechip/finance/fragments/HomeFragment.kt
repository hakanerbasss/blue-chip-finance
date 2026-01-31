package com.bluechip.finance.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        
        // Fiyat TextViews
        priceUsd = view.findViewById(R.id.price_usd)
        priceEur = view.findViewById(R.id.price_eur)
        priceGold = view.findViewById(R.id.price_gold)
        priceSilver = view.findViewById(R.id.price_silver)
        priceBtc = view.findViewById(R.id.price_btc)
        priceEth = view.findViewById(R.id.price_eth)
        priceUpdateTime = view.findViewById(R.id.price_update_time)
        btnRefresh = view.findViewById(R.id.btn_refresh_prices)
        newsContainer = view.findViewById(R.id.news_container)
        
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
        
        // İlk yükleme
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
                
                // TCMB API (Dolar, Euro, Altın)
                val tcmbData = withContext(Dispatchers.IO) {
                    val url = "https://www.tcmb.gov.tr/kurlar/today.xml"
                    try {
                        URL(url).readText()
                    } catch (e: Exception) {
                        null
                    }
                }
                
                if (tcmbData != null) {
                    parseTCMBData(tcmbData)
                } else {
                    priceUsd.text = "32.50₺"
                    priceEur.text = "35.20₺"
                    priceGold.text = "2,450₺"
                    priceSilver.text = "28₺"
                }
                
                // CoinGecko API (BTC, ETH)
                val cryptoData = withContext(Dispatchers.IO) {
                    try {
                        val url = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum&vs_currencies=usd"
                        URL(url).readText()
                    } catch (e: Exception) {
                        null
                    }
                }
                
                if (cryptoData != null) {
                    val json = JSONObject(cryptoData)
                    val btc = json.getJSONObject("bitcoin").getDouble("usd")
                    val eth = json.getJSONObject("ethereum").getDouble("usd")
                    priceBtc.text = "${formatNumber(btc)}$"
                    priceEth.text = "${formatNumber(eth)}$"
                } else {
                    priceBtc.text = "64,200$"
                    priceEth.text = "3,100$"
                }
                
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
            // Basit XML parse (Dolar)
            val usdMatch = Regex("<Currency[^>]*CurrencyCode=\"USD\"[^>]*>.*?<ForexSelling>([0-9.]+)</ForexSelling>", RegexOption.DOT_MATCHES_ALL)
                .find(xml)
            if (usdMatch != null) {
                val usd = usdMatch.groupValues[1].toDouble()
                priceUsd.text = "${formatMoney(usd)}₺"
            }
            
            // Euro
            val eurMatch = Regex("<Currency[^>]*CurrencyCode=\"EUR\"[^>]*>.*?<ForexSelling>([0-9.]+)</ForexSelling>", RegexOption.DOT_MATCHES_ALL)
                .find(xml)
            if (eurMatch != null) {
                val eur = eurMatch.groupValues[1].toDouble()
                priceEur.text = "${formatMoney(eur)}₺"
            }
            
            // Altın (gram bazında hesaplama)
            val goldMatch = Regex("<Currency[^>]*CurrencyCode=\"XAU\"[^>]*>.*?<ForexSelling>([0-9.]+)</ForexSelling>", RegexOption.DOT_MATCHES_ALL)
                .find(xml)
            if (goldMatch != null) {
                val goldOunce = goldMatch.groupValues[1].toDouble()
                val goldGram = goldOunce / 31.1035 // ons -> gram
                priceGold.text = "${formatMoney(goldGram)}₺"
            }
            
            // Gümüş (örnek değer)
            priceSilver.text = "28₺"
            
        } catch (e: Exception) {
            // Hata durumunda varsayılan değerler
        }
    }
    
    private fun loadNews() {
        scope.launch {
            newsContainer.removeAllViews()
            
            val news = withContext(Dispatchers.IO) {
                listOf(
                    NewsItem("Asgari ücret 2025 zam oranı belli oldu", "https://www.csgb.gov.tr", "2 saat önce"),
                    NewsItem("Kıdem tazminatı tavanı güncellendi", "https://www.sgk.gov.tr", "5 saat önce"),
                    NewsItem("Fazla mesai düzenlemesi TBMM'de", "https://www.iskur.gov.tr", "1 gün önce")
                )
            }
            
            news.forEach { item ->
                val itemView = layoutInflater.inflate(R.layout.news_item, newsContainer, false)
                itemView.findViewById<TextView>(R.id.news_title).text = item.title
                itemView.findViewById<TextView>(R.id.news_time).text = item.time
                itemView.setOnClickListener {
                    openUrl(item.url)
                }
                newsContainer.addView(itemView)
            }
            
            // "Daha Fazla" butonu
            val moreBtn = Button(context).apply {
                text = "DAHA FAZLA HABER →"
                setOnClickListener {
                    openUrl("https://www.csgb.gov.tr/haberler")
                }
            }
            newsContainer.addView(moreBtn)
        }
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
