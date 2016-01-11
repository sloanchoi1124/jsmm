package bat;

//import java.awt.Label;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import util.Label;
import ir.Ir.AssignmentIrStatement;
import ir.Ir.FlatIrTraceFunction;
import ir.Ir.IfIrStatement;
import ir.Ir.IrBlockFunction;
import ir.Ir.IrBlockProgram;
import ir.Ir.IrFunction;
import ir.Ir.IrProgram;
import ir.Ir.IrStatement;
import ir.Ir.IrTraceFunction;
import ir.Ir.IrTraceProgram;
import ir.Ir.IrTraceProgram2;
import ir.Ir.JumpIrStatement;
import ir.Ir.NopIrStatement;
import ir.RelationalOperator;

public class BlocksandTraces {
	// abstract class? what is that?

	public BlocksandTraces() {
		// TODO Auto-generated constructor stub
	}

	public static IrBlockProgram blockify(IrProgram program) {
		IrProgram p = program;

		Map<Label, IrBlockFunction> r = new HashMap<Label, IrBlockFunction>();

		for (Map.Entry<Label, IrFunction> pair : p.functions.entrySet()) {
			IrFunction f = pair.getValue();
			Map<Label, List<IrStatement>> bodyBlocks = new HashMap<Label, List<IrStatement>>();
			List<IrStatement> bbl = new ArrayList<IrStatement>();
			Label x = null;
			for (IrStatement statement : f.body) {
				if (statement instanceof AssignmentIrStatement) {
					AssignmentIrStatement y = (AssignmentIrStatement) statement;
				}
				if (isLabelStatement(statement) && bbl.isEmpty()) {
					x = statement.label;
					bbl.add(statement);
				}
				else if (isLabelStatement(statement) && !bbl.isEmpty()) {
					if (isJumpStatement(statement)) {
						bodyBlocks.put(x, bbl);
						x = statement.label;
						if (! isJumpStatement(bbl.get(bbl.size() -1))) {
							// the last element of the previous block is not a jump
							// gotta add a jump to the current thing
							JumpIrStatement j = new JumpIrStatement(x);
							bbl.add(j);
						}
						bbl = new ArrayList<IrStatement>();
						bbl.add(statement);
					}
					else {
						bodyBlocks.put(x, bbl);
						x = statement.label;
						if (!isJumpStatement(bbl.get(bbl.size() -1))) {
							JumpIrStatement j = new JumpIrStatement(x);
							bbl.add(j);
						}
						bbl = new ArrayList<IrStatement>();
						bbl.add(statement);
					}
				}
				else if (!isLabelStatement(statement)) {
					if (isJumpStatement(statement)) {
						bbl.add(statement);
						bodyBlocks.put(x,bbl);
						bbl = new ArrayList<IrStatement>();
					}
					else {
						bbl.add(statement);
					}
				}
			}
			bodyBlocks.put(x, bbl);
			IrBlockFunction y = new IrBlockFunction(f.functionLabel, f.parameters, bodyBlocks, f.bodyLabel,
					f.exitLabel, f.returnValue);
			// is it okay that I'm making a new label? It seems to work...
			r.put(f.functionLabel, y);
		}
		return new IrBlockProgram(p.globals,r, p.stringLiterals);
	}

	// checks to see if it's a label statement
	private static boolean isLabelStatement(IrStatement statement) {
		if (statement.label != null) {
			return true;
		}
		else {
			return false;
		}
	}

	// checks to see if it's a jump or conditional statement
	private static boolean isJumpStatement(IrStatement statement) {
		if (statement instanceof JumpIrStatement) {
			return true;
		}
		else if (statement instanceof IfIrStatement) {
			return true;
		}
		else {
			return false;
		}
	}


