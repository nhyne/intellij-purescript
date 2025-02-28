package org.purescript.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.util.containers.addIfNotNull
import org.purescript.features.DocCommentOwner
import org.purescript.parser.WHERE
import org.purescript.psi.classes.PSClassDeclaration
import org.purescript.psi.classes.PSClassMember
import org.purescript.psi.data.PSDataConstructor
import org.purescript.psi.data.PSDataDeclaration
import org.purescript.psi.declaration.PSFixityDeclaration
import org.purescript.psi.declaration.PSValueDeclaration
import org.purescript.psi.exports.*
import org.purescript.psi.imports.PSImportDeclaration
import org.purescript.psi.name.PSModuleName
import org.purescript.psi.newtype.PSNewTypeConstructor
import org.purescript.psi.newtype.PSNewTypeDeclaration
import org.purescript.psi.typesynonym.PSTypeSynonymDeclaration
import kotlin.reflect.KProperty1


class PSModule(node: ASTNode) :
    PSPsiElement(node),
    PsiNameIdentifierOwner,
    DocCommentOwner {
    override fun getName(): String {
        return nameIdentifier.name
    }

    override fun setName(name: String): PsiElement? {
        val properName = PSPsiFactory(project).createModuleName(name)
            ?: return null
        nameIdentifier.replace(properName)
        return this
    }

    override fun getNameIdentifier(): PSModuleName {
        return findNotNullChildByClass(PSModuleName::class.java)
    }

    override fun getTextOffset(): Int = nameIdentifier.textOffset

    fun getImportDeclarationByName(name: String): PSImportDeclaration? {
        return importDeclarations
            .asSequence()
            .find { (it.name ?: "") == name }
    }

    val fixityDeclarations: Array<PSFixityDeclaration> get() =
        findChildrenByClass(PSFixityDeclaration::class.java)

    /**
     * @return the [PSFixityDeclaration] that this module exports,
     * both directly and through re-exported modules
     */
    val exportedFixityDeclarations: List<PSFixityDeclaration>
        get() = getExportedDeclarations(
            fixityDeclarations,
            PSImportDeclaration::importedFixityDeclarations,
            PSExportedOperator::class.java
        )

    /**
     * @return the where keyword in the module header
     */
    val whereKeyword: PsiElement
        get() = findNotNullChildByType(WHERE)

    /**
     * Helper method for retrieving various types of exported declarations.
     *
     * @param declarations The declarations of the wanted type in this module
     * @param importedDeclarationProperty The property for the imported declarations in an [PSImportDeclaration]
     * @param exportedItemClass The class of the [PSExportedItem] to use when filtering the results
     * @return the [Declaration] element that this module exports
     */
    private fun <Declaration : PsiNamedElement> getExportedDeclarations(
        declarations: Array<Declaration>,
        importedDeclarationProperty: KProperty1<PSImportDeclaration, List<Declaration>>,
        exportedItemClass: Class<out PSExportedItem>
    ): List<Declaration> {
        val explicitlyExportedItems = exportList?.exportedItems
            ?: return declarations.toList()

        val explicitlyNames = explicitlyExportedItems
            .filterIsInstance(exportedItemClass)
            .map { it.name }
            .toSet()

        val exportedDeclarations = mutableListOf<Declaration>()

        val exportsSelf = explicitlyExportedItems
            .filterIsInstance<PSExportedModule>()
            .any { it.name == name }

        declarations.filterTo(exportedDeclarations) {
            exportsSelf || it.name in explicitlyNames
        }

        explicitlyExportedItems.filterIsInstance<PSExportedModule>()
            .flatMap { it.importDeclarations }
            .flatMapTo(exportedDeclarations) { importedDeclarationProperty.get(it) }

        return exportedDeclarations
    }

    /**
     * If the export list is null, this module implicitly exports all its members.
     * @return the [PSExportList] in this module, if it exists
     */
    val exportList: PSExportList?
        get() = findChildByClass(PSExportList::class.java)

    /**
     * @return the [PSImportDeclaration] elements in this module
     */
    val importDeclarations: Array<PSImportDeclaration>
        get() =
            findChildrenByClass(PSImportDeclaration::class.java)

    /**
     * @return the [PSValueDeclaration] elements in this module
     */
    val valueDeclarations: Array<PSValueDeclaration>
        get() = findChildrenByClass(PSValueDeclaration::class.java)

    /**
     * @return the [PSForeignValueDeclaration] elements in this module
     */
    val foreignValueDeclarations: Array<PSForeignValueDeclaration>
        get() =
            findChildrenByClass(PSForeignValueDeclaration::class.java)

    /**
     * @return the [PSForeignDataDeclaration] elements in this module
     */
    val foreignDataDeclarations: Array<PSForeignDataDeclaration>
        get() =
            findChildrenByClass(PSForeignDataDeclaration::class.java)

    /**
     * @return the [PSNewTypeDeclaration] elements in this module
     */
    val newTypeDeclarations: Array<PSNewTypeDeclaration>
        get() =
            findChildrenByClass(PSNewTypeDeclaration::class.java)

    /**
     * @return the [PSNewTypeConstructor] elements in this module
     */
    val newTypeConstructors: List<PSNewTypeConstructor>
        get() =
            newTypeDeclarations.map { it.newTypeConstructor }

    /**
     * @return the [PSDataDeclaration] elements in this module
     */
    val dataDeclarations: Array<PSDataDeclaration>
        get() =
            findChildrenByClass(PSDataDeclaration::class.java)

    /**
     * @return the [PSDataConstructor] elements in this module
     */
    val dataConstructors: List<PSDataConstructor>
        get() =
            dataDeclarations.flatMap { it.dataConstructors.toList() }

    /**
     * @return the [PSTypeSynonymDeclaration] elements in this module
     */
    val typeSynonymDeclarations: Array<PSTypeSynonymDeclaration>
        get() =
            findChildrenByClass(PSTypeSynonymDeclaration::class.java)

    /**
     * @return the [PSClassDeclaration] elements in this module
     */
    val classDeclarations: Array<PSClassDeclaration>
        get() =
            findChildrenByClass(PSClassDeclaration::class.java)

    /**
     * @return the [PSValueDeclaration] that this module exports,
     * both directly and through re-exported modules
     */
    val exportedValueDeclarations: List<PSValueDeclaration>
        get() = getExportedDeclarations(
            valueDeclarations,
            PSImportDeclaration::importedValueDeclarations,
            PSExportedValue::class.java
        )

    /**
     * @return the [PSForeignValueDeclaration] elements that this module exports,
     * both directly and through re-exported modules
     */
    val exportedForeignValueDeclarations: List<PSForeignValueDeclaration>
        get() = getExportedDeclarations(
            foreignValueDeclarations,
            PSImportDeclaration::importedForeignValueDeclarations,
            PSExportedValue::class.java
        )

    /**
     * @return the [PSForeignDataDeclaration] elements that this module exports,
     * both directly and through re-exported modules
     */
    val exportedForeignDataDeclarations: List<PSForeignDataDeclaration>
        get() = getExportedDeclarations(
            foreignDataDeclarations,
            PSImportDeclaration::importedForeignDataDeclarations,
            PSExportedData::class.java
        )

    /**
     * @return the [PSNewTypeDeclaration] elements that this module exports,
     * both directly and through re-exported modules
     */
    val exportedNewTypeDeclarations: List<PSNewTypeDeclaration>
        get() = getExportedDeclarations(
            newTypeDeclarations,
            PSImportDeclaration::importedNewTypeDeclarations,
            PSExportedData::class.java
        )

    /**
     * @return the [PSNewTypeConstructor] elements that this module exports,
     * both directly and through re-exported modules
     */
    val exportedNewTypeConstructors: List<PSNewTypeConstructor>
        get() {
            val explicitlyExportedItems = exportList?.exportedItems
                ?: return newTypeConstructors

            val exportedNewTypeConstructors = mutableListOf<PSNewTypeConstructor>()

            for (exportedData in explicitlyExportedItems.filterIsInstance<PSExportedData>()) {
                if (exportedData.exportsAll) {
                    exportedNewTypeConstructors.addIfNotNull(exportedData.newTypeDeclaration?.newTypeConstructor)
                } else {
                    exportedData.dataMembers
                        .mapNotNull { it.reference.resolve() }
                        .filterIsInstanceTo(exportedNewTypeConstructors)
                }
            }

            explicitlyExportedItems.filterIsInstance<PSExportedModule>()
                .flatMap { it.importDeclarations }
                .flatMapTo(exportedNewTypeConstructors) { it.importedNewTypeConstructors }

            return exportedNewTypeConstructors
        }

    /**
     * @return the [PSDataDeclaration] elements that this module exports,
     * both directly and through re-exported modules
     */
    val exportedDataDeclarations: List<PSDataDeclaration>
        get() = getExportedDeclarations(
            dataDeclarations,
            PSImportDeclaration::importedDataDeclarations,
            PSExportedData::class.java
        )

    /**
     * @return the [PSDataConstructor] elements that this module exports,
     * both directly and through re-exported modules
     */
    val exportedDataConstructors: List<PSDataConstructor>
        get() {
            val explicitlyExportedItems = exportList?.exportedItems
                ?: return dataConstructors

            val exportedDataConstructors = mutableListOf<PSDataConstructor>()

            for (exportedData in explicitlyExportedItems.filterIsInstance<PSExportedData>()) {
                if (exportedData.exportsAll) {
                    exportedData.dataDeclaration?.dataConstructors
                        ?.mapTo(exportedDataConstructors) { it }
                } else {
                    exportedData.dataMembers
                        .mapNotNull { it.reference.resolve() }
                        .filterIsInstanceTo(exportedDataConstructors)
                }
            }

            explicitlyExportedItems.filterIsInstance<PSExportedModule>()
                .flatMap { it.importDeclarations }
                .flatMapTo(exportedDataConstructors) { it.importedDataConstructors }

            return exportedDataConstructors
        }

    /**
     * @return the [PSTypeSynonymDeclaration] elements that this module exports,
     * both directly and through re-exported modules
     */
    val exportedTypeSynonymDeclarations: List<PSTypeSynonymDeclaration>
        get() = getExportedDeclarations(
            typeSynonymDeclarations,
            PSImportDeclaration::importedTypeSynonymDeclarations,
            PSExportedData::class.java
        )

    /**
     * @return the [PSClassDeclaration] elements that this module exports,
     * both directly and through re-exported modules
     */
    val exportedClassDeclarations: List<PSClassDeclaration>
        get() = getExportedDeclarations(
            classDeclarations,
            PSImportDeclaration::importedClassDeclarations,
            PSExportedClass::class.java
        )

    /**
     * @return the [PSClassMember] elements that this module exports,
     * both directly and through re-exported modules
     */
    val exportedClassMembers: List<PSClassMember>
        get() = getExportedDeclarations(
            classDeclarations.flatMap { it.classMembers.asSequence() }.toTypedArray(),
            PSImportDeclaration::importedClassMembers,
            PSExportedValue::class.java
        )

    val reexportedModuleNames: List<String>
        get() =
            exportList?.exportedItems?.filterIsInstance(PSExportedModule::class.java)
                ?.map { it.name }
                ?.toList()
                ?: emptyList()

    val exportedNames: List<String>
        get() =
            exportList?.exportedItems
                ?.filter { it !is PSExportedModule }
                ?.map { it.text.trim() }
                ?.toList()
                ?: emptyList()

    override val docComments: List<PsiComment>
        get() = getDocComments()

    fun addImportDeclaration(importDeclaration: PSImportDeclaration) {
        val lastImportDeclaration = importDeclarations.lastOrNull()
        val insertPosition = lastImportDeclaration ?: whereKeyword
        val newLine = PSPsiFactory(project).createNewLine()
        addAfter(importDeclaration, insertPosition)
        addAfter(newLine, insertPosition)
        if (lastImportDeclaration == null) {
            addAfter(newLine, insertPosition)
        }
    }
    val exportsSelf:Boolean get() =
        exportList?.exportedItems
            ?.filterIsInstance<PSExportedModule>()
            ?.any { it.name == module?.name }
            ?: true
}
