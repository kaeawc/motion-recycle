package co.hinge.motionrecycle

import android.content.Context
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.app.Activity
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.view.View
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import arrow.core.Try
import arrow.core.orNull

object Blur {

    fun blurScreen(activity: Activity, view: View): BitmapDrawable? {

        val displayMetrics = activity.resources?.displayMetrics
        val displayWidth = displayMetrics?.widthPixels ?: 0
        val displayHeight = Math.max((displayMetrics?.heightPixels ?: 0) - 64, 0)
        val viewWidth = view.width
        val viewHeight = view.height
        val width = if (viewWidth == 0) displayWidth else viewWidth
        val height = if (viewHeight == 0) displayHeight else viewHeight
        if (width == 0) return null
        if (height == 0) return null

        return try {
            val bitmap = drawViewToBitmap(width, height, view, Color.parseColor("#ffffffff"))
            val blurredBitmap = fastBlur(activity.baseContext, bitmap, 25f)
            bitmap.recycle()
            blurredBitmap.toDrawable(activity.resources)
        } catch (ex: OutOfMemoryError) {
            Runtime.getRuntime().gc()
            Timber.e("Could not blur, out of memory")
            null
        } catch (ex: IllegalArgumentException) {
            Timber.e(ex, "Could not blur, invalid args for creating bitmap")
            null
        }
    }

    //http://stackoverflow.com/a/9596132/1121509
    private fun drawViewToBitmap(width: Int, height: Int, view: View, color: Int): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        canvas.drawColor(color)
        view.draw(canvas)

        /**
         * Whatever the screen size is, we want either something scaled
         * down by 2x or a 2MB limit on the resulting resized bitmap.
         */
//        val maxByteCount = Math.min((returnedBitmap.byteCount / 4f).toInt(), 2_000_000)
//        val scale = getSmallerScale(maxByteCount, width, height, 1f)
        return returnedBitmap // resizeBitmap(returnedBitmap, scale)
    }

    private fun getSmallerScale(maxByteCount: Int, width: Int, height: Int, current: Float): Float {
        val scale = current * 0.5f
        return if ((width * scale) * (height * scale) * 4 < maxByteCount) {
            getBiggerScale(maxByteCount, width, height, scale)
        } else {
            getSmallerScale(maxByteCount, width, height, scale)
        }
    }

    private fun getBiggerScale(maxByteCount: Int, width: Int, height: Int, current: Float): Float {
        val scale = current * 1.1f
        return if ((width * scale) * (height * scale) * 4 < maxByteCount) {
            getBiggerScale(maxByteCount, width, height, scale)
        } else {
            current
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, scale: Float): Bitmap {


        val targetWidth = (bitmap.width * scale).toInt()
        val targetHeight = (bitmap.height * scale).toInt()
        val scaleX = targetWidth.toFloat() / bitmap.width
        val scaleY = targetHeight.toFloat() / bitmap.height

        val result = Try {
            val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val m = Matrix().apply {
                setScale(scaleX, scaleY)
            }
            canvas.drawBitmap(bitmap, m, Paint())
            output
        }.orNull()


        // If we have a valid result, recycle the original bitmap. Otherwise return the original
        return result?.also { bitmap.recycle() } ?: bitmap
    }

    private fun fastBlur(context: Context, sentBitmap: Bitmap, radius: Float): Bitmap {

        val bitmap = sentBitmap.copy(sentBitmap.config, true)
        val rs = RenderScript.create(context)
        val input = Allocation.createFromBitmap(rs, sentBitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT)
        val output = Allocation.createTyped(rs, input.type)
        val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        when (radius) {
            in 0f..25f -> script.setRadius(radius)
            else -> script.setRadius(25f)
        }
        script.setInput(input)
        script.forEach(output)
        output.copyTo(bitmap)

        val paint = Paint()
        val filter = LightingColorFilter(-0x333333, 0x111111)
        paint.colorFilter = filter
        val canvas = Canvas(bitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return bitmap
    }
}
