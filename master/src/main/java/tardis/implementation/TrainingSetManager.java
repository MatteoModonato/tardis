package tardis.implementation;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import jbse.mem.Clause;

import static tardis.implementation.Util.shorten;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import tardis.Main;

/**
 * Class that takes as input the path conditions processed by EvoSuite.
 * The bloom filter structure is produced using the concrete and abstract
 * path conditions. The bloom filter structure is added to the training 
 * set associated with label 0:infeasible or 1:feasible. 
 * If LogManager.generateCSV is set to true the data (path condition,
 * abstract path condition, concrete path condition, bloom filter structure, 
 * label) are added to csv.
 */

public class TrainingSetManager {
	
	//print the bitSet array as a series of bits
	public static String[] printBits(BitSet[] array) {
		String[] arr = new String[ConvertPCToBloomFilter.N_ROWS];
		for (int i = 0; i < ConvertPCToBloomFilter.N_ROWS; i++) {
			StringBuilder s = new StringBuilder();
			for (int j = 0; j < ConvertPCToBloomFilter.N_COLUMNS; j++) {
					s.append(array[i].get(j) ? "1" : "0");
			}
			arr[i] = s.toString();
		}
		return arr;
	}
	
		
	//save training set information in file.csv
	public static void PrintToCSV(String PathToString, String[] generalArray, String[] generalArraySliced, String[] specificArray, String[] specificArraySliced, Object[] clauseArray, BitSet[] bloomFilterStructure, String label) throws IOException {
		
		File directory = new File(LogManager.PATH);
	    if (!directory.exists()){
	        directory.mkdir();
	    }
			
		File csv = new File(LogManager.PATH+"trainingSet.csv");
		try(FileWriter fw = new FileWriter(csv, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw)) {
			if (csv.length()==0) { 
				out.println("Original path condition;General path condition;General path condition sliced;Specific path condition;Specific path condition sliced;ClauseArray;BloomFilterStructure;Label");
				out.println(PathToString+";"+Arrays.toString(generalArray)+";"+Arrays.toString(generalArraySliced)+";"+Arrays.toString(specificArray)+";"+Arrays.toString(specificArraySliced)+";"+Arrays.toString(clauseArray)+";"+Arrays.toString(printBits(bloomFilterStructure))+";"+label);
			}
			else {
				out.println(PathToString+";"+Arrays.toString(generalArray)+";"+Arrays.toString(generalArraySliced)+";"+Arrays.toString(specificArray)+";"+Arrays.toString(specificArraySliced)+";"+Arrays.toString(clauseArray)+";"+Arrays.toString(printBits(bloomFilterStructure))+";"+label);
			}
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//manage the path conditions from which EvoSuite is able or not able to generate test cases
	public static void PCEvosuiteSuccessFailure(Collection<Clause> PC, boolean evosuiteResult) throws IOException {
		if(PC!=null) {

			Object[] clauseArray = shorten(PC).toArray();
			String PathToString = Util.stringifyPathCondition(shorten(PC));

			//split pc clauses into array
			String[] generalArray = PathToString.split(" && ");
			String[] specificArray = PathToString.split(" && ");
			//generate general clauses
			for (int i=0; i < generalArray.length; i++){
				generalArray[i]=generalArray[i].replaceAll("[0-9]", "");
			}

			//Slicing call
			Object[] outputSliced = SlicingManager.Slicing(specificArray, clauseArray, generalArray);
			String[] specificArraySliced = (String[]) outputSliced[0];
			String[] generalArraySliced = (String[]) outputSliced[1];

			BitSet[] bloomFilterStructure = ConvertPCToBloomFilter.bloomFilter(specificArraySliced, generalArraySliced);
			
			//add to trainingSet
			if (evosuiteResult == true) {
				Main.trainingSet.add(new StructureLaberPair(bloomFilterStructure, 1));

				if (LogManager.generateCSV)
					PrintToCSV(PathToString, generalArray, generalArraySliced, specificArray, specificArraySliced, clauseArray, bloomFilterStructure, "1");
				
				System.out.println("[ML MODEL] For path condition: " + PathToString);
			    System.out.println("[ML MODEL] PCEvosuiteSuccess: Data add to Training Set");
			}
			else {
				Main.trainingSet.add(new StructureLaberPair(bloomFilterStructure, 0));

				if (LogManager.generateCSV)
					PrintToCSV(PathToString, generalArray, generalArraySliced, specificArray, specificArraySliced, clauseArray, bloomFilterStructure, "0");
				
				System.out.println("[ML MODEL] For path condition: " + PathToString);
			    System.out.println("[ML MODEL] PCEvosuiteFailure: Data add to Training Set");
			}
		}
	}
  
	//manage the path conditions of EvoSuite seed tests: remove the last clause and rerun the workflow
	public static void PCEvosuiteSuccessSeedTest(Collection<Clause> PC) throws IOException {
		if(PC!=null) {

			Object[] clauseArray = shorten(PC).toArray();
			String PathToString = Util.stringifyPathCondition(shorten(PC));

			//split pc clauses into array
			String[] generalArray = PathToString.split(" && ");
			String[] specificArray = PathToString.split(" && ");
			//generate general clauses
			for (int i=0; i < generalArray.length; i++){
				generalArray[i]=generalArray[i].replaceAll("[0-9]", "");
			}
			
			//Slicing call
			Object[] outputSliced = SlicingManager.Slicing(specificArray, clauseArray, generalArray);
			String[] specificArraySliced = (String[]) outputSliced[0];
			String[] generalArraySliced = (String[]) outputSliced[1];
			
			BitSet[] bloomFilterStructure = ConvertPCToBloomFilter.bloomFilter(specificArraySliced, generalArraySliced);
			//add to trainingSet
			Main.trainingSet.add(new StructureLaberPair(bloomFilterStructure, 1));
			
			if (LogManager.generateCSV)
				PrintToCSV(PathToString, generalArray, generalArraySliced, specificArray, specificArraySliced, clauseArray, bloomFilterStructure, "1");
			
			//remove the last clause and rerun the workflow
			for (int i=specificArray.length - 1; i > 0; i--) {
				String[] specificArrayNoLast = new String[i];
				Object[] clauseArrayNoLast = new Object[i];
				String[] generalArrayNoLast = new String[i];
				System.arraycopy(specificArray, 0, specificArrayNoLast, 0, i);
				System.arraycopy(clauseArray, 0, clauseArrayNoLast, 0, i);
				System.arraycopy(generalArray, 0, generalArrayNoLast, 0, i);
				
				//Slicing call
				Object[] outputSlicedNoLast = SlicingManager.Slicing(specificArrayNoLast, clauseArrayNoLast, generalArrayNoLast);
				String[] specificArraySlicedNoLast = (String[]) outputSlicedNoLast[0];
				String[] generalArraySlicedNoLast = (String[]) outputSlicedNoLast[1];
				
				BitSet[] bloomFilterStructureNoLast = ConvertPCToBloomFilter.bloomFilter(specificArraySlicedNoLast, generalArraySlicedNoLast);
				
				Main.trainingSet.add(new StructureLaberPair(bloomFilterStructureNoLast, 1));
				
				if (LogManager.generateCSV)
					PrintToCSV("---", generalArrayNoLast, generalArraySlicedNoLast, specificArrayNoLast, specificArraySlicedNoLast, clauseArrayNoLast, bloomFilterStructureNoLast, "1");
			}
		}
	}
}

