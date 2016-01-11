package util;


public class Temp implements Comparable<Temp> {

	private static int count_ = 0;

	private int id_;
	
	
	public Temp() { 
		id_ = count_;
		count_++;
	}
	
	
	public int compareTo(Temp other) {
		return this.id_ - other.id_;
	}
	
	
	public boolean equals(Object object) {
		Temp other = (Temp) object;
		return this.id_ == other.id_;
	}
	
	
	public String toString() {
		return "t" + id_;
	}

}
