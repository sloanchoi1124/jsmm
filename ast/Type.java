package ast;


import java.util.List;
import java.util.ArrayList;
import java.util.SortedMap;


public abstract class Type {

	public static final NullType NULL_TYPE = new NullType();
	public static class NullType extends Type {
		private NullType() { }
	}
	
	public static final BooleanType BOOLEAN_TYPE = new BooleanType();
	public static class BooleanType extends Type {
		private BooleanType() { }
	}

	public static final IntType INT_TYPE = new IntType();
	public static class IntType extends Type {
		private IntType() { }
	}
	
	public static final StringType STRING_TYPE = new StringType();
	public static class StringType extends Type {
		private StringType() { }
	}
	
	public static final AnyType ANY_TYPE = new AnyType();
	public static class AnyType extends Type {
		private AnyType() { }
	}
	
	public static class TupleType extends Type {
		public final List<Type> types;
		public TupleType(List<Type> types) {
			this.types = types;
		}
	}
	public static final TupleType VOID_TYPE = new TupleType(new ArrayList<Type>());
	
	public static class ArrayType extends Type {
		public final Type innerType;
		public ArrayType(Type innerType) {
			this.innerType = innerType;
		}
	}
	
	public static class ObjectType extends Type {
		public final SortedMap<String, Type> memberTypes;
		public ObjectType(SortedMap<String, Type> memberTypes) {
			this.memberTypes = memberTypes;
		}
	}
	
	public static class FunctionType extends Type {
		public final Type inType, outType;
		public FunctionType(Type inType, Type outType) {
			this.inType = inType;
			this.outType = outType;
		}
	}
	
}
