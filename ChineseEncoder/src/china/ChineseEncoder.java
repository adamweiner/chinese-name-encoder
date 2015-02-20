package china;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

// object that encoder will return
class CNEnc {
	boolean didWork;
	ArrayList<String[]> encs;
	double[] confidence;

	CNEnc(boolean work, ArrayList<String[]> enc, double[] conf) {
		didWork = work;
		encs = enc;
		confidence = conf;
	}
};

public class ChineseEncoder {
	
	private HashMap<Integer, String> toks = new HashMap<Integer, String>();
	private ArrayList<String> valids = new ArrayList<String>(), xqlusiv = new ArrayList<String>(), unq = new ArrayList<String>();
	private ArrayList<ArrayList<String>> fullDB = new ArrayList<ArrayList<String>>();
	private ArrayList<ArrayList<String[]>> enc = new ArrayList<ArrayList<String[]>>();
	private String[] sevens = {"chiuann", "shiuann"};
	
	public ChineseEncoder() {}
	
	public CNEnc encode(String[] input) {
		ArrayList<String[]> arr = new ArrayList<String[]>();
		arr.add(input);
		CNEnc fail = new CNEnc(false, arr, null);
		
		// fail if more than 3 tokens provided
		if (input.length > 3)
			return fail;
		
		// populate toks
		for (int i = 0; i < input.length; i++)
			toks.put(i, input[i].toLowerCase());
		
		// handle seven character token cases
		for (int i = 0; i < toks.size(); i++) {
			for (String seven : sevens) {
				if (toks.get(i).contains(seven) && !seven.equals(toks.get(i))) {
					if (toks.size() == 3) // split required, but 3 tokens already exist
						return fail;
					else { // handle and separate seven character transliterations
						String extract = toks.get(i);
						extract = extract.replace(seven, "-");
						if (toks.get(i).indexOf(seven) == 0) {
							toks.put(i, seven);
							for (int j = toks.size() - 1; j > i; j--) {
								toks.put(j + 1, toks.get(j));
								System.out.println((j + 1) + " " + toks.get(j));
							}
							toks.put(i + 1, extract.replace("-", ""));
						} else {
							String[] exsplit = extract.split("-");
							toks.put(i, exsplit[0]);
							int plus = 1;
							if (exsplit.length == 2)
								plus = 2;
							for (int j = toks.size() - 1; j > i; j--) {
								toks.put(j + plus, toks.get(j));
							}
							toks.put(i + 1, seven);
							if (plus == 2)
								toks.put(i + 2, exsplit[1]);
						}
						
						if (toks.size() > 3)
							return fail;
					}
				}
			}
		}
		
		ArrayList<ArrayList<String>> splits = new ArrayList<ArrayList<String>>();
		// if there are < 3 tokens we can attempt to split
		if (toks.size() < 3) {
			String sev = null;
			int sevIndex = -1;
			// return if all tokens are sevens
			boolean all_sevens = true;
			for (int i = 0; i < toks.size(); i++) {
				boolean foundSev = false;
				for (String seven : sevens) {
					if (toks.get(i).equals(seven)) {
						sev = seven;
						sevIndex = i;
						foundSev = true;
					}
				}
				if (!foundSev)
					all_sevens = false;
			}
			if (all_sevens)
				return return_success();
		
			if (sevIndex != -1) { // remove seven from toks for now
				HashMap<Integer, String> temp = new HashMap<Integer, String>();
				if (sevIndex == 0)
					temp.put(0, toks.get(1));
				else
					temp.put(0, toks.get(0));
				toks = temp;	
				splits = split(2);
			} else
				splits = split(3);
			
			// add seven back in
			if (sevIndex != -1) {
				if (sevIndex == 0) {
					toks.put(1, toks.get(0));
					toks.put(0, sev);
					for (ArrayList<String> split : splits)
						split.add(0, sev);
				} else {
					toks.put(1, sev);
					for (ArrayList<String> split : splits)
						split.add(sev);
				}
			}	
		}
		
		if (splits.isEmpty()) {
			// if no splits occur and toks are not all valid chars, fail
			for (int i = 0; i < toks.size(); i++) {
				if (!valid_char(toks.get(i)))
					return fail;
			}
			
			// add unsplit toks to splits array for encoding process
			ArrayList<String> temp = new ArrayList<String>();
			for (int i = 0; i < toks.size(); i++) {
				temp.add(toks.get(i));
			}
			splits.add(temp);
		}
		
		/*// print all generated splits
		for (ArrayList<String> a1 : splits) {
			for (String st : a1)
				System.out.print(st + "|");
			System.out.print("\n");
		}*/
		
		// time to encode!
		for (ArrayList<String> split : splits) {
			// to be used for ranking, currently unimplemented
			boolean xquq = true;
			for (String s : split) {
				if (!isExclusive(s) && !isUnique(s))
					xquq = false;
			}
			
			String[] st = new String[split.size()];
			for (int i = 0; i < split.size(); i++)
				st[i] = split.get(i);
			enc.add(find_py(st));
			
			/*// print all possible encodings
			for (ArrayList<String[]> a : enc) {
				for (String[] o : a) {
					for (String s : o) 
						System.out.print(s + " ");
				System.out.println();
				}
			}*/
		}
		
		// return
		return return_success();
	}
	
