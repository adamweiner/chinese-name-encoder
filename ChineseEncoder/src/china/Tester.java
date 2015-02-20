package china;

import java.io.BufferedReader;
import java.io.FileReader;

public class Tester {
	
	public static void main (String[] args) {
		try {
			BufferedReader reader;
			reader = new BufferedReader(new FileReader("TestNames.csv"));
			String line;
			ChineseEncoder ce = new ChineseEncoder();
			while ((line = reader.readLine()) != null) {
				CNEnc out = ce.encode(line.split(" "));
				// print all possible transliterations for each name
				for (String[] arr : out.encs) {
					for (String s : arr)
						System.out.print(s + ",");
					System.out.println();
				}
				ce.reset();
			}
			reader.close();
		} catch (Exception e) {
			System.out.println(e.getLocalizedMessage());
			System.exit(0);
		}
	}
}
