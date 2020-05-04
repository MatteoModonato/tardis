package tardis.implementation;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import com.opencsv.CSVWriter;
import jbse.mem.Clause;

import static tardis.implementation.Util.shorten;
import java.io.FileWriter;
import java.io.IOException;
import tardis.Main;

/**
 * Class that takes as input the path conditions processed by EvoSuite.
 * The bloom filter structure is produced using the concrete and abstract
 * path conditions and this is added to the training set associated with
 * its label (0: infeasible, 1: feasible). 
 * The data (path condition, abstract path condition, concrete path condition,
 * bloom filter structure, label) are added to data.csv.
 */

public class TrainingSetManager {
	
	public static int N_ROWS = 16;
	public static int N_COLUMNS = 64;
	
	//metodo per stampare l'array di bitSet come serie di bit
	public static String[] printBits(BitSet[] array) {
		String[] arr = new String[N_ROWS];
		for (int i = 0; i < N_ROWS; i++) {
			StringBuilder s = new StringBuilder();
			for (int j = 0; j < N_COLUMNS; j++) {
					s.append(array[i].get(j) ? "1" : "0");
			}
			arr[i] = s.toString();
		}
		return arr;
	}
	
	
	
  
	public static void PCWriterCSVSuccess(Collection<Clause> PC, String className) throws IOException {
		if(PC!=null) {
			//className= manyInfeasiblePC/ManyInfeasiblePC
			String nameClass = className.replaceAll("/.*", "");
			//nameClass=manyInfeasiblePC

			//lavorare con la collection short
			Object[] clauseArray = shorten(PC).toArray();

			String PathToString = Util.stringifyPathCondition(shorten(PC));

			//divido le varie clausole delle patyh condition in un array
			String[] generalArray = PathToString.split(" && ");
			//System.out.println("General path condition(Num of clauses: " +generalArray.length+ "):  ");
			for (int i=0; i < generalArray.length; i++){
				//System.out.println("Prima della generalizzazione: "+generalArray[i]);
				//se contiene lo / significa che devo gestire anche il package eliminandolo della clausole, altrimenti no
				if (className.contains("/"))
					generalArray[i]=generalArray[i].replaceAll("[0-9]", "").replaceAll(nameClass, "").replaceAll("/", "").replaceAll("\\{ROOT\\}:", "").replaceAll("this", "").replaceAll("\\.", "");
				else
					generalArray[i]=generalArray[i].replaceAll("[0-9]", "").replaceAll("\\{ROOT\\}:", "").replaceAll("this", "").replaceAll("\\.", "");
				//System.out.println("Dopo la generalizzazione: "+generalArray[i]);

			}

			//chiamata al metodo Slicing
			//Object[] outputSliced = SlicingManager.Slicing(clauseArray, generalArray);
			//Object[] clauseArraySliced = (Object[]) outputSliced[0];
			//String[] generalArraySliced = (String[]) outputSliced[1];


			//BitSet[] bloomFilterStructure = bloomFilter(clauseArraySliced, generalArraySliced);
			BitSet[] bloomFilterStructure = bloomFilter(clauseArray, generalArray);

			Main.trainingSet.add(new StructureLaberPair(bloomFilterStructure, 1));
			//System.out.println("TrainingSet size: "+Main.trainingSet.size());

			String[] record;
			CSVWriter writer = new CSVWriter(new FileWriter("data.csv", true),
					';', 
					CSVWriter.NO_QUOTE_CHARACTER,
					CSVWriter.DEFAULT_ESCAPE_CHARACTER,
					CSVWriter.DEFAULT_LINE_END);
			//record = new String[] {PathToString, Arrays.toString(generalArray), Arrays.toString(clauseArray), Arrays.toString(generalArraySliced), Arrays.toString(clauseArraySliced), Arrays.toString(printBits(bloomFilterStructure)), "1"};
			record = new String[] {PathToString, Arrays.toString(generalArray), Arrays.toString(clauseArray), Arrays.toString(printBits(bloomFilterStructure)), "1"};
			//record = new String[] {PathToString.replace(";", ""), Arrays.toString(generalArray).replace(";", ""), Arrays.toString(clauseArray).replace(";", ""), Arrays.toString(printBits(bloomFilterStructure)), "1"};
			//record = new String[] {PathToString, Arrays.toString(generalArray), Arrays.toString(clauseArray), Arrays.toString(clauseArraySliced), Arrays.toString(printBits(bloomFilterStructure)).replace("[", "").replace("]", "").replace(",", ";"), "1"};
			writer.writeNext(record);
			writer.close();
			System.out.println("+++++[TrainingSetManager] For path condition: " + PathToString);
		    System.out.println("+++++[TrainingSetManager] PCWriterCSVSuccess: +++++ Data add to Training Set and saved to csv +++++");
		}
	}
  
	
	
	
	public static void PCWriterCSVFailure(Collection<Clause> PC, String className) throws IOException {
		if(PC!=null) {
			//className= manyInfeasiblePC/ManyInfeasiblePC
			String nameClass = className.replaceAll("/.*", "");
			//nameClass=manyInfeasiblePC

			//lavorare con la collection short
			Object[] clauseArray = shorten(PC).toArray();

			String PathToString = Util.stringifyPathCondition(shorten(PC));

			//divido le varie clausole delle patyh condition in un array
			String[] generalArray = PathToString.split(" && ");
			//System.out.println("General path condition(Num of clauses: " +generalArray.length+ "):  ");
			for (int i=0; i < generalArray.length; i++){
				//System.out.println("Prima della generalizzazione: "+generalArray[i]);
				//se contiene lo / significa che devo gestire anche il package eliminandolo della clausole, altrimenti no
				if (className.contains("/"))
					generalArray[i]=generalArray[i].replaceAll("[0-9]", "").replaceAll(nameClass, "").replaceAll("/", "").replaceAll("\\{ROOT\\}:", "").replaceAll("this", "").replaceAll("\\.", "");
				else
					generalArray[i]=generalArray[i].replaceAll("[0-9]", "").replaceAll("\\{ROOT\\}:", "").replaceAll("this", "").replaceAll("\\.", "");
				//System.out.println("Dopo la generalizzazione: "+generalArray[i]);

			}

			//chiamata al metodo Slicing
			//Object[] outputSliced = SlicingManager.Slicing(clauseArray, generalArray);
			//Object[] clauseArraySliced = (Object[]) outputSliced[0];
			//String[] generalArraySliced = (String[]) outputSliced[1];

			//BitSet[] bloomFilterStructure = bloomFilter(clauseArraySliced, generalArraySliced);
			BitSet[] bloomFilterStructure = bloomFilter(clauseArray, generalArray);

			Main.trainingSet.add(new StructureLaberPair(bloomFilterStructure, 0));
			//System.out.println("TrainingSet size: "+Main.trainingSet.size());

			String[] record;
			CSVWriter writer = new CSVWriter(new FileWriter("data.csv", true),
					';', 
					CSVWriter.NO_QUOTE_CHARACTER,
					CSVWriter.DEFAULT_ESCAPE_CHARACTER,
					CSVWriter.DEFAULT_LINE_END);
			//record = new String[] {PathToString, Arrays.toString(generalArray), Arrays.toString(clauseArray), Arrays.toString(generalArraySliced), Arrays.toString(clauseArraySliced), Arrays.toString(printBits(bloomFilterStructure)), "0"};
			record = new String[] {PathToString, Arrays.toString(generalArray), Arrays.toString(clauseArray), Arrays.toString(printBits(bloomFilterStructure)), "0"};
			//record = new String[] {PathToString.replace(";", ""), Arrays.toString(generalArray).replace(";", ""), Arrays.toString(clauseArray).replace(";", ""), Arrays.toString(printBits(bloomFilterStructure)), "0"};
			//record = new String[] {PathToString, Arrays.toString(generalArray), Arrays.toString(clauseArray), Arrays.toString(clauseArraySliced), Arrays.toString(printBits(bloomFilterStructure)).replace("[", "").replace("]", "").replace(",", ";"), "0"};
			writer.writeNext(record);
			writer.close();
			System.out.println("+++++[TrainingSetManager] For path condition: " + PathToString);
		    System.out.println("+++++[TrainingSetManager] PCWriterCSVFailure: +++++ Data add to Training Set and saved to csv +++++");
		}
	}
	
	
	
	
	public static BitSet[] bloomFilter(Object[] clauseArray, String[] generalArray) {

		//creo array di bitSet bloom filter e array con dentro numeri primi per generare varie funzioni di hash
		BitSet[] bloomFilterStructure = new BitSet[N_ROWS];
		for (int i=0; i < bloomFilterStructure.length; i++){
			bloomFilterStructure[i] =  new BitSet(N_COLUMNS);
		}
		int[] primeNumber = new int[] {7, 11, 13};

		//scorro l'array che contiere le differenti condizioni della path condition  
		for (int i = 0; i < clauseArray.length; i++) {
			String singleClauseArray = clauseArray[i].toString();
			String singleGeneralArray = generalArray[i];
			//scorro i vari numeri primi per applicare differenti funzioni di hash alla condizione generale e specifica
			for (int j = 0; j < primeNumber.length; j++) {
				long hashGeneral = primeNumber[j];
				long hashClause = primeNumber[j];

				//uso la funzione hashCode applicata direttamente all'intera stringa della condizione generale e specifica
				hashGeneral = 31*hashGeneral + singleGeneralArray.hashCode();
				hashClause = 31*hashClause + singleClauseArray.hashCode();

				long hashToPositiveGeneral = Math.abs(hashGeneral);
				long hashToPositiveClause = Math.abs(hashClause);

				//faccio modulo per riportare gli hash generati nel range della dimensione dell'array bidimensionale
				long indexGeneral = hashToPositiveGeneral%64;
				long indexClause = hashToPositiveClause%15;
				
				int indexIntGeneral = (int) indexGeneral;
				int indexIntClause = (int) indexClause;
				
				//setto ad 1 il bit corrispondente all'indice generale sulla prima riga
				//setto ad 1 il bit corrispondente all'indice specifico sulla colonna del bit generale precedente
				bloomFilterStructure[0].set(indexIntGeneral);
				bloomFilterStructure[indexIntClause+1].set(indexIntGeneral);
			}  
		}
		return bloomFilterStructure;
	}
	
}

