package tardis.implementation;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Knn {

	public static Object[] knn(List<StructureLaberPair> bloomFilterList, BitSet[] query) {

		int k = 3;
		List<Result> resultList = new ArrayList<Result>();
		int countLabel0 = 0;
		int countLabel1 = 0;
		int countDistance0 = 0;
		int countDistance0Label0 = 0;
		int countDistance0Label1 = 0;
		double total = 0;

		for (StructureLaberPair structure : bloomFilterList) {
			double both=0;
			double atLeast=0;
			for (int i = 0; i < 16 ; i++) {
				for (int j = 0; j < 64; j++) {
					if (structure.getBloomFilterStructure()[i].get(j) == true && query[i].get(j) == true)
						both++;
					else if (structure.getBloomFilterStructure()[i].get(j) == false && query[i].get(j) == true)
						atLeast++;
					else if (structure.getBloomFilterStructure()[i].get(j) == true && query[i].get(j) == false)
						atLeast++;
				}
			}
			double jaccardDistance = both/(both+atLeast);
			resultList.add(new Result(jaccardDistance, structure.getLabel()));
			//System.out.println("Jaccard distance: "+jaccardDistance);
		}

		Collections.sort(resultList, new DistanceComparator());

		int[] nearestKPoints = new int[k];
		double[] nearestKPointsDistance = new double[k];
		for(int l = 0; l < k; l++){
			nearestKPoints[l] = resultList.get(l).label;
			nearestKPointsDistance[l] = resultList.get(l).distance;
			if (nearestKPoints[l] == 0)
				countLabel0 = countLabel0+1;
			else
				countLabel1 = countLabel1+1;
		}
		
		for(int i=0; i<nearestKPointsDistance.length; i++){
			if (nearestKPointsDistance[i]==0) {
				countDistance0 = countDistance0+1;
			}
			else {
				if (nearestKPoints[i]==0)
					countDistance0Label0 = countDistance0Label0+1;
				else
					countDistance0Label1 = countDistance0Label1+1;
			}
			total = total + nearestKPointsDistance[i];
		}
		
		double average = total / nearestKPointsDistance.length;
		
		//Gestisco anche i casi in cui una o più distanze è zero: non
		//esiste ancora nel training set evidenza per quella query.
		if (countDistance0==3 || countDistance0==2) {
			Object[] output = new Object[]{0, 3, 0.0};
			return output;
		}
		else if (countDistance0==1) {
			if (countDistance0Label0==countDistance0Label1) {
				Object[] output = new Object[]{0, 3, average};
				return output;
			}
			else if (countDistance0Label0==2) {
				Object[] output = new Object[]{0, 2, average};
				return output;
			}
			else if (countDistance0Label1==2) {
				Object[] output = new Object[]{1, 2, average};
				return output;
			}		
		}
		
		if (countLabel0>countLabel1) {
			Object[] output = new Object[]{0, countLabel0, average};
			return output;
		}
		else {
			Object[] output = new Object[]{1, countLabel1, average};
			return output;
		}
	}

	static class Result {	
		double distance;
		int label;
		public Result(double distance, int label){
			this.label = label;
			this.distance = distance;	    	    
		}
	}

	static class DistanceComparator implements Comparator<Result> {
		@Override
		public int compare(Result a, Result b) {
			return a.distance > b.distance ? -1 : a.distance == b.distance ? 0 : 1;
		}
	}
}

