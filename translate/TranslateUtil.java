package translate;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static ast.Ast.Expression;
import static ir.Ir.*;
import static translate.TranslateExpression.translateExpression;
import util.Label;
import util.Temp;


public class TranslateUtil {
	
	private static final String STDLIB_PREFIX = "_stdlib_";
	
	static final IrExpression ZERO = new LiteralIrExpression(0);
	static final IrExpression ONE = new LiteralIrExpression(1);
	
	
	/* compute set of temporaries that are assigned values across a list
	 * of statements (usually the body of a function)
	 */
	public static Set<Temp> definedTemps(List<IrStatement> statements) {
		Set<Temp> temps = new TreeSet<Temp>();
		for (IrStatement statement : statements) {
			if (statement instanceof AssignmentIrStatement) {
				AssignmentIrStatement assign = (AssignmentIrStatement) statement;
				IrExpression left = assign.lhs;
				if (left instanceof TempIrExpression) {
					TempIrExpression tempExp = (TempIrExpression) left;
					temps.add(tempExp.temp);
				}
			}
		}
		return temps;
	}
	
	
	
	static IrExpression addExplicitCall(EnvironmentIfc env, Expression astFun, List<Expression> astArgs) {
		IrExpression irFun = translateExpression(env, astFun);
		// in current (simplified model), function should evaluate to name (label)
		if (! (irFun instanceof NameIrExpression)) {
			// do nothing
		} else {
			throw new RuntimeException("illogical call procedure translation");
		}
		List<IrExpression> irArgs = new ArrayList<IrExpression>();			
		for (Expression e : astArgs) {
			irArgs.add(translateExpression(env, e));
		}
		return addCall(env, irFun, irArgs); 
	}
	
	
	
	/* adds an assignment from a memory expression to a new temporary
	 * returns new temporary
	 */
	static IrExpression addLoad(EnvironmentIfc environment, 
			IrExpression base, IrExpression offset) {
		return addPlainAssignment_(environment, 
				new MemoryIrExpression(base, offset));
	}
	
	
	// adds an assignment from given value to a memory expression
	static void addStore(EnvironmentIfc environment, 
			IrExpression base, IrExpression offset, IrExpression value) {
		environment.add(new AssignmentIrStatement(
				new MemoryIrExpression(base, offset), value));
	}
	
		
	/* adds a call to an implicit standard library routine 
	 * returns new temporary where result of call has been stored
	 */
	static IrExpression addImplicitCall(EnvironmentIfc environment, String name,
			List<IrExpression> arguments) {
		IrExpression callExp = new CallIrExpression(new NameIrExpression(new Label(name)), arguments);
		return addPlainAssignment_(environment, callExp);
	}
	
	
	/* adds a call to function indicated by a code reference
	 * (an IR expression that can be interpreted as pointing to code of a function)
	 * returns new temporary where result of call has been stored
	 */
	static IrExpression addCall(EnvironmentIfc environment, IrExpression codeReference,
			List<IrExpression> arguments) {
		IrExpression callExp = new CallIrExpression(codeReference, arguments);
		return addPlainAssignment_(environment, callExp);
	}
	
	
	/* converts a name to a standard-library label
	 */
	static Label makeStandardLabel(String name) {
		return new Label(STDLIB_PREFIX + name);
	}
	
	/* adds assignment of given expression to new temporary
	 * and returns the new temporary
	 */
	private static IrExpression addPlainAssignment_(EnvironmentIfc environment,
			IrExpression expression) {
		IrExpression tempExp = new TempIrExpression(new Temp());
		environment.add(new AssignmentIrStatement(tempExp, expression));
		return tempExp;
	}
		
}
