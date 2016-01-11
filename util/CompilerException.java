package util;

public class CompilerException extends RuntimeException {

	public CompilerException(Location location, String message) {
		super(message + " " + location);
	}

}
