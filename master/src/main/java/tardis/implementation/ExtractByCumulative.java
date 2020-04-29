package tardis.implementation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class ExtractByCumulative {
	
	public static JBSEResult extractByCumulative (ArrayList<JBSEResult> labelVoting) {
		
		//ArrayList<JBSEResult> listOriginal = new ArrayList<JBSEResult>();
		//listOriginal.addAll(labelVoting);
		
		ArrayList<Double> listCumulative = new ArrayList<Double>();
		
		for (int i = 0; i<labelVoting.size(); i++) {
			listCumulative.add(i, labelVoting.get(i).getAverage());
		}
		
		//eseguo la cumulativa
		for (int i = 1; i<labelVoting.size(); i++) {
			double cumulative = listCumulative.get(i)+listCumulative.get(i-1);
			listCumulative.set(i, cumulative);
		}
		
		//converto gli intervalli da double ad int
		ArrayList<Integer> listCumulativeInt = new ArrayList<Integer>();
		for(Double d : listCumulative){
			d = d*100;
			listCumulativeInt.add(d.intValue());
		}
		listCumulativeInt.add(0, 0);
		
		//genero un numero randomico compreso tra 0 e il massimo in listCumulativeInt
		Random rand = new Random();
		int upperbound=listCumulativeInt.get(listCumulativeInt.size()-1)-1;
		int intRandom = rand.nextInt(upperbound);
		
		for (int i = 1; i<listCumulativeInt.size(); i++) {
			if(intRandom>=listCumulativeInt.get(i-1) && intRandom<listCumulativeInt.get(i)) {
				return labelVoting.get(i-1);
			}
		}
		return null;

	}

}

