package tn.keeftalk.chat.extension.workflow

import android.util.Log
import kotlinx.serialization.Serializable

@Serializable
data class ScriptExpression(
    val expression: String, // e.g., "score + 10", "status == active"
    val variables: Map<String, String> = emptyMap()
)

class ExtensionScriptInterpreter {
    private val TAG = "ExtensionScriptInterpreter"

    fun evaluate(expression: String, contextVariables: Map<String, String>): String {
        try {
            val cleanExpr = expression.trim()
            // Handle simple arithmetic (e.g. "a + b" or "val * 2")
            if (cleanExpr.contains("+")) {
                val parts = cleanExpr.split("+").map { it.trim() }
                val v1 = resolveValue(parts[0], contextVariables).toDoubleOrNull() ?: 0.0
                val v2 = resolveValue(parts[1], contextVariables).toDoubleOrNull() ?: 0.0
                return (v1 + v2).toString()
            }
            if (cleanExpr.contains("-")) {
                val parts = cleanExpr.split("-").map { it.trim() }
                val v1 = resolveValue(parts[0], contextVariables).toDoubleOrNull() ?: 0.0
                val v2 = resolveValue(parts[1], contextVariables).toDoubleOrNull() ?: 0.0
                return (v1 - v2).toString()
            }
            if (cleanExpr.contains("*")) {
                val parts = cleanExpr.split("*").map { it.trim() }
                val v1 = resolveValue(parts[0], contextVariables).toDoubleOrNull() ?: 0.0
                val v2 = resolveValue(parts[1], contextVariables).toDoubleOrNull() ?: 0.0
                return (v1 * v2).toString()
            }
            if (cleanExpr.contains("==")) {
                val parts = cleanExpr.split("==").map { it.trim() }
                val v1 = resolveValue(parts[0], contextVariables)
                val v2 = resolveValue(parts[1], contextVariables)
                return (v1 == v2).toString()
            }

            return resolveValue(cleanExpr, contextVariables)
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating expression '$expression': ${e.message}")
            return "0"
        }
    }

    private fun resolveValue(token: String, contextVariables: Map<String, String>): String {
        val clean = token.trim()
        if (clean.startsWith("\"") && clean.endsWith("\"")) {
            return clean.removeSurrounding("\"")
        }
        return contextVariables[clean] ?: clean
    }
}
