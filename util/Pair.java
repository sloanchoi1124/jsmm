package util;


public class Pair<A,B> implements Comparable<Pair<A,B>> {
	public final A first;
	public final B second;
	
	public Pair(A first, B second) {
		this.first = first;
		this.second = second;
	}

	public String toString() {
		return "<" + first + ", " + second + ">";
	}
	
	public boolean equals(Object object) {
		if (object instanceof Pair) {
			Pair<A,B> other = (Pair<A,B>) object;
			return first.equals(other.first) && second.equals(other.second);
	    }
		else {
			return false;
		}
	}
	
	public int compareTo(Pair<A,B> other) {
		Comparable<A> cFirst = (Comparable<A>) first;
		int comparison = cFirst.compareTo(other.first);
		if (comparison == 0) {
			Comparable<B> cSecond = (Comparable<B>) second;
			comparison = cSecond.compareTo(other.second);
		}
		return comparison;
	}

}
	
