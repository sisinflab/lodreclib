package it.poliba.sisinflab.LODRec.learning;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import gnu.trove.iterator.TDoubleIterator;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectFloatHashMap;
import it.poliba.sisinflab.LODRec.evaluation.Evaluator;
import it.poliba.sisinflab.LODRec.recommender.Recommender;

public class LibLinearLearner {
	
	private String workingDir;
	private String modelFileName;
	
	private String trainingFile;
	private String validationFile;
	private String user_path_index_file;
	
	private int num_features;
	private TDoubleArrayList listC;
	private TDoubleArrayList listEps;
	private TDoubleArrayList listP;
	private TIntArrayList listSolverType;
	boolean silent;
	
	private String evalMetric;
	private float relUnknownItems;
	private float evalRatingThresh;
	
	private String validationRatingFile;
	
	private FeatureNode[][] XTrain;
	private double[] yTrain;
	
	private int topN = 100;
	
	private static Logger logger = LogManager.getLogger(LibLinearLearner.class.getName());
	
	public LibLinearLearner(String workingDir, String validationRatingFile, 
			float evalRatingThresh, boolean silentLearning, String listStrSolverType, 
			String listStrC, String listStrEps, String listStrP, String evalMetric,
			float relUnknownItems){
		
		this.workingDir = workingDir;
		this.evalRatingThresh = evalRatingThresh;
		this.relUnknownItems = relUnknownItems;
		this.evalMetric = evalMetric;
		this.silent = silentLearning;
		this.validationRatingFile = validationRatingFile;
		
		modelFileName = workingDir + "bestModel";
		
		listC = new TDoubleArrayList();
		String[] parts = listStrC.split(",");
		for (int i = 0; i < parts.length; i++) {
			double val = Double.parseDouble(parts[i]);
			listC.add(val);
		}
		listEps = new TDoubleArrayList();
		parts = listStrEps.split(",");
		for (int i = 0; i < parts.length; i++) {
			double val = Double.parseDouble(parts[i]);
			listEps.add(val);
		}
		listP = new TDoubleArrayList();
		parts = listStrP.split(",");
		for (int i = 0; i < parts.length; i++) {
			double val = Double.parseDouble(parts[i]);
			listP.add(val);
		}
		listSolverType = new TIntArrayList();
		parts = listStrSolverType.split(",");
		for (int i = 0; i < parts.length; i++) {
			int val = Integer.parseInt(parts[i]);
			listSolverType.add(val);
		}
		
		init();
		
	}
	
	private void init() {
		
		this.user_path_index_file = workingDir + "user_path_index";
		countNumFeatures();
		
		this.trainingFile = workingDir + "train";
		this.validationFile = workingDir + "validation";
	}
	
	private void countNumFeatures(){
		
		try {
			
			int count = 0;
			
			BufferedReader br = new BufferedReader(new FileReader(user_path_index_file));
			while(br.readLine()!=null)
				count++;
			
			num_features = count;
			br.close();
			
		}
		catch(Exception e){
			e.printStackTrace();
			num_features = 0;
		}
		
	}
    
    
    private void loadTrainDataset(String file){
    	
    	logger.info("Loading dataset");
    	int nRows = computeTrainRows(file);
    	
    	
    	try{
			BufferedReader br = new BufferedReader(new FileReader(file));
			XTrain = new FeatureNode[nRows][];
			yTrain = new double[nRows];
			
			
			String line = null;
			int j = 0;
			FeatureNode[] fn = null;
			while((line=br.readLine()) != null){
				
				String[] vals = line.split(" ");
				fn = new FeatureNode[vals.length-3];
				for(int i = 3; i < vals.length; i++){
					String[] ss = vals[i].split(":");
					int key = Integer.parseInt(ss[0]);
					double value = Double.parseDouble(ss[1]);
					fn[i-3] = new FeatureNode(key, value);
				}
				
				XTrain[j] = fn;
				yTrain[j] = Double.parseDouble(vals[0]);
				
				j++;
			}
			
			br.close();
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
    	
    }
    
    private int computeTrainRows(String trainFile) {
    	
    	int nRows = 0;
    	
    	try {
			BufferedReader br = new BufferedReader(new FileReader(trainFile));
			
			while ((br.readLine()) != null)
				nRows++;

			br.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
    	
    	
    	return nRows;
    	
    }
    
    private Problem createProblem(){
    	
    	logger.info("Creating problem");
    	
    	Problem problem = new Problem();
		problem.l = XTrain.length;// - 1; // number of training examples
		problem.n = num_features + 1; // number of features
		problem.x = XTrain; // feature nodes
		problem.y = yTrain; // target values
		
		logger.info("Number of training examples: " + problem.l);
		logger.info("Number of features: " + problem.n);
		
		return problem;
    	
    }
    
    public void train(){
    	
    	try {
    		
    		double bestPerf = 0, bestC = 0, bestEps = 0;
			Model bestModel = null;
			int bestModelType = 0;
			
			Model model;
			Recommender pred;
			
			TObjectFloatHashMap<String> evalRes = new TObjectFloatHashMap<String>();
			
			String[] str = evalMetric.split("@");
	    	int topK = Integer.parseInt(str[1]);
	    	String metric = str[0];
	    	
	    	Evaluator evaluator = new Evaluator(validationRatingFile, topK, 
	    			evalRatingThresh, relUnknownItems);
	    	
	    	if (silent)
				Linear.setDebugOutput(null);
	    	
	    	loadTrainDataset(trainingFile);
	    	Problem problem = createProblem();
	    	
	    	logger.info("Start learning process");
	    	
	    	TIntIterator itS = listSolverType.iterator();
	    	while(itS.hasNext()) {
	    		
	    		int s = itS.next();
	    		if (s != 11 & s != 12 & s != 13) {
					listP = new TDoubleArrayList();
					listP.add(0.1);
				}
	    		
	    		TDoubleIterator itC = listC.iterator();
	    		while(itC.hasNext()) {
	    			
	    			double c = itC.next();
	    			
	    			TDoubleIterator itE = listEps.iterator();
	    			while(itE.hasNext()) {
	    				
	    				double e = itE.next();
	    				
	    				TDoubleIterator itP = listP.iterator();
	    				while(itP.hasNext()) {
	    					
	    					double p = itP.next();
	    					
	    					SolverType solver = SolverType.getById(s);
							Parameter parameter = new Parameter(solver, c, e, p);
							logger.info("solver: " + s + ", c:" + c + ", eps:" + e+ ", p:" + p);
							model = Linear.train(problem, parameter);
							
							pred = new Recommender(model, topN);
							Map<Integer, List<Integer>> validRec = pred
									.computeRec(this.validationFile);

							evalRes = evaluator.eval(validRec);
							
							float perf = evalRes.get(metric);
							if (perf >= bestPerf) {

								bestPerf = perf;
								bestModel = model;

								bestModelType = parameter.getSolverType()
										.getId();
								bestC = parameter.getC();
								bestEps = parameter.getEps();
							}
	    					
	    				}
	    			}
	    		}
	    	}
	    	
	    	logger.info("-----------------------------------------------------------------");
			logger.info("BEST MODEL " + bestModelType + ". C: "
					+ bestC + ". Eps: " + bestEps + " . Metric " + evalMetric
					+ ": " + bestPerf);
			
			File modelFile = new File(modelFileName);
			bestModel.save(modelFile);
    	}
    	catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    }

}
