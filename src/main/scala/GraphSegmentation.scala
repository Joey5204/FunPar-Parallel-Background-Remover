import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.opencv_core._
import org.bytedeco.javacpp.Loader
import org.bytedeco.javacv.{OpenCVFrameConverter, Java2DFrameConverter}

object GraphSegmentation {
  Loader.load(classOf[org.bytedeco.opencv.opencv_core.Mat])

  def applyGrabCut(inputMat: Mat): Mat = {
    val resizedImage = new Mat()
    resize(inputMat, resizedImage, new Size(800, 1067), 0, 0, INTER_AREA)

    val rectWidth = (resizedImage.cols() * 0.8).toInt // 80% of the width
    val rectHeight = (resizedImage.rows() * 0.9).toInt // 90% of the height
    val rectX = (resizedImage.cols() - rectWidth) / 2
    val rectY = (resizedImage.rows() - rectHeight) / 2
    val rect = new Rect(rectX, rectY, rectWidth, rectHeight)

    val mask = new Mat(resizedImage.rows(), resizedImage.cols(), CV_8UC1, new Scalar(GC_BGD))
    val bgdModel = new Mat()
    val fgdModel = new Mat()

    println("Starting GrabCut...")
    grabCut(resizedImage, mask, rect, bgdModel, fgdModel, 10, GC_INIT_WITH_RECT)
    println("GrabCut completed")

    val fgMask = new Mat()
    val tmpMask1 = new Mat()
    val tmpMask2 = new Mat()
    compare(mask, new Mat(new Scalar(GC_FGD)), tmpMask1, CMP_EQ)
    compare(mask, new Mat(new Scalar(GC_PR_FGD)), tmpMask2, CMP_EQ)
    bitwise_or(tmpMask1, tmpMask2, fgMask)

    val kernel = getStructuringElement(MORPH_ELLIPSE, new Size(5, 5))
    dilate(fgMask, fgMask, kernel)
    erode(fgMask, fgMask, kernel)

    val result = new Mat(resizedImage.size(), CV_8UC4, new Scalar(0, 0, 0, 0))
    val bgr = new Mat()
    cvtColor(resizedImage, bgr, COLOR_BGR2BGRA)
    bgr.copyTo(result, fgMask)

    result
  }
}