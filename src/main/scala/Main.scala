@main def run(): Unit = 
  val imagePath = "src/main/resources/images.png"
  val image = ImageLoader.loadImage(imagePath)
  println("ImageLoader done!")