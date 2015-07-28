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
import it.poliba.sisinflab.LODRec.recommender.LibLinearLearner;
import it.poliba.sisinflab.LODRec.recommender.RankLibLearner;
import it.poliba.sisinflab.LODRec.recommender.Recommender;
import it.poliba.sisinflab.LODRec.sparqlDataExtractor.RDFTripleExtractor;
import it.poliba.sisinflab.LODRec.sprank.itemPathExtractor.ItemPathExtractor;
import it.poliba.sisinflab.LODRec.sprank.userPathExtractor.UserPathExtractor;
import it.poliba.sisinflab.LODRec.utils.PropertyFileReader;

public class Main {

	public static void main(String[] args) {

		long start, stop;

		// load config file
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
		
		// load parameters from command line
		loadCommandParams(args);
		

		if (dataExtraction) {
			RDFTripleExtractor m = new RDFTripleExtractor(workingDir,
					itemMetadataFile, inputItemURIsFile, endpoint, graphURI,
					tdbDirectory, datasetFile, inverseProps, outputTextFormat,
					outputBinaryFormat, propsFile, caching, append, nThreads,
					jenatdb);

			start = System.currentTimeMillis();
			m.run();
			stop = System.currentTimeMillis();
			logger.info("Finished all threads. Data extraction terminated in [sec]: "
					+ ((stop - start) / 1000));
		}

		// ItemPathExtractor
		if (itemPathExtraction & rec_algorithm == 1) {

			ItemPathExtractor pe = new ItemPathExtractor(workingDir,
					itemMetadataFile, pathFile, computeInversePaths,
					selectTopPaths, numTopPaths, numItemTopPaths, outputPathBinaryFormat,
					outputPathTextFormat, inverseProps, itemsInMemory, nThreads);

			start = System.currentTimeMillis();
			pe.start();
			stop = System.currentTimeMillis();
			logger.info("Item paths extraction terminated in [sec]: "
					+ ((stop - start) / 1000));

		} else if (itemPathExtraction) {
			System.out.println("the recc algorithm you set [" + rec_algorithm
					+ "] is not expected to compute this operation ");
		}

		// UserPathExtractor

		if (userPathExtraction & rec_algorithm == 1) {

			UserPathExtractor upe = new UserPathExtractor(workingDir,
					trainRatingFile, validationRatingFile, testRatingFile,
					normalize, pathFile, itemMetadataFile, paths_in_memory,
					user_items_sampling, ratingThreshold, nThreads);

			start = System.currentTimeMillis();

			upe.start();

			stop = System.currentTimeMillis();
			logger.info("User path extraction terminated in [sec]: "
					+ ((stop - start) / 1000));

		} else if (userPathExtraction) {
			System.out.println("the recc algorithm you set [" + rec_algorithm
					+ "] is not expected to compute this operation ");
		}

		// itemGraphEmbedding

		if (itemGraphEmbedding & (rec_algorithm == 2 || rec_algorithm == 3)) {

			ItemGraphEmbedder mapper = new ItemGraphEmbedder(workingDir,
					itemMetadataFile, entityMapFile, branchMapFile,
					embeddingOption, trainRatingFile, max_branch_length,
					addCollabFeatures, onlyEntityBranches, minmax_norm, idf,
					max_f, min_f, length_normaliz, listAlphaVals);
			start = System.currentTimeMillis();

			mapper.computeMapping();

			stop = System.currentTimeMillis();
			logger.info("item graph embedding terminated in [sec]: "
					+ ((stop - start) / 1000));

		} else if (itemGraphEmbedding) {
			System.out.println("the recc algorithm you set [" + rec_algorithm
					+ "] does not support graph embedding ");
		}

		if (cmpRec & (rec_algorithm == 1)) {

			start = System.currentTimeMillis();
			if (libLinear) {

				LibLinearLearner l = new LibLinearLearner(workingDir,
						trainRatingFile, validationRatingFile,
						evalRatingThresh, relUnknownItems, implicit,
						silentLearning, listStrSolverType, listStrC,
						listStrEps, listStrP);

				l.train();

			} else if (rankLib) {
				
				
				RankLibLearner l = new RankLibLearner(workingDir,
						nLearningThreads, rankerType, evalRatingThresh, nIterations,nTrees,nTreeLeaves,learningRate,nRoundToStopEarly);
				l.train();
				
				
			}
			stop = System.currentTimeMillis();
			logger.info("Learning terminated in [sec]: "
					+ ((stop - start) / 1000));

			start = System.currentTimeMillis();

			Recommender p = new Recommender(workingDir, recOutputFile, topN,
					implicit, testRatingFile, libLinear, rankLib);

			p.computeRecc();
			stop = System.currentTimeMillis();
			logger.info("Recommendation Comp. terminated in [sec]: "
					+ ((stop - start) / 1000));

		}

		if (cmpRec & (rec_algorithm == 2)) {

			start = System.currentTimeMillis();
			UserModelRecommender rec = new UserModelRecommender(topN, nThreads,
					recOutputFile, itemMetadataFile, embeddingOption,
					entityMapFile, branchMapFile, trainRatingFile,
					validationRatingFile, implicit, listStrSolverType,
					listStrC, listStrEps, evalRatingThresh, relUnknownItems,
					negRatingThresh, timesRealFb, nValidNegEx, minTrainEx, addNegValidationEx);

			rec.exec();
			stop = System.currentTimeMillis();
			logger.info("Single User Recc terminated in [sec]: "
					+ ((stop - start) / 1000));

		}

		if (cmpRec & (rec_algorithm == 3)) {

			start = System.currentTimeMillis();
			UserProfileSimilarityRecommender rec = new UserProfileSimilarityRecommender(
					topN, recOutputFile, itemMetadataFile, embeddingOption,
					entityMapFile, branchMapFile, trainRatingFile, implicit,
					evalRatingThresh, nThreads);

			rec.exec();
			stop = System.currentTimeMillis();
			logger.info("Single User ProfileSim Recc terminated in [sec]: "
					+ ((stop - start) / 1000));
		}

		// Evaluator

		if (eval) {

			start = System.currentTimeMillis();
			Evaluator ev = new Evaluator(workingDir, itemMetadataFile,
					itemMetadataEvalFile, trainRatingFile, testRatingFile,
					evalRatingThresh, negRatingThresh, relUnknownItems, topN);

			ev.eval(recOutputFile, outputEvaluationFile);

			stop = System.currentTimeMillis();
			logger.info("Evaluation terminated in [sec]: "
					+ ((stop - start) / 1000));

		}
		//
		if (evalDir) {

			start = System.currentTimeMillis();
			Evaluator ev = new Evaluator(workingDir, itemMetadataFile,
					itemMetadataEvalFile, trainRatingFile, testRatingFile,
					evalRatingThresh, negRatingThresh, relUnknownItems, topN);

			ev.evalDir(reccDirToEval);

			stop = System.currentTimeMillis();
			logger.info("Evaluation terminated in [sec]: "
					+ ((stop - start) / 1000));

		}

	}

