package com.touchflux.app.fx

import android.graphics.Canvas
import kotlin.random.Random

/**
 * Owns the live effects. Surface-agnostic: the in-app playground, the live wallpaper and the
 * system-wide overlay all drive the very same engine.
 */
class EffectEngine(private val density: Float) {
    private val effects = ArrayList<Effect>(64)
    private val rng = Random(System.nanoTime())

    @Volatile
    var config: FxConfig = FxConfig.DEFAULT

    val idle: Boolean get() = effects.isEmpty()

    fun tap(x: Float, y: Float) = spawn(x, y, small = false)

    fun trail(x: Float, y: Float) = spawn(x, y, small = true)

    fun clear() = effects.clear()

    fun update(dt: Float) {
        var i = effects.size - 1
        while (i >= 0) {
            val e = effects[i]
            e.update(dt)
            if (e.dead) effects.removeAt(i)
            i--
        }
    }

    fun draw(c: Canvas) {
        for (i in effects.indices) effects[i].draw(c)
    }

    private fun spawn(x: Float, y: Float, small: Boolean) {
        val cfg = config
        val type = if (cfg.effect == EffectType.RANDOM) PICKS[rng.nextInt(PICKS.size)] else cfg.effect
        val unit = density * cfg.size * (if (small) 0.55f else 1f)
        val life = baseDuration(type) * cfg.duration * (if (small) 0.6f else 1f)
        val spec = Spec(x, y, unit, life, pickColor(cfg.palette), cfg.intensity, small, rng)

        val fx: Effect = when (type) {
            EffectType.ORB -> OrbEffect(spec)
            EffectType.LIGHTNING -> LightningEffect(spec)
            EffectType.NOVA -> NovaEffect(spec)
            EffectType.GEO -> GeoEffect(spec)
            else -> RippleEffect(spec)
        }
        if (effects.size >= MAX_EFFECTS) effects.removeAt(0)
        effects.add(fx)
    }

    private fun baseDuration(t: EffectType): Float = when (t) {
        EffectType.RIPPLE -> 1.7f
        EffectType.ORB -> 1.35f
        EffectType.LIGHTNING -> 0.60f
        EffectType.NOVA -> 1.25f
        EffectType.GEO -> 1.8f
        EffectType.RANDOM -> 1.2f
    }

    private fun pickColor(p: Palette): Int {
        if (p.rainbow) {
            val cycle = (System.nanoTime() / 1_000_000L) % 6000L
            return Colors.fromHue(cycle / 6000f * 360f + rng.nextFloat() * 30f)
        }
        return p.colors[rng.nextInt(p.colors.size)]
    }

    private companion object {
        const val MAX_EFFECTS = 90
        val PICKS = arrayOf(EffectType.NOVA, EffectType.GEO, EffectType.RIPPLE, EffectType.ORB, EffectType.LIGHTNING)
    }
}
