
package buildast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;

import ast.Type;
import static ast.Type.*;


public class StandardLibrary {
		
	public static void addLibrary(EnvironmentIfc environment) {
		SortedMap<String, Type> memberTypes;
		ArrayList<Type> tupleTypeElements;
		
		memberTypes = new TreeMap<String, Type>();
		memberTypes.put("log",	new FunctionType(STRING_TYPE, VOID_TYPE));
		environment.defineVariable(null, "console", new ObjectType(memberTypes));
		
		memberTypes = new TreeMap<String, Type>();
		
		tupleTypeElements = new ArrayList<Type>(Arrays.asList(STRING_TYPE, INT_TYPE));
		memberTypes.put("charAt", new FunctionType(new TupleType(tupleTypeElements), STRING_TYPE));
		memberTypes.put("charCodeAt", new FunctionType(new TupleType(tupleTypeElements), INT_TYPE));
		memberTypes.put("exit",	new FunctionType(INT_TYPE, VOID_TYPE));
		memberTypes.put("fromCharCode", new FunctionType(INT_TYPE, STRING_TYPE));
		memberTypes.put("intToString", new FunctionType(INT_TYPE, STRING_TYPE));
		memberTypes.put("length", new FunctionType(new ArrayType(ANY_TYPE), INT_TYPE));
		memberTypes.put("parseInt", new FunctionType(STRING_TYPE, INT_TYPE));
		memberTypes.put("read",	new FunctionType(VOID_TYPE, STRING_TYPE));
		memberTypes.put("strlen", new FunctionType(STRING_TYPE, INT_TYPE));
		environment.defineVariable(null, "Jsmm", new ObjectType(memberTypes));
	}
	
}
