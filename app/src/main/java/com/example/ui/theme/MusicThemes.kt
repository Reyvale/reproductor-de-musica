package com.example.ui.theme

import androidx.compose.ui.graphics.Color

data class CustomTheme(
    val id: String,
    val name: String,
    val description: String,
    val background: Color,
    val surface: Color,
    val primary: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val gradientColors: List<Color>,
    val glowColor: Color,
    val sliderStyle: String, // "Classic Knob", "Neon Tube", "Carbon Fiber"
    val visualizerStyle: String // "Bar Spectrum", "Analog VU", "Concentric Pulse"
)

object MusicThemes {
    val themes = listOf(
        CustomTheme(
            id = "matrix_green",
            name = "Negro & Verde Neón",
            description = "Fusión premium de negro puro OLED y verde neón hacker de alta fidelidad con destellos radiactivos.",
            background = Color(0xFF000000),
            surface = Color(0xFF0D0F0D),
            primary = Color(0xFF39FF14),
            accent = Color(0xFF00FF66),
            textPrimary = Color(0xFFE5FFE5),
            textSecondary = Color(0xFF7D9E7D),
            gradientColors = listOf(Color(0xFF0C100C), Color(0xFF000000)),
            glowColor = Color(0x5539FF14),
            sliderStyle = "Neon Tube",
            visualizerStyle = "Bar Spectrum"
        ),
        CustomTheme(
            id = "graphite",
            name = "Classic Graphite",
            description = "Aesthetic core, deep charcoal and brushed steel with electric teal glows.",
            background = Color(0xFF121315),
            surface = Color(0xFF1D1F22),
            primary = Color(0xFF14DFD0),
            accent = Color(0xFF00FFCC),
            textPrimary = Color(0xFFECEFF1),
            textSecondary = Color(0xFF90A4AE),
            gradientColors = listOf(Color(0xFF181B1F), Color(0xFF101114)),
            glowColor = Color(0x3314DFD0),
            sliderStyle = "Classic Knob",
            visualizerStyle = "Bar Spectrum"
        ),
        CustomTheme(
            id = "cyberpunk",
            name = "Cyberpunk Decay",
            description = "High-contrast dark synth wave with radioactive magenta and acid green highlights.",
            background = Color(0xFF07000B),
            surface = Color(0xFF1E002B),
            primary = Color(0xFFFF007F),
            accent = Color(0xFF39FF14),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFB080D0),
            gradientColors = listOf(Color(0xFF0B0011), Color(0xFF020005)),
            glowColor = Color(0x55FF007F),
            sliderStyle = "Neon Tube",
            visualizerStyle = "Bar Spectrum"
        ),
        CustomTheme(
            id = "gold",
            name = "High-Res Gold Master",
            description = "Luxurious audiophile brushed gold, carbon fiber details, and high-fidelity gold text.",
            background = Color(0xFF0E0D0B),
            surface = Color(0xFF1C1B17),
            primary = Color(0xFFD4AF37),
            accent = Color(0xFFFFD700),
            textPrimary = Color(0xFFFFFDF5),
            textSecondary = Color(0xFFB5A990),
            gradientColors = listOf(Color(0xFF141311), Color(0xFF080706)),
            glowColor = Color(0x22FFD700),
            sliderStyle = "Carbon Fiber",
            visualizerStyle = "Analog VU"
        ),
        CustomTheme(
            id = "emerald",
            name = "Emerald Acid Glow",
            description = "Deep matrix military forest green with glowing radioactive lime accents.",
            background = Color(0xFF070C09),
            surface = Color(0xFF101914),
            primary = Color(0xFF00FF66),
            accent = Color(0xFF50FA7B),
            textPrimary = Color(0xFFE2F4E9),
            textSecondary = Color(0xFF8DA396),
            gradientColors = listOf(Color(0xFF09120E), Color(0xFF030504)),
            glowColor = Color(0x4400FF66),
            sliderStyle = "Classic Knob",
            visualizerStyle = "Bar Spectrum"
        ),
        CustomTheme(
            id = "amber",
            name = "Acoustic Amber Cabin",
            description = "Roasted coffee, logs of ancient oak, warm golden copper accents, and analog warmth.",
            background = Color(0xFF141010),
            surface = Color(0xFF261D1C),
            primary = Color(0xFFFF8200),
            accent = Color(0xFFFFC04A),
            textPrimary = Color(0xFFFBF1EE),
            textSecondary = Color(0xFFCEAFA6),
            gradientColors = listOf(Color(0xFF1E1615), Color(0xFF0E0A0A)),
            glowColor = Color(0x33FF8200),
            sliderStyle = "Classic Knob",
            visualizerStyle = "Analog VU"
        ),
        CustomTheme(
            id = "glacier",
            name = "Frozen Glacier",
            description = "Antarctic slate mist, icy cyan icebergs, and clean high-contrast polar white lines.",
            background = Color(0xFF10151E),
            surface = Color(0xFF1E2838),
            primary = Color(0xFF4AC2FF),
            accent = Color(0xBB4AC2FF),
            textPrimary = Color(0xFFF0F5FA),
            textSecondary = Color(0xFFA0B2C6),
            gradientColors = listOf(Color(0xFF161F2C), Color(0xFF0B0E14)),
            glowColor = Color(0x224AC2FF),
            sliderStyle = "Carbon Fiber",
            visualizerStyle = "Concentric Pulse"
        )
    )

    fun getThemeById(id: String): CustomTheme {
        return themes.find { it.id == id } ?: themes[0]
    }
}
