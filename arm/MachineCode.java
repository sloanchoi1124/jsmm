package arm;

import java.util.List;
import java.util.Map;
import java.util.Set;

import util.Label;
import util.Temp;
import ir.Ir.*;

public class MachineCode {
	
	public static StringBuilder sb;
	public static Map<Temp, Integer> temps;
	public static Map<Temp, Integer> globals;
	public static Map<Label, String> strings;
	public static Map<Label, FlatIrTraceFunction> functions;
	public static final int WORD_SIZE = 4;
	public static String globalLabel = "L_globals";
	

	
	public static String machineCode(IrTraceProgram2 program,
			Map<Temp, Integer> tempMap) {
		sb = new StringBuilder();
		temps = tempMap;
		globals = Helpers.makeGlobalMap(program.globals);

		strings = program.stringLiterals;
		functions = program.functions;
		printHeaders(program.stringLiterals, program.functions);
		for (FlatIrTraceFunction function : program.functions.values()) {
			printTraceFunction(function, temps, globals);
		}
		return sb.toString();
	}
	
	private static void printHeaders(Map<Label, String> stringLiterals,
			Map<Label, FlatIrTraceFunction> functions) {

		sb.append("\t.arch armv6\n\n");
		sb.append("\t.data\n");
		sb.append("\t.align\n");
		for (Temp t : globals.keySet()) {
			sb.append("global_" + t.toString() + ": .word 0;\n");
		}
		sb.append("\n");
		for (Label l : strings.keySet()) {
			sb.append(l.toString() + ": .asciz \"" + strings.get(l) + "\"\n");
		}
		sb.append("\n\n");

		sb.append("\t.text\n");
		sb.append("\t.align\n\n");
		sb.append("\t.global main\n\n");
		if (globals.size() > 0) {
			sb.append("L_globals:\n");
			for (Temp t : globals.keySet()) {
				sb.append("\t.word global_" + t.toString() + "\n");
			}
		}
		sb.append("\n");
		
		// print std library
		// Is this the right way to do it? Who knows!
		sb.append("L_stdlib_jsmm: .word _stdlib_Jsmm\n");
		sb.append("L_stdlib_console: .word _stdlib_console\n");

		sb.append("\n");
		
	}
	
