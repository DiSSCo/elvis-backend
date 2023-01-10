package org.synthesis.infrastructure

import com.lowagie.text.DocumentException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xhtmlrenderer.pdf.ITextRenderer
import java.io.OutputStream

/**
 * Render html as pdf to specified output stream
 *
 * @throws [DocumentException]
 */
fun ITextRenderer.asHtmlToPdfWriter(): suspend (html: String, to: OutputStream) -> Unit =
    { html, to ->
        setDocumentFromString(html)
        layout()

        withContext(Dispatchers.IO) {
            createPDF(to)
        }
    }
