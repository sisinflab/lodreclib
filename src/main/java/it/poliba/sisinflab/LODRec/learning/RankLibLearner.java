package it.poliba.sisinflab.LODRec.learning;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import ciir.umass.edu.features.FeatureManager;
import ciir.umass.edu.learning.CoorAscent;
import ciir.umass.edu.learning.RANKER_TYPE;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.learning.Ranker;
import ciir.umass.edu.learning.RankerTrainer;
import ciir.umass.edu.learning.boosting.AdaRank;
import ciir.umass.edu.learning.boosting.RankBoost;
import ciir.umass.edu.learning.tree.LambdaMART;
import ciir.umass.edu.learning.tree.RFRanker;
import ciir.umass.edu.metric.MetricScorer;
import ciir.umass.edu.metric.MetricScorerFactory;
import ciir.umass.edu.utilities.MyThreadPool;
import gnu.trove.map.hash.TIntObjectHashMap;

public class RankLibLearner {
	
	private String modelFileName;
	
	private String trainingFile;
	private String validationFile;
	
	TIntObjectHashMap<RANKER_TYPE> map_rankers;
	String trainMetric;
	private float evalRatingThresh;
	protected RANKER_TYPE rankerType;
	
	int nThreads;
	boolean useSparseRepresentation = true;
	private boolean silent;
	
	private static Logger logger = LogManager.getLogger(RankLibLearner.class.getName());
	
	public RankLibLearner(String workingDir, int nThreads,
			int rankerType, float evalRatingThresh, String trainMetric, boolean silent,
			int nIteration, double tolerance, int nThreshold, int nTrees, int nTreeLeaves,
			float learningRate, int minLeafSupport, int maxSelCount, int nMaxIteration,
			int nRestart, boolean regularized, int nRoundToStopEarly, int nBag, 
			float featureSamplingRate, float subSamplingRate) {
		
		this.nThreads = nThreads;
		this.evalRatingThresh = evalRatingThresh;
		this.trainMetric = trainMetric;
		this.silent = silent;
		
		trainingFile = workingDir + "train";
		validationFile = workingDir + "validation";
		modelFileName = workingDir + "bestModel";
		
		init();		
		
		this.rankerType = map_rankers.get(rankerType);
		
		if(this.rankerType.compareTo(RANKER_TYPE.RANKBOOST)==0){
			
			if(nIteration >= 0)
				RankBoost.nIteration=nIteration;
			
			if(nThreshold >= 0) 
				RankBoost.nThreshold = nThreshold;
			
		}else if(this.rankerType.compareTo(RANKER_TYPE.ADARANK)==0){
			
			if(nIteration >= 0)
				AdaRank.nIteration=nIteration;	
			
			if(tolerance >= 0)
				AdaRank.tolerance = tolerance;
			
			AdaRank.maxSelCount = maxSelCount;
			
		}else if(this.rankerType.compareTo(RANKER_TYPE.COOR_ASCENT)==0){
			
			CoorAscent.nMaxIteration=nMaxIteration;
			
			if(tolerance >= 0)
				CoorAscent.tolerance = tolerance;
			
			CoorAscent.nRestart = nRestart;
			CoorAscent.regularized = regularized;
			
		}else if(this.rankerType.compareTo(RANKER_TYPE.LAMBDAMART)==0){
			
			if(nTrees >= 0)
				LambdaMART.nTrees=nTrees;
			
			if(nTreeLeaves >= 0)
				LambdaMART.nTreeLeaves=nTreeLeaves;
			
			if(nThreshold >= 0)
				LambdaMART.nThreshold = nThreshold;
			
			LambdaMART.learningRate=learningRate;
			LambdaMART.nRoundToStopEarly=nRoundToStopEarly;
			LambdaMART.minLeafSupport = minLeafSupport;
			
		}else if(this.rankerType.compareTo(RANKER_TYPE.RANDOM_FOREST)==0){
			
			RFRanker.nBag=nBag;
			
			if(nTrees >= 0)
				RFRanker.nTrees = nTrees;
			
			if(nTreeLeaves >= 0)
				RFRanker.nTreeLeaves=nTreeLeaves;
			
			if(nThreshold >= 0)
				RFRanker.nThreshold = nThreshold;
			
			RFRanker.featureSamplingRate = featureSamplingRate;
			RFRanker.learningRate = learningRate;
			RFRanker.minLeafSupport = minLeafSupport;
			RFRanker.subSamplingRate = subSamplingRate;
		}
		
	}
	
	public void init() {
		
		map_rankers = new TIntObjectHashMap<RANKER_TYPE>();
		
		map_rankers.put(1, RANKER_TYPE.RANKBOOST);
		map_rankers.put(2, RANKER_TYPE.ADARANK);
		map_rankers.put(3, RANKER_TYPE.COOR_ASCENT);
		map_rankers.put(4, RANKER_TYPE.LAMBDAMART);
		map_rankers.put(5, RANKER_TYPE.RANDOM_FOREST);
		
	}
	
	public void train() {

		MetricScorer trainScorer = 
				new MetricScorerFactory().createScorer(trainMetric, evalRatingThresh);

		MyThreadPool.init(nThreads);

		List<RankList> train = new ArrayList<RankList>();
		List<RankList> validation = new ArrayList<RankList>();
		
		int[] features = loadData(trainingFile, validationFile, train,
				validation);
		
		logger.info("Start learning process");
		
		Ranker ranker = new RankerTrainer().train(rankerType, train, validation, features, 
				trainScorer, silent);
		
		logger.info("ranker: " + ranker.name() + " - " + trainMetric + " = " 
				+ ranker.getScoreOnValidationData());
		
		ranker.save(modelFileName);
		
		logger.info("Model saved to: " + modelFileName);

		MyThreadPool.getInstance().shutdown();
		
	}
	
	public int[] loadData(String trainFile, String validFile, List<RankList> train, 
			List<RankList> validation) {

		int[] features = null;

		List<RankList> tmp = readInput(trainFile); // read input

		train.addAll(tmp);

		tmp = readInput(validFile); // read input

		validation.addAll(tmp);

		features = FeatureManager.getFeatureFromSampleVector(train);

		return features;

	}
	
	public List<RankList> readInput(String inputFile) {
		return FeatureManager.readInput(inputFile, false,
				useSparseRepresentation);
	}

}
