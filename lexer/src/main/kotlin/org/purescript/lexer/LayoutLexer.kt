package org.purescript.lexer

import com.intellij.lexer.DelegateLexer
import com.intellij.lexer.Lexer
import com.intellij.psi.tree.IElementType
import org.purescript.lexer.token.SourcePos
import org.purescript.lexer.token.SourceRange
import org.purescript.lexer.token.SourceToken
import org.purescript.parser.*
import org.purescript.psi.PSElementType

data class LayoutStack(
    val sourcePos: SourcePos,
    val layoutDelimiter: LayoutDelimiter,
    val tail: LayoutStack?
)

data class LayoutState(
    val stack: LayoutStack?,
    val acc: List<Pair<SuperToken, LayoutStack?>>
)

data class Lexeme(
    val token: SourceToken,
    val trailingWhitespace: List<SourceToken>
) {
    val tokens get () = listOf(token) + trailingWhitespace
    val range get() = SourceRange(
        token.range.start,
        trailingWhitespace.lastOrNull()?.range?.end ?: token.range.end
    )
    val value = token.value
}

data class SuperToken(
    val qualified: List<Lexeme>,
    val token: Lexeme,
) {
    val tokens get() = qualified.flatMap { it.tokens} + token.tokens
    val range get() = SourceRange(
        qualified.firstOrNull()?.range?.start ?: token.range.start,
        token.range.end
    )
    val value = token.value
}

enum class LayoutDelimiter {
    Root,
    TopDecl,
    TopDeclHead,
    DeclGuard,
    Case,
    CaseBinders,
    CaseGuard,
    LambdaBinders,
    Paren,
    Brace,
    Square,
    If,
    Then,
    Property,
    Forall,
    Tick,
    Let,
    LetStmt,
    Where,
    Of,
    Do,
    Ado,
}

fun isIndented(lyt: LayoutDelimiter): Boolean = when (lyt) {
    LayoutDelimiter.Let -> true
    LayoutDelimiter.LetStmt -> true
    LayoutDelimiter.Where -> true
    LayoutDelimiter.Of -> true
    LayoutDelimiter.Do -> true
    LayoutDelimiter.Ado -> true
    else -> false
}

fun isTopDecl(tokPos: SourcePos, stk: LayoutStack?): Boolean = when {
    stk?.tail == null || stk.tail.tail != null -> {
        false
    }
    stk.tail.layoutDelimiter != LayoutDelimiter.Root -> {
        false
    }
    stk.layoutDelimiter != LayoutDelimiter.Where -> {
        false
    }
    else -> {
        tokPos.column == stk.sourcePos.column
    }
}
fun toSuper(token: Lexeme): SuperToken = SuperToken(emptyList(), token)
fun toLexeme(token: SourceToken): Lexeme = Lexeme(token, emptyList())

fun lytToken(pos: SourcePos, value: PSElementType): SuperToken =
    toSuper(toLexeme(SourceToken(
    range = SourceRange(pos, pos),
    value = value
    )))

fun <A> snoc(acc: List<A>, pair: A): List<A> {
    val acc2 = acc.toMutableList()
    acc2 += pair
    return acc2
}

fun collapse(
    tokPos: SourcePos,
    p: (SourcePos, SourcePos, LayoutDelimiter) -> Boolean,
    state: LayoutState
): LayoutState {
    val (stack, acc) = state
    if (stack != null) {
        val (lytPos, lyt, tail) = stack
        return if (p(tokPos, lytPos, lyt)) {
            val nextAcc = if (isIndented(lyt)) {
                val pair = lytToken(tokPos, LAYOUT_END) to tail
                snoc(acc, pair)
            } else {
                acc
            }
            collapse(tokPos, p, LayoutState(tail, nextAcc))
        } else {
            state
        }
    } else {
        return state
    }
}
fun offsideP(tokPos:SourcePos, lytPos: SourcePos, lyt: LayoutDelimiter): Boolean {
    return isIndented(lyt) && tokPos.column < lytPos.column
}


fun sepP(tokPos: SourcePos, lytPos: SourcePos): Boolean =
    tokPos.column == lytPos.column && tokPos.line != lytPos.line

fun insertToken(token: SuperToken, state: LayoutState): LayoutState {
    val (stk, acc) = state
    val acc2 = acc.toMutableList()
    acc2 += token to stk
    return LayoutState(stk, acc2)
}

