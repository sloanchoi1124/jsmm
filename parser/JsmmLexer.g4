

lexer grammar JsmmLexer;


@lexer::header {
import java.lang.StringBuilder;

import util.StringHelper; 
}


// punctuation
LPAREN : '(' ;
RPAREN : ')' ;

LBRACK : '[' ;
RBRACK : ']' ;

LBRACE : '{' ;
RBRACE : '}' ;

COMMA : ',' ;
COLON : ':' ;
SEMICOLON : ';' ;


// assignment and assignment operators
ASSIGN : '=' ;

INCREMENT : '++' ;
DECREMENT :	'--' ;

PLUSEQ : '+=' ;
MINUSEQ : '-=' ;
TIMESEQ : '*=' ;


// operators
EQ : '===' ;
NEQ : '!==' ;

LT : '<' ;
LTE : '<=' ;
GT : '>' ;
GTE : '>=' ;

PLUS : '+' ;
MINUS : '-' ;
TIMES :	'*' ;
DIVIDE : '/' ;
MOD : '%' ;

AND : '&&' ;
OR : '||' ;
NOT : '!' ;

DOT : '.' ;


// reserved words 
BREAK : 'break' ;
CONTINUE : 'continue' ;
DO : 'do' ;
ELSE : 'else' ;
FUNCTION : 'function' ;
IF : 'if' ;
RETURN : 'return' ;
VAR : 'var' ;
WHILE : 'while' ;


// types 
TYPE_BEGIN : '/*:' ;
TYPE_BOOLEAN : '#bool' ;
TYPE_INTEGER : '#int' ;
TYPE_STRING : '#str' ;
TYPE_VOID  : '#void' ;
TYPE_ARRAY : '#arr' ;
TYPE_OBJECT : '#obj' ;
TYPE_FUNCTION : '#fun' ;
TYPE_END : ':*/' ;


// literals 
NULL : 'null' ;
TRUE : 'true' ;
FALSE : 'false' ;

INT_LITERAL : '0' | NONZERO DIGIT* ;

STRING_LITERAL
	: (('"' (~["\n] | '\\\"')* '\"')
		| ('\'' (~['\n] | '\\\'')* '\''))
	// once we translate escape sequences, our original method of testing will break,
	// at least for string literals, both because an embedded newlines will break a string
	// literal altogether and even without that the idempotency between
	// "\t" and an actual tab will be lost.
	
	// comment out for ease of unparsing
	{ setText(StringHelper.unescape(getText())); }
	;

	

// identifiers
ID : LETTER ID_CHAR* ;



// helper tokens
fragment NONZERO : [1-9] ;
fragment DIGIT : [0-9] ;
fragment LETTER : [a-zA-Z] ;
fragment ID_CHAR : DIGIT | LETTER | '_' ;
fragment END_LINE : '\n' | '\r' | '\n\r' ;
fragment NON_END_LINE : ~[\n\r] ;


// comments and white space
WHITESPACE : [ \n\r\t\f]+ -> skip ;

SINGLE_LINE_COMMENT : '//' NON_END_LINE* END_LINE -> skip ;

// multiline comments make use of ANTLR's mode stack
// assumption is that comments neither begin nor end with ':'
// lest they conflict with type annotations.
MULTILINE_COMMENT_BEGIN : '/*' -> skip , pushMode(MC) ;

mode MC;
COMMENTED_TYPE_BEGIN : '/*:' -> skip ;
COMMENTED_TYPE_END : ':*/' -> skip ;

COMMENTED_COMMENT_BEGIN : '/*' -> skip , pushMode(MC) ;
COMMENTED_COMMENT_END : '*/' -> skip , popMode ;
COMMENTED_IGNORABLE : . -> skip ;
