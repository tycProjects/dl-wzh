package com.smali.fox

import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.formatter.DexFormatter
import java.io.File
import java.util.zip.ZipFile

/** Uses the same dexlib2 family as the original APK for class/method discovery. */
object DexScanner {
    const val PREMIUM_KEYWORD_COUNT = 2000
    data class MethodResult(val owner: String, val name: String, val descriptor: String, val category: String, val confidence: Int, val body: String, val keywords: List<String>)
    data class ZMethodResult(val owner: String, val name: String, val descriptor: String, val body: String)
    data class Result(val dexCount: Int, val stringsCount: Int, val methods: List<MethodResult>, val zMethods: List<ZMethodResult>)

    private val keywords = listOf("purchase", "billing", "subscribe", "subscription", "product", "premium", "vip", "unlock", "payment", "checkout", "order", "pay", "iap", "pro", "entitlement", "access_granted", "accesslevel", "license_valid", "trial_active", "trial_expired", "feature_unlocked", "ispaid", "islicensed", "remove_ads", "ad_free", "membership", "userlevel", "premium_active", "premium_key", "premium_status", "premium_unlocked", "pro_active", "pro_status", "pro_unlocked", "vip_active", "vip_key", "vip_status", "vip_unlocked", "vip_user", "subscription_active", "subscription_valid", "subscription_expired", "subscription_status", "purchase_token", "querypurchases", "verifyentitlement", "verifylicense", "sku_purchase", "sku_subscription", "شراء", "اشتراك", "دفع", "مدفوع")
    private val worldKeywords = listOf("premium_active", "premium_status", "premium_unlocked", "pro_active", "pro_status", "vip_active", "vip_status", "subscription_active", "subscription_valid", "purchase_token", "verify_entitlement", "entitlement_active", "access_granted", "license_valid", "trial_active", "trial_expired", "feature_unlocked", "ispaid", "islicensed", "hasaccess", "hasentitlement", "checkentitlement", "verifyaccess", "checklicense", "validatelicense", "validatepurchase", "verifypurchase", "purchasehistory", "purchaseoriginaljson", "queryproductdetails", "queryproductdetailsasync", "querypurchasesasync", "restorepurchase", "acknowledgepurchase", "consumepurchase", "billingclient", "billingresult", "customerinfo", "entitlementinfo", "offerings", "offering", "paywall", "premiumfeature", "premiumaccess", "premiumenabled", "premiumstate", "premiumcheck", "premiumflag", "subscriptionlevel", "subscriptiontier", "subscriptionplan", "subscriptionpurchase", "subscribepro", "subscriberpro", "unlockall", "fullversion", "fullaccess", "paiduser", "paidversion", "adfree", "noads", "removeads", "featureflag", "featureenabled", "membershipstatus", "memberlevel", "signed_purchase", "is_licensed", "is_entitled", "has_entitlement", "abonnement", "abonnement_actif", "achat", "achete", "premium_actif", "licence_valide", "suscripcion", "suscripcion_activa", "compra", "comprado", "premium_activo", "licencia_valida", "assinatura", "assinatura_ativa", "compra_realizada", "licenca_valida", "abonnement_aktiv", "zahlung", "gekauft", "lizenz_gueltig", "abonament", "cumparare", "licenta_valida", "подписка", "покупка", "куплено", "лицензия", "премиум", "оплата", "اشتراك", "شراء", "مشتريات", "بريميوم", "مميز", "مدفوع", "ترخيص", "تفعيل", "نسخة_كاملة", "فتح_المميزات", "بدون_اعلانات", "회원권", "구독", "구매", "premium_aktif", "langganan", "beli", "lisensi_valid", "abonelik", "satınalma", "gecerli_lisans")
    /** Exactly 2,000 deterministic, semantically meaningful Premium/Billing tokens. */
    private val premiumKeywordBank: Set<String> by lazy {
        // Internal Premium/entitlement vocabulary only; deliberately excludes Play Store and billing-provider names.
        val roots = listOf("premium", "vip", "pro", "plus", "member", "membership", "subscriber", "subscription", "entitlement", "entitled", "license", "licence", "access", "feature", "content", "account", "profile", "user", "role", "plan", "tier", "level", "status", "state", "flag", "mode", "edition", "version", "package", "bundle", "module", "capability", "permission", "privilege", "authority", "identity", "credential", "activation", "activate", "unlock", "unlocked", "paid", "adfree", "noads", "trial", "intro", "lifetime", "monthly", "yearly", "annual", "quarterly", "renewal", "validity", "accesskey", "licensekey", "premiumkey", "vipkey", "prokey", "memberkey", "entitlementkey", "featurekey", "pluskey", "is", "has", "can", "check", "verify", "validate", "isvalid", "isactive", "isenabled", "isowned", "ispaid", "ispremium", "isvip", "ispro", "isplus", "hasaccess", "haspremium", "hasentitlement", "haslicense", "hasmembership", "hasactive", "checkpremium", "checkaccess", "checklicense", "checkentitlement", "verifypremium", "verifyaccess", "verifylicense", "verifyentitlement", "validatepremium", "validateaccess", "validatelicense", "validateentitlement")
        val suffixes = listOf("active", "inactive", "enabled", "disabled", "valid", "invalid", "verified", "unverified", "expired", "available", "unavailable", "owned", "notowned", "unlocked", "locked", "paid", "free", "premium", "vip", "pro", "plus", "member", "membership", "subscriber", "subscription", "entitlement", "entitled", "license", "licence", "access", "feature", "content", "account", "profile", "user", "role", "plan", "tier", "level", "status", "state", "flag", "mode", "edition", "version", "package", "bundle", "module", "capability", "permission", "privilege", "authority", "identity", "credential", "activation", "activated", "activationstate", "unlock", "unlocked", "adfree", "noads", "trial", "trialactive", "trialexpired", "intro", "introactive", "lifetime", "monthly", "yearly", "annual", "quarterly", "renewal", "renewed", "cancelled", "suspended", "revoked", "blocked", "allowed", "denied", "granted", "rejected", "present", "missing", "known", "unknown", "key", "token", "secret", "signature", "hash", "claim", "claims", "metadata", "config", "configuration", "settings", "limits", "quota", "duration", "expiry", "expiration", "start", "end", "date", "time", "check", "checker", "checking", "verify", "verified", "verification", "validate", "validated", "validation", "entitlementcheck", "entitlementstate", "entitlementstatus", "accesscheck", "accessstate", "accessstatus", "licensecheck", "licensestate", "licensestatus", "premiumcheck", "premiumstate", "premiumstatus", "premiumflag", "premiumfeature", "premiumcontent", "premiumaccess", "premiumenabled", "premiumuser", "premiumaccount", "vipcheck", "vipstate", "vipstatus", "procheck", "prostate", "prostatus", "pluscheck", "plusstate", "plusstatus", "membercheck", "memberstate", "memberstatus", "subscriptioncheck", "subscriptionstate", "subscriptionstatus", "subscriptionactive", "subscriptionvalid", "featurecheck", "featurestate", "featurestatus", "featureenabled", "contentaccess", "fullaccess", "fullversion", "fullfeature", "allfeatures", "removeads", "adremoval", "restoreaccess", "retainaccess", "grantaccess", "revokeaccess", "setpremium", "clearpremium", "enablepremium", "disablepremium", "setentitlement", "clearentitlement", "setlicense", "clearmembership", "setmembership")
        val separators = listOf("", "_", "-", ".")
        roots.flatMap { root -> suffixes.flatMap { suffix -> separators.map { separator -> "${root}${separator}${suffix}" } } }.distinct().take(2000).toSet()
    }

