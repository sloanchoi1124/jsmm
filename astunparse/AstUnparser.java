
package astunparse;


import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ast.Ast.*;
import ast.SymbolTable;
import ast.Type;
import static ast.Type.*;
import util.StringHelper;
import util.Uid;



public class AstUnparser {
	
	public final static int TAB_SIZE = 2;
	
	private static StringBuilder builder;
	private static int indentLevel;
	private static SymbolTable symbolTable;
	
	
	// ------------------------------- PROGRAMS ------------------------------

	public static String unparse(Program program) {
		builder = new StringBuilder();
		indentLevel = 0;
		symbolTable = program.symbolTable;
	
		// for simplicity, variable declarations are all uninitialized globals
		// if location listed as null, assumption is standard library so variable not printed
		for (Uid uid : program.globals) {
			if (symbolTable.getLocation(uid) == null) {
				continue;
			}
			print("var ");
			Type ty = symbolTable.getType(uid);
			if (ty != null) {
				unparseType(ty);
				print(" ");
			} else {
				print("/*:#void:*/ ");
			}
			unparseId(uid);
			print(";");
			if (ty == null) {
				print(" // #void = unspecified type");
			}
			print("\n");
		}
		for (Statement stmt : program.body) {
			unparseStatement(stmt);
		}
		return builder.toString();
	}

	
	
	// ------------------------------- STATEMENTS ------------------------------

	private static void unparseStatement(Statement statement) {
		indent();
		if (statement instanceof AssignmentStatement) {
			AssignmentStatement assignment = (AssignmentStatement) statement;
			unparseExpression(assignment.lhs);
			print(" = ");
			unparseExpression(assignment.rhs);
			finish(statement);
			
		} else if (statement instanceof BreakStatement) {
			print("break;\n");
			
		} else if (statement instanceof CallStatement) {
			CallStatement call = (CallStatement) statement;
			unparseExpression(call.procedure);
			print("(");
			unparseExpressions(call.arguments);
			print(")");
			finish(statement);
			
		} else if (statement instanceof CompoundStatement) {
			CompoundStatement compound = (CompoundStatement) statement;
			print("{\n");
			pushIndent();
			for (Statement s : compound.body) {
				unparseStatement(s);
			}
			popIndent();
			indent();
			print("}\n");
		
		} else if (statement instanceof ContinueStatement) {
			print("continue;\n");
		
		} else if (statement instanceof IfStatement) {
			IfStatement ifThen = (IfStatement) statement;
			print("if (");
			unparseExpression(ifThen.condition);
			print(")\n");
			printSubstatement(ifThen.thenStatement);
			Statement elseStatement = ifThen.elseStatement;
			if (elseStatement != null) {
				print("else\n");
				printSubstatement(ifThen.elseStatement);
			}
			
		} else if (statement instanceof ReturnStatement) {
			ReturnStatement ret = (ReturnStatement) statement;
			print("return");
			if (ret.returnValue != null) {
				print(" ");
				unparseExpression(ret.returnValue);
			}
			finish(statement);
			
		} else if (statement instanceof WhileStatement) {
			WhileStatement wh = (WhileStatement) statement;
			print("while (");
			unparseExpression(wh.condition);
			print(")\n");
			printSubstatement(wh.body);

		} else if (statement instanceof DoWhileStatement) {
			DoWhileStatement dws = (DoWhileStatement) statement;
			print("do");
			printSubstatement(dws.body);
			indent();
			print("while (");
			unparseExpression(dws.condition);
			print(")");
			finish(statement);
		
		} else {
			throw new RuntimeException("unexpected Statement class: " + statement.getClass());
		}
		
	}
	
	
	private static void printSubstatement(Statement statement) {
		if (statement instanceof CompoundStatement) {
			unparseStatement(statement);
		} else {
			pushIndent();
			unparseStatement(statement);
			popIndent();
		}	
	}	
	
	
	
	// ------------------------------- EXPRESSIONS ------------------------------
	