fun pushStack(
    lytPos: SourcePos,
    lyt: LayoutDelimiter,
    state: LayoutState
): LayoutState =
    LayoutState(LayoutStack(lytPos, lyt, state.stack), state.acc)

fun identSepP(tokPos: SourcePos, lytPos: SourcePos, lyt: LayoutDelimiter): Boolean =
    isIndented(lyt) && sepP(tokPos, lytPos)

fun insertSep(tokPos: SourcePos, state: LayoutState): LayoutState {
    val (stk, acc) = state
    val (lytPos, lyt, tail) = stk ?: return state
    val sepTok = lytToken(tokPos, LAYOUT_SEP)
    return when {
        LayoutDelimiter.TopDecl == lyt && sepP(tokPos, lytPos) ->
            insertToken(sepTok, LayoutState(tail, acc))
        LayoutDelimiter.TopDeclHead == lyt && sepP(tokPos, lytPos) ->
            insertToken(sepTok, LayoutState(tail, acc))
        identSepP(tokPos, lytPos, lyt) -> when (lyt) {
            LayoutDelimiter.Of -> pushStack(
                tokPos,
                LayoutDelimiter.CaseBinders,
                insertToken(sepTok, state)
            )
            else -> insertToken(sepTok, state)
        }
        else -> state
    }
}


fun popStack(
    state: LayoutState,
    p: (LayoutDelimiter) -> Boolean
): LayoutState {
    val lyt = state.stack?.layoutDelimiter
    return if (lyt != null && p(lyt)) {
        LayoutState(state.stack.tail, state.acc)
    } else {
        state
    }
}

fun find(
    stack: LayoutStack?,
    function: (LayoutStack) -> Boolean
): LayoutStack? {
    if (stack == null) {
        return null
    } else if (function(stack)) {
        return stack
    } else {
        return find(stack.tail, function)
    }
}


fun insertEnd(tokPos: SourcePos, state: LayoutState): LayoutState =
    insertToken(lytToken(tokPos, LAYOUT_END), state)


fun insertDefault(
    src: SuperToken, tokPos: SourcePos, state: LayoutState
): LayoutState {
    return insertToken(
        src,
        insertSep(tokPos, collapse(tokPos, ::offsideP, state))
    )
}

fun insertKwProperty(
    src: SuperToken,
    tokPos: SourcePos,
    k: (LayoutState) -> LayoutState,
    state: LayoutState
): LayoutState {
    val state2 = insertDefault(src, tokPos, state)
    return if (state.stack?.layoutDelimiter == LayoutDelimiter.Property) {
        LayoutState(state2.stack?.tail, state2.acc)
    } else {
        k(state2)
    }
}

fun insertStart(
    nextPos: SourcePos, lyt: LayoutDelimiter, state: LayoutState
): LayoutState {
    val stk = state.stack
    val (pos, _, _) = find(stk) { stack: LayoutStack ->
        isIndented(stack.layoutDelimiter)
    } ?: return insertToken(
        lytToken(nextPos, LAYOUT_START),
        pushStack(nextPos, lyt, state)
    )
    return if (nextPos.column <= pos.column) {
        state
    } else {
        insertToken(
            lytToken(nextPos, LAYOUT_START),
            pushStack(nextPos, lyt, state)
        )
    }
}

fun offsideEndP(
    tokPos: SourcePos, lytPos: SourcePos, lyt: LayoutDelimiter
): Boolean {
    return isIndented(lyt) && tokPos.column <= lytPos.column
}

fun indentedP(
    tokPos: SourcePos, ignore: SourcePos, lyt: LayoutDelimiter
): Boolean =
    isIndented(lyt)

