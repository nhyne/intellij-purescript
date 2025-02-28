package org.purescript.parser

import com.intellij.psi.tree.IElementType

class ParsecRef(private val init: Parsec.() -> Parsec) : Parsec() {
    private val ref: Parsec by lazy { this.init() }
    override fun parse(context: ParserContext): ParserInfo = ref.parse(context)
    public override fun calcName() = ref.name!!
    override fun calcExpectedName() = ref.expectedName!!
    override fun canStartWith(type: IElementType) = ref.canStartWith(type)
    public override fun calcCanBeEmpty() = ref.canBeEmpty()
}