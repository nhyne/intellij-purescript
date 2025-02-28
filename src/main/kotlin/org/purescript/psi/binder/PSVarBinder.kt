package org.purescript.psi.binder

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import org.purescript.psi.PSPsiElement
import org.purescript.psi.PSPsiFactory
import org.purescript.psi.name.PSIdentifier

class PSVarBinder(node: ASTNode) :
    PSPsiElement(node), PsiNameIdentifierOwner {

    override fun getName(): String = nameIdentifier.name

    override fun setName(name: String): PsiElement? {
        val newName =
            PSPsiFactory(project).createIdentifier(name) ?: return null
        this.nameIdentifier.replace(newName)
        return this
    }

    override fun getNameIdentifier(): PSIdentifier {
        return findChildByClass(PSIdentifier::class.java)!!
    }

    override fun getUseScope(): SearchScope {
        return LocalSearchScope(containingFile)
    }
}