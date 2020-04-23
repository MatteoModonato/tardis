package tardis.implementation;

import java.util.ArrayList;
import java.util.Arrays;
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
        	total = total + nearestKPointsDistance[i];
        }
		double average = total / nearestKPointsDistance.length;
		
		if (countLabel0>countLabel1) {
			Object[] output = new Object[]{findPopular(nearestKPoints), countLabel0, average};
			return output;
		}
		else {
			Object[] output = new Object[]{findPopular(nearestKPoints), countLabel1, average};
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

	public static int findPopular(int[] a) {

		if (a == null || a.length == 0)
			return 0;

		Arrays.sort(a);

		int previous = a[0];
		int popular = a[0];
		int count = 1;
		int maxCount = 1;

		for (int i = 1; i < a.length; i++) {
			if (a[i] == previous)
				count++;
			else {
				if (count > maxCount) {
					popular = a[i-1];
					maxCount = count;
				}
				previous = a[i];
				count = 1;
			}
		}

		return count > maxCount ? a[a.length-1] : popular;

	}
}

