import java.awt.image.BufferedImage

object ImagePartitioner {
    case class Region(x: Int, y: Int, width: Int, height: Int)

    val MIN_REGION_SIZE = 500
    val MAX_DEPTH = 5

    def partitionImage(image: BufferedImage, edgeDensityMap: Array[Array[Double]], threshold: Double): List[Region] = {
        val initialRegion = Region(0, 0, image.getWidth, image.getHeight)
        partitionRegion(initialRegion, edgeDensityMap, threshold, depth = 0)
    }

    private def partitionRegion(region: Region, edgeDensityMap: Array[Array[Double]], threshold: Double, depth: Int): List[Region] = {
        if (depth >= MAX_DEPTH || region.width <= MIN_REGION_SIZE || region.height <= MIN_REGION_SIZE) {
            List(region)
        } else {
            val avgDensity = computeRegionEdgeDensity(region, edgeDensityMap)
            if (avgDensity < threshold) {
                List(region)
            } else {
                val halfWidth = region.width / 2
                val halfHeight = region.height / 2

                val subregions = List(
                    Region(region.x, region.y, halfWidth, halfHeight),
                    Region(region.x + halfWidth, region.y, halfWidth, halfHeight),
                    Region(region.x, region.y + halfHeight, halfWidth, halfHeight),
                    Region(region.x + halfWidth, region.y + halfHeight, halfWidth, halfHeight)
                )
                subregions.flatMap(r => partitionRegion(r, edgeDensityMap, threshold, depth + 1))
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
}