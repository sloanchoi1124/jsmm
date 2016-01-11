package cflat;


import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static ir.Ir.*;
import static translate.TranslateUtil.definedTemps;
import util.Label;
import util.StringHelper;
import util.Temp;


public class Cflat {
	
	private static StringBuilder sb;  

	
	public static String toCflatString(IrProgram program) {
		sb = new StringBuilder();
		printHeaders(program.globals, program.stringLiterals);
		for (IrFunction function : program.functions.values()) {
			printFunction(function, program.globals);
		}		
		return sb.toString();
	}

	
	public static String toCflatString(IrBlockProgram program) {
		sb = new StringBuilder();
		printHeaders(program.globals, program.stringLiterals);
		for (IrBlockFunction function : program.functions.values()) {
			printBlockFunction(function, program.globals);
		}		
		return sb.toString();
	}
	
	
	public static String toCflatString(IrTraceProgram program) {
		sb = new StringBuilder();
		printHeaders(program.globals, program.stringLiterals);
		for (IrTraceFunction function : program.functions.values()) {
			printTraceFunction(function, program.globals);
		}		
		return sb.toString();
	}
	//
	public static String toCflatString(IrTraceProgram2 program) {
		sb = new StringBuilder();
		printHeaders(program.globals, program.stringLiterals);
		for (FlatIrTraceFunction function : program.functions.values()) {
			printFlatTraceFunction(function, program.globals);
		}		
		return sb.toString();
	}
	
	
	private static void printHeaders(Set<Temp> globals, Map<Label, String> stringLiterals) {
		sb.append("#include \"cflat_stdlib.c\"\n\n");
		// define string literals
		for (Map.Entry<Label, String> entry : stringLiterals.entrySet()) {
			String literal = StringHelper.escape(entry.getValue());
			sb.append("int " + entry.getKey() + " = (int) " + literal + ";\n");
		}
		if (!stringLiterals.isEmpty()) {
			sb.append('\n');
		}
		// define globals
		int n = globals.size();
		Temp[] tempArray = globals.toArray(new Temp[n]);
		if (n > 0) {
			sb.append("int ");
		}
		for (int i = 0; i < n-1; i++) {
			sb.append("" + tempArray[i] + ", ");
		}
		if (n > 0) {
			sb.append("" + tempArray[n-1] + ";\n");
		}
		sb.append('\n');
	}

	
	private static void printFunction(IrFunction function, Set<Temp> globals) {
		Set<Temp> temps = definedTemps(function.body);
		temps.removeAll(function.parameters);
		printFunctionHeader(function.functionLabel, globals, 
				function.parameters, temps);
		for (IrStatement statement : function.body) {
			printStatement(statement);
		}
		sb.append("}\n\n");
	}

	
	private static void printBlockFunction(IrBlockFunction function, Set<Temp> globals) {
		Set<Temp> temps = definedTempsBlocks(function.bodyBlocks.values());
		temps.removeAll(function.parameters);
		printFunctionHeader(function.functionLabel, globals, 
				function.parameters, temps);
		sb.append("goto " + function.bodyLabel + ";\n");
		for (Map.Entry<Label, List<IrStatement>> pair : function.bodyBlocks.entrySet()) {
			for (IrStatement statement : pair.getValue()) {
				printStatement(statement);
			}
			sb.append("\n");
		}
		sb.append("}\n\n");
	}

	
	//
	private static void printFlatTraceFunction(FlatIrTraceFunction function, Set<Temp> globals) {
		Collection<List<IrStatement>> x = new HashSet<List<IrStatement>>();
		x.add(function.flatBodyTraces);
		Set<Temp> temps = definedTempsBlocks(x);
		printFunctionHeader(function.functionLabel, globals, 
				function.parameters, temps);
		sb.append("goto " + function.bodyLabel + ";\n");
		for (IrStatement trace : function.flatBodyTraces) {
					printStatement(trace);
			sb.append("\n");
		}
		sb.append("}\n\n");
	}
	//
	public static String toCflatString(List<List<IrStatement>> l) {
		sb = new StringBuilder();
		for (List<IrStatement> stat : l) {
			for (IrStatement s : stat) {
				printStatement(s);
			}
		}
		return sb.toString();
	}
	
	
	private static void printTraceFunction(IrTraceFunction function, Set<Temp> globals) {
		Set<Temp> temps = definedTempsTraces(function.bodyTraces);
		temps.removeAll(function.parameters);
		printFunctionHeader(function.functionLabel, globals, 
				function.parameters, temps);
		sb.append("goto " + function.bodyLabel + ";\n");
		for (List<List<IrStatement>> trace : function.bodyTraces) {
			for (List<IrStatement> block : trace) {
				for (IrStatement statement : block) {
					printStatement(statement);
				}
			}
			sb.append("\n");
		}
		sb.append("}\n\n");
	}
	
	
	private static void printFunctionHeader(Label label, Set<Temp> globals,
			List<Temp> parameters, Set<Temp> definedTemps) {
		sb.append("int " + label + "(");
		int n = parameters.size();
		for (int i = 0; i < n-1; i++) {
			sb.append("int " + parameters.get(i) + ", ");
		}
		if (n > 0) {
			sb.append("int " + parameters.get(n-1));
		}
		sb.append(") {\n");
		// declare temporaries
		Set<Temp> temps = new TreeSet<Temp>();
		temps.addAll(definedTemps);
		temps.removeAll(globals);
		n = temps.size();
		Temp[] tempArray = temps.toArray(new Temp[n]);
		if (n > 0) {
			sb.append("\tint ");
		}
		for (int i = 0; i < n-1; i++) {
			sb.append("" + tempArray[i] + ", ");
		}
		if (n > 0) {
			sb.append("" + tempArray[n-1] + ";\n");
		}	
	}
	
	
	private static void printStatement(IrStatement statement) {
		if (statement.label != null) {
			sb.append("" + statement.label + ":\n\t");
		}
		
		if (statement instanceof AssignmentIrStatement) {
			AssignmentIrStatement assign = (AssignmentIrStatement) statement;
			sb.append("\t");
			printExpression(assign.lhs);
			sb.append(" = ");
			printExpression(assign.rhs);
			
		} else if (statement instanceof IfIrStatement) {
			IfIrStatement conditional = (IfIrStatement) statement;
			sb.append("\tif (");
			printExpression(conditional.left);
			sb.append(" " + conditional.op + " "); //CHECKME
			printExpression(conditional.right);
			//
			if (conditional.elseLabel == null) {
				//
				sb.append(") goto " + conditional.thenLabel);
			//
			}
			//
			else {
				sb.append(") goto " + conditional.thenLabel + "; else goto " +
						conditional.elseLabel);
			//
			}
		
		} else if (statement instanceof JumpIrStatement) {
			JumpIrStatement jump = (JumpIrStatement) statement;
			sb.append("\tgoto " + jump.target);
			
		} else if (statement instanceof NopIrStatement) {
			// "no op" - no need to output anything
	
		} else if (statement instanceof ReturnIrStatement) {
			ReturnIrStatement ret = (ReturnIrStatement) statement;
			sb.append("\treturn ");
			if (ret.expression != null) {
				printExpression(ret.expression);
			} 
			
		} else {
			throw new RuntimeException("unexpected IrStatement class: " + statement.getClass());
		}
		sb.append(";\n");
	}
	
	
	private static void printExpression(IrExpression expression) {
		if (expression instanceof BinaryIrExpression) {
			BinaryIrExpression binary = (BinaryIrExpression) expression;
			printExpression(binary.left);
			sb.append(" " + binary.op + " ");
			printExpression(binary.right);
			
		} else if (expression instanceof CallIrExpression) {
			//System.out.println("call expression");
			CallIrExpression call = (CallIrExpression) expression;
			if (call.codeReference instanceof NameIrExpression) {
				NameIrExpression name = (NameIrExpression) call.codeReference;
				sb.append("" + name.name + "(");
			} else if (call.codeReference instanceof TempIrExpression) {
				TempIrExpression funPtr = (TempIrExpression) call.codeReference;
				sb.append("((int (*)(");
				int n = call.arguments.size();
				//System.out.println(call.arguments);
				if (n > 0) {
					sb.append("int");
				}
				for (int i = 1; i < n; i++) {
					sb.append(", int");
				}
				sb.append("))");
				printExpression(funPtr);
				sb.append(")(");
			} else {
				throw new RuntimeException("ill-formed IR function call");
			}
			int n = call.arguments.size();
			for (int i = 0; i < n-1; i++) {
				printExpression(call.arguments.get(i));
				sb.append(", ");
			}
			if (n > 0) {
				printExpression(call.arguments.get(n-1));
			}
			sb.append(")");
			
		} else if (expression instanceof LiteralIrExpression) {
			LiteralIrExpression constant = (LiteralIrExpression) expression;
			sb.append("" + constant.value);
			
		} else if (expression instanceof MemoryIrExpression) {
			MemoryIrExpression memory = (MemoryIrExpression) expression;
			sb.append("*((int *)(");
			printExpression(memory.base);
			sb.append(" + ");
			printExpression(memory.index);
			sb.append("))");
		
		} else if (expression instanceof NameIrExpression) {
			NameIrExpression name = (NameIrExpression) expression;
			sb.append("((int)" + name.name +")");
			
		} else if (expression instanceof TempIrExpression) {
			TempIrExpression tempExp = (TempIrExpression) expression;
			sb.append("" + tempExp.temp);
			
		} else {
			throw new RuntimeException("unexpected IrExpression class: " + expression.getClass());
		}	
	}
	
	private static Set<Temp> definedTempsBlocks(Collection<List<IrStatement>> blocks) {
		Set<Temp> temps = new TreeSet<Temp>();
		for (List<IrStatement> block : blocks) {
			temps.addAll(definedTemps(block));
		}
		return temps;
	}
	
	private static Set<Temp> definedTempsTraces(List<List<List<IrStatement>>> traces) {
		Set<Temp> temps = new TreeSet<Temp>();
		for (List<List<IrStatement>> trace : traces) {
			temps.addAll(definedTempsBlocks(trace));
		}
		return temps;
	}
	
}
