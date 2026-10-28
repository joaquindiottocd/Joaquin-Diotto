package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.Client
import com.example.data.model.Formatters
import com.example.data.model.Sale
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    fun generateAndShareProductPdf(context: Context, client: Client, sale: Sale) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 at 72dpi
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        drawProductFicha(canvas, client, sale, pageInfo.pageWidth)

        pdfDocument.finishPage(page)

        val fileName = "Ficha_${client.fullName.replace(" ", "_")}_${sale.product.replace(" ", "_")}.pdf"
        val file = File(context.cacheDir, fileName)

        try {
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            sharePdfFileViaWhatsApp(context, file, "Ficha de pago: ${sale.product} - ${client.fullName}")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al generar PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun generateAndShareAllProductsPdf(context: Context, client: Client, sales: List<Sale>) {
        if (sales.isEmpty()) {
            Toast.makeText(context, "El cliente no posee productos activos.", Toast.LENGTH_SHORT).show()
            return
        }

        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        sales.forEachIndexed { index, sale ->
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            drawProductFicha(canvas, client, sale, pageWidth)

            pdfDocument.finishPage(page)
        }

        val fileName = "Ficha_Completa_${client.fullName.replace(" ", "_")}.pdf"
        val file = File(context.cacheDir, fileName)

        try {
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            sharePdfFileViaWhatsApp(context, file, "Fichas de pago completas de ${client.fullName}")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al generar PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun sharePdfFileViaWhatsApp(context: Context, file: File, title: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "$title - Capital Diario")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val whatsappIntent = Intent(intent).apply {
                setPackage("com.whatsapp")
            }

            if (whatsappIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(whatsappIntent)
            } else {
                context.startActivity(Intent.createChooser(intent, "Enviar $title por..."))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al compartir archivo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun drawProductFicha(canvas: Canvas, client: Client, sale: Sale, pageWidth: Int) {
        val navyColor = Color.rgb(12, 45, 87) // #0C2D57
        val lightGray = Color.rgb(240, 244, 248)
        val textDark = Color.rgb(30, 30, 30)
        val textMuted = Color.rgb(100, 110, 120)
        val greenColor = Color.rgb(46, 125, 50)
        val orangeColor = Color.rgb(230, 81, 0)

        val paint = Paint().apply { isAntiAlias = true }

        var y = 30f

        // 1. Navy Header Banner
        paint.color = navyColor
        canvas.drawRoundRect(20f, y, pageWidth - 20f, y + 40f, 8f, 8f, paint)

        paint.color = Color.WHITE
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CAPITAL DIARIO", 35f, y + 25f, paint)

        paint.textSize = 12f
        val fichaTitle = "FICHA DE PAGO"
        val fichaWidth = paint.measureText(fichaTitle)
        canvas.drawText(fichaTitle, pageWidth - 35f - fichaWidth, y + 25f, paint)

        y += 55f

        // 2. Client Info Card
        paint.color = lightGray
        canvas.drawRoundRect(20f, y, pageWidth - 20f, y + 80f, 8f, 8f, paint)

        paint.color = textDark
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(client.fullName.uppercase(), 35f, y + 24f, paint)

        paint.color = textMuted
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("DNI: ${client.dni}    Tel: ${client.phone}", 35f, y + 42f, paint)
        canvas.drawText("Domicilio: ${client.address}", 35f, y + 58f, paint)
        client.collectorName?.let {
            canvas.drawText("Cobrador: $it", 35f, y + 72f, paint)
        }

        y += 95f

        // 3. Product Info & Credit Header
        paint.color = navyColor
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(sale.product.uppercase(), 20f, y, paint)

        paint.color = textMuted
        paint.textSize = 11f
        val creditText = "Crédito #${sale.creditNumber}"
        val creditWidth = paint.measureText(creditText)
        canvas.drawText(creditText, pageWidth - 20f - creditWidth, y, paint)

        y += 18f
        canvas.drawText("Total financiado: ${Formatters.formatCurrency(sale.totalAmount)}", 20f, y, paint)

        y += 15f

        // 4. Metric Cards
        val totalCuotas = sale.installments.size
        val totales = sale.installments.count { it.isPaid }
        val parciales = sale.installments.count { !it.isPaid && (it.amount - it.balance) > 0 }
        val pendientes = sale.installments.count { !it.isPaid && (it.amount - it.balance) <= 0 }

        val metricWidth = (pageWidth - 40f - 18f) / 4f
        val metricHeight = 35f

        // Box 1: CUOTAS
        var xBox = 20f
        paint.color = lightGray
        canvas.drawRoundRect(xBox, y, xBox + metricWidth, y + metricHeight, 6f, 6f, paint)
        paint.color = navyColor
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("$totalCuotas", xBox + metricWidth / 2 - paint.measureText("$totalCuotas") / 2, y + 16f, paint)
        paint.textSize = 8f
        paint.color = textMuted
        canvas.drawText("CUOTAS", xBox + metricWidth / 2 - paint.measureText("CUOTAS") / 2, y + 28f, paint)

        // Box 2: TOTALES
        xBox += metricWidth + 6f
        paint.color = Color.rgb(232, 245, 233)
        canvas.drawRoundRect(xBox, y, xBox + metricWidth, y + metricHeight, 6f, 6f, paint)
        paint.color = greenColor
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("$totales", xBox + metricWidth / 2 - paint.measureText("$totales") / 2, y + 16f, paint)
        paint.textSize = 8f
        canvas.drawText("TOTALES", xBox + metricWidth / 2 - paint.measureText("TOTALES") / 2, y + 28f, paint)

        // Box 3: PARCIALES
        xBox += metricWidth + 6f
        paint.color = Color.rgb(255, 243, 224)
        canvas.drawRoundRect(xBox, y, xBox + metricWidth, y + metricHeight, 6f, 6f, paint)
        paint.color = orangeColor
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("$parciales", xBox + metricWidth / 2 - paint.measureText("$parciales") / 2, y + 16f, paint)
        paint.textSize = 8f
        canvas.drawText("PARCIALES", xBox + metricWidth / 2 - paint.measureText("PARCIALES") / 2, y + 28f, paint)

        // Box 4: PENDIENTES
        xBox += metricWidth + 6f
        paint.color = lightGray
        canvas.drawRoundRect(xBox, y, xBox + metricWidth, y + metricHeight, 6f, 6f, paint)
        paint.color = navyColor
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("$pendientes", xBox + metricWidth / 2 - paint.measureText("$pendientes") / 2, y + 16f, paint)
        paint.textSize = 8f
        paint.color = textMuted
        canvas.drawText("PENDIENTES", xBox + metricWidth / 2 - paint.measureText("PENDIENTES") / 2, y + 28f, paint)

        y += metricHeight + 15f

        // 5. Installments Table / Grid (2 columns)
        val colWidth = (pageWidth - 40f - 10f) / 2f
        val cardHeight = 48f

        sale.installments.chunked(2).forEach { chunk ->
            if (y + cardHeight > 780f) {
                return@forEach
            }

            for (i in chunk.indices) {
                val inst = chunk[i]
                val xCard = if (i == 0) 20f else 20f + colWidth + 10f
                val paidAmount = (inst.amount - inst.balance).coerceAtLeast(0.0)

                paint.color = Color.WHITE
                canvas.drawRoundRect(xCard, y, xCard + colWidth, y + cardHeight, 6f, 6f, paint)

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                paint.color = Color.rgb(220, 225, 230)
                canvas.drawRoundRect(xCard, y, xCard + colWidth, y + cardHeight, 6f, 6f, paint)
                paint.style = Paint.Style.FILL

                // Title: Cuota X
                paint.color = textDark
                paint.textSize = 10f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("Cuota ${inst.number}", xCard + 8f, y + 14f, paint)

                // Status Badge
                val statusStr = when {
                    inst.isPaid -> "PAGADA"
                    paidAmount > 0 -> "PARCIAL"
                    else -> "PENDIENTE"
                }
                val badgeColor = when {
                    inst.isPaid -> greenColor
                    paidAmount > 0 -> orangeColor
                    else -> navyColor
                }
                paint.color = badgeColor
                paint.textSize = 8f
                val badgeW = paint.measureText(statusStr)
                canvas.drawText(statusStr, xCard + colWidth - 8f - badgeW, y + 14f, paint)

                // Due date and amount
                paint.color = textMuted
                paint.textSize = 9f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("Vence ${Formatters.formatDate(inst.dueDate)}", xCard + 8f, y + 28f, paint)

                paint.color = textDark
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val amtStr = Formatters.formatCurrency(inst.amount)
                val amtW = paint.measureText(amtStr)
                canvas.drawText(amtStr, xCard + colWidth - 8f - amtW, y + 28f, paint)

                // Subline balance
                paint.textSize = 8f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                if (inst.isPaid) {
                    paint.color = greenColor
                    canvas.drawText("Abonó ${Formatters.formatCurrency(paidAmount)}", xCard + 8f, y + 41f, paint)
                } else if (paidAmount > 0) {
                    paint.color = orangeColor
                    canvas.drawText("Abonó ${Formatters.formatCurrency(paidAmount)}", xCard + 8f, y + 41f, paint)
                    val balStr = "Saldo ${Formatters.formatCurrency(inst.balance)}"
                    canvas.drawText(balStr, xCard + colWidth - 8f - paint.measureText(balStr), y + 41f, paint)
                } else {
                    paint.color = textMuted
                    canvas.drawText("Sin abonos", xCard + 8f, y + 41f, paint)
                }
            }

            y += cardHeight + 8f
        }

        // Footer note
        val pendingTotal = sale.installments.filter { !it.isPaid }.sumOf { it.balance }
        paint.color = lightGray
        canvas.drawRoundRect(20f, 790f, pageWidth - 20f, 820f, 6f, 6f, paint)

        paint.color = textDark
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Saldo pendiente del producto: ${Formatters.formatCurrency(pendingTotal)}", 30f, 808f, paint)
    }
}
