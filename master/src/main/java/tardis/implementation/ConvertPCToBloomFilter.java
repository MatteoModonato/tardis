package tardis.implementation;

import static tardis.implementation.Util.shorten;
import java.io.IOException;
import java.util.BitSet;
import java.util.Collection;
import jbse.mem.Clause;

/**
 * Class that takes as input the path conditions and 
 * produces the bloom filter structure using the 
 * concrete and abstract path conditions.
 */

public class ConvertPCToBloomFilter {
	
	public static int N_ROWS = 16;
	public static int N_COLUMNS = 64;
	
	public static BitSet[] PCToBloomFilter(Collection<Clause> PC, String className) throws IOException {
		if(PC!=null) {
			//className= manyInfeasiblePC/ManyInfeasiblePC
			String nameClass = className.replaceAll("/.*", "");
			//nameClass=manyInfeasiblePC

			//lavorare con la collection short
			Object[] clauseArray = shorten(PC).toArray();

			String PathToString = Util.stringifyPathCondition(shorten(PC));

			//divido le varie clausole delle patyh condition in un array
			String[] generalArray = PathToString.split(" && ");
			String[] specificArray = PathToString.split(" && ");
			//System.out.println("General path condition(Num of clauses: " +generalArray.length+ "):  ");
			for (int i=0; i < generalArray.length; i++){
				//System.out.println("Prima della generalizzazione: "+generalArray[i]);
				//se contiene lo / significa che devo gestire anche il package eliminandolo della clausole, altrimenti no
				if (className.contains("/")) {
					generalArray[i]=generalArray[i].replaceAll("[0-9]", "").replaceAll(nameClass, "").replaceAll("/", "").replaceAll("\\{ROOT\\}:", "").replaceAll("this", "").replaceAll("\\.", "");
					specificArray[i]=specificArray[i].replaceAll(nameClass, "").replaceAll("/", "").replaceAll("\\{ROOT\\}:", "").replaceAll("this", "").replaceAll("\\.", "");
				}
				else {
					generalArray[i]=generalArray[i].replaceAll("[0-9]", "").replaceAll("\\{ROOT\\}:", "").replaceAll("this", "").replaceAll("\\.", "");
					specificArray[i]=specificArray[i].replaceAll("\\{ROOT\\}:", "").replaceAll("this", "").replaceAll("\\.", "");
				}
				//System.out.println("Dopo la generalizzazione: "+generalArray[i]);

			}

			//chiamata al metodo Slicing
			//Object[] outputSliced = SlicingManager.Slicing(PathToStringArray, clauseArray, generalArray);
			//Object[] clauseArraySliced = (Object[]) outputSliced[0];
			//String[] generalArraySliced = (String[]) outputSliced[1];


			//BitSet[] bloomFilterStructure = bloomFilter(clauseArraySliced, generalArraySliced);
			BitSet[] bloomFilterStructure = bloomFilter(specificArray, generalArray);

			return bloomFilterStructure;
		}
		else {
			BitSet[] bloomFilterStructure = new BitSet[N_ROWS];
			return bloomFilterStructure;
		}
	}
	
	
	public static BitSet[] bloomFilter(String[] specificArray, String[] generalArray) {

		//creo array di bitSet bloom filter e array con dentro numeri primi per generare varie funzioni di hash
		BitSet[] bloomFilterStructure = new BitSet[N_ROWS];
		for (int i=0; i < bloomFilterStructure.length; i++){
			bloomFilterStructure[i] =  new BitSet(N_COLUMNS);
		}
		int[] primeNumber = new int[] {7, 11, 13};

		//scorro l'array che contiere le differenti condizioni della path condition  
		for (int i = 0; i < specificArray.length; i++) {
			String singleClauseArray = specificArray[i];
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
