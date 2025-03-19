@main def run(): Unit = {
  val imagePath = "src/main/resources/images.png"
  val image = ImageLoader.loadImage(imagePath)
  println("ImageLoader done!")
  
  val edgeDensityMap = EdgeDensity.calculateEdgeDensity(image)
  println("Edge density map calculated.")

  val threshold = 0.1
  val regions = ImagePartitioner.partitionImage(image, edgeDensityMap, threshold)
  println(s"Image partitioned into ${regions.length} regions.")

  for ((region, index) <- regions.zipWithIndex) {
    val subImage = image.getSubimage(region.x, region.y, region.width, region.height)
    val outputPath = s"src/main/resources/output/region_$index.jpg"
    javax.imageio.ImageIO.write(subImage, "jpg", new java.io.File(outputPath))
    println(s"Saved region $index at: $outputPath")
  }
  println("Partitioning complete")
}