	private static void printTraceFunction(FlatIrTraceFunction function, 
			Map<Temp, Integer> temps, Map<Temp, Integer> globals) {
		// print entrance Label
		sb.append("" + function.functionLabel + ":\n\t");
		
		// prologue
		Helpers.printPrologue(sb); 

		// translate body
		for (IrStatement statement : function.flatBodyTraces) {
			printStatement(statement);
		}
		
		sb.append("\n");
	}
	

	
	private static void printStatement(IrStatement statement) {
		if (statement.label != null) {
			sb.append("\n" + statement.label + ":\n\t");
		}
		
		if (statement instanceof AssignmentIrStatement) {
			AssignmentIrStatement assign = (AssignmentIrStatement) statement;
			
			boolean globalAssign = Helpers.isGlobal(assign.lhs, globals);
			
			String lhs;
			String globalOffset = null;
			
			if (globalAssign){
				// instanceof TempIrExpression
				TempIrExpression tempExp = (TempIrExpression) assign.lhs;
				lhs = "r2"; // default global store register
				globalOffset = "" + globals.get(tempExp.temp)*WORD_SIZE;
				
				
			} else if (assign.lhs instanceof MemoryIrExpression) {
				MemoryIrExpression memExp = (MemoryIrExpression) assign.lhs;
				// corner case-y instance of stores
				
				String reg = Helpers.rhsToRegister(sb, assign.rhs, globals);
				String base = Helpers.expToRegister(sb, memExp.base, temps, globals);
				String offset = Helpers.expToRegister(sb, memExp.index, temps, globals);
				sb.append("str " + reg + ", " + "[" + base + ", " + offset + "]\n\t");
				return;
			} else {
				// otherwise follows same rules as left/right of BinaryIrExp
				lhs = Helpers.expToRegister(sb, assign.lhs, temps, globals);
			}
			
			if (assign.rhs instanceof BinaryIrExpression) {
				// only TempIrExps and LiteralIrExps can be in BinaryIrExp left/right
				// I think?
				
				BinaryIrExpression bin = (BinaryIrExpression) assign.rhs;
				
				String select = ""; // indicates whether to take remainder or integer result of division

				
				String rStr = Helpers.expToRegister(sb, bin.left, temps, globals);
				String lStr = Helpers.expToRegister(sb, bin.right, temps, globals);
				
				switch (bin.op) {
				case PLUS:
					sb.append("add " + lhs + ", " + lStr + ", " + rStr + "\n\t");
					break;
				case MINUS:
					sb.append("sub " + lhs + ", " + lStr + ", " + rStr + "\n\t");
					break;
				case TIMES:
					sb.append("mul " + lhs + ", " + lStr + ", " + rStr + "\n\t");
					break;
				case DIVIDE: // intentional fall through
					select = "r0";
				case MOD:
					if (select.equals("")) { // only assign if not DIVIDE
						select = "r1";
					}
					sb.append("mov r0, " + lStr + "\n\t");
					sb.append("mov r1, " + rStr + "\n\t");
					sb.append("bl  __aeabi_idivmod" + "\n\t");
					sb.append("mov " + lhs + ", " + select + "\n\t");
					break;
				}

			} else if (assign.rhs instanceof TempIrExpression) {
				TempIrExpression tempExp = (TempIrExpression) assign.rhs;
				String rhs;
				if (Helpers.isGlobal(tempExp, globals)) {
					rhs = Helpers.rhsToRegister(sb, tempExp, globals);
				} else {
					rhs = Helpers.expToRegister(sb, tempExp, temps, globals); 
				}
				sb.append("mov " + lhs + ", " + rhs + "\n\t");
				
			} else if (assign.rhs instanceof CallIrExpression) {
				
				CallIrExpression call = (CallIrExpression) assign.rhs;
				
				/*
				 * Currently assuming a max of 2 args, because our compiler is degenerate.
				 */
				
				sb.append("sub sp, sp, #16\n\t");
				for (int i=0; i<4; i++) {
					sb.append("str r" + i + ", [sp, #" + 4*i + "]\n\t");
				}
				
				List<IrExpression> args = call.arguments;
				if (args.size() > 2) {
					System.out.println("@Not equipped to handle > 2 args in the present moment!");
				}
				
				// store args in r0-r2 (exclusive)
				int numArgs = 0;
				for (IrExpression exp : args) {
					if (exp instanceof NameIrExpression) {
						String arg = Helpers.expToRegister(sb, exp, temps, globals);
						sb.append("ldr r" + numArgs + ", =" + arg + "\n\t");
						numArgs++;
					} else {
						// this case may not be all-inclusive
						String arg = Helpers.expToRegister(sb, exp, temps, globals);
						sb.append("mov r" + numArgs + ", " + arg + "\n\t");
						numArgs++;
					}
				}
				
				
				/* As of production, this section of the code is a work in progress.
				 * So it goes.
				 */
				
				String reference = "";
				sb.append("@ CallIr: " + call.codeReference + "\n\t");
				if (call.codeReference instanceof NameIrExpression) {
					NameIrExpression nameExp = (NameIrExpression) call.codeReference;

					reference = nameExp.name.toString();
					sb.append("bl " + reference + "\n\t"); // Unsure about this.
					
				} else if (call.codeReference instanceof TempIrExpression ||
						   call.codeReference instanceof LiteralIrExpression) {
					
					reference = Helpers.expToRegister(sb, call.codeReference, temps, globals);
					
					// branch and link to function
					sb.append("blx " + reference + "\n\t"); // And also this.
				} 
				
				
				
				// move return val into specified reg
				sb.append("mov " + lhs + ", r0\n\t");
				
				// restore r0-3
				for (int i=3; i>=0; i--) {
					sb.append("ldr r" + i + ", [sp, #" + 4*i + "]\n\t");
				}
				sb.append("add sp, sp, #16\n\t");
				
				
			} else if (assign.rhs instanceof MemoryIrExpression) {
				MemoryIrExpression memExp = (MemoryIrExpression) assign.rhs;
				
				// .base and .offset should be Temps? Regardless, I think we
				// can use expToRegister for both (since either Temp or Literal)
				
				String base;
				if (memExp.base instanceof NameIrExpression) {
					base = "L" + ((NameIrExpression) memExp.base).name.toString();
				} else {
					base = Helpers.expToRegister(sb, memExp.base, temps, globals);
				}
				String offset = Helpers.expToRegister(sb, memExp.index, temps, globals);

				sb.append("ldr " + lhs + ", " + base + "\n\t");
				sb.append("ldr " + lhs + ", [" + lhs + ", " + offset +"]\n\t");
				
			} else if (assign.rhs instanceof LiteralIrExpression) {
				LiteralIrExpression literal = (LiteralIrExpression) assign.rhs;
				String val;
				if (literal.value > 255) {
					val = Helpers.largeLiteralRegister(sb, literal.value);
				} else {
					val = "#" + literal.value;
				}
				sb.append("mov " + lhs + ", " + val + "\n\t");
				
			} else if (assign.rhs instanceof NameIrExpression) {
				/* strings, std lib identifiers (?), function definitions (?) 
				 * This is super iffy.
				 */
				
				NameIrExpression name = (NameIrExpression) assign.rhs;

				if (functions.containsKey(name.name)) {
					sb.append("ldr " + lhs + ", " + name.name.toString() + "\n\t");
				} else if (strings.containsKey(name.name)) {
					sb.append("ldr " + lhs + ", =" + name.name.toString() + "\n\t");
				}
				
				
			} else {
				// error
			}
			
			if (globalAssign) {
				// store back global variable here if it's the lhs assign
				sb.append("ldr r3, " + globalLabel + " + " + globalOffset + "\n\t");
				sb.append("str r2, [r3]\n\t");
			}
			
		} else if (statement instanceof IfIrStatement) {
			IfIrStatement conditional = (IfIrStatement) statement;
			
			String left = Helpers.expToRegister(sb, conditional.left, temps, globals);
			String right = Helpers.expToRegister(sb, conditional.right, temps, globals);
			String branchtype = "BRANCHTYPE UNSET";
			
			sb.append("cmp " + left + ", " + right + "\n\t");
						
			switch (conditional.op) {
			case EQ:
				branchtype = "beq";
				break;
			case NEQ:
				branchtype = "neq";
				break;
			case LT:
				branchtype = "blt";
				break;
			case LTE:
				branchtype = "ble";
				break;
			case GT:
				branchtype = "bgt";
				break;
			case GTE:
				branchtype = "bge";
				break;
			default:
				break;
			}
			
			sb.append(branchtype + " " + conditional.elseLabel + "\n\t");
		
		} else if (statement instanceof JumpIrStatement) {
			JumpIrStatement jump = (JumpIrStatement) statement;
			sb.append("b " + jump.target + "\n\t");
			
		} else if (statement instanceof NopIrStatement) {
			// "no op" - no need to output anything
	
		} else if (statement instanceof ReturnIrStatement) {
			ReturnIrStatement ret = (ReturnIrStatement) statement;

			if (ret.expression != null) {
				String retval = Helpers.expToRegister(sb, ret.expression, temps, globals);
				sb.append("mov r0, " + retval + "\n\t");
			}
			// epilogue goes *between* the other two statements
			Helpers.printEpilogue(sb);
			
			sb.append("bx lr\n\t");
			
		} else {
			throw new RuntimeException("unexpected IrStatement class: " + statement.getClass());
		}
	}

	

}
