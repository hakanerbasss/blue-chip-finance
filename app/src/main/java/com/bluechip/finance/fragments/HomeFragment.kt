package com.bluechip.finance.fragments

import android.app.AlertDialog
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
import com.bluechip.finance.Article
import com.bluechip.finance.NewsApiService
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import coil.load

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

        // UI Tanımlamaları
        priceUsd = view.findViewById(R.id.price_usd)
        priceEur = view.findViewById(R.id.price_eur)
        priceGold = view.findViewById(R.id.price_gold)
        priceBtc = view.findViewById(R.id.price_btc)
        priceEth = view.findViewById(R.id.price_eth)
        priceUpdateTime = view.findViewById(R.id.price_update_time)
        btnRefresh = view.findViewById(R.id.btn_refresh_prices)
        
        rvNews = view.findViewById(R.id.rvNews)
        tvNewsStatus = view.findViewById(R.id.tvNewsStatus)
        currencySwitch = view.findViewById(R.id.currency_switch)
        val btnMoreNews = view.findViewById<Button>(R.id.btn_more_news)

        // RecyclerView Ayarı (Dikey liste olarak ayarlandı)
        rvNews.layoutManager = LinearLayoutManager(context)
        rvNews.isNestedScrollingEnabled = false

        // Kart Tıklamaları
        view.findViewById<MaterialCardView>(R.id.card_overtime).setOnClickListener { navigateToFragment(OvertimeFragment()) }
        view.findViewById<MaterialCardView>(R.id.card_agi).setOnClickListener { navigateToFragment(SeveranceFragment()) }
        view.findViewById<MaterialCardView>(R.id.card_tax).setOnClickListener { navigateToFragment(TaxFragment()) }
        view.findViewById<MaterialCardView>(R.id.card_severance).setOnClickListener { navigateToFragment(AnnualLeaveFragment()) }

        btnRefresh.setOnClickListener { loadPrices(); loadNewsFromAPI() }
        btnMoreNews.setOnClickListener { loadNewsFromAPI() }

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
        
        scope.launch {
            try {
                tvNewsStatus.text = "Haberler güncelleniyor..."
                
                // Retrofit kullanarak haberleri çekiyoruz
                val apiService = NewsApiService.create()
                val response = withContext(Dispatchers.IO) { 
                    apiService.getNews(country = "us", apiKey = apiKey) 
                }

                if (isAdded && response.articles.isNotEmpty()) {
                    // Kendi oluşturduğumuz Adapter ve click listener
                    rvNews.adapter = InternalNewsAdapter(response.articles) { article -> 
                        showNewsDialog(article.title, article.url) 
                    }
                    tvNewsStatus.text = "Son güncelleme: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}"
                } else {
                    tvNewsStatus.text = "Haber bulunamadı."
                }
            } catch (e: Exception) {
                tvNewsStatus.text = "Bağlantı hatası: ${e.message}"
            }
        }
    }

    // --- PİYASA FİYATLARI KODLARI (DEĞİŞMEDİ) ---
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
                if (isAdded) Toast.makeText(context, "Fiyatlar güncellenemedi", Toast.LENGTH_SHORT).show()
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
        val symbol = if (isTL) "₺" else "$"
        priceUsd.text = "${formatMoney(if (isTL) usdRate else 1.0)}$symbol"
        priceEur.text = "${formatMoney(if (isTL) eurRate else eurRate / usdRate)}$symbol"
        priceGold.text = "${formatMoney(if (isTL) goldOunceUSD * usdRate else goldOunceUSD)}$symbol"
        priceBtc.text = "${if (isTL) formatNumber(btcUSD * usdRate) else formatNumber(btcUSD)}$symbol"
        priceEth.text = "${if (isTL) formatNumber(ethUSD * usdRate) else formatNumber(ethUSD)}$symbol"
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

    private fun navigateToFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).addToBackStack(null).commit()
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }

    // --- YENİ ADAPTER (item_news.xml İÇİN) ---
    inner class InternalNewsAdapter(private val list: List<Article>, val onClick: (Article) -> Unit) : RecyclerView.Adapter<InternalNewsAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.findViewById(R.id.tvNewsTitle)
            val source: TextView = v.findViewById(R.id.tvNewsSource)
            val img: ImageView = v.findViewById(R.id.ivNewsImage)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_news, p, false))
        override fun onBindViewHolder(h: VH, p: Int) {
            val item = list[p]
            h.title.text = item.title
            h.source.text = item.description ?: "Detaylar için tıklayın"
            h.img.load(item.urlToImage) {
                crossfade(true)
                placeholder(android.R.drawable.progress_horizontal)
                error(android.R.drawable.stat_notify_error)
            }
            h.itemView.setOnClickListener { onClick(item) }
        }
        override fun getItemCount() = list.size
    }
}
