package buildast;


import java.util.HashMap;
import java.util.Map;

import ast.SymbolTable;
import ast.Type;
import util.Location;
import util.Uid;


public class HashSymbolTable implements SymbolTable {
	
	private class Data {
		final Location location;
		final String name;
		Type type;
		public Data(Location location, String name, Type type) {
			this.location = location;
			this.name = name;
			this.type = type;
		}
	}
	
	private final Map<Uid, Data> map;

	
	public HashSymbolTable() {
		map = new HashMap<Uid, Data>();
	}
	
	
	public boolean contains(Uid uid) {
		return map.containsKey(uid);
	}
	
	
	public Location getLocation(Uid uid) {
		return get(uid).location;
	}
	
	
	public String getName(Uid uid) {
		return get(uid).name;
	}

	
	public Type getType(Uid uid) {
		return get(uid).type;
	}
	
	
	public void add(Uid uid, Location location, String name, Type type) {
		map.put(uid, new Data(location, name, type));
	}
	
	
	public void updateType(Uid uid, Type type) {
		Data data = map.get(uid);
		if (data.type == null) {
			data.type = type;
		} else {
			throw new RuntimeException("cannot update non-null type entry");
		}
	}
	
	
	
	private Data get(Uid uid) {
		Data data = map.get(uid);
		if (data == null) {
			throw new RuntimeException("unique identifier [" + uid + "] not found in symbol table");
		}
		return data;
	}
	
}


