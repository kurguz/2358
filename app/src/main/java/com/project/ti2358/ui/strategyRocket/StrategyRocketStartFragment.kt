package com.project.ti2358.ui.strategyRocket

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.project.ti2358.R
import com.project.ti2358.data.manager.Stock
import com.project.ti2358.data.manager.StrategyRocket
import com.project.ti2358.service.*
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class StrategyRocketStartFragment : Fragment() {

    val strategyRocket: StrategyRocket by inject()
    var adapterList: ItemRocketRecyclerViewAdapter = ItemRocketRecyclerViewAdapter(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_rocket_start, container, false)
        val list = view.findViewById<RecyclerView>(R.id.list)

        list.addItemDecoration(
            DividerItemDecoration(
                list.context,
                DividerItemDecoration.VERTICAL
            )
        )

        if (list is RecyclerView) {
            with(list) {
                layoutManager = LinearLayoutManager(context)
                adapter = adapterList
            }
        }

        val buttonStart = view.findViewById<Button>(R.id.buttonStart)
        buttonStart.setOnClickListener { _ ->
            if (strategyRocket.stocksSelected.isNotEmpty())
                view.findNavController().navigate(R.id.action_nav_rocket_start_to_nav_rocket_finish)
        }

        val buttonUpdate = view.findViewById<Button>(R.id.buttonUpdate)
        buttonUpdate.setOnClickListener { _ ->
            adapterList.setData(strategyRocket.process())
        }

        val checkBox = view.findViewById<CheckBox>(R.id.check_box)
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            for (stock in strategyRocket.process()) {
                strategyRocket.setSelected(stock, !isChecked)
            }
            adapterList.notifyDataSetChanged()
        }

        adapterList.setData(strategyRocket.process())

        return view
    }

    inner class ItemRocketRecyclerViewAdapter(
        private var values: List<Stock>
    ) : RecyclerView.Adapter<ItemRocketRecyclerViewAdapter.ViewHolder>() {

        fun setData(newValues: List<Stock>) {
            values = newValues
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.fragment_rocket_start_item,
                parent,
                false
            )

            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.stock = item

            holder.checkBoxView.setOnCheckedChangeListener(null)
            holder.checkBoxView.isChecked = strategyRocket.isSelected(item)

            holder.tickerView.text = "${position}. ${item.marketInstrument.ticker}"
            holder.priceView.text = item.getPriceString()

            val volume = item.getTodayVolume() / 1000f
            holder.volumeTodayView.text = "%.1f".format(volume) + "k"

            holder.changePriceAbsoluteView.text = "%.2f".format(item.changePriceDayAbsolute) + " $"
            holder.changePricePercentView.text = "%.2f".format(item.changePriceDayPercent) + "%"

            if (item.changePriceDayAbsolute < 0) {
                holder.changePriceAbsoluteView.setTextColor(Utils.RED)
                holder.changePricePercentView.setTextColor(Utils.RED)
            } else {
                holder.changePriceAbsoluteView.setTextColor(Utils.GREEN)
                holder.changePricePercentView.setTextColor(Utils.GREEN)
            }

            holder.checkBoxView.setOnCheckedChangeListener { _, isChecked ->
                strategyRocket.setSelected(holder.stock, !isChecked)
            }

            holder.itemView.setOnClickListener {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tinkoff.ru/invest/stocks/${holder.stock.marketInstrument.ticker}/"))
                startActivity(browserIntent)
            }
        }

        override fun getItemCount(): Int = values.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            lateinit var stock: Stock

            val tickerView: TextView = view.findViewById(R.id.stock_item_ticker)
            val priceView: TextView = view.findViewById(R.id.stock_item_price)

            val volumeTodayView: TextView = view.findViewById(R.id.stock_item_volume_today)

            val changePriceAbsoluteView: TextView = view.findViewById(R.id.stock_item_price_change_absolute)
            val changePricePercentView: TextView = view.findViewById(R.id.stock_item_price_change_percent)

            val checkBoxView: CheckBox = view.findViewById(R.id.check_box)
        }
    }
}