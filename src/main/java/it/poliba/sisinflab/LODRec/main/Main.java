package it.poliba.sisinflab.LODRec.main;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import it.poliba.sisinflab.LODRec.evaluation.Evaluator;
import it.poliba.sisinflab.LODRec.graphkernel.graphEmbedding.ItemGraphEmbedder;
import it.poliba.sisinflab.LODRec.graphkernel.heuristic.UserProfileSimilarityRecommender;
import it.poliba.sisinflab.LODRec.graphkernel.model.UserModelRecommender;
import it.poliba.sisinflab.LODRec.learning.LibLinearLearner;
import it.poliba.sisinflab.LODRec.learning.RankLibLearner;
import it.poliba.sisinflab.LODRec.recommender.Recommender;
import it.poliba.sisinflab.LODRec.sparqlDataExtractor.RDFTripleExtractor;
import it.poliba.sisinflab.LODRec.sprank.itemPathExtractor.ItemPathExtractor;
import it.poliba.sisinflab.LODRec.sprank.userPathExtractor.UserPathExtractor;
import it.poliba.sisinflab.LODRec.utils.PropertyFileReader;

public class Main {

	private static Logger logger = LogManager
			.getLogger(Main.class.getName());
	
	// default configuration file
	private static String configFile = "config.properties";
	
	
	/* ---------- RECOMMENDATION ALGORITHM ---------- */
	/* ---------------------------------------------- */
	
	static int recAlgorithm = 1; // SPrank
		
	
	/* -------------- GENERAL SETTINGS -------------- */
	/* ---------------------------------------------- */
	
	static boolean dataExtraction = false;
	static boolean itemGraphEmbedding = false;
	static boolean itemPathExtraction = false;
	static boolean userPathExtraction = false;
	static boolean computeRec = false;
	static boolean evaluation = false;
	static boolean evaluationDir = false;
	
	public static String workingDir = "./";
	public static int nThreads = Runtime.getRuntime().availableProcessors();
	public static boolean implicit = false;
	
	// input training file 
	private static String inputTrainRatingFile = "TrainRating";
	// input validation file 
	private static String inputValidationRatingFile = "ValidationRating";
	// input test file 
	private static String inputTestRatingFile = "TestRating";
	
	
	/* -------------- DATA EXTRACTION --------------- */
	/* ---------------------------------------------- */
	
	// set jenatdb=false to query remote endpoint - jenatdb=true to query local dataset
	private static boolean jenatdb = false;
	// sparql endpoint address
	private static String endpoint = "http://live.dbpedia.org/sparql";
	private static String graphURI = "http://dbpedia.org";
	// if jenatdb=true set local dataset parameters
	private static String tdbDirectory = "TDB";
	private static String localDatasetFile = "dump.nt";
	private static String inputItemURIsFile = "input_uri";
	// set inverseProps=true to consider directed property
	private static boolean inverseProps = true;
	// output file
	public static String itemsMetadataFile = "itemsMetadata";
	private static boolean outputTextFormat = true;
	private static boolean outputBinaryFormat = true;
	private static String propertiesFile = "props.xml";
	// set caching=true to enable caching (requires a lot of memory)
	private static boolean caching = false;
	// set append=true to continue a previous extraction
	private static boolean append = false;
	
	
	/* ------------- ITEM GRAPH EMBEDDING ----------- */
	/* ---------------------------------------------- */
	
	// 1 -> entity from text 
	// 2 -> branch from text 
	// 3 -> entity from binary  
	// 4 -> branch from binary
	private static int embeddingOption = 1;
	private static String entityMapFile = "_entity_mapping";
	private static String branchMapFile = "_branch_mapping";
	private static boolean addCollabFeatures = true;
	private static int maxBranchLength = 1;
	private static boolean minMaxNorm = false;
	private static int maxFreq= 1000000;
	private static int minFreq = 0;
	private static boolean idf = false;
	private static boolean lengthNorm = false;
	private static boolean onlyEntityBranches = true;
	private static String listAlphaVals = "";
	

	/* -------------- PATHS EXTRACTION -------------- */
	/* ---------------------------------------------- */
	
	// set selectTopPaths=true to consider only popular path
	private static boolean selectTopPaths = true;
	// number of top paths to consider in paths extraction
	private static int nTopPaths = 50;
	// number of items to consider in top paths computation
	private static int nItemTopPaths = 100;
	// number of items loaded in memory in paths extraction
	private static int itemsInMemory = 1000;
	// output format
	private static String pathsFile = workingDir + "itemPaths";
	private static boolean outputPathsTextFormat = false;
	private static boolean outputPathsBinaryFormat = true;
	// if you want to consider i1-i2 and i2-i1
	private static boolean computeInversePaths = false;
	
	
	/* ----------- USER PATHS EXTRACTION ------------ */
	/* ---------------------------------------------- */
	