	public static IrTraceProgram traceify(IrBlockProgram program) {
		IrBlockProgram p =  program;

		Map<Label, IrTraceFunction> r = new HashMap<Label, IrTraceFunction>();
		//IrTraceProgram takes in a Map<Label, IrTraceFunction>


		//Make a new IrTraceFunction at the end of making a trace and put it in r

		for (Map.Entry<Label, IrBlockFunction> pair : p.functions.entrySet()) {
			IrBlockFunction f = pair.getValue();
			//Mapping labels to their blocks
			Map<Label, List<IrStatement>> blox = new HashMap<Label, List<IrStatement>>();
			//Lists of lists of statements (list of blocks)
			List<List<IrStatement>> blocks = new ArrayList<List<IrStatement>>();
			//Whenever a label has been added to the block, add it here? or the label you take off?
			//perhaps make this out of the loop?
			List<Label> labelsUsed = new ArrayList<Label>();
			//BodyTraces - trace will go into bodyTraces
			List<List<List<IrStatement>>> bodyTraces = new ArrayList<List<List<IrStatement>>>();
			//Keeps track of each block's label and if it's been used in a trace or not
			Map<Label, Boolean> blockStatuses = new HashMap<Label, Boolean>();
			//Keeps track of the else labels
			List<Label> elses = new ArrayList<Label>();

			Map<List<IrStatement>, Label> h = new HashMap<List<IrStatement>,Label>();


			//Initializes all block statuses to false (unused)
			for (Entry<Label, List<IrStatement>> e : f.bodyBlocks.entrySet()) {
				blox.put(e.getKey(), e.getValue());
				blockStatuses.put(e.getKey(), false);
				h.put(e.getValue(), e.getKey());
			}

			//Initializes blocks list
			for (Entry<Label, List<IrStatement>> en : f.bodyBlocks.entrySet()) {
				List<IrStatement> blockBody = en.getValue();
				blocks.add(blockBody);
			}

			List<IrStatement> b = blox.get(f.bodyLabel);

			while (!blox.isEmpty()) {
				List<List<IrStatement>> T = new ArrayList<List<IrStatement>>();
				if (b == blox.get(f.bodyLabel)) {
					b = b;
				}
				else {
					if (!elses.isEmpty()) {
						b = blox.get(elses.get(0));
						elses.remove(0);
					}
					else {
						Map.Entry<Label,List<IrStatement>> entry = blox.entrySet().iterator().next();
						b = entry.getValue();
					}
				}
				Label l = h.get(b);
				while (!blockStatuses.get(l)) {
					IrStatement last = b.get(b.size() - 1);
					if (last instanceof IfIrStatement) {
						IfIrStatement iis = (IfIrStatement) last;
						if (labelsUsed.contains(iis.elseLabel)) {
							// so far I haven't been able to test if this works
							elses.add(iis.elseLabel);
							Label newThen = iis.elseLabel;
							RelationalOperator newOp = null;
							if (iis.op == RelationalOperator.NEQ) {
								newOp = RelationalOperator.EQ;
							}
							else if (iis.op == RelationalOperator.EQ) {
								newOp = RelationalOperator.NEQ;
							}
							else if (iis.op == RelationalOperator.LT) {
								newOp = RelationalOperator.GTE;
							}
							else if (iis.op == RelationalOperator.GT) {
								newOp = RelationalOperator.LTE;
							}
							else if (iis.op == RelationalOperator.GTE) {
								newOp = RelationalOperator.LT;
							}
							else if (iis.op == RelationalOperator.LTE) {
								newOp = RelationalOperator.GT;
							}
							IfIrStatement Fiis = new IfIrStatement(newOp, iis.left, iis.right, newThen, null);
							Fiis.label = iis.label;
							last = Fiis;
							b.remove(b.size()-1);
							b.add(last);
							T.add(b);
							blox.remove(l);
							blockStatuses.put(l, true);
							labelsUsed.add(l);
							b = blox.get(iis.thenLabel);
							l = h.get(b);
						}
						else {
							elses.add(iis.thenLabel);
							IfIrStatement Fiis = new IfIrStatement(iis.op, iis.left, iis.right, iis.thenLabel, null);
							Fiis.label = iis.label;
							last = Fiis;
							b.remove(b.size()-1);
							b.add(last);
							T.add(b);
							blox.remove(l);
							blockStatuses.put(l, true);
							labelsUsed.add(l);
							b = blox.get(iis.elseLabel);
							l = h.get(b);
						}
					}
					else if (last instanceof JumpIrStatement) {
						JumpIrStatement jis = (JumpIrStatement) last;
						if (jis.target == f.exitLabel && !labelsUsed.contains(f.exitLabel)) {
							T.add(b);
							blox.remove(l);
							blockStatuses.put(l, true);
							labelsUsed.add(l);
							b = blox.get(jis.target);
							l = h.get(b);
							T.add(b);
							blox.remove(l);
							blockStatuses.put(l,  true);
							labelsUsed.add(l);
						}
						else {
							if (labelsUsed.contains(jis.target)) {
								T.add(b);
								blox.remove(l);
								blockStatuses.put(l,  true);
								labelsUsed.add(l);

								break;
							}
							else {
								T.add(b);
								blox.remove(l);
								blockStatuses.put(l, true);
								labelsUsed.add(l);
								b = blox.get(jis.target);
								l = h.get(b);
							}
						}
					}
				}
				bodyTraces.add(T);
			}
			IrTraceFunction bt = new IrTraceFunction(f.functionLabel, f.parameters, bodyTraces, f.bodyLabel, f.exitLabel, f.returnValue);
			r.put(f.functionLabel, bt);
		}
		return new IrTraceProgram(p.globals, r, p.stringLiterals);
	}