fun insertLayout(
    src: SuperToken,
    nextPos: SourcePos,
    stack: LayoutStack?
): LayoutState {
    val tokenValue = src.value
    val tokPos = src.range.start
    val state = LayoutState(stack, emptyList())
    val (stk, acc) = state

    return when (tokenValue) {
        DATA -> {
            val state2 = insertDefault(src, tokPos, state)
            if (isTopDecl(tokPos, state2.stack)) {
                pushStack(tokPos, LayoutDelimiter.TopDecl, state2)
            } else {
                popStack(state2) { it == LayoutDelimiter.Property }
            }
        }

        CLASS -> {
            val state2 = insertDefault(src, tokPos, state)
            if (isTopDecl(tokPos, state2.stack)) {
                pushStack(tokPos, LayoutDelimiter.TopDeclHead, state2)
            } else {
                popStack(state2) { it == LayoutDelimiter.Property }
            }
        }

        WHERE -> {
            fun whereP(
                tokPos: SourcePos,
                lytPos: SourcePos,
                lyt: LayoutDelimiter
            ): Boolean =
                if (lyt == LayoutDelimiter.Do) true
                else offsideEndP(tokPos, lytPos, lyt)

            when (stk?.layoutDelimiter) {
                LayoutDelimiter.TopDeclHead ->
                    LayoutState(stk.tail, acc)
                        .let { insertToken(src, it) }
                        .let {
                            insertStart(
                                nextPos,
                                LayoutDelimiter.Where,
                                it
                            )
                        }
                LayoutDelimiter.Property ->
                    insertToken(src, LayoutState(stk.tail, acc))
                else ->
                    state
                        .let { collapse(tokPos, ::whereP, it) }
                        .let { insertToken(src, it) }
                        .let {
                            insertStart(
                                nextPos,
                                LayoutDelimiter.Where,
                                it
                            )
                        }
            }
        }

        IN -> {
            fun inP(
                tokPos: SourcePos,
                ignore: SourcePos,
                lyt: LayoutDelimiter
            ): Boolean =
                when (lyt) {
                    LayoutDelimiter.Let -> false
                    LayoutDelimiter.Ado -> false
                    else -> isIndented(lyt)
                }

            val (stk, acc2) = collapse(tokPos, ::inP, state)
            val (_, lyt, stk2) = stk ?: return state
                .let { insertDefault(src, tokPos, it) }
                .let { popStack(it) { it == LayoutDelimiter.Property } }
            if (lyt == LayoutDelimiter.LetStmt && stk2?.layoutDelimiter == LayoutDelimiter.Ado) {
                return LayoutState(stk2.tail, acc2)
                    .let { insertEnd(tokPos, it) }
                    .let { insertEnd(tokPos, it) }
                    .let { insertToken(src, it) }
            } else if (isIndented(lyt)) {
                return LayoutState(stk2, acc2)
                    .let { insertEnd(tokPos, it) }
                    .let { insertToken(src, it) }
            } else {
                return state
                    .let { insertDefault(src, tokPos, it) }
                    .let { popStack(it) { it == LayoutDelimiter.Property } }
            }
        }

        LET -> {
            fun next(state: LayoutState): LayoutState {
                val (p, lyt, _) = state.stack ?: return insertStart(
                    nextPos,
                    LayoutDelimiter.Let,
                    state
                )
                return when {
                    lyt == LayoutDelimiter.Do && p.column == tokPos.column ->
                        insertStart(nextPos, LayoutDelimiter.LetStmt, state)
                    lyt == LayoutDelimiter.Ado && p.column == tokPos.column ->
                        insertStart(nextPos, LayoutDelimiter.LetStmt, state)
                    else -> insertStart(nextPos, LayoutDelimiter.Let, state)
                }
            }

            return insertKwProperty(src, tokPos, ::next, state)
        }

        DO ->
            insertKwProperty(
                src,
                tokPos,
                { insertStart(nextPos, LayoutDelimiter.Do, it) },
                state
            )

        ADO ->
            insertKwProperty(
                src,
                tokPos,
                { insertStart(nextPos, LayoutDelimiter.Ado, it) },
                state
            )

        CASE ->
            insertKwProperty(
                src,
                tokPos,
                { pushStack(tokPos, LayoutDelimiter.Case, it) },
                state
            )

        OF -> {
            val state2 = collapse(tokPos, ::indentedP, state)
            return if (state2.stack?.layoutDelimiter == LayoutDelimiter.Case) {
                LayoutState(state2.stack.tail, state2.acc)
                    .let { insertToken(src, it) }
                    .let { insertStart(nextPos, LayoutDelimiter.Of, it) }
                    .let {
                        pushStack(
                            nextPos,
                            LayoutDelimiter.CaseBinders,
                            it
                        )
                    }
            } else {
                state2
                    .let { insertDefault(src, tokPos, it) }
                    .let { popStack(it) { it == LayoutDelimiter.Property } }
            }
        }

        IF ->
            insertKwProperty(
                src,
                tokPos,
                { pushStack(tokPos, LayoutDelimiter.If, it) },
                state
            )

        THEN -> {
            val state2 = collapse(tokPos, ::indentedP, state)
            if (state2.stack?.layoutDelimiter == LayoutDelimiter.If) {
                LayoutState(state2.stack.tail, state2.acc)
                    .let { insertToken(src, it) }
                    .let { pushStack(tokPos, LayoutDelimiter.Then, it) }
            } else {
                insertDefault(src, tokPos, state)
                    .let { popStack(it) { it == LayoutDelimiter.Property } }
            }
        }

        ELSE -> {
            val state2 = collapse(tokPos, ::indentedP, state)
            if (state2.stack?.layoutDelimiter == LayoutDelimiter.Then) {
                LayoutState(state2.stack.tail, state2.acc)
                    .let { insertToken(src, it) }
            } else {
                val state3 = collapse(tokPos, ::offsideP, state)
                if (isTopDecl(tokPos, state3.stack)) {
                    insertToken(src, state3)
                } else {
                    insertSep(tokPos, state3)
                        .let { insertToken(src, it) }
                        .let { popStack(it) { it == LayoutDelimiter.Property } }
                }
            }
        }

        FORALL ->
            insertKwProperty(
                src,
                tokPos,
                { pushStack(tokPos, LayoutDelimiter.Forall, it) },
                state
            )

        BACKSLASH -> state
            .let { insertDefault(src, tokPos, it) }
            .let { pushStack(tokPos, LayoutDelimiter.LambdaBinders, it) }

        ARROW -> {
            fun arrowP(
                tokPos: SourcePos,
                lytPos: SourcePos,
                lyt: LayoutDelimiter
            ): Boolean =
                when (lyt) {
                    LayoutDelimiter.Do -> true
                    LayoutDelimiter.Of -> false
                    else -> offsideEndP(tokPos, lytPos, lyt)
                }

            fun guardP(lyt: LayoutDelimiter): Boolean = when (lyt) {
                LayoutDelimiter.CaseBinders -> true
                LayoutDelimiter.CaseGuard -> true
                LayoutDelimiter.LambdaBinders -> true
                else -> false
            }
            state
                .let { collapse(tokPos, ::arrowP, it) }
                .let { popStack(it, ::guardP) }
                .let { insertToken(src, it) }
        }

        EQ -> {
            fun equalsP(
                tokPos: SourcePos,
                ignore: SourcePos,
                lyt: LayoutDelimiter
            ): Boolean =
                when (lyt) {
                    LayoutDelimiter.Where -> true
                    LayoutDelimiter.Let -> true
                    LayoutDelimiter.LetStmt -> true
                    else -> false
                }
            val (stk2, acc2) = collapse(tokPos, ::equalsP, state)
            when (stk2?.layoutDelimiter) {
                LayoutDelimiter.DeclGuard -> LayoutState(stk2.tail, acc2)
                    .let { insertToken(src, it) }
                else -> insertDefault(src, tokPos, state)
            }
        }

        PIPE -> {
            val state2 = collapse(tokPos, ::offsideEndP, state)
            val (stk, _) = state2
            when (stk?.layoutDelimiter) {
                LayoutDelimiter.Of -> state2
                    .let {
                        pushStack(tokPos, LayoutDelimiter.CaseGuard, it)
                    }
                    .let { insertToken(src, it) }
                LayoutDelimiter.Let -> state2
                    .let {
                        pushStack(tokPos, LayoutDelimiter.DeclGuard, it)
                    }
                    .let { insertToken(src, it) }
                LayoutDelimiter.LetStmt -> state2
                    .let {
                        pushStack(tokPos, LayoutDelimiter.DeclGuard, it)
                    }
                    .let { insertToken(src, it) }
                LayoutDelimiter.Where -> state2
                    .let {
                        pushStack(tokPos, LayoutDelimiter.DeclGuard, it)
                    }
                    .let { insertToken(src, it) }
                else -> state.let { insertDefault(src, tokPos, it) }
            }
        }

        TICK -> {
            val state2 = collapse(tokPos, ::indentedP, state)
            if (state2.stack?.layoutDelimiter == LayoutDelimiter.Tick) {
                LayoutState(state2.stack.tail, state2.acc)
                    .let { insertToken(src, it) }
            } else {
                state
                    .let { insertDefault(src, tokPos, it) }
                    .let { pushStack(tokPos, LayoutDelimiter.Tick, it) }
            }
        }

        COMMA -> {
            val state2 = collapse(tokPos, ::indentedP, state)
            if (state2.stack?.layoutDelimiter == LayoutDelimiter.Brace) {
                state2
                    .let { insertToken(src, it) }
                    .let { pushStack(tokPos, LayoutDelimiter.Property, it) }
            } else {
                state2
                    .let { insertToken(src, it) }
            }
        }

        DOT -> {
            val state2 = insertDefault(src, tokPos, state)
            if (state2.stack?.layoutDelimiter == LayoutDelimiter.Forall) {
                LayoutState(state2.stack.tail, state2.acc)
            } else {
                state2
                    .let { pushStack(tokPos, LayoutDelimiter.Property, it) }
            }
        }

        LPAREN -> state
            .let { insertDefault(src, tokPos, it) }
            .let { pushStack(tokPos, LayoutDelimiter.Paren, it) }

        LCURLY -> state
            .let { insertDefault(src, tokPos, it) }
            .let { pushStack(tokPos, LayoutDelimiter.Brace, it) }
            .let { pushStack(tokPos, LayoutDelimiter.Property, it) }

        LBRACK -> state
            .let { insertDefault(src, tokPos, it) }
            .let { pushStack(tokPos, LayoutDelimiter.Square, it) }

        RPAREN -> state
            .let { collapse(tokPos, ::indentedP, it) }
            .let { popStack(it) { it == LayoutDelimiter.Paren } }
            .let { insertToken(src, it) }

        RCURLY -> state
            .let { collapse(tokPos, ::indentedP, it) }
            .let { popStack(it) { it == LayoutDelimiter.Property } }
            .let { popStack(it) { it == LayoutDelimiter.Brace } }
            .let { insertToken(src, it) }

        RBRACK -> state
            .let { collapse(tokPos, ::indentedP, it) }
            .let { popStack(it) { it == LayoutDelimiter.Square } }
            .let { insertToken(src, it) }

        STRING -> state
            .let { insertDefault(src, tokPos, it) }
            .let { popStack(it) { it == LayoutDelimiter.Property } }

        IDENT, TYPE -> state
            .let { insertDefault(src, tokPos, it) }
            .let { popStack(it) { it == LayoutDelimiter.Property } }

        OPERATOR -> state
            .let { collapse(tokPos, ::offsideP, it) }
            .let { insertSep(tokPos, it) }
            .let { insertToken(src, it) }

        else -> insertDefault(src, tokPos, state)
    }
}

