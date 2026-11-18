package com.example.domain

import com.example.data.local.ParsingRuleEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType

data class ParsedTransactionData(
    val type: TransactionType,
    val amount: Double,
    val personName: String,
    val phoneNumber: String,
    val upiId: String,
    val transactionReference: String,
    val paymentApp: String,
    val paymentAppPackage: String,
    val timestamp: Long,
    val rawText: String,
    val needsVerification: Boolean,
    val uncertainReasons: List<String>
)

sealed interface ParseOutcome {
    data class Parsed(val data: ParsedTransactionData) : ParseOutcome
    data class Ignored(val reason: String) : ParseOutcome
}

class SmartMoneyNotificationParser {

    companion object {
        private val SENSITIVE_AUTH_PATTERNS = listOf(
            Regex("""\botp\b""", RegexOption.IGNORE_CASE),
            Regex("""one[\s\-]?time[\s\-]?password""", RegexOption.IGNORE_CASE),
            Regex("""verification\s+code""", RegexOption.IGNORE_CASE),
            Regex("""security\s+code""", RegexOption.IGNORE_CASE),
            Regex("""auth(?:entication)?\s+code""", RegexOption.IGNORE_CASE),
            Regex("""login\s+code""", RegexOption.IGNORE_CASE),
            Regex("""\bmpin\b""", RegexOption.IGNORE_CASE),
            Regex("""\bupi\s*pin\b""", RegexOption.IGNORE_CASE),
            Regex("""\batm\s*pin\b""", RegexOption.IGNORE_CASE),
            Regex("""\btpin\b""", RegexOption.IGNORE_CASE),
            Regex("""\bcvv\b""", RegexOption.IGNORE_CASE),
            Regex("""\bpassword\b""", RegexOption.IGNORE_CASE),
            Regex("""do\s+not\s+share""", RegexOption.IGNORE_CASE),
            Regex("""never\s+share\s+this""", RegexOption.IGNORE_CASE),
            Regex("""\b2fa\b""", RegexOption.IGNORE_CASE)
        )

        private val PROMO_OR_NON_TXN_PATTERNS = listOf(
            Regex("""\brequesting\s+money\b""", RegexOption.IGNORE_CASE),
            Regex("""\bpayment\s+request\b""", RegexOption.IGNORE_CASE),
            Regex("""\bcollect\s+request\b""", RegexOption.IGNORE_CASE),
            Regex("""\breminder\b""", RegexOption.IGNORE_CASE),
            Regex("""\bpre[\-\s]?approved\b""", RegexOption.IGNORE_CASE),
            Regex("""\bapply\s+now\b""", RegexOption.IGNORE_CASE),
            Regex("""\bwin\s+upto\b""", RegexOption.IGNORE_CASE),
            Regex("""\bfailed\b""", RegexOption.IGNORE_CASE),
            Regex("""\bdeclined\b""", RegexOption.IGNORE_CASE)
        )

        private val INVALID_NAME_TOKENS = setOf(
            "your", "you", "account", "acct", "a/c", "bank", "upi", "bank account",
            "your account", "your bank", "ref", "utr", "rs", "inr", "india", "the",
            "payment", "money", "wallet", "merchant", "unknown", "vpa"
        )
    }

    fun isSensitiveAuthNotification(text: String): Boolean {
        return SENSITIVE_AUTH_PATTERNS.any { it.containsMatchIn(text) }
    }

    fun parse(
        packageName: String,
        title: String,
        body: String,
        timestamp: Long,
        rules: List<ParsingRuleEntity>,
        flagUnknownSenderAsPending: Boolean = true,
        trackAllInstalledPaymentApps: Boolean = true,
        resolvedAppLabel: String? = null
    ): ParseOutcome {
        val combinedText = listOf(title.trim(), body.trim())
            .filter { it.isNotBlank() }
            .joinToString(" — ")

        if (combinedText.isBlank()) {
            return ParseOutcome.Ignored("Empty notification text")
        }

        // Security Rule: Never capture OTPs, PINs, passwords, or auth alerts
        if (isSensitiveAuthNotification(combinedText)) {
            return ParseOutcome.Ignored("Filtered sensitive OTP/PIN/auth notification")
        }

        // Ignore failed transactions, collect requests, and promotional offers
        if (PROMO_OR_NON_TXN_PATTERNS.any { it.containsMatchIn(combinedText) }) {
            return ParseOutcome.Ignored("Filtered non-completed or promotional notification")
        }

        val explicitRule = rules.firstOrNull {
            it.packageName.equals(packageName, ignoreCase = true)
        }
        if (explicitRule != null && !explicitRule.isEnabled) {
            return ParseOutcome.Ignored("App tracking disabled for $packageName")
        }
        if (explicitRule == null && !trackAllInstalledPaymentApps) {
            return ParseOutcome.Ignored("Unlisted app ignored because auto-track all apps is disabled")
        }

        val matchedRule = explicitRule
            ?: rules.firstOrNull { it.isEnabled }
            ?: return ParseOutcome.Ignored("No enabled parsing rules")

        val amount = extractAmount(combinedText, matchedRule.amountRegex)
            ?: return ParseOutcome.Ignored("No transaction amount detected")

        if (amount <= 0.0) {
            return ParseOutcome.Ignored("Non-positive transaction amount")
        }

        val sentList = matchedRule.sentKeywords.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
        val receivedList = matchedRule.receivedKeywords.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }

