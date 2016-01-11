package translate;


import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import static ast.Ast.*;
import ast.Operator;
import ast.Type;
import static ast.Type.ObjectType;
import static ast.Type.STRING_TYPE;
import static ir.Ir.*;
import ir.ArithmeticOperator;
import ir.RelationalOperator;
import typecheck.TypeCompare;
import util.Label;
import util.Temp;
import util.Uid;


class TranslateExpression {
	
	static IrExpression translateExpression(EnvironmentIfc env, Expression expression) {
		if (expression instanceof BooleanLiteralExpression) {
			BooleanLiteralExpression boolExp = (BooleanLiteralExpression) expression;
			if (boolExp.value) {                
				return TranslateUtil.ONE;
			}
			else {
				return TranslateUtil.ZERO;
			}

		} else if (expression instanceof IntLiteralExpression) {
			IntLiteralExpression intExp = (IntLiteralExpression) expression;
			return new LiteralIrExpression(intExp.value);
			
		} else if (expression instanceof StringLiteralExpression) {
			StringLiteralExpression stringExp = (StringLiteralExpression) expression;
			Label label = new Label("str", "lit");
			env.addString(label, stringExp.value);
			return new NameIrExpression(label);

		} else if (expression instanceof NullExpression) {
			return TranslateUtil.ZERO;
			
		} else if (expression instanceof IdExpression) {
			IdExpression id = (IdExpression) expression;
			if (env.isStandard(id.uid)) {
				String name = env.getSymbolTable().getName(id.uid);
				Label label = TranslateUtil.makeStandardLabel(name);
				return new NameIrExpression(label);
			} else {
				Temp idTemp = env.getTemp(id.uid);
				if (idTemp == null) {
					idTemp = new Temp();
					env.addTemp(id.uid, idTemp);
				}
				return new TempIrExpression(idTemp);
			}
			
		} else if (expression instanceof AccessExpression) {
			AccessExpression access = (AccessExpression) expression;
			Type t = env.getType(access.object);
			if (t instanceof ObjectType) {
				IrExpression irObj = translateExpression(env, access.object);
				ObjectType objTy = (ObjectType) t;
				int i = 0;
				for (String id : objTy.memberTypes.keySet()) {
					if (id.equals(access.member)) {
						break;
					}
					i++;
				}
				if (i < objTy.memberTypes.size()) {
					return TranslateUtil.addLoad(env, irObj,
							new LiteralIrExpression(i * env.getWordSize()));
				} else {
					throw new RuntimeException("object member not found");
				}
			} else {
				throw new RuntimeException("access on non-object type");
			}
			
		} else if (expression instanceof ArrayExpression) {
			ArrayExpression array = (ArrayExpression) expression;
			List<IrExpression> libArgs = new ArrayList<IrExpression>();
			// allocate array triple with no data
			libArgs.add(new LiteralIrExpression(3 * env.getWordSize()));
			IrExpression arrayExp = 
			    TranslateUtil.addImplicitCall(env, StandardLibrary.ALLOCATE, libArgs);
			TranslateUtil.addStore(env, arrayExp, TranslateUtil.ZERO, TranslateUtil.ZERO);
			TranslateUtil.addStore(env, arrayExp, 
			                       new LiteralIrExpression(env.getWordSize()),
			                       TranslateUtil.ZERO);
			TranslateUtil.addStore(env, arrayExp,  
			                       new LiteralIrExpression(2 * env.getWordSize()),
			                       TranslateUtil.ZERO);			
			// store each element in that array using append
			for (Expression e : array.elements) {
			    IrExpression t = translateExpression(env, e);
			    libArgs = new ArrayList<IrExpression>();
			    libArgs.add(arrayExp);
			    libArgs.add(t);
			    TranslateUtil.addImplicitCall(env, 
			                                  StandardLibrary.APPEND, libArgs);
			}
			return arrayExp;
			
		} else if (expression instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) expression;
			switch (binary.operator) {
			case AND:
			case OR:
			{
				RelationalOperator op = RelationalOperator.EQ;
				if (binary.operator == Operator.AND) {
					op = RelationalOperator.NEQ;
				}
				IrExpression irLeft = translateExpression(env, binary.left);
				IrExpression tempExp = new TempIrExpression(new Temp());
				env.add(new AssignmentIrStatement(tempExp, irLeft));
				Label rightLabel = new Label();
				Label doneLabel = new Label();
				env.add(new IfIrStatement(op, irLeft, TranslateUtil.ZERO,
						rightLabel, doneLabel));
				env.setLabel(rightLabel);
				IrExpression irRight = translateExpression(env, binary.right);
				env.add(new AssignmentIrStatement(tempExp, irRight));
				env.setLabel(doneLabel);
				return tempExp;
			}

			case PLUS:
				if (TypeCompare.matches(STRING_TYPE, env.getType(binary))) {
					List<IrExpression> arguments = new ArrayList<IrExpression>();
					arguments.add(translateExpression(env, binary.left));
					arguments.add(translateExpression(env, binary.right));
					return TranslateUtil.addImplicitCall(env,
					                                     StandardLibrary.CONCAT, arguments);
				}
				// otherwise, intentionally fall through
			case MINUS:
			case TIMES:
			case DIVIDE:
			case MOD:
			{
				ArithmeticOperator op = null;
				switch (binary.operator) {
				case PLUS: op = ArithmeticOperator.PLUS; break;
				case MINUS: op = ArithmeticOperator.MINUS; break;
				case TIMES: op = ArithmeticOperator.TIMES; break;
				case DIVIDE: op = ArithmeticOperator.DIVIDE; break;
				case MOD: op = ArithmeticOperator.MOD; break;
				default: throw new RuntimeException("logically impossible");
				}
				IrExpression irLeft = translateExpression(env, binary.left);
				IrExpression irRight = translateExpression(env, binary.right);
				return new BinaryIrExpression(op, irLeft, irRight);
			}
			
			case GT:
			case GTE:
			case LT:
			case LTE:
				//TODO: add string comparison
				return translateComparison_(env, binary.operator, binary.left, binary.right);
				
			case EQ:
			case NEQ:
				if (TypeCompare.matches(STRING_TYPE, env.getType(binary))) {
					List<IrExpression> arguments = new ArrayList<IrExpression>();
					arguments.add(translateExpression(env, binary.left));
					arguments.add(translateExpression(env, binary.right));
					IrExpression eq = TranslateUtil.addImplicitCall(env,
							StandardLibrary.STRING_EQ, arguments);
					if (binary.operator == Operator.EQ) {
						return eq;
					} else {
						return new BinaryIrExpression(ArithmeticOperator.MINUS, 
								TranslateUtil.ONE, eq); 
					} 
				} else {
					return translateComparison_(env, binary.operator, binary.left, binary.right);
				}
				
			default:
				throw new RuntimeException("Invalid operator " + binary.operator +
						" in binary expression");
			}

		} else if (expression instanceof CallExpression) { // CHECKME
			CallExpression call = (CallExpression) expression;
			return TranslateUtil.addExplicitCall(env, call.function, call.arguments);
		
		} else if (expression instanceof FunctionExpression) {
			FunctionExpression function = (FunctionExpression) expression;
			// TODO: escapes and closures
			List<Temp> parameterTemps = new ArrayList<Temp>();
			for (Uid uid : function.parameters) {
			    Temp temp = new Temp();
			    env.addTemp(uid, temp);
			    parameterTemps.add(temp);
			}
			Label name = env.enterFunction(parameterTemps);
			env.setLabel(env.getBodyLabel());
			for (Statement statement : function.body) {
			    TranslateStatement.translateStatement(env, statement);
			}
			env.setLabel(env.getExitLabel());
			IrExpression returnExp = null;
			if (env.getReturnTemp() != null) {
			    returnExp = new TempIrExpression(env.getReturnTemp());
			} else {
				returnExp = TranslateUtil.ZERO; // CHECKME
			}
			env.add(new ReturnIrStatement(returnExp));						
			env.exitFunction();
//			// generate closure
//			List<IrExpression> libArgs = new ArrayList<IrExpression>();
//			int escapes = ...;
//			libArgs.add(new LiteralIrExpression((1 + escapes) * env.getWordSize()));
//			IrExpression arrayExp = 
//			    TranslateUtil.addImplicitCall(env, StandardLibrary.ALLOCATE, libArgs);
//			TranslateUtil.addStore(env, arrayExp, TranslateUtil.ZERO, new NameIrExpression(name));
			// here is where we would pack in escape map
			// ...
			return new NameIrExpression(name);
			//return arrayExp;
		
		} else if (expression instanceof ObjectExpression) {
			ObjectExpression obj = (ObjectExpression) expression;
			List<IrExpression> libArgs = new ArrayList<IrExpression>();
			// allocate array 
			libArgs.add(new LiteralIrExpression(obj.members.size() * env.getWordSize()));
			IrExpression arrayExp = 
			    TranslateUtil.addImplicitCall(env, StandardLibrary.ALLOCATE, libArgs);
			// store each element in object consecutively in array in alphabetical order by label
			int i = 0;
			for (Map.Entry<String, Expression> pair : obj.members.entrySet()) {
				env.pushName(pair.getKey());
			    IrExpression t = translateExpression(env, pair.getValue());
			    TranslateUtil.addStore(env, arrayExp,  
	                       new LiteralIrExpression(i * env.getWordSize()),
	                       t);
			    i++;
			    env.popName();
			}
			return arrayExp;
			
		} else if (expression instanceof SubscriptExpression) {
			SubscriptExpression subscript = (SubscriptExpression) expression;
			IrExpression arrayTriple = translateExpression(env, subscript.array);
			IrExpression base = TranslateUtil.addLoad(env, arrayTriple, 
			                                          TranslateUtil.ZERO);
			IrExpression perceivedIndex = translateExpression(env, subscript.index);
			IrExpression actualIndex = new BinaryIrExpression(ArithmeticOperator.TIMES,
					perceivedIndex, new LiteralIrExpression(env.getWordSize()));
			return new MemoryIrExpression(base, actualIndex);
			
		} else if (expression instanceof TypedExpression) {
			TypedExpression typed = (TypedExpression) expression;
			return translateExpression(env, typed.expression);
			
		} else if (expression instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) expression;
			IrExpression irOperand = translateExpression(env, unary.operand);
			switch (unary.operator) {
			case NOT: // since true=1, false=0, then !x = 1-x
				return new BinaryIrExpression(ArithmeticOperator.MINUS,
						TranslateUtil.ONE, irOperand);
			case MINUS: // -x = 0 - x
				return new BinaryIrExpression(ArithmeticOperator.MINUS,
						TranslateUtil.ZERO, irOperand);
			default:
				throw new RuntimeException("Invalid operator in unary-operator expression");
			}
			
		} else {
			throw new RuntimeException("unexpected Expression class: " + expression.getClass());
		}
	}
	
	
	private static IrExpression translateComparison_(EnvironmentIfc env,
			Operator astOp, Expression leftExp, Expression rightExp) {
			Label thenLabel = new Label();
			Label elseLabel = new Label();
			Label joinLabel = new Label();
			RelationalOperator op = null;
			switch (astOp) {
			case GT: op = RelationalOperator.GT; break;
			case GTE: op = RelationalOperator.GTE; break;
			case LT: op = RelationalOperator.LT; break;
			case LTE: op = RelationalOperator.LTE; break;
			case EQ: op = RelationalOperator.EQ; break;
			case NEQ: op = RelationalOperator.NEQ; break;
			default: throw new RuntimeException("invalid relational operator");
			}
			IrExpression irLeft = translateExpression(env, leftExp);
			IrExpression irRight = translateExpression(env, rightExp);
			env.add(new IfIrStatement(op, irLeft, irRight,
					thenLabel, elseLabel));
			env.setLabel(thenLabel);
			IrExpression tempExp = new TempIrExpression(new Temp());  
			env.add(new AssignmentIrStatement(tempExp, TranslateUtil.ONE));
			env.add(new JumpIrStatement(joinLabel));
			env.setLabel(elseLabel);
			env.add(new AssignmentIrStatement(tempExp, TranslateUtil.ZERO));
			env.setLabel(joinLabel);
			return tempExp;
	}
	
}