fun getTokensFromStack(stkIn: LayoutStack?): Sequence<LayoutDelimiter> {
    var stk = stkIn
    return generateSequence {
        val (_, lyt, tail) = stk ?: return@generateSequence null
        stk = tail
        lyt
    }
}

fun unwindLayout(
    pos: SourcePos,
    stkIn: LayoutStack?
): Sequence<SuperToken> {
    return getTokensFromStack(stkIn)
        .filter { isIndented(it) }
        .map { lytToken(pos, LAYOUT_END) }
}

fun lex(
    tokens: List<SuperToken>
): List<SuperToken> {
    val sourcePos = SourcePos(0, 0, 0)
    var stack: LayoutStack? = LayoutStack(
        sourcePos,
        LayoutDelimiter.Root,
        null
    )
    val acc = mutableListOf<SuperToken>()
    var startPos = sourcePos
    for (posToken in tokens) {
        val nextStart = posToken.range.end
        val (nextStack, toks) = insertLayout(posToken, nextStart, stack)
        val ts = toks.map { it.first }
        acc += ts
        stack = nextStack
        startPos = nextStart
    }
    acc += unwindLayout(startPos, stack)
    return acc
}

fun correctLineAndColumn(
    source: CharSequence
): (SourceToken, SourceToken) -> SourceToken {
    fun go(
        previous: SourceToken,
        current: SourceToken,
    ): SourceToken {
        val (_, start) = previous.range
        val (_, end) = current.range
        // might be expensive
        val subSequence = source
            .subSequence(start.offset, end.offset)
        val newlineIndex = subSequence
            .lastIndexOf("\n")
        val noNewline = newlineIndex == -1
        val tokenLength = end.offset - start.offset
        val newEnd = if (noNewline) {
            SourcePos(
                start.line,
                start.column + tokenLength,
                end.offset
            )
        } else {
            SourcePos(
                start.line + subSequence.count { it == '\n' },
                tokenLength - newlineIndex - 1,
                end.offset
            )
        }
        return SourceToken(
            range = SourceRange(start, newEnd),
            value = current.value
        )
    }
    return ::go
}

