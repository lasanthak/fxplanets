package org.lasantha.fxplanets.controller

import org.lasantha.fxplanets.model.ControlPath
import org.lasantha.fxplanets.model.Entity
import org.lasantha.fxplanets.model.Path
import org.lasantha.fxplanets.view.ImageWrapper
import org.lasantha.fxplanets.view.LRControlImage

open class Shape(val entity: Entity, val path: Path, val image: ImageWrapper)

class ControlShape(entity: Entity, val controlPath: ControlPath, val controlImage: LRControlImage) : Shape(entity, controlPath, controlImage)
