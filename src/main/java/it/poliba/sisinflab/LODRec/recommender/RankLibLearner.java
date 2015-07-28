package it.poliba.sisinflab.LODRec.recommender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ciir.umass.edu.features.FeatureManager;
import ciir.umass.edu.features.LinearNormalizer;
import ciir.umass.edu.features.Normalizer;
import ciir.umass.edu.learning.CoorAscent;
import ciir.umass.edu.learning.RANKER_TYPE;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.learning.Ranker;
import ciir.umass.edu.learning.RankerFactory;
import ciir.umass.edu.learning.RankerTrainer;
import ciir.umass.edu.learning.boosting.AdaRank;
import ciir.umass.edu.learning.boosting.RankBoost;
import ciir.umass.edu.learning.tree.LambdaMART;
import ciir.umass.edu.learning.tree.RFRanker;
import ciir.umass.edu.metric.MetricScorer;
import ciir.umass.edu.metric.MetricScorerFactory;
import ciir.umass.edu.utilities.MyThreadPool;

public class RankLibLearner {

	// coordinate ascent params
	// public static int nRestart = 5;
	// public static int nMaxIteration = 25;

	// adarank params
	// public static int nIteration = 500;
	// rankboost params
	// public static int nIteration = 300;//number of rounds

	// random forest params
	// public static int nTreeLeaves = 100;
	// public static int nBag = 300;

	// lambdamart params
	// public static int nTrees = 50;//1000 the number of trees
	// public static float learningRate = 0.1F;//0.1or shrinkage

	Map<Integer, RANKER_TYPE> map_rankers = new HashMap();

	String trainMetric = "NDCG@10";
	String testMetric = "NDCG@10";

	private int nIterations;
	private int nTrees;
	private int nRoundToStopEarly;
	private int nTreeLeaves;
	private float learningRate;

	boolean normalize = false;

	int nThread = -1; // nThread = #cpu-cores

	boolean useSparseRepresentation = true;

	public static Normalizer nml = new LinearNormalizer(); // new
															// SumNormalizor();//
	public static String modelFile = "";

	protected RankerFactory rFact = new RankerFactory();
	protected MetricScorerFactory mFact = new MetricScorerFactory();
	//
	protected MetricScorer trainScorer = null;
	protected MetricScorer testScorer = null;
	protected RANKER_TYPE rankerType;

	private String workingDir;

	private String trainingFile;

	private String validationFile;
	private float evalRatingThresh;

	private int int_rankerType;



	public RankLibLearner(String workingDir, int nLearningThreads,
			int rankerType, float evalRatingThresh, int nIterations,
			int nTrees, int nTreeLeaves, float learningRate,
			int nRoundToStopEarly) {
		this.workingDir = workingDir;
		this.nThread = nLearningThreads;
		this.int_rankerType = rankerType;
		this.trainingFile = workingDir + "train";
		this.validationFile = workingDir + "validation";
		this.modelFile = workingDir + "bestModel";
		this.evalRatingThresh = evalRatingThresh;
		this.nIterations=nIterations;
		this.nTrees=nTrees;
		this.nTreeLeaves=nTreeLeaves;
		this.learningRate=learningRate;
		this.nRoundToStopEarly=nRoundToStopEarly;

		init();

		this.rankerType = map_rankers.get(int_rankerType);
		
		if(this.rankerType.compareTo(RANKER_TYPE.RANKBOOST)==0){
			RankBoost.nIteration=nIterations;
		}else if(this.rankerType.compareTo(RANKER_TYPE.ADARANK)==0){
			AdaRank.nIteration=nIterations;			
		}else if(this.rankerType.compareTo(RANKER_TYPE.COOR_ASCENT)==0){
			CoorAscent.nMaxIteration=nIterations;
			
		}else if(this.rankerType.compareTo(RANKER_TYPE.LAMBDAMART)==0){
			LambdaMART.nTrees=nIterations;
			LambdaMART.learningRate=learningRate;
			LambdaMART.nTreeLeaves=nTreeLeaves;
			LambdaMART.nRoundToStopEarly=nRoundToStopEarly;
			
		}else if(this.rankerType.compareTo(RANKER_TYPE.RANDOM_FOREST)==0){
			RFRanker.nBag=nIterations;
			RFRanker.nTreeLeaves=nTreeLeaves;
		}

	}

	private void init() {
		map_rankers.put(1, RANKER_TYPE.RANKBOOST);
		map_rankers.put(2, RANKER_TYPE.ADARANK);
		map_rankers.put(3, RANKER_TYPE.COOR_ASCENT);
		map_rankers.put(4, RANKER_TYPE.LAMBDAMART);
		map_rankers.put(5, RANKER_TYPE.RANDOM_FOREST);
	}

	public void train() {

		trainScorer = mFact.createScorer(trainMetric, this.evalRatingThresh);

		if (nThread == -1)
			nThread = Runtime.getRuntime().availableProcessors();
		MyThreadPool.init(nThread);

		List<RankList> train = new ArrayList<RankList>();
		List<RankList> validation = new ArrayList<RankList>();

		int[] features = loadData(trainingFile, validationFile, train,
				validation);

		RankerTrainer trainer = new RankerTrainer();
		Ranker ranker = trainer.train(rankerType, train, validation, features,
				trainScorer);

		if (modelFile.compareTo("") != 0) {
			System.out.println("");
			ranker.save(modelFile);
			System.out.println("Model saved to: " + modelFile);
		}
		MyThreadPool.getInstance().shutdown();
	}

	public int[] loadData(String trainFile, String validFile,
			List<RankList> train, List<RankList> validation) {

		// this.trainFile = trainFile;

		int[] features = null;

		List<RankList> tmp = readInput(trainFile);// read input

		train.addAll(tmp);

		tmp = readInput(validFile);// read input

		validation.addAll(tmp);

		features = FeatureManager.getFeatureFromSampleVector(train);

		System.out.println("train data points " + train.size());
		System.out.println("validation data points " + validation.size());

		return features;

	}

	public List<RankList> readInput(String inputFile, Set<Integer> listFeatures) {
		return FeatureManager.readInput(inputFile, false,
				useSparseRepresentation, listFeatures);
	}

	public List<RankList> readInput(String inputFile) {
		return FeatureManager.readInput(inputFile, false,
				useSparseRepresentation);
	}

	public List<RankList> readInput(String inputFile, int[] features) {
		Set<Integer> setFeatures = new HashSet();

		for (int i = 0; i < features.length; i++)
			setFeatures.add(features[i]);

		return FeatureManager.readInput(inputFile, false,
				useSparseRepresentation, setFeatures);

	}

	//
	public int[] readFeature(String featureDefFile) {
		FeatureManager fm = new FeatureManager();
		int[] features = fm.getFeatureIDFromFile(featureDefFile);
		return features;
	}

	public void normalize(List<RankList> samples) {
		for (int i = 0; i < samples.size(); i++)
			nml.normalize(samples.get(i));
	}

	public void normalize(List<RankList> samples, int[] fids) {
		for (int i = 0; i < samples.size(); i++)
			nml.normalize(samples.get(i), fids);
	}

}