        val lower = combinedText.lowercase()
        val hasSent = sentList.any { lower.contains(it) }
        val hasReceived = receivedList.any { lower.contains(it) }

        if (!hasSent && !hasReceived) {
            return ParseOutcome.Ignored("No transaction direction keyword detected")
        }

        val uncertainReasons = mutableListOf<String>()
        val type = when {
            hasReceived && !hasSent -> TransactionType.INCOME
            hasSent && !hasReceived -> TransactionType.EXPENSE
            else -> {
                // Disambiguate when both appear (e.g., "Paid ... received by ...")
                val firstSentIdx = sentList.mapNotNull { kw -> lower.indexOf(kw).takeIf { it >= 0 } }.minOrNull() ?: Int.MAX_VALUE
                val firstRecIdx = receivedList.mapNotNull { kw -> lower.indexOf(kw).takeIf { it >= 0 } }.minOrNull() ?: Int.MAX_VALUE
                uncertainReasons.add("Ambiguous sent/received wording")
                if (firstRecIdx < firstSentIdx) TransactionType.INCOME else TransactionType.EXPENSE
            }
        }

        // Extract UPI ID / VPA first
        val upiId = extractFirstGroup(combinedText, matchedRule.upiIdRegex)
            ?.trim()
            ?.takeIf { it.contains("@") && !it.endsWith("@") }
            ?: TransactionEntity.UNKNOWN

        // Extract Reference / UTR
        val refNo = extractFirstGroup(combinedText, matchedRule.referenceRegex)
            ?.trim()
            ?.takeIf { it.length >= 6 }
            ?: TransactionEntity.UNKNOWN

        // CRITICAL PRIVACY/ACCURACY RULE:
        // Never assume a UPI ID is a mobile number! Strip UPI IDs and UTRs before scanning for phone numbers.
        var sanitizedForPhone = combinedText
        if (upiId != TransactionEntity.UNKNOWN) {
            sanitizedForPhone = sanitizedForPhone.replace(upiId, " ")
        }
        if (refNo != TransactionEntity.UNKNOWN) {
            sanitizedForPhone = sanitizedForPhone.replace(refNo, " ")
        }
        val phoneNumber = extractExplicitPhone(sanitizedForPhone, matchedRule.phoneRegex)
            ?: TransactionEntity.UNKNOWN

        // Extract Person Name depending on direction (strip UPI ID, phone, and ref first so boundaries are clean)
        var textForName = combinedText
        if (upiId != TransactionEntity.UNKNOWN) {
            textForName = textForName.replace("($upiId)", " , ").replace(upiId, " , ")
        }
        if (phoneNumber != TransactionEntity.UNKNOWN) {
            textForName = textForName.replace(phoneNumber, " , ")
        }
        if (refNo != TransactionEntity.UNKNOWN) {
            textForName = textForName.replace(refNo, " , ")
        }
        textForName = textForName.replace(
            Regex("""\b(?:Mob|Mobile|Phone|Ph|Tel|Contact|UTR|Ref(?:\s*No)?|Txn\s*ID)\s*[:#\-]?""", RegexOption.IGNORE_CASE),
            " , "
        )

        val primaryNameRegex = if (type == TransactionType.INCOME) {
            matchedRule.senderNameRegex
        } else {
            matchedRule.receiverNameRegex
        }
        val extractedName = extractCleanName(textForName, primaryNameRegex)
            ?: extractFallbackTitleName(title, type)
            ?: TransactionEntity.UNKNOWN

        if (extractedName == TransactionEntity.UNKNOWN && flagUnknownSenderAsPending) {
            uncertainReasons.add("Counterparty name unavailable")
        }
        if (refNo == TransactionEntity.UNKNOWN && amount >= 5000.0) {
            uncertainReasons.add("High-value entry without UTR/Reference")
        }

        val appName = when {
            explicitRule != null -> explicitRule.appDisplayName
            !resolvedAppLabel.isNullOrBlank() -> resolvedAppLabel.trim()
            else -> friendlyNameFromPackage(packageName)
        }

