package tn.keeftalk.chat.extension

import org.junit.Assert.*
import org.junit.Test
import tn.keeftalk.chat.extension.workflow.*

class WorkflowEngineTest {

    @Test
    fun testSuccessfulWorkflowExecution() {
        val definition = WorkflowDefinition(
            id = "test.workflow",
            name = "Test Workflow",
            steps = listOf(
                WorkflowStep(
                    id = "step1",
                    type = WorkflowStepType.ASSIGN,
                    key = "username",
                    value = "keeftalk_user",
                    successNextStep = "step2"
                ),
                WorkflowStep(
                    id = "step2",
                    type = WorkflowStepType.TRANSFORM_UPPER,
                    inputKey = "username",
                    outputKey = "username_upper",
                    successNextStep = "step3"
                ),
                WorkflowStep(
                    id = "step3",
                    type = WorkflowStepType.VALIDATE_NOT_BLANK,
                    inputKey = "username_upper",
                    successNextStep = null
                )
            )
        )

        val engine = WorkflowEngine()
        val result = engine.executeWorkflow(definition)

        assertTrue(result.success)
        assertEquals("KEEFTALK_USER", result.data["username_upper"])
        assertEquals(3, result.stepsExecuted)
        assertNull(result.errorMessage)
    }

    @Test
    fun testMathCalculationWorkflow() {
        val definition = WorkflowDefinition(
            id = "math.workflow",
            name = "Math Workflow",
            steps = listOf(
                WorkflowStep(
                    id = "s1",
                    type = WorkflowStepType.ASSIGN,
                    key = "score",
                    value = "85",
                    successNextStep = "s2"
                ),
                WorkflowStep(
                    id = "s2",
                    type = WorkflowStepType.CALCULATE_MATH,
                    inputKey = "score",
                    outputKey = "bonus_score",
                    expression = "+",
                    operand = 15.0
                )
            )
        )

        val engine = WorkflowEngine()
        val result = engine.executeWorkflow(definition)

        assertTrue(result.success)
        assertEquals("100.0", result.data["bonus_score"])
    }
}
