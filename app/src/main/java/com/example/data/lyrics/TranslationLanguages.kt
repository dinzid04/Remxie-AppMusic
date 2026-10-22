package com.example.data.lyrics

data class TranslationLanguage(
    val code: String,
    val name: String,
)

val TRANSLATION_LANGUAGES: List<TranslationLanguage> = listOf(
    TranslationLanguage("id", "Indonesian (Bahasa Indonesia)"),
    TranslationLanguage("en", "English"),
    TranslationLanguage("ja", "Japanese (日本語)"),
    TranslationLanguage("ko", "Korean (한국어)"),
    TranslationLanguage("zh-CN", "Chinese Simplified (简体中文)"),
    TranslationLanguage("zh-TW", "Chinese Traditional (繁體中文)"),
    TranslationLanguage("es", "Spanish (Español)"),
    TranslationLanguage("fr", "French (Français)"),
    TranslationLanguage("de", "German (Deutsch)"),
    TranslationLanguage("pt", "Portuguese (Português)"),
    TranslationLanguage("it", "Italian (Italiano)"),
    TranslationLanguage("ru", "Russian (Русский)"),
    TranslationLanguage("ar", "Arabic (العربية)"),
    TranslationLanguage("hi", "Hindi (हिन्दी)"),
    TranslationLanguage("th", "Thai (ไทย)"),
    TranslationLanguage("vi", "Vietnamese (Tiếng Việt)"),
    TranslationLanguage("ms", "Malay (Bahasa Melayu)"),
    TranslationLanguage("tl", "Filipino / Tagalog"),
    TranslationLanguage("tr", "Turkish (Türkçe)"),
    TranslationLanguage("nl", "Dutch (Nederlands)"),
    TranslationLanguage("pl", "Polish (Polski)"),
    TranslationLanguage("sv", "Swedish (Svenska)"),
    TranslationLanguage("uk", "Ukrainian (Українська)"),
    TranslationLanguage("el", "Greek (Ελληνικά)"),
    TranslationLanguage("he", "Hebrew (עברית)"),
    TranslationLanguage("bn", "Bengali (বাংলা)"),
    TranslationLanguage("ta", "Tamil (தமிழ்)"),
    TranslationLanguage("te", "Telugu (తెలుగు)"),
    TranslationLanguage("ur", "Urdu (اردو)"),
    TranslationLanguage("fa", "Persian (فارسی)"),
)

fun translationLanguageName(code: String): String {
    return TRANSLATION_LANGUAGES.firstOrNull { it.code.equals(code, ignoreCase = true) }?.name
        ?: code.uppercase()
}
