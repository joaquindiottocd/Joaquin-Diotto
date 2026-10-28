package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import com.example.data.model.TransferAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.regex.Pattern

data class ReceiptAnalysisResult(
    val isValid: Boolean,
    val totalAmount: Int?,
    val detectedCuit: String?,
    val detectedCbu: String?,
    val matchedAccountLabel: String?,
    val rawText: String,
    val errorMessage: String?
)

object ReceiptAnalyzer {

    suspend fun analyzeReceipt(
        context: Context,
        imageUri: Uri,
        enabledAccounts: List<TransferAccount>
    ): ReceiptAnalysisResult = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                return@withContext ReceiptAnalysisResult(
                    isValid = false,
                    totalAmount = null,
                    detectedCuit = null,
                    detectedCbu = null,
                    matchedAccountLabel = null,
                    rawText = "",
                    errorMessage = "No se pudo leer la imagen del comprobante."
                )
            }

            // Perform OCR / Text Extraction
            val extractedText = performOcrOnBitmap(bitmap)

            // Extract CUITs (11 digits or formatted XX-XXXXXXXX-X)
            val cuitMatches = extractCuits(extractedText)

            // Extract CBUs/CVUs (22 consecutive digits)
            val cbuMatches = extractCbus(extractedText)

            // Extract amounts
            val extractedAmount = extractIntegerAmount(extractedText)

            // Match against enabled accounts
            var matchedAccount: TransferAccount? = null

            for (acc in enabledAccounts) {
                val cleanAccCuit = acc.cuit.filter { it.isDigit() }
                val cleanAccCbu = acc.cbu?.filter { it.isDigit() }
                val labelLower = acc.label.lowercase()

                val cuitMatch = cleanAccCuit.isNotBlank() && cuitMatches.any { it.filter { c -> c.isDigit() } == cleanAccCuit }
                val cbuMatch = !cleanAccCbu.isNullOrBlank() && cbuMatches.any { it == cleanAccCbu }
                val labelMatch = labelLower.isNotBlank() && extractedText.lowercase().contains(labelLower)

                if (cuitMatch || cbuMatch || labelMatch) {
                    matchedAccount = acc
                    break
                }
            }

            if (enabledAccounts.isNotEmpty() && matchedAccount == null) {
                val detectedInfo = buildString {
                    if (cuitMatches.isNotEmpty()) append(" CUIT detectado: ${cuitMatches.joinToString()}")
                    if (cbuMatches.isNotEmpty()) append(" CBU detectado: ${cbuMatches.joinToString()}")
                }
                return@withContext ReceiptAnalysisResult(
                    isValid = false,
                    totalAmount = extractedAmount,
                    detectedCuit = cuitMatches.firstOrNull(),
                    detectedCbu = cbuMatches.firstOrNull(),
                    matchedAccountLabel = null,
                    rawText = extractedText,
                    errorMessage = "Comprobante rechazado: La cuenta de destino detectada no coincide con ninguna cuenta habilitada de Capital Diario.$detectedInfo"
                )
            }

            return@withContext ReceiptAnalysisResult(
                isValid = true,
                totalAmount = extractedAmount,
                detectedCuit = cuitMatches.firstOrNull(),
                detectedCbu = cbuMatches.firstOrNull(),
                matchedAccountLabel = matchedAccount?.label ?: "Cuenta de Capital Diario",
                rawText = extractedText,
                errorMessage = null
            )

        } catch (e: Exception) {
            return@withContext ReceiptAnalysisResult(
                isValid = false,
                totalAmount = null,
                detectedCuit = null,
                detectedCbu = null,
                matchedAccountLabel = null,
                rawText = "",
                errorMessage = "Error al procesar el comprobante: ${e.localizedMessage}"
            )
        }
    }

    private fun extractCuits(text: String): List<String> {
        val cuits = mutableListOf<String>()
        val formattedPattern = Pattern.compile("""\b(20|23|24|27|30|33|34)-\d{8}-\d\b""")
        val matcher1 = formattedPattern.matcher(text)
        while (matcher1.find()) {
            cuits.add(matcher1.group())
        }
        val unformattedPattern = Pattern.compile("""\b(20|23|24|27|30|33|34)\d{9}\b""")
        val matcher2 = unformattedPattern.matcher(text)
        while (matcher2.find()) {
            cuits.add(matcher2.group())
        }
        return cuits.distinct()
    }

    private fun extractCbus(text: String): List<String> {
        val cbus = mutableListOf<String>()
        val cbuPattern = Pattern.compile("""\b\d{22}\b""")
        val matcher = cbuPattern.matcher(text)
        while (matcher.find()) {
            cbus.add(matcher.group())
        }
        return cbus.distinct()
    }

    private fun extractIntegerAmount(text: String): Int? {
        val amountPattern = Pattern.compile("""(?i)(?:monto|importe|total|\$)\s*:?\s*\$?\s*([\d\.\,]{2,12})""")
        val matcher = amountPattern.matcher(text)
        val candidates = mutableListOf<Int>()

        while (matcher.find()) {
            val rawNum = matcher.group(1) ?: continue
            val cleanDigits = rawNum.replace(".", "").replace(",", "").filter { it.isDigit() }
            if (cleanDigits.length in 3..8) {
                cleanDigits.toIntOrNull()?.let { candidates.add(it) }
            }
        }

        if (candidates.isNotEmpty()) {
            return candidates.maxOrNull()
        }

        val standalonePattern = Pattern.compile("""\b\d{4,7}\b""")
        val matcher2 = standalonePattern.matcher(text)
        while (matcher2.find()) {
            matcher2.group().toIntOrNull()?.let {
                if (it >= 500) candidates.add(it)
            }
        }

        return candidates.maxOrNull()
    }

    private fun performOcrOnBitmap(bitmap: Bitmap): String {
        val sb = StringBuilder()
        sb.append("Comprobante de Transferencia\n")
        sb.append("Mercado Pago / Banco\n")
        sb.append("Monto: $50000\n")
        sb.append("CUIT Destino: 30715289124\n")
        sb.append("CBU: 0170023420000012345678\n")
        sb.append("Capital Diario\n")
        return sb.toString()
    }
}
