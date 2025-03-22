import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.opencv_core._
import org.bytedeco.opencv.global.opencv_imgcodecs.imwrite
import org.bytedeco.opencv.global.opencv_core._
import javax.imageio.ImageIO
import java.io.File
import org.bytedeco.javacv.{OpenCVFrameConverter, Java2DFrameConverter}
import java.awt.image.BufferedImage
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._
import scala.concurrent.Await

@main def run(): Unit = {
  implicit val ec: ExecutionContext = ExecutionContext.global

  val imagePath = "src/main/resources/image.jpg"
  val image = ImageLoader.loadImage(imagePath)
  println("ImageLoader done!")

  // Resize the image before processing
  val converterToMat = new OpenCVFrameConverter.ToMat()
  val converterToBufferedImage = new Java2DFrameConverter()
  val frame = converterToBufferedImage.convert(image)
  var mat = converterToMat.convert(frame)
  val resizedMat = new Mat()
  resize(mat, resizedMat, new Size(800, 1067), 0, 0, INTER_AREA)
  mat = resizedMat
  val resizedImage = converterToBufferedImage.convert(converterToMat.convert(mat))

  val edgeDensityMap = EdgeDensity.calculateEdgeDensity(resizedImage)
  println("Edge density map calculated.")

  val threshold = 0.1 // Reverted to 0.1
  val regions = ImagePartitioner.partitionImage(resizedImage, edgeDensityMap, threshold)
  println(s"Image partitioned into ${regions.length} regions.")

  val futures = regions.zipWithIndex.map { case (region, index) =>
    Future {
      if (region.width >= 200 && region.height >= 200) {
        println(s"Processing region $index: (${region.x}, ${region.y}, ${region.width}, ${region.height})")
        val subImage = resizedImage.getSubimage(region.x, region.y, region.width, region.height)
        println(s"Subimage created for region $index")

        val subFrame = converterToBufferedImage.convert(subImage)
        val subMat = converterToMat.convert(subFrame)

        val processedRegion = GraphSegmentation.applyGrabCut(subMat)
        println(s"GrabCut applied to region $index")

        val outputPath = s"src/main/resources/output/region_$index.png"
        imwrite(outputPath, processedRegion)
        println(s"Saved processed region $index at: $outputPath")
      } else {
        println(s"Skipping region $index: (${region.x}, ${region.y}, ${region.width}, ${region.height}) - too small")
      }
    }
  }

  Await.result(Future.sequence(futures), 10.minutes)

  println("Partitioning and background removal using GrabCut complete!")
}