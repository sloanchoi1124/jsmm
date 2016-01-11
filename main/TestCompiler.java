package main;



import ir.Ir.FlatIrTraceFunction;
import ir.Ir.IrBlockProgram;
import ir.Ir.IrProgram;
import ir.Ir.IrStatement;
import ir.Ir.IrTraceProgram;
import ir.Ir.IrTraceProgram2;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;

import parser.JsmmLexer;
import parser.JsmmParser;
import parser.JsmmParser.ProgramContext;
import ra.ControlFlowGraph;
import ra.InterferenceGraph;
import ra.LivenessAnalysis;
import ra.LivenessAnalysis.LivenessEntry;
import translate.Flatten;
import translate.Translate;
import typecheck.TypeCheck;
import util.Label;
import util.Location;
import util.Temp;
import util.Uid;
import ast.Ast.Program;
import ast.Type;
import astunparse.AstUnparser;
import bat.BlocksandTraces;
import buildast.BuildAst;
import cflat.Cflat;



public class TestCompiler {
	
	static final int WORD_SIZE = 4;	
	
	// set to false to turn off display of various compiler stages
	private static boolean verbosity = false;
	
	public static void main(String[] args) throws IOException {
		// if any additional command-line argument passed in, turn verbosity on
		verbosity = args.length > 0;
		try {
			CharStream charStream = new ANTLRInputStream(System.in);
			Lexer lexer = new JsmmLexer(charStream);

			CommonTokenStream tokenStream = new CommonTokenStream(lexer); 
			JsmmParser parser = new JsmmParser(tokenStream);
			
			display("parsing...");
			ProgramContext programParseTree = parser.program();
			if (parser.getNumberOfSyntaxErrors() > 0) {
				throw new RuntimeException("syntax error");
			}
			display("...parsing completed.");
			
			display("converting to AST...");
			Program program = BuildAst.buildAst(programParseTree);
			display("...conversion completed.");
			
			display("symbol table before type checking:");
			displaySymbols(program);
			
			display("Type checking... ");
			Map<Uid, Type> expressionTypeMap = TypeCheck.typeCheck(program);
			display(" ...type checked successfully.\n");
			
			display("symbol table after type checking:");
			displaySymbols(program);
			
			
			display("converting from AST to IR... ");
			IrProgram irProgram = Translate.translate(program, expressionTypeMap, WORD_SIZE);
			display("... converted successfully.");
				
			display("flattening IR...");
			Flatten.flatten(irProgram);
			display("... flattened successfully.");
			
			
			//
			display("blockifying");
			IrBlockProgram bp = BlocksandTraces.blockify(irProgram);
			display("done blockifying");
			
			//
			display("traceifying");
			IrTraceProgram tp = BlocksandTraces.traceify(bp);
			display("done traceifying");

			//
			display("flattening");
			
			IrTraceProgram2 firtp = BlocksandTraces.flatTraceProgram(tp);
			
			
			FlatIrTraceFunction firtf_main = firtp.functions.get(new Label("main"));
			
			
			ControlFlowGraph cfg = ControlFlowGraph.createCFG(firtf_main);
			List<LivenessEntry> livenessEntries = LivenessAnalysis.getLivenessTable(cfg);
			
			InterferenceGraph ifg = new InterferenceGraph(livenessEntries);
			ifg.checkInterference();

			
			
			Map<Temp, Integer> temps = ifg.getInterferenceGraph().coloring(7);
			String machineCode = arm.MachineCode.machineCode(firtp, temps);
			System.out.println(machineCode);
			
			
		} catch (Exception e) {
			System.out.println (e) ;
			throw e;
		}
	}

	private static void display(String s) {
		if (verbosity) {
			System.out.println(s);
		}
	}
	
	private static void displaySymbols(Program p) {
		for (Uid uid : p.globals) {
			String s = "";
			s += p.symbolTable.getName(uid);
			s += " : ";
			Type t = p.symbolTable.getType(uid);
			if (t != null) {
				s += AstUnparser.typeToString(t);
			} else {
				s += "<no type>";
			}
			s += " : ";
			Location l = p.symbolTable.getLocation(uid);
			if (l != null) {
				s += l;
			} else {
				s += "[no loc]";
			}
			display(s);		
		}
	}

}
