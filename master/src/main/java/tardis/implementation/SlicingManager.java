package tardis.implementation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jbse.mem.ClauseAssumeAliases;
import jbse.mem.ClauseAssumeExpands;
import jbse.mem.ClauseAssumeNull;

/**
 * Class that takes as input the concrete and 
 * abstract path conditions and returns the 
 * path conditions after applying the Slicing 
 * procedure starting from the last clause.
 */

public class SlicingManager {
	
	public static Object[] Slicing(String[] PathToStringArray, Object[] clauseArray, String[] generalArray) {
		
		HashSet<String> variableSet = new HashSet<String>();
		String[] clauseArrayInput = new String[clauseArray.length];
		ArrayList<HashSet<String>> originSets = new ArrayList<HashSet<String>>();
		
		Pattern pattern = Pattern.compile("\\{(.*?\\d)\\}");
		Pattern patternVariable = Pattern.compile("\\(\\{ROOT\\}:(.*?)\\)");
		
		//creo originSet: insieme delle origin di ogni clausola
		for (int j=0; j < PathToStringArray.length; j++) {
			Matcher matcherVariable = patternVariable.matcher(PathToStringArray[j]);
			Matcher matcher = pattern.matcher(clauseArray[j].toString());
			if (matcherVariable.find()) {
				ArrayList<String> variablePathToStringClause = new ArrayList<String>();
				ArrayList<String> variableClauseArrayClause = new ArrayList<String>();
				matcherVariable.reset();
				while (matcherVariable.find()) {
					variablePathToStringClause.add(matcherVariable.group(0));
				}
				while (matcher.find()) {
					variableClauseArrayClause.add(matcher.group(1));
				}
				HashSet<String> set = new HashSet<String>();
				for (int i=0; i < variablePathToStringClause.size(); i++) {
					String[] split = variablePathToStringClause.get(i).split("(?<=:)");
					if (split[split.length-1].contains("]") || split[split.length-1].contains("length")){
						set.add(variableClauseArrayClause.get(i));
					}
					else {
						StringBuilder origin = new StringBuilder();
						for (int k=0; k < split.length-1; k++) {
							origin.append(split[k]);
						}
						//replace per eliminare la parentesi prima di {ROOT} in modo da avere coerenza
						//con le origin di ClauseAssumeExpands, ClauseAssumeAliases e ClauseAssumeNull
						set.add(origin.toString().replaceFirst("\\(", ""));
					}
				}
				originSets.add(set);
			}
			else if (clauseArray[j] instanceof ClauseAssumeExpands) {
				HashSet<String> set = new HashSet<String>();
				String[] split = PathToStringArray[j].split("(?<=:)");
				StringBuilder origin = new StringBuilder();
				for (int k=0; k < split.length-1; k++) {
					origin.append(split[k]);
				}
				set.add(origin.toString());
				originSets.add(set);
			} else if (clauseArray[j] instanceof ClauseAssumeAliases) {
				HashSet<String> set = new HashSet<String>();
				String[] beforeSplit = PathToStringArray[j].split("aliases");
				String[] split = beforeSplit[0].split("(?<=:)");
				StringBuilder origin = new StringBuilder();
				for (int k=0; k < split.length-1; k++) {
					origin.append(split[k]);
				}
				set.add(origin.toString());
				originSets.add(set);
			} else if (clauseArray[j] instanceof ClauseAssumeNull) {
				HashSet<String> set = new HashSet<String>();
				String[] split = PathToStringArray[j].split("(?<=:)");
				StringBuilder origin = new StringBuilder();
				for (int k=0; k < split.length-1; k++) {
					origin.append(split[k]);
				}
				set.add(origin.toString());
				originSets.add(set);
			}
		}
		
		//rimuovere le origin di tipo {ROOT}:
		for(HashSet<String> h: originSets) {
			h.remove("{ROOT}:");
		}
		
		
		//aggiungo all'hashset le variabili dell'ultima condizione della path condition
		//TODO vedere se devo resettare matcher
		Matcher matcher = pattern.matcher(clauseArray[clauseArray.length-1].toString());
		while (matcher.find()) {
			variableSet.add(matcher.group(1));
		}
		variableSet.addAll(originSets.get(originSets.size()-1));
		clauseArrayInput[clauseArray.length-1] = clauseArray[clauseArray.length-1].toString();
		
		//scorro clauseArray.length - 1 volte le condizioni della path condition per individuare anche le dipendenze indirette
		for (int l=0; l < clauseArray.length - 1; l++) {
			//scorro tutte le condizioni della path condition eccetto l'ultima
			for (int m=0; m < clauseArray.length - 1; m++) {
				HashSet<String> supportSet = new HashSet<String>();
				//aggiungo le origin della condizione che sto analizzando al set di supporto
				supportSet.addAll(originSets.get(m));
				Matcher matcherLoop = pattern.matcher(clauseArray[m].toString());
				//per ogni match aggiungo la variabile in un set di supporto
				//successivamente controllo se almeno un elemento del support set è in comune con il variable set
				//in tal caso aggiungo l'intero support set al variable set
				while (matcherLoop.find()) {
					supportSet.add(matcherLoop.group(1));
					for (String variable : supportSet) {
						if (variableSet.contains(variable)) {
							clauseArrayInput[m] = clauseArray[m].toString();
							variableSet.addAll(supportSet);	
						}
					}
				}
			}
		}
		
		//rimozione degli elementi nulli di clauseArrayInput
		//eliminazione delle clausole generalizzate contenute in generalArray nella medesima posizione dei valori null in clauseArrayInput
		List<String> values1 = new ArrayList<String>();
		List<String> values2 = new ArrayList<String>();
		for(int k=0; k<clauseArrayInput.length; k++) {
			if(clauseArrayInput[k] != null) { 
				values1.add(PathToStringArray[k]);
				values2.add(generalArray[k]);
			}
		}
		String[] specificArrayOutput = values1.toArray(new String[values1.size()]);
		String[] generalArrayOutput = values2.toArray(new String[values2.size()]);
		
		
		Object[] output = new Object[]{specificArrayOutput, generalArrayOutput};
		return output;
	
	}
}
