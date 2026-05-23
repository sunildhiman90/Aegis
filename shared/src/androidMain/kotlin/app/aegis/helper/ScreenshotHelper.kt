package app.aegis.helper

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.TakeScreenshotCallback
import android.graphics.Bitmap
import android.os.Build
import android.view.Display
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ScreenshotHelper(
    private val service: AccessibilityService,
    private val executor: Executor // Pass Executors.newSingleThreadExecutor() from service
) {

    /**
     * Captures the screen and returns a Bitmap using Coroutines.
     * Returns null if the capture fails or API is too old (< Android 11).
     */
    suspend fun captureScreen(): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return null // Not supported on older Android versions
        }

        return suspendCancellableCoroutine { continuation ->
            val callback = object : TakeScreenshotCallback {
                override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                    try {
                        val bitmap = Bitmap.wrapHardwareBuffer(
                            result.hardwareBuffer,
                            result.colorSpace
                        )
                        // Copy the bitmap because hardware buffers can be closed
                        val copy = bitmap?.copy(Bitmap.Config.ARGB_8888, false)
                        result.hardwareBuffer.close()
                        
                        if (copy != null) {
                            continuation.resume(copy)
                        } else {
                            continuation.resume(null)
                        }
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }

                override fun onFailure(errorCode: Int) {
                    // Log the error code if needed
                    continuation.resume(null)
                }
            }

            service.takeScreenshot(Display.DEFAULT_DISPLAY, executor, callback)
        }
    }
}