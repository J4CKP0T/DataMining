/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;

/**
 *
 * @author darma010
 */
public class Hcrminer{

    public static class Node implements Comparable<Node> {
      public String item;
      public int frequency;

      public Node(String _item, int _freq) {
      this.item = _item;
      this.frequency = _freq;
      }

      //Custom Comparator for EPlist sorting
      @Override
      public int compareTo(Node node){
          return this.frequency > node.frequency ? 1 : (this.frequency < node.frequency ? -1 : 0);
      }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        float minsup = Float.parseFloat(args[0]);
        String minconf = args[1];
        String inputfile = args[2];
        String outputfile = args[3];
        String optionNum = args[4];

        //Create initial projected database
        HashMap<String, ArrayList<String>> initDB = createInitialDB(inputfile);

            //Print check for initDB
            for(String key: initDB.keySet()){
                String transID = key.toString();
                ArrayList<String> transList = initDB.get(key);
                System.out.println(transID + " " + transList);
            }

        //Create EPlist for initDB
        ArrayList<Node> initEPlist = createEPlist(initDB);

            //Print check for EPlist
            for(Node node: initEPlist){
                System.out.println("Item: " + node.item + " Frequency: " + node.frequency);
            }

        //Prune EPlist
        supPruneEPlist(minsup, initEPlist);

        /*
        //Print check for EPlist after pruning
        for(Node node: initEPlist){
            System.out.println("Item: " + node.item + " Frequency: " + node.frequency);
        }
        //sort and check sorting of EPlist
        initEPlist = sortDecreasingFreq(initEPlist);
        for(Node node: initEPlist){
            System.out.println("Item: " + node.item + " Frequency: " + node.frequency);
        }
        */
    }

    //Method that creates initial DB from input file
    public static HashMap createInitialDB(String fileName){
        HashMap<String, ArrayList<String>> initDB = new HashMap();

        String line = null;

        //Read input file line by line and put entries into initDB
        try{
            BufferedReader br = new BufferedReader(new FileReader(fileName));

            while((line= br.readLine()) != null){

                //System.out.println(line);

                //File is in the form: transactionID itemID
                String[] lineArr = line.split(" ");
                String transID = lineArr[0];
                String itemID = lineArr[1];

                //System.out.println(transID);
                //System.out.println(itemID);

                if(initDB.containsKey(transID)){
                    initDB.get(transID).add(itemID);
                }
                else{
                    ArrayList<String> transactionList = new ArrayList();
                    transactionList.add(itemID);
                    initDB.put(transID, transactionList);
                }
            }
        }

        //Check for file not found/mistyped file
        catch(FileNotFoundException ex){
            System.out.println("Unable to open: " + fileName);
        }

        //Check for Read/Write Errors
        catch(IOException ex){
            System.out.println("Error reading file: " + fileName);
            ex.printStackTrace();
        }

        return initDB;
    }


/////// EPlist functions ////////////////////////////////////////////////////////////////////

    //EP list maker function
    public static ArrayList<Node> createEPlist(HashMap<String, ArrayList<String>> dB){
        ArrayList<Node> newEPlist = new ArrayList<Node>();
        int i;
        for(String transID: dB.keySet()){
            for(i = 0; i<dB.get(transID).size(); i++){
                String transItem = dB.get(transID).get(i);
                int j;
                //if EPlist is empty
                if(newEPlist.size()==0){
                    Hcrminer.Node newItem = new Hcrminer.Node(transItem, 1);
                    newEPlist.add(newItem);
                    continue;
                }
                else{
                    if(EPlistContains(transItem, newEPlist)){
                        incrementEPNodeFreq(transItem, newEPlist);
                        continue;
                    }
                    else{
                        Hcrminer.Node newItem = new Hcrminer.Node(transItem, 1);
                        newEPlist.add(newItem);
                        continue;
                    }
                }
            }
        }
        return newEPlist;
    }

    //EPlist Contains function
    public static boolean EPlistContains(String transItem, ArrayList<Node> EPlist){
        int i;
        for(i=0; i<EPlist.size(); i++){
            if(EPlist.get(i).item.equals(transItem)){
                return true;
            }
        }
        return false;
    }

    //EPlist increment frequency function
    public static void incrementEPNodeFreq(String transItem, ArrayList<Node> EPlist){
        int i;
        for(i=0; i<EPlist.size(); i++){
            if(EPlist.get(i).item.equals(transItem)){
                EPlist.get(i).frequency++;
                return;
            }
        }
    }

    //EPlist pruning function for minsup
    public static void supPruneEPlist(float minsup, ArrayList<Node> EPlist){
        int i;
        for(i=0; i<EPlist.size(); i++){
            if(EPlist.get(i).frequency < (int)minsup){
                EPlist.remove(i);
            }
        }
    }

    //Function to sort EPlist in increasing frequency
    public static ArrayList<Node> sortIncreasingFreq(ArrayList<Node> EPlist){
      Collections.sort(EPlist);
      return EPlist;
    }

    //Function to sort EPlist in decreasing frequency
    public static ArrayList<Node> sortDecreasingFreq(ArrayList<Node> EPlist){
      ArrayList<Node> sortedEPlist = new ArrayList<Node>();
      Collections.sort(EPlist);
      int i;
      for(i = EPlist.size()-1; i>=0; i--){
        sortedEPlist.add(EPlist.get(i));
      }
      return sortedEPlist;
    }
///////////////////////////////////////////////////////////////////////////////////////////

}
