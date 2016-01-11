package ra;

import ir.Ir.AssignmentIrStatement;
import ir.Ir.BinaryIrExpression;
import ir.Ir.CallIrExpression;
import ir.Ir.IfIrStatement;
import ir.Ir.IrExpression;
import ir.Ir.IrStatement;
import ir.Ir.JumpIrStatement;
import ir.Ir.LiteralIrExpression;
import ir.Ir.MemoryIrExpression;
import ir.Ir.NameIrExpression;
import ir.Ir.NopIrStatement;
import ir.Ir.ReturnIrStatement;
import ir.Ir.TempIrExpression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import util.Temp;

public class LivenessAnalysis {

	private static class GenKillEntry {
		public Set<Integer> succ;
		public Set<Temp> gen, kill;
		GenKillEntry() {
			succ = new HashSet<Integer>();
		}
	}

	// IrStatements possible = Assignment, If, Jump, Nop, Return
	// IrExpressions possible = Binary, Call, Literal, Memory, Name, Temp
	private static List<GenKillEntry> generateGenKillTable(ControlFlowGraph cfg) {
		List<GenKillEntry> table = new ArrayList<GenKillEntry>();
		// Populate table with blank entries
		for (int i=0; i<cfg.vertices.size(); i++) {
			table.add(new GenKillEntry());
		}
		// Fill with information
		for (Vertex<IrStatement> vertex: cfg.vertices) {
			GenKillEntry entry = table.get(vertex.getIndex());
			// Add successor indices
			for (Edge outgoingEdge : vertex.getOutgoingEdges()) {
				Vertex<IrStatement> neighbor = (Vertex<IrStatement>) outgoingEdge.getToVertex();
				entry.succ.add(neighbor.getIndex());
			}
			// Add gen/kill info
			IrStatement statement = vertex.getData();
			if (statement instanceof AssignmentIrStatement) {
				AssignmentIrStatement assignment = (AssignmentIrStatement) statement;
				entry.gen = getTempsUsed(assignment.rhs);
				entry.kill = getTempsUsed(assignment.lhs);
			} else if (statement instanceof IfIrStatement) {
				IfIrStatement ifStatement = (IfIrStatement) statement;
				entry.gen = getTempsUsed(ifStatement.left);
				entry.gen.addAll(getTempsUsed(ifStatement.right));
				entry.kill = new HashSet<Temp>();
			} else if (statement instanceof ReturnIrStatement) {
				ReturnIrStatement returnStatement = (ReturnIrStatement) statement;
				entry.gen = getTempsUsed(returnStatement.expression);
				entry.kill = new HashSet<Temp>();
			} else if (statement instanceof JumpIrStatement || statement instanceof NopIrStatement) {
				entry.gen = new HashSet<Temp>();
				entry.kill = new HashSet<Temp>();
			}
		}
		return table;
	}

	private static class InOutEntry {
		public Set<Integer> succ;
		public Set<Temp> in, out;
		InOutEntry(Set<Integer> succ) {
			this.succ = succ; // Nothing modifies successors at this point, so same set can be used.    new HashSet<Integer>(succ);
			this.in = new HashSet<Temp>();
			this.out = new HashSet<Temp>();
		}
		public boolean equals(InOutEntry other) {
			return memberEquality(succ, other.succ) && memberEquality(in, other.in) && memberEquality(out, other.out);
		}
		private boolean memberEquality(Set one, Set two) {
			Iterator iter = one.iterator();
			while (iter.hasNext()) {
				Object obj = iter.next();
				if (! two.contains(obj)) {
					return false;
				}
			}
			return true;
		}
	}
	
