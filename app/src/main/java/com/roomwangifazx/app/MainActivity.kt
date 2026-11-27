package com.roomwangifazx.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val bg = Color.rgb(8, 8, 10)
    private val panel = Color.rgb(22, 15, 18)
    private val red = Color.rgb(245, 48, 70)
    private val muted = Color.rgb(170, 155, 160)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var body: LinearLayout
    private var profileName = "Room Wangi"
    private val terminalLines = listOf(
        "[BOOT] ROOM WANGI FAZX client initialized",
        "[CHECK] Android VPN hand-off ready",
        "[INFO] Security checklist loaded locally",
        "[INFO] Waiting for user action..."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    private fun base(title: String, subtitle: String) {
        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(28))
        }
        val brand = text("ROOM WANGI FAZX", 13f, red, true)
        page.addView(brand)
        page.addView(text(title, 27f, Color.WHITE, true))
        page.addView(text(subtitle, 13f, muted, false).apply {
            setPadding(0, dp(4), 0, dp(18))
        })
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(body)
        scroll.addView(page)
        setContentView(scroll)
    }

    private fun showHome() {
        base("VPN CONTROL", "Secure connection launcher • Android")
        card(body, "CONNECTION") {
            addView(text("VPN PROFILE", 11f, muted, true))
            val profile = EditText(this@MainActivity).apply {
                setText(profileName)
                setTextColor(Color.WHITE)
                setHintTextColor(muted)
                hint = "Nama profile OpenVPN"
                setSingleLine(true)
                setPadding(dp(12), dp(10), dp(12), dp(10))
                setBackgroundColor(Color.rgb(35, 25, 29))
            }
            addView(profile, matchWrap())
            addView(button("OPEN OPENVPN CLIENT") {
                profileName = profile.text.toString().trim().ifBlank { "Room Wangi" }
                launchOpenVpn(profileName)
            })
            addView(text(
                "Catatan: profile harus sudah diimpor di aplikasi OpenVPN for Android. " +
                "Aplikasi ini memakai hand-off resmi, bukan menanam ulang kode GPL.",
                12f, muted, false
            ))
        }
        card(body, "QUICK ACCESS") {
            addView(button("SECURITY DASHBOARD") { showSecurity() })
            addView(button("TERMINAL SIMULATION") { showTerminal() })
            addView(button("OPENVPN PROFILE HELP") { showVpnHelp() })
        }
        card(body, "STATUS") {
            addView(text("CLIENT UI: READY", 13f, Color.rgb(100, 220, 150), true))
            addView(text("VPN TUNNEL: NOT VERIFIED BY THIS APP", 12f, muted, false))
            addView(text("This screen does not claim a VPN is connected until the OpenVPN client reports it.", 12f, muted, false))
        }
    }

    private fun showSecurity() {
        base("SECURITY CENTER", "Checklist lokal untuk akun Google milik sendiri")
        card(body, "ACCOUNT SECURITY CHECKLIST") {
            addView(text("Checklist ini hanya panduan. Aplikasi tidak membaca password, OTP, email, atau data akun Google.", 12f, muted, false))
            listOf(
                "Aktifkan verifikasi 2 langkah",
                "Periksa perangkat yang login",
                "Tinjau aktivitas keamanan terbaru",
                "Pastikan email dan nomor pemulihan benar",
                "Gunakan password unik dan kuat",
                "Hapus akses aplikasi pihak ketiga yang tidak dikenal"
            ).forEach { label ->
                addView(CheckBox(this@MainActivity).apply {
                    text = label
                    setTextColor(Color.WHITE)
                    buttonTintList = android.content.res.ColorStateList.valueOf(red)
                    textSize = 14f
                    setPadding(0, dp(5), 0, dp(5))
                })
            }
            addView(button("GOOGLE ACCOUNT SECURITY") {
                openUrl("https://myaccount.google.com/security")
            })
            addView(button("GOOGLE ACCOUNT RECOVERY") {
                openUrl("https://accounts.google.com/signin/recovery")
            })
        }
        body.addView(button("BACK TO DASHBOARD") { showHome() })
    }

    private fun showTerminal() {
        base("TERMINAL", "Visual simulation • no network scanning")
        val output = TextView(this).apply {
            setTextColor(Color.rgb(255, 90, 110))
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.rgb(15, 10, 12))
            text = "CART3R / ROOM WANGI FAZX\n"
        }
        body.addView(output, matchWrap())
        body.addView(button("RUN LOCAL SIMULATION") {
            output.text = "CART3R / ROOM WANGI FAZX\n"
            var index = 0
            val task = object : Runnable {
                override fun run() {
                    if (index < terminalLines.size) {
                        output.append(terminalLines[index] + "\n")
                        index++
                        handler.postDelayed(this, 550)
                    }
                }
            }
            handler.post(task)
        })
        body.addView(button("BACK TO DASHBOARD") { showHome() })
    }

    private fun showVpnHelp() {
        base("OPENVPN SETUP", "Import profile once in the OpenVPN client")
        card(body, "STEPS") {
            addView(text("1. Install OpenVPN for Android from its official distribution.\n\n" +
                "2. Import the .ovpn profile provided by your VPN administrator.\n\n" +
                "3. Return to ROOM WANGI FAZX and enter the exact profile name.\n\n" +
                "4. Tap OPEN OPENVPN CLIENT and approve Android's VPN permission if requested.",
                14f, Color.WHITE, false))
            addView(text("The VPN provider must supply a valid server and credentials. This starter app does not generate VPN servers or credentials.", 12f, muted, false))
        }
        body.addView(button("BACK TO DASHBOARD") { showHome() })
    }

    private fun launchOpenVpn(name: String) {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                setClassName("de.blinkt.openvpn", "de.blinkt.openvpn.api.ConnectVPN")
                putExtra("de.blinkt.openvpn.api.profileName", name)
            }
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "OpenVPN for Android belum terpasang atau profile tidak tersedia.", Toast.LENGTH_LONG).show()
            openUrl("https://github.com/schwabe/ics-openvpn")
        } catch (e: Exception) {
            Toast.makeText(this, "Tidak dapat membuka OpenVPN: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openUrl(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: Exception) { Toast.makeText(this, "Link tidak dapat dibuka.", Toast.LENGTH_SHORT).show() }
    }

    private fun card(parent: LinearLayout, title: String, content: LinearLayout.() -> Unit) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(14), dp(15), dp(14))
            setBackgroundColor(panel)
        }
        box.addView(text(title, 12f, red, true))
        box.content()
        val params = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) }
        parent.addView(box, params)
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(5), 0, dp(5))
        }

    private fun button(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(red)
            setOnClickListener { action() }
            val p = LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(9)
                bottomMargin = dp(5)
            }
            layoutParams = p
        }

    private fun matchWrap() = LinearLayout.LayoutParams(-1, -2)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
