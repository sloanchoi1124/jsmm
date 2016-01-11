/**
 * Interface for environment for converting 
 * concrete parse trees to abstract syntax trees.
 * 
 */

package buildast;


import java.util.Set;

import ast.SymbolTable;
import ast.Type;
import util.Location;
import util.Uid;


public interface EnvironmentIfc {

	/* Starts a new scope for local variables and parameters */
	void pushNewScope();

	/* Ends a local scope */
	void popScope();

	/* Associates the given variable name with a new uid and 
	 * the supplied type and location;
	 * a CompilerException is thrown if the name has already been defined.
	 */
	Uid defineVariable(Location location, String name, Type type);

	/* Returns the uid associated with this variable name.
	 * If the name not visible in any active scope, a CompilerException is thrown.
	 */
	Uid findVariable(Location location, String name);
	
	/*
	 * Returns set of uids defined in current scope. 
	 */
	Set<Uid> getUids();
	
	/* Returns the symbol table. Intended to be used after conversion to
	 * abstract syntax is complete.
	 */
	SymbolTable extractSymbolTable();
	
}