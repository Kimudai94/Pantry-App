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
            
            // Basic filtering logic:
            // 1. Skip lines that look like prices (e.g., "1,99", "2.50 EUR")
            if (isPrice(text)) continue
            
            // 2. Skip common receipt keywords
            if (isCommonReceiptKeyword(text)) continue
            
            // 3. Clean up the text (remove quantities at start, etc.)
            val cleaned = cleanIngredientName(text)
            
            if (cleaned.length > 2) {
                results.add(cleaned)
            }
        }
        
        return results.distinct()
    }

    private fun isPrice(text: String): Boolean {
        val priceRegex = """.*\d+[,.]\d{2}.*""".toRegex()
        return priceRegex.matches(text) || text.contains("EUR", ignoreCase = true) || text.contains("€")
    }

    private fun isCommonReceiptKeyword(text: String): Boolean {
        val keywords = listOf(
            "SUMME", "TOTAL", "MWST", "STEUER", "DATUM", "UHRZEIT", "BON", "BELEG",
            "KARTE", "BAR", "RUECKGELD", "GEGEBEN", "FILIALE", "TEL:", "DANKE",
            "NETTO", "BRUTTO", "PFAND", "ZWISCHENSUMME"
        )
        return keywords.any { text.contains(it, ignoreCase = true) }
    }

    private fun cleanIngredientName(text: String): String {
        // Remove leading numbers and units (e.g., "1 STK APFEL" -> "APFEL")
        // This is a very basic implementation and could be much more sophisticated
        return text.replace("""^\d+\s*(STK|G|KG|L|ML|X)?\s+""".toRegex(RegexOption.IGNORE_CASE), "")
            .trim()
            .split("  ")[0] // Often receipts have multiple spaces before the price
    }
}
