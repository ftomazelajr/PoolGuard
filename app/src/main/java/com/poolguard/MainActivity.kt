package com.poolguard

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var db: PoolDatabase
    private lateinit var adapter: PoolAdapter
    private lateinit var tvProgress: TextView
    private lateinit var progressBar: ProgressBar
    private val pools = mutableListOf<PoolData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        db = PoolDatabase(this)
        tvProgress = findViewById(R.id.tvProgress)
        progressBar = findViewById(R.id.progressBar)
        val rv = findViewById<RecyclerView>(R.id.rvPools)
        val btn = findViewById<Button>(R.id.btnAdd)

        pools.addAll(db.getAll())
        adapter = PoolAdapter(pools) { showTasks(it) }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
        btn.setOnClickListener { showAddDialog() }
        updateUI()
    }

    private fun updateUI() {
        val done = pools.count { it.completed }
        tvProgress.text = "$done/4 piscinas"
        progressBar.progress = if (pools.isEmpty()) 0 else (done * 100) / pools.size
        adapter.update(pools)
        if (done >= 4 && pools.isNotEmpty()) {
            AlertDialog.Builder(this).setTitle("🎉 PARABÉNS!").setMessage("4 piscinas completadas!").setPositiveButton("🏆 OK") { _, _ -> }.show()
        }
    }

    private fun showAddDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_pool, null)
        val etName = view.findViewById<EditText>(R.id.etName)
        val etClient = view.findViewById<EditText>(R.id.etClient)
        val etPhone = view.findViewById<EditText>(R.id.etPhone)
        val cbs = listOf(view.findViewById<CheckBox>(R.id.cbSeg), view.findViewById<CheckBox>(R.id.cbTer), view.findViewById<CheckBox>(R.id.cbQua), view.findViewById<CheckBox>(R.id.cbQui), view.findViewById<CheckBox>(R.id.cbSex))
        val dias = listOf("SEG","TER","QUA","QUI","SEX")

        AlertDialog.Builder(this).setTitle("🏊 Nova Piscina").setView(view)
            .setPositiveButton("💾 SALVAR") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isEmpty()) { Toast.makeText(this, "Digite o nome!", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                val client = etClient.text.toString().trim().ifEmpty { "N/A" }
                val phone = etPhone.text.toString().trim().ifEmpty { "N/A" }
                val d = cbs.mapIndexed { i, cb -> if (cb.isChecked) dias[i] else null }.filterNotNull().joinToString(" ")
                val pool = PoolData(name = name, client = client, phone = phone, days = d.ifEmpty { "HOJE" })
                pools.add(pool); db.save(pool); updateUI()
                Toast.makeText(this, "✅ $name salva!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("CANCELAR", null).show()
    }

    private fun showTasks(pool: PoolData) {
        val tasks = listOf("🧪 Medir pH", "🧪 Medir Alcalinidade", "🧪 Colocar Cloro", "🧹 Escovar Paredes", "🗑️ Limpar Cesto")
        val checked = BooleanArray(5)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(32, 16, 32, 16); setBackgroundColor(Color.parseColor("#FF0A1628"))
        }
        tasks.forEachIndexed { i, t ->
            val cb = CheckBox(this).apply { text = t; setTextColor(Color.WHITE); setPadding(0, 8, 0, 8); setOnCheckedChangeListener { _, v -> checked[i] = v } }
            container.addView(cb)
        }
        AlertDialog.Builder(this).setTitle("🏊 ${pool.name}").setMessage("👤 ${pool.client}\n📞 ${pool.phone}\n📅 ${pool.days}").setView(container)
            .setPositiveButton("📸 CONCLUIR COM FOTO") { _, _ ->
                if (checked.all { it }) {
                    startActivity(Intent(this, CameraActivity::class.java))
                    pool.completed = true; db.updateCompleted(pool.id, true); updateUI()
                    Toast.makeText(this, "✅ ${pool.name} concluída!", Toast.LENGTH_SHORT).show()
                } else { Toast.makeText(this, "⚠️ Marque TODAS as tarefas!", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("FECHAR", null).show()
    }
}
