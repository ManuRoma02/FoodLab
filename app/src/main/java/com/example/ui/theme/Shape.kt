package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Apple Standard Corner Radii
val AppleCornerRadiusSmall = 10.dp
val AppleCornerRadiusMedium = 16.dp
val AppleCornerRadiusCard = 24.dp
val AppleCornerRadiusModal = 28.dp
val AppleCornerRadiusPill = 32.dp

// Apple Standard Shapes
val AppleCardShape = RoundedCornerShape(AppleCornerRadiusCard)
val AppleModalShape = RoundedCornerShape(AppleCornerRadiusModal)
val ApplePillShape = RoundedCornerShape(AppleCornerRadiusPill)

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(AppleCornerRadiusSmall),
    medium = RoundedCornerShape(AppleCornerRadiusMedium),
    large = RoundedCornerShape(AppleCornerRadiusCard),
    extraLarge = RoundedCornerShape(AppleCornerRadiusModal)
)
