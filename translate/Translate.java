package translate;

// top-level translation of AST programs to IR programs


import java.util.Map;
import java.util.Set;

import static ast.Ast.Program;
import static ast.Ast.Statement;
import ast.Type;
import static ir.Ir.IrProgram;
import static ir.Ir.IrFunction;
import static ir.Ir.ReturnIrStatement;
import util.CompilerError;
import util.Label;
import util.Temp;
import util.Uid;


public class Translate {
	public static IrProgram translate(Program program, Map<Uid, Type> expressionTypeMap, int wordSize) {
		EnvironmentIfc env = new Environment(program.symbolTable, expressionTypeMap, wordSize);
		env.enterMain();
		env.setLabel(env.getBodyLabel());
		for (Statement statement : program.body) {
			TranslateStatement.translateStatement(env, statement);
		}
		env.setLabel(env.getExitLabel());
		env.add(new ReturnIrStatement(TranslateUtil.ZERO));
		env.exitFunction();		
		Map<Label,IrFunction> functions = env.getFunctions();
		IrFunction mainFunction = functions.get(new Label("main"));
		Set<Temp> globals = TranslateUtil.definedTemps(mainFunction.body);
		globals.removeAll(mainFunction.parameters);
		
		CompilerError.flush();
		return new IrProgram(globals, functions, env.getLabelStringMap());
	}
}
