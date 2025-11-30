package unq.desapp.futbol;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "unq.desapp.futbol")
public class ArchitectureTest {

    @ArchTest
    static final ArchRule sanity_check = classes()
            .that().resideInAPackage("unq.desapp.futbol")
            .should().resideInAPackage("unq.desapp.futbol");

}