fun posFromOffset(offset: Int): SourcePos {
    return SourcePos(0, 0, offset)
}

fun rangeFromOffsets(start: Int, end: Int): SourceRange {
    return SourceRange(posFromOffset(start), posFromOffset(end))
}

fun getTokens(lexer: Lexer): Sequence<SourceToken> {
    return generateSequence {
        val sourceToken: SourceToken? = lexer.tokenType?.let { value ->
            SourceToken(
                range = rangeFromOffsets(
                    lexer.tokenStart,
                    lexer.tokenEnd
                ),
                value = value
            )
        }
        lexer.advance()
        sourceToken
    }
}

class LayoutLexer(delegate: Lexer) : DelegateLexer(delegate) {

    private var tokens: List<SourceToken> = listOf<SourceToken>()
    private var index = 0;
    private val root = SourceToken(rangeFromOffsets(0, 0), WS)

    override fun start(
        buffer: CharSequence,
        startOffset: Int,
        endOffset: Int,
        initialState: Int
    ) {
        require(startOffset == 0) { "does not support incremental lexing: startOffset must be 0" }
        require(initialState == 0) { "does not support incremental lexing: initialState must be 0" }

        super.start(buffer, startOffset, endOffset, initialState)
        this.tokens = getTokens(delegate)
            .runningFold(root, correctLineAndColumn(buffer))
            .drop(1)
            .toList()
            .let { toSupers(it) }
            .let(::lex)
            .flatMap { it.tokens}
        index = 0
    }

