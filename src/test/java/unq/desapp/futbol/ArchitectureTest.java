package unq.desapp.futbol;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import com.tngtech.archunit.core.importer.ImportOption;

import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "unq.desapp.futbol", importOptions = { ImportOption.DoNotIncludeTests.class })
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

    @ArchTest
    static final ArchRule controllers_should_have_name_ending_with_controller = classes()
            .that().resideInAPackage("..webservice..")
            .and().areTopLevelClasses()
            .should().haveSimpleNameEndingWith("Controller");

    @ArchTest
    static final ArchRule services_should_have_name_ending_with_service = classes()
            .that().resideInAPackage("..service..")
            .and().areTopLevelClasses()
            .should().haveSimpleNameEndingWith("Service")
            .orShould().haveSimpleNameEndingWith("ServiceImpl");

    @ArchTest
    static final ArchRule repositories_should_have_name_ending_with_repository = classes()
            .that().resideInAPackage("..repository..")
            .and().areTopLevelClasses()
            .should().haveSimpleNameEndingWith("Repository");

    @ArchTest
    static final ArchRule controllers_should_be_annotated_with_rest_controller = classes()
            .that().resideInAPackage("..webservice..")
            .and().areTopLevelClasses()
            .should().beAnnotatedWith(RestController.class);

    @ArchTest
    static final ArchRule services_should_be_annotated_with_service = classes()
            .that().resideInAPackage("..service..")
            .and().areTopLevelClasses()
            .and().haveSimpleNameEndingWith("ServiceImpl")
            .should().beAnnotatedWith(Service.class);

    @ArchTest
    static final ArchRule repositories_should_be_annotated_with_repository = classes()
            .that().resideInAPackage("..repository..")
            .and().areTopLevelClasses()
            .should().beAnnotatedWith(Repository.class);

}
