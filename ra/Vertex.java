package ra;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Vertex<T> {
	//what is stored in the vertex
	private T data;
	private int index;
	private int colorLabel;
	private String label;
	private List<Edge> outgoingEdges;
	
	//constructor
	public Vertex (T data){
		this.data = data;
		this.outgoingEdges = new ArrayList<Edge>();
		this.colorLabel = -1;
		this.label = "null";
	}
	
	public Vertex (T data, int index){
		this.data = data;
		this.index = index;
		this.outgoingEdges = new ArrayList<Edge>();
		this.colorLabel = -1;
		this.label = "null";
	}
	
	public Vertex (String label) {
		this.outgoingEdges = new ArrayList<Edge>();
		this.colorLabel = -1;
		this.label = label;
	}
	
	public String toString() {
		String s = "vertex " + this.data + " color:" + this.colorLabel;
		return s;
	}
	
	@Override
	public boolean equals(Object v) {
		Vertex<T> t = (Vertex<T>) v;
		//System.out.println("equals is called");
		return this.data.equals(t.data);
	}
	public String vertexInfo() {
		String s = "vertex: " + this.data;
		return s;
	}

	public int getIndex() {
		return this.index;
	}
	public T getData() {
		return this.data;
	}
	
	public void setData(T data) {
		this.data = data;
	}
	
	public int getColorLabel() {
		return this.colorLabel;
	}
	
	public void setColorLabel(int i) {
		this.colorLabel = i;
	}
	
	public String getLabel() {
		return this.label;
	}
	
	public void addOutgoingEdge (Vertex<T> to) {
		Edge edge = new Edge(this, to);
		this.outgoingEdges.add(edge);
	}
	
	public List<Edge> getOutgoingEdges() {
		return this.outgoingEdges;
	}
	
	public int getOutgoingEdgesCount() {
		return this.outgoingEdges.size();
	}
	
	public List<Vertex<T>> getNeighbors() {
		List<Vertex<T>> neighbors = new ArrayList<Vertex<T>>();
		for (Edge e: this.outgoingEdges) {
			neighbors.add((Vertex<T>) e.getToVertex());
		}
		return neighbors;
	}
	
	public boolean removeOutgoingEdges(Vertex<T> vertex) {
		for (Iterator<Edge> iterator = this.outgoingEdges.iterator(); iterator.hasNext(); ){
			Edge temp = iterator.next();
			if (temp.getToVertex() == vertex) {
				iterator.remove();
				return true;
			}
		}
		return false;
	}
	
	public static void main (String args[]) {
		Vertex test = new Vertex<Integer>("a");
		//System.out.println(test);
		test.setColorLabel(4);
		//System.out.println(test);
		
	}
	
}
