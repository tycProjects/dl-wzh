package com.anjo.adb.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.anjo.adb.R
import com.anjo.adb.core.ShellEngine
import com.anjo.adb.data.Categoria
import com.anjo.adb.data.ProfilePresets
import com.anjo.adb.data.SettingsRepository
import com.anjo.adb.service.FloatingBubbleService
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import rikka.shizuku.Shizuku

class MainActivity : FragmentActivity() {

    private lateinit var repo: SettingsRepository

    private val permissaoListener = Shizuku.OnRequestPermissionResultListener { _, grant ->
        atualizarBannerStatus()
        if (grant != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            AlertDialog.Builder(this)
                .setTitle("Shizuku não autorizado")
                .setMessage("Sem autorizar o Shizuku, as funções marcadas com \"requer Shizuku ou ADB\" ficam desligadas.")
                .setPositiveButton("Ok", null)
                .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        repo = SettingsRepository(this)
        Shizuku.addRequestPermissionResultListener(permissaoListener)

        configurarAbas()
        configurarPerfis()
        atualizarBannerStatus()
        com.anjo.adb.util.ThemeTint.aplicarEmTabs(findViewById(R.id.tabLayout))

        findViewById<Button>(R.id.btnVerTudo).setOnClickListener { abrirVerTudo() }
        findViewById<Button>(R.id.btnCorPainel).setOnClickListener {
            ColorPickerDialog.mostrar(this, repo.corPainel()) { cor ->
                repo.setCorPainel(cor)
                recreate()
            }
        }
        findViewById<Button>(R.id.btnEscolherApps).setOnClickListener {
            startActivity(Intent(this, AppPickerActivity::class.java))
        }

        val swBolinha = findViewById<Switch>(R.id.switchBolinha)
        swBolinha.isChecked = repo.bolinhaAtiva()
        swBolinha.setOnCheckedChangeListener { _, ativo -> alternarBolinha(ativo) }

        findViewById<TextView>(R.id.txtStatusShizuku).setOnClickListener {
            if (!ShellEngine.isReady()) ShellEngine.requestPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        atualizarBannerStatus()
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissaoListener)
        super.onDestroy()
    }

    private fun atualizarBannerStatus() {
        val status = findViewById<TextView>(R.id.txtStatusShizuku)
        val sub = findViewById<TextView>(R.id.txtStatusSub)
        when (ShellEngine.state()) {
            ShellEngine.State.PRONTO -> {
                status.text = "✓ Shizuku conectado"
                sub.text = "Todas as funções de sistema estão liberadas"
            }
            ShellEngine.State.NAO_AUTORIZADO -> {
                status.text = "Shizuku instalado, falta autorizar"
                sub.text = "Toque aqui pra autorizar o Anjo ADB"
            }
            ShellEngine.State.NAO_INSTALADO -> {
                status.text = "Shizuku ou ADB não detectado"
                sub.text = "Instale o app Shizuku e pareie por ADB sem fio — sem isso, as funções de sistema não funcionam"
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun configurarPerfis() {
        val row = findViewById<LinearLayout>(R.id.rowPerfis)
        row.removeAllViews()
        val perfis = listOf("EXTREMO", "EQUILIBRADO", "BATERIA", "PERSONAL")
        for (nome in perfis) {
            val btn = Button(this)
            btn.text = nome
            btn.textSize = 11f
            btn.isAllCaps = false
            btn.setTextColor(resources.getColor(
                if (nome == repo.perfilAtual()) android.R.color.white else R.color.text_secondary, theme))
            btn.background = resources.getDrawable(
                if (nome == repo.perfilAtual()) R.drawable.bg_pill_accent else R.drawable.bg_card, theme)
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp.marginEnd = 6
            btn.layoutParams = lp
            btn.setOnClickListener {
                repo.setPerfilAtual(nome)
                ProfilePresets.aplicar(repo, nome)
                configurarPerfis()
                recreate() // refaz as abas pra refletir os novos estados
            }
            row.addView(btn)
        }
    }

    private fun configurarAbas() {
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val categorias = listOf(
            Categoria.JOGOS to "JOGOS", Categoria.BOOST to "BOOST", Categoria.GPU to "GPU",
            Categoria.CPU to "CPU", Categoria.AUDIO to "ÁUDIO", Categoria.REDE to "REDE",
            Categoria.VISUAL to "VISUAL", Categoria.TOQUE to "TOQUE"
        )
        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = categorias.size
            override fun createFragment(position: Int) = ToggleListFragment.novo(categorias[position].first)
        }
        TabLayoutMediator(tabLayout, viewPager) { tab, pos -> tab.text = categorias[pos].second }.attach()
    }

    /** Botão pedido: mostra CPU + GPU + Toque juntos numa lista só. */
    private fun abrirVerTudo() {
        val itens = com.anjo.adb.data.ToggleRegistry.todos.filter {
            it.categoria == Categoria.CPU || it.categoria == Categoria.GPU || it.categoria == Categoria.TOQUE
        }
        val dialogView = layoutInflater.inflate(R.layout.fragment_toggle_list, null)
        val recycler = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerToggles)
        recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        recycler.adapter = ToggleCardAdapter(itens, repo) { _, _ -> }
        dialogView.findViewById<android.view.View>(R.id.btnAplicar).setOnClickListener {
            for (item in itens) {
                com.anjo.adb.core.ToggleActionMapper.aplicar(this, item.id, repo.isAtivo(item.id))
            }
        }
        AlertDialog.Builder(this)
            .setTitle("Tudo: CPU + GPU + Toque")
            .setView(dialogView)
            .setNegativeButton("Fechar", null)
            .show()
    }

    private fun alternarBolinha(ativo: Boolean) {
        repo.setBolinhaAtiva(ativo)
        if (ativo) {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                return
            }
            startService(Intent(this, FloatingBubbleService::class.java))
        } else {
            stopService(Intent(this, FloatingBubbleService::class.java))
        }
    }
}
