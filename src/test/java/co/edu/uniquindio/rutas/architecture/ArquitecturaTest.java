package co.edu.uniquindio.rutas.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Hace cumplir la ARQUITECTURA EN 3 CAPAS y la IMPLEMENTACIÓN PROPIA (AGENTS.md).
 *
 * <pre>
 *   controller / cli  ->  service / algorithm / model  ->  repository
 *   (Constantes y RutasApplication: transversales)
 * </pre>
 * Si una regla falla, NO se modifica esta prueba: se mueve o corrige el código.
 */
@AnalyzeClasses(packages = "co.edu.uniquindio.rutas",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    private static final String CONTROLADORES = "..rutas.controller..";
    private static final String CLI = "..rutas.cli..";
    private static final String SERVICIOS = "..rutas.service..";
    private static final String DATOS = "..rutas.repository..";
    private static final String ALGORITMOS = "..rutas.algorithm..";
    private static final String MODELO = "..rutas.model..";

    @ArchTest
    static final ArchRule capas = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Presentacion").definedBy(CONTROLADORES, CLI)
            .layer("Logica").definedBy(SERVICIOS, ALGORITMOS, MODELO)
            .layer("Datos").definedBy(DATOS)
            .whereLayer("Presentacion").mayNotBeAccessedByAnyLayer()
            .whereLayer("Logica").mayOnlyBeAccessedByLayers("Presentacion")
            .whereLayer("Datos").mayOnlyBeAccessedByLayers("Logica");

    /** Implementación propia: los algoritmos solo usan el JDK básico y el modelo. */
    @ArchTest
    static final ArchRule algoritmosPuros = classes().that().resideInAPackage(ALGORITMOS)
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("java.lang..", "java.util..", MODELO, ALGORITMOS)
            .because("los algoritmos son de implementación propia y no dependen de librerías, "
                    + "de la capa de datos ni de los servicios (sección 3 del enunciado)");

    @ArchTest
    static final ArchRule modeloPuro = classes().that().resideInAPackage(MODELO)
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("java.lang..", "java.util..", "java.io..", MODELO)
            .because("el modelo de dominio no depende de Spring ni de la capa de datos");

    @ArchTest
    static final ArchRule algoritmosSinSpring = noClasses().that()
            .resideInAnyPackage(ALGORITMOS, MODELO)
            .should().dependOnClassesThat().resideInAPackage("org.springframework..");

    /** Solo la presentación imprime; la JVM hija de medición usa stdout como canal. */
    @ArchTest
    static final ArchRule logicaNoImprime = noClasses().that()
            .resideInAnyPackage(SERVICIOS, ALGORITMOS, MODELO, DATOS)
            .and().doNotHaveSimpleName("ProcesoAislado")
            .should().accessField(System.class, "out")
            .because("imprimir es responsabilidad de la presentación y contamina las mediciones");

    @ArchTest
    static final ArchRule datosNoConoceSpringWeb = noClasses().that()
            .resideInAnyPackage(SERVICIOS, ALGORITMOS, MODELO, DATOS)
            .should().dependOnClassesThat().resideInAPackage("org.springframework.web..");
}
