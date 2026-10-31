package com.example.runbuddy.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.runbuddy.activity.RunResultRepository;
import com.example.runbuddy.plan.MonthlyPlanRepository;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * A port implemented by a {@code @Transactional} adapter must not have {@code default} methods.
 *
 * <p>A default method runs on the Spring proxy's target without going through the proxy again, so
 * the methods it calls run outside the adapter's transaction. Without a transaction,
 * {@code RlsTransactionManager} never sets {@code app.user_id} and the database refuses the query
 * (this happened with {@code getOrEmpty}, see ADR-0007). The rule covers every such interface, so a
 * new port is checked without being listed here.
 */
class TransactionalPortRulesTest {

    private static final JavaClasses MAIN_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.example.runbuddy");

    static final DescribedPredicate<JavaClass> IMPLEMENTED_BY_A_TRANSACTIONAL_CLASS =
            DescribedPredicate.describe("are implemented by a @Transactional class",
                    port -> port.getAllSubclasses().stream().anyMatch(TransactionalPortRulesTest::isTransactional));

    static final ArchCondition<JavaClass> HAVE_NO_DEFAULT_METHODS =
            new ArchCondition<>("have no default methods") {
                @Override
                public void check(JavaClass port, ConditionEvents events) {
                    port.getMethods().stream()
                            .filter(TransactionalPortRulesTest::isDefaultMethod)
                            .forEach(method -> events.add(SimpleConditionEvent.violated(method,
                                    method.getFullName() + " is a default method; move it into the adapter "
                                            + "or the caller (ADR-0007)")));
                }
            };

    static final ArchRule PORTS_OF_TRANSACTIONAL_ADAPTERS_HAVE_NO_DEFAULT_METHODS = classes()
            .that().areInterfaces()
            .and(IMPLEMENTED_BY_A_TRANSACTIONAL_CLASS)
            .should(HAVE_NO_DEFAULT_METHODS)
            .because("a default method calls the adapter without its transaction, so row level security "
                    + "has no user and the query fails (ADR-0007)");

    @Test
    void portsOfTransactionalAdaptersHaveNoDefaultMethods() {
        PORTS_OF_TRANSACTIONAL_ADAPTERS_HAVE_NO_DEFAULT_METHODS.check(MAIN_CLASSES);
    }

    /** Guards the rule itself: if it stopped finding the ports, it would pass without checking anything. */
    @Test
    void theRuleCoversTheRepositoryPorts() {
        Set<String> covered = MAIN_CLASSES.stream()
                .filter(JavaClass::isInterface)
                .filter(IMPLEMENTED_BY_A_TRANSACTIONAL_CLASS)
                .map(JavaClass::getName)
                .collect(Collectors.toSet());

        assertThat(covered).contains(MonthlyPlanRepository.class.getName(), RunResultRepository.class.getName());
    }

    /** Proves the condition catches a default method, on an interface written only for this test. */
    @Test
    void aDefaultMethodIsReported() {
        JavaClasses offending = new ClassFileImporter().importClasses(PortWithDefault.class);

        var result = classes().that().areInterfaces().should(HAVE_NO_DEFAULT_METHODS).evaluate(offending);

        assertThat(result.hasViolation()).isTrue();
        assertThat(result.getFailureReport().getDetails()).singleElement().asString()
                .contains("PortWithDefault.getOrEmpty()");
    }

    interface PortWithDefault {
        String find();

        default String getOrEmpty() {
            return find();
        }

        static PortWithDefault of(String value) {
            return () -> value;
        }
    }

    private static boolean isTransactional(JavaClass type) {
        return type.isAnnotatedWith(Transactional.class)
                || type.getMethods().stream().anyMatch(method -> method.isAnnotatedWith(Transactional.class));
    }

    private static boolean isDefaultMethod(JavaMethod method) {
        Set<JavaModifier> modifiers = method.getModifiers();
        return !modifiers.contains(JavaModifier.ABSTRACT)
                && !modifiers.contains(JavaModifier.STATIC)
                && !modifiers.contains(JavaModifier.SYNTHETIC)
                && !modifiers.contains(JavaModifier.BRIDGE);
    }
}
