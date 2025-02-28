package org.purescript.keyword

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class KeywordCompletionContributorTest : BasePlatformTestCase() {
    fun `test do`() {
        val src = """
        module Main where
        dof = 1
        main = d{-caret-}
        """.trimIndent()
        val replace = src.replace("{-caret-}", "<caret>")
        myFixture.configureByText("Main.purs", replace)
        myFixture.testCompletionVariants("Main.purs", "do", "dof", "ado")
    }
    
    fun `test ado`() {
        val src = """
        module Main where
        adof = 1
        main = ad{-caret-}
        """.trimIndent()
        val replace = src.replace("{-caret-}", "<caret>")
        myFixture.configureByText("Main.purs", replace)
        myFixture.testCompletionVariants("Main.purs", "ado", "adof")
    }

}