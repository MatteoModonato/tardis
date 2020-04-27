package tardis.implementation;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import tardis.Main;
import tardis.framework.InputBuffer;
import tardis.framework.OutputBuffer;

/**
 * @param <E> the type of the items stored in the buffer.
 */
public final class ListInputOutputBuffer<E> implements InputBuffer<E>, OutputBuffer<E> {
	public final ArrayList<JBSEResult> list = new ArrayList<>();
	//private final LinkedBlockingQueue<Integer> supportQueue = new LinkedBlockingQueue<>();

    @Override
    public boolean add(E item) {
    	//supportQueue.add(1);
    	//System.out.println("Lunghezza this.list ADD: "+ this.list.size());
        return this.list.add((JBSEResult) item);
    }

    @Override
    public E poll(long timeoutDuration, TimeUnit timeoutTimeUnit) throws InterruptedException {
    	if(this.list.size()!=0) {
    	//if (supportQueue.poll(timeoutDuration, timeoutTimeUnit) != null) {
    		int threshold = 5;
			int minTrainsetLen = 2;
			boolean flag = false;
			
			System.out.println("Lunghezza this.list: "+ this.list.size());
			System.out.println("Lunghezza Main.trainingSet: "+ Main.trainingSet.size());
			System.out.println("Lunghezza Main.trainingSetLen: "+ Main.trainingSetLen);
			
			//Se non ho almeno tre elementi nel training set allora non posso 
			//fare knn quindi utilizzo FIFO restituendo il primo elemento della lista.
			if (Main.trainingSet.size()>minTrainsetLen) {
				ArrayList<Double> label1Voting3 = new ArrayList<Double>();
				ArrayList<Double> label1Voting2 = new ArrayList<Double>();
				ArrayList<Double> label0Voting2 = new ArrayList<Double>();
				ArrayList<Double> label0Voting3 = new ArrayList<Double>();
				
				for (int i=0; i<this.list.size(); i++) {
					//Se il trainingSet non è aumentato di n elementi allora classifico solo 
					//gli elementi dentro la lista che hanno la label uguale a 2 cioè significa 
					//che non sono ancora mai stati classificati con KNN.
					if(Main.trainingSet.size()<Main.trainingSetLen+threshold) {
						//System.out.println("SONO NELL'IF CHE CALCOLA SOLO QUELLI ANCORA NON CALCOLATI");
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
								label1Voting3.add(this.list.get(i).getAverage());
							}
							else {
								label1Voting2.add(this.list.get(i).getAverage());
							}
						}
						else {
							if (this.list.get(i).getVoting() == 2) {
								label0Voting2.add(this.list.get(i).getAverage());
							}
							else {
								label0Voting3.add(this.list.get(i).getAverage());
							}	
						}
						
					}
					//Se il trainingSet è aumentato di n elementi allora classifico tutti 
					//gli elementi dentro la lista indipendentemente dal fatto che siano già
					//stati classificati una volta con il KNN. (Riclassifico tutto poichè il 
					//trainingSet è cresciuto di un valore significativo).
					else {
						flag = true;
						//System.out.println("SONO NELL'ELSE CHE RICALCOLA TUTTO");
						//System.out.println("Valore della flag nell'else: "+flag);
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
						
						if (label == 1) {
							if (voting == 3) {
								label1Voting3.add(average);
							}
							else {
								label1Voting2.add(average);
							}
						}
						else {
							if (voting == 2) {
								label0Voting2.add(average);
							}
							else {
								label0Voting3.add(average);
							}	
						}
						
					}
					
				}
				if (flag == true) {
					Main.trainingSetLen = Main.trainingSet.size();
					//System.out.println("SONO NELL'IF DELLA FLAG");
				}
				double averageReturned = ExtractByCumulative.extractByCumulative(label1Voting3, label1Voting2, label0Voting2, label0Voting3);
				for (int i=0; i<this.list.size(); i++) {
					if (this.list.get(i).getAverage() == averageReturned) {
						JBSEResult item = this.list.get(i);
						this.list.remove(i);
						System.out.println("RETURN -- CUMULATIVE");
						return (E) item;
					}
				}
				
			}
			JBSEResult item = this.list.get(0);
			this.list.remove(0);
			System.out.println("RETURN -- FIFO");
			return (E) item;

    	}
    	//se la lista è vuota allora ritorna null (meccanismo per sostituire il timeout)
    	E item = null;
    	return item;
    }

    @Override
    public boolean isEmpty() {
        return this.list.isEmpty();
    }
}
