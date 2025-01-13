package com.example.arbitrage

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import androidx.room.Room
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private val coinMarketCapUrl = "https://api.coinmarketcap.com/data-api/v3/cryptocurrency/market-pairs/latest?slug=bitcoin&start=101&limit=100&category=spot&sort=cmc_rank_advanced"
    private lateinit var database: AppDatabase
    private lateinit var opportunityAdapter: OpportunityAdapter
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "arbitrage-db").build()

        progressBar = findViewById(R.id.progressBar)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        opportunityAdapter = OpportunityAdapter()
        recyclerView.adapter = opportunityAdapter

        val viewModel = ViewModelProvider(this)[OpportunityViewModel::class.java]

        // Observe database changes and update UI
        lifecycleScope.launch {
            database.opportunityDao().getAll().collectLatest { opportunities ->
                opportunityAdapter.submitList(opportunities)
            }
        }

        // Fetch initial data
        fetchInitialData()


        // User input for currency
        val currencyInput: EditText = findViewById(R.id.currencyInput)
        val addButton: Button = findViewById(R.id.addButton)

        addButton.setOnClickListener {
            val currency = currencyInput.text.toString().trim().lowercase()
            if (validateCurrencyInput(currency)) {
                val websocketUrl = "wss://stream.binance.com:9443/ws/${currency}usdt@trade"
                connectToWebSocket(websocketUrl)
                Toast.makeText(this, "Subscribed to $currency/USDT", Toast.LENGTH_SHORT).show()
                currencyInput.text.clear()
            } else {
                Toast.makeText(this, "Please enter a valid currency (letters only)", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun fetchInitialData() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            try{
                val response = fetchData(coinMarketCapUrl)
                if (response != null) {
                    val opportunities = analyzeArbitrageOpportunities(response)
                    saveOpportunities(opportunities)
                } else {
                    showError("Failed to fetch initial data.")
                }
            } catch (e: Exception) {
                showError("Error fetching initial ${e.message}")
            } finally {
                 withContext(Dispatchers.Main){
                        progressBar.visibility = View.GONE
                  }
            }

        }
    }

    private fun validateCurrencyInput(currency: String): Boolean {
        return currency.isNotEmpty() && currency.matches(Regex("[a-z]+"))
    }

    // Function to fetch data from CoinMarketCap API
    private suspend fun fetchData(url: String): JSONObject? {
          return try {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseData = response.body?.string()
                if (responseData != null) {
                    JSONObject(responseData)
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Function to analyze arbitrage opportunities
    private fun analyzeArbitrageOpportunities(JSONObject): List<ArbitrageOpportunity> {
        val opportunities = mutableListOf<ArbitrageOpportunity>()
        val marketPairs = data.getJSONObject("data").getJSONArray("marketPairs")

        for (i in 0 until marketPairs.length()) {
            val pair = marketPairs.getJSONObject(i)
            val exchange = pair.getJSONObject("exchange").getString("name")
            val price = pair.getJSONObject("quote").getJSONObject("USD").getDouble("price")

            opportunities.add(ArbitrageOpportunity(exchange, price))
        }
        return opportunities
    }

    // Function to save opportunities to the database
    private suspend fun saveOpportunities(opportunities: List<ArbitrageOpportunity>) {
        withContext(Dispatchers.IO) {
            database.opportunityDao().insertAll(opportunities)
        }
    }

    // Function to connect to WebSocket for a specific URL
    private fun connectToWebSocket(url: String) {
         progressBar.visibility = View.VISIBLE
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        val webSocket = client.newWebSocket(request, object : WebSocketListener() {
             override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                super.onOpen(webSocket, response)
                lifecycleScope.launch(Dispatchers.Main){
                    Toast.makeText(this@MainActivity,"WebSocket connected",Toast.LENGTH_SHORT).show()
                }

            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val data = JSONObject(text)
                        val opportunity = ArbitrageOpportunity(
                            exchange = data.getString("e"),
                            price = data.getDouble("p")
                        )
                        saveOpportunities(listOf(opportunity))
                    } catch(e: Exception){
                        showError("Error processing WebSocket message: ${e.message}")
                    } finally {
                         withContext(Dispatchers.Main){
                              progressBar.visibility = View.GONE
                        }
                    }
                }
            }
             override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                super.onFailure(webSocket, t, response)
                showError("WebSocket connection failed: ${t.message}")
                 lifecycleScope.launch(Dispatchers.Main){
                     progressBar.visibility = View.GONE
                 }
             }

             override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                 super.onClosed(webSocket, code, reason)
                 lifecycleScope.launch(Dispatchers.Main){
                     Toast.makeText(this@MainActivity,"WebSocket closed",Toast.LENGTH_SHORT).show()
                 }

             }
        })

    }
    private fun showError(message: String){
         lifecycleScope.launch(Dispatchers.Main){
             Toast.makeText(this@MainActivity,message,Toast.LENGTH_LONG).show()
         }
    }
}