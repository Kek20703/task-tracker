package com.tasktracker.backend;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;
import jakarta.persistence.Entity;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Архитектурные правила сервиса. Тесты проверяют не поведение, а структуру кода:
 * если правило нарушено, сборка падает.
 */
@AnalyzeClasses(packages = "com.tasktracker.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String ROOT = "com.tasktracker.backend";

    // --- Слои ---

    @ArchTest
    static final ArchRule layers_are_respected = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Controller").definedBy(ROOT + ".controller..")
            .layer("Service").definedBy(ROOT + ".service..")
            .layer("Repository").definedBy(ROOT + ".repository..")
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
            .because("запрос идёт строго сверху вниз: контроллер -> сервис -> репозиторий");

    @ArchTest
    static final ArchRule controllers_do_not_touch_entities_or_repositories = noClasses()
            .that().resideInAPackage(ROOT + ".controller..")
            .should().dependOnClassesThat().resideInAnyPackage(ROOT + ".entity..", ROOT + ".repository..")
            .because("наружу отдаются только DTO, JPA-сущности не должны утекать в REST API");

    @ArchTest
    static final ArchRule entities_are_independent = noClasses()
            .that().resideInAPackage(ROOT + ".entity..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    ROOT + ".controller..", ROOT + ".service..", ROOT + ".repository..",
                    ROOT + ".dto..", ROOT + ".config..", ROOT + ".exception..")
            .because("сущности — нижний уровень, они ничего не знают об остальном приложении");

    @ArchTest
    static final ArchRule dto_are_plain_data = noClasses()
            .that().resideInAPackage(ROOT + ".dto..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    ROOT + ".controller..", ROOT + ".service..", ROOT + ".repository..", ROOT + ".entity..")
            .because("DTO — просто данные, преобразование из сущностей делает сервис");

    @ArchTest
    static final ArchRule no_package_cycles = slices()
            .matching(ROOT + ".(*)..")
            .should().beFreeOfCycles();

    // --- Расположение и именование ---

    @ArchTest
    static final ArchRule rest_controllers_live_in_controller_package = classes()
            .that().areAnnotatedWith(RestController.class)
            .should().resideInAPackage(ROOT + ".controller..")
            .andShould().haveSimpleNameEndingWith("Controller");

    @ArchTest
    static final ArchRule services_live_in_service_package = classes()
            .that().areAnnotatedWith(Service.class)
            .should().resideInAPackage(ROOT + ".service..")
            .andShould().haveSimpleNameEndingWith("Service");

    @ArchTest
    static final ArchRule repositories_live_in_repository_package = classes()
            .that().areAssignableTo(Repository.class)
            .should().resideInAPackage(ROOT + ".repository..")
            .andShould().haveSimpleNameEndingWith("Repository");

    @ArchTest
    static final ArchRule entities_live_in_entity_package = classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage(ROOT + ".entity..");

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
