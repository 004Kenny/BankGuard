package com.example.bankguard.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Corner radii from the Stitch round-2 DESIGN.md.
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),  // chips, badges, system tags
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),     // buttons, inputs, inset panels
    large = RoundedCornerShape(16.dp),      // cards
    extraLarge = RoundedCornerShape(24.dp), // dialogs
)

object BankGuardShapes {
    val chip = RoundedCornerShape(8.dp)
    val button = RoundedCornerShape(12.dp)
    val inset = RoundedCornerShape(12.dp)
    val card = RoundedCornerShape(16.dp)
    val iconBox = RoundedCornerShape(12.dp)
    val sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val pill = RoundedCornerShape(percent = 50)
}
