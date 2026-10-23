package tn.keeftalk.chat.data.local

import org.junit.Assert.*
import org.junit.Test

class MigrationTest {

    @Test
    fun testAllMigrationObjectsAreNonNullAndRegistered() {
        assertNotNull("MIGRATION_62_63 must not be null", KeeftalkDatabase.MIGRATION_62_63)
        assertNotNull("MIGRATION_63_64 must not be null", KeeftalkDatabase.MIGRATION_63_64)
        assertNotNull("MIGRATION_64_65 must not be null", KeeftalkDatabase.MIGRATION_64_65)
        assertNotNull("MIGRATION_65_66 must not be null", KeeftalkDatabase.MIGRATION_65_66)
        assertNotNull("MIGRATION_66_67 must not be null", KeeftalkDatabase.MIGRATION_66_67)
        assertNotNull("MIGRATION_67_68 must not be null", KeeftalkDatabase.MIGRATION_67_68)
        assertNotNull("MIGRATION_73_74 must not be null", KeeftalkDatabase.MIGRATION_73_74)
        assertNotNull("MIGRATION_74_82 must not be null", KeeftalkDatabase.MIGRATION_74_82)
        assertNotNull("MIGRATION_82_83 must not be null", KeeftalkDatabase.MIGRATION_82_83)
        assertNotNull("MIGRATION_95_96 must not be null", KeeftalkDatabase.MIGRATION_95_96)
        assertNotNull("MIGRATION_96_100 must not be null", KeeftalkDatabase.MIGRATION_96_100)
        assertNotNull("MIGRATION_100_101 must not be null", KeeftalkDatabase.MIGRATION_100_101)
        assertNotNull("MIGRATION_101_102 must not be null", KeeftalkDatabase.MIGRATION_101_102)
        assertNotNull("MIGRATION_102_103 must not be null", KeeftalkDatabase.MIGRATION_102_103)
        assertNotNull("MIGRATION_103_104 must not be null", KeeftalkDatabase.MIGRATION_103_104)
        assertNotNull("MIGRATION_104_105 must not be null", KeeftalkDatabase.MIGRATION_104_105)
        assertNotNull("MIGRATION_112_113 must not be null", KeeftalkDatabase.MIGRATION_112_113)
        assertNotNull("MIGRATION_113_114 must not be null", KeeftalkDatabase.MIGRATION_113_114)
        assertNotNull("MIGRATION_114_115 must not be null", KeeftalkDatabase.MIGRATION_114_115)
        assertNotNull("MIGRATION_115_116 must not be null", KeeftalkDatabase.MIGRATION_115_116)
        assertNotNull("MIGRATION_116_117 must not be null", KeeftalkDatabase.MIGRATION_116_117)
        assertNotNull("MIGRATION_117_118 must not be null", KeeftalkDatabase.MIGRATION_117_118)
        assertNotNull("MIGRATION_118_119 must not be null", KeeftalkDatabase.MIGRATION_118_119)
    }

    @Test
    fun testMigrationBoundaryVersionsAreSequential() {
        assertEquals(62, KeeftalkDatabase.MIGRATION_62_63.startVersion)
        assertEquals(63, KeeftalkDatabase.MIGRATION_62_63.endVersion)

        assertEquals(66, KeeftalkDatabase.MIGRATION_66_67.startVersion)
        assertEquals(67, KeeftalkDatabase.MIGRATION_66_67.endVersion)

        assertEquals(74, KeeftalkDatabase.MIGRATION_74_82.startVersion)
        assertEquals(82, KeeftalkDatabase.MIGRATION_74_82.endVersion)

        assertEquals(96, KeeftalkDatabase.MIGRATION_96_100.startVersion)
        assertEquals(100, KeeftalkDatabase.MIGRATION_96_100.endVersion)

        assertEquals(101, KeeftalkDatabase.MIGRATION_101_102.startVersion)
        assertEquals(102, KeeftalkDatabase.MIGRATION_101_102.endVersion)

        assertEquals(103, KeeftalkDatabase.MIGRATION_103_104.startVersion)
        assertEquals(104, KeeftalkDatabase.MIGRATION_103_104.endVersion)

        assertEquals(113, KeeftalkDatabase.MIGRATION_113_114.startVersion)
        assertEquals(114, KeeftalkDatabase.MIGRATION_113_114.endVersion)

        assertEquals(116, KeeftalkDatabase.MIGRATION_116_117.startVersion)
        assertEquals(117, KeeftalkDatabase.MIGRATION_116_117.endVersion)

        assertEquals(117, KeeftalkDatabase.MIGRATION_117_118.startVersion)
        assertEquals(118, KeeftalkDatabase.MIGRATION_117_118.endVersion)

        assertEquals(118, KeeftalkDatabase.MIGRATION_118_119.startVersion)
        assertEquals(119, KeeftalkDatabase.MIGRATION_118_119.endVersion)
    }
}
