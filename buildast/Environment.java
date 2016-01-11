
package buildast;


import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import ast.SymbolTable;
import ast.Type;
import util.CompilerException;
import util.Location;
import util.Uid;


public class Environment implements EnvironmentIfc {
	private Deque<Map<String, Uid>> scopes;
	private SymbolTable symbolTable;
		
	
	public Environment() { 
		scopes = new ArrayDeque<Map<String, Uid>>();
		symbolTable = new HashSymbolTable();
		pushNewScope();
	}
	
	
	public SymbolTable extractSymbolTable() {
		return symbolTable;
	}
	
	
	public void pushNewScope() { 
		scopes.addFirst(new HashMap<String, Uid>());
	}
	
	
	public void popScope() {
		scopes.removeFirst();
	}
	
		
	public Uid defineVariable(Location location, String name, Type type) {
		Map<String, Uid> scope = scopes.peekFirst();
		if (!scope.containsKey(name)) {
			Uid uid = new Uid();
			scope.put(name, uid);
			symbolTable.add(uid, location, name, type);
			return uid;
		} else {
			throw new CompilerException(location,
					"variable identifier redefiniton: " + name);
		}
	}		

		
	public Uid findVariable(Location location, String name) { 
		Uid uid = findVariableRaw(name);
		if (uid == null) {
			throw new CompilerException(location,
					"undefined variable identifier: " + name);
		} else {
			return uid;
		}
	}	
	
	
	public Set<Uid> getUids() {
		return new HashSet<Uid>(scopes.peek().values());
	}
	
	
	private Uid findVariableRaw(String name) { 
		Iterator<Map<String, Uid>> iter = scopes.iterator();
		while(iter.hasNext()) {
			Map<String, Uid> scope = iter.next();
			Uid id = scope.get(name);
			if (id != null) {
				return id;
			}
		}
		return null;
	}	
	
	
	void debug() {
		Iterator<Map<String, Uid>> iter = scopes.descendingIterator();
		String s = "";
		int level = 0;
		while(iter.hasNext()) {
			System.out.println(s + "scope " + level);
			Map<String, Uid> scope = iter.next();
			for (Map.Entry<String,Uid> p : scope.entrySet()) {
				System.out.println(s + p.getKey() + ": " + p.getValue());
			}
			s += "  ";
			level++;
		}
	}
	
}
