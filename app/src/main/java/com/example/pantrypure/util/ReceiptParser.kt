package com.example.pantrypure.util

import com.google.mlkit.vision.text.Text

class ReceiptParser {
    /**
     * Parses the recognized text from a receipt and extracts potential ingredient names.
     */
    fun parseReceipt(visionText: Text): List<String> {
        val lines = visionText.textBlocks.flatMap { it.lines }
        val results = mutableListOf<String>()

        for (line in lines) {
            val text = line.text.trim()
            
            // Skip common receipt keywords like "SUMME", "TOTAL", etc.
            if (isCommonReceiptKeyword(text)) continue
            
            // Clean up the text: Remove prices at the end and quantities at the start
            val cleaned = cleanIngredientName(text)
            
            if (cleaned.length > 2) {
                results.add(cleaned)
            }
        }
        
        return results.distinct()
    }

    private fun isCommonReceiptKeyword(text: String): Boolean {
        val keywords = listOf(
            "SUMME", "TOTAL", "MWST", "STEUER", "DATUM", "UHRZEIT", "BON", "BELEG",
            "KARTE", "BAR", "RUECKGELD", "GEGEBEN", "FILIALE", "TEL:", "DANKE",
            "NETTO", "BRUTTO", "PFAND", "ZWISCHENSUMME", "HELFEN SIE", "VIELEN DANK"
        )
        return keywords.any { text.contains(it, ignoreCase = true) }
    }

    private fun cleanIngredientName(text: String): String {
        // 1. Remove prices at the end (e.g., "1,99", "2.50 A", "3.00€")
        // Matches digits, followed by comma/dot, two digits, optional space, and optional letter or Euro symbol at the end
        var result = text.replace("""\d+[,.]\d{2}(\s*[A-Z€])?$""".toRegex(), "").trim()
        
        // 2. Remove leading quantities (e.g., "1 STK", "0,500 kg", "2 x")
        result = result.replace("""^(\d+([,.]\d+)?\s*(x|stk|kg|g|l|ml)?\s+)""".toRegex(RegexOption.IGNORE_CASE), "")
            .trim()

        // 3. Remove common prefixes that don't belong to the name
        result = result.replace("""^[*#-]\s*""".toRegex(), "")
        
        // 4. Split by multiple spaces (often used to separate name from other columns)
        return result.split("  ")[0].trim()
    }
}