	public static List<List<IrStatement>> flatTrace(IrTraceProgram program) {
		IrTraceProgram p = program;
		Map<Label, IrTraceFunction> functions = p.functions;
		List<List<IrStatement>> listFlatFuns = new ArrayList<List<IrStatement>>();
		for (Entry<Label, IrTraceFunction> entry : functions.entrySet()) {
			List<IrStatement> fun = new ArrayList<IrStatement>();
			IrTraceFunction f = entry.getValue();
			List<List<List<IrStatement>>> bts = f.bodyTraces;
			for (List<List<IrStatement>> i : bts) {
				for (List<IrStatement> ii : i) {
					for (IrStatement iii : ii) {
						
						fun.add(iii);
					}
				}
			}
			listFlatFuns.add(fun);
		}
		List<List<IrStatement>> fin = new ArrayList<List<IrStatement>>();
		for (List<IrStatement> e : listFlatFuns) {
			List<IrStatement> fun = new ArrayList<IrStatement>();
			for (int i = 0; i<e.size(); i++) {
				if (e.get(i) instanceof JumpIrStatement) {
					JumpIrStatement j = (JumpIrStatement) e.get(i);
					if (i+1 < e.size()) {
						if (j.target == e.get(i+1).label) {
							//nothing
						}
						else {
							fun.add(e.get(i));
						}
					}
					else {
						fun.add(e.get(i));
					}
				}
				else {
					fun.add(e.get(i));
				}
			}
			fin.add(fun);
			}
		return fin;
	}

	public static IrTraceProgram2 flatTraceProgram(IrTraceProgram tracePro) {
		IrTraceProgram p = tracePro;
		List<List<IrStatement>> flatstuff = flatTrace(p);
		Map<Label, IrTraceFunction> f = p.functions;
		Map<Label, FlatIrTraceFunction> ff = new HashMap<Label, FlatIrTraceFunction>();
		for (Entry<Label, IrTraceFunction> entry : f.entrySet()) {
			IrTraceFunction tf = entry.getValue();
			FlatIrTraceFunction flattf = new FlatIrTraceFunction(tf.functionLabel, tf.parameters, flatstuff.get(0), tf.bodyLabel, tf.exitLabel, tf.returnValue);
			flatstuff.remove(0);
			ff.put(entry.getKey(), flattf);
		}
		return new IrTraceProgram2(p.globals, ff, p.stringLiterals);
	}


}
