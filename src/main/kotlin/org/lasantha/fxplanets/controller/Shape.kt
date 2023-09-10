package org.lasantha.fxplanets.controller

import org.lasantha.fxplanets.model.ControlPath
import org.lasantha.fxplanets.model.Entity
import org.lasantha.fxplanets.model.Path
import org.lasantha.fxplanets.view.ImageWrapper

open class Shape(val entity: Entity, val path: Path, val image: ImageWrapper)

class ControlShape(entity: Entity, val controlPath: ControlPath, image: ImageWrapper) : Shape(entity, controlPath, image)
