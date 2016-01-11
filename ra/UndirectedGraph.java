package ra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

public class UndirectedGraph<T> extends Graph<T>{
	@Override
	public void addEdge(Vertex<T> vertex1, Vertex<T> vertex2) {
		// TODO Auto-generated method stub
		if (vertex1.getNeighbors().contains(vertex2) == false) {
			vertex1.addOutgoingEdge(vertex2);
			vertex2.addOutgoingEdge(vertex1);			
		}

	}
	
	public void removeEdge(Vertex<T> vertex1, Vertex<T> vertex2) {
		vertex1.removeOutgoingEdges(vertex2);
		vertex2.removeOutgoingEdges(vertex1);
	}
	
	public void removeVertex(Vertex<T> vertex) {
		//remove all of its edges
		for (Vertex temp: vertex.getNeighbors()) {
			this.removeEdge(vertex, temp);
		}
		//remove the vertex from the vertex list
		this.vertices.remove(vertex);
	}
	
	public int getVertexCount() {
		return this.vertices.size();
	}
	
	public List<Vertex<T>> getVertices() {
		return this.vertices;
	}
	
	//coloring r4- r11
	public Map<T,Integer> coloring(int colorNumber) {
		Map<T, Integer> returnMap = new HashMap<T, Integer>();
//		//initialise
		Stack<List<Vertex<T>>> stack = new Stack<List<Vertex<T>>>();
//		System.out.println("---------------------------");
//		System.out.println("---------------------------");
//		System.out.println("before the stack is initialized, print the graph and its neighbors first");
//		for (Vertex<T> v: this.vertices) {
//			System.out.println(v + " has neighbors " + v.getNeighbors());
//		}
//		System.out.println("---------------------------");
//		System.out.println("---------------------------");
//		System.out.println("before the stack is initialized, print the graph and the outgoing edges of each node");
//		for (Vertex<T> v: this.vertices) {
//			System.out.println(v + " has edges" + v.getOutgoingEdges());
//		}

		//System.out.println("stack is initialized");
		//simplify
		while (true) {
			List<Vertex<T>> list = new ArrayList<Vertex<T>>();
			if (this.vertices.size() == 0) {
				//System.out.println("no need to color");
				break;
			}
			for (Iterator<Vertex<T>> iterator = this.vertices.iterator(); iterator.hasNext(); ){
				Vertex<T> node = iterator.next();
				if (node.getNeighbors().size() < colorNumber) {
					//put the node and its neighbors on the stack 
					//System.out.println(node + "has neighbors" + node.getNeighbors());
					list.add(node);
					list.addAll(node.getNeighbors());
					stack.push(list);
					//then remove the node from the graph
					this.removeVertex(node);
					break;
				}
			}
			//if there is no node with less than N edges, push any node and its neighbors
			if (list.isEmpty() == true) {
				//push the first node in this.vertices 
				Vertex<T> anyNode = this.vertices.get(0);
				
				list.add(anyNode);
				list.addAll(anyNode.getNeighbors());
				stack.push(list);
				this.removeVertex(anyNode);
			}
			if (this.vertices.size() > 0) {
				continue;
			} else {
				//System.out.println("simplfy is done");
				break;
			}
		}
		//select
		//suppose before the graph is colored, its color label is -1
		//System.out.println("the stack looks like this");
		//System.out.println(stack);
		while (stack.isEmpty() == false) {
			List<Vertex<T>> toColor = stack.pop();
			
			List<Integer> availableColors = new ArrayList<Integer>();
			for (int i = 0; i < colorNumber; i++) {
				availableColors.add(i);
			}
			//System.out.println("before removing colors: " + availableColors);
			
			for (int i = 1; i < toColor.size(); i++) {
				if (toColor.get(i).getColorLabel() == -1) {
					throw new RuntimeException ("THIS NODE SHOULD HAVE ALREADY BEEN COLORED!!!!");
				} else {
					//System.out.println(toColor.get(i) + " is already colored");
					int colorToRemove = toColor.get(i).getColorLabel();
					availableColors.remove((Integer)colorToRemove);
				}
			}
			//System.out.println("available colors: " + availableColors);
			if (availableColors.isEmpty() == true) {
				throw new RuntimeException("RUNNING OUT OF COLORS! COLORING FAILED");
			} else {
				if (toColor.get(0).getColorLabel() != -1) {
					throw new RuntimeException("THIS NODE SHOULDN'T HAVE BEEN COLORED!!!");
				} else {
					toColor.get(0).setColorLabel(availableColors.get(0));
					//System.out.println(toColor.get(0) + " is now colored");
					returnMap.put((T) toColor.get(0).getData(), toColor.get(0).getColorLabel() + 4);
				}
			}
			//System.out.println("LOOP");
		}
		return returnMap;
	}
    public void printColoringResult() {
    	for (Vertex<T> node: this.vertices) {
    		//System.out.println(node);
    	}
    }
    
    public void printGraph() {
    	for (Vertex v: this.vertices) {
    		//System.out.println(v);
    		List<Edge> edges = v.getOutgoingEdges();
    		for (Edge e: edges) {
    			//System.out.println(e);
    		}
    	}
    }
    
    public void printGraphInfo() {
    	for (Vertex v: this.vertices) {
    		//System.out.println("VERTEX" + v.getData());
    		List<Edge> edges = v.getOutgoingEdges();
    		for (Edge e: edges) {
    			//System.out.println(e.edgeInfo());
    		}
    	}
    }
    
    public static void main(String args[]) {
    	Vertex a = new Vertex<Integer>("a");
    	Vertex b = new Vertex<Integer>("b");
    	Vertex c = new Vertex<Integer>("c");
    	Vertex d = new Vertex<Integer>("d");
    	Vertex e = new Vertex<Integer>("e");
    	Vertex f = new Vertex<Integer>("f");
    	UndirectedGraph g = new UndirectedGraph<Integer>();
    	g.addVertex(a);
    	g.addVertex(b);
    	g.addVertex(c);
    	g.addVertex(d);
    	g.addVertex(e);
    	g.addVertex(a);
    	g.addVertex(a);
    	g.addEdge(a, b);
    	g.addEdge(a, b);
    	g.addEdge(a, b);
    	g.addEdge(a, c);
    	g.addEdge(a, d);
    	g.addEdge(b, c);
    	g.addEdge(b, d);
    	g.addEdge(b, e);
    	g.addEdge(c, d);
    	g.addEdge(c, e);
    	g.addEdge(d, e);
    	g.printGraph();
    	g.removeVertex(e);
//    	g.coloring(4);
    	//System.out.println("----------");
//    	System.out.println("----------");
//    	g.printColoringResult();
    	g.printGraph();
    	
    }
}
