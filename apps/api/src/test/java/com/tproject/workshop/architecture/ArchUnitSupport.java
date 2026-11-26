package com.tproject.workshop.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

final class ArchUnitSupport {

    static final JavaClasses PRODUCTION_CLASSES =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("com.tproject.workshop");

    static final JavaClasses TEST_CLASSES =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.ONLY_INCLUDE_TESTS)
                    .importPackages("com.tproject.workshop");

    private ArchUnitSupport() {}

    static ArchRule freeze(ArchRule rule) {
        return FreezingArchRule.freeze(rule);
    }
}
