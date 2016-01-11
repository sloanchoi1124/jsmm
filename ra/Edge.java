package ra;

public class Edge {
	private Vertex<?> from;
	private Vertex<?> to;
	
	public Edge (Vertex<?> from, Vertex<?> to){
		this.from = from;
		this.to = to;
	}
	
	public Vertex<?> getFromVertex() {
		return this.from;
	}
	
	public Vertex<?> getToVertex() {
		return this.to;
	}
	
	public String toString() {
		String s = "Edge (" + this.from.getData()+ ", " + this.to.getData() + ")";
		return s;
	}
	
	public String edgeInfo() {
		String s = "Edge (" + this.from.vertexInfo() + ", " + this.to.vertexInfo() + ")";
		return s;
	}
	
	public static void main(String args[]) {
		Vertex a = new Vertex<Integer>("a");
		Vertex b = new Vertex<Integer>("b");
		Edge ab = new Edge(a, b);
		System.out.println(ab);
	}
	
}