	// default configuration file
	private static String configFile = "config.properties";
	static Logger logger = LogManager.getLogger(Main.class.getName());
	public static String workingDir = "./";
	static boolean dataExtraction = false;
	// sprank
	static boolean itemPathExtraction = false;
	static boolean userPathExtraction = false;

	// graph kernel
	static boolean itemGraphEmbedding = false;
	static boolean eval = false;
	static boolean cmpRec = false;

	private static String trainRatingFile = "TrainSet";
	private static String validationRatingFile = "ValidationSet";
	private static String testRatingFile = "TestTrainSet";
	private static String outputEvaluationFile = workingDir + "evaluation";

	// 1->sprank
	// 2->graph kernel model-based
	// 3->graph kernel heuristic
	private static int rec_algorithm = 1;

	private static int nThreads = 10;
	private static int nLearningThreads = 5;

	private static boolean implicit = false;
	
	// input/output
	private static String itemMetadataFile = workingDir + "item_metadata";
	private static String itemMetadataEvalFile = "metadata_eval";
	private static String inputItemURIsFile = "input_uri";
	private static String pathFile = workingDir + "item_path";
	private static String recOutputFile = workingDir + "rec";
		
	// sparql endpoint address
	private static String endpoint = "http://live.dbpedia.org/sparql";
	private static String graphURI = "http://dbpedia.org";
	// set jenatdb=false to query remote endpoint - jenatdb=true to query local dataset
	private static boolean jenatdb = false;
	//i f jenatdb=true set local dataset parameters
	private static String tdbDirectory = "TDB";
	private static String datasetFile = "dump.nt";
	// set inverseProps=true to consider directed property
	private static boolean inverseProps = true;
	private static boolean outputTextFormat = true;
	private static boolean outputBinaryFormat = true;
	private static String propsFile = "props.xml";
	// set caching=true to enable caching in the sparql extraction (requires a lot of memory)
	private static boolean caching = false;
	// set append=true if you want continue a previous sparql extraction
	private static boolean append = false;
	// if you want consider only popular path
	private static boolean selectTopPaths = true;
	// number of top paths to consider in paths extraction
	private static int numTopPaths = 50;
	// number of items to consider for top paths computation
	private static int numItemTopPaths = 100;
	// number of items loaded in memory in paths extraction
	private static int itemsInMemory = 1000;
	private static boolean outputPathTextFormat = false;
	private static boolean outputPathBinaryFormat = true;
	// if you want to consider i1-i2 and i2-i1
	private static boolean computeInversePaths = false;
	private static boolean normalize = true;
	// percentage of paths loaded in memory in user paths extraction
	private static int paths_in_memory = 100;
	// percentage of rated user items to consider in user paths extraction
	private static int user_items_sampling = 100;
	// user rates threshold (>)
	private static float ratingThreshold = 3;
	