        return ParseOutcome.Parsed(
            ParsedTransactionData(
                type = type,
                amount = amount,
                personName = extractedName,
                phoneNumber = phoneNumber,
                upiId = upiId,
                transactionReference = refNo,
                paymentApp = appName,
                paymentAppPackage = packageName,
                timestamp = timestamp,
                rawText = combinedText,
                needsVerification = uncertainReasons.isNotEmpty(),
                uncertainReasons = uncertainReasons
            )
        )
    }

    private fun extractAmount(text: String, regexPattern: String): Double? {
        val regex = runCatching { Regex(regexPattern, RegexOption.IGNORE_CASE) }.getOrNull() ?: return null
        val match = regex.find(text) ?: return null
        val numStr = match.groupValues.getOrNull(1)?.replace(",", "")?.trim() ?: return null
        return numStr.toDoubleOrNull()
    }

    private fun extractFirstGroup(text: String, regexPattern: String): String? {
        if (regexPattern.isBlank()) return null
        val regex = runCatching { Regex(regexPattern, RegexOption.IGNORE_CASE) }.getOrNull() ?: return null
        val match = regex.find(text) ?: return null
        return match.groupValues.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun extractExplicitPhone(sanitizedText: String, regexPattern: String): String? {
        val fromRule = extractFirstGroup(sanitizedText, regexPattern)
        if (fromRule != null) {
            val digits = normalizeIndianPhone(fromRule)
            if (digits != null) return digits
        }
        // Secondary strict check: explicit +91 followed by 10 digits not followed by '@'
        val plus91Regex = Regex("""(?<!\w)(\+91[\s\-]?[6-9][0-9]{9})(?!\w|@)""")
        val plusMatch = plus91Regex.find(sanitizedText)?.groupValues?.getOrNull(1)
        return plusMatch?.let { normalizeIndianPhone(it) }
    }

    private fun normalizeIndianPhone(raw: String): String? {
        val digitsOnly = raw.filter { it.isDigit() }
        return when {
            digitsOnly.length == 10 && digitsOnly.first() in '6'..'9' -> digitsOnly
            digitsOnly.length == 12 && digitsOnly.startsWith("91") && digitsOnly[2] in '6'..'9' ->
                "+$digitsOnly"
            else -> null
        }
    }

    private fun extractCleanName(text: String, regexPattern: String): String? {
        val candidate = extractFirstGroup(text, regexPattern) ?: return null
        val cleaned = candidate
            .replace(Regex("""^(?:Mr\.?|Mrs\.?|Ms\.?|Dr\.?|Shri)\s+""", RegexOption.IGNORE_CASE), "")
            .trim()
            .trimEnd('.', ',', ';', ':')
        if (cleaned.length < 2) return null
        if (cleaned.contains("@")) return null // Don't treat a VPA as a human name
        if (cleaned.all { it.isDigit() || it == '+' || it == '-' || it == ' ' }) return null
        if (cleaned.lowercase() in INVALID_NAME_TOKENS) return null
        return cleaned
    }

    private fun extractFallbackTitleName(title: String, type: TransactionType): String? {
        // E.g. Google Pay notification title is often "Rahul Sharma" with body "Paid you ₹500"
        val t = title.trim()
        if (t.isBlank() || t.length > 35) return null
        if (t.contains("₹") || t.contains("Rs", ignoreCase = true) || t.contains("INR", ignoreCase = true)) return null
        if (t.contains("@") || t.any { it.isDigit() }) return null
        val lower = t.lowercase()
        val systemWords = listOf(
            "google pay", "phonepe", "paytm", "bhim", "amazon", "cred",
            "hdfc", "sbi", "icici", "axis", "kotak", "payment", "transaction", "alert", "upi"
        )
        if (systemWords.any { lower.contains(it) }) return null
        if (lower in INVALID_NAME_TOKENS) return null
        return if (type == TransactionType.INCOME || type == TransactionType.EXPENSE) t else null
    }

    private fun friendlyNameFromPackage(packageName: String): String {
        val lower = packageName.lowercase()
        return when {
            lower.contains("paisa") -> "Google Pay"
            lower.contains("phonepe") -> "PhonePe"
            lower.contains("paytm") -> "Paytm"
            lower.contains("navi") -> "Navi"
            lower.contains("npci") -> "BHIM UPI"
            lower.contains("amazon") -> "Amazon Pay"
            lower.contains("dreamplug") -> "CRED"
            lower.contains("whatsapp") -> "WhatsApp Pay"
            lower.contains("mobikwik") -> "MobiKwik"
            lower.contains("freecharge") -> "Freecharge"
            lower.contains("super.payments") -> "Super.money"
            lower.contains("jupiter") -> "Jupiter"
            lower.contains("epifi") -> "Fi Money"
            lower.contains("indwin") || lower.contains("slice") -> "Slice"
            lower.contains("tatadigital") -> "Tata Neu"
            lower.contains("bharatpe") -> "BharatPe"
            lower.contains("airtel") -> "Airtel Payments"
            lower.contains("jio") -> "JioPay"
            lower.contains("hdfc") -> "HDFC Bank"
            lower.contains("sbi") -> "SBI"
            lower.contains("icici") -> "ICICI Bank"
            lower.contains("axis") -> "Axis Bank"
            lower.contains("kbank") || lower.contains("kotak") -> "Kotak Bank"
            lower.contains("idfc") -> "IDFC FIRST Bank"
            lower.contains("fedmobile") -> "Federal Bank"
            lower.contains("baroda") -> "Bank of Baroda"
            lower.contains("messaging") || lower.contains("mms") -> "Bank SMS"
            packageName.isBlank() -> TransactionEntity.UNKNOWN
            else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }
}