	private ArrayList<ArrayList<String>> split(int max) {
		ArrayList<ArrayList<ArrayList<String>>> splits = new ArrayList<ArrayList<ArrayList<String>>>();
		ArrayList<ArrayList<String>> out = new ArrayList<ArrayList<String>>();
		
		// fail if logically unsplittable
		if (toks.size() == 1 && toks.get(0).length() > (max * 6))
			return out;
		if (toks.size() == 2 && toks.get(0).length() > 6 && toks.get(1).length() > 6)
			return out;
				
		for (int i = 0; i < toks.size(); i++) {
			splits.add(new ArrayList<ArrayList<String>>()); // initialize list of splits for each token
			if (toks.get(i).length() <= 2) { // if valid token 2 chars or less add to list
				if (valid_char(toks.get(i))) {
					ArrayList<String> temp = new ArrayList<String>();
					temp.add(toks.get(i));
					splits.get(i).add(temp);
				} else
					return out;
			} else { // split token
				if (valid_char(toks.get(i))) { // full token valid
					ArrayList<String> temp = new ArrayList<String>();
					temp.add(toks.get(i));
					splits.get(i).add(temp);
				}
				// get all possible splits
				ArrayList<ArrayList<String>> ss = split_str(toks.get(i), toks.size());
				if (!ss.isEmpty()) {
					for (ArrayList<String> s : ss) {
						splits.get(i).add(s);
					}
				}
			}
		}
		
		// only return splits within the maximum number of allowed tokens
		if (splits.size() == 2) {
			for (ArrayList<String> a1 : splits.get(0)) {
				for (ArrayList<String> a2 : splits.get(1)) {
					if (a1.size() + a2.size() <= max) {
						@SuppressWarnings("unchecked")
						ArrayList<String> temp = (ArrayList<String>) a1.clone();
						temp.addAll(a2);
						out.add(temp);
					}
				}
			}
		} else {
			for (ArrayList<String> a : splits.get(0)) {
				if (a.size() <= max)
					out.add(a);
			}
		}
		return out;
	}
	
	private ArrayList<ArrayList<String>> split_str(String tok, int tokc) {
		ArrayList<ArrayList<String>> out = new ArrayList<ArrayList<String>>();
		int max = 2; // default: 2 tokens exist, can split to two chars
		if (tokc == 1) // 1 token exists, can split to three chars
			max = 3;
		
		// find all 2 char splits
		for (int i = 1; i < tok.length() - 1; i++) {
			if (valid_char(tok.substring(0, i)) && valid_char(tok.substring(i, tok.length()))) {
				ArrayList<String> temp = new ArrayList<String>();
				temp.add(tok.substring(0, i));
				temp.add(tok.substring(i, tok.length()));
				out.add(temp);
			}
		}
		
		if (max == 3) { // find all 3 char splits
			for (int i = 1; i < tok.length() - 1; i++) {
				if (valid_char(tok.substring(0, i))) {
					for (int j = tok.length() - 1; j > i; j--) {
						if (valid_char(tok.substring(i, j))) {
							ArrayList<String> temp = new ArrayList<String>();
							temp.add(tok.substring(0, i));
							temp.add(tok.substring(i, j));
							temp.add(tok.substring(j, tok.length()));
							out.add(temp);
						}
					}
				}				
			}
		}	
		
		return out;
	}
	