    private fun toLexemes(sourceTokens: List<SourceToken>): List<Lexeme> {
        var token:SourceToken? = null
        var trailing = mutableListOf<SourceToken>()
        var lexemes = mutableListOf<Lexeme>()
        for (t in sourceTokens) {
            if (token == null) {
                token = t
            } else {
                when(t.value) {
                    WS -> trailing.add(t)
                    MLCOMMENT -> trailing.add(t)
                    SLCOMMENT -> trailing.add(t)
                    DOC_COMMENT -> trailing.add(t)
                    else -> {
                        lexemes.add(Lexeme(token, trailing))
                        token = t
                        trailing = mutableListOf()
                    }
                }
            }
        }
        if (token != null) lexemes.add(Lexeme(token, trailing))
        return lexemes

    }

    private fun toSupers(sourceTokens: List<SourceToken>): List<SuperToken> {
        val lexemes = toLexemes(sourceTokens)
        var qualified = mutableListOf<Lexeme>()
        var lexeme:Lexeme? = null
        var superTokens = mutableListOf<SuperToken>()
        for (l in lexemes) {
            if (lexeme == null) {
                lexeme = l
            } else {
                when {
                    lexeme.value == PROPER_NAME &&
                        l.value  == DOT &&
                        l.trailingWhitespace.isEmpty() &&
                        lexeme.trailingWhitespace.isEmpty() -> {
                        // <Proper Name><.>
                        qualified.add(lexeme)
                        qualified.add(l)
                        lexeme = null
                    }
                    else -> {
                        superTokens.add(SuperToken(qualified, lexeme))
                        qualified = mutableListOf()
                        lexeme = l
                    }
                }
            }
        }
        if (lexeme != null) superTokens.add(SuperToken(qualified, lexeme))
        return superTokens
    }

    override fun advance() {
        index ++
    }

    override fun getTokenType(): IElementType? {
        return tokens.getOrNull(index)?.value
    }

    override fun getTokenEnd(): Int {
        return  tokens[index].range.end.offset
    }

    override fun getTokenStart(): Int {
        return tokens[index].range.start.offset
    }
}