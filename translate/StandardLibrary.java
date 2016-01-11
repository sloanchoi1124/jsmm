package translate;

//CHECKME!!!

class StandardLibrary {

	static final String PREFIX = "cflat_stdlib_"; 
	
	// implicit standard library functions //TODO: ADD OTHERS???
	static final String ALLOCATE = PREFIX + "_alloc";
	static final String APPEND = PREFIX + "_push";
	static final String CONCAT = PREFIX + "_strcat";
	static final String STRING_EQ = PREFIX + "_streq";
	static final String STRING_LT = PREFIX + "_streq";

}
