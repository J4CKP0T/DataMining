/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
 import java.io.*;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Set;
 import java.util.Map;
 import java.util.Iterator;

 import java.util.Collections;

/**
 *
 * @author darma010
 */
public class Hcrminer2{

  /* Global frequency of each itemID { item : frequency, ... } */
	public static HashMap<String, Integer> histogram = new HashMap();
	public static Set<String> uset = new HashSet<String>();
	public static ArrayList<String> ulist = new ArrayList(); // Same as uset, but in List representation
	public static ArrayList<String> fq =  new ArrayList();



	public static void main(String[] args) {
		float minsup = Float.parseFloat(args[0]);
		String minconf = args[1];
		String infile = args[2];
		String outfile = args[3];

		System.out.println("minsup: " + minsup + " minconf: " + minconf + " infile: " + infile + " outfile: " + outfile);


		// Create initial projected database
		HashMap<String, ArrayList<String>> itemSet = createSet("tiny") ;
		for (String key : itemSet.keySet()) {
			Collections.sort(itemSet.get(key));
		}

		// Convert universal set to list
		ulist.addAll(uset);
		Collections.sort(ulist);


		System.out.println("ulist: " + ulist);
		// Main function
		System.out.println("TP: " + TP("", itemSet, minsup));
		System.out.println("fq: " + fq);


	}




	/*
		Creates hashtable such that the set of items that make up the transaction will be derived by combining
		the item IDs of all the lines that correspond to the same transaction ID
			itemSet = { transactionID : {itemID1, itemID2, ...} , ...}\
	*/
	public static HashMap createSet(String fileName) {

		HashMap<String, ArrayList<String>> itemSet = new HashMap();


		String line = null;

		// Print out each line and add IDs to itemSet
		try {
			BufferedReader br = new BufferedReader(new FileReader(fileName));

			while ((line = br.readLine()) != null) {
				/*
					File is in the form:
						transactionID itemID
				*/

				String[] lineArr = line.split(" ");
				String transId = lineArr[0];
				String itemId = lineArr[1];

				//System.out.println(line);


				// Create intial DB
				if (itemSet.containsKey(transId)) {
					itemSet.get(transId).add(itemId);
				}
				else {
					ArrayList<String> l = new ArrayList();
					l.add(itemId);
					itemSet.put(transId, l);

				}

				// Create histogram
				if (histogram.containsKey(itemId)) {
					int c = histogram.get(itemId);
					histogram.put(itemId, ++c);
				}
				else
					histogram.put(itemId, 1);



				// Create universal set
				uset.add(itemId);



			}

			//System.out.println(uset);
		}


		// Mistyped file name
		catch(FileNotFoundException ex) {
			System.out.println("Unable to open " + fileName);
		}

		// Read/Write errors
		catch(IOException ex) {
			System.out.println("Error reading file: " + fileName);
			ex.printStackTrace();
		}




		return itemSet;

	}


		/*
			Create set of all items--should be first level in tree
		*/
		public static String TP(String P, HashMap<String, ArrayList<String>> DB, float minsup) {

			ArrayList<String> ep = new ArrayList();
			HashMap<String, Integer> local_hist = new HashMap();


			System.out.println("-------------------------------------------------");
			System.out.println("P: " + P);


			// Step 1 -----------------------------------------------------------------------------
			// Determine the frequent items in DB(P) and denote them by E(P)
			for (String itemId : ulist) {
				for (String key : DB.keySet()) {

					// Check if itemId is in the transaction pattern
					// If so, increase the support in the local histogram
					if (DB.get(key).contains(itemId)) {
						if (local_hist.containsKey(itemId)) {
							int c = local_hist.get(itemId);
							local_hist.put(itemId, ++c);
						}
						else
							local_hist.put(itemId, 1);

						// Prune the search space if the support of itemId is at least the minsup
						if (local_hist.get(itemId) >= minsup) {
							ep.add(itemId);
							break; // skip the rest because we've reached minsup for
						}
					}
				}
			}


			if (ep.isEmpty())
			{
				//System.out.println("DB WHEN EP IS E: " +DB);
				fq.add(P);
				System.out.println("Inside base case: P = " + P + ",  fq (after adding p): " + fq);
				System.out.println("Inside base case E(P): " +ep + "DB(P): " + DB);
				return P;
			}

			System.out.println("E(P): " + ep);

			// Step 2 -----------------------------------------------------------------------------
			// Eliminate from DB(P) any items not in E(P)
			Set<String> ep_set = new HashSet();
			ep_set.addAll(ep);

			Iterator<Map.Entry<String, ArrayList<String>>> iter = DB.entrySet().iterator();
			while(iter.hasNext())
			{
				Map.Entry<String, ArrayList<String>> entry = iter.next();

					Set<String> temp = new HashSet<String>();
					temp.addAll(entry.getValue());
					temp.retainAll(ep_set);

					// We check if the intersection between a transaction and EP is not empty
					// If it is, then no item in EP is found in the transaction, so remove transaction
					if (temp.isEmpty()) { // ep not in DB(P)
						iter.remove();
				}


			}

				System.out.println("DB: " + DB);




			// Step 3 -------------------------------------------------------------------------------
			// For each item x in EP(x), call TP(Px, DB(Px))
			for (String x : ep)
			{
				// Projected DB of x
				HashMap<String, ArrayList<String>> PDBx = new HashMap();


				ArrayList<String> seen = new ArrayList();

				for (String key : DB.keySet())
				{
					if (DB.get(key).contains(x)) {
						ArrayList<String> temp = new ArrayList(DB.get(key));


						seen.add(x);
						System.out.println("Seen :" +seen);
						//for (String s : seen)
							//temp.remove(s);

						for ( int i =0; i< temp.size(); i++)
							{
								System.out.println(temp.size());
								//System.out.println(i);
							if (temp.get(i).compareTo(x) <=0)
								temp.set(i,"");
						}

						//temp.remove(x);


						// After removing x, only include the new transaction items (in the projected database of x)
						// if the set of items is not empty
						if (!temp.isEmpty()) {
							ArrayList<String> remove = new ArrayList();
							remove.add("");
							temp.removeAll(remove);
							PDBx.put(key, temp);
						}
					}
					//System.out.println("PDB(x): " + PDBx);
					// Recursive step

				}

				boolean pr = false;
				for (String s : fq)
				{
					if (s.contains(P) && P.compareTo("") != 0)
						return P;

				}
				if (! PDBx.isEmpty())
				{
					System.out.println("value size: " + PDBx.get("3").size());
					empty_dict(PDBx);
				System.out.println("DBPx: " + PDBx);
				System.out.println(P + " calling " + P+x);
				TP(P+x, PDBx, minsup);
				}
				else
				{
					System.out.println("Adding to fq in else: " +P);
					fq.add(P);
				}


			}

			//System.out.println("E(P): " + ep);
			//System.out.println("DB(P): " + DB);
			System.out.println("Right before final return--P = " + P + ",   fq = " +fq);
			return P;


		}
		public static boolean empty_dict(HashMap<String, ArrayList<String>> dict)
		{

			System.out.println("Before Loop");
			for(String key : dict.keySet())
			{
				if(dict.get(key).size()!=0)
				{
					System.out.println("get(key) in empty_dict " +dict.get(key));
					return false;
				}
			}
			return true;
		}
}
