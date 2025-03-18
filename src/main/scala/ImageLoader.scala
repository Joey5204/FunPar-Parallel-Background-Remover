import java.io.File
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

object ImageLoader {
  def loadImage(filePath: String): BufferedImage = {
    val img: BufferedImage = ImageIO.read(new File(filePath))
    println(s"Image loaded successfully! Dimensions: ${img.getWidth} x ${img.getHeight}")
    img
  }
}