    // Backward-compatible alias: no fabricated placeholder tokens are included.
    private val generatedKeywordBank: Set<String> by lazy { premiumKeywordBank }
    private val quickWords = keywords + worldKeywords + premiumKeywordBank + generatedKeywordBank + listOf("revenuecat", "qonversion", "adapty", "glassfy", "apphud", "superwall", "purchasely", "playbilling", "googleplaybilling", "purchaseRegex", "purchaseExtraRegex")

    fun scanApk(path: String, onMethodFound: (MethodResult) -> Unit = {}, onDexStart: (String, Int, Int) -> Unit = { _, _, _ -> }, onProgress: (String, Int, Int) -> Unit = { _, _, _ -> }, onZMethodFound: (ZMethodResult) -> Unit = {}): Result {
        val methods = mutableListOf<MethodResult>(); val zMethods = mutableListOf<ZMethodResult>(); var dexCount = 0; var stringsCount = 0; var hiddenCount = 0
        val tempDir = File.createTempFile("editorpremium", "dexdir").apply { delete(); mkdirs() }
        try {
            ZipFile(path).use { zip ->
                val dexEntries = zip.entries().asSequence().filter { it.name.matches(Regex("classes\\d*\\.dex")) }.toList()
                dexEntries.forEachIndexed { index, entry ->
                    dexCount++
                    onDexStart(entry.name, index + 1, dexEntries.size)
                    val dexPath = File(tempDir, entry.name.replace('/', '_'))
                    zip.getInputStream(entry).use { input -> dexPath.outputStream().use { output -> input.copyTo(output) } }
                    val dex = DexFileFactory.loadDexFile(dexPath, Opcodes.forApi(35))
                    stringsCount += dex.stringReferences.count()
                    var scannedInDex = 0
                    var zInDex = 0
                    dex.classes.forEach { cls -> cls.methods.forEach { method ->
                        scannedInDex++
                        if (method.returnType == "Z") zInDex++
                        if (scannedInDex == 1 || scannedInDex % 25 == 0 || method.returnType == "Z") onProgress(entry.name, scannedInDex, zInDex)
                        // Reject non-boolean methods before formatting Smali. Formatting
                        // every method was the main reason streaming appeared frozen.
                        if (method.returnType != "Z") return@forEach
                        val zBody = formatSmali(cls.type, method)
                        val zResult = ZMethodResult(cls.type, method.name, descriptor(method), zBody)
                        zMethods += zResult
                        onZMethodFound(zResult)
                        val quickSignature = "${cls.type} ${method.name}".lowercase()
                        val namedCandidate = hasExplicitPremiumNameSignal(quickSignature) || (isCheckName(method.name) && quickWords.any { quickSignature.contains(it) })
                        val hiddenCandidate = isObfuscated(cls.type, method.name)
                        if (!namedCandidate && !hiddenCandidate) return@forEach
                        // Inspect raw instructions before formatting Smali. Hidden methods need
                        // a real semantic reference plus a real decision path; a generic branch
                        // or a short obfuscated method is never enough.
                        val rawScore = rawPremiumDecisionScore(method, quickSignature)
                        if (rawScore < 4 || !hasRealBooleanDecision(method)) return@forEach
                        val body = zBody
                        val detectedKeywords = detectKeywords(cls.type, method.name, body)
                        val score = smartScore(cls.type, method.name, method.returnType, body, detectedKeywords)
                        // Strong multi-signal classifier: a name alone is never enough.
                        // Require Premium vocabulary plus executable/state evidence.
                        val evidenceScore = premiumEvidenceScore(cls.type, method.name, body, detectedKeywords)
                        if (evidenceScore < 10 || !hasConcretePremiumSignal(cls.type, method.name, body) || !isOriginalStylePremiumCheck(cls.type, method.name, body, detectedKeywords)) return@forEach
                        val explicit = classify("${cls.type} ${method.name}")
                        val hidden = explicit == null
                        if (hidden && hiddenCount++ >= 80) return@forEach
                        val category = explicit ?: "تحقق شراء مخفي (Z)"
                        val result = MethodResult(cls.type, method.name, descriptor(method), category, confidence(category, score + evidenceScore / 2), body, detectedKeywords)
                        methods += result
                        onMethodFound(result)
                    } }
                }
            }
        } finally { tempDir.deleteRecursively() }
        return Result(dexCount, stringsCount, methods.distinctBy { it.owner + it.name + it.descriptor }, zMethods.distinctBy { it.owner + it.name + it.descriptor })
    }

