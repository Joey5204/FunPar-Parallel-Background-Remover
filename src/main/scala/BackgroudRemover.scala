import java.awt.image.BufferedImage
import java.awt.Color
import scala.collection.parallel.CollectionConverters._

object BackgroundRemover {
    def detectBackgroundColor(image: BufferedImage): Color = {
        val corners = List(
            new Color(image.getRGB(0, 0)), // topleft
            new Color(image.getRGB(image.getWidth - 1, 0)), // topright
            new Color(image.getRGB(0, image.getHeight - 1)), // bottomleft
            new Color(image.getRGB(image.getWidth - 1, image.getHeight - 1)) // bottomright
        )

        // average RGB of the corners
        val avgR = corners.map(_.getRed).sum / corners.length
        val avgG = corners.map(_.getGreen).sum / corners.length
        val avgB = corners.map(_.getBlue).sum / corners.length

        new Color(avgR, avgG, avgB)
    }

    def createMask(image: BufferedImage, bgColor: Color, threshold: Int): Array[Array[Int]] = {
        val width = image.getWidth
        val height = image.getHeight
        val mask = Array.ofDim[Int](width, height)

        // parallel processing of pixels
        (0).until(width).par.foreach { x =>
            (0 until height).foreach { y =>
                val pixel = new Color(image.getRGB(x, y))
                if (colorDistance(pixel, bgColor) > threshold) {
                    mask(x)(y) = 1 // foreground
                } 
                else {
                    mask(x)(y) = 0 // background
                }
            }
        }
        mask
    }

    private def colorDistance(c1: Color, c2: Color): Double = {
        val rDiff = c1.getRed - c2.getRed
        val gDiff = c1.getGreen - c2.getGreen
        val bDiff = c1.getBlue - c2.getBlue
        Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff)
    }

    def applyMask(image: BufferedImage, mask: Array[Array[Int]]): BufferedImage = {
        val output = new BufferedImage(image.getWidth, image.getHeight, BufferedImage.TYPE_INT_ARGB)

        for (x <- (0).until(image.getWidth); y <- (0).until(image.getHeight)) {
            if (mask(x)(y) == 1) {
                output.setRGB(x, y, image.getRGB(x, y)) // foreground pixel
            }
            else {
                output.setRGB(x, y, 0) // transparent background
            }
        }
        output
    }
}