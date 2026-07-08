package com.poolguard

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PoolAdapter(
    private var pools: List<PoolData>,
    private val onClick: (PoolData) -> Unit
) : RecyclerView.Adapter<PoolAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: TextView = v.findViewById(R.id.tvIcon)
        val name: TextView = v.findViewById(R.id.tvName)
        val info: TextView = v.findViewById(R.id.tvInfo)
        val status: TextView = v.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int): VH {
        return VH(LayoutInflater.from(p.context).inflate(R.layout.item_pool, p, false))
    }

    override fun onBindViewHolder(h: VH, i: Int) {
        val pool = pools[i]
        h.icon.text = if (pool.completed) "✅" else "🏊"
        h.name.text = pool.name
        h.info.text = "${pool.client} • ${pool.phone} • ${pool.days}"
        if (pool.completed) {
            h.status.text = "CONCLUÍDO"
            h.status.setBackgroundColor(Color.parseColor("#FF00E676"))
        } else {
            h.status.text = "PENDENTE"
            h.status.setBackgroundColor(Color.parseColor("#FFFFAB00"))
        }
        h.status.setTextColor(Color.parseColor("#FF0A1628"))
        h.itemView.setOnClickListener { onClick(pool) }
    }

    override fun getItemCount() = pools.size

    fun update(list: List<PoolData>) {
        pools = list
        notifyDataSetChanged()
    }
}
