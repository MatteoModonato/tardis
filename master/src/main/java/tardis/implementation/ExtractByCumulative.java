package tardis.implementation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class ExtractByCumulative {
	
	public static Double extractByCumulative (ArrayList<Double> label1Voting3, ArrayList<Double> label1Voting2,
			ArrayList<Double> label0Voting2, ArrayList<Double> label0Voting3) {
		
		int range1 = 1;
		int range12 = 24;
		int range2 = 25;
		int range22 = 49;
		int range3 = 50;
		int range32 = 74;
		int range4 = 75;
		int range42 = 100;
		
		ArrayList<Double> listOriginalAvarage = new ArrayList<Double>();
		listOriginalAvarage.addAll(label1Voting3);
		listOriginalAvarage.addAll(label1Voting2);
		listOriginalAvarage.addAll(label0Voting2);
		listOriginalAvarage.addAll(label0Voting3);
		
		//utilizzo la normalizzazione per "spalmare" i valori nei range
		if (label1Voting3.isEmpty() == false) {
			double minLabel1Voting3 = Collections.min(label1Voting3);
			double maxLabel1Voting3 = Collections.max(label1Voting3);
			
			for (int i = 0; i<label1Voting3.size(); i++) {
				Double normalized = ((range42-range4)*((label1Voting3.get(i)-minLabel1Voting3)/(maxLabel1Voting3-minLabel1Voting3)))+range4;
				if (normalized.isNaN())
						normalized = (double) range42;
				label1Voting3.set(i, normalized);
			}
		}
		if (label1Voting2.isEmpty() == false) {
			double minLabel1Voting2 = Collections.min(label1Voting2);
			double maxLabel1Voting2 = Collections.max(label1Voting2);
			
			for (int i = 0; i<label1Voting2.size(); i++) {
				Double normalized = ((range32-range3)*((label1Voting2.get(i)-minLabel1Voting2)/(maxLabel1Voting2-minLabel1Voting2)))+range3;
				if (normalized.isNaN())
						normalized = (double) range32;;
				label1Voting2.set(i, normalized);
			}
		}
		if (label0Voting2.isEmpty() == false) {
			double minLabel0Voting2 = Collections.min(label0Voting2);
			double maxLabel0Voting2 = Collections.max(label0Voting2);
			
			for (int i = 0; i<label0Voting2.size(); i++) {
				Double normalized = ((range22-range2)*((label0Voting2.get(i)-minLabel0Voting2)/(maxLabel0Voting2-minLabel0Voting2)))+range2;
				if (normalized.isNaN())
						normalized = (double) range22;;
				label0Voting2.set(i, normalized);
			}
		}
		if (label0Voting3.isEmpty() == false) {
			double minLabel0Voting3 = Collections.min(label0Voting3);
			double maxLabel0Voting3 = Collections.max(label0Voting3);
			
			for (int i = 0; i<label0Voting3.size(); i++) {
				Double normalized = ((range12-range1)*((label0Voting3.get(i)-minLabel0Voting3)/(maxLabel0Voting3-minLabel0Voting3)))+range1;
				if (normalized.isNaN())
						normalized = (double) range12;
				label0Voting3.set(i, normalized);
			}
		}
		
		//appendo tutte le liste per fare la cumulativa
		label1Voting3.addAll(label1Voting2);
		label1Voting3.addAll(label0Voting2);
		label1Voting3.addAll(label0Voting3);
		
		//eseguo la cumulativa
		for (int i = 1; i<label1Voting3.size(); i++) {
			double cumulative = label1Voting3.get(i)+label1Voting3.get(i-1);
			label1Voting3.set(i, cumulative);
		}
		
		//converto gli intervalli da double ad int
		ArrayList<Integer> listCumulativeInt = new ArrayList<Integer>();
		for(Double d : label1Voting3){
			listCumulativeInt.add(d.intValue());
		}
		listCumulativeInt.add(0, 0);
		
		//genero un numero randomico compreso tra 0 e il massimo in listCumulativeInt
		Random rand = new Random();
		int upperbound=listCumulativeInt.get(listCumulativeInt.size()-1)-1;
		int intRandom = rand.nextInt(upperbound);
		
		for (int i = 1; i<listCumulativeInt.size(); i++) {
			if(intRandom>=listCumulativeInt.get(i-1) && intRandom<listCumulativeInt.get(i)) {
				return listOriginalAvarage.get(i-1);
			}
		}
		return (double) 0;
	}

}

