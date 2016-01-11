package util;


public class Label implements Comparable<Label> {

	private static int count_ = 0;
	
	private final String name_;

	
	// create a unique label based on the supplied base and suffix
	public Label(String base, String suffix) {
		StringBuilder sb = new StringBuilder("L_");
		if (base.length() > 0) {
			sb.append(base);
			sb.append("_");
		}
		if (suffix.length() > 0) {
			sb.append(suffix);
			sb.append("_");
		}
		sb.append("" + count_);
		name_ = sb.toString();
		count_++;
	}
	
	// create a unique label 
	public Label() {
		this("", "");
	}
	
	// create a label version of the name - this is not a unique label  
	public Label(String name) {
		name_ = name;
	}
	
	
	public String toString() { return name_; }
	
	
	public boolean equals(Object obj) {
		if (obj instanceof Label) {
			Label otherLabel = (Label) obj;
			return name_.equals(otherLabel.name_);
		}
		else return false;
	}
	
	
	public int compareTo(Label other) {
		return name_.compareTo(other.name_);
	}
	
	
	public int hashCode() { return name_.hashCode(); }

}
