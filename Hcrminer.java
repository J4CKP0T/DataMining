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
public class Hcrminer{

  /* Global frequency of each itemID { item : frequency, ... } */
	public static HashMap<String, Integer> singleElements = new HashMap();
	public static Set<String> nulEP = new HashSet<String>();
	//public static ArrayList<String> ulist = new ArrayList(); // Same as nulEP, but in List representation
	public static HashMap<String, Integer> freqItemsets =  new HashMap();



	public static void main(String[] args) throws IOException {
		float minsup = Float.parseFloat(args[0]);
		float minconf = Float.parseFloat(args[1]);
		String infile = args[2];
		String outfile = args[3];
    int option = Integer.parseInt(args[4]);


		System.out.println("minsup: " + minsup + " minconf: " + minconf + " infile: " + infile + " outfile: " + outfile);

  	// Create initial projected database
		long start = 0;
		long end = 0;
		long duration = 0;

		start = System.nanoTime();
		HashMap<String, ArrayList<String>> itemSet = createSet(infile, minsup) ;
		end = System.nanoTime();
		System.out.println("createSet took: " + (end-start) + " nanoseconds");

		System.out.println("Size of nulEP: " + nulEP.size());

		for (String key : itemSet.keySet()) {
			Collections.sort(itemSet.get(key));
		}

		// Main function

		start = System.nanoTime();
		System.out.println("TP: " + TP("", itemSet, minsup,0));
		end = System.nanoTime();
		System.out.println("TP took: " + (end-start) + " nanoseconds");
    //Print frequent itemsets
		System.out.println("freqItemsets: " + freqItemsets);
    System.out.println("singleElements: " + singleElements);
    System.out.println("nulEP: " + nulEP);

    //Rule generation
    ruleGen(freqItemsets);

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

			// Create singleElements
			if (singleElements.containsKey(itemId)) {
				int c = singleElements.get(itemId);
				singleElements.put(itemId, ++c);

				// Only add itemIds to our set if its support is greater than the minsup
				if (singleElements.get(itemId) >= minsup)
					nulEP.add(itemId);
			}
			else
				singleElements.put(itemId, 1);
			}
		return itemSet;
	}

    //Function that recurses
		public static String TP(String P, HashMap<String, ArrayList<String>> DB, float minsup, int count) {

			ArrayList<String> ep = new ArrayList();
			HashMap<String, Integer> local_hist = new HashMap();


			// Step 1 -----------------------------------------------------------------------------
			// Determine the frequent items in DB(P) and denote them by E(P)
			for (String itemId : nulEP) {
				boolean added =false;
				for (String key : DB.keySet()) {

					// Check if itemId is in the transaction pattern
					// If so, increase the support in the local singleElements
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
				freqItemsets.put(P,count);
				return P;
			}

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

				// Recursive step
				}
				if (! PDBx.isEmpty())
				{
					TP(P+","+x, PDBx, minsup, local_hist.get(x));
				}
				else
				{
					freqItemsets.put(P,count);
				}


			}
			return P;


		}

    public static void ruleGen(HashMap<String, Integer> freqItemsets){
        //Remove frequent itemsets of length 1
        Iterator<Map.Entry<String, Integer>> iter = freqItemsets.entrySet().iterator();
  			while(iter.hasNext())
  			{
  				Map.Entry<String, Integer> entry = iter.next();
          String temp = entry.getKey();
          String[] lineArr= temp.split(",");
          if(lineArr.length < 3){
            iter.remove();
          }
        }
        Iterator<Map.Entry<String, Integer>> iter2 = freqItemsets.entrySet().iterator();
        //Generate rules for remaining frequent itemsets of length >= 2
        while(iter2.hasNext())
        {
          Map.Entry<String, Integer> entry = iter2.next();
          Set<String> h1= new HashSet<String>();
          String temp = entry.getKey();
          String[] lineArr = temp.split(",");
          int i;
          for(i=0; i<lineArr.length;i++){
            if(!lineArr[i].isEmpty()){
              h1.add(lineArr[i]);
            }
          }
          //System.out.println(h1)
          //System.out.println(h1.size());
          apGenRules(entry.getKey(), h1);
        }
    }

    public static void apGenRules(String freqItemset, Set<String> h){
        //Code that emulates algorithm 6.3 in textbook
    }
}
