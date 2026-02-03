import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Haber veri yapıları
data class NewsResponse(val articles: List<Article>)
data class Article(val title: String, val description: String?, val urlToImage: String?, val url: String)

interface NewsApiService {
    @GET("v2/top-headlines")
    suspend fun getNews(
        @Query("country") country: String = "us",
        @Query("apiKey") apiKey: String = "bc7b44a1f4844c018557d4945800d61c"
    ): NewsResponse

    companion object {
        private const val BASE_URL = "https://newsapi.org/"

        fun create(): NewsApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NewsApiService::class.java)
        }
    }
}
