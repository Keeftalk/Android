package tn.keeftalk.chat.feature.mydin

import org.junit.Assert.*
import org.junit.Test
import tn.keeftalk.chat.feature.mydin.model.*
import tn.keeftalk.chat.feature.mydin.service.MyDinAudioService

class MyDinModelTest {

    @Test
    fun testCitiesListContainsCairoAndMakkah() {
        assertTrue(CITIES.isNotEmpty())
        val cairo = CITIES.find { it.name == "Cairo" }
        assertNotNull(cairo)
        assertEquals("القاهرة", cairo?.arName)

        val makkah = CITIES.find { it.name == "Makkah" }
        assertNotNull(makkah)
        assertEquals("مكة المكرمة", makkah?.arName)
    }

    @Test
    fun testDouaaItemsAndCategories() {
        assertEquals(39, DOUAA_ITEMS.size)
        assertEquals(8, DOUAA_CATEGORIES.size)

        val morningDouaas = DOUAA_ITEMS.filter { it.cat == "morning" }
        assertTrue(morningDouaas.isNotEmpty())

        val d1 = DOUAA_ITEMS.find { it.id == "d1" }
        assertNotNull(d1)
        assertEquals("الاستغفار بعد الصلاة", d1?.title)
    }

    @Test
    fun testHadithItemsAndBooks() {
        assertTrue(HADITH_ITEMS.isNotEmpty())
        assertEquals(7, HADITH_BOOKS.size)
        assertEquals(6, HADITH_CATEGORIES.size)

        val bukhariBook = HADITH_BOOKS.find { it.id == "bukhari" }
        assertNotNull(bukhariBook)
        assertEquals("صحيح البخاري", bukhariBook?.nameAr)
    }

    @Test
    fun testSurahList114Surahs() {
        assertEquals(114, SURAH_LIST.size)
        val fatihah = SURAH_LIST[0]
        assertEquals(1, fatihah.number)
        assertEquals("الفاتحة", fatihah.arName)
        assertEquals(7, fatihah.ayahsCount)

        val nas = SURAH_LIST[113]
        assertEquals(114, nas.number)
        assertEquals("الناس", nas.arName)
        assertEquals(6, nas.ayahsCount)
    }

    @Test
    fun testAll114SurahDescriptionsAndVirtues() {
        for (i in 1..114) {
            val desc = getSurahDescription(i)
            val virtue = getSurahVirtue(i)
            assertTrue("Description missing for surah $i", desc.isNotBlank())
            assertTrue("Virtue missing for surah $i", virtue.isNotBlank())
        }
    }

    @Test
    fun testRecitersList() {
        assertTrue(RECITERS.isNotEmpty())
        val defaultReciter = RECITERS[0]
        assertEquals("Husary_128kbps", defaultReciter.id)
        assertEquals("الحصري", defaultReciter.shortName)
    }

    @Test
    fun testTajweedParser() {
        val rawTajweed = "[n[بِسْمِ]] [m[اللَّهِ]]"
        val parsed = parseTajweedToAnnotatedString(rawTajweed, isDark = false)
        assertEquals("بِسْمِ اللَّهِ", parsed.text)
    }

    @Test
    fun testDownloadSizeEstimation() {
        val baqarah = SURAH_LIST.find { it.number == 2 }!!
        val estimatedMb = baqarah.ayahsCount * 0.35f
        assertTrue(estimatedMb > 90f)
    }

    @Test
    fun testAudioServiceActions() {
        assertEquals("tn.keeftalk.chat.mydin.ACTION_PLAY_AYAH", MyDinAudioService.ACTION_PLAY_AYAH)
        assertEquals("tn.keeftalk.chat.mydin.ACTION_TOGGLE_PLAY", MyDinAudioService.ACTION_TOGGLE_PLAY)
        assertEquals("tn.keeftalk.chat.mydin.ACTION_PREV", MyDinAudioService.ACTION_PREV)
        assertEquals("tn.keeftalk.chat.mydin.ACTION_NEXT", MyDinAudioService.ACTION_NEXT)
    }
}