    private fun descriptor(method: Method): String = "(${method.parameterTypes.joinToString("")})${method.returnType}"
    private fun classify(value: String): String? {
        val lower = value.lowercase(); val hit = keywords.firstOrNull { lower.contains(it.lowercase()) } ?: return null
        return when (hit) { "billing", "iap", "payment", "pay", "دفع" -> "دفع وفوترة"; "subscribe", "subscription", "اشتراك" -> "اشتراكات"; "premium", "vip", "pro", "unlock", "مدفوع" -> "Premium / VIP"; else -> "شراء ومنتجات" }
    }
    private fun confidence(category: String, score: Int) = (75 + score * 3).coerceAtMost(99)
    private val explicitPremiumTokens = listOf(
        "premium", "premiumactive", "premiumenabled", "premiumstatus", "premiumstate", "premiumflag",
        "haspremium", "ispremium", "checkpremium", "verifypremium", "premiumaccess", "premiumuser",
        "vipactive", "vipstatus", "vipuser", "isvip", "proactive", "prostatus", "ispro",
        "plusactive", "plusstatus", "isplus", "hasentitlement", "isentitled", "entitlementactive",
        "validlicense", "licensevalid", "licensed", "checklicense", "verifylicense", "license_valid",
        "membershipstatus", "hasmembership", "subscriptionactive", "subscriptionvalid", "subscriptionstatus",
        "fullaccess", "allfeatures", "unlockpremium", "removeads", "adfree", "noads", "access_granted",
        "premium_active", "premium_status", "premium_unlocked", "pro_active", "vip_active", "entitlement_active",
        "بريميوم", "مميز", "مدفوع", "ترخيص", "عضوية", "نسخةكاملة", "وصولكامل", "بدوناعلانات"
    )
    private fun hasExplicitPremiumNameSignal(signature: String): Boolean = explicitPremiumTokens.any { signature.contains(it) }
    private fun hasConcretePremiumSignal(owner: String, name: String, body: String): Boolean {
        val source = "$owner $name $body".lowercase().replace("_", "")
        return explicitPremiumTokens.count { source.contains(it.replace("_", "")) } >= 1
    }
    private val purchaseSemanticTokens = listOf(
        "purchase", "purchased", "purchaseinfo", "purchasestate", "purchasetoken", "purchasehistory",
        "ownership", "owned", "entitlement", "entitled", "premium", "vip", "pro", "plus", "license",
        "licence", "subscription", "membership", "accessgranted", "fullaccess", "unlockpremium",
        "removeads", "adfree", "noads", "ispaid", "haspremium", "ispremium", "isvip", "ispro", "isplus",
        "verifylicense", "validlicense", "checkentitlement", "premiumactive", "premiumstatus", "licensevalid",
        "بريميوم", "مدفوع", "ترخيص", "عضوية", "شراء", "مشتريات", "وصولكامل"
    )
    private fun rawPremiumDecisionScore(method: Method, signature: String): Int {
        val impl = method.implementation ?: return 0
        var semantic = 0; var score = 0; var hasField = false; var hasInvoke = false; var hasBranch = false; var hasResult = false; var hasReturn = false
        val signatureLower = signature.lowercase().replace("_", "")
        if (purchaseSemanticTokens.any { signatureLower.contains(it) }) semantic++
        impl.instructions.take(120).forEach { instruction ->
            val opcode = instruction.opcode.name.lowercase()
            val raw = buildString { append(opcode); if (instruction is ReferenceInstruction) append(" ").append(DexFormatter.INSTANCE.getReference(instruction.reference).lowercase()) }.replace("_", "")
            if (purchaseSemanticTokens.any { raw.contains(it) }) semantic++
            if (opcode.startsWith("iget") || opcode.startsWith("sget")) { hasField = true; score++ }
            if (opcode.startsWith("invoke-")) { hasInvoke = true; score++ }
            if (opcode.startsWith("if-")) { hasBranch = true; score++ }
            if (opcode == "move-result" || opcode == "move-result-object") { hasResult = true; score++ }
            if (opcode == "return-boolean") { hasReturn = true; score++ }
        }
        if (semantic > 0) score += 2
        if (hasField && hasInvoke) score++
        if (hasBranch && hasResult) score++
        return score
    }
    private fun hasRealBooleanDecision(method: Method): Boolean {
        val instructions = method.implementation?.instructions?.toList() ?: return false
        val hasBranch = instructions.any { it.opcode.name.startsWith("if-") }
        val hasReturn = instructions.any { it.opcode.name == "return-boolean" }
        val hasStateOrCall = instructions.any { val op = it.opcode.name; op.startsWith("iget") || op.startsWith("sget") || op.startsWith("invoke-") }
        return hasBranch && hasReturn && hasStateOrCall
    }
    private fun smartScore(owner: String, name: String, returnType: String, body: String, detected: List<String>): Int {
        if (returnType != "Z") return 0
        var score = detected.size * 2
        if (isCheckName(name)) score += 2
        if (isBillingApi(body)) score += 4
        return score
    }
    /** Weighted Premium detector. It combines semantic, bytecode, and state signals. */
    private fun premiumEvidenceScore(owner: String, name: String, body: String, detected: List<String>): Int {
        val signature = "$owner $name".lowercase()
        val code = body.lowercase()
        var score = 0
        val strongHits = strongOriginalTokens.count { signature.contains(it) || code.contains(it) }
        val internalHits = listOf(
            "premium", "premiumactive", "premiumenabled", "premiumstatus", "premiumstate", "premiumflag",
            "haspremium", "ispremium", "checkpremium", "verifypremium", "premiumaccess", "premiumuser",
            "isvip", "vipactive", "vipstatus", "vipuser", "ispro", "proactive", "prostatus",
            "isplus", "plusactive", "plusstatus", "hasentitlement", "isentitled",
            "license", "licence", "validlicense", "licensed", "checklicense", "verifylicense",
            "membership", "memberstatus", "subscriptionactive", "subscriptionvalid", "subscriptionstatus",
            "fullaccess", "featureenabled", "unlocked", "unlockpremium", "removeads", "adfree", "noads",
            "premium_active", "premium_status", "premium_unlocked", "pro_active", "vip_active", "license_valid",
            "entitlement_active", "access_granted", "is_paid", "ispaid", "مدفوع", "بريميوم", "مميز", "ترخيص", "عضوية"
        ).count { signature.contains(it) || code.contains(it) }
        score += (strongHits * 3).coerceAtMost(12)
        score += (internalHits * 2).coerceAtMost(12)
        if (detected.isNotEmpty()) score += 2
        if (isCheckName(name)) score += 2
        if (body.contains("if-", true) || body.contains("if_", true)) score += 2
        if (body.contains("return", true) && body.contains("return-boolean", true)) score += 2
        if (body.contains("iget", true) || body.contains("sget", true)) score += 2
        if (body.contains("invoke-", true) || body.contains("invoke_", true)) score += 1
        if (body.contains("const-string", true)) score += 1
        // Penalize generic flags unless they also carry a concrete Premium signal.
        val genericOnly = listOf("featureenabled", "isenabled", "isactive", "enabled", "visible", "debug", "testmode").any { signature.contains(it) } && internalHits == 0
        if (genericOnly) score -= 5
        return score
    }
    private fun detectKeywords(owner: String, name: String, body: String): List<String> {
        val source = "$owner $name $body"
        val words = listOf("purchase", "purchased", "haspurchased", "ispurchased", "purchaseinfo", "purchasestate", "purchasetoken", "purchase_token", "purchasecheck", "purchasechecker", "purchasevalidation", "purchase_history", "purchasehistoryrecord", "purchaseoriginaljson", "purchase_original_json", "purchasebuy", "validatepurchase", "verifypurchase", "billing", "billingclient", "billingresult", "billingresponse", "billingservice", "billingmanager", "payment", "paymentstate", "paymentstatus", "paymentverified", "verifypayment", "paywall", "premium", "premiumuser", "premiumfeature", "premiumcontent", "premiumaccess", "premiumenabled", "premiumstatus", "premiumstate", "premiumcheck", "checkpremium", "ispremium", "haspremium", "vip", "vipuser", "vip_active", "vip_key", "vip_status", "vip_unlocked", "vip_user", "vipflag", "vipuser", "user_is_vip", "setvip", "setpremium", "vipdisplaykeywords", "pro", "ispro", "haspro", "proversion", "profeature", "proaccess", "subscription", "subscribed", "subscribe", "subscribe_pro", "subscribepro", "subscriberpro", "issubscribed", "subscription_active", "subscription_ended", "subscription_expired", "subscription_level", "subscription_purchase", "subscription_purchase_origin", "subscription_status", "subscription_tier", "subscription_valid", "subscriptionkey", "subscriptionstate", "subscriptionstatus", "subscriptionplan", "entitlement", "entitlements", "hasentitlement", "isentitled", "entitled", "entitlementstatus", "checkentitlement", "verifyentitlement", "accessentitlement", "licensed", "license", "licence", "validlicense", "licensecheck", "checklicense", "verifylicense", "activation", "activated", "activate", "activationstatus", "unlocked", "unlock", "unlock_all", "unlockall", "unlocks", "unlockpremium", "unlockpro", "fullversion", "fullaccess", "paid", "paiduser", "paidversion", "removeads", "remove_ads", "remove_ads_purchase", "adfree", "ad_free", "noads", "no_ads", "product", "productdetails", "productid", "producttype", "sku", "sku_purchase", "sku_subscription", "skudetails", "queryproductdetails", "queryproductdetailsasync", "inapp", "inappbilling", "order", "checkout", "owns", "owner", "hasaccess", "accesslevel", "hasaccesslevel", "featureenabled", "isenabled", "isactive", "isvalid", "querypurchases", "querypurchasesasync", "getpurchases", "getpurchase", "getpurchasehistory", "acknowledgepurchase", "consumepurchase", "purchasehistory", "iap", "entitlementinfo", "verifylicense", "restore_purchase", "signed_purchase", "playbilling", "googleplaybilling", "purchaseRegex", "purchaseExtraRegex", "vipArabicTag", "skipped_premium_offerings", "mymembership", "membershipstatus", "memberlevel", "premiumlevel", "premiumflag", "featureflag", "paidfeature", "profeatureenabled", "شراء", "مشتريات", "تحققشراء", "حالةالشراء", "اشتراك", "اشتراكات", "حالةالاشتراك", "دفع", "مدفوع", "مميز", "بريميوم", "عضوية", "ترخيص", "فحصالترخيص", "تفعيل", "فتح", "نسخةكاملة", "وصولكامل", "بدوناعلانات", "ازالةالاعلانات")
        return (words + worldKeywords + premiumKeywordBank + generatedKeywordBank + originalTokens.toList()).filter { source.contains(it, true) }.distinct()
    }
    private val premiumKeywords = setOf("premium", "premiumuser", "premiumfeature", "premiumcontent", "premiumaccess", "premiumenabled", "premiumstatus", "premiumcheck", "ispremium", "haspremium", "vip", "isvip", "vipuser", "vip_active", "vip_key", "vip_status", "vip_unlocked", "vip_user", "vipflag", "user_is_vip", "setvip", "setpremium", "pro", "ispro", "proversion", "profeature", "proaccess", "subscription_active", "subscription_valid", "subscription_status", "subscription_expired", "subscription_purchase", "subscription_level", "subscribepro", "subscriberpro", "unlock", "unlock_all", "unlockall", "unlockpremium", "unlockpro", "unlocked", "fullversion", "fullaccess", "paid", "paiduser", "remove_ads_purchase", "removeads", "adfree", "noads", "entitlement", "entitlements", "hasentitlement", "isentitled", "entitled", "checkentitlement", "verifyentitlement", "licensed", "license", "validlicense", "verifylicense", "activation", "activated", "featureenabled", "hasaccess", "accesslevel", "hasaccesslevel", "revenuecat", "customerinfo", "entitlementinfo", "qonversion", "adapty", "glassfy", "apphud", "superwall", "purchasely", "hasactivesubscription", "issubscribed", "mymembership", "membershipstatus", "premiumlevel", "premiumflag", "مميز", "بريميوم", "مدفوع", "عضوية", "ترخيص", "تفعيل", "نسخةكاملة", "بدوناعلانات")
    private fun isBillingApi(body: String): Boolean = listOf("billingclient", "billingresult", "querypurchases", "getpurchases", "acknowledgepurchase", "consumepurchase", "productdetails", "purchasehistory", "revenuecat", "customerinfo", "entitlementinfo", "qonversion", "adapty", "glassfy", "apphud", "superwall", "purchasely", "hasactivesubscription", "hasaccesslevel", "checkentitlement").any { body.contains(it, true) }
    private fun hasRawPremiumSignal(method: Method): Boolean {
        val implementation = method.implementation ?: return false
        var hasPremium = false
        var hasBehavior = false
        implementation.instructions.take(90).forEach { instruction ->
            val raw = buildString {
                append(instruction.opcode.name.lowercase())
                if (instruction is ReferenceInstruction) append(" ").append(DexFormatter.INSTANCE.getReference(instruction.reference).lowercase())
            }
            if (explicitPremiumTokens.any { raw.replace("_", "").contains(it.replace("_", "")) }) hasPremium = true
            if (raw.startsWith("if-") || raw.startsWith("if_") || raw.startsWith("iget") || raw.startsWith("sget") || raw.startsWith("invoke-")) hasBehavior = true
        }
        return hasPremium && hasBehavior
    }
    private fun hasObfuscatedBooleanShape(method: Method): Boolean {
        val instructions = method.implementation?.instructions?.toList() ?: return false
        val hasBranch = instructions.any { it.opcode.name.startsWith("if-") || it.opcode.name.startsWith("if_") }
        val hasStateRead = instructions.any {
            val opcode = it.opcode.name
            opcode.startsWith("iget") || opcode.startsWith("sget") || opcode.startsWith("invoke-") || opcode.startsWith("invoke_") || opcode == "move-result" || opcode == "move-result-object"
        }
        val hasReturn = instructions.any { it.opcode.name == "return" || it.opcode.name == "return-boolean" || it.opcode.name == "return_" }
        return hasBranch && hasStateRead && hasReturn
    }
    private val referenceSignalWords = setOf("premium", "vip", "pro", "purchase", "billing", "subscription", "entitlement", "license", "unlock", "paywall", "product", "sku", "iap", "adfree", "remove_ads", "querypurchases", "verify", "active", "valid", "owned", "paid", "trial", "membership")
    private fun isOriginalStylePremiumCheck(owner: String, name: String, body: String, detected: List<String>): Boolean {
        val signature = "$owner $name".lowercase()
        val exact = (originalTokens + premiumKeywordBank + generatedKeywordBank).filter { it.length >= 5 && (signature.contains(it, true) || body.contains(it, true)) }
        val hasBranch = body.contains("if-", true) || body.contains("if_", true)
        val hasBooleanReturn = body.contains("return", true)
        val hasStateRead = body.contains("iget", true) || body.contains("sget", true) || body.contains("invoke-", true) || body.contains("const-string", true)
        val hasPurchaseState = exact.any { it in strongOriginalTokens } || exact.any { it in premiumKeywordBank && it.length >= 8 } || exact.any { it in generatedKeywordBank && it.length >= 8 }
        val namedCheckWithBody = isCheckName(name) && hasPurchaseState && hasBooleanReturn && (hasBranch || hasStateRead)
        val strongBody = hasPurchaseState && hasBooleanReturn && (hasBranch || hasStateRead)
        val hiddenBody = isObfuscated(owner, name) && hasBranch && hasBooleanReturn && hasStateRead
        return namedCheckWithBody || strongBody || hiddenBody
    }
    private val strongOriginalTokens = setOf("premium_active", "premium_key", "premium_status", "premium_unlocked", "premiumflag", "premiumkey", "premiumupgrade", "pro_active", "pro_status", "pro_unlocked", "vip_active", "vip_unlocked", "vip_user", "vip_status", "user_is_vip", "verifyentitlement", "verifylicense", "subscription_valid", "subscription_active", "subscription_status", "subscription_expired", "purchase_token", "purchaseoriginaljson", "querypurchases", "queryproductdetails", "sku_purchase", "sku_subscription", "remove_ads_purchase", "restore_purchase", "signed_purchase", "setpremium", "setvip", "haspremium", "ispremium", "hasactivesubscription")
    private val originalTokens = strongOriginalTokens + setOf("premium", "premiumflag", "premiumlevel", "vip", "vipflag", "vipuser", "isvip", "ispro", "proversion", "subscription", "subscribed", "subscribepro", "subscriberpro", "purchase", "purchased", "purchasehistory", "purchasetoken", "billing", "payment", "entitlement", "license", "unlock", "unlocked", "removeads", "adfree", "noads")
    private fun isObfuscated(owner: String, name: String): Boolean {
        val simpleOwner = owner.substringAfterLast('/').removeSuffix(";")
        return name.length <= 3 && name.matches(Regex("[A-Za-z0-9_\$]+")) && simpleOwner.length <= 5
    }
    private fun isCheckName(name: String): Boolean = name.startsWith("is", true) || name.startsWith("has", true) || name.startsWith("can", true) || name.startsWith("check", true) || name.startsWith("verify", true) || name.startsWith("valid", true) || name.startsWith("owns", true)
    private fun formatSmali(owner: String, method: Method): String {
        val access = org.jf.dexlib2.AccessFlags.getAccessFlagsForMethod(method.accessFlags).joinToString(" ") { flag -> flag.name.lowercase() }
        val header = ".method ${if (access.isBlank()) "public" else access} ${method.name}${descriptor(method)}"
        val impl = method.implementation ?: return ".class $owner\n$header\n    # no implementation (abstract/native)\n.end method"
        val lines = StringBuilder(".class $owner\n$header\n    .registers ${impl.registerCount}\n")
        var address = 0
        impl.instructions.forEach { instruction ->
            lines.append("    :L").append(address.toString(16).padStart(4, '0')).append('\n')
            lines.append("        ").append(formatInstruction(instruction)).append('\n')
            address += instruction.codeUnits
        }
        lines.append(".end method")
        return lines.toString()
    }
    private fun formatInstruction(i: org.jf.dexlib2.iface.instruction.Instruction): String {
        val args = mutableListOf<String>()
        when (i) {
            is org.jf.dexlib2.iface.instruction.formats.Instruction45cc -> { args += registers(i); args += ref(i.reference); args += ref(i.reference2) }
            is org.jf.dexlib2.iface.instruction.formats.Instruction35c -> { args += registers(i); args += ref(i.reference) }
            is org.jf.dexlib2.iface.instruction.formats.Instruction3rc -> { args += "{v${i.startRegister} .. v${i.startRegister + i.registerCount - 1}}"; args += ref(i.reference) }
            is org.jf.dexlib2.iface.instruction.formats.Instruction4rcc -> { args += "{v${i.startRegister} .. v${i.startRegister + i.registerCount - 1}}"; args += ref(i.reference); args += ref(i.reference2) }
            is org.jf.dexlib2.iface.instruction.OneRegisterInstruction -> args += "v${i.registerA}"
            is org.jf.dexlib2.iface.instruction.TwoRegisterInstruction -> { args += "v${i.registerA}"; args += "v${i.registerB}" }
            is org.jf.dexlib2.iface.instruction.ThreeRegisterInstruction -> { args += "v${i.registerA}"; args += "v${i.registerB}"; args += "v${i.registerC}" }
        }
        if (i is org.jf.dexlib2.iface.instruction.ReferenceInstruction) args += ref(i.reference)
        if (i is org.jf.dexlib2.iface.instruction.NarrowLiteralInstruction) args += i.narrowLiteral.toString()
        if (i is org.jf.dexlib2.iface.instruction.WideLiteralInstruction) args += i.wideLiteral.toString()
        if (i is org.jf.dexlib2.iface.instruction.OffsetInstruction) args += ":L${(i.codeOffset).toString(16)}"
        return i.opcode.name.lowercase() + if (args.isEmpty()) "" else " " + args.joinToString(", ")
    }
    private fun registers(i: org.jf.dexlib2.iface.instruction.formats.Instruction35c): List<String> = listOf(i.registerC, i.registerD, i.registerE, i.registerF, i.registerG).take(i.registerCount).map { "v$it" }
    private fun registers(i: org.jf.dexlib2.iface.instruction.formats.Instruction45cc): List<String> = listOf(i.registerC, i.registerD, i.registerE, i.registerF, i.registerG).take(i.registerCount).map { "v$it" }
    private fun ref(reference: Any): String = when (reference) {
        is org.jf.dexlib2.iface.reference.Reference -> org.jf.dexlib2.formatter.DexFormatter.INSTANCE.getReference(reference)
        else -> reference.toString()
    }
}
