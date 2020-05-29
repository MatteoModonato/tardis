package tardis.implementation;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import tardis.Main;

/**
 * Class that manages the generation of log files.
 * If generateLogFiles is set to true, a log file will be generated for 
 * each poll operation, containing information relating to the training
 * set, the pathConditionBufferer and the extracted path condition.
 * If generateLogFiles is set to false log system is disabled.
 */

public class LogManager {
	
	public final static boolean generateLogFiles = true;

	public static void logManager(ArrayList<String> buffer, long id) {
		
		int conuntFeasible = 0;
		int conuntInFeasible = 0;
		for(StructureLaberPair element:Main.trainingSet) {
			if(element.label==1)
				conuntFeasible= conuntFeasible+1;
			else
				conuntInFeasible= conuntInFeasible+1;
		}
		
		buffer.add(0, "Training set -- Length :"+Main.trainingSet.size()+" Feasibe PC: "+conuntFeasible+" Infeasibe PC: "+conuntInFeasible+"\n");
		
		try (FileWriter file = new FileWriter("outputlog/log"+id+".txt")) {
			for(String line:buffer) {
				file.write(line+"\n");
			}
			file.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
