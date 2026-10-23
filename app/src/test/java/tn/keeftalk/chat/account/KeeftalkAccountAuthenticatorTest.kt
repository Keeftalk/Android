package tn.keeftalk.chat.account

import org.junit.Assert.assertEquals
import org.junit.Test

class KeeftalkAccountAuthenticatorTest {

    @Test
    fun testAuthenticatorConstants() {
        assertEquals("tn.keeftalk.account", KeeftalkAbstractAccountAuthenticator.ACCOUNT_TYPE)
        assertEquals("Full Access", KeeftalkAbstractAccountAuthenticator.AUTH_TOKEN_TYPE_FULL_ACCESS)
    }
}
