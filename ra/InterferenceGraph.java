package ra;

import ir.Ir.AssignmentIrStatement;
import ir.Ir.IrStatement;
import ir.Ir.TempIrExpression;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ra.LivenessAnalysis.LivenessEntry;
import translate.Translate;
import util.Pair;
import util.Temp;

public class InterferenceGraph {
	private List<LivenessEntry> iList;
	private List<Pair<Temp, Temp>> pairList;
	private UndirectedGraph<Temp> udg;
	
	public InterferenceGraph (List<LivenessEntry> entryList) {
		this.iList = entryList;
		this.udg = new UndirectedGraph<Temp>();
		this.pairList = new ArrayList<Pair<Temp,Temp>>();
	}
	
	public UndirectedGraph<Temp> getInterferenceGraph() {
		this.checkInterference();
		this.buildGraph();
		return this.udg;
	}
	
	public void buildGraph() {
		for (Pair<Temp, Temp> pair: pairList) {
			Temp first = pair.first;
			Temp second = pair.second;
			Vertex<Temp> a = new Vertex<Temp>(first);
			Vertex<Temp> b = new Vertex<Temp>(second);
			if (this.udg.getVertices().contains(a) == false && this.udg.getVertices().contains(b) == false) {
				this.udg.addVertex(a);
				this.udg.addVertex(b);
				this.udg.addEdge(a, b);
			} else if(this.udg.getVertices().contains(a) == true && this.udg.getVertices().contains(b) == false) {
				this.udg.addVertex(b);
				for (Vertex<Temp> temp: this.udg.getVertices()) {
					if (temp.equals(a)) {
						this.udg.addEdge(b, temp);
						break;
					}
				}
			} else  if (this.udg.getVertices().contains(a) == false && this.udg.getVertices().contains(b) == true) {
				this.udg.addVertex(a);
				for (Vertex<Temp> temp: this.udg.getVertices()) {
					if (temp.equals(b)) {
						this.udg.addEdge(a, temp);
						break;
					}
				}
			} else{
				for(Vertex<Temp> a_eq: this.udg.getVertices()) {
					if (a_eq.equals(a)) {
						for (Vertex<Temp> b_eq: this.udg.getVertices()) {
							if (b_eq.equals(b)) {
								this.udg.addEdge(a_eq, b_eq);
							}
						}
					}
				}
			}
		}
	}
	
	public List<Pair<Temp, Temp>> getList() {
		return pairList;
	}
	
	public void checkInterference() {
		for (LivenessEntry iEntry: this.iList) {
			//only check if i is an assignmentIrStatement; otherwise, kill[i] will be empty
			if (iEntry.i instanceof AssignmentIrStatement) {
				AssignmentIrStatement i = (AssignmentIrStatement) iEntry.i;
				for (Temp x: iEntry.kill) {
					//System.out.println(x + "loop through kill[i]");
//					System.out.println(iEntry.out);
					for (Temp y: iEntry.out) {
						//System.out.println("loop through out[i]");
						//cannot interfere with itself
						if (x == y) {
							//System.out.println(x + "cannot interfere with itself");
							continue;
						} else {
							//check if it is x: =y
							if (i.lhs instanceof TempIrExpression && i.rhs instanceof TempIrExpression) {
								//System.out.println("lhs and rhs are both TempIrExpression");
								TempIrExpression lhs_ir = (TempIrExpression) i.lhs;
								TempIrExpression rhs_ir = (TempIrExpression) i.rhs;
								if (lhs_ir.temp.equals(x) && rhs_ir.temp.equals(y)) {
									//x := y, !interfere
									//System.out.println("x := y,"+ x + " and " + y + " don't interfere");
									continue;
								} else {
									//x and y must interfere
									Pair<Temp, Temp> pair = new Pair<Temp, Temp>(x, y);
									pairList.add(pair);
									//System.out.println("add " + pair + " to the list");
								}
							} else {
								//then x and y must interfere
								Pair<Temp, Temp> pair = new Pair<Temp, Temp>(x, y);
								pairList.add(pair);
								//System.out.println("add " + pair + " to the list");
							}
						}
					}
				}
			}
		}

	}
	public static void main(String args[]) {
		
	}
	
}
