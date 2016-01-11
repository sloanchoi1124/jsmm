package buildast;


import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import util.Location;

/* note: ending lines and positions for tokens will be inaccurate if token 
 * spans more than one line.
 */ 

public class MakeLocation {
	
	public static Location makeLoc(Token start, Token stop) {
		int n = stop.getText().length();
		return new Location(start.getLine(), start.getCharPositionInLine(),
				stop.getLine(), stop.getCharPositionInLine() + (n-1));
	}
	
	public static Location makeLoc(Token token) {
		return makeLoc(token, token);
	}
	
	public static Location makeLoc(TerminalNode node) {
		return MakeLocation.makeLoc(node.getSymbol());
	}
	
	public static Location makeLoc(TerminalNode start, TerminalNode stop) {
		return MakeLocation.makeLoc(start.getSymbol(), stop.getSymbol());
	}
	
	public static Location makeLoc(Token start, Location stop) {
		return new Location(start.getLine(), start.getCharPositionInLine(),
				stop.getEndLine(), stop.getEndColumn());
	}
	
	public static Location makeLoc(TerminalNode node, Location stop) {
		return makeLoc(node.getSymbol(), stop);
	}
	
	public static Location makeLoc(Location start, Token stop) {
		int n = stop.getText().length();
		return new Location(start.getBeginLine(), start.getBeginColumn(),
				stop.getLine(), stop.getCharPositionInLine() + (n-1));
	}
	
	public static Location makeLoc(Location start, TerminalNode node) {
		return makeLoc(start, node.getSymbol());
	}
	
	public static Location makeLoc(Location start, Location stop) {
		return new Location(start.getBeginLine(), start.getBeginColumn(),
				stop.getEndLine(), stop.getEndColumn());
	}
	
	
}
