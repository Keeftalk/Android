package tn.keeftalk.chat.extension

import org.junit.Assert.*
import org.junit.Test
import tn.keeftalk.chat.extension.workflow.ExtensionScriptInterpreter

class ExtensionScriptInterpreterTest {

    @Test
    fun testScriptEvaluation() {
        val interpreter = ExtensionScriptInterpreter()
        val vars = mapOf("score" to "80", "bonus" to "20", "status" to "active")

        assertEquals("100.0", interpreter.evaluate("score + bonus", vars))
        assertEquals("60.0", interpreter.evaluate("score - 20", vars))
        assertEquals("true", interpreter.evaluate("status == active", vars))
    }
}
