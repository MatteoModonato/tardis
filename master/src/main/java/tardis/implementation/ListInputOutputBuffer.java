package tardis.implementation;

import static tardis.implementation.Util.shorten;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import tardis.Main;
import tardis.framework.InputBuffer;
import tardis.framework.OutputBuffer;

/**
 * Class that implements the JBSEResult choice system through classifier.
 * The classification of the path conditions into feasible and infeasible 
 * is carried out each time a poll is performed on the pathConditionBuffer.
 * The class is more likely to extract path conditions that are classified as 
 * feasible (label 1) and less likely to extract those that are classified as 
 * infeasible (label 0) (50% label1Voting3, 30% label1Voting2, 
 * 15% label0Voting2, 5% label0Voting3).
 */

public final class ListInputOutputBuffer<E> implements InputBuffer<E>, OutputBuffer<E> {
	static final AtomicLong IDPOLL = new AtomicLong(0);
	public final ArrayList<JBSEResult> list = new ArrayList<>();

    @Override
    public boolean add(E item) {
        return this.list.add((JBSEResult) item);
    }

    @Override
    public E poll(long timeoutDuration, TimeUnit timeoutTimeUnit) throws InterruptedException {
    	if(this.list.size()!=0) {
    		int threshold = 5;
    		int minTrainsetLen = 2;
    		boolean flag = false;

    		//If I don't have at least three items in the training set and two in the list, it makes 
    		//no sense to use KNN then I use FIFO to return the first element of the list.
    		if (Main.trainingSet.size()>minTrainsetLen && this.list.size()>1) {
    			ArrayList<JBSEResult> label1Voting3 = new ArrayList<JBSEResult>();
    			ArrayList<JBSEResult> label1Voting2 = new ArrayList<JBSEResult>();
    			ArrayList<JBSEResult> label0Voting2 = new ArrayList<JBSEResult>();
    			ArrayList<JBSEResult> label0Voting3 = new ArrayList<JBSEResult>();

    			ArrayList<String> bufferLog = new ArrayList<String>();

    			for (int i=0; i<this.list.size(); i++) {
    				//If the trainingSet has not increased by n elements then I classify only the elements 
    				//inside the list which have the label equal to 2 that means they have never been 
    				//classified with KNN yet.
    				if(Main.trainingSet.size()<Main.trainingSetLen+threshold) {
    					if(this.list.get(i).getLabel() == 2) {
    						Object[] result = Knn.knn(Main.trainingSet, this.list.get(i).getbloomFilterStructure());
    						int label = (int) result[0];
    						int voting = (int) result[1];
    						double average = (double) result[2];

    						this.list.get(i).setLabel(label);
    						this.list.get(i).setVoting(voting);
    						this.list.get(i).setAverage(average);

    						//System.out.println("-------------------");
    						//System.out.println("label: "+label);
    						//System.out.println("voting: "+voting);
    						//System.out.println("average: "+average);
    						//System.out.println("-------------------");
    					}

    					if (this.list.get(i).getLabel() == 1) {
    						if (this.list.get(i).getVoting() == 3) {
    							label1Voting3.add(this.list.get(i));
    						}
    						else {
    							label1Voting2.add(this.list.get(i));
    						}
    					}
    					else {
    						if (this.list.get(i).getVoting() == 2) {
    							label0Voting2.add(this.list.get(i));
    						}
    						else {
    							label0Voting3.add(this.list.get(i));
    						}	
    					}

    				}
    				//If the trainingSet is increased by n elements then I classify all the elements within the list 
    				//regardless of whether they have already been classified once with the KNN. (I reclassify 
    				//everything as the trainingSet has grown by a significant value).
    				else {
    					flag = true;
    					Object[] result = Knn.knn(Main.trainingSet, this.list.get(i).getbloomFilterStructure());
    					int label = (int) result[0];
    					int voting = (int) result[1];
    					double average = (double) result[2];

    					this.list.get(i).setLabel(label);
    					this.list.get(i).setVoting(voting);
    					this.list.get(i).setAverage(average);

    					//System.out.println("-------------------");
    					//System.out.println("label: "+label);
    					//System.out.println("voting: "+voting);
    					//System.out.println("average: "+average);
    					//System.out.println("-------------------");

    					if (this.list.get(i).getLabel() == 1) {
    						if (this.list.get(i).getVoting() == 3) {
    							label1Voting3.add(this.list.get(i));
    						}
    						else {
    							label1Voting2.add(this.list.get(i));
    						}
    					}
    					else {
    						if (this.list.get(i).getVoting() == 2) {
    							label0Voting2.add(this.list.get(i));
    						}
    						else {
    							label0Voting3.add(this.list.get(i));
    						}	
    					}

    				}
    				if (LogManager.generateLogFiles)
    					bufferLog.add("ID: "+this.list.get(i).incrementalId + " | Label: "+this.list.get(i).getLabel() + " | Voting: "+this.list.get(i).getVoting() + " | Average: "+this.list.get(i).getAverage() + " | PC: "+Util.stringifyPathCondition(shorten(this.list.get(i).getFinalState().getPathCondition())) + " | PCSliced: ");
    			}

    			if (flag == true) {
    				Main.trainingSetLen = Main.trainingSet.size();
    			}

    			ArrayList<ArrayList<JBSEResult>> setOfPolls = new ArrayList<ArrayList<JBSEResult>>();
    			setOfPolls.add(label1Voting3);
    			setOfPolls.add(label1Voting2);
    			setOfPolls.add(label0Voting2);
    			setOfPolls.add(label0Voting3);

    			//Generate random number between 0 and 100 to choose which set to extract the JBSEResult from
    			//Probability and sequence order of sets:
    			//50% --> label1Voting3
    			//30% --> label1Voting2
    			//15% --> label0Voting2
    			//5%  --> label0Voting3

    			int range1 = 100;				
    			//increase range2 to increase the % of choosing label1Voting3
    			int range2 = 50;
    			int range3 = 20;
    			int range4 = 5;

    			Random rand = new Random();
    			int upperbound=100;
    			int intRandom = rand.nextInt(upperbound);

    			int index;
    			if(intRandom<=range1 && intRandom>100-range2) {
    				index=0;
    			}
    			else if(intRandom<=range2 && intRandom>range3) {
    				index=1;
    			}
    			else if(intRandom<=range3 && intRandom>range4) {
    				index=2;
    			}
    			else {
    				index=3;
    			}

    			//Start from the set chosen through intRandom
    			//If the chosen set is empty I go to the immediately following set
    			for (int i = index; i<setOfPolls.size(); i++) {
    				if(!setOfPolls.get(i).isEmpty()) {	
    					JBSEResult item = ExtractByCumulative.extractByCumulative(setOfPolls.get(i));
    					
    					if (LogManager.generateLogFiles) {
    						bufferLog.add("PC SETS DISTIBUTION -- label1voting3: "+label1Voting3.size()+" |label1voting2: "+label1Voting2.size()+" |label0voting2: "+label0Voting2.size()+" |label0voting3: "+label0Voting3.size()+"\n");
    						bufferLog.add("SETS RANDOM NUMBER:"+intRandom);
    						bufferLog.add("RETURN -- POLLS. ID: "+item.incrementalId+" Label: "+item.getLabel()+" Voting: "+item.getVoting()+" Average: "+item.getAverage());
    	    				LogManager.logManager(bufferLog, IDPOLL.getAndIncrement());
    	    				System.out.println("[ML MODEL] Write Log to file: log"+(IDPOLL.get()-1)+".txt");
    	    			}

    					//Iterate over this.list to identify by id the JBSEResult returned and delete it from the list
    					for (int j=0; j<this.list.size(); j++) {
    						if(this.list.get(j).getId() == item.getId()) {
    							this.list.remove(j);
    							System.out.println("[ML MODEL] RETURN -- POLL ID: "+item.incrementalId+" Label: "+item.getLabel()+" Voting: "+item.getVoting()+" Average: "+item.getAverage()+" |label1voting3: "+label1Voting3.size()+" |label1voting2: "+label1Voting2.size()+" |label0voting2: "+label0Voting2.size()+" |label0voting3: "+label0Voting3.size() + " ||| Length this.list: "+ this.list.size() + " Length TrainingSet: "+ Main.trainingSet.size() + " Length TrainingSet Threshold: "+ Main.trainingSetLen);
    							return (E) item;
    						}
    					}
    				}
    			}
    		}
    		JBSEResult item = this.list.get(0);
    		this.list.remove(0);
    		System.out.println("[ML MODEL] RETURN -- FIFO ||| Length this.list: "+ this.list.size() + " Length TrainingSet: "+ Main.trainingSet.size() + " Length TrainingSet Threshold: "+ Main.trainingSetLen);
    		return (E) item;

    	}
    	//If the list is empty then it returns null (timeout replacement)
    	E item = null;
    	return item;
    }

    @Override
    public boolean isEmpty() {
        return this.list.isEmpty();
    }
}
