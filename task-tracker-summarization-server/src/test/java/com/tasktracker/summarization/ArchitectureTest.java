package com.tasktracker.summarization;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "com.tasktracker.summarization", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String ROOT = "com.tasktracker.summarization";

    private static final String CONTROLLER = ROOT + ".controller..";
    private static final String SERVICE = ROOT + ".service..";
    private static final String REPOSITORY = ROOT + ".repository..";
    private static final String ENTITY = ROOT + ".entity..";
    private static final String DTO = ROOT + ".dto..";
    private static final String EXCEPTION = ROOT + ".exception..";
    private static final String VALIDATION = ROOT + ".validation..";
    private static final String CONFIG = ROOT + ".config..";
    private static final String SECURITY = ROOT + ".security..";

    @ArchTest
    static final ArchRule every_class_lives_in_a_known_package = classes()
            .that().doNotHaveFullyQualifiedName(ROOT + ".SummarizationApplication")
            .should().resideInAnyPackage(
                    CONTROLLER, SERVICE, REPOSITORY, ENTITY, DTO, EXCEPTION, VALIDATION, CONFIG, SECURITY)
            .because("классы раскладываются по слоям, иначе сборка падает");

    @ArchTest
    static final ArchRule only_services_use_repositories = noClasses()
            .that().resideOutsideOfPackages(SERVICE, REPOSITORY)
            .should().dependOnClassesThat().resideInAPackage(REPOSITORY)
            .because("к репозиториям ходят только сервисы");

    @ArchTest
    static final ArchRule nobody_depends_on_controllers = noClasses()
            .that().resideOutsideOfPackage(CONTROLLER)
            .should().dependOnClassesThat().resideInAPackage(CONTROLLER)
            .because("контроллеры — верхний слой");

    @ArchTest
    static final ArchRule repositories_do_not_depend_on_upper_layers = noClasses()
            .that().resideInAPackage(REPOSITORY)
            .should().dependOnClassesThat().resideInAnyPackage(CONTROLLER, SERVICE, DTO, CONFIG, SECURITY)
            .because("репозитории не зависят от верхних слоёв");

    @ArchTest
    static final ArchRule only_services_and_repositories_use_entities = noClasses()
            .that().resideOutsideOfPackages(SERVICE, REPOSITORY, ENTITY)
            .should().dependOnClassesThat().resideInAPackage(ENTITY)
            .because("сущности не выходят за service и repository");

    @ArchTest
    static final ArchRule entities_are_independent = noClasses()
            .that().resideInAPackage(ENTITY)
            .should().dependOnClassesThat().resideInAnyPackage(
                    CONTROLLER, SERVICE, REPOSITORY, DTO, EXCEPTION, VALIDATION, CONFIG, SECURITY)
            .because("сущности не зависят от остальных слоёв");

    @ArchTest
    static final ArchRule dto_are_plain_data = noClasses()
            .that().resideInAPackage(DTO)
            .should().dependOnClassesThat().resideInAnyPackage(
                    CONTROLLER, SERVICE, REPOSITORY, ENTITY, EXCEPTION, VALIDATION, CONFIG, SECURITY)
            .because("DTO — простые данные без зависимостей от слоёв");

    @ArchTest
    static final ArchRule no_package_cycles = slices()
            .matching(ROOT + ".(*)..")
            .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule controllers_live_in_controller_package = classes()
            .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .or().areAnnotatedWith(Controller.class)
            .should().resideInAPackage(CONTROLLER)
            .andShould().haveSimpleNameEndingWith("Controller")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule services_live_in_service_package = classes()
            .that().areAnnotatedWith(Service.class)
            .should().resideInAPackage(SERVICE)
            .andShould().haveSimpleNameEndingWith("Service");

    @ArchTest
    static final ArchRule repositories_live_in_repository_package = classes()
            .that().areAssignableTo("org.springframework.data.repository.Repository")
            .or().areAnnotatedWith(org.springframework.stereotype.Repository.class)
            .should().resideInAPackage(REPOSITORY)
            .andShould().haveSimpleNameEndingWith("Repository")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule persistent_classes_live_in_entity_package = classes()
            .that().areAnnotatedWith(Table.class)
            .should().resideInAPackage(ENTITY)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule no_field_injection = GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
    @ArchTest
    static final ArchRule no_standard_streams = GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
    @ArchTest
    static final ArchRule no_generic_exceptions = GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;
    @ArchTest
    static final ArchRule no_java_util_logging = GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
}
