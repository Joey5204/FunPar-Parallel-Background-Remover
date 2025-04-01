import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.opencv_core._
import java.awt.image.BufferedImage

object ImagePartitioner {
  case class Region(x: Int, y: Int, width: Int, height: Int)

  val MAX_DEPTH = 5
  val MIN_REGION_FRACTION = 0.2

  def partitionImage(image: BufferedImage, edgeDensityMap: Array[Array[Double]], threshold: Double): List[Region] = {
    val smallerDimension = Math.min(image.getWidth, image.getHeight)
    val MIN_REGION_SIZE = (smallerDimension * MIN_REGION_FRACTION).toInt

    // grabCut on the full image to detect subject
    val subjectMask = GraphSegmentation.getSubjectMask(image)

    // partition the image recursively
    val initialRegion = Region(0, 0, image.getWidth, image.getHeight)
    val allRegions = partitionRegion(initialRegion, edgeDensityMap, threshold, depth = 0, MIN_REGION_SIZE)

    // filter out regions that don’t contain the subject
    val subjectRegions = allRegions.filter(region => containsSubject(region, subjectMask))

    println(s"Filtered from ${allRegions.length} to ${subjectRegions.length} subject-containing regions.")

    // cleanup
    subjectMask.release()

    subjectRegions
  }

  private def partitionRegion(region: Region, edgeDensityMap: Array[Array[Double]], threshold: Double, depth: Int, MIN_REGION_SIZE: Int): List[Region] = {
    if (depth >= MAX_DEPTH || region.width <= MIN_REGION_SIZE || region.height <= MIN_REGION_SIZE) {
      List(region)
    } else {
      val avgDensity = computeRegionEdgeDensity(region, edgeDensityMap)
      if (avgDensity < threshold) {
        List(region)
      } else {
        val halfWidth = region.width / 2
        val halfHeight = region.height / 2

        if (halfWidth < MIN_REGION_SIZE || halfHeight < MIN_REGION_SIZE) {
          List(region)
        } else {
          val subregions = List(
            Region(region.x, region.y, halfWidth, halfHeight),
            Region(region.x + halfWidth, region.y, halfWidth, halfHeight),
            Region(region.x, region.y + halfHeight, halfWidth, halfHeight),
            Region(region.x + halfWidth, region.y + halfHeight, halfWidth, halfHeight)
          )
          subregions.flatMap(r => partitionRegion(r, edgeDensityMap, threshold, depth + 1, MIN_REGION_SIZE))
        }
      }
    }
  }

  private def computeRegionEdgeDensity(region: Region, edgeDensityMap: Array[Array[Double]]): Double = {
    var sum = 0.0
    var count = 0

    for (i <- region.x until (region.x + region.width); if i < edgeDensityMap.length) {
      for (j <- region.y until (region.y + region.height); if j < edgeDensityMap(0).length) {
        sum += edgeDensityMap(i)(j)
        count += 1
      }
    }
    if (count == 0) 0.0 else sum / count
  }

  private def containsSubject(region: Region, subjectMask: Mat): Boolean = {
    // extract region from mask
    val subMask = new Mat(subjectMask, new Rect(region.x, region.y, region.width, region.height))
    
    // convert to single channel if needed
    val singleChannelMask = if (subMask.channels() > 1) {
      val temp = new Mat()
      cvtColor(subMask, temp, COLOR_BGR2GRAY)
      temp
    } else {
      subMask
    }
    
    val subjectPixels = countNonZero(singleChannelMask)
    
    // calculate the area of the region
    val regionArea = region.width * region.height
    // require at least 5% of the region to contain subject pixels
    val subjectPixelFraction = subjectPixels.toDouble / regionArea
    val containsSubject = subjectPixelFraction > 0.05

    println(s"Region (${region.x}, ${region.y}, ${region.width}, ${region.height}): $subjectPixels subject pixels, fraction = $subjectPixelFraction, containsSubject = $containsSubject")
    
    // cleanup
    if (singleChannelMask != subMask) singleChannelMask.release()
    subMask.release()
    
    containsSubject
  }
}