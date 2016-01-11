package ir;


class StandardLibrary {

	static final String PREFIX = "cflat_stdlib_";
	
	// implicit standard library functions
	static final String APPEND = PREFIX + "_append";
	static final String ALLOCATE = PREFIX + "_alloc";
	static final String CONCAT = PREFIX + "_strcat";
	static final String ECHO = PREFIX + "_echo";
	static final String STRING_EQ = PREFIX + "_streq";

	// check if a string matches an explicit standard library name
	static boolean isStandard(String s) {
		return (s.equals("charat") || s.equals("chr") || 
				s.equals("die") || s.equals("ohce") || 
				s.equals("ord") || s.equals("strlen"));
	}

}
