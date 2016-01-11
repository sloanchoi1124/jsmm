
package buildast;


import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.antlr.v4.runtime.tree.TerminalNode;

import parser.JsmmParser.Id_type_pairContext;
import parser.JsmmParser.TypeContext;
import parser.JsmmParser.Type_annotationContext;
import static ast.Ast.*;
import static ast.Type.*;
import ast.Operator;
import ast.Type;
import static parser.JsmmParser.*;
import util.CompilerException;
import util.Location;
import util.Uid;

import static buildast.MakeLocation.*;


public class BuildAst {
	
	private static EnvironmentIfc environment;
	
	// ---------- programs ----------
	public static Program buildAst(ProgramContext programContext) {
		environment = new Environment();
		StandardLibrary.addLibrary(environment);
		List<Statement> body = new ArrayList<Statement>();
		convertBody(body, programContext.declaration(), programContext.statement());
		return new Program(environment.extractSymbolTable(), environment.getUids(), body);
	}
	
	
	private static void convertBody(List<Statement> body, List<DeclarationContext> declCtxs, List<StatementContext> stmtCtxs) {
		for (DeclarationContext dc : declCtxs) {
			if (dc instanceof InitDeclContext) { // VAR type_annotation? ID ASSIGN expression SEMICOLON
				InitDeclContext ic = (InitDeclContext) dc;
				TerminalNode id = ic.ID();
				Type ty = null;
				if (ic.type_annotation() != null ) {
					ty = convertTypeAnnotation(ic.type_annotation());		
				}
				// semantics of js-- require that rhs be evaluated before creating variable
				Expression rhs = convertExpression(ic.expression());
				Location idLoc = makeLoc(id);
				Uid uid = environment.defineVariable(idLoc, id.getText(), ty);
				Expression lhs = new IdExpression(idLoc, uid);
				Location location = makeLoc(ic.VAR(), ic.SEMICOLON());
				body.add(new AssignmentStatement(location, lhs, rhs));
				
			} else { // uninitialized declarations VAR type_annotation ID (COMMA ID)* SEMICOLON
				UninitDeclContext uc = (UninitDeclContext)dc;
				Type ty = convertTypeAnnotation(uc.type_annotation());		
				for (TerminalNode tn : uc.ID()) {
					environment.defineVariable(makeLoc(tn), tn.getText(), ty);
				}
			}
		}
		for (StatementContext stmt : stmtCtxs) {
			body.add(convertStatement(stmt));
		}
	}
	
	
	// ---------- statements ----------
	private static Statement convertStatement(StatementContext statementContext) {
		if (statementContext instanceof BreakStmtContext) {
			BreakStmtContext bc = (BreakStmtContext)statementContext;
			return new BreakStatement(makeLoc(bc.BREAK()));
			
		} else if (statementContext instanceof ContinueStmtContext) {
			ContinueStmtContext cc = (ContinueStmtContext)statementContext;
			return new ContinueStatement(makeLoc(cc.CONTINUE()));
			
		} else if (statementContext instanceof AssignStmtContext) { // lvalue ASSIGN expression SEMICOLON
			AssignStmtContext ac = (AssignStmtContext) statementContext;
			Expression lvalExp = convertLvalue(ac.lvalue());
			Location location = makeLoc(lvalExp.location, ac.SEMICOLON());
			Expression rvalExp = convertExpression(ac.expression());
			return new AssignmentStatement(location, lvalExp, rvalExp);
		
		} else if (statementContext instanceof IncDecStmtContext) { // lvalue (INCREMENT | DECREMENT) SEMICOLON 
			IncDecStmtContext idc = (IncDecStmtContext) statementContext;
			Expression lvalExp = convertLvalue(idc.lvalue());
			Location location = makeLoc(lvalExp.location, idc.SEMICOLON());
			Operator op = (idc.INCREMENT() != null) ? Operator.PLUS : Operator.MINUS;
			// CHECKME: is full copy of lval needed? just using ref to it for now
			Expression rvalExp = new BinaryExpression(location, lvalExp, op, ONE);
			return new AssignmentStatement(location, lvalExp, rvalExp);

		} else if (statementContext instanceof AssignOpStmtContext) { // lvalue (PLUSEQ | MINUSEQ | TIMESEQ) expression SEMICOLON
			AssignOpStmtContext aoc = (AssignOpStmtContext) statementContext;
			// CHECKME: same question as for IncDec case
			Expression lvalExp = convertLvalue(aoc.lvalue());
			Location location = makeLoc(lvalExp.location, aoc.SEMICOLON());
			Operator op;
			if (aoc.PLUSEQ() != null) {
				op = Operator.PLUS;
			} else if (aoc.MINUSEQ() != null) {
				op = Operator.MINUS;
			} else if (aoc.TIMESEQ() != null) {
				op = Operator.TIMES;
			} else {
				throw new RuntimeException("unexpected assignment operator");
			}
			Expression subExp = convertExpression(aoc.expression());
			Expression rvalExp = new BinaryExpression(subExp.location, lvalExp, op, subExp);
			return new AssignmentStatement(location, lvalExp, rvalExp);
		
		} else if (statementContext instanceof IfStmtContext) { // IF LPAREN expression RPAREN statement (ELSE statement)?
			IfStmtContext ic = (IfStmtContext) statementContext;
			Expression condition = convertExpression(ic.expression());
			Location location;
			Statement thenStmt = convertStatement(ic.statement(0));
			Statement elseStmt;
			if (ic.statement().size() > 1) {
				elseStmt = convertStatement(ic.statement(1));
				location = makeLoc(ic.IF(), elseStmt.location);
			} else { // null for else branch indicates else-less if
				elseStmt = null;
				location = makeLoc(ic.IF(), thenStmt.location);
			}
			return new IfStatement(location, condition, thenStmt, elseStmt);
			
		} else if (statementContext instanceof WhileStmtContext) { // WHILE LPAREN expression RPAREN statement
			WhileStmtContext wc = (WhileStmtContext) statementContext;
			Expression condition = convertExpression(wc.expression());
			Statement body = convertStatement(wc.statement());
			Location loc = makeLoc(wc.WHILE(), body.location);
			return new WhileStatement(loc, condition, body);
			
		} else if (statementContext instanceof DoWhileStmtContext) { // DO statement WHILE LPAREN expression RPAREN SEMICOLON
			DoWhileStmtContext dwc = (DoWhileStmtContext) statementContext;
			Location location = makeLoc(dwc.DO(), dwc.SEMICOLON());
			Statement body = convertStatement(dwc.statement());
			Expression condition = convertExpression(dwc.expression());
			return new DoWhileStatement(location, body, condition);
		
		} else if (statementContext instanceof ReturnStmtContext) { // RETURN expression? SEMICOLON
			ReturnStmtContext rc = (ReturnStmtContext) statementContext;
			Location location = makeLoc(rc.RETURN(), rc.SEMICOLON());
			Expression returnValue = null;
			if (rc.expression() != null) {
				returnValue = convertExpression(rc.expression());
			} 
			return new ReturnStatement(location, returnValue);
		
		} else if (statementContext instanceof ProcCallStmtContext) { // expression LPAREN expressions? RPAREN SEMICOLON
			ProcCallStmtContext pcc = (ProcCallStmtContext) statementContext;
			Expression procedure = convertExpression(pcc.expression());
			Location location = makeLoc(procedure.location, pcc.SEMICOLON());
			List<Expression> arguments = new ArrayList<Expression>();
			if (pcc.expressions() != null) {
				for (ExpressionContext ec : pcc.expressions().expression()) {
					arguments.add(convertExpression(ec));
				}
			}
			return new CallStatement(location, procedure, arguments);
		
		} else if (statementContext instanceof CompoundStmtContext) { //  LBRACE statement* RBRACE  
			CompoundStmtContext cc = (CompoundStmtContext) statementContext;
			Location location = makeLoc(cc.LBRACE(), cc.RBRACE());
			List<Statement> body = new ArrayList<Statement>();
			for (StatementContext sc : cc.statement()) {
				body.add(convertStatement(sc));
			}
			return new CompoundStatement(location, body);
		
		} else {
			throw new RuntimeException("unexpected statement context");
		}
	}
	
	
	
