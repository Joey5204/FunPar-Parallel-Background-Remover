@main def run(): Unit = {
  val imagePath = "src/main/resources/images.png"
  val image = ImageLoader.loadImage(imagePath)
  println("ImageLoader done!")

  val bgColor = BackgroundRemover.detectBackgroundColor(image)
  println(s"Detected background color: $bgColor")

  val edgeDensityMap = EdgeDensity.calculateEdgeDensity(image)
  println("Edge density map calculated.")

  val threshold = 0.1
  val regions = ImagePartitioner.partitionImage(image, edgeDensityMap, threshold)
  println(s"Image partitioned into ${regions.length} regions.")

  for ((region, index) <- regions.zipWithIndex) {
    val subImage = image.getSubimage(region.x, region.y, region.width, region.height)
    
    // Create a binary mask for the region
    val mask = BackgroundRemover.createMask(subImage, bgColor, threshold = 50)
    val processedRegion = BackgroundRemover.applyMask(subImage, mask)

    val outputPath = s"src/main/resources/output/region_$index.png"
    javax.imageio.ImageIO.write(processedRegion, "png", new java.io.File(outputPath))
    println(s"Saved region $index at: $outputPath")
  }
  
  println("Partitioning and background removal complete!")
}
