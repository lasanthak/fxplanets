package org.lasantha.fxplanets.controller

import org.lasantha.fxplanets.model.Entity
import org.lasantha.fxplanets.model.Path
import org.lasantha.fxplanets.view.ImageWrapper

data class Shape(val entity: Entity, val path: Path, val image: ImageWrapper)