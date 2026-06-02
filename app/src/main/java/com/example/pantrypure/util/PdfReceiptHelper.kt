package com.example.pantrypure.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import androidx.core.graphics.createBitmap

class PdfReceiptHelper(private val context: Context) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Processes a PDF file from a Uri, renders its pages to Bitmaps,
     * and performs OCR on each page.
     */
    fun processPdfInvoice(
        uri: Uri,
        onPageProcessed: (Text) -> Unit,
        onError: (Exception) -> Unit,
        onFinished: () -> Unit
    ) {
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                val renderer = PdfRenderer(fd)
                val pageCount = renderer.pageCount

                for (i in 0 until pageCount) {
                    val page = renderer.openPage(i)
                    
                    // Render with higher density for better OCR results
                    val bitmap = createBitmap(page.width * 2, page.height * 2)
                    
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    
                    val image = InputImage.fromBitmap(bitmap, 0)
                    
                    recognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            onPageProcessed(visionText)
                            if (i == pageCount - 1) onFinished()
                        }
                        .addOnFailureListener { e ->
                            onError(e)
                            if (i == pageCount - 1) onFinished()
                        }
                    
                    page.close()
                }
                renderer.close()
            }
        } catch (e: Exception) {
            onError(e)
            onFinished()
        }
    }
}
