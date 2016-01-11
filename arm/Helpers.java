package arm;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ir.Ir.IrExpression;
import ir.Ir.LiteralIrExpression;
import ir.Ir.NameIrExpression;
import ir.Ir.TempIrExpression;
import util.Temp;

public class Helpers {
	
	public static String globalLabel = "L_globals";
	
	public static void printPrologue(StringBuilder sb) {
		// naive, storing all registers ?
		// should use stm op, because reasons?
		sb.append("@prologue\n\t");
		sb.append("sub sp, sp, #32\n\t");
		sb.append("str lr, [sp, #0]\n\t");
		for (int i=4; i<11; i++) {
			sb.append("str r" + i + ", [sp, #" + 4*(i-3) + "]\n\t");
		}
		sb.append("\n");
	}
	
	public static void printEpilogue(StringBuilder sb) {
		// really should use ldm, but whatever
		// naive works
		sb.append("\n");
		sb.append("@epilogue\n\t");
		for (int i=10; i>=4; i--) {
			sb.append("ldr r" + i + ", [sp, #" + 4*(i-3) + "]\n\t");
		}
		sb.append("ldr lr, [sp, #0]\n\t");
		sb.append("sub sp, sp, #32\n\t");
		sb.append("\n");
	}
	
	public static boolean isLiteral(String exp) {
		// detect if a string represents an ARM literal val ("#__");
		// otherwise it should be a register ("r_");
		if (exp.charAt(0) == '#') {
			return true;
		} else {
			return false;
		}
	}
	
	public static String rhsToRegister(StringBuilder sb, IrExpression expression, 
			Map<Temp, Integer> globals) {
		// has to be global temp
		// or literal; if literal then store literal to register first
		
		if (expression instanceof LiteralIrExpression) {
			LiteralIrExpression literal = (LiteralIrExpression) expression;
			return largeLiteralRegister(sb, literal.value);
		}
		
		TempIrExpression tempExp = (TempIrExpression) expression;
		Temp temp = tempExp.temp;
		String offset = "" + globals.get(temp)*4;
		sb.append("ldr r2, " + globalLabel + " + " + offset + "\n\t");
		sb.append("ldr r2, [r2]\n\t");
		return "r2";
		
	}
	
	public static String expToRegister(StringBuilder sb, IrExpression expression,
			Map<Temp, Integer> temps, Map<Temp, Integer> globals) {
		/* In this context, expression MUST BE instanceof
		 * TempIrExpression, NameIrExpression or literal
		 */
		
		if (expression instanceof LiteralIrExpression) {
			// If literal, put literal value;
			// If large literal (>255) store in register
			
			LiteralIrExpression literal = (LiteralIrExpression) expression;
			if (literal.value < 256) {
				return "#" + literal.value;
			} else {
				return largeLiteralRegister(sb, literal.value);
			}
		}
		
		// if not literal then temp OR name
		if (expression instanceof NameIrExpression) {
			return ((NameIrExpression) expression).name.toString(); // ???
		}
		
		TempIrExpression tempExp = (TempIrExpression) expression;
		Temp temp = tempExp.temp;
		
		if (temps.keySet().contains(temp)) {
			return "r" +  temps.get(temp);
		} else if (globals.keySet().contains(temp) /*|| globals2.contains(temp)*/) {
			
			return "r2";
		} else {
			System.out.println("@REGISTER ALLOCATION ERROR FOUND"); 			
			
			// This is the emergency register, so to speak. Because it is rarely used,
			// *sometimes* this will allow something that wouldn't otherwise compile to
			// do so. Baby steps.
			
			return "r3";
		}
	}
	
	public static String largeLiteralRegister(StringBuilder sb, int val) {
		/* Turn a 32-bit int into a series of 8-bit immediates, 
		 * successively moved into a register
		 */
		int next;
		sb.append("mov r3, #0" + "\n\t"); // ensure r3 empty
		for (int i=1; i<4; i++) { // loop exactly three times
			next = (val>>32-8*i) % 256; // get next 8 bits
			sb.append("add r3, r3, #" + next + "\n\t"); // add 8 bits to r3
			sb.append("lsl r3, r3, #8" + "\n\t");  // L shift r3 by 8
		}
		
		next = val % 256; // get last 8 bits
		sb.append("add r3, #" + next + "\n\t"); //add to r3
		
		return "r3";
	}
	
	public static Map<Temp, Integer> makeGlobalMap(Set<Temp> globals) {
		Map<Temp, Integer> ret = new HashMap<Temp, Integer>();
		int count = 0;
		for (Temp t : globals) {
			ret.put(t, count);
			count++;
		}
		return ret;
	}
	
	public static boolean isGlobal(IrExpression expression, Map<Temp, Integer> globals) {
		// determine if a TempIrExpression represents a global variable
		// otherwise, should be a local
		if (expression instanceof TempIrExpression) {
			TempIrExpression tempExp = (TempIrExpression) expression;
			if (globals.containsKey(tempExp.temp)) {
				return true;
			}
		}
		return false;
	}
}
