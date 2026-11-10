package com.example.runbuddy.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/**
 * Importing runs knows nothing of AI reviews (#36): the import announces new runs with
 * {@code RunsImported}, and {@code review} listens. So the import can neither wait for the AI nor fail
 * with it, and runs can be imported with no AI configured at all.
 */
class ImportDependencyRulesTest {

    private static final JavaClasses MAIN_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.example.runbuddy");

    @Test
    void activitiesDoNotDependOnReviewsOrTheAdvisor() {
        noClasses().that().resideInAPackage("com.example.runbuddy.activity..")
                .should().dependOnClassesThat().resideInAnyPackage("com.example.runbuddy.review..", "com.example.runbuddy.advisor..")
                .check(MAIN_CLASSES);
    }
}
