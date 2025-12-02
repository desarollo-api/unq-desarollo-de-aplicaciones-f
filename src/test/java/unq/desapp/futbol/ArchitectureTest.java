package unq.desapp.futbol;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(packages = "unq.desapp.futbol", importOptions = { ImportOption.DoNotIncludeTests.class })
@Tag("arch")
public class ArchitectureTest {

        @ArchTest
        static final ArchRule layered_architecture = layeredArchitecture()
                        .consideringOnlyDependenciesInAnyPackage("unq.desapp.futbol..")
                        .layer("Controller").definedBy("..controller..", "..webservice..")
                        .layer("Service Interfaces").definedBy("..service")
                        .layer("Service Implementations").definedBy("..service.impl..")
                        .layer("Repository").definedBy("..repository..", "..persistence..")
                        .layer("Security").definedBy("..security..", "..auth..")
                        .layer("Model").definedBy("..model..")
                        .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                        .whereLayer("Service Implementations").mayOnlyBeAccessedByLayers("Controller", "Security")
                        .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service Implementations");

        @ArchTest
        static final ArchRule controllers_should_have_name_ending_with_controller = classes()
                        .that().resideInAPackage("..controller..")
                        .and().areTopLevelClasses()
                        .should().haveSimpleNameEndingWith("Controller");

        @ArchTest
        static final ArchRule services_should_have_name_ending_with_service = classes()
                        .that().resideInAPackage("..service")
                        .and().areTopLevelClasses()
                        .and().areInterfaces()
                        .should().haveSimpleNameEndingWith("Service");

        @ArchTest
        static final ArchRule service_impls_should_have_name_ending_with_service_impl = classes()
                        .that().resideInAPackage("..service.impl..")
                        .and().areTopLevelClasses()
                        .should().haveSimpleNameEndingWith("ServiceImpl");

        @ArchTest
        static final ArchRule repositories_should_have_name_ending_with_repository = classes()
                        .that().resideInAPackage("..repository..")
                        .and().areTopLevelClasses()
                        .should().haveSimpleNameEndingWith("Repository");

        @ArchTest
        static final ArchRule controllers_should_be_annotated_with_rest_controller = classes()
                        .that().resideInAPackage("..controller..")
                        .and().areTopLevelClasses()
                        .should().beAnnotatedWith(RestController.class);

        @ArchTest
        static final ArchRule services_should_be_annotated_with_service = classes()
                        .that().resideInAPackage("..service.impl..")
                        .and().areTopLevelClasses()
                        .should().beAnnotatedWith(Service.class);

        @ArchTest
        static final ArchRule service_interfaces_should_not_be_annotated_with_service = classes()
                        .that().resideInAPackage("..service")
                        .and().areInterfaces()
                        .should().notBeAnnotatedWith(Service.class);

        @ArchTest
        static final ArchRule repositories_should_be_annotated_with_repository = classes()
                        .that().resideInAPackage("..repository..")
                        .and().areTopLevelClasses()
                        .should().beAnnotatedWith(Repository.class);

        @ArchTest
        static final ArchRule no_cycles_between_packages = slices()
                        .matching("unq.desapp.futbol.(*)..")
                        .should().beFreeOfCycles();

        @ArchTest
        static final ArchRule no_field_injection = noFields()
                        .should().beAnnotatedWith(Autowired.class)
                        .because("Field injection is discouraged; use constructor injection instead.");
}
