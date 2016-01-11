 
parser grammar JsmmParser;

options {
	tokenVocab=JsmmLexer;
}

program : declaration* statement* ;


declaration
	: VAR type_annotation? ID ASSIGN expression SEMICOLON       #InitDecl
	| VAR type_annotation ID (COMMA ID)* SEMICOLON              #UninitDecl
	;


statement
	: lvalue ASSIGN expression SEMICOLON                        #AssignStmt
	| lvalue (INCREMENT | DECREMENT) SEMICOLON                  #IncDecStmt
	| lvalue (PLUSEQ | MINUSEQ | TIMESEQ) expression SEMICOLON  #AssignOpStmt
	| IF LPAREN expression RPAREN statement (ELSE statement)?   #IfStmt
	| WHILE LPAREN expression RPAREN statement                  #WhileStmt
	| DO statement WHILE LPAREN expression RPAREN SEMICOLON     #DoWhileStmt
	| BREAK SEMICOLON                                           #BreakStmt
	| CONTINUE SEMICOLON                                        #ContinueStmt
	| RETURN expression? SEMICOLON                              #ReturnStmt
	| expression LPAREN expressions? RPAREN SEMICOLON           #ProcCallStmt
	| LBRACE statement* RBRACE                                  #CompoundStmt
	;


lvalue
	: ID
	| lvalue DOT ID
	| lvalue LBRACK expression RBRACK
	;


expressions : expression (COMMA expression)* ;

expression
	: LPAREN expression RPAREN                                 #ParenExp
	| expression DOT ID                                        #AccessExp
	| expression LBRACK expression RBRACK                      #SubscriptExp
	| expression LPAREN expressions? RPAREN                    #FunCallExp
	| NOT expression                                           #NotExp
	| MINUS expression                                         #UminusExp
	| expression (TIMES | DIVIDE | MOD) expression             #MultiplicativeExp
	| expression (PLUS | MINUS) expression                     #AdditiveExp
	| expression (LT | LTE | GT | GTE | EQ | NEQ) expression   #CompareExp
	| expression AND expression                                #AndExp
	| expression OR expression                                 #OrExp

	| LBRACK expressions? RBRACK                               #ArrayExp
	| LBRACE json_pairs? RBRACE                                #ObjectExp
	
	| FUNCTION LPAREN parameters? RPAREN type_annotation
	     LBRACE	declaration* statement* RBRACE                 #FunExp
	
	| type_annotation expression                               #TypedExp

	| NULL            #NullExp
    | TRUE            #TrueExp
    | FALSE           #FalseExp
    | INT_LITERAL     #IntExp
    | STRING_LITERAL  #StringExp
    
    | ID              #IdExp
;


json_pairs : json_pair (COMMA json_pair)* ;
	
json_pair :
	ID COLON expression ;
	

type_annotation : TYPE_BEGIN type TYPE_END ; 

types : type (COMMA type)* ;

id_type_pairs : id_type_pair (COMMA id_type_pair)* ;

id_type_pair : ID COLON type ;

type
	: TYPE_BOOLEAN
	| TYPE_INTEGER
	| TYPE_STRING
	| TYPE_VOID
	| TYPE_ARRAY LBRACK type RBRACK
	| TYPE_OBJECT LBRACE id_type_pairs? RBRACE 
	| TYPE_FUNCTION LPAREN types? SEMICOLON type RPAREN
	;
 

parameters :
	  parameter
	| parameter (COMMA parameter)*
	;
	
parameter : type_annotation ID ;
