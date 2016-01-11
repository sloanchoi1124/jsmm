package util;

public class CompilerError {
	
	private static int errorCount_ = 0;
	private static int warningCount_ = 0;
	
	public static void fatal(String message) {
		errorCount_++;
		System.err.println("Fatal error: " + message + ".");
		flush();
	}
	
	public static void error(String message) {
		errorCount_++;
		System.err.println("Error: " + message + ".");
	}
	
	public static void warn(String message) {
		warningCount_++;
		System.err.println("Warning: " + message + ".");
	}
	
	public static int getErrorCount() {
		return errorCount_;
	}
	
	public static void reset() {
		errorCount_ = 0;
	}
	
	public static boolean hasErrors() {
		return errorCount_ > 0;
	}
	
	public static void flush() {
		if (errorCount_ > 0) {
			System.err.print("Compiler encountered " + 
					errorCount_ + " error");
			if (errorCount_ > 1) {
				System.err.print("s");
			}
			System.err.println("; aborting compilation.");
			System.exit(1);
		}
	}
}
