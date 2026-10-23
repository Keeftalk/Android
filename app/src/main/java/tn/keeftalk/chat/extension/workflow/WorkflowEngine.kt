package tn.keeftalk.chat.extension.workflow

import android.util.Log
import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicInteger

@Serializable
enum class WorkflowStepType {
    ASSIGN,
    TRANSFORM_UPPER,
    TRANSFORM_LOWER,
    VALIDATE_NOT_BLANK,
    VALIDATE_REGEX,
    CALCULATE_MATH,
    CONDITIONAL_BRANCH,
    LOOP_COUNT,
    SHOW_MESSAGE
}

@Serializable
data class WorkflowStep(
    val id: String,
    val type: WorkflowStepType,
    val key: String = "",
    val inputKey: String = "",
    val outputKey: String = "",
    val value: String = "",
    val regex: String = "",
    val expression: String = "+", // e.g., "+", "-", "*", "/", "%"
    val operand: Double = 0.0,
    val successNextStep: String? = null,
    val failureNextStep: String? = null
)

@Serializable
data class WorkflowDefinition(
    val id: String,
    val name: String,
    val maxSteps: Int = 200,
    val steps: List<WorkflowStep> = emptyList()
)

data class WorkflowExecutionResult(
    val success: Boolean,
    val data: Map<String, String>,
    val stepsExecuted: Int,
    val errorMessage: String? = null
)

class WorkflowEngine {
    private val TAG = "WorkflowEngine"
    private val MAX_ALLOWED_STEPS = 500

    fun executeWorkflow(
        definition: WorkflowDefinition,
        initialData: Map<String, String> = emptyMap()
    ): WorkflowExecutionResult {
        val memory = initialData.toMutableMap()
        val stepMap = definition.steps.associateBy { it.id }
        var currentStepId = definition.steps.firstOrNull()?.id
        val stepsCounter = AtomicInteger(0)
        val budget = definition.maxSteps.coerceAtMost(MAX_ALLOWED_STEPS)

        try {
            while (currentStepId != null) {
                if (stepsCounter.incrementAndGet() > budget) {
                    return WorkflowExecutionResult(
                        success = false,
                        data = memory,
                        stepsExecuted = stepsCounter.get(),
                        errorMessage = "Execution budget exceeded ($budget max steps). Possible infinite loop."
                    )
                }

                val step = stepMap[currentStepId] ?: break
                var stepSuccess = true

                when (step.type) {
                    WorkflowStepType.ASSIGN -> {
                        memory[step.key.ifBlank { step.outputKey }] = step.value
                    }
                    WorkflowStepType.TRANSFORM_UPPER -> {
                        val input = memory[step.inputKey] ?: ""
                        memory[step.outputKey] = input.uppercase()
                    }
                    WorkflowStepType.TRANSFORM_LOWER -> {
                        val input = memory[step.inputKey] ?: ""
                        memory[step.outputKey] = input.lowercase()
                    }
                    WorkflowStepType.VALIDATE_NOT_BLANK -> {
                        val input = memory[step.inputKey] ?: ""
                        stepSuccess = input.isNotBlank()
                    }
                    WorkflowStepType.VALIDATE_REGEX -> {
                        val input = memory[step.inputKey] ?: ""
                        stepSuccess = input.matches(Regex(step.regex))
                    }
                    WorkflowStepType.CALCULATE_MATH -> {
                        val inputVal = (memory[step.inputKey] ?: "0").toDoubleOrNull() ?: 0.0
                        val res = when (step.expression) {
                            "+" -> inputVal + step.operand
                            "-" -> inputVal - step.operand
                            "*" -> inputVal * step.operand
                            "/" -> if (step.operand != 0.0) inputVal / step.operand else 0.0
                            else -> inputVal
                        }
                        memory[step.outputKey] = res.toString()
                    }
                    WorkflowStepType.CONDITIONAL_BRANCH -> {
                        val val1 = memory[step.inputKey] ?: ""
                        stepSuccess = val1 == step.value
                    }
                    WorkflowStepType.LOOP_COUNT -> {
                        val count = step.value.toIntOrNull() ?: 1
                        val currentLoop = (memory[step.key] ?: "0").toIntOrNull() ?: 0
                        if (currentLoop < count) {
                            memory[step.key] = (currentLoop + 1).toString()
                            stepSuccess = true
                        } else {
                            stepSuccess = false
                        }
                    }
                    WorkflowStepType.SHOW_MESSAGE -> {
                        Log.i(TAG, "Workflow [${definition.id}] message: ${step.value}")
                    }
                }

                currentStepId = if (stepSuccess) {
                    step.successNextStep
                } else {
                    step.failureNextStep
                }
            }

            return WorkflowExecutionResult(
                success = true,
                data = memory,
                stepsExecuted = stepsCounter.get(),
                errorMessage = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Workflow execution error: ${e.message}", e)
            return WorkflowExecutionResult(
                success = false,
                data = memory,
                stepsExecuted = stepsCounter.get(),
                errorMessage = e.message ?: "Unknown workflow execution error"
            )
        }
    }
}
