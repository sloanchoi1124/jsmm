package translate;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import static ir.Ir.*;
import util.Label;
import util.Temp;


public class Flatten {

	static final int MAX_WEIGHT = 2; // TODO: is this for statements or for expressions? leaning latter
	
	// define the local environment!!!
	// something that keeps a pointer to where in current list we are working?
	private static List<IrStatement> statements;
	
	private static void append(IrStatement statement) {
		statements.add(statement);
	}
	
	
	//CHECKME: should this return a new program? is it a deep-enough "copy"?
	public static void flatten(IrProgram program) {
		for (Map.Entry<Label, IrFunction> pair : program.functions.entrySet()) {
			program.functions.put(pair.getKey(),
					flattenFunction(pair.getValue()));
		}	
	}
	
	
	private static IrFunction flattenFunction(IrFunction function) {
		statements = new ArrayList<IrStatement>();
		for (IrStatement statement : function.body) {
			IrStatement flattened = flattenStatement(statement);
			flattened.label = statement.label;
			append(flattened);
		}
		return new IrFunction(function.functionLabel,
				function.parameters,
				statements,
				function.bodyLabel,
				function.exitLabel, 
				function.returnValue);
	}
	
	
	private static IrStatement flattenStatement(IrStatement statement) {
		if (statement instanceof NopIrStatement ||
				statement instanceof JumpIrStatement) {
			return statement;
		
		} else if (statement instanceof AssignmentIrStatement) {
			AssignmentIrStatement assignStm = (AssignmentIrStatement) statement;
			IrExpression lhs, rhs;
			if (calcWeight(assignStm.lhs) == 1) {
				lhs = assignStm.lhs;
				rhs = flattenExpression(MAX_WEIGHT, assignStm.rhs);
			} else {
				rhs = flattenExpression(1, assignStm.rhs);
				lhs = flattenExpression(MAX_WEIGHT, assignStm.lhs); // TODO: CHECKME!!!
			}
			return new AssignmentIrStatement(lhs, rhs);
			
		}  else if (statement instanceof IfIrStatement) {
			IfIrStatement ifStm = (IfIrStatement) statement;
			IrExpression flattenedLeft = flattenExpression(1, ifStm.left);
			IrExpression flattenedRight = flattenExpression(1, ifStm.right);
			return new IfIrStatement(ifStm.op, 
					flattenedLeft, flattenedRight, ifStm.thenLabel, ifStm.elseLabel);
			
		} else if (statement instanceof ReturnIrStatement) {
			ReturnIrStatement returnStm = (ReturnIrStatement) statement;
			IrExpression flattenedRet = flattenExpression(MAX_WEIGHT, returnStm.expression);
			return new ReturnIrStatement(flattenedRet);
			
		} else {
			throw new RuntimeException("unexpected IrStatement class: " + statement.getClass());
		}
	}
	

	// assumes weightLimit >= 1
	private static IrExpression flattenExpression(int weightLimit, IrExpression expression) {
		// assumption is that literals will have _already_ been reduced
		// meaning we do not have binops over two literals
		// and that literals are not too large
		if (expression instanceof LiteralIrExpression ||
				expression instanceof NameIrExpression ||
				expression instanceof TempIrExpression) {
			return expression;
		} 
		
		if (weightLimit == 1) {
			IrExpression lhs = new TempIrExpression(new Temp());
			IrExpression rhs = flattenExpression(MAX_WEIGHT, expression);
			append(new AssignmentIrStatement(lhs, rhs));
			return lhs;
		}
		
		if (expression instanceof BinaryIrExpression) {
			BinaryIrExpression binExp = (BinaryIrExpression) expression;
			int leftWeight = calcWeight(binExp.left);
			int rightWeight = calcWeight(binExp.right);
			if (leftWeight + rightWeight <= weightLimit) {
				return binExp;
			} else {
				IrExpression left, right;
				if (rightWeight < weightLimit) {
					right = binExp.right;
					left = flattenExpression(1, binExp.left); // since if here, leftWeight > 1
				} else {
					// we need to flatten left _before_ we flatten right to preserve meaning
					// even if left would otherwise not need to be broken up
					left = flattenExpression(1, binExp.left);
					right = flattenExpression(weightLimit - 1, binExp.right);
				}
				return new BinaryIrExpression(binExp.op, left, right);
			}
			
		} else if (expression instanceof MemoryIrExpression) {
			MemoryIrExpression memExp = (MemoryIrExpression) expression;
			// TODO: consider if index is Literal 0 whether that should still count as weight 1
			// otherwise, this is really same logic as for binary expressions above
			int baseWeight = calcWeight(memExp.base);
			int indexWeight = calcWeight(memExp.index);
			if (baseWeight + indexWeight <= weightLimit) {
				return memExp;
			} else {
				IrExpression base, index;
				if (indexWeight < weightLimit) {
					index = memExp.index;
					base = flattenExpression(1, memExp.base);
				} else {
					base = flattenExpression(1, memExp.base);
					index = flattenExpression(weightLimit - 1, memExp.index);
				}
				return new MemoryIrExpression(base, index);
			}

		} else if (expression instanceof CallIrExpression) {
			CallIrExpression callExp = (CallIrExpression) expression;
			List<IrExpression> flattenedArgs = new ArrayList<IrExpression>();
			for (IrExpression arg : callExp.arguments) {
				flattenedArgs.add(flattenExpression(1, arg));
			}
			return new CallIrExpression(callExp.codeReference, flattenedArgs);
			
		} else {
			throw new RuntimeException("unexpected IrExpression class: " + expression.getClass());
		}
	}
	
	
	private static int calcWeight(IrExpression expression) {
		if (expression instanceof LiteralIrExpression ||
				expression instanceof NameIrExpression ||
				expression instanceof TempIrExpression) {
			return 1;
		} else if (expression instanceof BinaryIrExpression) {
			BinaryIrExpression binExp = (BinaryIrExpression) expression;
			return calcWeight(binExp.left) + calcWeight(binExp.right);
		} else if (expression instanceof MemoryIrExpression) {
			MemoryIrExpression memExp = (MemoryIrExpression) expression;
			return calcWeight(memExp.base) + calcWeight(memExp.index);
		} else if (expression instanceof CallIrExpression) {
			CallIrExpression callExp = (CallIrExpression) expression;
			if (callExp.arguments.size() == 0) {
				return MAX_WEIGHT;
			} else {
				int argWeight = 1;
				for (IrExpression arg : callExp.arguments) {
					argWeight = Math.max(argWeight, calcWeight(arg));
				}
				return argWeight + MAX_WEIGHT - 1;
			}
		} else {
			throw new RuntimeException("unexpected IrExpression class: " + expression.getClass());
		}
	}
	
}
