import java.awt.image.BufferedImage

object EdgeDensity {
    def calculateEdgeDensity(image: BufferedImage): Array[Array[Double]] = {
        val width = image.getWidth
        val height = image.getHeight
        val edgeDensityMap = Array.ofDim[Double](width, height)

        // edge detection
        val sobelKernelX = Array(Array(-1, 0, 1), Array(-2, 0, 2), Array(-1, 0, 1))
        val sobelKernelY = Array(Array(-1, -2, -1), Array(0, 0, 0), Array(1, 2, 1))

        for (x <- 1 until width - 1) {
            for (y <- 1 until height - 1) {
                val gx = applyKernel(image, sobelKernelX, x, y)
                val gy = applyKernel(image, sobelKernelY, x, y)
                val gradientMagnitude = math.sqrt(gx * gx + gy * gy)

                edgeDensityMap(x)(y) = gradientMagnitude / 255.0
            }
        }
        edgeDensityMap
    }

    private def applyKernel(image: BufferedImage, kernel: Array[Array[Int]], x: Int, y: Int): Int = {
        var sum = 0
        for (i <- -1 to 1; j <- -1 to 1) {
            val pixel = new java.awt.Color(image.getRGB(x + i, y + j))
            val intensity = (pixel.getRed + pixel.getGreen + pixel.getBlue) / 3
            sum += intensity * kernel(i + 1)(j + 1)
        }
        sum
    }
}
