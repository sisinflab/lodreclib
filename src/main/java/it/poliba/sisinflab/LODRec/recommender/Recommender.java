package it.poliba.sisinflab.LODRec.recommender;
import it.poliba.sisinflab.LODRec.main.Main;
import it.poliba.sisinflab.LODRec.utils.BST;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import ciir.umass.edu.learning.Ranker;
import ciir.umass.edu.learning.RankerFactory;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;

public class Recommender {
	
	private String workingDir;
	private int nThreads;
	
	private String recFile;
	
	private String modelFile;
	
	private int topN;
	
	private boolean libLinear;
	private boolean rankLib;
	
	private Ranker rankLibModel;
	private Model libLinearModel;
	
	private static Logger logger = LogManager.getLogger(Recommender.class.getName());
	
	public Recommender(Model model, int topN) {

		this.workingDir = Main.workingDir;
		this.libLinearModel = model;
		this.topN = topN;

	}
	
	public Recommender(String workingDir, String recOutputFile, int topN, int nThreads,
			boolean libLinear, boolean rankLib) {

		this.workingDir = workingDir;
		this.recFile = recOutputFile;
		this.topN = topN;
		this.nThreads = nThreads;
		this.libLinear = libLinear;
		this.rankLib = rankLib;

		init();

	}

	private void init() {

		this.modelFile = workingDir +"bestModel";
		loadModel();

	}

	private void loadModel() {

		logger.info("Loading prediction model");
		
		try {
			if (libLinear) {

				File model_file = new File(modelFile);
				libLinearModel = Model.load(model_file);

			} else if (rankLib) {

				RankerFactory rFact = new RankerFactory();
				rankLibModel = rFact.loadRanker(modelFile);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public Map<Integer, List<Integer>> computeRec(String file) {
		
		Map<Integer, List<Integer>> recc = new HashMap<Integer, List<Integer>>();
		
		try {
		
			ExecutorService executor = Executors.newFixedThreadPool(1);
			
			Runnable worker = new RecommenderWorker(file, recc, libLinearModel, topN);
			executor.execute(worker);
			
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		return recc;
		
	}
	
	public void computeRec(){
		
		logger.info("Start computing recommendations");
		
		ExecutorService executor;
		executor = Executors.newFixedThreadPool(nThreads);
		
		try {
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(recFile));
    	
	    	File directory = new File(workingDir);
			File[] files = directory.listFiles();
			for (int i = 0; i < files.length; i++) {
				
				if(files[i].getName().contains("test")) {
					
					String testFile = workingDir + files[i].getName();
					
					// path extraction worker user-items
					Runnable worker = new RecommenderWorker(testFile, bw, libLinearModel, 
							rankLibModel, topN, libLinear, rankLib);
					// run the worker thread  
					executor.execute(worker);
					
				}
				
			}
			
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			
			bw.flush();
			bw.close();
		
		}
		catch(Exception e) {
			e.printStackTrace();
		}		
    	
    }
	
	public Map<Integer, List<Integer>> computeRecc(FeatureNode[][] X,
			int[] userIndex, int[] itemIndex, boolean implicit) {

		Map<Integer, List<Integer>> recc = new HashMap<Integer, List<Integer>>();
		// logger.info("Start computing reccomendations");
		// usare un Btree invece della mappa per tener traccia solo dei topN

		Map<Integer, BST> predictions = new HashMap();

		double[] prob_estimates;
		double pred;
		for (int i = 0; i < X.length; i++) {

			/*if (libLinearModel.isProbabilityModel() & implicit) {
				prob_estimates = new double[2];
				Linear.predictProbability(libLinearModel, X[i], prob_estimates);
				pred = prob_estimates[0];
			} else
				pred = Linear.predict(libLinearModel, X[i]);*/
			
			pred = Linear.predict(libLinearModel, X[i]);

			int user_id = userIndex[i];
			int item_id = itemIndex[i];

			if (!predictions.containsKey(user_id))
				predictions.put(user_id, new BST(topN));

			predictions.get(user_id).insert(pred, item_id);

		}

		for (int user_id : predictions.keySet()) {
			BST bst = predictions.get(user_id);
			bst.visit();
			List<Integer> list_items = bst.getSortedValues();
			recc.put(user_id, list_items);
		}
		return recc;

	}

}