	// percentage of paths loaded in memory in user paths extraction
	private static int pathsInMemory = 100;
	// percentage of rated user items to consider in user paths extraction
	private static int userItemsSampling = 100;
	// user rates threshold (>)
	private static float ratingThreshold = 3;
	private static boolean normalize = true;
	// split validation set for multi-threading recommendation
	private static boolean splitValidationSet = true;
	
	
	/* -------------- LEARNING ---------------- */
	/* ---------------------------------------------- */
	
	private static boolean rankLib = true;
	private static boolean libLinear = false;
	private static boolean silentLearning = true;
	
	// liblinear parameters
	// for multi-class classification
	//     0 -- L2-regularized logistic regression (primal)
	//     1 -- L2-regularized L2-loss support vector classification (dual)
	//     2 -- L2-regularized L2-loss support vector classification (primal)
	//     3 -- L2-regularized L1-loss support vector classification (dual)
	//     4 -- support vector classification by Crammer and Singer
	//     5 -- L1-regularized L2-loss support vector classification
	//     6 -- L1-regularized logistic regression
	//     7 -- L2-regularized logistic regression (dual)
	// for regression
	//    11 -- L2-regularized L2-loss support vector regression (primal)
	//    12 -- L2-regularized L2-loss support vector regression (dual)
	//    13 -- L2-regularized L1-loss support vector regression (dual)
	private static String listStrSolverType = "11";
	private static String listStrC = "1,10,100,1000";
	private static String listStrEps = "0.1";
	private static String listStrP = "0.1";
	private static int timesRealFb = 5;
	private static int nValidNegEx = 1000;
	private static int minTrainEx = 100;
	private static boolean addNegValidationEx = true;
	
	// ranklib parameters
	
	// 1->RANKER_TYPE.RANKBOOST
	// 2->RANKER_TYPE.ADARANK
	// 3->RANKER_TYPE.COOR_ASCENT
	// 4->RANKER_TYPE.LAMBDAMART
	// 5->RANKER_TYPE.RANDOM_FOREST
	private static int rankerType = 4;
	
	// ranker parameters
	
	private static int nIteration = -1;
	private static double tolerance = -1;
	private static int nThreshold = -1;
	private static int nTrees = -1;
	private static int nTreeLeaves = -1;
	private static float learningRate = (float) 0.1;
	private static int minLeafSupport = 1;
	
	// ADARANK parameters
	private static int maxSelCount = 5;
	
	// COOR_ASCENT parameters
	private static int nMaxIteration = 25;
	private static int nRestart = 5;
	private static boolean regularized = false;
	
	// LAMBDAMART parameters
	private static int nRoundToStopEarly = 100;
	
	// RANDOM_FOREST parameters
	private static int nBag = 300;
	private static float subSamplingRate = (float) 1;
	private static float featureSamplingRate = (float) 0.3;
	
	
	/* ---------------- EVALUATION ------------------ */
	/* ---------------------------------------------- */
	

	private static String evalMetric = "P@10";
	private static float evalRatingThresh = 4;
	private static float relUnknownItems = 3;
	private static float negRatingThresh = 2;
	private static String itemsMetadataEvalFile = "metadata_eval";
	private static String outputEvaluationFile = "evaluation";
	private static String recDirToEval = "";
	
	
	/* -------------- RECOMMENDATION ---------------- */
	/* ---------------------------------------------- */
	
