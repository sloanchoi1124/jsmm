
package typecheck;


import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

import static ast.Ast.*;
import ast.Operator;
import ast.SymbolTable;
import ast.Type;
import static ast.Type.*;
import util.CompilerError;
import util.Uid;


public class TypeCheck {

	private static Map<Uid, Type> expressionTypeMap;
	private static SymbolTable symbolTable;
	private static Deque<Type> returnTypeStack;
	// the hasReturnedStack serves one, somewhat clunky purpose: 
	// to catch the case where a non-void function has no return at all
	// observe that the actual problem of spotted functions that _may_ 
	// return correct type of value is trickier and requires some form
	// of control-flow analysis; that could be handled at next stage
	private static Deque<Boolean> hasReturnedStack;
	
	public static Map<Uid, Type> typeCheck(Program program) {
		expressionTypeMap = new HashMap<Uid, Type>();
		symbolTable = program.symbolTable;
		returnTypeStack = new ArrayDeque<Type>();
		hasReturnedStack = new ArrayDeque<Boolean>();
		for (Statement statement : program.body) {
			checkStatement(statement);
		}
		CompilerError.flush();
		return expressionTypeMap;
	}

	
	private static void checkStatement(Statement statement) {
		if (statement instanceof AssignmentStatement) {
			AssignmentStatement assignment = (AssignmentStatement) statement;
			Type leftTy = checkExpression(assignment.lhs);
			Type rightTy = checkExpression(assignment.rhs);
			TypeUtil.match(statement.location, "assignment", leftTy, rightTy);
			if (TypeUtil.isNonspecific(leftTy)) {
				// this can only happen if lhs is an id and this is its initial assignment
				if (assignment.lhs instanceof IdExpression) {
					IdExpression id = (IdExpression)(assignment.lhs);
					if (TypeUtil.isNonspecific(rightTy)) {
						TypeUtil.error(assignment.location, "fully specified type cannot be inferred for assignment");
					}
					symbolTable.updateType(id.uid, rightTy);
				} else {
					throw new RuntimeException("unexpected untyped lvalue");
				}
			}
			
		} else if (statement instanceof BreakStatement || statement instanceof ContinueStatement) {
			// nothing to do; whether or not these are in a loop is for a later phase of the compiler
			
		} else if (statement instanceof CallStatement) {
			CallStatement call = (CallStatement) statement;
			Type ty = checkExpression(call.procedure);
			if (ty instanceof FunctionType) {
				FunctionType procTy = (FunctionType) ty;
				if (TypeCompare.matches(procTy.outType, VOID_TYPE)) {
					List<Type> formalTys = TypeUtil.tupleize(procTy.inType);
					List<Expression> arguments = call.arguments;
					if (formalTys.size() == arguments.size()) {
						for (int i = 0; i < formalTys.size(); i++) {
							Expression arg = arguments.get(i);
							Type actualTy = checkExpression(arg);
							TypeUtil.match(arg.location, "argument", formalTys.get(i), actualTy);
						}
					}				
					else {
						TypeUtil.error(call.location, "aritys don't match");
					}
				}
				else {
					TypeUtil.error(call.procedure.location, "procedure expected; function with non-void return type found");
				}
			}
			else {
				TypeUtil.error(call.procedure.location, "function type expected");
			}
			
		} else if (statement instanceof CompoundStatement) {
			CompoundStatement compound = (CompoundStatement) statement;
			for (Statement s : compound.body) {
				checkStatement(s);
			}
		
		} else if (statement instanceof IfStatement) {
			IfStatement ifThen = (IfStatement) statement;
			Type condTy = checkExpression(ifThen.condition);
			TypeUtil.match(ifThen.condition.location, "if condition", BOOLEAN_TYPE, condTy);
			checkStatement(ifThen.thenStatement);
			if (ifThen.elseStatement != null) {
				checkStatement(ifThen.elseStatement);
			}
			
		} else if (statement instanceof ReturnStatement) {
			ReturnStatement ret = (ReturnStatement) statement;
			Type retTy = VOID_TYPE;
			if (ret.returnValue != null) {
				retTy = checkExpression(ret.returnValue);
				hasReturnedStack.pop();
				hasReturnedStack.push(true);
			}
			if (!returnTypeStack.isEmpty()) {
				Type expectedTy = returnTypeStack.peek();
				if (TypeCompare.matches(VOID_TYPE, expectedTy) &&
						!TypeCompare.matches(VOID_TYPE, retTy)) {
					TypeUtil.error(ret.location, 
								"non-void return in procedure");
				} else if (!TypeCompare.matches(VOID_TYPE, expectedTy) &&
						TypeCompare.matches(VOID_TYPE, retTy)) {
					TypeUtil.error(ret.location, 
							"return without expression in non-void function");
				} else {
					if (ret.returnValue != null) {
						TypeUtil.match(ret.returnValue.location,
								"return expression", expectedTy, retTy);
					}
					// otherwise nothing to do because void matches void
				}
			} else {
				CompilerError.error("Return statement not allowed in global scope " + 
					ret.location);
			}
		
		} else if (statement instanceof WhileStatement) {
			WhileStatement wh = (WhileStatement) statement;
            TypeUtil.match(wh.condition.location, "while condition", 
                           BOOLEAN_TYPE, checkExpression(wh.condition));
			checkStatement(wh.body);

		} else if (statement instanceof DoWhileStatement) {
			DoWhileStatement dws = (DoWhileStatement) statement;
			checkStatement(dws.body);
            TypeUtil.match(dws.condition.location, "do-while condition", 
                           BOOLEAN_TYPE, checkExpression(dws.condition));
		
		} else {
			throw new RuntimeException("unexpected Statement class: " + statement.getClass());
		}
		
	}
	
	
	private static Type checkExpression(Expression e) {
		Type t = checkExpressionAux(e);
		expressionTypeMap.put(e.nodeId, t);
		return t;
	}
	
	
	private static Type checkExpressionAux(Expression expression) {
		if (expression instanceof BooleanLiteralExpression) {
			return BOOLEAN_TYPE;

		} else if (expression instanceof IntLiteralExpression) {
			return INT_TYPE;

		} else if (expression instanceof StringLiteralExpression) {
			return STRING_TYPE;

		} else if (expression instanceof NullExpression) {
			return NULL_TYPE;

		} else if (expression instanceof IdExpression) {
			IdExpression id = (IdExpression) expression;
			Type ty = symbolTable.getType(id.uid);
			if (ty == null) {
				ty = ANY_TYPE;
			}
			return ty;
			
		} else if (expression instanceof AccessExpression) {
			AccessExpression access = (AccessExpression) expression;
			// check that access.object
			// a) is an object and b) has access.member in its type
			Type ty = checkExpression(access.object);
			if (ty instanceof ObjectType) {
				ObjectType objTy = (ObjectType)ty;
				Type memberTy = objTy.memberTypes.get(access.member);
				if (memberTy != null) {
					return memberTy;
				}
				TypeUtil.error(access.location, "object member label '" + access.member + "' not found");
				return INT_TYPE; // try and prevent cascade of type errors
			} else {
				TypeUtil.error(access.object.location, "member access disallowed for non-object expressions");
				return INT_TYPE; // try and prevent cascade of type errors
			}

		} else if (expression instanceof ArrayExpression) {
			ArrayExpression array = (ArrayExpression) expression;
			List<Expression> elements = array.elements;
			if (elements.isEmpty()) {
				return new ArrayType(ANY_TYPE);
			} else {
				Type baseType = checkExpression(elements.get(0));
				for (int i = 1; i < elements.size(); i++) {
					Expression e = elements.get(i);
					Type nextType = checkExpression(e);
					TypeUtil.match(e.location, "array element", 
							baseType, nextType);
					baseType = TypeUtil.join(baseType, nextType);
				}
				return new ArrayType(baseType);
			}
			
		} else if (expression instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) expression;
			Type leftTy = checkExpression(binary.left);
			Type rightTy = checkExpression(binary.right);
			Type expectedTy = null;
			Type resultTy = null;
			switch (binary.operator) {
			case AND:
			case OR:
				expectedTy = BOOLEAN_TYPE;
				resultTy = BOOLEAN_TYPE;
				break;
			case PLUS:
				if (TypeCompare.matches(STRING_TYPE, leftTy)) {
					expectedTy = STRING_TYPE;
					resultTy = STRING_TYPE;
					break;
				}
				// otherwise, intentionally fall through
			case MINUS:
			case TIMES:
			case DIVIDE:
			case MOD:
				expectedTy = INT_TYPE;
				resultTy = INT_TYPE;
				break;
			case GT:
			case GTE:
			case LT:
			case LTE:
				expectedTy = INT_TYPE;
				resultTy = BOOLEAN_TYPE;
				break;
			case EQ:
			case NEQ:
				resultTy = BOOLEAN_TYPE;
				break;
			default:
				throw new RuntimeException("Invalid operator " + binary.operator +
						" in binary expression");
			}
			if (binary.operator == Operator.EQ || binary.operator == Operator.NEQ) {
				if (TypeUtil.isAtomic(leftTy)) {
					TypeUtil.match(binary.right.location, "right operand for " +
							binary.operator, leftTy, rightTy);
				}
				else {
					TypeUtil.error(binary.location, "invalid types for comparsion");
				}
			}
			else {
				TypeUtil.match(binary.left.location, "left operand for " + 
						binary.operator, expectedTy, leftTy);
				TypeUtil.match(binary.right.location, "right operand for " +
						binary.operator, expectedTy, rightTy);
			}
			return resultTy;

		} else if (expression instanceof CallExpression) {
			CallExpression call = (CallExpression) expression;
			Type ty = checkExpression(call.function);
			if (ty instanceof FunctionType) {
				FunctionType funTy = (FunctionType) ty;
				if (!TypeCompare.matches(funTy.outType, VOID_TYPE)) {
					List<Type> formalTys = TypeUtil.tupleize(funTy.inType);
					List<Expression> arguments = call.arguments;
					if (formalTys.size() == arguments.size()) {
						for (int i = 0; i < formalTys.size(); i++) {
							Expression arg = arguments.get(i);
							Type actualTy = checkExpression(arg);
							TypeUtil.match(arg.location, "argument", formalTys.get(i), actualTy);
						}
					}				
					else {
						TypeUtil.error(call.location, "aritys don't match");
					}
					return funTy.outType;
				}
				else {
					TypeUtil.error(call.function.location, "function expected; procedure (void return type) found");
					return INT_TYPE; // try and prevent cascade of type errors
				}
			}
			else {
				TypeUtil.error(call.function.location, "function type expected");
				return INT_TYPE; // try and prevent cascade of type errors
			}
		
		} else if (expression instanceof FunctionExpression) {
			FunctionExpression function = (FunctionExpression) expression;
			returnTypeStack.push(function.returnType);
			hasReturnedStack.push(false);
			for (Statement s : function.body) {
				checkStatement(s);
			}
			if (!TypeCompare.matches(function.returnType, VOID_TYPE) &&
					!hasReturnedStack.peek()) {
				TypeUtil.error(function.location, "non-void function lacks return");
			}
			hasReturnedStack.pop();
			returnTypeStack.pop();
			List<Type> formalTys = new ArrayList<Type>();
			for (Uid uid : function.parameters) {
				Type argTy = symbolTable.getType(uid);
				if (argTy != null) {
					formalTys.add(argTy);
				} else {
					throw new RuntimeException("missing type for function parameter");
				}
			}
			Type inTy;
			if (formalTys.size() == 0) {
				inTy = VOID_TYPE;
			} else if (formalTys.size() == 1) {
				inTy = formalTys.get(0);
			} else {
				inTy = new TupleType(formalTys);
			}
			return new FunctionType(inTy, function.returnType);
		
		} else if (expression instanceof ObjectExpression) {
			ObjectExpression obj = (ObjectExpression) expression;
			SortedMap<String, Type> memberTypes = new TreeMap<String, Type>();
			Iterator<Map.Entry<String, Expression>> it = obj.members.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, Expression> p = it.next();
				memberTypes.put(p.getKey(), checkExpression(p.getValue()));
			}
			return new ObjectType(memberTypes);
			
		} else if (expression instanceof SubscriptExpression) {
			SubscriptExpression subscript = (SubscriptExpression) expression;
			Type baseTy = checkExpression(subscript.array);
			Type indexTy = checkExpression(subscript.index);
			TypeUtil.match(subscript.index.location, "array index", INT_TYPE, indexTy);
			if (baseTy instanceof ArrayType) {
				ArrayType arrayTy = (ArrayType) baseTy;
				return arrayTy.innerType;
			}
			else {
				TypeUtil.error(subscript.array.location, "array type expected");
				return INT_TYPE; // try and prevent cascade of type errors
			}
			
		} else if (expression instanceof TypedExpression) {
			TypedExpression typed = (TypedExpression) expression;
			Type implicitTy = checkExpression(typed.expression);
			TypeUtil.match(typed.location, "typed", typed.type, implicitTy);
			return typed.type;
			
		} else if (expression instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) expression;
			Type operandTy = checkExpression(unary.operand);
			switch (unary.operator) {
			case NOT:
				TypeUtil.match(unary.operand.location, "logical negation", 
						BOOLEAN_TYPE, operandTy);
				return BOOLEAN_TYPE;
			case MINUS:
				TypeUtil.match(unary.operand.location, "arithmetic negation",
						INT_TYPE, operandTy);
				return INT_TYPE;
			default:
				throw new RuntimeException("Invalid operator in unary-operator expression");
			}
			
		} else {
			throw new RuntimeException("unexpected Expression class: " + expression.getClass());
		}
	}
	
}

