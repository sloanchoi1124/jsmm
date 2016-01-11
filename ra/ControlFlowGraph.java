package ra;

import ir.Ir.FlatIrTraceFunction;
import ir.Ir.IfIrStatement;
import ir.Ir.IrStatement;
import ir.Ir.JumpIrStatement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.Label;
import util.Temp;

public class ControlFlowGraph extends Graph<IrStatement> {
//	FlatIrTraceFunction function;
	Temp returnTemp;
	Map<Integer, Vertex<IrStatement>> indexMap;
	
	private ControlFlowGraph() {
		super();
		indexMap = new HashMap<Integer, Vertex<IrStatement>>();
	}
	
	@Override
	// Define CFG as a directed graph
	public void addEdge(Vertex<IrStatement> vertex1, Vertex<IrStatement> vertex2) {
		vertex1.addOutgoingEdge(vertex2);
	}

	public static ControlFlowGraph createCFG(FlatIrTraceFunction function) {
		ControlFlowGraph cfg = new ControlFlowGraph();
		cfg.returnTemp = function.returnValue;
		List<IrStatement> statements = function.flatBodyTraces;
		
		// First pass (add vertices to CFG and vertices/labels to corresponding maps)
		Map<Label,Integer> labelMap = new HashMap<Label,Integer>();
		int statementIndex = 0;
		for (IrStatement statement : statements) {
			Vertex<IrStatement> currentVertex = new Vertex<IrStatement>(statement, statementIndex);
			cfg.addVertex(currentVertex);
			cfg.indexMap.put(statementIndex, currentVertex);
			if (statement.label != null) {
				labelMap.put(statement.label, statementIndex);
			}
			statementIndex++;
		}
		
		// Second pass (add necessary edges)
		statementIndex = 0;
		for (IrStatement statement : statements) {
			Vertex<IrStatement> currentVertex = cfg.indexMap.get(statementIndex);
			Vertex<IrStatement> nextVertex;
			if (statement instanceof IfIrStatement) {
				IfIrStatement ifStatement = (IfIrStatement) statement;
				// Handle THEN
				nextVertex = cfg.indexMap.get(labelMap.get(ifStatement.thenLabel));
				cfg.addEdge(currentVertex, nextVertex);
				// Handle ELSE
				nextVertex = cfg.indexMap.get(labelMap.get(ifStatement.elseLabel));
				cfg.addEdge(currentVertex, nextVertex);
			} else if (statement instanceof JumpIrStatement) {
				JumpIrStatement jumpStatement = (JumpIrStatement) statement;
				nextVertex = cfg.indexMap.get(labelMap.get(jumpStatement.target));
				cfg.addEdge(currentVertex, nextVertex);
			} else {
				nextVertex = cfg.indexMap.get(statementIndex + 1);
				if (nextVertex != null) {
					cfg.addEdge(currentVertex, nextVertex);
				}
			}
			statementIndex++;
		}
		
		return cfg;
	}
	
}
