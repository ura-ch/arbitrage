package com.example.arbitrage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OpportunityAdapter : RecyclerView.Adapter<OpportunityAdapter.ViewHolder>() {

    private var opportunities: List<ArbitrageOpportunity> = emptyList()

    fun submitList(newList: List<ArbitrageOpportunity>) {
        opportunities = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_opportunity, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val opportunity = opportunities[position]
        holder.bind(opportunity)
    }

    override fun getItemCount(): Int = opportunities.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val exchangeView: TextView = itemView.findViewById(R.id.exchange)
        private val priceView: TextView = itemView.findViewById(R.id.price)

        fun bind(opportunity: ArbitrageOpportunity) {
            exchangeView.text = opportunity.exchange
            priceView.text = opportunity.price.toString()
        }
    }
}