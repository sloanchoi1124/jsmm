package util;

public class Uid implements Comparable<Uid> {
	private static int count_ = 0;

	private int id_;
	
	public Uid() { 
		id_ = count_;
		count_++;
	}
	
	public int compareTo(Uid other) {
		return this.id_ - other.id_;
	}
	
	public boolean equals(Object object) {
		Uid other = (Uid) object;
		return this.id_ == other.id_;
	}

	public String toString() {
		return "" + id_;
	}
}
