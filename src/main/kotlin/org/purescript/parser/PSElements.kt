package org.purescript.parser

import org.purescript.psi.PSElementType

val Module = PSElementType("Module")
val ExportList = PSElementType("ExportList")
val ExportedClass = PSElementType("ExportedClass")
val ExportedData = PSElementType("ExportedData")
val ExportedDataMember = PSElementType("ExportedDataMember")
val ExportedDataMemberList = PSElementType("ExportedDataMemberList")
val ExportedKind = PSElementType("ExportedKind")
val ExportedModule = PSElementType("ExportedModule")
val ExportedOperator = PSElementType("ExportedOperator")
val ExportedType = PSElementType("ExportedType")
val ExportedValue = PSElementType("ExportedValue")
val Star = PSElementType("Star")
val Bang = PSElementType("Bang")
val RowKind = PSElementType("RowKind")
val FunKind = PSElementType("FunKind")
val DocComment = PSElementType("DocComment")
val Type = PSElementType("Type")
val TypeArgs = PSElementType("TypeArgs")
val ForAll = PSElementType("ForAll")
val ConstrainedType = PSElementType("ConstrainedType")
val Row = PSElementType("Row")
val ObjectType = PSElementType("ObjectType")
val TypeVar = PSElementType("TypeVar")
val TypeVarName = PSElementType("TypeVarName")
val TypeVarKinded = PSElementType("TypeVarKinded")
val TypeConstructor = PSElementType("TypeConstructor")
val TypeAtom = PSElementType("TypeAtom")
val GenericIdentifier = PSElementType("GenericIdentifier")
val LocalIdentifier = PSElementType("LocalIdentifier")
val DataDeclaration = PSElementType("DataDeclaration")
val DataConstructorList = PSElementType("DataConstructorList")
val DataConstructor = PSElementType("DataConstructor")
val Signature = PSElementType("Signature")
val TypeSynonymDeclaration = PSElementType("TypeSynonymDeclaration")
val ValueDeclaration = PSElementType("ValueDeclaration")
val ForeignDataDeclaration = PSElementType("ForeignDataDeclaration")
val ExternInstanceDeclaration = PSElementType("ExternInstanceDeclaration")
val ForeignValueDeclaration = PSElementType("ForeignValueDeclaration")
val FixityDeclaration = PSElementType("FixityDeclaration")
val ImportDeclaration = PSElementType("ImportDeclaration")
val ImportAlias = PSElementType("ImportAlias")
val ImportList = PSElementType("ImportList")
val ImportedClass = PSElementType("ImportedClass")
val ImportedData = PSElementType("ImportedData")
val ImportedDataMemberList = PSElementType("ImportedDataMemberList")
val ImportedDataMember = PSElementType("ImportedDataMember")
val ImportedKind = PSElementType("ImportedKind")
val ImportedOperator = PSElementType("ImportedOperator")
val ImportedType = PSElementType("ImportedType")
val ImportedValue = PSElementType("ImportedValue")
val PositionedDeclarationRef = PSElementType("PositionedDeclarationRef")
val ClassDeclaration = PSElementType("ClassDeclaration")
val ClassConstraintList = PSElementType("ClassConstraintList")
val ClassConstraint = PSElementType("ClassConstraint")
val ClassFunctionalDependencyList =
    PSElementType("ClassFunctionalDependencyList")
val ClassFunctionalDependency = PSElementType("ClassFunctionalDependency")
val ClassMember = PSElementType("ClassMember")
val ClassMemberList = PSElementType("ClassMemberList")
val InstanceDeclaration = PSElementType("TypeInstanceDeclaration")
val NewtypeDeclaration = PSElementType("NewtypeDeclaration")
val NewTypeConstructor = PSElementType("NewTypeConstructor")
val Guard = PSElementType("Guard")
val NullBinder = PSElementType("NullBinder")
val StringBinder = PSElementType("StringBinder")
val CharBinder = PSElementType("CharBinder")
val BooleanBinder = PSElementType("BooleanBinder")
val NumberBinder = PSElementType("NumberBinder")
val NamedBinder = PSElementType("NamedBinder")
val VarBinder = PSElementType("VarBinder")
val ConstructorBinder = PSElementType("ConstructorBinder")
val ObjectBinder = PSElementType("ObjectBinder")
val ObjectBinderField = PSElementType("ObjectBinderField")
val BinderAtom = PSElementType("BinderAtom")
val Binder = PSElementType("Binder")
val ValueRef = PSElementType("ValueRef")
val BooleanLiteral = PSElementType("BooleanLiteral")
val NumericLiteral = PSElementType("NumericLiteral")
val StringLiteral = PSElementType("StringLiteral")
val CharLiteral = PSElementType("CharLiteral")
val ArrayLiteral = PSElementType("ArrayLiteral")
val ObjectLiteral = PSElementType("ObjectLiteral")
val Abs = PSElementType("Abs")
val IdentInfix = PSElementType("IdentInfix")
val ExpressionConstructor = PSElementType("ExpressionConstructor")
val ExpressionIdentifier = PSElementType("ExpressionIdentifier")
/** Symbol is a operator in parenthesis
`(+)`
in
```
addOne = (+) 1
```
 */
val ExpressionSymbol = PSElementType("ExpressionSymbol")
/**  Operator in expression
`+`
in
```
addOne a = a + 1
```
 */
val ExpressionOperator = PSElementType("ExpressionOperator")
val ExpressionWhere = PSElementType("ExpressionWhere")
val Case = PSElementType("Case")
val CaseAlternative = PSElementType("CaseAlternative")
val IfThenElse = PSElementType("IfThenElse")
val Let = PSElementType("Let")
val Parens = PSElementType("Parens")
val UnaryMinus = PSElementType("UnaryMinus")
val Accessor = PSElementType("Accessor")
val DoBlock = PSElementType("DoBlock")
val DoNotationLet = PSElementType("DoNotationLet")
val DoNotationBind = PSElementType("DoNotationBind")
val DoNotationValue = PSElementType("DoNotationValue")
val Value = PSElementType("Value")
val Fixity = PSElementType("Fixity")
val JSRaw = PSElementType("JavaScript")
val ModuleName = PSElementType("ModuleName")
val Identifier = PSElementType("identifier")
val Symbol = PSElementType("symbol")
val QualifiedSymbol = PSElementType("symbol")
val ProperName = PSElementType("ProperName")
val OperatorName = PSElementType("OperatorName")
val QualifiedIdentifier = PSElementType("QualifiedIdentifier")
val QualifiedProperName = PSElementType("QualifiedProperName")
val QualifiedOperatorName = PSElementType("QualifiedOperatorName")
val importModuleName = PSElementType("ImportModuleName")
val ClassName = PSElementType("ClassName")
val pImplies = PSElementType("Implies")
val TypeHole = PSElementType("TypeHole")
