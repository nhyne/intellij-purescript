package org.purescript.file

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.*
import com.intellij.util.io.EnumeratorStringDescriptor
import org.purescript.psi.PSForeignDataDeclaration
import org.purescript.psi.PSModule
import org.purescript.psi.data.PSDataDeclaration
import org.purescript.psi.exports.PSExportedData
import org.purescript.psi.exports.PSExportedModule
import org.purescript.psi.newtype.PSNewTypeDeclaration
import org.purescript.psi.typesynonym.PSTypeSynonymDeclaration

/**
 * An index on what type declarations every module exports.
 * The index contains the following declarations:
 *  - [PSTypeSynonymDeclaration]
 *  - [PSDataDeclaration]
 *  - [PSNewTypeDeclaration]
 *  - [PSForeignDataDeclaration]
 */
class ExportedTypesIndex : ScalarIndexExtension<String>(), DataIndexer<String, Void?, FileContent> {

    override fun map(inputData: FileContent): Map<String, Void?> {
        val file = inputData.psiFile as? PSFile ?: return emptyMap()
        val module = file.module ?: return emptyMap()
        val exportList = module.exportList
        return if (exportList == null) {
            declarations(module)
        } else {
            val explicitlyExported = exportList.exportedItems
                .filterIsInstance<PSExportedData>()
                .associate { it.name to null }
            if (module.exportsSelf) {
                explicitlyExported + declarations(module)
            } else {
                explicitlyExported
            }
        }
    }

    private fun declarations(module: PSModule): Map<String, Nothing?> {
        val exportedNames = mutableSetOf<String>()
        module.typeSynonymDeclarations.mapTo(exportedNames) { it.name }
        module.dataDeclarations.mapTo(exportedNames) { it.name }
        module.newTypeDeclarations.mapTo(exportedNames) { it.name }
        module.foreignDataDeclarations.mapTo(exportedNames) { it.name }
        return exportedNames.associateWith { null }
    }

    override fun getName(): ID<String, Void?> = NAME

    override fun getIndexer() = this

    override fun getKeyDescriptor(): EnumeratorStringDescriptor =
        EnumeratorStringDescriptor.INSTANCE

    override fun getVersion() = 2

    override fun getInputFilter() =
        DefaultFileTypeSpecificInputFilter(PSFileType.INSTANCE)

    override fun dependsOnFileContent() = true

    companion object {
        val NAME =
            ID.create<String, Void?>("org.purescript.file.ExportedTypesIndex")

        fun filesExportingType(
            project: Project,
            typeName: String
        ): List<PSFile> =
            ReadAction.compute<List<PSFile>, Throwable> {
                val allScope = GlobalSearchScope.allScope(project)
                val files = FileBasedIndex
                    .getInstance()
                    .getContainingFiles(NAME, typeName, allScope)
                files
                    .map { PsiManager.getInstance(project).findFile(it) }
                    .filterIsInstance(PSFile::class.java)
            }
    }

}
