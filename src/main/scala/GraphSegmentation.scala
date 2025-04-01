import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.opencv_core._
import org.bytedeco.javacv.{OpenCVFrameConverter, Java2DFrameConverter}
import java.awt.image.BufferedImage

object GraphSegmentation {
  def getSubjectMask(image: BufferedImage): Mat = {
    val converter = new Java2DFrameConverter()
    val frame = converter.convert(image)
    val converterToMat = new OpenCVFrameConverter.ToMat()
    val mat = converterToMat.convert(frame)

    // convert to BGR if needed (BufferedImage is typically RGB)
    val bgrMat = new Mat()
    if (mat.channels() == 1) {
      cvtColor(mat, bgrMat, COLOR_GRAY2BGR)
    } else {
      cvtColor(mat, bgrMat, COLOR_RGB2BGR)
    }

    val resizedMat = new Mat()
    resize(bgrMat, resizedMat, new Size(800, 1067), 0, 0, INTER_AREA)

    val mask = new Mat(resizedMat.rows(), resizedMat.cols(), CV_8UC1, new Scalar(GC_BGD))
    val bgdModel = new Mat()
    val fgdModel = new Mat()

    val rectWidth = (resizedMat.cols() * 0.6).toInt // 60% of the width
    val rectHeight = (resizedMat.rows() * 0.7).toInt // 70% of the height
    val rectX = ((resizedMat.cols() - rectWidth) / 2).toInt
    val rectY = ((resizedMat.rows() - rectHeight) / 2).toInt
    val rect = new Rect(rectX, rectY, rectWidth, rectHeight)

    // mark the center as probable foreground
    val centerWidth = (resizedMat.cols() * 0.5).toInt
    val centerHeight = (resizedMat.rows() * 0.7).toInt
    val centerX = ((resizedMat.cols() - centerWidth) / 2).toInt
    val centerY = ((resizedMat.rows() - centerHeight) / 2).toInt
    val centerRect = new Rect(centerX, centerY, centerWidth, centerHeight)
    val centerMask = new Mat(mask, centerRect)
    centerMask.put(new Scalar(GC_PR_FGD))
    centerMask.release()

    grabCut(resizedMat, mask, rect, bgdModel, fgdModel, 5, GC_INIT_WITH_RECT)

    // create foreground mask
    val foregroundMask = new Mat(mask.size(), mask.`type`(), new Scalar(0))
    
    val gcFgdMat = new Mat(mask.size(), mask.`type`(), new Scalar(GC_FGD))
    val gcPrFgdMat = new Mat(mask.size(), mask.`type`(), new Scalar(GC_PR_FGD))
    
    // get definite foreground
    val fgdPixels = new Mat()
    compare(mask, gcFgdMat, fgdPixels, CMP_EQ)
    
    // get probable foreground
    val prFgdPixels = new Mat()
    compare(mask, gcPrFgdMat, prFgdPixels, CMP_EQ)
    
    // combine
    bitwise_or(fgdPixels, prFgdPixels, foregroundMask)

    // debugging
    val debugMaskPath = "src/main/resources/output/subject_mask.png"
    imwrite(debugMaskPath, foregroundMask)
    println(s"Saved subject mask at: $debugMaskPath")

    // cleanup
    Seq(mat, bgrMat, resizedMat, mask, bgdModel, fgdModel, 
        fgdPixels, prFgdPixels, gcFgdMat, gcPrFgdMat).foreach(_.release())

    foregroundMask // return the foreground mask for applyGrabCut
  }

  def applyGrabCut(mat: Mat, subjectMask: Mat, region: Rect): Mat = {
    val bgrMat = if (mat.channels() == 1) {
      val temp = new Mat()
      cvtColor(mat, temp, COLOR_GRAY2BGR)
      temp
    } else if (mat.channels() == 4) {
      val temp = new Mat()
      cvtColor(mat, temp, COLOR_BGRA2BGR)
      temp
    } else {
      mat
    }

    // extract corresponding region from subject mask
    val subMask = new Mat(subjectMask, region)

    // create mask for GrabCut
    val grabCutMask = new Mat(bgrMat.rows(), bgrMat.cols(), CV_8UC1, new Scalar(GC_BGD))

    // copy subject mask into grabCutMask
    val nonZeroMask = new Mat()
    compare(subMask, new Mat(subMask.size(), subMask.`type`(), new Scalar(0)), nonZeroMask, CMP_GT)
    val prFgdMask = new Mat(grabCutMask.size(), grabCutMask.`type`(), new Scalar(GC_PR_FGD))
    prFgdMask.copyTo(grabCutMask, nonZeroMask)

    val bgdModel = new Mat()
    val fgdModel = new Mat()

    // GrabCut with the initialized mask
    grabCut(bgrMat, grabCutMask, new Rect(), bgdModel, fgdModel, 5, GC_INIT_WITH_MASK)

    // create foreground mask
    val foregroundMask = new Mat()
    
    // create mats for comparison
    val gcFgdMat = new Mat(grabCutMask.size(), grabCutMask.`type`(), new Scalar(GC_FGD))
    val gcPrFgdMat = new Mat(grabCutMask.size(), grabCutMask.`type`(), new Scalar(GC_PR_FGD))
    
    // get definite foreground
    val fgdPixels = new Mat()
    compare(grabCutMask, gcFgdMat, fgdPixels, CMP_EQ)
    
    // get probable foreground
    val prFgdPixels = new Mat()
    compare(grabCutMask, gcPrFgdMat, prFgdPixels, CMP_EQ)
    
    // combine both
    bitwise_or(fgdPixels, prFgdPixels, foregroundMask)

    // create output with transparency
    val result = new Mat(bgrMat.size(), CV_8UC4, new Scalar(0, 0, 0, 0))
    
    // convert BGR to BGRA
    val bgraMat = new Mat()
    cvtColor(bgrMat, bgraMat, COLOR_BGR2BGRA)
    
    // copy foreground pixels
    bgraMat.copyTo(result, foregroundMask)

    // cleanup
    Seq(subMask, nonZeroMask, grabCutMask, bgdModel, fgdModel, 
        foregroundMask, fgdPixels, prFgdPixels, bgraMat,
        gcFgdMat, gcPrFgdMat).foreach(_.release())
    if (bgrMat != mat) bgrMat.release()

    result
  }
}