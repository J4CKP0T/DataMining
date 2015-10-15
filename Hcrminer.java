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

/**
 *
 * @author darma010
 */
public class Hcrminer {

    public static ArrayList<String> EP = new ArrayList();

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
      /*
        //Print check for initDB
        for(String key: initDB.keySet()){
            String transID = key.toString();
            ArrayList<String> transList = initDB.get(key);
            System.out.println(transID + " " + transList);
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
}
