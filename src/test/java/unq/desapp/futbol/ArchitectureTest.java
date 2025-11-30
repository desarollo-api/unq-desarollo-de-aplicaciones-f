package unq.desapp.futbol;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "unq.desapp.futbol")
public class ArchitectureTest {

    @ArchTest
    static final ArchRule sanity_check = classes()
            .that().resideInAPackage("unq.desapp.futbol")
            .should().resideInAPackage("unq.desapp.futbol");

    @ArchTest
    static final ArchRule layered_architecture = layeredArchitecture()
            .consideringOnlyDependenciesInAnyPackage("unq.desapp.futbol..")
            .layer("Controller").definedBy("..webservice..")
            .layer("Service").definedBy("..service..")
            .layer("Repository").definedBy("..repository..")
            .layer("Security").definedBy("..security..")
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Security")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service");

}
