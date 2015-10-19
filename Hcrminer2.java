/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
 import java.io.*;
 import java.nio.file.Files;
 import java.nio.file.Paths;
 import java.nio.charset.StandardCharsets;
 import java.util.Arrays;
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
	//public static ArrayList<String> ulist = new ArrayList(); // Same as uset, but in List representation
	public static HashMap<String, Integer> fq =  new HashMap();



	public static void main(String[] args) throws IOException {
		float minsup = Float.parseFloat(args[0]);
		String minconf = args[1];
		String infile = args[2];
		String outfile = args[3];

		System.out.println("minsup: " + minsup + " minconf: " + minconf + " infile: " + infile + " outfile: " + outfile);

  	// Create initial projected database
		long start = 0;
		long end = 0;
		long duration = 0;

		start = System.nanoTime();
		HashMap<String, ArrayList<String>> itemSet = createSet(infile, minsup) ;
		end = System.nanoTime();
		System.out.println("createSet took: " + (end-start) + " nanoseconds");

		int count = 0;

		for (String key : itemSet.keySet())
		{
			if(itemSet.get(key).contains("1") && itemSet.get(key).contains("274") && itemSet.get(key).contains("326") )
				count ++;
		}
		System.out.println(count);
		System.out.println("Size of uset: " + uset.size());
		//System.out.println("Histo: " + histogram.get("1"));


		for (String key : itemSet.keySet()) {
			Collections.sort(itemSet.get(key));
		}

		// Convert universal set to list
		//ulist.addAll(uset);
		//Collections.sort(ulist);


		////System.out.println("ulist: " + ulist);
		// Main function

		start = System.nanoTime();
		System.out.println("TP: " + TP("", itemSet, minsup,0));
		end = System.nanoTime();
		System.out.println("TP took: " + (end-start) + " nanoseconds");



		System.out.println("fq: " + fq);

		start = System.nanoTime();
		//generateAssoc(itemSet);
		end = System.nanoTime();
		System.out.println("Partitioning took: " + (end-start) + " nanoseconds");


	}







	/*
		Creates hashtable such that the set of items that make up the transaction will be derived by combining
		the item IDs of all the lines that correspond to the same transaction ID
			itemSet = { transactionID : {itemID1, itemID2, ...} , ...}\
	*/
	public static HashMap createSet(String fileName, float minsup) throws IOException {


		HashMap<String, ArrayList<String>> itemSet = new HashMap();
		for (String line : Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8)) {
			String[] lineArr = line.split(" ");
			String transId = lineArr[0];
			String itemId = lineArr[1];


			// Create initial DB
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

				// Only add itemIds to our set if its support is greater than the minsup
				if (histogram.get(itemId) >= minsup)
					uset.add(itemId);


			}
			else
				histogram.put(itemId, 1);

			}


		return itemSet;

	}


		/*
			Create set of all items--should be first level in tree
		*/
		public static String TP(String P, HashMap<String, ArrayList<String>> DB, float minsup, int count) {

			ArrayList<String> ep = new ArrayList();
			HashMap<String, Integer> local_hist = new HashMap();


			//System.out.println("-------------------------------------------------");
			//System.out.println("P: " + P);


			// Step 1 -----------------------------------------------------------------------------
			// Determine the frequent items in DB(P) and denote them by E(P)
			for (String itemId : uset) {
				boolean added =false;
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
						if (local_hist.get(itemId) >= minsup && ! added) {
							ep.add(itemId);
							added= true;
							//break; // skip the rest because we've reached minsup for
						}

					}
				}
			}


			if (ep.isEmpty())
			{
				////System.out.println("DB WHEN EP IS E: " +DB);
				//System.out.println("P :" + P + " Count:" +count);
				fq.put(P,count);
				//System.out.println("Inside base case: P = " + P + ",  fq (after adding p): " + fq);
				//System.out.println("Inside base case E(P): " +ep + "DB(P): " + DB);
				return P;
			}

			//System.out.println("E(P): " + ep);

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

				//System.out.println("DB: " + DB);




			// Step 3 -------------------------------------------------------------------------------
			// For each item x in EP(x), call TP(Px, DB(Px))
			for (String x : ep)
			{
				// Projected DB of x
				HashMap<String, ArrayList<String>> PDBx = new HashMap();

				for (String key : DB.keySet())
				{
					if (DB.get(key).contains(x)) {

						ArrayList<String> temp = new ArrayList(DB.get(key));

						for ( int i =0; i< temp.size(); i++)
							{
							if (temp.get(i).compareTo(x) <=0)
								temp.set(i,"");
						}

						// After removing x, only include the new
						// transaction items (in the projected database of x)
						// if the set of items is not empty
						if (!temp.isEmpty()) {
							ArrayList<String> remove = new ArrayList();
							remove.add("");
							temp.removeAll(remove);
							PDBx.put(key, temp);
						}
					}
					////System.out.println("PDB(x): " + PDBx);
					// Recursive step

				}
				if (! PDBx.isEmpty())
				{
					//System.out.println("value size: " + PDBx.get("3").size());
					//System.out.println("DBPx: " + PDBx);
					//System.out.println(P + " calling " + P+x);
					TP(P+","+x, PDBx, minsup, local_hist.get(x));
				}
				else
				{
					fq.put(P,count);
				}


			}

			////System.out.println("E(P): " + ep);
			////System.out.println("DB(P): " + DB);
			//System.out.println("Right before final return--P = " + P + ",   fq = " +fq);
			return P;


		}


		public static void generateAssoc(HashMap<String, ArrayList<String>> itemSet) {
			// Partition the frequent item set I into X a
			// Create an array of all frequent patterns
			ArrayList<String> patterns = new ArrayList();
			for (String pattern : fq.keySet())
				patterns.add(pattern);



			int length = patterns.size();

			String[] x;
			String[] y;


			// Contains unions of all possible partitions
			HashMap<Set,Integer> unions = new HashMap();

			for (int i = 0; i < length; i++) {
				for (int j = 0; j < length; j++) {
					if (i != j) {

						Set<String> temp = new HashSet();
						x = patterns.get(i).substring(1).split(",");
						y = patterns.get(j).substring(1).split(",");



						for (String e : x)
							temp.add(e);
						for (String e : y)
							temp.add(e);


						unions.put(temp,0);
						System.out.println(Arrays.toString(x) + "      " + Arrays.toString(y));

					}
				}
			}



			ArrayList<String> items = new ArrayList();
			for (String key : itemSet.keySet()) {
				Set<String> temp = new HashSet();
				items = itemSet.get(key);
				//System.out.println("Key" + key);
				for (Set u : unions.keySet())
				{
					temp.addAll(items);
					//System.out.println("Init: " + temp);
					//System.out.println("Other" + u);
					temp.retainAll(u);
					//System.out.println("union " + temp);
					//System.out.println("---------------------");

					if(temp.equals(u))
					{
						int c = unions.get(u);
							unions.put(u, ++c);
					}
				}
			}

			System.out.println(unions);






			return;
		}






	public static class ItemFreqPair implements Comparable<ItemFreqPair>{


		String itemId;
		int freq;

		public ItemFreqPair(String itemId, int freq) {
			this.itemId = itemId;
			this.freq = freq;
		}

		public String toString() {
			return itemId + " = " + freq;
		}

		@Override
		public int compareTo(ItemFreqPair p) {
			return p.freq - this.freq;
		}


	}
}
