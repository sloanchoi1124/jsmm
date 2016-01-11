package translate;


import static ast.Ast.*;
import static ir.Ir.*;
import static translate.TranslateExpression.translateExpression;
import ir.RelationalOperator;
import util.CompilerError;
import util.Label;


class TranslateStatement {

	static void translateStatement(EnvironmentIfc env, Statement statement) {
		if (statement instanceof AssignmentStatement) {
			AssignmentStatement assignment = (AssignmentStatement) statement;
			if (assignment.lhs instanceof IdExpression) {
				IdExpression id = (IdExpression) (assignment.lhs);
				env.pushName(env.getSymbolTable().getName(id.uid));
			}
			IrExpression irLhs = translateExpression(env, assignment.lhs);
			IrExpression irRhs = translateExpression(env, assignment.rhs);
			env.add(new AssignmentIrStatement(irLhs, irRhs));
			if (assignment.lhs instanceof IdExpression) {
				env.popName();
			}
			
		} else if (statement instanceof BreakStatement) {
			BreakStatement breakStatement = (BreakStatement) statement;
			Label endLabel = env.getEnd();
			if (endLabel != null) {
				env.add(new JumpIrStatement(endLabel));
			}
			else {
				CompilerError.error("Break not in loop " + breakStatement.location);
			}
			
		} else if (statement instanceof ContinueStatement) {
			ContinueStatement continueStatement = (ContinueStatement) statement;
			Label beginLabel = env.getBegin();
			if (beginLabel != null) {
				env.add(new JumpIrStatement(beginLabel));
			}
			else {
				CompilerError.error("Continue not in loop " + continueStatement.location);
			}
			
		} else if (statement instanceof CallStatement) {
			CallStatement call = (CallStatement) statement;
			TranslateUtil.addExplicitCall(env, call.procedure, call.arguments);
			
		} else if (statement instanceof CompoundStatement) {
			CompoundStatement compound = (CompoundStatement) statement;
			for (Statement s : compound.body) {
				translateStatement(env, s);
			}
			
		} else if (statement instanceof IfStatement) {
			IfStatement ifThen = (IfStatement) statement;
			Label thenLabel = new Label("if", "then");
			Label elseLabel = new Label("if", "else");
			Label doneLabel = new Label("if", "done");
			IrExpression conditionExp = translateExpression(env, ifThen.condition);
			env.add(new IfIrStatement(RelationalOperator.NEQ,
					conditionExp, TranslateUtil.ZERO, thenLabel, elseLabel));
			env.setLabel(thenLabel);
			translateStatement(env, ifThen.thenStatement);
			env.add(new JumpIrStatement(doneLabel));
			env.setLabel(elseLabel);
			if (ifThen.elseStatement != null) {
				translateStatement(env, ifThen.elseStatement);
			}
			env.setLabel(doneLabel);
			
		} else if (statement instanceof ReturnStatement) {
			ReturnStatement ret = (ReturnStatement) statement;
			/* returns works by assigning value to designated return temp
			 * for this function; then jumping to exit label of function
			 * that way every function has single point of return
			 */
			if (ret.returnValue != null) {
			    env.setReturn();
			    env.add(new AssignmentIrStatement(
			    		new TempIrExpression(env.getReturnTemp()),
			    		translateExpression(env, ret.returnValue)));
			}
			env.add(new JumpIrStatement(env.getExitLabel()));
		
		} else if (statement instanceof WhileStatement) {
			WhileStatement wh = (WhileStatement) statement;
			Label beginLoopLabel = new Label("loop", "begin");
			Label bodyLoopLabel = new Label("loop", "body");
			Label endLoopLabel = new Label("loop", "end");
			env.pushLoop(beginLoopLabel, endLoopLabel);
			env.setLabel(beginLoopLabel);
			IrExpression conditionExp = translateExpression(env, wh.condition);
			env.add(new IfIrStatement(RelationalOperator.NEQ,
					conditionExp, TranslateUtil.ZERO, bodyLoopLabel, endLoopLabel));
			env.setLabel(bodyLoopLabel);
			translateStatement(env, wh.body);
			env.add(new JumpIrStatement(beginLoopLabel));
			env.setLabel(endLoopLabel);
			env.popLoop();

		} else if (statement instanceof DoWhileStatement) {
			DoWhileStatement dws = (DoWhileStatement) statement;
			Label beginLoopLabel = new Label("loop", "begin");
			Label endLoopLabel = new Label("loop", "end");
			env.pushLoop(beginLoopLabel, endLoopLabel);
			env.setLabel(beginLoopLabel);
			translateStatement(env, dws.body);
			IrExpression conditionExp = translateExpression(env, dws.condition);
			env.add(new IfIrStatement(RelationalOperator.NEQ,
					conditionExp, TranslateUtil.ZERO, beginLoopLabel, endLoopLabel));
			env.setLabel(endLoopLabel);
			env.popLoop();
		
		} else {
			throw new RuntimeException("unexpected Statement class: " + statement.getClass());
		}
		
	}
	
}
