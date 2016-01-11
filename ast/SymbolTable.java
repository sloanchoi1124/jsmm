/**
 * Symbol tables for js-- abstract syntax trees
 * 
 * all three of the "get" methods should throw a runtime exception if the 
 * supplied Uid is not found
 * 
 */

package ast;


import util.Location;
import util.Uid;


public interface SymbolTable {

	

	/* check if an id is in the table */
	boolean contains(Uid uid);

	/* get the location of an id */
	Location getLocation(Uid uid);

	/* get the name of an id */
	String getName(Uid uid);

	/* get the type of an id */
	Type getType(Uid uid);
	
	
	/* add a new entry to the table 
	 *   mapping the uid to the the location, name and type of an id
	 */
	void add(Uid uid, Location location, String name, Type type);
	
	
	/* update type information in case where it has been left as null */
	void updateType(Uid uid, Type type);

}