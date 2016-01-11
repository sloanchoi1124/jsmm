package ast;

import java.util.List;
import java.util.SortedMap;
import java.util.Set;

import util.Location;
import util.Uid;

public abstract class Ast {
	
	public static class Program {
		public final SymbolTable symbolTable; // intended to be (mostly) immutable
		public final Set<Uid> globals;
		public final List<Statement> body;
		public Program(SymbolTable symbolTable, Set<Uid> globals, List<Statement> body) {
			this.symbolTable = symbolTable;
			this.globals = globals;
			this.body = body;
		}
	}
	
	public static abstract class AstNode {
		public final Uid nodeId;
		public final Location location;
		public AstNode(Location location) {
			this.nodeId = new Uid();
			this.location = location;
		}
	}
	
	
	public static abstract class Statement extends AstNode {
		public Statement(Location location) {
			super(location);
		}
	}
	
	public static final class AssignmentStatement extends Statement {
		public final Expression lhs, rhs;
		public AssignmentStatement(Location location, Expression lhs, Expression rhs) {
			super(location);
			this.lhs = lhs;
			this.rhs = rhs;
		}
	}
	
	public static final class IfStatement extends Statement {
		public final Expression condition;
		public final Statement thenStatement, elseStatement;
		public IfStatement(Location location, Expression condition, Statement thenStatement, Statement elseStatement) {
			super(location);
			this.condition = condition;
			this.thenStatement = thenStatement;
			this.elseStatement = elseStatement;
		}
	}
	
	public static final class WhileStatement extends Statement {
		public final Expression condition;
		public final Statement body;
		public WhileStatement(Location location, Expression condition, Statement body) {
			super(location);
			this.condition = condition;
			this.body = body;
		}
	}
	
	public static final class DoWhileStatement extends Statement {
		public final Statement body;
		public final Expression condition;
		public DoWhileStatement(Location location, Statement body, Expression condition) {
			super(location);
			this.body = body;
			this.condition = condition;
		}
	}
	
	public static final class BreakStatement extends Statement {
		public BreakStatement(Location location) {
			super(location);
		}
	}

	public static final class ContinueStatement extends Statement {
		public ContinueStatement(Location location) {
			super(location);
		}
	}
	
	public static final class ReturnStatement extends Statement {
		public final Expression returnValue;
		public ReturnStatement(Location location, Expression returnValue) {
			super(location);
			this.returnValue = returnValue;
		}
	}
	
	public static final class CallStatement extends Statement {
		public final Expression procedure;
		public final List<Expression> arguments;
		public CallStatement(Location location, Expression procedure, List<Expression> arguments) {
			super(location);
			this.procedure = procedure;
			this.arguments = arguments;
		}
	}
	
	public static final class CompoundStatement extends Statement {
		public final List<Statement> body;
		public CompoundStatement(Location location, List<Statement> body) {
			super(location);
			this.body = body;
		}
	}
	
	
	
	public static abstract class Expression extends AstNode {
		public Expression(Location location) {
			super(location);
		}
	}
	
	public static final class AccessExpression extends Expression {
		public final Expression object;
		public final String member;
		public AccessExpression(Location location, Expression object, String member) {
			super(location);
			this.object = object;
			this.member = member;
		}
	}
	
	public static final class SubscriptExpression extends Expression {
		public final Expression array, index;
		public SubscriptExpression(Location location, Expression array, Expression index) {
			super(location);
			this.array = array;
			this.index = index;
		}
	}
	
	public static final class CallExpression extends Expression {
		public final Expression function;
		public final List<Expression> arguments;
		public CallExpression(Location location, Expression function, List<Expression> arguments) {
			super(location);
			this.function = function;
			this.arguments = arguments;
		}
	}

	public static final class UnaryExpression extends Expression {
		public final Operator operator;
		public final Expression operand;
		public UnaryExpression(Location location, Operator operator, Expression operand) {
			super(location);
			this.operator = operator;
			this.operand = operand;
		}
	}
	
	public static final class BinaryExpression extends Expression {
		public final Expression left, right;
		public final Operator operator;
		public BinaryExpression(Location location, Expression left, Operator operator, Expression right) {
			super(location);
			this.left = left;
			this.operator = operator;
			this.right = right;
		}
	}

	public static final class ArrayExpression extends Expression {
		public final List<Expression> elements;
		public ArrayExpression(Location location, List<Expression> elements) {
			super(location);
			this.elements = elements;
		}
	}
	
	public static final class ObjectExpression extends Expression {
		public final SortedMap<String, Expression> members;
		public ObjectExpression(Location location, SortedMap<String, Expression> members) {
			super(location);
			this.members = members;
		}
	}

	public static final class FunctionExpression extends Expression {
		public final List<Uid> parameters;
		public final Type returnType;
		public final Set<Uid> locals; // separate from parameters
		public final List<Statement> body;
		public FunctionExpression(Location location, 
									List<Uid> parameters,
									Type returnType,
									Set<Uid> locals,
									List<Statement> body) {
			super(location);
			this.parameters = parameters;
			this.returnType = returnType;
			this.locals = locals;
			this.body = body;
		}
	}

	public static final class TypedExpression extends Expression {
		public final Type type;
		public final Expression expression;
		public TypedExpression(Location location, Type type, Expression expression) {
			super(location);
			this.type = type;
			this.expression = expression;
		}
	}
	
	public static final class NullExpression extends Expression {
		public NullExpression(Location location) {
			super(location);
		}
	}

	public static final class BooleanLiteralExpression extends Expression {
		public final boolean value;
		public BooleanLiteralExpression(Location location, boolean value) {
			super(location);
			this.value = value;
		}
	}
	
	public static final class IntLiteralExpression extends Expression {
		public final int value;
		public IntLiteralExpression(Location location, int value) {
			super(location);
			this.value = value;
		}
	}
	
	public static final class StringLiteralExpression extends Expression {
		public final String value;
		public StringLiteralExpression(Location location, String value) {
			super(location);
			this.value = value;
		}
	}

	public static final class IdExpression extends Expression {
		public final Uid uid;
		public IdExpression(Location location, Uid uid) {
			super(location);
			this.uid = uid;
		}
	}

}
