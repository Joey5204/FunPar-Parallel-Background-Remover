import java.awt.image.BufferedImage

object ImagePartitioner {
    case class Region(x: Int, y: Int, width: Int, height: Int)

    val MIN_REGION_SIZE = 50 // min width/height of a region

    def partitionImage(image: BufferedImage, edgeDensityMap: Array[Array[Double]], threshold: Double): List[Region] = {
        val initialRegion = Region(0, 0, image.getWidth, image.getHeight)
        partitionRegion(initialRegion, edgeDensityMap, threshold)
    }

    private def partitionRegion(region: Region, edgeDensityMap: Array[Array[Double]], threshold: Double): List[Region] = {
        val avgDensity = computeRegionEdgeDensity(region, edgeDensityMap)

        if (avgDensity < threshold || region.width <= MIN_REGION_SIZE || region.height <= MIN_REGION_SIZE) {
            List(region)
        } 
        else {
        // split into 4 
        val halfWidth = region.width / 2
        val halfHeight = region.height / 2

        val subregions = List(
            Region(region.x, region.y, halfWidth, halfHeight), // topleft
            Region(region.x + halfWidth, region.y, halfWidth, halfHeight), // topright
            Region(region.x, region.y + halfHeight, halfWidth, halfHeight), // bottomleft
            Region(region.x + halfWidth, region.y + halfHeight, halfWidth, halfHeight) // bottomright
        )
        // recursively partition each subregion
        subregions.flatMap(partitionRegion(_, edgeDensityMap, threshold))
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
}
