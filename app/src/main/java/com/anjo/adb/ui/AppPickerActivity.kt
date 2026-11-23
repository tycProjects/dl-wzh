package com.anjo.adb.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anjo.adb.R
import com.anjo.adb.data.SettingsRepository
import com.google.android.material.tabs.TabLayout

class AppPickerActivity : AppCompatActivity() {

    private lateinit var repo: SettingsRepository
    private lateinit var apps: List<ApplicationInfo>
    private var modoCongelar = true
    private val selecaoCongelar = mutableSetOf<String>()
    private val selecaoProteger = mutableSetOf<String>()
    private lateinit var recycler: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_picker)
        repo = SettingsRepository(this)
        selecaoCongelar.addAll(repo.appsCongelar())
        selecaoProteger.addAll(repo.appsProtegidos())

        val pm = packageManager
        apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null } // só apps com ícone/launcher
            .sortedBy { it.loadLabel(pm).toString().lowercase() }

        recycler = findViewById(R.id.recyclerApps)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = AppAdapter()

        val tabs = findViewById<TabLayout>(R.id.tabsModo)
        tabs.addTab(tabs.newTab().setText("Congelar no jogo"))
        tabs.addTab(tabs.newTab().setText("Nunca fechar"))
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                modoCongelar = tab.position == 0
                recycler.adapter?.notifyDataSetChanged()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        findViewById<Button>(R.id.btnSalvarApps).setOnClickListener {
            repo.setAppsCongelar(selecaoCongelar)
            repo.setAppsProtegidos(selecaoProteger)
            Toast.makeText(this, "Salvo", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    inner class AppAdapter : RecyclerView.Adapter<AppAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val icone = v.findViewById<ImageView>(R.id.imgAppIcone)
            val nome = v.findViewById<TextView>(R.id.txtAppNome)
            val check = v.findViewById<CheckBox>(R.id.checkApp)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app_check, parent, false)
            return VH(v)
        }

        override fun getItemCount() = apps.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val info = apps[position]
            val pm = packageManager
            holder.nome.text = info.loadLabel(pm)
            holder.icone.setImageDrawable(info.loadIcon(pm))
            val selecaoAtiva = if (modoCongelar) selecaoCongelar else selecaoProteger
            holder.check.setOnCheckedChangeListener(null)
            holder.check.isChecked = info.packageName in selecaoAtiva
            holder.check.setOnCheckedChangeListener { _, marcado ->
                if (marcado) selecaoAtiva.add(info.packageName) else selecaoAtiva.remove(info.packageName)
            }
        }
    }
}
