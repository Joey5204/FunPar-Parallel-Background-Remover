import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.opencv_core._
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

  val converterToMat = new OpenCVFrameConverter.ToMat()
  val converterToBufferedImage = new Java2DFrameConverter()
  val frame = converterToBufferedImage.convert(image)
  var mat = converterToMat.convert(frame)
  val resizedMat = new Mat()
  resize(mat, resizedMat, new Size(800, 1067), 0, 0, INTER_AREA)
  mat = resizedMat
  val resizedImage = converterToBufferedImage.convert(converterToMat.convert(mat))

  val subjectMask = GraphSegmentation.getSubjectMask(resizedImage)
  println("Subject mask created.")

  val edgeDensityMap = EdgeDensity.calculateEdgeDensity(resizedImage)
  println("Edge density map calculated.")

  val threshold = 0.05
  val regions = ImagePartitioner.partitionImage(resizedImage, edgeDensityMap, threshold)
  println(s"Image partitioned into ${regions.length} regions.")

  // process regions in parallel and collect results
  val futureResults = regions.zipWithIndex.map { case (region, index) =>
    Future {
      if (region.width >= 200 && region.height >= 200) {
        println(s"Processing region $index: (${region.x}, ${region.y}, ${region.width}, ${region.height})")
        val subImage = resizedImage.getSubimage(region.x, region.y, region.width, region.height)
        println(s"Subimage created for region $index")

        // create new converters for each thread to avoid thread-safety issues
        val threadConverterToBufferedImage = new Java2DFrameConverter()
        val threadConverterToMat = new OpenCVFrameConverter.ToMat()

        val subFrame = threadConverterToBufferedImage.convert(subImage)
        val subMat = threadConverterToMat.convert(subFrame)

        // pass the subject mask and region to applyGrabCut
        val processedRegion = GraphSegmentation.applyGrabCut(subMat, subjectMask, new Rect(region.x, region.y, region.width, region.height))
        println(s"GrabCut applied to region $index")

        // debug
        val outputPath = s"src/main/resources/output/region_$index.png"
        imwrite(outputPath, processedRegion)
        println(s"Saved processed region $index at: $outputPath")

        // clean subMat, but keep processedRegion for merging
        subMat.release()

        // return the processed Mat and its region for combining
        Some((processedRegion, region))
      } else {
        println(s"Skipping region $index: (${region.x}, ${region.y}, ${region.width}, ${region.height}) - too small")
        None
      }
    }
  }

  // wait for futures to complete and collect results
  val results = Await.result(Future.sequence(futureResults), 10.minutes).flatten

  // create a blank Mat for the final image (with alpha channel)
  val finalImage = new Mat(resizedMat.rows(), resizedMat.cols(), CV_8UC4, new Scalar(0, 0, 0, 0))

  // merge the processed regions into the final image
  results.foreach { case (processedMat, region) =>
    val roi = new Mat(finalImage, new Rect(region.x, region.y, region.width, region.height))
    processedMat.copyTo(roi)
    roi.release()
    processedMat.release()
  }

  // Save the final combined image
  val finalOutputPath = "src/main/resources/output/final_output.png"
  imwrite(finalOutputPath, finalImage)
  println(s"Saved final combined image at: $finalOutputPath")

  // Clean up
  finalImage.release()
  mat.release()
  resizedMat.release()
  subjectMask.release()

  println("Partitioning and background removal using GrabCut complete!")
}