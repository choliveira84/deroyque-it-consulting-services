package com.deroyque.activation.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "com.deroyque.activation", importOptions = {ImportOption.DoNotIncludeTests.class})
public class ArchitectureRulesTest {

    @ArchTest
    static final ArchRule hexagonal_architecture_layers_must_be_respected =
            layeredArchitecture()
                    .consideringAllDependencies()
                    .layer("Domain").definedBy("..domain..")
                    .layer("Application").definedBy("..application..")
                    .layer("Infrastructure").definedBy("..infrastructure..")

                    .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure")
                    .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
                    .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer();

    @ArchTest
    static final ArchRule domain_and_application_must_not_depend_on_swagger =
            noClasses().that()
                    .resideInAnyPackage("..domain..", "..application..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("io.swagger..", "org.springdoc..");
}
