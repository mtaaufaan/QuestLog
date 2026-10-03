package com.rds.questlog.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font as GoogleFontEntry
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.rds.questlog.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

/** Google Font + fallback font perangkat (dipakai bila provider/sertifikat belum siap). */
private fun family(name: String, fallback: String, vararg variants: Pair<FontWeight, FontStyle>): FontFamily =
    FontFamily(
        variants.map { (w, s) -> GoogleFontEntry(GoogleFont(name), provider, w, s) } +
            variants.map { (w, s) -> Font(DeviceFontFamilyName(fallback), w, s) },
    )

private val Normal = FontStyle.Normal

val CormorantGaramond = family(
    "Cormorant Garamond", "serif",
    FontWeight.Medium to Normal, FontWeight.SemiBold to Normal,
)
val Lora = family(
    "Lora", "serif",
    FontWeight.Normal to Normal, FontWeight.SemiBold to Normal, FontWeight.Normal to FontStyle.Italic,
)
val JetBrainsMono = family("JetBrains Mono", "monospace", FontWeight.Normal to Normal)

/** Gaya untuk URL / label halaman / ukuran font (design.md §3.1). */
val QlMonoStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp)

val QlTypography = Typography(
    headlineLarge = TextStyle(fontFamily = CormorantGaramond, fontWeight = FontWeight.SemiBold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = CormorantGaramond, fontWeight = FontWeight.SemiBold, fontSize = 26.sp),
    headlineSmall = TextStyle(fontFamily = CormorantGaramond, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = CormorantGaramond, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = Lora, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Lora, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Lora, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = Lora, fontWeight = FontWeight.Normal, fontSize = 11.sp),
)
