package ir;


public enum RelationalOperator {

	EQ  { public String toString() { return "=="; }},
	NEQ { public String toString() { return "!="; }},
	LT  { public String toString() { return "<";  }},
	LTE { public String toString() { return "<="; }},
	GT  { public String toString() { return ">";  }}, 
	GTE { public String toString() { return ">="; }}, 
	
}
