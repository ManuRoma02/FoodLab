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

// Neo-Brutalism Crisp Corner Radii
val NeoBrutalCornerRadiusSmall = 6.dp
val NeoBrutalCornerRadiusMedium = 8.dp
val NeoBrutalCornerRadiusCard = 12.dp
val NeoBrutalCornerRadiusModal = 14.dp
val NeoBrutalCornerRadiusPill = 10.dp

val NeoBrutalCardShape = RoundedCornerShape(NeoBrutalCornerRadiusCard)
val NeoBrutalModalShape = RoundedCornerShape(NeoBrutalCornerRadiusModal)
val NeoBrutalPillShape = RoundedCornerShape(NeoBrutalCornerRadiusPill)

val NeoBrutalShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(NeoBrutalCornerRadiusSmall),
    medium = RoundedCornerShape(NeoBrutalCornerRadiusMedium),
    large = RoundedCornerShape(NeoBrutalCornerRadiusCard),
    extraLarge = RoundedCornerShape(NeoBrutalCornerRadiusModal)
)

// Bento Craft Corner Radii (Warm, Organic & Chunky Bento Grid)
val BentoCraftCornerRadiusSmall = 10.dp
val BentoCraftCornerRadiusMedium = 16.dp
val BentoCraftCornerRadiusCard = 22.dp
val BentoCraftCornerRadiusModal = 26.dp
val BentoCraftCornerRadiusPill = 99.dp

val BentoCraftCardShape = RoundedCornerShape(BentoCraftCornerRadiusCard)
val BentoCraftModalShape = RoundedCornerShape(BentoCraftCornerRadiusModal)
val BentoCraftPillShape = RoundedCornerShape(BentoCraftCornerRadiusPill)

val BentoCraftShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(BentoCraftCornerRadiusSmall),
    medium = RoundedCornerShape(BentoCraftCornerRadiusMedium),
    large = RoundedCornerShape(BentoCraftCornerRadiusCard),
    extraLarge = RoundedCornerShape(BentoCraftCornerRadiusModal)
)

// Nothing OS Shapes (Industrial, flat surfaces, 12-16px cards & 999px pills)
val NothingCornerRadiusSmall = 6.dp
val NothingCornerRadiusMedium = 10.dp
val NothingCornerRadiusCard = 14.dp
val NothingCornerRadiusModal = 16.dp
val NothingCornerRadiusPill = 999.dp

val NothingCardShape = RoundedCornerShape(NothingCornerRadiusCard)
val NothingModalShape = RoundedCornerShape(NothingCornerRadiusModal)
val NothingPillShape = RoundedCornerShape(NothingCornerRadiusPill)

val NothingShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(NothingCornerRadiusSmall),
    medium = RoundedCornerShape(NothingCornerRadiusMedium),
    large = RoundedCornerShape(NothingCornerRadiusCard),
    extraLarge = RoundedCornerShape(NothingCornerRadiusModal)
)