	// item graph embedding
	
	// 1->entity from text 2->branch from text 3->entity from binary  4-> branch from binary
	private static int embeddingOption = 1;
	private static String entityMapFile = "_entity_mapping";
	private static String branchMapFile = "_branch_mapping";
	private static boolean addCollabFeatures = true;
	private static int max_branch_length = 1;
	private static boolean minmax_norm = false;
	private static int max_f = 1000000;
	private static int min_f = 0;
	private static boolean idf = false;
	private static boolean length_normaliz = false;
	private static boolean onlyEntityBranches = true;
	private static String listAlphaVals = "";
	
	
	// learning
	
	private static boolean rankLib = true;
	private static boolean libLinear = false;
	
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
	private static int topN = 15000;
	private static boolean silentLearning = true;
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
	private static int nIterations = 500;
	private static int nTrees = 100;
	private static int nTreeLeaves = 250;
	private static float learningRate = (float) 0.01;
	private static int nRoundToStopEarly = 100;
	
	
	// evaluation
	
	private static float evalRatingThresh = 4;
	private static float negRatingThresh = 2;
	private static float relUnknownItems = 3;
	private static boolean evalDir = false;
	private static String reccDirToEval = "";
	
	public static void loadParams() {

		try {
			PropertyConfigurator.configure("log4j.properties");
			
			Map<String, String> prop = PropertyFileReader
					.loadProperties(configFile);

			// working directory where all files produced by the app are located
			if(prop.containsKey(PropertyFileReader.keywords.workingDir.toString()))
				workingDir = prop.get(PropertyFileReader.keywords.workingDir.toString());

			// performs data extraction - extract data using sparql
			if(prop.containsKey(PropertyFileReader.keywords.dataExtraction.toString()))
				dataExtraction = Boolean.parseBoolean(prop.get(PropertyFileReader.keywords.dataExtraction
						.toString()));

			// sprank ---- performs item-item path computation
			if(prop.containsKey(PropertyFileReader.keywords.itemPathExtraction.toString()))
				itemPathExtraction = Boolean.parseBoolean(prop.get(PropertyFileReader.keywords.itemPathExtraction
						.toString()));
			
			// sprank ---- performs user-item path computation, construct train,
			// validation and test sets
			if(prop.containsKey(PropertyFileReader.keywords.userPathExtraction.toString()))
				userPathExtraction = Boolean.parseBoolean(prop.get(PropertyFileReader.keywords.userPathExtraction
						.toString()));

			// hashrec ---- computes the feature vector representation for each
			// item considering the item metadata file
			if(prop.containsKey(PropertyFileReader.keywords.itemGraphEmbedding.toString()))
				itemGraphEmbedding = Boolean.parseBoolean(prop.get(PropertyFileReader.keywords.itemGraphEmbedding
						.toString()));
			
			if(prop.containsKey(PropertyFileReader.keywords.cmpRec.toString()))
				cmpRec = Boolean.parseBoolean(prop.get(PropertyFileReader.keywords.cmpRec.toString()));

			// // evaluates the recommendation lists present in the out dir
			if(prop.containsKey(PropertyFileReader.keywords.eval.toString()))
				eval = Boolean.parseBoolean(prop.get(PropertyFileReader.keywords.eval.toString()));

			if(prop.containsKey(PropertyFileReader.keywords.evalDir.toString()))
				evalDir = Boolean.parseBoolean(prop.get(PropertyFileReader.keywords.evalDir.toString()));

			// file containing the train ratings
			if(prop.containsKey(PropertyFileReader.keywords.trainRatingsFile.toString()))
				trainRatingFile = prop.get(PropertyFileReader.keywords.trainRatingsFile.toString());
			
			// file containing the validation ratings
			if(prop.containsKey(PropertyFileReader.keywords.validationRatingsFile.toString()))
				validationRatingFile = prop.get(PropertyFileReader.keywords.validationRatingsFile
						.toString());
			
			// file containing the test ratings
			if(prop.containsKey(PropertyFileReader.keywords.testRatingsFile.toString()))
				testRatingFile = prop.get(PropertyFileReader.keywords.testRatingsFile.toString());

			// recommendation algorithm to run
			if(prop.containsKey(PropertyFileReader.keywords.rec_alg.toString()))
				rec_algorithm = Integer.parseInt(prop.get(PropertyFileReader.keywords.rec_alg.toString()));

			if(prop.containsKey(PropertyFileReader.keywords.rankLib.toString()))
				rankLib = Boolean.parseBoolean(prop.get(PropertyFileReader.keywords.rankLib.toString()));

			if(prop.containsKey(PropertyFileReader.keywords.libLinear.toString()))
				libLinear = Boolean.parseBoolean(prop.get(PropertyFileReader.keywords.libLinear.toString()));

			// n. of threads
			if(prop.containsKey(PropertyFileReader.keywords.nThreads.toString()))
				nThreads = Integer.parseInt(prop.get(PropertyFileReader.keywords.nThreads.toString()));

			// n. of threads using for the learning phase
			if(prop.containsKey(PropertyFileReader.keywords.nLearningThreads.toString()))
				nLearningThreads = Integer.parseInt(prop.get(PropertyFileReader.keywords.nLearningThreads
						.toString()));

			// treat ratings as positive observation. randomly selected
			// negative/unknown items are added to the training set
			if(prop.containsKey(PropertyFileReader.keywords.implicit.toString()))
				implicit = Boolean.parseBoolean(prop.get(PropertyFileReader.keywords.implicit.toString()));

			// final recommendation file generated by the recommender
			if(prop.containsKey(PropertyFileReader.keywords.recFile.toString()))
				recOutputFile = workingDir	+ prop.get(PropertyFileReader.keywords.recFile.toString());

			// --------------------------- ITEM DATA EXTRACTION PARAMS
			// --------------------------------------
			if(prop.containsKey(PropertyFileReader.extractionKeywords.itemContentFile.toString()))
				itemMetadataFile = workingDir + 
					prop.get(PropertyFileReader.extractionKeywords.itemContentFile.toString());
			
			if(prop.containsKey(PropertyFileReader.extractionKeywords.inputURIfile.toString()))
				inputItemURIsFile = prop.get(PropertyFileReader.extractionKeywords.inputURIfile
						.toString());
			
			if(prop.containsKey(PropertyFileReader.extractionKeywords.endpoint.toString()))
				endpoint = prop.get(PropertyFileReader.extractionKeywords.endpoint.toString());
			
			if(prop.containsKey(PropertyFileReader.extractionKeywords.graphURI.toString()))
				graphURI = prop.get(PropertyFileReader.extractionKeywords.graphURI.toString());
			
			if(prop.containsKey(PropertyFileReader.extractionKeywords.TDBdirectory.toString()))
				tdbDirectory = workingDir + 
					prop.get(PropertyFileReader.extractionKeywords.TDBdirectory.toString());
			
			if(prop.containsKey(PropertyFileReader.extractionKeywords.datasetFile.toString()))
				datasetFile = prop.get(PropertyFileReader.extractionKeywords.datasetFile
						.toString());
			
			if(prop.containsKey(PropertyFileReader.extractionKeywords.directed.toString()))
				inverseProps = Boolean.parseBoolean(prop.get(PropertyFileReader.extractionKeywords.directed
						.toString()));
			
			if(prop.containsKey(PropertyFileReader.extractionKeywords.outputTextFormat.toString()))
				outputTextFormat = Boolean.parseBoolean(prop.get(PropertyFileReader.extractionKeywords.outputTextFormat
						.toString()));
			
			if(prop.containsKey(PropertyFileReader.extractionKeywords.outputBinaryFormat.toString()))
				outputBinaryFormat = Boolean.parseBoolean(prop
							.get(PropertyFileReader.extractionKeywords.outputBinaryFormat.toString()));
			
			if(prop.containsKey(PropertyFileReader.extractionKeywords.jenatdb.toString()))
				jenatdb = Boolean.parseBoolean(prop.get(PropertyFileReader.extractionKeywords.jenatdb
						.toString()));
			
			if(prop.containsKey(PropertyFileReader.extractionKeywords.propsFile.toString()))
				propsFile = prop.get(PropertyFileReader.extractionKeywords.propsFile.toString());
			
			if(prop.containsKey(PropertyFileReader.extractionKeywords.caching.toString()))
				caching = Boolean.parseBoolean(prop.get(PropertyFileReader.extractionKeywords.caching
						.toString()));
			
			if(prop.containsKey(PropertyFileReader.extractionKeywords.append.toString()))
				append = Boolean.parseBoolean(prop.get(PropertyFileReader.extractionKeywords.append
						.toString()));

			// --------------------------- SPRANK PATH EXTRACTION PARAMS
			// --------------------------------------

			if(prop.containsKey(PropertyFileReader.pathKeywords.computeTopPaths.toString()))
				selectTopPaths = Boolean.parseBoolean(prop.get(PropertyFileReader.pathKeywords.computeTopPaths
						.toString()));
			
			if(prop.containsKey(PropertyFileReader.pathKeywords.numTopPaths.toString()))
				numTopPaths = Integer.parseInt(prop.get(PropertyFileReader.pathKeywords.numTopPaths
						.toString()));
			
			if(prop.containsKey(PropertyFileReader.pathKeywords.numItemTopPaths.toString()))
				numItemTopPaths = Integer.parseInt(prop.get(PropertyFileReader.pathKeywords.numItemTopPaths
						.toString()));

			if(prop.containsKey(PropertyFileReader.pathKeywords.itemsInMemory.toString()))
				itemsInMemory = Integer.parseInt(prop.get(PropertyFileReader.pathKeywords.itemsInMemory
						.toString()));

			if(prop.containsKey(PropertyFileReader.pathKeywords.outputPathFile.toString()))
				pathFile = workingDir + prop.get(PropertyFileReader.pathKeywords.outputPathFile
						.toString());
			
			if(prop.containsKey(PropertyFileReader.pathKeywords.outputExtractionTextFormat.toString()))
				outputPathTextFormat = Boolean.parseBoolean(
						prop.get(PropertyFileReader.pathKeywords.outputExtractionTextFormat.toString()));
			
			if(prop.containsKey(PropertyFileReader.pathKeywords.outputExtractionBinaryFormat.toString()))
				outputPathBinaryFormat = Boolean.parseBoolean(
						prop.get(PropertyFileReader.pathKeywords.outputExtractionBinaryFormat
								.toString()));
			
			if(prop.containsKey(PropertyFileReader.pathKeywords.computeInversePaths.toString()))
				computeInversePaths = Boolean.parseBoolean(
						prop.get(PropertyFileReader.pathKeywords.computeInversePaths.toString()));

			if(prop.containsKey(PropertyFileReader.pathKeywords.normalize.toString()))
				normalize = Boolean.parseBoolean(prop.get(PropertyFileReader.pathKeywords.normalize.toString()));
			
			if(prop.containsKey(PropertyFileReader.pathKeywords.pathsInMemory.toString()))
				paths_in_memory = Integer.parseInt(prop.get(PropertyFileReader.pathKeywords.pathsInMemory
						.toString()));
			
			if(prop.containsKey(PropertyFileReader.pathKeywords.userItemsSampling.toString()))
				user_items_sampling = Integer.parseInt(
						prop.get(PropertyFileReader.pathKeywords.userItemsSampling.toString()));
			
			if(prop.containsKey(PropertyFileReader.pathKeywords.ratingThreshold.toString()))
				ratingThreshold = Integer.parseInt(prop.get(PropertyFileReader.pathKeywords.ratingThreshold
						.toString()));

			if(prop.containsKey(PropertyFileReader.graphEMbKeywords.entityBasedEmbFile.toString()))
				entityMapFile = prop.get(PropertyFileReader.graphEMbKeywords.entityBasedEmbFile
						.toString());
			
			if(prop.containsKey(PropertyFileReader.graphEMbKeywords.branchBasedEmbFile.toString()))
				branchMapFile = prop.get(PropertyFileReader.graphEMbKeywords.branchBasedEmbFile
						.toString());
			
			if(prop.containsKey(PropertyFileReader.graphEMbKeywords.embeddingOption.toString()))
				embeddingOption = Integer.parseInt(
						prop.get(PropertyFileReader.graphEMbKeywords.embeddingOption.toString()));
			
			if(prop.containsKey(PropertyFileReader.graphEMbKeywords.listAlphaVals.toString()))
				listAlphaVals = prop.get(PropertyFileReader.graphEMbKeywords.listAlphaVals
						.toString());
			
			if(prop.containsKey(PropertyFileReader.graphEMbKeywords.min_freq.toString()))
				min_f = Integer.parseInt(prop.get(PropertyFileReader.graphEMbKeywords.min_freq
						.toString()));
			
			if(prop.containsKey(PropertyFileReader.graphEMbKeywords.max_freq.toString()))
				max_f = Integer.parseInt(prop.get(PropertyFileReader.graphEMbKeywords.max_freq
						.toString()));
			
			if(prop.containsKey(PropertyFileReader.graphEMbKeywords.minmax_norm.toString()))
				minmax_norm = Boolean.parseBoolean(prop.get(PropertyFileReader.graphEMbKeywords.minmax_norm
						.toString()));
			
			if(prop.containsKey(PropertyFileReader.graphEMbKeywords.idf.toString()))
				idf = Boolean.parseBoolean(prop.get(PropertyFileReader.graphEMbKeywords.idf.toString()));

			if(prop.containsKey(PropertyFileReader.graphEMbKeywords.length_normaliz.toString()))
				length_normaliz = Boolean.parseBoolean(
						prop.get(PropertyFileReader.graphEMbKeywords.length_normaliz.toString()));

			if(prop.containsKey(PropertyFileReader.graphEMbKeywords.addCollabFeatures.toString()))
				addCollabFeatures = Boolean.parseBoolean(
						prop.get(PropertyFileReader.graphEMbKeywords.addCollabFeatures.toString()));

			if(prop.containsKey(PropertyFileReader.graphEMbKeywords.max_branch_length.toString()))
				max_branch_length = Integer.parseInt(
						prop.get(PropertyFileReader.graphEMbKeywords.max_branch_length.toString()));
			
			if(prop.containsKey(PropertyFileReader.graphEMbKeywords.onlyEntityBranches.toString()))
				onlyEntityBranches = Boolean.parseBoolean(
						prop.get(PropertyFileReader.graphEMbKeywords.onlyEntityBranches.toString()));

			// ranklib learning

			if(prop.containsKey(PropertyFileReader.learningKeywords.rankerType.toString()))
				rankerType = Integer.parseInt(prop.get(PropertyFileReader.learningKeywords.rankerType
						.toString()));

			if(prop.containsKey(PropertyFileReader.learningKeywords.nIterations.toString()))
				nIterations = Integer.parseInt(prop.get(PropertyFileReader.learningKeywords.nIterations
						.toString()));
			
			if(prop.containsKey(PropertyFileReader.learningKeywords.nTrees.toString()))
				nTrees = Integer.parseInt(prop.get(PropertyFileReader.learningKeywords.nTrees
						.toString()));
			
			if(prop.containsKey(PropertyFileReader.learningKeywords.nRoundToStopEarly.toString()))
				nRoundToStopEarly = Integer.parseInt(
						prop.get(PropertyFileReader.learningKeywords.nRoundToStopEarly.toString()));
			
			if(prop.containsKey(PropertyFileReader.learningKeywords.nTreeLeaves.toString()))
				nTreeLeaves = Integer.parseInt(prop.get(PropertyFileReader.learningKeywords.nTreeLeaves
						.toString()));
			
			if(prop.containsKey(PropertyFileReader.learningKeywords.learningRate.toString()))
				learningRate = Float.parseFloat(prop.get(PropertyFileReader.learningKeywords.learningRate
						.toString()));

			// liblinear learning
			
			if(prop.containsKey(PropertyFileReader.learningKeywords.solverType.toString()))
				listStrSolverType = prop.get(PropertyFileReader.learningKeywords.solverType
						.toString());
			
			if(prop.containsKey(PropertyFileReader.learningKeywords.c.toString()))
				listStrC = prop.get(PropertyFileReader.learningKeywords.c.toString());
			
			if(prop.containsKey(PropertyFileReader.learningKeywords.eps.toString()))
				listStrEps = prop.get(PropertyFileReader.learningKeywords.eps.toString());
			
			if(prop.containsKey(PropertyFileReader.learningKeywords.p.toString()))
				listStrP = prop.get(PropertyFileReader.learningKeywords.p.toString());
			
			if(prop.containsKey(PropertyFileReader.learningKeywords.timesRealFb.toString()))
				timesRealFb = Integer.parseInt(prop.get(PropertyFileReader.learningKeywords.timesRealFb
					.toString()));
			
			if(prop.containsKey(PropertyFileReader.learningKeywords.nValidNegEx.toString()))
				nValidNegEx = Integer.parseInt(prop.get(PropertyFileReader.learningKeywords.nValidNegEx
					.toString()));
			
			if(prop.containsKey(PropertyFileReader.learningKeywords.minTrainEx.toString()))
				minTrainEx = Integer.parseInt(prop.get(PropertyFileReader.learningKeywords.minTrainEx
					.toString()));
			
			// add randomly selected negative/unknown items to the validation data
			if(prop.containsKey(PropertyFileReader.learningKeywords.addNegValidationEx.toString()))
				addNegValidationEx = Boolean.parseBoolean(
						prop.get(PropertyFileReader.learningKeywords.addNegValidationEx.toString()));
			
			if(prop.containsKey(PropertyFileReader.keywords.topN.toString()))
				topN = Integer.parseInt(prop.get(PropertyFileReader.keywords.topN.toString()));
			
			if(prop.containsKey(PropertyFileReader.keywords.nLearningThreads.toString()))
				nLearningThreads = Integer.parseInt(prop.get(PropertyFileReader.keywords.nLearningThreads
					.toString()));
			
			if(prop.containsKey(PropertyFileReader.learningKeywords.silent.toString()))
				silentLearning = Boolean.parseBoolean(prop.get(PropertyFileReader.learningKeywords.silent
					.toString()));

			// eval
			
			if(prop.containsKey(PropertyFileReader.evalKeywords.thresh.toString()))
				evalRatingThresh = Float.parseFloat(
						prop.get(PropertyFileReader.evalKeywords.thresh.toString()));

			if(prop.containsKey(PropertyFileReader.evalKeywords.negRatingThresh.toString()))
				negRatingThresh = Float.parseFloat(
						prop.get(PropertyFileReader.evalKeywords.negRatingThresh.toString()));
			
			if(prop.containsKey(PropertyFileReader.evalKeywords.relUnknownItems.toString()))
				relUnknownItems = Float.parseFloat(
						prop.get(PropertyFileReader.evalKeywords.relUnknownItems.toString()));
			
			if(prop.containsKey(PropertyFileReader.evalKeywords.reccDirToEval.toString()))
				reccDirToEval = prop.get(PropertyFileReader.evalKeywords.reccDirToEval
						.toString());

			if(prop.containsKey(PropertyFileReader.evalKeywords.itemMetadataEvalFile.toString()))
				itemMetadataEvalFile = prop.get(PropertyFileReader.evalKeywords.itemMetadataEvalFile
						.toString());

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
					
			if (arg.compareTo("dataExtraction") == 0) {
				dataExtraction = Boolean.parseBoolean(val);
			} else if (arg.compareTo("cmpRec") == 0) {
				cmpRec = Boolean.parseBoolean(val);
			} else if (arg.compareTo("eval") == 0) {
				eval = Boolean.parseBoolean(val);
			} else if (arg.compareTo("evalDir") == 0) {
				evalDir = Boolean.parseBoolean(val);
			} else if (arg.compareTo("itemPathExtraction") == 0) {
				itemPathExtraction = Boolean.parseBoolean(val);
			} else if (arg.compareTo("userPathExtraction") == 0) {
				userPathExtraction = Boolean.parseBoolean(val);
			} else if (arg.compareTo("itemGraphEmbedding") == 0) {
				itemGraphEmbedding = Boolean.parseBoolean(val);
			} else if (arg.compareTo("workingDir") == 0) {
				workingDir = val;
			} else if (arg.compareTo("itemContentFile") == 0) {
				itemMetadataFile = val;
			} else if (arg.compareTo("recFile") == 0) {
				recOutputFile = val;
			} else if (arg.compareTo("rec_alg") == 0) {
				rec_algorithm = Integer.parseInt(val);
			} else if (arg.compareTo("trainRatingsFile") == 0) {
				trainRatingFile = val;
			} else if (arg.compareTo("validationRatingsFile") == 0) {
				validationRatingFile = val;
			} else if (arg.compareTo("testRatingsFile") == 0) {
				testRatingFile = val;
			} else if (arg.compareTo("libLinear") == 0) {
				libLinear = Boolean.parseBoolean(val);
			} else if (arg.compareTo("rankLib") == 0) {
				rankLib = Boolean.parseBoolean(val);
			} else if (arg.compareTo("nThreads") == 0) {
				nThreads = Integer.parseInt(val);
			} else if (arg.compareTo("nLearningThreads") == 0) {
				nLearningThreads = Integer.parseInt(val);
			} else if (arg.compareTo("implicit") == 0) {
				implicit = Boolean.parseBoolean(val);
			} else if (arg.compareTo("jenatdb") == 0) {
				jenatdb = Boolean.parseBoolean(val);
			} else if (arg.compareTo("datasetFile") == 0) {
				datasetFile = val;
			} else if (arg.compareTo("TDBdirectory") == 0) {
				tdbDirectory = val;
			} else if (arg.compareTo("endpoint") == 0) {
				endpoint = val;
			} else if (arg.compareTo("graphURI") == 0) {
				graphURI = val;
			} else if (arg.compareTo("inputURIfile") == 0) {
				inputItemURIsFile = val;
			} else if (arg.compareTo("propsFile") == 0) {
				propsFile = val;
			} else if (arg.compareTo("directed") == 0) {
				inverseProps = Boolean.parseBoolean(val);
			} else if (arg.compareTo("outputTextFormat") == 0) {
				outputTextFormat = Boolean.parseBoolean(val);
			} else if (arg.compareTo("outputBinaryFormat") == 0) {
				outputBinaryFormat = Boolean.parseBoolean(val);
			} else if (arg.compareTo("append") == 0) {
				append = Boolean.parseBoolean(val);
			} else if (arg.compareTo("caching") == 0) {
				caching = Boolean.parseBoolean(val);
			} else if (arg.compareTo("itemsInMemory") == 0) {
				itemsInMemory = Integer.parseInt(val);
			} else if (arg.compareTo("normalize") == 0) {
				normalize = Boolean.parseBoolean(val);
			} else if (arg.compareTo("outputExtractionTextFormat") == 0) {
				outputPathTextFormat = Boolean.parseBoolean(val);
			} else if (arg.compareTo("outputExtractionBinaryFormat") == 0) {
				outputPathBinaryFormat = Boolean.parseBoolean(val);
			} else if (arg.compareTo("outputPathFile") == 0) {
				pathFile = val;
			} else if (arg.compareTo("computeInversePaths") == 0) {
				computeInversePaths = Boolean.parseBoolean(val);
			} else if (arg.compareTo("computeTopPaths") == 0) {
				selectTopPaths = Boolean.parseBoolean(val);
			} else if (arg.compareTo("numTopPaths") == 0) {
				numTopPaths = Integer.parseInt(val);
			} else if (arg.compareTo("numItemTopPaths") == 0) {
				numItemTopPaths = Integer.parseInt(val);
			} else if (arg.compareTo("pathsInMemory") == 0) {
				paths_in_memory = Integer.parseInt(val);
			} else if (arg.compareTo("userItemsSampling") == 0) {
				user_items_sampling = Integer.parseInt(val);
			} else if (arg.compareTo("ratingThreshold") == 0) {
				ratingThreshold = Integer.parseInt(val);
			} else if (arg.compareTo("embeddingOption") == 0) {
				embeddingOption = Integer.parseInt(val);
			} else if (arg.compareTo("max_branch_length") == 0) {
				max_branch_length = Integer.parseInt(val);
			} else if (arg.compareTo("addCollabFeatures") == 0) {
				addCollabFeatures = Boolean.parseBoolean(val);
			} else if (arg.compareTo("minmax_norm") == 0) {
				minmax_norm = Boolean.parseBoolean(val);
			} else if (arg.compareTo("min_freq") == 0) {
				min_f = Integer.parseInt(val);
			} else if (arg.compareTo("max_freq") == 0) {
				max_f = Integer.parseInt(val);
			} else if (arg.compareTo("idf") == 0) {
				idf = Boolean.parseBoolean(val);
			} else if (arg.compareTo("length_normaliz") == 0) {
				length_normaliz = Boolean.parseBoolean(val);
			} else if (arg.compareTo("onlyEntityBranches") == 0) {
				onlyEntityBranches = Boolean.parseBoolean(val);
			} else if (arg.compareTo("entityBasedEmbFile") == 0) {
				entityMapFile = val;
			} else if (arg.compareTo("branchBasedEmbFile") == 0) {
				branchMapFile = val;
			} else if (arg.compareTo("rankerType") == 0) {
				rankerType = Integer.parseInt(val);
			} else if (arg.compareTo("nIterations") == 0) {
				nIterations = Integer.parseInt(val);
			} else if (arg.compareTo("nTrees") == 0) {
				nTrees = Integer.parseInt(val);
			} else if (arg.compareTo("nRoundToStopEarly") == 0) {
				nRoundToStopEarly = Integer.parseInt(val);
			} else if (arg.compareTo("nTreeLeaves") == 0) {
				nTreeLeaves = Integer.parseInt(val);
			} else if (arg.compareTo("learningRate") == 0) {
				learningRate = Float.parseFloat(val);
			} else if (arg.compareTo("solverType") == 0) {
				listStrSolverType = val;
			} else if (arg.compareTo("c") == 0) {
				listStrC = val;
			} else if (arg.compareTo("eps") == 0) {
				listStrEps = val;
			} else if (arg.compareTo("p") == 0) {
				listStrP = val;
			} else if (arg.compareTo("timesRealFb") == 0) {
				timesRealFb = Integer.parseInt(val);
			} else if (arg.compareTo("nValidNegEx") == 0) {
				nValidNegEx = Integer.parseInt(val);
			} else if (arg.compareTo("addNegValidationEx") == 0) {
				addNegValidationEx = Boolean.parseBoolean(val);
			}  else if (arg.compareTo("minTrainEx") == 0) {
				minTrainEx = Integer.parseInt(val);
			} else if (arg.compareTo("topN") == 0) {
				topN = Integer.parseInt(val);
			} else if (arg.compareTo("silent") == 0) {
				silentLearning = Boolean.parseBoolean(val);
			} else if (arg.compareTo("thresh") == 0) {
				evalRatingThresh = Float.parseFloat(val);
			} else if (arg.compareTo("negRatingThresh") == 0) {
				negRatingThresh = Float.parseFloat(val);
			} else if (arg.compareTo("relUnknownItems") == 0) {
				relUnknownItems = Float.parseFloat(val);
			} else if (arg.compareTo("itemMetadataEvalFile") == 0) {
				itemMetadataEvalFile = val;
			} else if (arg.compareTo("reccDirToEval") == 0) {
				reccDirToEval = val;
			} else if (arg.compareTo("listAlphaVals") == 0) {
				listAlphaVals = val;
			}
		}
		
	}

}
