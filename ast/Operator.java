package ast;



public enum Operator {

	AND { public String toString() { return "&&"; }},
	OR  { public String toString() { return "||"; }},
	NOT { public String toString() { return "!";  }},
	
	EQ  { public String toString() { return "==="; }},
	NEQ { public String toString() { return "!=="; }},
	LT  { public String toString() { return "<";  }},
	LTE { public String toString() { return "<="; }},
	GT  { public String toString() { return ">";  }}, 
	GTE { public String toString() { return ">="; }}, 
	
	PLUS   { public String toString() { return "+"; }},
	MINUS  { public String toString() { return "-"; }}, 
	TIMES  { public String toString() { return "*"; }},
	DIVIDE { public String toString() { return "/"; }}, 
	MOD    { public String toString() { return "%"; }},
	
}
