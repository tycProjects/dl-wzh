package com.example.data.local

import com.example.data.model.DictionaryEntry

/**
 * =========================================================================
 * BISAYA ↔ SUBANEN DICTIONARY - INITIAL SEED DATA (STAGE 1 TESTING ONLY)
 * =========================================================================
 *
 * NOTE FOR THE USER / COMPILER:
 * This file contains a few clearly labeled placeholder entries used solely
 * for initial testing of UI, Room database, search, and favorites.
 *
 * NO INVENTED SUBANEN WORDS ARE PRESENTED AS FINAL.
 * Every entry is explicitly tagged:
 *  - verificationStatus: "Needs verification"
 *  - notes: "[TEST PLACEHOLDER - Awaiting User Verification]"
 *
 * HOW TO ADD OR REPLACE WORDS LATER (WHEN YOU SAY "FINALIZE THE DICTIONARY"):
 * 1. You can add new entries directly to this list:
 *
 *      DictionaryEntry(
 *          bisayaWord = "your Bisaya word",
 *          subanenWord = "your Subanen translation",
 *          bisayaMeaning = "Bisaya explanation / English meaning",
 *          subanenMeaning = "Subanen explanation",
 *          partOfSpeech = "Noun / Verb / Adjective / Greeting",
 *          exampleSentence = "Example sentence in context",
 *          notes = "Dialect / Regional notes / Verification details",
 *          alternativeTranslations = "Synonyms or dialect alternatives",
 *          verificationStatus = "Verified" or "Needs verification"
 *      )
 *
 * 2. You can also add words dynamically at runtime using the "Add Word"
 *    feature inside the app's Settings/Home screen!
 * =========================================================================
 */
object InitialDictionaryData {

    val SAMPLE_TEST_ENTRIES = listOf(
        DictionaryEntry(
            id = 1,
            bisayaWord = "Maayong adlaw",
            subanenWord = "Maulom gondaw",
            bisayaMeaning = "Pangumusta sa maadlaw (Good day / Hello)",
            subanenMeaning = "Pagpangumusta sa maadlaw (Daytime greeting)",
            partOfSpeech = "Phrase / Greeting",
            exampleSentence = "Maayong adlaw kaninyong tanan.",
            notes = "[TEST PLACEHOLDER - Awaiting User Verification] Standard daytime greeting.",
            alternativeTranslations = "Maayong buntag (Morning)",
            verificationStatus = DictionaryEntry.STATUS_NEEDS_VERIFICATION,
            isFavorite = true
        ),
        DictionaryEntry(
            id = 2,
            bisayaWord = "Salamat",
            subanenWord = "Magsukol",
            bisayaMeaning = "Pagpadayag og pasalamat (Thank you)",
            subanenMeaning = "Pagpasalamat (Expression of thanks)",
            partOfSpeech = "Interjection",
            exampleSentence = "Daghang salamat sa imong pag-abag kanamo.",
            notes = "[TEST PLACEHOLDER - Awaiting User Verification] Central/Western Subanen dialect placeholder.",
            alternativeTranslations = "Salamat",
            verificationStatus = DictionaryEntry.STATUS_NEEDS_VERIFICATION,
            isFavorite = true
        ),
        DictionaryEntry(
            id = 3,
            bisayaWord = "Tubig",
            subanenWord = "Tubig",
            bisayaMeaning = "Ilimnon o likido nga gikinahanglan sa kinabuhi (Water)",
            subanenMeaning = "Tubig (Water / River / Stream)",
            partOfSpeech = "Noun",
            exampleSentence = "Palihug ko og bugnaw nga tubig.",
            notes = "[TEST PLACEHOLDER - Awaiting User Verification] Common cognate in Philippine languages.",
            alternativeTranslations = "Tobi",
            verificationStatus = DictionaryEntry.STATUS_NEEDS_VERIFICATION,
            isFavorite = false
        ),
        DictionaryEntry(
            id = 4,
            bisayaWord = "Balay",
            subanenWord = "Balay",
            bisayaMeaning = "Puy-anan sa tawo o pamilya (House / Home)",
            subanenMeaning = "Puy-anan (Dwelling / House)",
            partOfSpeech = "Noun",
            exampleSentence = "Nindot ug lig-on ang ilang bag-ong balay.",
            notes = "[TEST PLACEHOLDER - Awaiting User Verification] Some dialects use Gomà for house/shelter.",
            alternativeTranslations = "Gomà",
            verificationStatus = DictionaryEntry.STATUS_NEEDS_VERIFICATION,
            isFavorite = false
        ),
        DictionaryEntry(
            id = 5,
            bisayaWord = "Kaon",
            subanenWord = "Kuman",
            bisayaMeaning = "Pag-inom o paghungit og pagkaon (To eat)",
            subanenMeaning = "Pagkaon (To consume food)",
            partOfSpeech = "Verb",
            exampleSentence = "Dali, mangaon ta sa dili pa molakaw.",
            notes = "[TEST PLACEHOLDER - Awaiting User Verification] Action verb for eating.",
            alternativeTranslations = "Mokaon",
            verificationStatus = DictionaryEntry.STATUS_NEEDS_VERIFICATION,
            isFavorite = false
        )
    )
}
