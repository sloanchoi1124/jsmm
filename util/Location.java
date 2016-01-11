package util;

public class Location {
	private final int beginLine_, beginColumn_, endLine_, endColumn_;

	public Location(int beginLine, int beginColumn, int endLine, int endColumn) {
		beginLine_ = beginLine;
		beginColumn_ = beginColumn;
		endLine_ = endLine;
		endColumn_ = endColumn;
	}
	
	public Location(int beginLine, int beginColumn) {
		beginLine_ = beginLine;
		beginColumn_ = beginColumn;
		endLine_ = beginLine;
		endColumn_ = beginColumn;
	}
	
	public int getBeginLine() {
		return beginLine_;
	}

	public int getBeginColumn() {
		return beginColumn_;
	}

	public int getEndLine() {
		return endLine_;
	}

	public int getEndColumn() {
		return endColumn_;
	}

	public String toString() {
		if (beginLine_ == endLine_ && beginColumn_ == endColumn_) {
			return "[" + beginLine_ + ":" + beginColumn_ + "]";
		}
		else {
			return "[" + beginLine_ + ":" + beginColumn_ + "-" +  endLine_ + ":" + endColumn_ + "]"; 
		}
	}
}
