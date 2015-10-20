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
public class Hcrminer2{

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

    //Globally accessible dbOfDB
    public static HashMap<String, HashMap<String, ArrayList<String>>> dbOfDB;
    //Globally accessible freqItemset List
    public static ArrayList<String> freqItemsets;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        //Initialize dbOfDB
        dbOfDB = new HashMap<String, HashMap<String, ArrayList<String>>>();
        //Initialize list to store Frequent Itemsets
        freqItemsets = new ArrayList<String>();

        float minsup = Float.parseFloat(args[0]);
        String minconf = args[1];
        String inputfile = args[2];
        String outputfile = args[3];
        String optionNum = args[4];

        //Create initial projected database
        HashMap<String, ArrayList<String>> initDB = createInitialDB(inputfile);
        //Create EPlist for initDB
        ArrayList<Node> initEPlist = createEPlist(initDB);
        //Prune EPlist and initDB
        supPrune(minsup, initEPlist, initDB);
        dbOfDB.put("nul", initDB);

        for(Node node : initEPlist){
            TP(node.item, initDB, "nul", minsup);
        }

        System.out.println(freqItemsets);
    }

      //Function to recurse
      public static void TP(String item, HashMap<String, ArrayList<String>> currDB, String dbName, float minsup){
          if(currDB == dbOfDB.get("nul")){
            dbName = "";
          }
          else{
            dbName = dbName + "," + item;
          }
          //Build new DB and make necessary adjustments
          HashMap<String, ArrayList<String>> newDB = new HashMap<String, ArrayList<String>>(currDB);
          int i;
          for(String transID : newDB.keySet()){
              if(newDB.get(transID).contains(item)){
                i=newDB.get(transID).indexOf(item);
                newDB.get(transID).subList(0,i).clear();
              }else{
                  newDB.get(transID).clear();
              }
          }
          //Make new EPlist for new DB
          ArrayList<Node> newEP = createEPlist(newDB);
          //Print check for newEPlist
          for(Node node: newEP){
              System.out.println("Item: " + node.item + " Frequency: " + node.frequency);
          }
          //Prune newDB and newEP
          supPrune(minsup, newEP, newDB);
          //Stash newDB in Global dbOfDB
          dbOfDB.put(dbName, newDB);

          for(Node node : newEP){
              TP(node.item, newDB, dbName, minsup);
          }
          if(dbName != "" && newEP.size()==0){
            freqItemsets.add(dbName);
          }
          return;
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
        /*
        //Print check for initDB
        for(String key: initDB.keySet()){
            String transID = key.toString();
            ArrayList<String> transList = initDB.get(key);
            System.out.println(transID + " " + transList);
        }
        */
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
                //if transaction is empty set continue
                if(transItem == null){
                  continue;
                }
                //if EPlist is empty
                if(newEPlist.size()==0){
                    Hcrminer2.Node newItem = new Hcrminer2.Node(transItem, 1);
                    newEPlist.add(newItem);
                    continue;
                }
                else{
                    if(EPlistContains(transItem, newEPlist)){
                        incrementEPNodeFreq(transItem, newEPlist);
                        continue;
                    }
                    else{
                        Hcrminer2.Node newItem = new Hcrminer2.Node(transItem, 1);
                        newEPlist.add(newItem);
                        continue;
                    }
                }
            }
        }
        /*
        //Print check for newEPlist
        for(Node node: newEPlist){
            System.out.println("Item: " + node.item + " Frequency: " + node.frequency);
        }
        */
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

    //EPlist and dB pruning function for minsup
    public static void supPrune(float minsup, ArrayList<Node> epList, HashMap<String, ArrayList<String>> dB){
      /*
      //Print check before for EPlist
      for(Node node: epList){
          System.out.println("Item: " + node.item + " Frequency: " + node.frequency);
      }
      */
      /*
      //Print check before for dB
      for(String key: dB.keySet()){
          String transID = key.toString();
          ArrayList<String> transList = dB.get(key);
          System.out.println(transID + " " + transList);
      }
      */
        int i;
        int j;
        for(i=0; i<epList.size(); i++){
          if(epList.get(i).frequency < (int)minsup){
              for(String key: dB.keySet()){
                  if(dB.get(key).contains(epList.get(i).item)){
                    j=dB.get(key).indexOf(epList.get(i).item);
                    dB.get(key).remove(j);
                  }
              }
              epList.remove(i);
          }
        }
        /*
        //Print check after for EPlist
        for(Node node: epList){
            System.out.println("Item: " + node.item + " Frequency: " + node.frequency);
        }
        */
        /*
        //Print check before for dB
        for(String key: dB.keySet()){
            String transID = key.toString();
            ArrayList<String> transList = dB.get(key);
            System.out.println(transID + " " + transList);
        }
        */
        return;
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
