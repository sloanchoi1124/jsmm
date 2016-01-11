package ir;

// TODO: add support for closures

import java.util.List;
import java.util.Map;
import java.util.Set;

import util.Label;
import util.Temp;




public abstract class Ir {
	
	public static class IrProgram {
		public final Set<Temp> globals;
		public final Map<Label, IrFunction> functions;
        public final Map<Label, String> stringLiterals;
        public IrProgram(Set<Temp> globals, Map<Label, IrFunction> functions, Map<Label, String> stringLiterals) {
        	this.globals = globals;
        	this.functions = functions;
        	this.stringLiterals = stringLiterals;
        }
	}
	
	
	public static class IrBlockProgram {
		public final Set<Temp> globals;
		public final Map<Label, IrBlockFunction> functions;
        public final Map<Label, String> stringLiterals;
        public IrBlockProgram(Set<Temp> globals,
        		Map<Label, IrBlockFunction> functions, 
        		Map<Label, String> stringLiterals) {
        	this.globals = globals;
        	this.functions = functions;
        	this.stringLiterals = stringLiterals;
        }
	}
	
	
	public static class IrTraceProgram {
		public final Set<Temp> globals;
		public final Map<Label, IrTraceFunction> functions;
        public final Map<Label, String> stringLiterals;
        public IrTraceProgram(Set<Temp> globals,
        		Map<Label, IrTraceFunction> functions, 
        		Map<Label, String> stringLiterals) {
        	this.globals = globals;
        	this.functions = functions;
        	this.stringLiterals = stringLiterals;
        }
	}
	
	//
	public static class IrTraceProgram2 {
		public final Set<Temp> globals;
		public final Map<Label, FlatIrTraceFunction> functions;
        public final Map<Label, String> stringLiterals;
        public IrTraceProgram2(Set<Temp> globals,
        		Map<Label, FlatIrTraceFunction> functions, 
        		Map<Label, String> stringLiterals) {
        	this.globals = globals;
        	this.functions = functions;
        	this.stringLiterals = stringLiterals;
        }
	}
	
	
	public static class IrFunction {
		public final Label functionLabel;
		public final List<Temp> parameters;
		public final List<IrStatement> body;
		public final Label bodyLabel;
		public final Label exitLabel;
		public final Temp returnValue;
		public IrFunction(Label functionLabel, List<Temp> parameters,
				List<IrStatement> body, Label bodyLabel, Label exitLabel,
				Temp returnValue) {
			this.functionLabel = functionLabel;
			this.parameters = parameters;
			this.body = body;
			this.bodyLabel = bodyLabel;
			this.exitLabel = exitLabel;
			this.returnValue = returnValue;
		}
	}
	
	
	public static class IrBlockFunction {
		public final Label functionLabel;
		public final List<Temp> parameters;
		public final Map<Label, List<IrStatement>> bodyBlocks;
		public final Label bodyLabel;
		public final Label exitLabel;
		public final Temp returnValue;
		public IrBlockFunction(Label functionLabel, List<Temp> parameters,
				Map<Label, List<IrStatement>> bodyBlocks, Label bodyLabel, Label exitLabel,
				Temp returnValue) {
			this.functionLabel = functionLabel;
			this.parameters = parameters;
			this.bodyBlocks = bodyBlocks;
			this.bodyLabel = bodyLabel;
			this.exitLabel = exitLabel;
			this.returnValue = returnValue;
		}
	}
	
	
	public static class IrTraceFunction {
		public final Label functionLabel;
		public final List<Temp> parameters;
		public final List<List<List<IrStatement>>> bodyTraces;
		public final Label bodyLabel;
		public final Label exitLabel;
		public final Temp returnValue;
		public IrTraceFunction(Label functionLabel, List<Temp> parameters,
				List<List<List<IrStatement>>> bodyTraces, Label bodyLabel, Label exitLabel,
				Temp returnValue) {
			this.functionLabel = functionLabel;
			this.parameters = parameters;
			this.bodyTraces = bodyTraces;
			this.bodyLabel = bodyLabel;
			this.exitLabel = exitLabel;
			this.returnValue = returnValue;
		}
	}
	
	//
	public static class FlatIrTraceFunction {
		public final Label functionLabel;
		public final List<Temp> parameters;
		public final List<IrStatement> flatBodyTraces;
		public final Label bodyLabel;
		public final Label exitLabel;
		public final Temp returnValue;
		public FlatIrTraceFunction(Label functionLabel, List<Temp> parameters,
				List<IrStatement> flatBodyTraces, Label bodyLabel, Label exitLabel,
				Temp returnValue) {
			this.functionLabel = functionLabel;
			this.parameters = parameters;
			this.flatBodyTraces = flatBodyTraces;
			this.bodyLabel = bodyLabel;
			this.exitLabel = exitLabel;
			this.returnValue = returnValue;
		}
	}
	
	public static abstract class IrStatement {
		public Label label; // may get set after built
		IrStatement() {
			label = null;
		}
	}
	
	public static class NopIrStatement extends IrStatement { }
	
	public static class AssignmentIrStatement extends IrStatement {
		public final IrExpression lhs, rhs;
		public AssignmentIrStatement(IrExpression lhs, IrExpression rhs) {
			this.lhs = lhs;
			this.rhs = rhs;
		}
	}
	
	public static class IfIrStatement extends IrStatement { 
		public final RelationalOperator op;
		public final IrExpression left, right;
		public final Label thenLabel, elseLabel;
		public IfIrStatement(RelationalOperator op, IrExpression left,
			IrExpression right, Label thenLabel, Label elseLabel) {
			this.op = op;
			this.left = left;
			this.right = right;
			this.thenLabel = thenLabel;
			this.elseLabel = elseLabel;
		}
		
	}
	
	public static class JumpIrStatement extends IrStatement {
		public final Label target;
		public JumpIrStatement(Label target) {
			this.target = target;
		}
	}
	
	public static class ReturnIrStatement extends IrStatement {
		public final IrExpression expression;
		public ReturnIrStatement(IrExpression expression) {
			this.expression = expression;
		}
	}

	
	public static abstract class IrExpression {
	}
	
	public static class BinaryIrExpression extends IrExpression { 
		public final ArithmeticOperator op;
		public final IrExpression left, right;
		public BinaryIrExpression(ArithmeticOperator op, IrExpression left,
			IrExpression right) {
			this.op = op;
			this.left = left;
			this.right = right;
		}
	}
	
	public static class TempIrExpression extends IrExpression {
		public final Temp temp;
		public TempIrExpression(Temp temp) {
			this.temp = temp;
		}
	}
	
	public static class CallIrExpression extends IrExpression {
		public final IrExpression codeReference;
		public final List<IrExpression> arguments;
		public CallIrExpression(IrExpression codeReference, List<IrExpression> arguments) {
			this.codeReference = codeReference;
			this.arguments = arguments;
		}
	}
	
	public static class MemoryIrExpression extends IrExpression {
		public final IrExpression base, index;
		public MemoryIrExpression(IrExpression base, IrExpression index) {
			this.base = base;
			this.index = index;
		}
	}
	
	public static class LiteralIrExpression extends IrExpression {
		public final int value;
		public LiteralIrExpression(int value) {
			this.value = value;
		}
	}
	
	public static class NameIrExpression extends IrExpression {
		public final Label name;
		public NameIrExpression(Label name) {
			this.name = name;
		}
	}

}
