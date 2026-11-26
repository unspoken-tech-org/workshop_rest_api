package com.tproject.workshop.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tproject.workshop.architecture.ArchUnitSupport.PRODUCTION_CLASSES;
import static com.tproject.workshop.architecture.ArchUnitSupport.freeze;

import com.tproject.workshop.exception.DomainException;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

class ProductionArchitectureTest {

    @Test
    void controllersShouldNotDependOnRepositories() {
        freeze(
                        noClasses()
                                .that()
                                .resideInAPackage("..controller..")
                                .should()
                                .dependOnClassesThat()
                                .resideInAPackage("..repository.."))
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void securityConfigShouldNotDependOnControllers() {
        noClasses()
                .that()
                .resideInAPackage("..config.security..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..controller..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void controllerImplementationsShouldNotDeclareRequestMapping() {
        freeze(
                        noClasses()
                                .that()
                                .resideInAPackage("..controller.impl..")
                                .should()
                                .beAnnotatedWith(RequestMapping.class))
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void entitiesShouldNotGenerateLombokEqualsAndHashCode() {
        // @Data / @EqualsAndHashCode generate canEqual(Object) in bytecode (SOURCE retention)
        freeze(
                        noMethods()
                                .that()
                                .areDeclaredInClassesThat()
                                .areAnnotatedWith(Entity.class)
                                .should()
                                .haveName("canEqual"))
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void domainExceptionsShouldExtendDomainException() {
        freeze(
                        classes()
                                .that()
                                .resideInAPackage("..exception..")
                                .and()
                                .areAssignableTo(Throwable.class)
                                .should()
                                .beAssignableTo(DomainException.class))
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void dtosShouldBeRecordsWithStandardSuffix() {
        freeze(
                        classes()
                                .that()
                                .resideInAPackage("..dto..")
                                .and()
                                .areTopLevelClasses()
                                .and()
                                .areNotEnums()
                                .should()
                                .beRecords()
                                .andShould()
                                .haveNameMatching(".*(Request|Response|Query)$"))
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void shouldUsePluralUtilsPackageInsteadOfUtil() {
        freeze(noClasses().should().resideInAPackage("..util..")).check(PRODUCTION_CLASSES);
    }
}