	// ---------- expressions ------------
	private static Expression convertExpression(ExpressionContext expressionContext) {
		if (expressionContext instanceof NullExpContext) {
			NullExpContext nc = (NullExpContext) expressionContext;
			return new NullExpression(makeLoc(nc.NULL()));
		} else if (expressionContext instanceof FalseExpContext) {
			FalseExpContext fc = (FalseExpContext) expressionContext;
			return new BooleanLiteralExpression(makeLoc(fc.FALSE()), false);
		} else if (expressionContext instanceof TrueExpContext) {
			TrueExpContext tc = (TrueExpContext) expressionContext;
			return new BooleanLiteralExpression(makeLoc(tc.TRUE()), true);
			
		} else if (expressionContext instanceof IntExpContext) {
			IntExpContext ic = (IntExpContext)expressionContext;
			TerminalNode lit = ic.INT_LITERAL();
			Location location = makeLoc(lit);
			int i;
			try {
				i = Integer.parseInt(lit.getText());
			} catch (NumberFormatException e) {
				throw new CompilerException(location, "invalid integer literal");
			}
			return new IntLiteralExpression(location, i);
		
		} else if (expressionContext instanceof StringExpContext) {
			StringExpContext sc = (StringExpContext) expressionContext;
			return new StringLiteralExpression(makeLoc(sc.STRING_LITERAL()), sc.getText());
		
		} else if (expressionContext instanceof IdExpContext) {
			IdExpContext ic = (IdExpContext) expressionContext;
			TerminalNode id = ic.ID();
			Location location = makeLoc(id);
			Uid uid = environment.findVariable(location, id.getText());
			return new IdExpression(location, uid);
		
		} else if (expressionContext instanceof TypedExpContext) { // type_annotation expression 
			TypedExpContext tc = (TypedExpContext) expressionContext;
			Type ty = convertTypeAnnotation(tc.type_annotation());
			Expression e = convertExpression(tc.expression());
			return new TypedExpression(e.location, ty, e);
		
		} else if (expressionContext instanceof ParenExpContext) { // LPAREN expression RPAREN    
			ParenExpContext pc = (ParenExpContext) expressionContext;
			return convertExpression(pc.expression());
			
		} else if (expressionContext instanceof AccessExpContext) { // expression DOT ID
			AccessExpContext ac = (AccessExpContext) expressionContext;
			Expression obj = convertExpression(ac.expression());
			return new AccessExpression(makeLoc(obj.location, ac.ID()),obj, ac.ID().getText());
		
		} else if (expressionContext instanceof SubscriptExpContext) { // expression LBRACK expression RBRACK
			SubscriptExpContext sc = (SubscriptExpContext) expressionContext;
			Expression array = convertExpression(sc.expression(0));
			Expression index = convertExpression(sc.expression(1));
			Location location = makeLoc(array.location, sc.RBRACK());
			return new SubscriptExpression(location, array, index);
		
		} else if (expressionContext instanceof FunCallExpContext) { // expression LPAREN expressions? RPAREN  
			FunCallExpContext fcc = (FunCallExpContext) expressionContext;
			Expression f = convertExpression(fcc.expression());
			Location location = makeLoc(f.location, fcc.RPAREN());
			List<Expression> arguments = new ArrayList<Expression>();
			if (fcc.expressions() != null) {
				for (ExpressionContext ec : fcc.expressions().expression()) {
					arguments.add(convertExpression(ec));
				}
			}
			return new CallExpression(location, f, arguments);
		
		} else if (expressionContext instanceof NotExpContext) { // NOT expression
			NotExpContext nc = (NotExpContext) expressionContext;
			Expression operand = convertExpression(nc.expression());
			Location location = makeLoc(nc.NOT(), operand.location);
			return new UnaryExpression(location, Operator.NOT, operand);
		
		} else if (expressionContext instanceof UminusExpContext) { // MINUS expression 
			UminusExpContext umc = (UminusExpContext) expressionContext;
			Expression operand = convertExpression(umc.expression());
			Location location = makeLoc(umc.MINUS(), operand.location);
			return new UnaryExpression(location, Operator.MINUS, operand);
		
		} else if (expressionContext instanceof MultiplicativeExpContext) { // expression (TIMES | DIVIDE | MOD) expression
			MultiplicativeExpContext mc = (MultiplicativeExpContext) expressionContext;
			Expression left = convertExpression(mc.expression(0));
			Expression right = convertExpression(mc.expression(1));
			Location location = makeLoc(left.location, right.location);
			Operator op;
			if (mc.MOD() != null) {
				op = Operator.MOD;
			} else if (mc.DIVIDE() != null) {
				op = Operator.DIVIDE;
			} else if (mc.TIMES() != null) {
				op = Operator.TIMES;
			} else {
				throw new RuntimeException("unexpected multiplicative operator");
			}
			return new BinaryExpression(location, left, op, right);
		
		} else if (expressionContext instanceof AdditiveExpContext) { // expression (PLUS | MINUS) expression
			AdditiveExpContext ac = (AdditiveExpContext) expressionContext;
			Expression left = convertExpression(ac.expression(0));
			Expression right = convertExpression(ac.expression(1));
			Location location = makeLoc(left.location, right.location);
			Operator op;
			if (ac.PLUS() != null) {
				op = Operator.PLUS;
			} else if (ac.MINUS() != null) {
				op = Operator.MINUS;
			} else {
				throw new RuntimeException("unexpected additive operator");
			}
			return new BinaryExpression(location, left, op, right);
		
		} else if (expressionContext instanceof CompareExpContext) { // expression (LT | LTE | GT | GTE | EQ | NEQ) expression 
			CompareExpContext cc = (CompareExpContext) expressionContext;
			Expression left = convertExpression(cc.expression(0));
			Expression right = convertExpression(cc.expression(1));
			Location location = makeLoc(left.location, right.location);
			Operator op;
			if (cc.LT() != null) {
				op = Operator.LT;
			} else if (cc.LTE() != null) {
				op = Operator.LTE;
			} else if (cc.GT() != null) {
				op = Operator.GT;
			} else if (cc.GTE() != null) {
				op = Operator.GTE;
			} else if (cc.EQ() != null) {
				op = Operator.EQ;
			} else if (cc.NEQ() != null) {
				op = Operator.NEQ;
			} else {
				throw new RuntimeException("unexpected comparison operator");
			}
			return new BinaryExpression(location, left, op, right);
		
		} else if (expressionContext instanceof AndExpContext) { // expression AND expression 
			AndExpContext ac = (AndExpContext) expressionContext;
			Expression left = convertExpression(ac.expression(0));
			Expression right = convertExpression(ac.expression(1));
			Location location = makeLoc(left.location, right.location);
			return new BinaryExpression(location, left, Operator.AND, right);
			
		} else if (expressionContext instanceof OrExpContext) { // expression OR expression 
			OrExpContext oc = (OrExpContext) expressionContext;
			Expression left = convertExpression(oc.expression(0));
			Expression right = convertExpression(oc.expression(1));
			Location location = makeLoc(left.location, right.location);
			return new BinaryExpression(location, left, Operator.OR, right);
			
		} else if (expressionContext instanceof ArrayExpContext) { // LBRACK expressions? RBRACK
			ArrayExpContext ac = (ArrayExpContext) expressionContext;
			Location location = makeLoc(ac.LBRACK(), ac.RBRACK());
			List<Expression> elements = new ArrayList<Expression>();
			if (ac.expressions() != null) {
				for (ExpressionContext ec : ac.expressions().expression()) {
					elements.add(convertExpression(ec));
				}
			}
			return new ArrayExpression(location, elements);
			
		} else if (expressionContext instanceof ObjectExpContext) { // LBRACE json_pairs? RBRACE
			ObjectExpContext oc = (ObjectExpContext) expressionContext;
			Location location = makeLoc(oc.LBRACE(), oc.RBRACE());
			SortedMap<String, Expression> members = new TreeMap<String, Expression>();
			if (oc.json_pairs() != null) {
				for (Json_pairContext jpc : oc.json_pairs().json_pair()) {
					String id = jpc.ID().getText();
					if (!members.containsKey(id)) {
						members.put(id, convertExpression(jpc.expression()));
					} else {
						throw new CompilerException(location, "object member '" + id + "' redefined");
					}
				}
			}
			return new ObjectExpression(location, members);
		
		} else if (expressionContext instanceof FunExpContext) { // FUNCTION LPAREN parameters? RPAREN type_annotation LBRACE declaration* statement* RBRACE   
			FunExpContext fc = (FunExpContext) expressionContext;
			Location location = makeLoc(fc.FUNCTION(), fc.RBRACE());
			environment.pushNewScope();
			List<Uid> parameters = new ArrayList<Uid>();
			if (fc.parameters() != null) {
				for (ParameterContext pc : fc.parameters().parameter()) {
					Type ty = convertTypeAnnotation(pc.type_annotation());		
					TerminalNode id = pc.ID();
					Uid uid = environment.defineVariable(makeLoc(id), id.getText(), ty);
					parameters.add(uid);
				}
			}
			Type returnType = convertTypeAnnotation(fc.type_annotation());
			List<Statement> body = new ArrayList<Statement>();
			convertBody(body, fc.declaration(), fc.statement());
			Set<Uid> locals = environment.getUids();
			locals.removeAll(parameters);
			Expression result = new FunctionExpression(location, parameters,
					returnType, locals, body);
			environment.popScope();
			return result;
			
		} else {
			throw new RuntimeException("unexpected expression context");			
		}
		
	}

	
	
