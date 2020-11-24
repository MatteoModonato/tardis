package tardis.implementation;

import java.util.ArrayList;
import java.util.Random;

/**
 * Class that takes as input the set chosen by ListInputOutputBuffer.java. 
 * It chooses the JBSEResult to be extracted by cumulative of the average 
 * of the k distances and subsequent random extraction.
 */

public class ExtractByCumulative {
	
	public static JBSEResult extractByCumulative (ArrayList<JBSEResult> labelVoting) {
		
		ArrayList<Double> listCumulative = new ArrayList<Double>();
		
		for (int i = 0; i<labelVoting.size(); i++) {
			listCumulative.add(i, labelVoting.get(i).getAverage());
		}
		
		//calculate cumulative
		for (int i = 1; i<labelVoting.size(); i++) {
			double cumulative = listCumulative.get(i)+listCumulative.get(i-1);
			listCumulative.set(i, cumulative);
		}
		
		//convert ranges from double to int
		ArrayList<Integer> listCumulativeInt = new ArrayList<Integer>();
		for(Double d : listCumulative){
			d = d*100;
			listCumulativeInt.add(d.intValue());
		}
		listCumulativeInt.add(0, 0);
		
		//generate a random number between 0 and the maximum of listCumulativeInt
		Random rand = new Random();
		int upperbound=listCumulativeInt.get(listCumulativeInt.size()-1)-1;
		//handle case where getAverage() is always equal to 0
		if (upperbound<1) 
			return labelVoting.get(0);
		
		int intRandom = rand.nextInt(upperbound);
		
		for (int i = 1; i<listCumulativeInt.size(); i++) {
			if(intRandom>=listCumulativeInt.get(i-1) && intRandom<listCumulativeInt.get(i)) {
				return labelVoting.get(i-1);
			}
		}
		return null;

	}

}

