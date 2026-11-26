package com.tproject.workshop.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tproject.workshop.architecture.ArchUnitSupport.TEST_CLASSES;
import static com.tproject.workshop.architecture.ArchUnitSupport.freeze;

import com.tproject.workshop.integration.support.AbstractIntegrationTest;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;

class TestConventionsArchitectureTest {

    @Test
    void testsShouldNotUseApacheHttpStatus() {
        freeze(
                        noClasses()
                                .should()
                                .dependOnClassesThat()
                                .haveFullyQualifiedName("org.apache.http.HttpStatus"))
                .check(TEST_CLASSES);
    }

    @Test
    void testsShouldNotUseLenientMockitoStrictness() {
        noClasses()
                .that()
                .resideOutsideOfPackage("..architecture..")
                .should()
                .dependOnClassesThat()
                .areAssignableTo(Strictness.class)
                .check(TEST_CLASSES);
    }

    @Test
    void testsShouldNotCallSystemDefaultZoneClock() {
        noClasses()
                .that()
                .resideOutsideOfPackage("..architecture..")
                .should()
                .callMethod(Clock.class, "systemDefaultZone")
                .check(TEST_CLASSES);
    }

    @Test
    void unitTestsShouldNotUseSpringBootTest() {
        classes()
                .that()
                .haveSimpleNameEndingWith("Test")
                .and()
                .resideOutsideOfPackage("..integration.support..")
                .and()
                .resideOutsideOfPackage("..architecture..")
                .should()
                .notBeAnnotatedWith(SpringBootTest.class)
                .check(TEST_CLASSES);
    }

    @Test
    void unitTestsShouldBePackagePrivate() {
        classes()
                .that()
                .haveSimpleNameEndingWith("Test")
                .and()
                .resideOutsideOfPackage("..integration.support..")
                .and()
                .areTopLevelClasses()
                .should()
                .bePackagePrivate()
                .check(TEST_CLASSES);
    }

    @Test
    void integrationTestsShouldExtendAbstractIntegrationTest() {
        freeze(
                        classes()
                                .that()
                                .haveSimpleNameEndingWith("IT")
                                .and()
                                .resideOutsideOfPackage("..integration.support..")
                                .should()
                                .beAssignableTo(AbstractIntegrationTest.class))
                .check(TEST_CLASSES);
    }
}
