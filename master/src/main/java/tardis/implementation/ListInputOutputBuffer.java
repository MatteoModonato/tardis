package tardis.implementation;

import java.util.ArrayList;
import java.util.Random;
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
			
			System.out.println("Lunghezza this.list: "+ this.list.size());
			System.out.println("Lunghezza Main.trainingSet: "+ Main.trainingSet.size());
			System.out.println("Lunghezza Main.trainingSetLen: "+ Main.trainingSetLen);
			
			//Se non ho almeno tre elementi nel training set allora non posso 
			//fare knn quindi utilizzo FIFO restituendo il primo elemento della lista.
			if (Main.trainingSet.size()>minTrainsetLen) {
				ArrayList<JBSEResult> label1Voting3 = new ArrayList<JBSEResult>();
				ArrayList<JBSEResult> label1Voting2 = new ArrayList<JBSEResult>();
				ArrayList<JBSEResult> label0Voting2 = new ArrayList<JBSEResult>();
				ArrayList<JBSEResult> label0Voting3 = new ArrayList<JBSEResult>();
				
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
					
				}
				if (flag == true) {
					Main.trainingSetLen = Main.trainingSet.size();
					//System.out.println("SONO NELL'IF DELLA FLAG");
				}
				
				ArrayList<ArrayList<JBSEResult>> setOfPolls = new ArrayList<ArrayList<JBSEResult>>();
				setOfPolls.add(label1Voting3);
				setOfPolls.add(label1Voting2);
				setOfPolls.add(label0Voting2);
				setOfPolls.add(label0Voting3);
				
				//Genero numero randomico tra 0 e 100 per scegliere da quale insieme estarre il JBSEResult
				//Probalilità e ordine di successione degli insiemi:
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

				//Parto dall'insieme scelto tramite intRandom;
				//se l'insieme scelto è vuoto passo all'insieme immediatamente successivo
				for (int i = index; i<setOfPolls.size(); i++) {
					if(!setOfPolls.get(i).isEmpty()) {
						//JBSEResult item = setOfPolls.get(i).get(0);		
						JBSEResult item = ExtractByCumulative.extractByCumulative(setOfPolls.get(i));
						
						//Scorro this.list per identificare in base all'id il JBSEResult che ritorno 
						//ed eliminarlo dalla lista
						for (int j=0; j<this.list.size(); j++) {
							if(this.list.get(j).getId() == item.getId()) {
								this.list.remove(j);
								System.out.println("RETURN -- POLLS. Label: "+item.getLabel()+" Voting: "+item.getVoting()+" Average: "+item.getAverage());
								return (E) item;
							}
						}
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
