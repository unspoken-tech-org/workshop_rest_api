package com.tproject.workshop.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tproject.workshop.architecture.ArchUnitSupport.PRODUCTION_CLASSES;
import static com.tproject.workshop.architecture.ArchUnitSupport.freeze;
import static org.assertj.core.api.Assertions.assertThat;

import com.tproject.workshop.exception.DomainException;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
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

    // JPA entities must not use Lombok @Data (A17): all-fields equals/hashCode/toString break
    // identity in Sets and recurse through lazy relations. @Data has SOURCE retention, so the rule
    // looks for the canEqual(Object) it generates (also produced by @EqualsAndHashCode).
    @Test
    void entitiesShouldNotUseLombokData() {
        noMethods()
                .that()
                .areDeclaredInClassesThat()
                .areAnnotatedWith(Entity.class)
                .or()
                .areDeclaredInClassesThat()
                .areAnnotatedWith(Embeddable.class)
                .should()
                .haveName("canEqual")
                .because(
                        "@Data/@EqualsAndHashCode on JPA entities is forbidden; use @Getter/@Setter"
                                + " with id-based equals/hashCode and @ToString.Exclude on relations")
                .check(PRODUCTION_CLASSES);
    }

    // @Data has SOURCE retention and Lombok skips equals/hashCode (and canEqual) when the class
    // declares them, so the bytecode rule above cannot see @Data on an entity with a hand-written
    // equals. This source scan closes that gap.
    @Test
    void entitiesShouldNotBeAnnotatedWithLombokData() throws IOException {
        Pattern dataAnnotation = Pattern.compile("(?m)^\\s*@(lombok\\.)?Data\\b");
        List<String> offenders;
        try (Stream<Path> sources = Files.walk(Path.of("src/main/java"))) {
            offenders =
                    sources.filter(path -> path.toString().endsWith(".java"))
                            .filter(path -> {
                                String source = read(path);
                                return (source.contains("@Entity")
                                                || source.contains("@Embeddable"))
                                        && dataAnnotation.matcher(source).find();
                            })
                            .map(Path::toString)
                            .toList();
        }

        assertThat(offenders)
                .as("JPA entities/embeddables must use @Getter/@Setter, not @Data")
                .isEmpty();
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
