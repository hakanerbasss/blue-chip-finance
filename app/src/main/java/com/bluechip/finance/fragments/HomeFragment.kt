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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var rvNews: RecyclerView
    private lateinit var currencySwitch: Switch
    private lateinit var tvNewsStatus: TextView

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var usdRate = 32.5
    private var eurRate = 35.2
    private var goldOunceUSD = 2650.0
    private var btcUSD = 64200.0
    private var ethUSD = 3100.0
    private var isTL = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Fiyat Bileşenleri
        priceUsd = view.findViewById(R.id.price_usd)
        priceEur = view.findViewById(R.id.price_eur)
        priceGold = view.findViewById(R.id.price_gold)
        priceBtc = view.findViewById(R.id.price_btc)
        priceEth = view.findViewById(R.id.price_eth)
        priceUpdateTime = view.findViewById(R.id.price_update_time)
        btnRefresh = view.findViewById(R.id.btn_refresh_prices)
        
        // Haber Bileşenleri
        rvNews = view.findViewById(R.id.rvNews)
        tvNewsStatus = view.findViewById(R.id.tvNewsStatus)
        val btnMoreNews = view.findViewById<Button>(R.id.btn_more_news)
        
        currencySwitch = view.findViewById(R.id.currency_switch)

        // RecyclerView Ayarı (Yatay Kaydırma)
        rvNews.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        // Tıklama Dinleyicileri
        view.findViewById<MaterialCardView>(R.id.card_overtime).setOnClickListener { navigateToOvertime() }
        view.findViewById<MaterialCardView>(R.id.card_agi).setOnClickListener { navigateToSeverance() }
        view.findViewById<MaterialCardView>(R.id.card_tax).setOnClickListener { navigateToTax() }
        view.findViewById<MaterialCardView>(R.id.card_severance).setOnClickListener { navigateToAnnualLeave() }

        btnRefresh.setOnClickListener {
            loadPrices()
            loadNewsFromAPI()
        }

        btnMoreNews.setOnClickListener {
            loadNewsFromAPI()
        }

        currencySwitch.setOnCheckedChangeListener { _, isChecked ->
            isTL = !isChecked
            updatePriceDisplay()
        }

        loadPrices()
        loadNewsFromAPI()

        return view
    }

    private fun loadNewsFromAPI() {
        val apiKey = "bc7b44a1f4844c018557d4945800d61c"
        val url = "https://newsapi.org/v2/top-headlines?country=tr&category=business&apiKey=$apiKey"

        scope.launch {
            try {
                tvNewsStatus.text = "Haberler güncelleniyor..."
                
                val response = withContext(Dispatchers.IO) {
                    URL(url).readText()
                }

                val jsonResponse = JSONObject(response)
                val articlesArray = jsonResponse.getJSONArray("articles")
                val newsList = mutableListOf<NewsItem>()

                for (i in 0 until minOf(articlesArray.length(), 6)) {
                    val obj = articlesArray.getJSONObject(i)
                    newsList.add(NewsItem(
                        title = obj.getString("title"),
                        url = obj.getString("url"),
                        time = obj.optString("publishedAt").substringBefore("T"),
                        imageUrl = obj.optString("urlToImage", ""),
                        description = obj.optString("description", "")
                    ))
                }

                if (isAdded) {
                    rvNews.adapter = NewsAdapter(newsList) { item ->
                        showNewsDialog(item.title, item.url)
                    }
                    tvNewsStatus.text = "Son güncelleme: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}"
                }

            } catch (e: Exception) {
                tvNewsStatus.text = "Haberler yüklenemedi."
                e.printStackTrace()
            }
        }
    }

    // --- MEVCUT FİYAT FONKSİYONLARI (DEĞİŞMEDİ) ---
    private fun loadPrices() {
        scope.launch {
            try {
                btnRefresh.isEnabled = false
                val tcmbData = withContext(Dispatchers.IO) {
                    try { URL("https://www.tcmb.gov.tr/kurlar/today.xml").readText() } catch (e: Exception) { null }
                }
                if (tcmbData != null) parseTCMBData(tcmbData)

                val cryptoData = withContext(Dispatchers.IO) {
                    try { URL("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,tether-gold&vs_currencies=usd").readText() } catch (e: Exception) { null }
                }
                if (cryptoData != null) {
                    val json = JSONObject(cryptoData)
                    btcUSD = json.getJSONObject("bitcoin").getDouble("usd")
                    ethUSD = json.getJSONObject("ethereum").getDouble("usd")
                    goldOunceUSD = json.getJSONObject("tether-gold").getDouble("usd")
                }
                updatePriceDisplay()
                priceUpdateTime.text = "Son güncelleme: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}"
            } catch (e: Exception) {
                if (isAdded) Toast.makeText(context, "Fiyatlar yüklenemedi", Toast.LENGTH_SHORT).show()
            } finally { btnRefresh.isEnabled = true }
        }
    }

    private fun parseTCMBData(xml: String) {
        val usdMatch = Regex("<Currency[^>]*CurrencyCode=\"USD\"[^>]*>.*?<ForexSelling>([0-9.]+)</ForexSelling>", RegexOption.DOT_MATCHES_ALL).find(xml)
        if (usdMatch != null) usdRate = usdMatch.groupValues[1].toDouble()
        val eurMatch = Regex("<Currency[^>]*CurrencyCode=\"EUR\"[^>]*>.*?<ForexSelling>([0-9.]+)</ForexSelling>", RegexOption.DOT_MATCHES_ALL).find(xml)
        if (eurMatch != null) eurRate = eurMatch.groupValues[1].toDouble()
    }

    private fun updatePriceDisplay() {
        if (isTL) {
            priceUsd.text = "${formatMoney(usdRate)}₺"; priceEur.text = "${formatMoney(eurRate)}₺"
            priceGold.text = "${formatMoney(goldOunceUSD * usdRate)}₺"
            priceBtc.text = "${formatMoney(btcUSD * usdRate)}₺"; priceEth.text = "${formatMoney(ethUSD * usdRate)}₺"
        } else {
            priceUsd.text = "1.00$"; priceEur.text = "${formatMoney(eurRate / usdRate)}$"
            priceGold.text = "${formatMoney(goldOunceUSD)}$"; priceBtc.text = "${formatNumber(btcUSD)}$"; priceEth.text = "${formatNumber(ethUSD)}$"
        }
    }

    private fun showNewsDialog(title: String, url: String) {
        if (!isAdded) return
        val webView = WebView(requireContext()).apply {
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
            loadUrl(url)
        }
        AlertDialog.Builder(requireContext()).setTitle(title).setView(webView).setPositiveButton("Kapat", null).show()
    }

    private fun formatMoney(amount: Double) = String.format("%.2f", amount).replace('.', ',')
    private fun formatNumber(amount: Double) = String.format("%,.0f", amount).replace(',', '.')

    // Navigasyonlar
    private fun navigateToOvertime() { parentFragmentManager.beginTransaction().replace(R.id.fragment_container, OvertimeFragment()).addToBackStack(null).commit() }
    private fun navigateToSeverance() { parentFragmentManager.beginTransaction().replace(R.id.fragment_container, SeveranceFragment()).addToBackStack(null).commit() }
    private fun navigateToTax() { parentFragmentManager.beginTransaction().replace(R.id.fragment_container, TaxFragment()).addToBackStack(null).commit() }
    private fun navigateToAnnualLeave() { parentFragmentManager.beginTransaction().replace(R.id.fragment_container, AnnualLeaveFragment()).addToBackStack(null).commit() }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }

    // Haber Modeli
    data class NewsItem(val title: String, val url: String, val time: String, val imageUrl: String, val description: String)

    // Haber Adapter (İç Sınıf)
    inner class NewsAdapter(private val list: List<NewsItem>, val onClick: (NewsItem) -> Unit) : RecyclerView.Adapter<NewsAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val title = v.findViewById<TextView>(R.id.tvTitle)
            val desc = v.findViewById<TextView>(R.id.tvDesc)
            val img = v.findViewById<ImageView>(R.id.ivNews)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_news_card, p, false))
        override fun onBindViewHolder(h: VH, p: Int) {
            val item = list[p]
            h.title.text = item.title
            h.desc.text = item.description
            // Resim yükleme kütüphanesi (Coil/Glide) ekli değilse ImageView boş kalır. 
            // Basitlik için şimdilik setOnClickListener ekliyoruz.
            h.itemView.setOnClickListener { onClick(item) }
        }
        override fun getItemCount() = list.size
    }
}
