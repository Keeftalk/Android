package tn.keeftalk.chat.extension

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import tn.keeftalk.chat.extension.registry.DictionaryRegistry
import tn.keeftalk.chat.extension.registry.OfflineDictionaryEntry
import java.io.File

class DictionaryRegistryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testComplexJsonDictionaryLookup() {
        val jsonContent = """
            {
              "A": {
                "MEANINGS": [
                  ["Noun", "the 1st letter of the Roman alphabet", ["Letter"], []]
                ],
                "SYNONYMS": ["Ampere", "Angstrom"]
              },
              "AARDVARK": {
                "MEANINGS": [
                  ["Noun", "nocturnal burrowing mammal of Africa", ["Placental"], []]
                ],
                "SYNONYMS": ["Ant bear"]
              }
            }
        """.trimIndent()

        val jsonFile = File(tempFolder.root, "words.json")
        jsonFile.writeText(jsonContent)

        val registry = DictionaryRegistry()
        val entry = OfflineDictionaryEntry(
            id = "com.keeftalk.dictionary.english",
            title = "English Dictionary",
            dbFilePath = jsonFile.absolutePath
        )

        registry.registerDictionary(entry)

        val resultA = registry.lookupWord("a")
        assertNotNull(resultA)
        assertTrue(resultA!!.definition.contains("1st letter of the Roman alphabet"))
        assertTrue(resultA.definition.contains("Ampere"))

        val resultAardvark = registry.lookupWord("Aardvark")
        assertNotNull(resultAardvark)
        assertTrue(resultAardvark!!.definition.contains("nocturnal burrowing mammal"))
    }
}
