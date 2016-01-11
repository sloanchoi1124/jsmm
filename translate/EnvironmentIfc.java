/**
 * Interface for environment for translating abstract syntax to 
 * intermediate representation.
 * 
 */

package translate;


import java.util.List;
import java.util.Map;

import static ast.Ast.Expression;
import ast.Type;
import ast.SymbolTable;

import static ir.Ir.IrStatement;
import static ir.Ir.IrFunction;
import util.Label;
import util.Temp;
import util.Uid;


interface EnvironmentIfc {

	/* returns the symbol table associated with this environment */
	SymbolTable getSymbolTable();
	
	
	/* returns the word size for the target machine code */
	int getWordSize();
	
	/* marks start of translation of function
	 * creates label (based on environment) 
	 * remembers temporaries that correspond to function's parameters
	 * starts a new list of statements for function's body
	 * returns generated label
	 */
	Label enterFunction(List<Temp> parameters);
	
	void enterMain();
	
	/* marks completion of translation for current function
	 * adds map from function's label to instance of IrFunction 
	 */
	void exitFunction();
	
	/* append a statement to current function's body 
	 * assigns a label if one has been set; and if so clears label for next statement */ 
	void add(IrStatement statement);
	
	/* return Label where current function's body begins */
	Label getBodyLabel();
	
	/* return Label where current function exits */
	Label getExitLabel();
	
	/* assign a label for next statement */ 
	void setLabel(Label label);
	
	/* mark that current function will have a return value */
	void setReturn();
	
	/* return Temp where return value of current function is stored */
	Temp getReturnTemp();	
	
	/* marks entrance into a loop */
	void pushLoop(Label begin, Label end);

	/* exits most recently entered loop */
	void popLoop();
	
	/* return beginning label for current loop */
	Label getBegin();

	/* return ending label for current loop */
	Label getEnd();

	/* remembers name for future label generation upon
	 * entering new variable definition or object literal label */
	void pushName(String name);
	
	/* exits most recent naming set (i.e. variable definition or object literal label) */
	void popName();
	
	
	/* mark association from uid to temp */
	void addTemp(Uid uid, Temp temp);
	
	/* return temp that corresponds to specified uid */
	Temp getTemp(Uid uid); 
	
	/* mark association from label to string literal */
	void addString(Label label, String s);
	
	/* return map of function labels to corresponding IrFunction instances */
	Map<Label, IrFunction> getFunctions();
	
	/* return map of labels to string literals */ 
	Map<Label, String> getLabelStringMap();
	
	/* return type corresponding to supplied expression */
	Type getType(Expression e);
	
	/* checks if specified uid corresponds to a standard-library identifier */
	boolean isStandard(Uid uid);
}