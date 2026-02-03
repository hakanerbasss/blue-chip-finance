package com.bluechip.finance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class NewsAdapter(private var articles: List<Article>) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    class NewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.ivNewsImage)
        val tvTitle: TextView = view.findViewById(R.id.tvNewsTitle)
        val tvSource: TextView = view.findViewById(R.id.tvNewsSource)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val article = articles[position]
        holder.tvTitle.text = article.title
        holder.tvSource.text = article.description ?: "Haber detayı için tıklayın"
        
        // Görseli internetten yükleme (Coil kütüphanesi ile)
        holder.ivImage.load(article.urlToImage) {
            crossfade(true)
            placeholder(android.R.drawable.ic_menu_report_image)
        }
    }

    override fun getItemCount(): Int = articles.size

    // Yeni haberler geldiğinde listeyi güncellemek için
    fun updateData(newArticles: List<Article>) {
        articles = newArticles
        notifyDataSetChanged()
    }
}
