

package typecheck;


import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ast.Type;
import static ast.Type.*;

public class TypeCompare {
	
	/* returns true if type0 and type1 are equivalent types; false otherwise
	 */
	public static boolean matches(Type type0, Type type1) {
		
		// AnyType matches any type
		if ((type0 instanceof AnyType) || (type1 instanceof AnyType)) {
			return true;
		}
		
		if (type0 instanceof NullType && (type1 instanceof NullType || type1 instanceof ObjectType)) {
			return true;
		}
		
		if (type0 instanceof ArrayType) {
			ArrayType arrayType0 = (ArrayType) type0;
			if (type1 instanceof ArrayType) {
				ArrayType arrayType1 = (ArrayType) type1;
				return matches(arrayType0.innerType, arrayType1.innerType);
			}
			
		} else if (type0 instanceof FunctionType) {
			FunctionType functionType0 = (FunctionType) type0;
			if (type1 instanceof FunctionType) {
				FunctionType functionType1 = (FunctionType) type1;
				return matches(functionType0.outType, functionType1.outType) &&
					matches(functionType0.inType, functionType1.inType);
			}
		
		} else if (type0 instanceof TupleType) {
			TupleType tupleType0 = (TupleType) type0;
			if (type1 instanceof TupleType) {
				TupleType tupleType1 = (TupleType) type1;
				List<Type> types0 = tupleType0.types;
				List<Type> types1 = tupleType1.types;
				if (types0.size() == types1.size()) {
					boolean matched = true;
					for(int i = 0; matched && i < types0.size(); i++) {
						matched = matches(types0.get(i), types1.get(i));
					}
					return matched;
				}
			}
			
		} else if (type0 instanceof ObjectType) {
			ObjectType objectType0 = (ObjectType) type0;
			if (type1 instanceof NullType) {
				return true;
			}
			if (type1 instanceof ObjectType) {
				ObjectType objectType1 = (ObjectType) type1;
				if (objectType0.memberTypes.size() == objectType1.memberTypes.size()) {
					Iterator<Map.Entry<String, Type>> it0 = objectType0.memberTypes.entrySet().iterator();
					Iterator<Map.Entry<String, Type>> it1 = objectType1.memberTypes.entrySet().iterator();
					boolean matched = true;
					while (matched && it0.hasNext()) {
						Map.Entry<String, Type> p0 = it0.next();
						Map.Entry<String, Type> p1 = it1.next();
						matched = p0.getKey().equals(p1.getKey()) &&
								matches(p0.getValue(), p1.getValue());
					}
					return matched;
				}
			}
		
		} else if (type0 instanceof BooleanType) {
			return (type1 instanceof BooleanType);
		
		} else if (type0 instanceof IntType) {
			return (type1 instanceof IntType);
		
		} else if (type0 instanceof StringType) {
			return (type1 instanceof StringType);
		}
				
		// in all other cases:
		return false;
	}
	
}