	// ----- lvalues generate expressions
	private static Expression convertLvalue(LvalueContext ctx) {
		if (ctx.DOT() != null) { // lvalue DOT ID
			Expression obj = convertLvalue(ctx.lvalue());
			return new AccessExpression(makeLoc(obj.location, ctx.ID()),obj, ctx.ID().getText());
		} else if (ctx.LBRACK() != null) { //  lvalue LBRACK expression RBRACK
			Expression array = convertLvalue(ctx.lvalue());
			Expression index = convertExpression(ctx.expression());
			Location location = makeLoc(array.location, ctx.RBRACK());
			return new SubscriptExpression(location, array, index);
		} else { // ID 
			TerminalNode id = ctx.ID();
			Location location = makeLoc(id);
			Uid uid = environment.findVariable(location, id.getText());
			return new IdExpression(location, uid);
		}
	}
	
	
	
	// ----------- types -----------
	private static Type convertTypeAnnotation(Type_annotationContext typeAnnotationContext) {
		return convertType(typeAnnotationContext.type());
	}

	
	private static Type convertType(TypeContext tc) {
		if (tc.TYPE_BOOLEAN() != null) {
			return Type.BOOLEAN_TYPE;
		} else if (tc.TYPE_INTEGER() != null) {
			return Type.INT_TYPE;
		} else if (tc.TYPE_STRING() != null) {
			return Type.STRING_TYPE;
		} else if (tc.TYPE_VOID() != null) {
			return new TupleType(new ArrayList<Type>());
		} else if (tc.TYPE_ARRAY() != null) { // TYPE_ARRAY LBRACK type RBRACK
			return new ArrayType(convertType(tc.type()));
		} else if (tc.TYPE_OBJECT() != null) { // TYPE_OBJECT LBRACE id_type_pairs? RBRACE
			SortedMap<String, Type> memberTypes = new TreeMap<String, Type>();
			if (tc.id_type_pairs() != null) {
				for (Id_type_pairContext ipc : tc.id_type_pairs()
						.id_type_pair()) {
					String id = ipc.ID().getText();
					if (!memberTypes.containsKey(id)) {
						memberTypes.put(id, convertType(ipc.type()));
					} else {
						throw new CompilerException(null, "object member '" + id + "' redefined");
					}
				}
			}
			return new ObjectType(memberTypes);
		} else if (tc.TYPE_FUNCTION() != null) { // TYPE_FUNCTION LPAREN types? SEMICOLON type RPAREN
			List<Type> parameterTypes = new ArrayList<Type>();
			if (tc.types() != null) {
				for (TypeContext ptc : tc.types().type()) {
					parameterTypes.add(convertType(ptc));
				}
			}
			Type inType;
			if (parameterTypes.size() == 0) {
				inType = VOID_TYPE;
			} else if (parameterTypes.size() == 1) {
				inType = parameterTypes.get(0);
			} else {
				inType = new TupleType(parameterTypes);
			}
			Type outType = convertType(tc.type());
			return new FunctionType(inType, outType);
		} else {
			throw new RuntimeException("unexpected syntactic type");
		}
	}
	
	
	
	
	
	
	private static final Expression ONE = new IntLiteralExpression(null, 1);

	

}
