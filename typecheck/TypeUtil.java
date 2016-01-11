
package typecheck;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ast.Type;
import static ast.Type.*;
import static astunparse.AstUnparser.typeToString;
import util.CompilerError;
import util.Location;


class TypeUtil {
	
	/* Returns underlying list of types in a tuple type, 
	 * or a singleton list for a non-tuple type.
	 */ 
	public static List<Type> tupleize(Type type) {
		if (type instanceof TupleType) {
			TupleType tuple = (TupleType) type;
			return tuple.types;
		}
		else {
			List<Type> types = new ArrayList<Type>();
			types.add(type);
			return types;
		}
	}
	
	
	/* Returns true if and only if expected type matches found type;
	 * if false, also flags an error with supplied message and location.
	 */
	public static boolean match(Location location, String message, 
			Type expectedTy, Type foundTy) {
		if (TypeCompare.matches(expectedTy, foundTy)) {
			return true;
		}
		else {
			error(location,
					message + " expected type " + typeToString(expectedTy) + 
					", but found type " + typeToString(foundTy));
			return false;
		}
	}


	/* Returns true if and only if given type is atomic. */
	public static boolean isAtomic(Type type) {
		return (type instanceof BooleanType ||
				type instanceof IntType ||
				type instanceof StringType);
	}
	
	
	/* Returns the the least general of two array types;
	 * or, if neither type is an array of any, just returns first type.
	 */
	public static Type join(Type type0, Type type1) {
		if (isAnyArray(type0)) {
			return type1;
		}
		else if (isAnyArray(type1)) {
			return type0;
		}
		else {
			return type0;
		}
	}
	
	
	/* Returns true if and only if given type is an array of "any".
	 */
	public static boolean isAnyArray(Type type) {
		if (type instanceof ArrayType) {
			ArrayType arrayType = (ArrayType) type;
			return arrayType.innerType instanceof AnyType;
		}
		else {
			return false;
		}
	}
	
	
	/* Returns true if and only if given type is "nonspecific":
	 * namely if it contains a type variable or null type
	 */
	public static boolean isNonspecific(Type type) {
		if (type instanceof AnyType || type instanceof NullType) {
			return true;
		}
		if (type instanceof ArrayType) {
			ArrayType arrayTy = (ArrayType) type;
			return isNonspecific(arrayTy.innerType);
		} else if (type instanceof ObjectType) {
			ObjectType objTy = (ObjectType) type;
			for (Map.Entry<String, Type> p : objTy.memberTypes.entrySet()) {
				if (isNonspecific(p.getValue())) {
					return true;
				}
			}
		} else if (type instanceof FunctionType) {
			FunctionType funTy = (FunctionType) type;
			return (isNonspecific(funTy.inType) || isNonspecific(funTy.outType)); 
		} else if (type instanceof TupleType) {
			TupleType tupTy = (TupleType) type;
			for (Type t : tupTy.types) {
				if (isNonspecific(t)) {
					return true;
				}
			}
		}
		return false;
	}
	
	
	/* Indicates a type mismatch error with description and location.
	 */
	public static void error(Location location, String message) {
		CompilerError.error("Type mismatch " + location + ": " + message);
	}
	
}
