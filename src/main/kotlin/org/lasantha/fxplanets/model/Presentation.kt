package org.lasantha.fxplanets.model

enum class PresentationType(val category: EntityCategory) {
    STAR(EntityCategory.PLANETARY),
    PLANET(EntityCategory.PLANETARY),
    MOON(EntityCategory.PLANETARY),
    ASTEROID(EntityCategory.UFO),
    ALIENSHIP(EntityCategory.UFO),
    SPACEBLOB(EntityCategory.UFO),
    EXPLOSION(EntityCategory.MISC),
    FIGHTER(EntityCategory.USER);
}

data class Presentation(val id: String, val type: PresentationType, val width: Int, val height: Int)

