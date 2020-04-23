package tardis.implementation;

import java.util.BitSet;

/**
 * class that defines the (bloomFilterStructure, label)
 * pair structure used in the trainingSet.
 */

public class StructureLaberPair {
	
	BitSet[] bloomFilterStructure;
	int label;
	
	StructureLaberPair(BitSet[] bloomFilterStructure, int label){
		this.bloomFilterStructure = bloomFilterStructure;
		this.label = label;
	}
	
	BitSet[] getBloomFilterStructure(){
		return this.bloomFilterStructure;
	}
	
	int getLabel() {
		return this.label;
	}
}

