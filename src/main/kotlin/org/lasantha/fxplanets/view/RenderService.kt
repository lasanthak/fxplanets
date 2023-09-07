package org.lasantha.fxplanets.view

import javafx.scene.canvas.GraphicsContext
import org.lasantha.fxplanets.service.GameService
import org.lasantha.fxplanets.service.PresentationService

class RenderService(private val gc: GraphicsContext, private val gameService: GameService, private val prService: PresentationService) {

    val imageLib = ImageLib(prService)

    fun update(time: Long) {
        for (e in gameService.entities()) {
            val path = gameService.path(e)
            e.update(path.location(time))
        }
    }

    fun clear() {
        for (e in gameService.entities()) {
            gc.clearRect(e.lastX, e.lastY, e.presentation.width.toDouble(), e.presentation.height.toDouble())
        }
    }

    fun draw(time: Long) {
        for (e in gameService.entities()) {
            val frame = imageLib.image(e.presentation).frame(time)
            gc.drawImage(frame, e.x, e.y)
        }
    }
}
