package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.GlucoseEmerald
import com.example.ui.theme.LiquidCyan
import com.example.ui.theme.LiquidCyanLight
import com.example.ui.theme.LiquidMagenta
import com.example.ui.theme.LiquidMagentaLight
import com.example.ui.theme.LiquidTeal
import com.example.ui.theme.LiquidViolet
import com.example.ui.theme.ProteinPurple
import com.example.ui.theme.SodiumAmber
import com.example.ui.theme.WaterBlue

enum class TonicityState(val displayName: String, val description: String, val cellVolumeRatio: Float) {
    HYPOTONIC("Hypotonic (Swollen)", "Extracellular solute is low. Water rushes in via osmosis; cell expands towards lysis.", 1.35f),
    ISOTONIC("Isotonic (Homeostasis)", "Equilibrium. Dynamic water flux in and out is perfectly balanced.", 1.0f),
    HYPERTONIC("Hypertonic (Crenated)", "Extracellular solute is concentrated. Water leaves; cell crenates and shrinks.", 0.72f)
}

enum class SoluteType(
    val symbol: String,
    val fullName: String,
    val color: Color,
    val mass: Float,
    val defaultPermeability: Float
) {
    WATER("H₂O", "Water Molecule", WaterBlue, 18f, 0.95f),
    SODIUM("Na⁺", "Sodium Ion", SodiumAmber, 23f, 0.20f),
    GLUCOSE("C₆H₁₂O₆", "Glucose", GlucoseEmerald, 180f, 0.10f),
    PROTEIN("Albumin", "Plasma Protein", ProteinPurple, 66500f, 0.0f)
}

data class FluidParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val type: SoluteType,
    val radius: Float = 5f,
    var isIntracellular: Boolean = false
)

data class LiquidBlob(
    val id: Int,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var radius: Float,
    val color: Color,
    val accentColor: Color,
    val label: String
)

data class CellularOrganelle(
    val id: String,
    val name: String,
    val scientificName: String,
    val role: String,
    val energyProduct: String,
    val description: String,
    val visualHue: Color,
    val keyFeatures: List<String>
)

val SampleOrganelles = listOf(
    CellularOrganelle(
        id = "mitochondria",
        name = "Mitochondria",
        scientificName = "Chondriosome / Powerhouse",
        role = "ATP Bioenergetics & Oxidative Phosphorylation",
        energyProduct = "30–32 ATP / Glucose",
        description = "Folded inner membrane (cristae) creates an electrochemical proton gradient driving the ATP Synthase rotary nanomotor.",
        visualHue = LiquidCyan,
        keyFeatures = listOf("Double lipid bilayer", "Proton Motive Force (Δp)", "Matrix Krebs Cycle", "Mitochondrial DNA (mtDNA)")
    ),
    CellularOrganelle(
        id = "nucleus",
        name = "Nucleus",
        scientificName = "Cellular Command Center",
        role = "Genomic Storage & RNA Transcription",
        energyProduct = "mRNA & Ribosome Subunits",
        description = "Encased in a perforated nuclear envelope with liquid nucleoplasm housing chromatin and the nucleolus.",
        visualHue = LiquidViolet,
        keyFeatures = listOf("Nuclear Pore Complex", "Dense Nucleolus", "Heterochromatin / Euchromatin", "Lamina structural mesh")
    ),
    CellularOrganelle(
        id = "ribosome_er",
        name = "Endoplasmic Reticulum & Ribosomes",
        scientificName = "Rough & Smooth ER",
        role = "Protein Synthesis & Lipid Synthesis",
        energyProduct = "Secretory Proteins & Lipids",
        description = "Continuous membrane labyrinth where ribosomes translate genetic codons into folded 3D peptide chains.",
        visualHue = LiquidMagenta,
        keyFeatures = listOf("Signal Recognition Particle (SRP)", "Chaperone protein folding", "Calcium reservoir", "Phospholipid synthesis")
    ),
    CellularOrganelle(
        id = "lysosome",
        name = "Lysosome & Peroxisome",
        scientificName = "Acid Hydrolase Compartment",
        role = "Autophagy, Waste Catabolism & Detox",
        energyProduct = "Recycled Amino Acids",
        description = "Acidic lumen (pH ~4.5-5.0) maintaining proton pumps to digest cellular debris, worn-out organelles, and pathogens.",
        visualHue = LiquidTeal,
        keyFeatures = listOf("V-type H⁺ ATPase pump", "Acid Hydrolases", "Autophagosome fusion", "Membrane protection against self-digestion")
    )
)

data class CreatorPlatformLink(
    val id: String,
    val title: String,
    val handleOrDescription: String,
    val url: String,
    val category: String,
    val accentColor: Color
)

val YeltiPlatformLinks = listOf(
    CreatorPlatformLink(
        id = "tilda",
        title = "Official Portal",
        handleOrDescription = "gdyelti.tilda.ws — Ivan Yelti's Central Hub",
        url = "https://gdyelti.tilda.ws/",
        category = "Main Site",
        accentColor = LiquidCyan
    ),
    CreatorPlatformLink(
        id = "youtube",
        title = "YouTube Channel",
        handleOrDescription = "Ivan Yelti / GD Yelti — Gameplay, showcases & level architecture",
        url = "https://www.youtube.com",
        category = "Streaming & Video",
        accentColor = LiquidMagenta
    ),
    CreatorPlatformLink(
        id = "discord",
        title = "Discord Community",
        handleOrDescription = "Yelti Discord Server — Community & Level Collaboration",
        url = "https://discord.com",
        category = "Community",
        accentColor = LiquidViolet
    ),
    CreatorPlatformLink(
        id = "telegram",
        title = "Telegram Channel",
        handleOrDescription = "Updates, release news & announcements",
        url = "https://telegram.org",
        category = "Broadcasts",
        accentColor = LiquidTeal
    ),
    CreatorPlatformLink(
        id = "twitter",
        title = "X (Twitter)",
        handleOrDescription = "@gdyelti — Creative updates & GD dev logs",
        url = "https://x.com",
        category = "Social",
        accentColor = LiquidCyanLight
    ),
    CreatorPlatformLink(
        id = "tiktok",
        title = "TikTok",
        handleOrDescription = "@gdyelti — Short-form highlights & visual clips",
        url = "https://tiktok.com",
        category = "Media",
        accentColor = LiquidMagentaLight
    )
)
