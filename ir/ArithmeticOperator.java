package ir;


public enum ArithmeticOperator {

	PLUS   { public String toString() { return "+"; }},
	MINUS  { public String toString() { return "-"; }}, 
	TIMES  { public String toString() { return "*"; }},
	DIVIDE { public String toString() { return "/"; }}, 
	MOD    { public String toString() { return "%"; }}

}