	private static List<InOutEntry> generateInOutTable(List<GenKillEntry> gkTable, Temp returnTemp) {
		int tableSize = gkTable.size();
		List<InOutEntry> newTable = new ArrayList<InOutEntry>();
		// Generate starting table
		for (GenKillEntry gkEntry : gkTable) {
			InOutEntry newEntry = new InOutEntry(gkEntry.succ);
			newTable.add(newEntry);
		}
		
		List<InOutEntry> oldTable;
		boolean equalityCheck;
		do {
			// Create and populate newTable
			oldTable = newTable;
			newTable = new ArrayList<InOutEntry>();
			for (InOutEntry oldEntry : oldTable) {
				newTable.add(new InOutEntry(oldEntry.succ));
			}
			// Calculate newTable in/out
			for (int i=tableSize-1; i>=0; i--) {
				InOutEntry newEntry = newTable.get(i);
				InOutEntry oldEntry = oldTable.get(i);
				GenKillEntry gkEntry = gkTable.get(i);
				
				// in[i] = gen[i] U (out[i]\kill[i])
				Set<Temp> newIn = new HashSet<Temp>(oldEntry.out);
				newIn.removeAll(gkEntry.kill);
				newIn.addAll(gkEntry.gen);
				newEntry.in = newIn;
				
				// out[i] = Union of all in[j] where j is all succ[i]
				Set<Temp> newOut = new HashSet<Temp>();
				if (newEntry.succ.isEmpty()) {
					if (returnTemp != null) {
						newOut.add(returnTemp);
					}
				} else {
					for (Integer j : oldEntry.succ) {
						InOutEntry otherEntry = oldTable.get(j);
//						System.out.println(otherEntry.in);
						newOut.addAll(otherEntry.in);
					}
					newEntry.out = newOut;
				}
			}
//			for (int i=0; i<tableSize; i++) {
//				System.out.println("" + oldTable.get(i).succ + " " + oldTable.get(i).in + " " + oldTable.get(i).out + "---" + newTable.get(i).succ + " " + newTable.get(i).in + " " + newTable.get(i).out);
//			}
//			System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
			equalityCheck = true;
			for (int i=0; i<tableSize; i++) {
				if (! newTable.get(i).equals(oldTable.get(i))) {
					equalityCheck = false;
					break;
				}
			}
		} while (! equalityCheck);
		
		return newTable;
	}
	
	private static Set<Temp> getTempsUsed(IrExpression exp) {
		Set<Temp> tempsUsed;
		if (exp instanceof BinaryIrExpression) {
			BinaryIrExpression bExp = (BinaryIrExpression) exp;
			tempsUsed = getTempsUsed(bExp.left);
			tempsUsed.addAll(getTempsUsed(bExp.right));
		} else if (exp instanceof CallIrExpression) {
			CallIrExpression cExp = (CallIrExpression) exp;
			tempsUsed = getTempsUsed(cExp.codeReference);
			for (IrExpression arg : cExp.arguments) {
				tempsUsed.addAll(getTempsUsed(arg));
			}
		} else if (exp instanceof MemoryIrExpression) {
			MemoryIrExpression mExp = (MemoryIrExpression) exp;
			tempsUsed = getTempsUsed(mExp.base);
			tempsUsed.addAll(getTempsUsed(mExp.index));
		} else if (exp instanceof TempIrExpression) {
			TempIrExpression tExp = (TempIrExpression) exp;
			tempsUsed = new HashSet<Temp>();
			tempsUsed.add(tExp.temp);
		} else if (exp instanceof LiteralIrExpression || exp instanceof NameIrExpression) {
			tempsUsed = new HashSet<Temp>();
		} else {
			throw new RuntimeException("Unexpected IrExpression " + exp.getClass());
		}
		return tempsUsed;
	}
	
	// Fully built entries to pass in for interference graph creation
	public static class LivenessEntry {
		public IrStatement i;
		public Set<Temp> kill, out;
		LivenessEntry(IrStatement i, Set<Temp> kill, Set<Temp> out) {
			this.i = i;
			this.kill = kill;
			this.out = out;
		}
	}
	
	public static List<LivenessEntry> getLivenessTable(ControlFlowGraph cfg) {
		List<LivenessEntry> livenessTable = new ArrayList<LivenessEntry>();
		List<GenKillEntry> genKillTable = generateGenKillTable(cfg);
		List<InOutEntry> inOutTable = generateInOutTable(genKillTable, cfg.returnTemp);
		for (int i=0; i<inOutTable.size(); i++) {
			IrStatement statement = cfg.indexMap.get(i).getData();
			GenKillEntry gke = genKillTable.get(i);
			InOutEntry ioe = inOutTable.get(i);
			livenessTable.add(new LivenessEntry(statement, gke.kill, ioe.out));
		}
		return livenessTable;
	}
	
	
}