	private ArrayList<String[]> find_py(String[] in) {
		ArrayList<String[]> out = new ArrayList<String[]>();
		
		if (fullDB.isEmpty()) { // load database
			try {
				BufferedReader reader;
				reader = new BufferedReader(new FileReader("china.csv"));
				String line;
				reader.readLine();
				while ((line = reader.readLine()) != null) {
					ArrayList<String> arr = new ArrayList<String>();
					String[] toks = line.split(",");
					for (String t : toks) {
						if (!t.isEmpty())
							arr.add(t);
					}
					fullDB.add(arr);
				}
				reader.close();
			} catch (Exception e) {
				System.out.println("Error reading required database.");
				System.exit(0);
			}
		}
		
		// populate list of possible pinyin transliterations for each token
		ArrayList<ArrayList<String>> py_opts = new ArrayList<ArrayList<String>>();
		for (int i = 0; i < in.length; i++) {
			py_opts.add(new ArrayList<String>());
			for (ArrayList<String> line : fullDB) {
				if (line.contains(in[i]))
					py_opts.get(i).add(line.get(0));
			}
		}
		
		// create list of all possible combinations of pinyin transliterations identified above
		if (py_opts.size() == 1) { // one token
			String[] out_arr = new String[py_opts.get(0).size()];
			for (int i = 0; i < py_opts.get(0).size(); i++)
				out_arr[i] = py_opts.get(0).get(i);
			out.add(out_arr);
		} else { // 2-3 tokens
			for (int i = 0; i < py_opts.get(0).size(); i++) {
				for (int j = 0; j < py_opts.get(1).size(); j++) {
					if (py_opts.size() == 3) { // three tokens
						for (int k = 0; k < py_opts.get(2).size(); k++)
							out.add(new String[] { py_opts.get(0).get(i), py_opts.get(1).get(j), py_opts.get(2).get(k) });
					} else // two tokens
						out.add(new String[] { py_opts.get(0).get(i), py_opts.get(1).get(j) });
				}
			}
		}
		return out;
	}
	
	private boolean valid_char(String s) {
		if (valids.isEmpty()) { // load valid character database
			try {
				BufferedReader reader;
				reader = new BufferedReader(new FileReader("nodup.csv"));
				String line;
				while ((line = reader.readLine()) != null)
					valids.add(line.trim().toLowerCase());
				reader.close();
			} catch (Exception e) {
				System.out.println("Error reading required database.");
				System.exit(0);
			}
		}
				
		// check for character in database
		if (valids.contains(s))
			return true;
		return false;
	}
	
	private boolean isExclusive(String s) {
		if (xqlusiv.isEmpty()) { // load exclusive database
			try {
				BufferedReader reader;
				reader = new BufferedReader(new FileReader("exclusive.csv"));
				String line;
				reader.readLine();
				while ((line = reader.readLine()) != null) {
					String[] chars = line.trim().toLowerCase().split(",");
					for (String c : chars) {
						if (!c.isEmpty())
							xqlusiv.add(c);
					}
				}
				reader.close();
			} catch (Exception e) {
				System.out.println("Error reading required database.");
				System.exit(0);
			}
		}
				
		// check for character in database
		if (xqlusiv.contains(s))
			return true;
		return false;
	}
	
	private boolean isUnique(String s) {
		if (unq.isEmpty()) { // load exclusive database
			try {
				BufferedReader reader;
				reader = new BufferedReader(new FileReader("unique.csv"));
				String line;
				reader.readLine();
				while ((line = reader.readLine()) != null) {
					String[] chars = line.trim().toLowerCase().split(",");
					for (String c : chars) {
						if (!c.isEmpty() && !unq.contains(c))
							unq.add(c);
					}
				}
				reader.close();
			} catch (Exception e) {
				System.out.println("Error reading required database.");
				System.exit(0);
			}
		}
				
		// check for character in database
		if (unq.contains(s))
			return true;
		return false;
	}
	
	private CNEnc return_success() {
		ArrayList<String[]> out = new ArrayList<String[]>();
		for (ArrayList<String[]> a : enc)
			for (String[] s : a)
				out.add(s);
		return new CNEnc(true, out, null);
	}
	
	public void reset() {
		toks = new HashMap<Integer, String>();
		enc = new ArrayList<ArrayList<String[]>>();
	}
	
}
