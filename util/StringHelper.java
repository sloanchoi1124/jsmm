package util;

// TODO: add full set of escape characters

public class StringHelper {

	/* translates a string into its unescaped version (removing starting and ending quotes) */
	public static String unescape(String s) {
        int n = s.length() - 1; // to remove final quote
        StringBuilder sb = new StringBuilder(n);
        int i = 1; // to skip initial quote
        while(i < n) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < n) {
                i++;
                c = s.charAt(i);
                switch (c) {
                case '\\': 
                    sb.append('\\');
                    break;
                case 'n':
                    sb.append('\n');
                    break;
                case 't':
                    sb.append('\t');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                default:
                    sb.append('\\');
                    sb.append(c);
                    break;
                }
            } else {
                sb.append(c);
            }
            i++;
        }
        return sb.toString();
    }
	
	
	/* translates a string into its escaped version, adding initial and ending quotes */
	public static String escape(String s) {
		int n = s.length();
		StringBuilder sb = new StringBuilder(2 * n);
		sb.append('"');
		for(int i = 0; i < n; i++) {
			char c = s.charAt(i);
			switch (c) {
			case	'\\':
				sb.append("\\\\");
				break;
			case	'\n':
				sb.append("\\n");
				break;
			case	'\t':
				sb.append("\\t");
				break;
			case	'\r':
				sb.append("\\r");
			case	'"':
				sb.append("\\\"");
				break;
			default:
				sb.append(c);
				break;
			}
		}
		sb.append('"');
		return sb.toString();
	}

}
