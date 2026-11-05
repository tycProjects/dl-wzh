package com.nexus.launcher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.nexus.launcher.adapter.GameIconAdapter
import com.nexus.launcher.model.GameApp
import com.nexus.launcher.service.OverlayService
import com.nexus.launcher.utils.GameRepository
import com.nexus.launcher.utils.GameUtils

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var rvRecentIcons: RecyclerView
    private lateinit var ivSelectedIcon: ImageView
    private lateinit var tvSelectedName: TextView
    private lateinit var tvSelectedPlaytime: TextView
    private lateinit var btnOpen: MaterialButton
    private lateinit var btnMenu: ImageView
    private lateinit var btnAddGame: ImageView
    private lateinit var emptyState: View
    private lateinit var selectedContainer: View

    private var games: MutableList<GameApp> = mutableListOf()
    private var selectedGame: GameApp? = null
    private var emulatorMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        loadSavedGames()
        setupDrawer()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        loadSavedGames()
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        rvRecentIcons = findViewById(R.id.rv_recent_icons)
        ivSelectedIcon = findViewById(R.id.iv_selected_icon)
        tvSelectedName = findViewById(R.id.tv_selected_name)
        tvSelectedPlaytime = findViewById(R.id.tv_selected_playtime)
        btnOpen = findViewById(R.id.btn_open)
        btnMenu = findViewById(R.id.btn_menu)
        btnAddGame = findViewById(R.id.btn_add_game)
        emptyState = findViewById(R.id.empty_state)
        selectedContainer = findViewById(R.id.selected_game_container)
    }

    private fun loadSavedGames() {
        games = GameRepository.getSavedGames(this).toMutableList()

        if (games.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            selectedContainer.visibility = View.GONE
            rvRecentIcons.visibility = View.GONE
            btnOpen.isEnabled = false
            return
        }

        emptyState.visibility = View.GONE
        selectedContainer.visibility = View.VISIBLE
        rvRecentIcons.visibility = View.VISIBLE

        rvRecentIcons.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvRecentIcons.adapter = GameIconAdapter(games) { game ->
            selectGame(game)
        }

        val prevPkg = selectedGame?.packageName
        val stillThere = games.find { it.packageName == prevPkg }
        selectGame(stillThere ?: games[0])
    }

    private fun selectGame(game: GameApp) {
        selectedGame = game
        ivSelectedIcon.setImageDrawable(game.icon)
        tvSelectedName.text = game.name
        tvSelectedPlaytime.text =
            "Dimainkan ${game.getFormattedPlayTime()} dalam 30 hari terakhir"
        btnOpen.isEnabled = true
    }

    private fun setupDrawer() {
        val totalMinutes = games.sumOf { it.playTimeMinutes }
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        findViewById<TextView>(R.id.tv_total_playtime)?.text =
            if (games.isEmpty()) "0 jam 0 mnt" else "$hours jam $mins mnt"

        val recentList = findViewById<LinearLayout>(R.id.recent_games_list)
        recentList?.removeAllViews()

        games.take(4).forEach { game ->
            val item = LayoutInflater.from(this)
                .inflate(R.layout.item_recent_game, recentList, false)
            item.findViewById<ImageView>(R.id.iv_game_icon).setImageDrawable(game.icon)
            item.findViewById<TextView>(R.id.tv_game_name).text = game.name
            item.findViewById<TextView>(R.id.tv_game_time).text = game.getFormattedPlayTime()
            item.setOnLongClickListener {
                confirmRemoveGame(game)
                true
            }
            recentList?.addView(item)
        }

        findViewById<SwitchMaterial>(R.id.switch_emulator)?.apply {
            isChecked = emulatorMode
            setOnCheckedChangeListener { _, checked ->
                emulatorMode = checked
                Toast.makeText(
                    this@MainActivity,
                    if (checked) "Mode Emulator / Desktop aktif" else "Mode Emulator nonaktif",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupClickListeners() {
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        btnAddGame.setOnClickListener {
            showAddGameDialog()
        }

        findViewById<View>(R.id.btn_empty_add)?.setOnClickListener {
            showAddGameDialog()
        }

        btnOpen.setOnClickListener {
            selectedGame?.let { game ->
                if (!Settings.canDrawOverlays(this)) {
                    showOverlayPermissionDialog()
                    return@setOnClickListener
                }

                val serviceIntent = Intent(this, OverlayService::class.java).apply {
                    putExtra(OverlayService.EXTRA_PACKAGE, game.packageName)
                }
                startForegroundService(serviceIntent)

                GameUtils.launchGame(this, game.packageName)
                Toast.makeText(this, "Membuka ${game.name}...", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<View>(R.id.btn_settings)?.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        findViewById<View>(R.id.item_cache)?.setOnClickListener {
            GameUtils.clearMemory(this)
            Toast.makeText(this, "Memori dikosongkan!", Toast.LENGTH_SHORT).show()
            findViewById<TextView>(R.id.tv_cache_size)?.text = "Data yang di-cache: 0 B"
        }

        findViewById<View>(R.id.item_add_game)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            showAddGameDialog()
        }

        findViewById<View>(R.id.item_album)?.setOnClickListener {
            Toast.makeText(this, "Album game (segera hadir)", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.item_other_settings)?.setOnClickListener {
            Toast.makeText(this, "Pengaturan lainnya (segera hadir)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddGameDialog() {
        val allApps = GameRepository.getAllLaunchableApps(this)
        val savedPkgs = games.map { it.packageName }.toSet()
        val available = allApps.filter { it.packageName !in savedPkgs }

        if (available.isEmpty()) {
            Toast.makeText(this, "Semua aplikasi sudah ditambahkan", Toast.LENGTH_SHORT).show()
            return
        }

        val names = available.map { it.name }.toTypedArray()
        val checked = BooleanArray(available.size)

        MaterialAlertDialogBuilder(this)
            .setTitle("Tambah Game / Aplikasi")
            .setMultiChoiceItems(names, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton("Tambah") { _, _ ->
                var added = 0
                checked.forEachIndexed { index, isChecked ->
                    if (isChecked) {
                        GameRepository.addGame(this, available[index].packageName)
                        added++
                    }
                }
                if (added > 0) {
                    Toast.makeText(this, "$added game ditambahkan", Toast.LENGTH_SHORT).show()
                    loadSavedGames()
                    setupDrawer()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun confirmRemoveGame(game: GameApp) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Hapus dari daftar?")
            .setMessage("Hapus \"${game.name}\" dari Nexus Launcher?")
            .setPositiveButton("Hapus") { _, _ ->
                GameRepository.removeGame(this, game.packageName)
                Toast.makeText(this, "${game.name} dihapus", Toast.LENGTH_SHORT).show()
                loadSavedGames()
                setupDrawer()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showOverlayPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.izin_overlay)
            .setMessage(R.string.izin_overlay_desc)
            .setPositiveButton(R.string.buka_pengaturan) { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
            .setNegativeButton(R.string.batal, null)
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