	private static void unparseExpressions(List<Expression> expressions) {
		int n = expressions.size();
		for (int i = 0; i < n-1; i++) {
			unparseExpression(expressions.get(i));
			print(", ");
		}
		if (n > 0) {
			unparseExpression(expressions.get(n-1));
		}
	}
	
	
	private static void unparseExpression(Expression expression) {
		if (expression instanceof AccessExpression) {
			AccessExpression access = (AccessExpression) expression;
			parenthesize(access.object);
			print(".");
			print(access.member);
			
		} else if (expression instanceof ArrayExpression) {
			ArrayExpression array = (ArrayExpression) expression;
			print("[");
			unparseExpressions(array.elements);
			print("]");
		
		} else if (expression instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) expression;
			parenthesize(binary.left);
			print(" " + binary.operator + " ");
			parenthesize(binary.right);
		
		} else if (expression instanceof BooleanLiteralExpression) {
			BooleanLiteralExpression bool = (BooleanLiteralExpression) expression;
			print("" + bool.value);
			
		} else if (expression instanceof CallExpression) {
			CallExpression call = (CallExpression) expression;
			parenthesize(call.function);
			print("(");
			unparseExpressions(call.arguments);
			print(")");
		
		} else if (expression instanceof FunctionExpression) {
			FunctionExpression function = (FunctionExpression) expression;
			print("function (");
			unparseParameters(function.parameters);
			print(") ");
			unparseType(function.returnType);
			print(" {\n");
			pushIndent();
			// as with global variables, local declarations are uninitialized
			for (Uid uid : function.locals) {
				indent();
				print("var ");
				Type ty = symbolTable.getType(uid);
				if (ty != null) {
					unparseType(ty);
					print(" ");
				} else {
					print("/*:#void:*/ ");
				}
				unparseId(uid);
				print(";");
				if (ty == null) {
					print(" // #void = unspecified type");
				}
				print("\n");
			}
			for (Statement s : function.body) {
				unparseStatement(s);
			}
			popIndent();
			print("}");
		
		} else if (expression instanceof IdExpression) {
			IdExpression id = (IdExpression) expression;
			unparseId(id.uid);
			
		} else if (expression instanceof IntLiteralExpression) {
			IntLiteralExpression intLiteral = (IntLiteralExpression) expression;
			print("" + intLiteral.value);
		
		} else if (expression instanceof NullExpression) {
			print("null");
			
		} else if (expression instanceof ObjectExpression) {
			ObjectExpression obj = (ObjectExpression) expression;
			print("{");
			unparseMembers(obj.members);
			print("}");
			
		} else if (expression instanceof StringLiteralExpression) {
			StringLiteralExpression stringLiteral = (StringLiteralExpression) expression;
			print(StringHelper.escape(stringLiteral.value)); // includes quotes
			
		} else if (expression instanceof SubscriptExpression) {
			SubscriptExpression subscript = (SubscriptExpression) expression;
			parenthesize(subscript.array);
			print("[");
			unparseExpression(subscript.index);
			print("]");
			
		} else if (expression instanceof TypedExpression) {
			TypedExpression typed = (TypedExpression) expression;
			unparseType(typed.type);
			print(" ");
			unparseExpression(typed.expression); // no need to call parenthesize as this is lowest precedence
			
		} else if (expression instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) expression;
			print("" + unary.operator);
			parenthesize(unary.operand);
			
		} else {
			throw new RuntimeException("unexpected Expression class: " + expression.getClass());
		}
	}
	
	
	private static void parenthesize(Expression expression) {
		if (expression instanceof SubscriptExpression ||
				expression instanceof CallExpression ||
				expression instanceof UnaryExpression ||
				expression instanceof BinaryExpression ||
				expression instanceof TypedExpression)
				 {
			print("(");
			unparseExpression(expression);
			print(")");
		} else {
			unparseExpression(expression);
		}
	}

	
	
	private static void unparseParameters(List<Uid> parameters) {
		int n = parameters.size();
		for (int i = 0; i < n-1; i++) {
			Uid uid = parameters.get(i);
			unparseType(symbolTable.getType(uid));
			print(" ");
			unparseId(uid);
			print(", ");
		}
		if (n > 0) {
			Uid uid = parameters.get(n-1);
			unparseType(symbolTable.getType(uid));
			print(" ");
			unparseId(uid);
		}
	}
	
	
	private static void unparseMembers(Map<String, Expression> memberMap) {
		Set<Map.Entry<String, Expression>> members = memberMap.entrySet();
		Iterator<Map.Entry<String, Expression>> it = members.iterator();
		Map.Entry<String, Expression> p;
		if (it.hasNext()) {
			p = it.next();
			print(p.getKey());
			print(": ");
			unparseExpression(p.getValue());
		}
		while (it.hasNext()) {
			p = it.next();
			print(", ");
			print(p.getKey());
			print(": ");
			unparseExpression(p.getValue());
		}
	}
	
	
	private static void unparseId(Uid uid) {
		print(symbolTable.getName(uid));
		// assumption is null location means defined elsewhere (i.e. standard library)
		// so leave name unadorned
		if (symbolTable.getLocation(uid) != null) {
			print("" + uid);
		}
	}
	
	
	// ------------------------------- TYPES ------------------------------
	
	private static void unparseType(Type type) {
		print("/*:");
		print(typeToString(type));
		print(":*/");
	}
	
	
	public static String typeToString(Type type) {
		if (type instanceof BooleanType) {
			return "#bool";
		} else if (type instanceof IntType) {
			return "#int";
		} else if (type instanceof StringType) {
			return "#string";
		} else if (type instanceof AnyType) {
			return "?";
		} else if (type instanceof NullType) {
			return "#null"; 
		} else if (type instanceof ArrayType) {
			ArrayType arrayType = (ArrayType)type;
			return "#arr[" + typeToString(arrayType.innerType) + "]";
		} else if (type instanceof ObjectType) {
			ObjectType objectType = (ObjectType) type;
			String s = "#obj{";
			Set<Map.Entry<String, Type>> memberTypes = objectType.memberTypes.entrySet();
			Iterator<Map.Entry<String, Type>> it = memberTypes.iterator();
			Map.Entry<String, Type> p;
			if (it.hasNext()) {
				p = it.next();
				s += p.getKey();
				s += ": ";
				s += typeToString(p.getValue());
			}
			while (it.hasNext()) {
				p = it.next();
				s += ", ";
				s += p.getKey();
				s += (": ");
				s += typeToString(p.getValue());
			}
			s += "}";
			return s;
		} else if (type instanceof FunctionType) {
			FunctionType functionType = (FunctionType) type;
			String s = "#fun(";
			s += typeToString(functionType.inType);
			s += "; ";
			s += typeToString(functionType.outType);
			s += ")";
			return s;
		} else if (type instanceof TupleType) {
			TupleType tupleType = (TupleType) type;
			String s = "";
			int n = tupleType.types.size();
			if (n == 0) {
				return "#void";
			}
			for (int i = 0; i < n-1; i++) {
				s += typeToString(tupleType.types.get(i));
				s += ", ";
			}
			if (n > 0) {
				s += typeToString(tupleType.types.get(n-1));
			}
			return s;
		} else {
			throw new RuntimeException("unexpected Type class: " + type.getClass());
		}
	}
	
	
	// ------------------------------- HELPERS ------------------------------
	
		
	private static void pushIndent() { 
		indentLevel++;
	}
	
	private static void popIndent() {
		if (indentLevel > 0) {
			indentLevel--;
		}
		else {
			System.out.println("WARNING - UNDERFLOW!!!\n" + builder);
			throw new RuntimeException("indentation stack underflow");
		}
	}
	
	private static void indent() {
		for (int i = 0; i < indentLevel; i++) {
			for (int j = 0; j < TAB_SIZE; j++) {
				builder.append(' ');
			}
		}
	}
	
	private static void finish(Statement statement) {
		print("; // " + statement.location + "\n");
	}
			
	private static void print(String s) {
		builder.append(s);
	}
}
