package de.peeeq.wurstscript.scalaFrontend

import _root_.de.peeeq.wurstscript.antlr.WurstParser._
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import org.eclipse.jdt.annotation.NonNull
import org.eclipse.jdt.annotation.Nullable
import Ast._
import Ast.JassTopLevelDeclaration
import _root_.de.peeeq.wurstscript.antlr.WurstParser
import _root_.de.peeeq.wurstscript.scalaFrontend.Ast.JassTopLevelDeclaration
import _root_.de.peeeq.wurstscript.scalaFrontend.Ast._

import scala.collection.JavaConversions._
import de.peeeq.wurstscript.utils.Utils
import de.peeeq.wurstscript.WurstOperator

//import _root_.de.peeeq.wurstscript.parser.Position

trait ErrorHandler {
  def sendError(e: CompileError) = ???
}

trait LineOffsets {}

class CompileError(pos: Position, message: String) extends RuntimeException {

}

class AntlrWurstParseTreeTransformer(
    private var file: String, 
    private var cuErrorHandler: ErrorHandler, 
    private var lineOffsets: LineOffsets) {

  def transform(cu: CompilationUnitContext): CompilationUnit = {
    var jassDecls = List[JassTopLevelDeclaration]()
    var packages = List[WPackage]()
    try {
      for (decl: WurstParser.TopLevelDeclarationContext <- cu.decls) {
        if (decl.jassTopLevelDeclaration() != null) {
          jassDecls :+= transformJassToplevelDecl(decl.jassTopLevelDeclaration())
        } else if (decl.wpackage() != null) {
          packages :+= transformPackage(decl.wpackage())
        }
      }
    } catch {
      case e: CompileError => {
        cuErrorHandler.sendError(e)
        e.printStackTrace()
      }
      case e: NullPointerException => e.printStackTrace()
    }
    CompilationUnit(jassDecls, packages)
  }

  private def transformJassToplevelDecl(d: JassTopLevelDeclarationContext): JassTopLevelDeclaration = {
    if (d.jassFuncDef() != null) {
      return transformJassFuncDef(d.jassFuncDef())
    } else if (d.jassGlobalsBlock() != null) {
      return transformJassGlobalsBlock(d.jassGlobalsBlock())
    } else if (d.jassNativeDecl() != null) {
      return transformJassNativeDecl(d.jassNativeDecl())
    } else if (d.jassTypeDecl() != null) {
      return transformJassTypeDecl(d.jassTypeDecl())
    }
    throw error(d, "unhandled case: " + text(d))
  }

  private def text(t: Token): Identifier = {
    if (t == null) {
      Identifier("").withPos(Position(1, 0))
    } else {
      Identifier(t.getText).withPos(source(t))
    }
  }

  private def text(t: ParserRuleContext): Identifier = {
    if (t == null) {
      Identifier("").withPos(Position(1, 0))
    } else {
      Identifier(t.getText).withPos(source(t))
    }
  }

  private def rawText(c: ParserRuleContext): String = {
    if (c == null) {
      return ""
    }
    c.getText
  }

  private def rawText(c: Token): String = {
    if (c == null) {
      return ""
    }
    c.getText
  }

  private def transformJassTypeDecl(t: JassTypeDeclContext): JassTopLevelDeclaration = {
    val modifiers = List[Modifier]()
    val name = text(t.name)
    val optTyp = transformOptionalType(t.typeExpr())
    NativeType(modifiers, name, optTyp).withPos(source(t))
  }

  private def transformJassNativeDecl(n: JassNativeDeclContext): JassTopLevelDeclaration = {
    val modifiers = List[Modifier]()
    val sig = transformFuncSig(n.jassFuncSignature())
    NativeFunc(modifiers, sig.name, sig.formalParameters, sig.returnType).withPos(source(n))
  }

  private def transformJassGlobalsBlock(g: JassGlobalsBlockContext): JassGlobalBlock = {
    var result = List[GlobalVarDef]()
    for (v <- g.jassGlobalDecl()) {
      var modifiers = List[Modifier]()
      if (v.constant != null) {
        modifiers :+= ModConstant().withPos(source(v.constant))
      }
      val optTyp = transformOptionalType(v.typeExpr())
      val name = text(v.name)
      val initialExpr = transformOptionalExpr(v.initial)
      result :+= GlobalVarDef(modifiers, optTyp, name, initialExpr).withPos(source(v))
    }
    JassGlobalBlock(result)
  }

  private def transformJassFuncDef(f: JassFuncDefContext): JassTopLevelDeclaration = {
    val modifiers = List[Modifier]()
    val sig = transformFuncSig(f.jassFuncSignature())
    val locals = transformJassLocals(f.jassLocals.toList)
    val stmts = transformJassStatements(f.jassStatements())

    val stmtsWithLocals = stmts.copy(body = locals ++ stmts.body)

    FuncDef(modifiers, sig.name, sig.typeParams, sig.formalParameters, sig.returnType, stmtsWithLocals).withPos(source(f))
  }

  private def transformJassLocals(jassLocals: List[JassLocalContext]): List[WStatement] = {
    var result = List[WStatement]()
    for (l <- jassLocals) {
      val modifiers = List[Modifier]()
      val optTyp = transformOptionalType(l.typeExpr())
      val name = text(l.name)
      val initialExpr = transformOptionalExpr(l.initial)
      result :+= LocalVarDef(modifiers, optTyp, name, initialExpr).withPos(source(l))
    }
    result
  }

  private def transformJassStatements(stmts: JassStatementsContext): WBlock = {
    val result = List[WStatement]()
    for (s <- stmts.jassStatement()) {
      result.add(transformJassStatement(s))
    }
    WBlock(result)
  }

  private def transformJassStatement(s: JassStatementContext): WStatement = {
    if (s.jassStatementCall() != null) {
      return transformJassStatementCall(s.jassStatementCall())
    } else if (s.jassStatementExithwhen() != null) {
      return transformJassStatementExitwhen(s.jassStatementExithwhen())
    } else if (s.jassStatementIf() != null) {
      return transformJassStatementIf(s.jassStatementIf())
    } else if (s.jassStatementLoop() != null) {
      return transformJassStatementLoop(s.jassStatementLoop())
    } else if (s.jassStatementReturn() != null) {
      return transformJassStatementReturn(s.jassStatementReturn())
    } else if (s.jassStatementSet() != null) {
      return transformJassStatementSet(s.jassStatementSet())
    }
    throw error(s, "unhandled case: " + text(s))
  }

  private def transformJassStatementSet(s: JassStatementSetContext): WStatement = {
    StmtSet(transformAssignable(s.left), transformExpr(s.right)).withPos(source(s))
  }

  private def transformJassStatementReturn(s: JassStatementReturnContext): WStatement = {
    val r = transformOptionalExpr(s.expr())
    val r2 = if (r.isInstanceOf[ExprEmpty]) None else Some(r)
    StmtReturn(r2).withPos(source(s))
  }

  private def transformJassStatementLoop(s: JassStatementLoopContext): WStatement = {
    StmtLoop(transformJassStatements(s.jassStatements())).withPos(source(s))
  }

  private def transformJassStatementIf(s: JassStatementIfContext): WStatement = {
    val thenBlock = transformJassStatements(s.thenStatements)
    val elseBlock = transformJassElseIfs(s.jassElseIfs())
    StmtIf(transformExpr(s.cond), thenBlock, elseBlock).withPos(source(s))
  }

  private def transformJassElseIfs(s: JassElseIfsContext): WStatement = {
    if (s == null) {
      StmtSkip()
    } else if (s.cond != null) {
      Ast.StmtIf(transformExpr(s.cond),
        transformJassStatements(s.thenStatements),
        transformJassElseIfs(s.jassElseIfs()))
        .withPos(source(s))
    } else if (s.elseStmts != null) {
      transformJassStatements(s.elseStmts)
    } else {
      StmtSkip()
    }
  }

  private def transformJassStatementExitwhen(s: JassStatementExithwhenContext): WStatement = {
    StmtExitwhen(transformExpr(s.cond)).withPos(source(s))
  }

  private def transformJassStatementCall(s: JassStatementCallContext): WStatement = {
    transformFunctionCall(s.exprFunctionCall())
  }

  private def transformPackage(p: WpackageContext): WPackage = {
    val modifiers = List[Modifier]()
    val imports = List[WImport]()
    for (i <- p.imports) {
      imports.add(transformImport(i))
    }
    val elements = List[WEntity]()
    for (e <- p.entities) {
      val en = transformEntity(e)
      if (en != null) {
        elements.add(en)
      }
    }
    WPackage(modifiers, text(p.name), imports, elements).withPos(source(p))
  }

  private def transformEntity(e: EntityContext): WEntity = {
    try {
      if (e.nativeType() != null) {
        return transformNativeType(e.nativeType())
      } else if (e.funcDef() != null) {
        return transformFuncDef(e.funcDef())
      } else if (e.varDef() != null) {
        return transformVardef(e.varDef())
      } else if (e.initBlock() != null) {
        return transformInit(e.initBlock())
      } else if (e.nativeDef() != null) {
        return transformNativeDef(e.nativeDef())
      } else if (e.classDef() != null) {
        return transformClassDef(e.classDef())
      } else if (e.enumDef() != null) {
        return transformEnumDef(e.enumDef())
      } else if (e.moduleDef() != null) {
        return transformModuleDef(e.moduleDef())
      } else if (e.interfaceDef() != null) {
        return transformInterfaceDef(e.interfaceDef())
      } else if (e.tupleDef() != null) {
        return transformTupleDef(e.tupleDef())
      } else if (e.extensionFuncDef() != null) {
        return transformExtensionFuncDef(e.extensionFuncDef())
      }
      if (e.exception != null) {
        return null
      }
      throw error(e, "not implemented " + text(e))
    } catch {
      case npe: NullPointerException => {
        npe.printStackTrace()
        null
      }
    }
  }

  private def transformExtensionFuncDef(f: ExtensionFuncDefContext): WEntity = {
    val src = source(f)
    val modifiers = transformModifiers(f.modifiersWithDoc())
    val extendedType = transformTypeExpr(f.receiverType)
    val sig = transformFuncSig(f.funcSignature())
    val body = transformStatements(f.statementsBlock())
    ExtensionFuncDef(modifiers, extendedType, sig.name, sig.typeParams, sig.formalParameters,
      sig.returnType, body).withPos(src)
  }

  private def transformModifiers(ms: ModifiersWithDocContext): List[Modifier] = {
    var result = List[Modifier]()
    if (ms.hotdocComment() != null) {
      result +:= WurstDoc(ms.hotdocComment().getText).withPos(source(ms.hotdocComment()))
    }
    for (m <- ms.modifiers) {
      result +:= transformModifier(m)
    }
    result
  }

  private def transformModifier(m: ModifierContext): Modifier = {
    val src = source(m)
    if (m.annotation() != null) {
      return Annotation(m.annotation().getText).withPos(src)
    }
    m.modType.getType match {
      case WurstParser.PUBLIC     => return VisibilityPublic().withPos(src)
      case WurstParser.PRIVATE    => return VisibilityPrivate().withPos(src)
      case WurstParser.PROTECTED  => return VisibilityProtected().withPos(src)
      case WurstParser.PULBICREAD => return VisibilityPublicread().withPos(src)
      case WurstParser.STATIC     => return ModStatic().withPos(src)
      case WurstParser.OVERRIDE   => return ModOverride().withPos(src)
      case WurstParser.ABSTRACT   => return ModAbstract().withPos(src)
      case WurstParser.CONSTANT   => return ModConstant().withPos(src)
    }
    throw error(m, "not implemented")
  }

  private def transformTupleDef(t: TupleDefContext): WEntity = {
    val src = source(t)
    val modifiers = transformModifiers(t.modifiersWithDoc())
    val name = text(t.name)
    val parameters = transformFormalParameters(t.formalParameters(), false)
    TupleDef(modifiers, name, List(), parameters).withPos(src)
  }

  private def transformInterfaceDef(i: InterfaceDefContext): WEntity = {
    val src = source(i)
    val modifiers = transformModifiers(i.modifiersWithDoc())
    val name = text(i.name)
    val typeParameters = transformTypeParams(i.typeParams())
    val extendsList = i.extended.map(transformTypeExpr(_))
    val slots = transformClassSlots(src, i.classSlots())
    InterfaceDef(modifiers=modifiers, 
        nameId=name, 
        typeParameters=typeParameters, 
        extendsList=extendsList.toList, 
        methods=slots.methods, 
        vars=slots.vars, 
        constructors=slots.constructors,
        moduleUses=slots.moduleUses, 
        onDestroy=slots.onDestroy)
      .withPos(src)
  }

  private def transformClassSlots(src: Position, slots: ClassSlotsContext): ClassSlotResult = {
    val result = new ClassSlotResult()
    if (slots != null && slots.slots != null) {
      for (slot <- slots.slots) {
        val s = transformClassSlot(slot)
        if (s.isInstanceOf[ConstructorDef]) {
          result.constructors.add(s.asInstanceOf[ConstructorDef])
        } else if (s.isInstanceOf[FuncDef]) {
          result.methods.add(s.asInstanceOf[FuncDef])
        } else if (s.isInstanceOf[ModuleUse]) {
          result.moduleUses.add(s.asInstanceOf[ModuleUse])
        } else if (s.isInstanceOf[OnDestroyDef]) {
          if (result.onDestroy == null) {
            result.onDestroy = s.asInstanceOf[OnDestroyDef]
          } else {
            throw new CompileError(s.attrSource(), "ondestroy already defined.")
          }
        } else if (s.isInstanceOf[GlobalVarDef]) {
          result.vars.add(s.asInstanceOf[GlobalVarDef])
        } else if (s.isInstanceOf[ClassDef]) {
          result.innerClasses.add(s.asInstanceOf[ClassDef])
        } else if (s != null) {
          throw error(slot, "unexpected classslot: " + s.getClass.getSimpleName)
        }
      }
    }
    if (result.onDestroy == null) {
      result.onDestroy = OnDestroyDef(StmtSkip()).withPos(src.artificial())
    }
    result
  }

  private def transformClassSlot(s: ClassSlotContext): ClassSlot = {
    try {
      if (s.constructorDef() != null) {
        return transformConstructorDef(s.constructorDef())
      } else if (s.moduleUse() != null) {
        return transformModuleUse(s.moduleUse())
      } else if (s.ondestroyDef() != null) {
        return transformOndestroyDef(s.ondestroyDef())
      } else if (s.varDef() != null) {
        return transformVardef(s.varDef())
      } else if (s.funcDef() != null) {
        return transformFuncDef(s.funcDef())
      } else if (s.classDef() != null) {
        return transformClassDef(s.classDef())
      }
      if (s.exception != null) {
        return null
      }
      throw error(s, "not matched: " + text(s))
    } catch {
      case npe: NullPointerException => {
        npe.printStackTrace()
        null
      }
    }
  }

  private def transformOndestroyDef(o: OndestroyDefContext): OnDestroyDef = {
    OnDestroyDef(transformStatements(o.statementsBlock())).withPos(source(o))
  }

  private def transformModuleUse(u: ModuleUseContext): ClassSlot = {
    ModuleUse(text(u.moduleName), transformTypeArgs(u.typeArgs())).withPos(source(u))
  }

  private def transformConstructorDef(c: ConstructorDefContext): ConstructorDef = {
    val modifiers = transformModifiers(c.modifiersWithDoc())
    val parameters = transformFormalParameters(c.formalParameters(), true)
    val body = transformStatementList(c.stmts.toList)
    val isExplicit = c.superArgs != null
    val superArgs = transformExprs(c.superArgs)
    ConstructorDef(modifiers, parameters, isExplicit, superArgs, body).withPos(source(c))
  }

  private def transformStatementList(stmts: List[StatementContext]): WBlock = {
    WBlock(stmts.map(transformStatement))
  }

  private def transformModuleDef(i: ModuleDefContext): WEntity = {
    val src = source(i)
    val modifiers = transformModifiers(i.modifiersWithDoc())
    val name = text(i.name)
    val typeParameters = transformTypeParams(i.typeParams())
    val slots = transformClassSlots(src, i.classSlots())
    ModuleDef(modifiers, name, typeParameters, slots.innerClasses, slots.methods, slots.vars,
      slots.constructors, slots.moduleUses, slots.onDestroy)
      .withPos(src)
  }

  private def transformEnumDef(i: EnumDefContext): WEntity = {
    val src = source(i)
    val modifiers = transformModifiers(i.modifiersWithDoc())
    val name = text(i.name)
    val members = i.enumMembers.map(m => EnumMember(List(), text(m)).withPos(source(m)))
    EnumDef(modifiers, name, members.toList).withPos(src)
  }

  private def transformClassDef(i: ClassDefContext): ClassDef = {
    val src = source(i)
    val modifiers = transformModifiers(i.modifiersWithDoc())
    val name = text(i.name)
    val typeParameters = transformTypeParams(i.typeParams())
    val extendedClass = transformOptionalType(i.extended)
    val implementsList = i.implemented.map(transformTypeExpr)
    val slots = transformClassSlots(src, i.classSlots())
    ClassDef(modifiers, name, typeParameters, extendedClass, implementsList.toList, slots.innerClasses,
      slots.methods, slots.vars, slots.constructors, slots.moduleUses, slots.onDestroy)
      .withPos(src)
  }

  private def transformNativeType(n: NativeTypeContext): NativeType = {
    var extended = 
      if (n.extended == null) None 
      else Some(TypeExprSimple(None, text(n.extended), List()).withPos(source(n.extended)))
    NativeType(List[Modifier](), text(n.name), extended).withPos(source(n))
  }

  private def transformFuncDef(f: FuncDefContext): FuncDef = {
    val sig = transformFuncSig(f.funcSignature())
    val modifiers = transformModifiers(f.modifiersWithDoc())
    val body = transformStatements(f.statementsBlock())
    FuncDef(modifiers, sig.name, sig.typeParams, sig.formalParameters, sig.returnType,
      body).withPos(source(f))
  }

  private def transformVardef(v: VarDefContext): GlobalVarDef = {
    val src = source(v)
    var modifiers = transformModifiers(v.modifiersWithDoc())
    if (v.constant != null) {
      modifiers +:= Ast.ModConstant().withPos(source(v.constant))
    }
    val initialExpr = transformOptionalExpr(v.initial)
    val name = text(v.name)
    val optTyp = transformOptionalType(v.varType)
    GlobalVarDef(modifiers, optTyp, name, initialExpr).withPos(src)
  }

  private def transformInit(i: InitBlockContext): InitBlock = {
    InitBlock(transformStatements(i.statementsBlock())).withPos(source(i))
  }

  private def transformStatements(b: StatementsBlockContext): WStatement = {
    if (b != null) 
      WBlock(b.statement().map(transformStatement).toList)
    else
      WBlock(List())
  }

  private def transformStatement(s: StatementContext): WStatement = {
    if (s.stmtIf() != null) {
      return transformIf(s.stmtIf())
    } else if (s.stmtWhile() != null) {
      return transformWhile(s.stmtWhile())
    } else if (s.localVarDef() != null) {
      return transformLocalVarDef(s.localVarDef())
    } else if (s.stmtSet() != null) {
      return transformStmtSet(s.stmtSet())
    } else if (s.expr() != null) {
      val e = transformExpr(s.expr())
      if (e.isInstanceOf[WStatement]) {
        return e.asInstanceOf[WStatement]
      } else {
        cuErrorHandler.sendError(new CompileError(source(s), printElement(e) +
          " cannot be used here. A full statement is required."))
        return StmtErr().withPos(source(s))
      }
    } else if (s.stmtReturn() != null) {
      return transformReturn(s.stmtReturn())
    } else if (s.stmtForLoop() != null) {
      return transformForLoop(s.stmtForLoop())
    } else if (s.stmtBreak() != null) {
      return StmtExitwhen(ExprBoolVal(true).withPos(source(s).artificial())).withPos(source(s))
    } else if (s.stmtSkip() != null) {
      return StmtSkip().withPos(source(s))
    } else if (s.stmtSwitch() != null) {
      return transformSwitch(s.stmtSwitch())
    }
    if (s.exception != null) {
      return StmtErr().withPos(source(s))
    }
    throw error(s, "not implemented: " + text(s) + "\n" + s.toStringTree())
  }

  private def transformSwitch(s: StmtSwitchContext): WStatement = {
    val expr = transformExpr(s.expr())
    val cases = s.switchCase().map(c => 
      SwitchCase(transformExpr(c.expr()),
          transformStatements(c.statementsBlock()))
          .withPos(source(c)))
    val switchDefault = 
      if (s.switchDefaultCase() == null) None
      else Some(transformStatements(s.switchDefaultCase().statementsBlock()))
    StmtSwitch(expr, cases.toList, switchDefault).withPos(source(s))
  }

  private def transformWhile(s: StmtWhileContext): WStatement = {
    StmtWhile(transformExpr(s.cond), transformStatements(s.statementsBlock())).withPos(source(s))
  }

  private def transformExprDestroy(e: ExprDestroyContext): ExprDestroy = {
    ExprDestroy(transformExpr(e.expr())).withPos(source(e))
  }

  private def transformReturn(s: StmtReturnContext): StmtReturn = {
    val r = transformOptionalExpr(s.expr())
    val r2 = 
      if (r.isInstanceOf[ExprEmpty]) None
      else Some(r)
    StmtReturn(r2).withPos(source(s))
  }

  private def transformStmtSet(s: StmtSetContext): WStatement = {
    val updatedExpr = transformAssignable(s.left)
    val src = source(s)
    if (s.assignOp != null) {
      var right = transformExpr(s.right)
      val op = getAssignOp(s.assignOp)
      if (op != null) {
        right = ExprBinary(updatedExpr, op, right).withPos(src.artificial())
      }
      StmtSet(updatedExpr, right).withPos(src)
    } else if (s.incOp != null) {
      val right = ExprBinary(updatedExpr, WurstOperator.PLUS, Ast.ExprIntVal("1").withPos(src.artificial()) ).withPos(src.artificial())
      return StmtSet(updatedExpr, right).withPos(src)
    } else if (s.decOp != null) {
      val right = ExprBinary(updatedExpr, WurstOperator.MINUS, Ast.ExprIntVal("1").withPos(src.artificial())).withPos(src.artificial())
      return Ast.StmtSet(updatedExpr, right).withPos(src)
    }
    throw error(s, "not implemented")
  }

  private def getAssignOp(assignOp: Token): WurstOperator = assignOp.getType match {
    case WurstParser.PLUS_EQ  => WurstOperator.PLUS
    case WurstParser.MINUS_EQ => WurstOperator.MINUS
    case WurstParser.MULT_EQ  => WurstOperator.MULT
    case WurstParser.DIV_EQ   => WurstOperator.DIV_REAL
    case WurstParser.EQ       => null
    case _                    => throw error(source(assignOp), "unhandled assign op: " + text(assignOp))
  }

  private def transformAssignable(e: ExprAssignableContext): AssignableExpr = {
    if (e.exprMemberVar() != null) {
      return transformExprMemberVar(e.exprMemberVar())
    } else if (e.exprVarAccess() != null) {
      return transformExprVarAccess(e.exprVarAccess())
    }
    throw error(e, "not implemented: " + text(e))
  }

  private def transformExprVarAccess(e: ExprVarAccessContext): AssignableExpr = {
    val va = ExprVarAccess(text(e.varname)).withPos(source(e.varname))
    if (e.indexes() == null) {
      va
    } else {
      ExprArrayLookup(va, transformIndexes(e.indexes())).withPos(source(e))
    }
  }

  private def transformExprMemberVar(e: ExprMemberVarContext): AssignableExpr = {
    transformExprMemberVarAccess2(source(e), e.expr(), e.dots, e.varname, e.indexes())
  }

  private def transformExprMemberVarAccess2(source: Position,
                                            e_expr: ExprContext,
                                            e_dots: Token,
                                            e_varname: Token,
                                            e_indexes: IndexesContext): AssignableExpr = {
    val left = transformExpr(e_expr)
    val varName = text(e_varname)
    val va = ExprMemberVar(left, varName, e_dots.getType == WurstParser.DOTDOT).withPos(source)
    if (e_indexes == null) {
      va
    } else {
      ExprArrayLookup(va, transformIndexes(e_indexes)).withPos(source)
    }
  }

  private def transformForLoop(s: StmtForLoopContext): WStatement = {
    if (s.forRangeLoop() != null) {
      return transformForRangeLoop(s.forRangeLoop())
    } else if (s.forIteratorLoop() != null) {
      return transformForIteratorLoop(s.forIteratorLoop())
    }
    throw error(s, "not implemented: " + text(s))
  }

  private def transformForRangeLoop(s: ForRangeLoopContext): WStatement = {
    val src = source(s)
    val loopVar = transformLocalVarDef(s.loopVar).copy(initialExpr=Some(transformExpr(s.start)))
    val to = transformExpr(s.end)
    var step: Expr = 
      if (s.step == null) Ast.ExprIntVal("1").withPos(source(s.direction).artificial()) 
      else transformExpr(s.step)
    val body = transformStatements(s.statementsBlock())
    StmtForRange(loopVar, to, step, s.direction.getType == WurstParser.TO, body).withPos(src)
  }

  private def transformLocalVarDef(v: LocalVarDefInlineContext): LocalVarDef = {
    val modifiers = List[Modifier]()
    val optTyp = transformOptionalType(v.typeExpr())
    val name = text(v.name)
    LocalVarDef(modifiers, optTyp, name, None).withPos(source(v))
  }

  private def transformForIteratorLoop(s: ForIteratorLoopContext): WStatement = {
    val src = source(s)
    val loopVar = transformLocalVarDef(s.loopVar)
    val in = transformExpr(s.iteratorExpr)
    val body = transformStatements(s.statementsBlock())
    val iterationMode = 
      if (s.iterStyle.getType == WurstParser.IN) ForIn()
      else ForFrom()
    StmtFor(loopVar, in, iterationMode, body).withPos(src)
  }

  private def transformLocalVarDef(l: LocalVarDefContext): LocalVarDef = {
    val modifiers = List[Modifier]()
    if (l.let != null) {
      modifiers.add(Ast.ModConstant(source(l.let)))
    }
    val optTyp = transformOptionalType(l.`type`)
    val name = text(l.name)
    val initialExpr = transformOptionalExpr(l.initial)
    Ast.LocalVarDef(source(l), modifiers, optTyp, name, initialExpr)
  }

  private def transformOptionalExpr(e: ExprContext): OptExpr = {
    if (e == null) {
      return Ast.NoExpr()
    }
    val r = transformExpr(e)
    r
  }

  private def transformExprNewObject(e: ExprNewObjectContext): ExprNewObject = {
    val typeName = text(e.className)
    val typeArgs = transformTypeArgs(e.typeArgs())
    val args = transformExprs(e.exprList())
    Ast.ExprNewObject(source(e), typeName, typeArgs, args)
  }

  private def transformMemberMethodCall2(source: Position,
                                         receiver: ExprContext,
                                         dots: Token,
                                         funcName: Token,
                                         typeArgs: TypeArgsContext,
                                         args: ExprListContext): ExprMemberMethod = {
    val left = transformExpr(receiver)
    if (dots.getType == WurstParser.DOT) {
      Ast.ExprMemberMethodDot(source, left, text(funcName), transformTypeArgs(typeArgs), transformExprs(args))
    } else {
      Ast.ExprMemberMethodDotDot(source, left, text(funcName), transformTypeArgs(typeArgs), transformExprs(args))
    }
  }

  private def transformFunctionCall(c: ExprFunctionCallContext): ExprFunctionCall = {
    Ast.ExprFunctionCall(source(c), text(c.funcName), transformTypeArgs(c.typeArgs()), transformExprs(c.exprList()))
  }

  private def transformExprs(es: ExprListContext): Arguments = {
    val result = Ast.Arguments()
    if (es != null) {
      for (e <- es.exprs) {
        result.add(transformExpr(e))
      }
    }
    if (result.size == 1 && result.get(0).isInstanceOf[ExprEmpty]) {
      result.clear()
    }
    result
  }

  private def transformIf(i: StmtIfContext): WStatement = {
    val cond = transformExpr(i.cond)
    val thenBlock = transformStatements(i.thenStatements)
    var elseBlock: WStatements = null
    elseBlock = if (i.elseStatements() != null) if (i.elseStatements().stmtIf() != null) List[WStatement](transformIf(i.elseStatements().stmtIf())) else transformStatements(i.elseStatements().statementsBlock()) else List[WStatement]()
    Ast.StmtIf(source(i), cond, thenBlock, elseBlock)
  }

  private def transformExpr(e: ExprContext): Expr = {
    var source = source(e)
    if (e.exprPrimary() != null) {
      return transformExprPrimary(e.exprPrimary())
    } else if (e.left != null && e.right != null && e.op != null) {
      return Ast.ExprBinary(source, transformExpr(e.left), transformOp(e.op), transformExpr(e.right))
    } else if (e.op != null && e.op.getType == WurstParser.NOT) {
      return Ast.ExprUnary(source, WurstOperator.NOT, transformExpr(e.right))
    } else if (e.op != null && e.op.getType == WurstParser.MINUS) {
      return Ast.ExprUnary(source, WurstOperator.UNARY_MINUS, transformExpr(e.right))
    } else if (e.castToType != null) {
      return Ast.ExprCast(source, transformTypeExpr(e.castToType), transformExpr(e.left))
    } else if (e.dotsVar != null) {
      return transformExprMemberVarAccess2(source, e.receiver, e.dotsVar, e.varName, e.indexes())
    } else if (e.dotsCall != null) {
      return transformMemberMethodCall2(source, e.receiver, e.dotsCall, e.funcName, e.typeArgs(), e.exprList())
    } else if (e.instaneofType != null) {
      return Ast.ExprInstanceOf(source, transformTypeExpr(e.instaneofType), transformExpr(e.left))
    }
    val left = getLeftParseTree(e)
    if (left != null) {
      source = source.withLeftPos(1 + stopPos(left))
    }
    val right = getRightParseTree(e)
    if (right != null) {
      source = source.withRightPos(beginPos(right))
    }
    Ast.ExprEmpty(source)
  }

  private def beginPos(left: ParseTree): Int = {
    if (left.isInstanceOf[ParserRuleContext]) {
      val left2 = left.asInstanceOf[ParserRuleContext]
      return left2.getStart.getStartIndex
    } else if (left.isInstanceOf[TerminalNode]) {
      val left2 = left.asInstanceOf[TerminalNode]
      return left2.getSymbol.getStartIndex
    }
    throw new Error("unhandled case: " + left.getClass + "  // " + left)
  }

  private def stopPos(left: ParseTree): Int = {
    if (left.isInstanceOf[ParserRuleContext]) {
      val left2 = left.asInstanceOf[ParserRuleContext]
      return left2.getStop.getStopIndex
    } else if (left.isInstanceOf[TerminalNode]) {
      val left2 = left.asInstanceOf[TerminalNode]
      return left2.getSymbol.getStopIndex
    }
    throw new Error("unhandled case: " + left.getClass + "  // " + left)
  }

  private def getLeftParseTree(e: ParserRuleContext): ParseTree = {
    if (e == null || e.getParent == null) {
      return null
    }
    val parent = e.getParent
    for (i <- 0 until parent.getChildCount if parent.getChild(i) == e) {
      if (i > 0) {
        return parent.getChild(i - 1)
      } else {
        return getLeftParseTree(parent)
      }
    }
    null
  }

  private def getRightParseTree(e: ParserRuleContext): ParseTree = {
    if (e == null || e.getParent == null) {
      return null
    }
    val parent = e.getParent
    for (i <- 0 until parent.getChildCount if parent.getChild(i) == e) {
      if (i < parent.getChildCount - 1) {
        return parent.getChild(i + 1)
      } else {
        return getRightParseTree(parent)
      }
    }
    null
  }

  private def transformOp(op: Token): WurstOperator = op.getType match {
    case WurstParser.OR         => WurstOperator.OR
    case WurstParser.AND        => WurstOperator.AND
    case WurstParser.EQEQ       => WurstOperator.EQ
    case WurstParser.NOT_EQ     => WurstOperator.NOTEQ
    case WurstParser.LESS_EQ    => WurstOperator.LESS_EQ
    case WurstParser.LESS       => WurstOperator.LESS
    case WurstParser.GREATER_EQ => WurstOperator.GREATER_EQ
    case WurstParser.GREATER    => WurstOperator.GREATER
    case WurstParser.PLUS       => WurstOperator.PLUS
    case WurstParser.MINUS      => WurstOperator.MINUS
    case WurstParser.MULT       => WurstOperator.MULT
    case WurstParser.DIV_REAL   => WurstOperator.DIV_REAL
    case WurstParser.DIV        => WurstOperator.DIV_INT
    case WurstParser.MOD_REAL   => WurstOperator.MOD_REAL
    case WurstParser.MOD        => WurstOperator.MOD_INT
    case WurstParser.NOT        => WurstOperator.NOT
  }

  private def transformExprPrimary(e: ExprPrimaryContext): Expr = {
    if (e.atom != null) {
      return transformAtom(e.atom)
    } else if (e.varname != null) {
      if (e.indexes() != null) {
        val index = transformIndexes(e.indexes())
        return Ast.ExprVarArrayAccess(source(e), text(e.varname), index)
      } else {
        return Ast.ExprVarAccess(source(e), text(e.varname))
      }
    } else if (e.expr() != null) {
      return transformExpr(e.expr())
    } else if (e.exprFunctionCall() != null) {
      return transformFunctionCall(e.exprFunctionCall())
    } else if (e.exprNewObject() != null) {
      return transformExprNewObject(e.exprNewObject())
    } else if (e.exprClosure() != null) {
      return transformClosure(e.exprClosure())
    } else if (e.exprStatementsBlock() != null) {
      return transformExprStatementsBlock(e.exprStatementsBlock())
    } else if (e.exprFuncRef() != null) {
      return transformExprFuncRef(e.exprFuncRef())
    } else if (e.exprDestroy() != null) {
      return transformExprDestroy(e.exprDestroy())
    }
    throw error(e, "not implemented " + text(e))
  }

  private def transformExprFuncRef(e: ExprFuncRefContext): ExprFuncRef = {
    val scopeName = if (e.scopeName == null) "" else rawText(e.scopeName)
    val funcName = text(e.funcName)
    Ast.ExprFuncRef(source(e), scopeName, funcName)
  }

  private def transformExprStatementsBlock(e: ExprStatementsBlockContext): ExprStatementsBlock = {
    Ast.ExprStatementsBlock(source(e), transformStatements(e.statementsBlock()))
  }

  private def transformClosure(e: ExprClosureContext): ExprClosure = {
    val parameters = transformFormalParameters(e.formalParameters(), true)
    val implementation = transformExpr(e.expr())
    Ast.ExprClosure(source(e), parameters, implementation)
  }

  private def transformIndexes(indexes: IndexesContext): List[Expr] = {
    val result = Ast.Indexes()
    result.add(transformExpr(indexes.expr()))
    result
  }

  private def transformAtom(a: Token): Expr = {
    val source = source(a)
    if (a.getType == WurstParser.INT) {
      return Ast.ExprIntVal(source, rawText(a))
    } else if (a.getType == WurstParser.REAL) {
      return Ast.ExprRealVal(source, rawText(a))
    } else if (a.getType == WurstParser.STRING) {
      return Ast.ExprStringVal(source, getStringVal(source, rawText(a)))
    } else if (a.getType == WurstParser.NULL) {
      return Ast.ExprNull(source)
    } else if (a.getType == WurstParser.TRUE) {
      return Ast.ExprBoolVal(source, true)
    } else if (a.getType == WurstParser.FALSE) {
      return Ast.ExprBoolVal(source, false)
    } else if (a.getType == WurstParser.THIS) {
      return Ast.ExprThis(source)
    } else if (a.getType == WurstParser.SUPER) {
      return Ast.ExprSuper(source)
    }
    throw error(source(a), "not implemented: " + text(a))
  }

  private def getStringVal(source: Position, text: String): String = {
    val res = new StringBuilder()
    for (i <- 1 until text.length - 1) {
      val c = text.charAt(i)
      if (c == '\\') {
        i += 1
        text.charAt(i) match {
          case '\\' => res.append('\\')
          case 'n'  => res.append('\n')
          case 'r'  => res.append('\r')
          case 't'  => res.append('\t')
          case '"'  => res.append('"')
          case '\'' => res.append('\'')
          case _    => throw new CompileError(source, "Invalid escape sequence: " + text.charAt(i))
        }
      } else {
        res.append(c)
      }
    }
    res.toString
  }

  private def transformNativeDef(n: NativeDefContext): WEntity = {
    val modifiers = transformModifiers(n.modifiersWithDoc())
    val sig = transformFuncSig(n.funcSignature())
    Ast.NativeFunc(source(n), modifiers, sig.name, sig.formalParameters, sig.returnType)
  }

  private def transformFuncSig(s: FuncSignatureContext): FuncSig = {
    val typeParams = transformTypeParams(s.typeParams())
    val formalParameters = transformFormalParameters(s.formalParameters(), true)
    val returnType = transformOptionalType(s.returnType)
    new FuncSig(text(s.name), typeParams, formalParameters, returnType)
  }

  private def transformFuncSig(s: JassFuncSignatureContext): FuncSig = {
    val typeParams = Ast.TypeParamDefs()
    val formalParameters = Ast.WParameters()
    for (p <- s.args) {
      formalParameters.add(transformFormalParameter(p, false))
    }
    val returnType = transformOptionalType(s.returnType)
    new FuncSig(text(s.name), typeParams, formalParameters, returnType)
  }

  private def transformOptionalType(t: TypeExprContext): OptTypeExpr = {
    if (t == null) {
      return Ast.NoTypeExpr()
    }
    transformTypeExpr(t)
  }

  private def transformTypeExpr(t: TypeExprContext): TypeExpr = {
    var scopeType: OptTypeExpr = null
    scopeType = if (t.typeExpr() != null) transformTypeExpr(t.typeExpr()) else Ast.NoTypeExpr()
    if (t.thistype != null) {
      return Ast.TypeExprThis(source(t), scopeType)
    } else if (t.typeName != null) {
      val typeName = rawText(t.typeName)
      val typeArgs = transformTypeArgs(t.typeArgs())
      return Ast.TypeExprSimple(source(t), scopeType, typeName, typeArgs)
    } else if (t.typeExpr() != null) {
      return Ast.TypeExprArray(source(t), scopeType.asInstanceOf[TypeExpr], transformOptionalExpr(t.arraySize))
    }
    throw error(t, "not implemented " + t.toStringTree())
  }

  private def error(source: Position, msg: String): CompileError = new CompileError(source, msg)

  private def error(source: ParserRuleContext, msg: String): CompileError = new CompileError(source(source), msg)

  private def transformTypeArgs(typeArgs: TypeArgsContext): TypeExprList = {
    val result = Ast.TypeExprList()
    for (e <- typeArgs.args) {
      result.add(transformTypeExpr(e))
    }
    result
  }

  private def transformFormalParameters(ps: FormalParametersContext, makeConstant: Boolean): WParameters = {
    val result = Ast.WParameters()
    for (p <- ps.params) {
      result.add(transformFormalParameter(p, makeConstant))
    }
    result
  }

  private def transformFormalParameter(p: FormalParameterContext, makeConstant: Boolean): WParameter = {
    val modifiers = List[Modifier]()
    if (makeConstant) {
      modifiers.add(Ast.ModConstant(source(p).artificial()))
    }
    Ast.WParameter(source(p), modifiers, transformTypeExpr(p.typeExpr()), text(p.name))
  }

  private def transformTypeParams(typeParams: TypeParamsContext): TypeParamDefs = {
    val result = Ast.TypeParamDefs()
    for (p <- typeParams.params) {
      result.add(transformTypeParam(p))
    }
    result
  }

  private def transformTypeParam(p: TypeParamContext): TypeParamDef = {
    val modifiers = List[Modifier]()
    Ast.TypeParamDef(source(p), modifiers, text(p.name))
  }

  private def transformImport(i: WImportContext): WImport = {
    Ast.WImport(source(i), i.isPublic != null, i.isInitLater != null, text(i.importedPackage))
  }

  private def source(p: ParserRuleContext): Position = {
    var stopIndex: Int = 0
    if (p.stop.getType == WurstParser.NL) {
      stopIndex = p.stop.getStartIndex + 1
      if (p.stop.getText.contains("\r")) {
        stopIndex += 1
      }
    } else {
      stopIndex = p.stop.getStopIndex + 1
    }
    Position(p.start.getStartIndex, stopIndex)
  }

  private def source(p: Token): Position = {
    Position(p.getStartIndex, p.getStopIndex + 1)
  }

  class FuncSig(var name: Identifier,
                var typeParams: TypeParamDefs,
                var formalParameters: WParameters,
                var returnType: OptTypeExpr)

  class ClassSlotResult {

    var innerClasses: ClassDefs = Ast.ClassDefs()

    var constructors: ConstructorDefs = Ast.ConstructorDefs()

    var moduleInstanciations: ModuleInstanciations = Ast.ModuleInstanciations()

    var vars: GlobalVarDefs = Ast.GlobalVarDefs()

    var methods: FuncDefs = Ast.FuncDefs()

    var moduleUses: ModuleUses = Ast.ModuleUses()

    var onDestroy: OnDestroyDef = null
  }

  def printElement(e: AstElement): String = {
      e.toString()
    }

}
