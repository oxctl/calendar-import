package uk.ac.ox.it.calendarimporter;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Allows tests to be only enabled when the integration test file exists.
 */
public class IntegrationTestCondition implements ExecutionCondition {

    public static final String TEST_PROPERTIES = "integration-test.properties";

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (Files.exists(Path.of(TEST_PROPERTIES))) {
            return ConditionEvaluationResult.enabled("Found configuration: "+ TEST_PROPERTIES);
        } else {
            return ConditionEvaluationResult.disabled("Disabled because no file: "+ TEST_PROPERTIES);
        }
    }
}