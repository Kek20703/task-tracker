package com.tasktracker.backend;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Архитектурные правила сервиса. Тесты проверяют не поведение, а структуру кода:
 * если правило нарушено, сборка падает.
 */
@AnalyzeClasses(packages = "com.tasktracker.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String ROOT = "com.tasktracker.backend";

    private static final String CONTROLLER = ROOT + ".controller..";
    private static final String SERVICE = ROOT + ".service..";
    private static final String REPOSITORY = ROOT + ".repository..";
    private static final String ENTITY = ROOT + ".entity..";
    private static final String DTO = ROOT + ".dto..";
    private static final String EXCEPTION = ROOT + ".exception..";
    private static final String VALIDATION = ROOT + ".validation..";
    private static final String CONFIG = ROOT + ".config..";
    private static final String SECURITY = ROOT + ".security..";

    // --- Раскладка по пакетам ---

    @ArchTest
    static final ArchRule every_class_lives_in_a_known_package = classes()
            .that().doNotHaveFullyQualifiedName(ROOT + ".TaskTrackerBackendApplication")
            .should().resideInAnyPackage(
                    CONTROLLER, SERVICE, REPOSITORY, ENTITY, DTO, EXCEPTION, VALIDATION, CONFIG, SECURITY)
            .because("правила ниже написаны для этих пакетов; класс в другом месте остался бы без проверок");

    // --- Слои: запрос идёт сверху вниз, контроллер -> сервис -> репозиторий ---

    @ArchTest
    static final ArchRule only_services_use_repositories = noClasses()
            .that().resideOutsideOfPackages(SERVICE, REPOSITORY)
            .should().dependOnClassesThat().resideInAPackage(REPOSITORY)
            .because("к данным ходят только через сервис — в том числе из config и security");

    @ArchTest
    static final ArchRule nobody_depends_on_controllers = noClasses()
            .that().resideOutsideOfPackage(CONTROLLER)
            .should().dependOnClassesThat().resideInAPackage(CONTROLLER)
            .because("контроллеры — верхний слой, их вызывает только Spring MVC");

    @ArchTest
    static final ArchRule repositories_do_not_depend_on_upper_layers = noClasses()
            .that().resideInAPackage(REPOSITORY)
            .should().dependOnClassesThat().resideInAnyPackage(CONTROLLER, SERVICE, DTO, CONFIG, SECURITY)
            .because("репозиторий знает только о сущностях");

    @ArchTest
    static final ArchRule only_services_and_repositories_use_entities = noClasses()
            .that().resideOutsideOfPackages(SERVICE, REPOSITORY, ENTITY)
            .should().dependOnClassesThat().resideInAPackage(ENTITY)
            .because("JPA-сущности не выходят из сервисного слоя: контроллеры, security и config получают DTO");

    @ArchTest
    static final ArchRule entities_are_independent = noClasses()
            .that().resideInAPackage(ENTITY)
            .should().dependOnClassesThat().resideInAnyPackage(
                    CONTROLLER, SERVICE, REPOSITORY, DTO, EXCEPTION, VALIDATION, CONFIG, SECURITY)
            .because("сущности — нижний уровень, они ничего не знают об остальном приложении");

    @ArchTest
    static final ArchRule dto_are_plain_data = noClasses()
            .that().resideInAPackage(DTO)
            .should().dependOnClassesThat().resideInAnyPackage(
                    CONTROLLER, SERVICE, REPOSITORY, ENTITY, EXCEPTION, CONFIG, SECURITY)
            .because("DTO — просто данные, преобразование из сущностей делает сервис");

    @ArchTest
    static final ArchRule no_package_cycles = slices()
            .matching(ROOT + ".(*)..")
            .should().beFreeOfCycles();

    // --- Расположение и именование ---

    @ArchTest
    static final ArchRule controllers_live_in_controller_package = classes()
            .that().areAnnotatedWith(RestController.class)
            .or().areAnnotatedWith(Controller.class)
            .should().resideInAPackage(CONTROLLER)
            .andShould().haveSimpleNameEndingWith("Controller");

    @ArchTest
    static final ArchRule services_live_in_service_package = classes()
            .that().areAnnotatedWith(Service.class)
            .should().resideInAPackage(SERVICE)
            .andShould().haveSimpleNameEndingWith("Service");

    @ArchTest
    static final ArchRule repositories_live_in_repository_package = classes()
            .that().areAssignableTo(org.springframework.data.repository.Repository.class)
            .or().areAnnotatedWith(org.springframework.stereotype.Repository.class)
            .should().resideInAPackage(REPOSITORY)
            .andShould().haveSimpleNameEndingWith("Repository");

    @ArchTest
    static final ArchRule persistent_classes_live_in_entity_package = classes()
            .that().areAnnotatedWith(Entity.class)
            .or().areAnnotatedWith(Embeddable.class)
            .or().areAnnotatedWith(MappedSuperclass.class)
            .should().resideInAPackage(ENTITY);

    // --- Гигиена кода ---

    @ArchTest
    static final ArchRule no_field_injection = GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION
            .because("зависимости передаются через конструктор");

    @ArchTest
    static final ArchRule no_standard_streams = GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
            .because("для вывода используется логгер");

    @ArchTest
    static final ArchRule no_generic_exceptions = GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS
            .because("у каждой ошибки свой тип, по которому выбирается HTTP-код ответа");

    @ArchTest
    static final ArchRule no_java_util_logging = GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
}
