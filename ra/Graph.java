package ra;

import java.util.ArrayList;
import java.util.List;

public abstract class Graph<T> {
    List<Vertex<T>> vertices;
	
	public Graph () {
		vertices = new ArrayList<Vertex<T>>();
	}
	
	public boolean addVertex(Vertex<T> vertex){
		boolean added = false;
		if (vertices.contains(vertex) == false) {
			added = vertices.add(vertex);
		}
		return added;
	}
	
	public List<Vertex<T>> getVertices() {
		return this.vertices;
	}
	
	public abstract void addEdge(Vertex<T> vertex1, Vertex<T> vertex2);
	
}