	private static String recommendationsFile = "rec";
	private static int topN = 15000;	
	
	
	public static void loadParams() {

		try {
			PropertyConfigurator.configure("log4j.properties");
			
			Map<String, String> prop = PropertyFileReader
					.loadProperties(configFile);
			
			// general settings
			
			if(prop.containsKey("recommendationAlgorithm"))
				recAlgorithm = Integer.parseInt(prop.get("recommendationAlgorithm"));
			
			if(prop.containsKey("dataExtraction"))
				dataExtraction = Boolean.parseBoolean(prop.get("dataExtraction"));
			
			if(prop.containsKey("itemGraphEmbedding"))
				itemGraphEmbedding = Boolean.parseBoolean(prop.get("itemGraphEmbedding"));

			if(prop.containsKey("itemPathExtraction"))
				itemPathExtraction = Boolean.parseBoolean(prop.get("itemPathExtraction"));
			
			if(prop.containsKey("userPathExtraction"))
				userPathExtraction = Boolean.parseBoolean(prop.get("userPathExtraction"));
			
			if(prop.containsKey("computeRec"))
				computeRec = Boolean.parseBoolean(prop.get("computeRec"));
			
			if(prop.containsKey("evaluation"))
				evaluation = Boolean.parseBoolean(prop.get("evaluation"));
			
			if(prop.containsKey("evaluationDir"))
				evaluationDir = Boolean.parseBoolean(prop.get("evaluationDir"));
			
			if(prop.containsKey("workingDir"))
				workingDir = prop.get("workingDir");

			if(prop.containsKey("nThreads")) {
				if(Integer.parseInt(prop.get("nThreads")) > 0)
					nThreads = Integer.parseInt(prop.get("nThreads"));
			}
			
			if(prop.containsKey("implicit"))
				implicit = Boolean.parseBoolean(prop.get("implicit"));
			
			if(prop.containsKey("inputTrainRatingFile"))
				inputTrainRatingFile = prop.get("inputTrainRatingFile");
			
			if(prop.containsKey("inputValidationRatingFile"))
				inputValidationRatingFile = prop.get("inputValidationRatingFile");
			
			if(prop.containsKey("inputTestRatingFile"))
				inputTestRatingFile = prop.get("inputTestRatingFile");
			
			
			// data extraction
			
			if(prop.containsKey("itemsMetadataFile"))
				itemsMetadataFile = workingDir + prop.get("itemsMetadataFile");
			
			if(prop.containsKey("inputItemURIsFile"))
				inputItemURIsFile = prop.get("inputItemURIsFile");
			
			if(prop.containsKey("endpoint"))
				endpoint = prop.get("endpoint");
			
			if(prop.containsKey("graphURI"))
				graphURI = prop.get("graphURI");
			
			if(prop.containsKey("tdbDirectory"))
				tdbDirectory = workingDir + prop.get("tdbDirectory");
			
			if(prop.containsKey("localDatasetFile"))
				localDatasetFile = prop.get("localDatasetFile");
			
			if(prop.containsKey("inverseProps"))
				inverseProps = Boolean.parseBoolean(prop.get("inverseProps"));
			
			if(prop.containsKey("outputTextFormat"))
				outputTextFormat = Boolean.parseBoolean(prop.get("outputTextFormat"));
			
			if(prop.containsKey("outputBinaryFormat"))
				outputBinaryFormat = Boolean.parseBoolean(prop.get("outputBinaryFormat"));
			
			if(prop.containsKey("jenatdb"))
				jenatdb = Boolean.parseBoolean(prop.get("jenatdb"));
			
			if(prop.containsKey("propertiesFile"))
				propertiesFile = prop.get("propertiesFile");
			
			if(prop.containsKey("caching"))
				caching = Boolean.parseBoolean(prop.get("caching"));
			
			if(prop.containsKey("append"))
				append = Boolean.parseBoolean(prop.get("append"));
			
			// item graph embedding
			
			if(prop.containsKey("embeddingOption"))
				embeddingOption = Integer.parseInt(prop.get("embeddingOption"));
			
			if(prop.containsKey("entityMapFile"))
				entityMapFile = prop.get("entityMapFile");
			
			if(prop.containsKey("branchMapFile"))
				branchMapFile = prop.get("branchMapFile");
			
			if(prop.containsKey("addCollabFeatures"))
				addCollabFeatures = Boolean.parseBoolean(prop.get("addCollabFeatures"));
			
			if(prop.containsKey("maxBranchLength"))
				maxBranchLength = Integer.parseInt(prop.get("maxBranchLength"));
			
			if(prop.containsKey("minMaxNorm"))
				minMaxNorm = Boolean.parseBoolean(prop.get("minMaxNorm"));
			
			if(prop.containsKey("maxFreq"))
				maxFreq = Integer.parseInt(prop.get("maxFreq"));
			
			if(prop.containsKey("minFreq"))
				minFreq = Integer.parseInt(prop.get("minFreq"));
			
			if(prop.containsKey("idf"))
				idf = Boolean.parseBoolean(prop.get("idf"));
			
			if(prop.containsKey("lengthNorm"))
				lengthNorm = Boolean.parseBoolean(prop.get("lengthNorm"));
			
			if(prop.containsKey("onlyEntityBranches"))
				onlyEntityBranches = Boolean.parseBoolean(prop.get("onlyEntityBranches"));
			
			if(prop.containsKey("listAlphaVals"))
				listAlphaVals = prop.get("listAlphaVals");
			
			// path extraction
			
			if(prop.containsKey("computeTopPaths"))
				selectTopPaths = Boolean.parseBoolean(prop.get("computeTopPaths"));
			
			if(prop.containsKey("nTopPaths"))
				nTopPaths = Integer.parseInt(prop.get("nTopPaths"));
			
			if(prop.containsKey("nItemTopPaths"))
				nItemTopPaths = Integer.parseInt(prop.get("nItemTopPaths"));

			if(prop.containsKey("itemsInMemory"))
				itemsInMemory = Integer.parseInt(prop.get("itemsInMemory"));

			if(prop.containsKey("pathsFile"))
				pathsFile = workingDir + prop.get("pathsFile");
			
			if(prop.containsKey("outputPathsTextFormat"))
				outputPathsTextFormat = Boolean.parseBoolean(prop.get("outputPathsTextFormat"));
			
			if(prop.containsKey("outputPathsBinaryFormat"))
				outputPathsBinaryFormat = Boolean.parseBoolean(prop.get("outputPathsBinaryFormat"));
			
			if(prop.containsKey("computeInversePaths"))
				computeInversePaths = Boolean.parseBoolean(prop.get("computeInversePaths"));

			
			// user path extraction
			
			if(prop.containsKey("pathsInMemory"))
				pathsInMemory = Integer.parseInt(prop.get("pathsInMemory"));
			
			if(prop.containsKey("userItemsSampling"))
				userItemsSampling = Integer.parseInt(prop.get("userItemsSampling"));
			
			if(prop.containsKey("ratingThreshold"))
				ratingThreshold = Integer.parseInt(prop.get("ratingThreshold"));

			if(prop.containsKey("normalize"))
				normalize = Boolean.parseBoolean(prop.get("normalize"));
			
			if(prop.containsKey("splitValidationSet"))
				splitValidationSet = Boolean.parseBoolean(prop.get("splitValidationSet"));
			
			// learning
			
			if(prop.containsKey("libLinear"))
				libLinear = Boolean.parseBoolean(prop.get("libLinear"));
			
			if(prop.containsKey("rankLib"))
				rankLib = Boolean.parseBoolean(prop.get("rankLib"));
			
			if(prop.containsKey("silentLearning"))
				silentLearning = Boolean.parseBoolean(prop.get("silentLearning"));			
			
			// liblinear
			
			if(prop.containsKey("listSolverType"))
				listStrSolverType = prop.get("listSolverType");
			
			if(prop.containsKey("listC"))
				listStrC = prop.get("listC");
			
			if(prop.containsKey("listEps"))
				listStrEps = prop.get("listEps");
			
			if(prop.containsKey("listP"))
				listStrP = prop.get("listP");
			
			if(prop.containsKey("timesRealFb"))
				timesRealFb = Integer.parseInt(prop.get("timesRealFb"));
			
			if(prop.containsKey("nValidNegEx"))
				nValidNegEx = Integer.parseInt(prop.get("nValidNegEx"));
			
			if(prop.containsKey("minTrainEx"))
				minTrainEx = Integer.parseInt(prop.get("minTrainEx"));
			
			if(prop.containsKey("addNegValidationEx"))
				addNegValidationEx = Boolean.parseBoolean(prop.get("addNegValidationEx"));
			
			// ranklib
			
			if(prop.containsKey("rankerType"))
				rankerType = Integer.parseInt(prop.get("rankerType"));
			
			// ranker parameters
			
			if(prop.containsKey("nIteration"))
				nIteration = Integer.parseInt(prop.get("nIteration"));
			
			if(prop.containsKey("tolerance"))
				tolerance = Double.parseDouble(prop.get("tolerance"));
			
			if(prop.containsKey("nThreshold"))
				nThreshold = Integer.parseInt(prop.get("nThreshold"));
			
			if(prop.containsKey("nTrees"))
				nTrees = Integer.parseInt(prop.get("nTrees"));
			
			if(prop.containsKey("nTreeLeaves"))
				nTreeLeaves = Integer.parseInt(prop.get("nTreeLeaves"));
			
			if(prop.containsKey("learningRate"))
				learningRate = Float.parseFloat(prop.get("learningRate"));
			
			if(prop.containsKey("minLeafSupport"))
				minLeafSupport = Integer.parseInt(prop.get("minLeafSupport"));
			
			// ADARANK parameters
			
			if(prop.containsKey("maxSelCount"))
				maxSelCount = Integer.parseInt(prop.get("maxSelCount"));
			
			// COOR_ASCENT parameters
			
			if(prop.containsKey("nMaxIteration"))
				nMaxIteration = Integer.parseInt(prop.get("nMaxIteration"));
			
			if(prop.containsKey("nRestart"))
				nRestart = Integer.parseInt(prop.get("nRestart"));
			
			if(prop.containsKey("regularized"))
				regularized = Boolean.parseBoolean(prop.get("regularized"));
			
			// LAMBDAMART
			
			if(prop.containsKey("nRoundToStopEarly"))
				nRoundToStopEarly = Integer.parseInt(prop.get("nRoundToStopEarly"));
			
			// RANDOM_FOREST
			
			if(prop.containsKey("nBag"))
				nBag = Integer.parseInt(prop.get("nBag"));
			
			if(prop.containsKey("featureSamplingRate"))
				featureSamplingRate = Float.parseFloat(prop.get("featureSamplingRate"));
			
			if(prop.containsKey("subSamplingRate"))
				subSamplingRate = Float.parseFloat(prop.get("subSamplingRate"));
			
			
			// evaluation 
			
			if(prop.containsKey("evalMetric"))
				evalMetric = prop.get("evalMetric");
			
			if(prop.containsKey("evalRatingThresh"))
				evalRatingThresh = Float.parseFloat(prop.get("evalRatingThresh"));
			
			if(prop.containsKey("relUnknownItems"))
				relUnknownItems = Float.parseFloat(prop.get("relUnknownItems"));
			
			if(prop.containsKey("negRatingThresh"))
				negRatingThresh = Float.parseFloat(prop.get("negRatingThresh"));
			
			if(prop.containsKey("itemsMetadataEvalFile"))
				itemsMetadataEvalFile = prop.get("itemsMetadataEvalFile");
			
			if(prop.containsKey("outputEvaluationFile"))
				outputEvaluationFile = prop.get("outputEvaluationFile");
			
			if(prop.containsKey("recDirToEval"))
				recDirToEval = prop.get("recDirToEval");
			
			
			// recommendation
			
			if(prop.containsKey("recommendationsFile"))
				recommendationsFile = workingDir + prop.get("recommendationsFile");

			if(prop.containsKey("topN"))
				topN = Integer.parseInt(prop.get("topN"));
			

		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void loadCommandParams(String[] args){
		
		// load other command line params
		String arg = "";
		String val = "";
		for(int i = 0; i < args.length; i++){
					
			arg = args[i].split("=")[0];
			val = args[i].split("=")[1];
			
			
			// general settings
			if (arg.compareTo("recommendationAlgorithm") == 0) {
				recAlgorithm = Integer.parseInt(val);
			} else if(arg.compareTo("dataExtraction") == 0) {
				dataExtraction = Boolean.parseBoolean(val);
			} else if(arg.compareTo("computeRecommendation") == 0) {
				computeRec = Boolean.parseBoolean(val);
			} else if(arg.compareTo("itemPathExtraction") == 0) {
				itemPathExtraction = Boolean.parseBoolean(val);
			} else if(arg.compareTo("userPathExtraction") == 0) {
				userPathExtraction = Boolean.parseBoolean(val);
			} else if(arg.compareTo("evaluation") == 0) {
				evaluation = Boolean.parseBoolean(val);
			} else if(arg.compareTo("evaluationDir") == 0) {
				evaluationDir = Boolean.parseBoolean(val);
			}  else if(arg.compareTo("workingDir") == 0) {
				workingDir = val;
			} else if(arg.compareTo("nThreads") == 0) {
				if(Integer.parseInt(val) > 0)
					nThreads = Integer.parseInt(val);
			} else if(arg.compareTo("implicit") == 0) {
				implicit = Boolean.parseBoolean(val);
			} else if (arg.compareTo("inputTrainRatingFile") == 0) {
				inputTrainRatingFile = val;
			} else if (arg.compareTo("inputValidationRatingFile") == 0) {
				inputValidationRatingFile = val;
			} else if (arg.compareTo("inputTestRatingFile") == 0) {
				inputTestRatingFile = val;
			} // data extraction
			else if(arg.compareTo("itemsMetadataFile") == 0) {
				itemsMetadataFile = val;
			} else if(arg.compareTo("jenatdb") == 0) {
				jenatdb = Boolean.parseBoolean(val);
			} else if(arg.compareTo("localDatasetFile") == 0) {
				localDatasetFile = val;
			} else if(arg.compareTo("TDBdirectory") == 0) {
				tdbDirectory = val;
			} else if(arg.compareTo("endpoint") == 0) {
				endpoint = val;
			} else if(arg.compareTo("graphURI") == 0) {
				graphURI = val;
			} else if(arg.compareTo("inputURIfile") == 0) {
				inputItemURIsFile = val;
			} else if(arg.compareTo("propertiesFile") == 0) {
				propertiesFile = val;
			} else if(arg.compareTo("directed") == 0) {
				inverseProps = Boolean.parseBoolean(val);
			} else if(arg.compareTo("outputTextFormat") == 0) {
				outputTextFormat = Boolean.parseBoolean(val);
			} else if(arg.compareTo("outputBinaryFormat") == 0) {
				outputBinaryFormat = Boolean.parseBoolean(val);
			} else if (arg.compareTo("append") == 0) {
				append = Boolean.parseBoolean(val);
			} else if (arg.compareTo("caching") == 0) {
				caching = Boolean.parseBoolean(val);
			} // item graph embedding
			else if(arg.compareTo("embeddingOption") == 0) {
				embeddingOption = Integer.parseInt(val);
			} else if(arg.compareTo("entityMapFile") == 0) {
				entityMapFile = val;
			} else if(arg.compareTo("branchMapFile") == 0) {
				branchMapFile = val;
			} else if(arg.compareTo("addCollabFeatures") == 0) {
				addCollabFeatures = Boolean.parseBoolean(val);
			} else if(arg.compareTo("maxBranchLength") == 0) {
				maxBranchLength = Integer.parseInt(val);
			} else if(arg.compareTo("minMaxNorm") == 0) {
				minMaxNorm = Boolean.parseBoolean(val);
			} else if(arg.compareTo("maxFreq") == 0) {
				maxFreq = Integer.parseInt(val);
			} else if(arg.compareTo("minFreq") == 0) {
				minFreq = Integer.parseInt(val);
			} else if(arg.compareTo("idf") == 0) {
				idf = Boolean.parseBoolean(val);
			} else if(arg.compareTo("lengthNorm") == 0) {
				lengthNorm = Boolean.parseBoolean(val);
			} else if(arg.compareTo("onlyEntityBranches") == 0) {
				onlyEntityBranches = Boolean.parseBoolean(val);
			} else if(arg.compareTo("listAlphaVals") == 0) {
				listAlphaVals = val;
			} // paths extraction
			else if (arg.compareTo("itemsInMemory") == 0) {
				itemsInMemory = Integer.parseInt(val);
			} else if (arg.compareTo("outputPathsTextFormat") == 0) {
				outputPathsTextFormat = Boolean.parseBoolean(val);
			} else if (arg.compareTo("outputPathsBinaryFormat") == 0) {
				outputPathsBinaryFormat = Boolean.parseBoolean(val);
			} else if (arg.compareTo("pathsFile") == 0) {
				pathsFile = val;
			} else if (arg.compareTo("computeInversePaths") == 0) {
				computeInversePaths = Boolean.parseBoolean(val);
			} else if (arg.compareTo("computeTopPaths") == 0) {
				selectTopPaths = Boolean.parseBoolean(val);
			} else if (arg.compareTo("nTopPaths") == 0) {
				nTopPaths = Integer.parseInt(val);
			} else if (arg.compareTo("nItemTopPaths") == 0) {
				nItemTopPaths = Integer.parseInt(val);
			} // user paths extraction
			else if (arg.compareTo("pathsInMemory") == 0) {
				pathsInMemory = Integer.parseInt(val);
			} else if (arg.compareTo("userItemsSampling") == 0) {
				userItemsSampling = Integer.parseInt(val);
			} else if (arg.compareTo("ratingThreshold") == 0) {
				ratingThreshold = Integer.parseInt(val);
			} else if (arg.compareTo("normalize") == 0) {
				normalize = Boolean.parseBoolean(val);
			} else if (arg.compareTo("splitValidationSet") == 0) {
				splitValidationSet = Boolean.parseBoolean(val);
			} // learning
			else if(arg.compareTo("silentLearning") == 0) {
				silentLearning = Boolean.parseBoolean(val);
			} else if(arg.compareTo("libLinear") == 0) {
				libLinear = Boolean.parseBoolean(val);
			} else if(arg.compareTo("rankLib") == 0) {
				rankLib = Boolean.parseBoolean(val);
			} else if(arg.compareTo("rankerType") == 0) {
				rankerType = Integer.parseInt(val);
			} else if(arg.compareTo("nIterations") == 0) {
				nIteration = Integer.parseInt(val);
			} else if(arg.compareTo("tolerance") == 0) {
				tolerance = Double.parseDouble(val);
			} else if(arg.compareTo("nThreshold") == 0) {
				nThreshold = Integer.parseInt(val);
			} else if(arg.compareTo("nTrees") == 0) {
				nTrees = Integer.parseInt(val);
			} else if(arg.compareTo("nTreeLeaves") == 0) {
				nTreeLeaves = Integer.parseInt(val);
			} else if(arg.compareTo("learningRate") == 0) {
				learningRate = Float.parseFloat(val);
			} else if(arg.compareTo("minLeafSupport") == 0) {
				minLeafSupport = Integer.parseInt(val);
			} else if(arg.compareTo("maxSelCount") == 0) {
				maxSelCount = Integer.parseInt(val);
			} else if(arg.compareTo("nMaxIteration") == 0) {
				nMaxIteration = Integer.parseInt(val);
			} else if(arg.compareTo("nRestart") == 0) {
				nRestart = Integer.parseInt(val);
			} else if(arg.compareTo("regularized") == 0) {
				regularized = Boolean.parseBoolean(val);
			} else if(arg.compareTo("nRoundToStopEarly") == 0) {
				nRoundToStopEarly = Integer.parseInt(val);
			} else if(arg.compareTo("nBag") == 0) {
				nBag = Integer.parseInt(val);
			} else if(arg.compareTo("featureSamplingRate") == 0) {
				featureSamplingRate = Float.parseFloat(val);
			} else if(arg.compareTo("subSamplingRate") == 0) {
				subSamplingRate = Float.parseFloat(val);
			} else if (arg.compareTo("listSolverType") == 0) {
				listStrSolverType = val;
			} else if (arg.compareTo("listC") == 0) {
				listStrC = val;
			} else if (arg.compareTo("listEps") == 0) {
				listStrEps = val;
			} else if (arg.compareTo("listP") == 0) {
				listStrP = val;
			} else if(arg.compareTo("timesRealFb") == 0) {
				timesRealFb = Integer.parseInt(val);
			} else if(arg.compareTo("nValidNegEx") == 0) {
				nValidNegEx = Integer.parseInt(val);
			} else if(arg.compareTo("minTrainEx") == 0) {
				minTrainEx = Integer.parseInt(val);
			} else if(arg.compareTo("addNegValidationEx") == 0) {
				addNegValidationEx = Boolean.parseBoolean(val);
			} // evaluation
			else if(arg.compareTo("evalMetric") == 0) {
				evalMetric = val;
			} else if(arg.compareTo("evalRatingThresh") == 0) {
				evalRatingThresh = Float.parseFloat(val);
			} else if(arg.compareTo("relUnknownItems") == 0) {
				relUnknownItems = Float.parseFloat(val);
			} else if(arg.compareTo("negRatingThresh") == 0) {
				negRatingThresh = Float.parseFloat(val);
			} else if(arg.compareTo("itemsMetadataEvalFile") == 0) {
				itemsMetadataEvalFile = val;
			} else if(arg.compareTo("outputEvaluationFile") == 0) {
				outputEvaluationFile = val;
			} else if(arg.compareTo("recDirToEval") == 0) {
				recDirToEval = val;
			}
			// recommentation
			else if (arg.compareTo("recommendationsFile") == 0) {
				recommendationsFile = val;
			} else if (arg.compareTo("topN") == 0) {
				topN = Integer.parseInt(val);
			} 
		}
		
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("start");
		
		long start, stop;

		// read arguments
		for (int i = 0; i < args.length; i++) {
			if (args[i].contains("=")) {

				String arg = args[i].split("=")[0];
				String val = args[i].split("=")[1];

				if (arg.compareTo("configFile") == 0)
					configFile = val;

			}
		}

		// load parameters from config file
		loadParams();
		System.out.println("parameters loaded");
		// load parameters from command line
		loadCommandParams(args);

		if (dataExtraction) {

			RDFTripleExtractor m = new RDFTripleExtractor(workingDir,
					itemsMetadataFile, inputItemURIsFile, endpoint, graphURI,
					tdbDirectory, localDatasetFile, inverseProps, outputTextFormat,
					outputBinaryFormat, propertiesFile, caching, append, nThreads,
					jenatdb);

			start = System.currentTimeMillis();
			m.run();
			stop = System.currentTimeMillis();
			logger.info("Finished all threads. Data extraction terminated in [sec]: "
					+ ((stop - start) / 1000));

		}

		// ItemPathExtractor
		if (itemPathExtraction & recAlgorithm == 1) {

			ItemPathExtractor pe =
					new ItemPathExtractor(workingDir,
					itemsMetadataFile, pathsFile, computeInversePaths,
					selectTopPaths, nTopPaths, nItemTopPaths, outputPathsBinaryFormat,
					outputPathsTextFormat, inverseProps, itemsInMemory, nThreads);

			start = System.currentTimeMillis();
			pe.start();
			stop = System.currentTimeMillis();
			logger.info("Item paths extraction terminated in [sec]: "
					+ ((stop - start) / 1000));

		} else if (itemPathExtraction) {
			logger.info("the recommendation algorithm you set [" + recAlgorithm
					+ "] is not expected to compute this operation ");
		}

		// UserPathExtractor
		if (userPathExtraction & recAlgorithm == 1) {

			UserPathExtractor upe = new UserPathExtractor(workingDir,
					inputTrainRatingFile, inputValidationRatingFile, normalize, 
					pathsFile, itemsMetadataFile, pathsInMemory, userItemsSampling, 
					ratingThreshold, nThreads, splitValidationSet);

			start = System.currentTimeMillis();

			upe.start();

			stop = System.currentTimeMillis();
			logger.info("User path extraction terminated in [sec]: "
					+ ((stop - start) / 1000));

		} else if (userPathExtraction) {
			logger.info("the recommendation algorithm you set [" + recAlgorithm
					+ "] is not expected to compute this operation ");
		}
		
		// itemGraphEmbedding

		if (itemGraphEmbedding & (recAlgorithm == 2 || recAlgorithm == 3)) {

			ItemGraphEmbedder mapper = new ItemGraphEmbedder(workingDir,
					itemsMetadataFile, entityMapFile, branchMapFile,
					embeddingOption, inputTrainRatingFile, maxBranchLength,
					addCollabFeatures, onlyEntityBranches, minMaxNorm, idf,
					maxFreq, minFreq, lengthNorm, listAlphaVals);
			start = System.currentTimeMillis();

			mapper.computeMapping();

			stop = System.currentTimeMillis();
			logger.info("item graph embedding terminated in [sec]: "
							+ ((stop - start) / 1000));

		} else if (itemGraphEmbedding) {
			logger.info("the reccommendation algorithm you set [" + recAlgorithm
							+ "] does not support graph embedding ");
		}
		
		// Compute recommendation with SPrank
		if(computeRec & (recAlgorithm == 1)) {
			
			// start learning
			
			start = System.currentTimeMillis();
			
			if(libLinear) {
			
				LibLinearLearner l = new LibLinearLearner(workingDir, 
						inputValidationRatingFile, evalRatingThresh, silentLearning,
						listStrSolverType, listStrC, listStrEps, listStrP, evalMetric,
						relUnknownItems);
	
				l.train();
			
			} else if(rankLib) {
				
				RankLibLearner l = new RankLibLearner(workingDir,
						nThreads, rankerType, evalRatingThresh, evalMetric, 
						silentLearning, nIteration, tolerance, nThreshold, nTrees,
						nTreeLeaves, learningRate, minLeafSupport, maxSelCount,
						nMaxIteration, nRestart, regularized, nRoundToStopEarly,
						nBag, featureSamplingRate, subSamplingRate);
				l.train();
				
			}
			
			stop = System.currentTimeMillis();
			logger.info("Learning terminated in [sec]: "
					+ ((stop - start) / 1000));
			
			// start computing recommendations
			
			start = System.currentTimeMillis();

			Recommender p = new Recommender(workingDir, recommendationsFile, topN, 
					nThreads, libLinear, rankLib);
			p.computeRec();
			
			stop = System.currentTimeMillis();
			logger.info("Recommendation comp. terminated in [sec]: "
					+ ((stop - start) / 1000));
			
		}
		
		if (computeRec & (recAlgorithm == 2)) {

			start = System.currentTimeMillis();
			UserModelRecommender rec = new UserModelRecommender(topN, nThreads,
					recommendationsFile, itemsMetadataFile, embeddingOption,
					entityMapFile, branchMapFile, inputTrainRatingFile,
					inputValidationRatingFile, implicit, listStrSolverType,
					listStrC, listStrEps, evalRatingThresh, relUnknownItems,
					negRatingThresh, timesRealFb, nValidNegEx, minTrainEx, 
					addNegValidationEx, evalMetric);

			rec.exec();
			stop = System.currentTimeMillis();
			logger.info("Single user recommendation comp. terminated in [sec]: "
					+ ((stop - start) / 1000));
			
		}
		
		if (computeRec & (recAlgorithm == 3)) {

			start = System.currentTimeMillis();
			UserProfileSimilarityRecommender rec = new UserProfileSimilarityRecommender(
					topN, recommendationsFile, itemsMetadataFile, embeddingOption,
					entityMapFile, branchMapFile, inputTrainRatingFile, implicit,
					evalRatingThresh, nThreads);

			rec.exec();
			stop = System.currentTimeMillis();
			logger.info("Single user ProfileSim recommendation comp. terminated in [sec]: "
					+ ((stop - start) / 1000));
		
		}
		
		if (evaluation) {

			start = System.currentTimeMillis();
			Evaluator ev = new Evaluator(workingDir, outputEvaluationFile, itemsMetadataFile,
					itemsMetadataEvalFile, inputTrainRatingFile, inputTestRatingFile,
					evalRatingThresh, negRatingThresh, relUnknownItems, topN);

			ev.eval(recommendationsFile, outputEvaluationFile);

			stop = System.currentTimeMillis();
			logger.info("Evaluation terminated in [sec]: "
					+ ((stop - start) / 1000));

		}
		//
		if (evaluationDir) {

			start = System.currentTimeMillis();
			Evaluator ev = new Evaluator(workingDir,outputEvaluationFile, itemsMetadataFile,
					itemsMetadataEvalFile, inputTrainRatingFile, inputTestRatingFile,
					evalRatingThresh, negRatingThresh, relUnknownItems, topN);

			ev.evalDir(recDirToEval);

			stop = System.currentTimeMillis();
			logger.info("Evaluation terminated in [sec]: "
					+ ((stop - start) / 1000));

		}

	}

}
