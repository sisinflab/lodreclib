package it.poliba.sisinflab.LODRec.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * This class is part of the LOD Recommender
 * 
 * This class loads configuration parameters from property file
 * 
 * @author Vito Mastromarino
 * @author Vito Claudio Ostuni
 */

public class PropertyFileReader {

	public static enum keywords {
		workingDir, dataExtraction, itemPathExtraction, userPathExtraction, itemGraphEmbedding, learning, predict, eval, trainRatingsFile, validationRatingsFile, testRatingsFile, rec_alg, nThreads, nLearningThreads, implicit, addNegValidationEx, recFile, topN, rankLib, libLinear, n_splits, evalDir, cmpRec
	}

	public static enum extractionKeywords {
		endpoint, graphURI, datasetFile, TDBdirectory, inputURIfile, itemContentFile, directed, propsFile, outputTextFormat, outputBinaryFormat, jenatdb, caching, outputTripleFormat, append
	}

	public static enum pathKeywords {
		itemsInMemory, outputExtractionTextFormat, outputExtractionBinaryFormat, userItemsSampling, ratingThreshold, normalize, computeTopPaths, pathsInMemory, numTopPaths, numItemTopPaths, outputPathFile, computeInversePaths
	}

	public static enum graphEMbKeywords {
		embeddingOption, entityBasedEmbFile, branchBasedEmbFile, nBits, addCollabFeatures, topNUserFeatures, max_branch_length,onlyEntityBranches, min_freq, max_freq, minmax_norm, sign,idf,length_normaliz,listAlphaVals
	}

	public static enum learningKeywords {
		solverType, c, eps, p, silent, rankerType, nIterations, nTrees, nRoundToStopEarly, nTreeLeaves, learningRate, minTrainEx, nValidNegEx, timesRealFb, addNegValidationEx
	}

	public static enum evalKeywords {
		thresh, negRatingThresh, relUnknownItems, reccDirToEval, itemMetadataEvalFile, outputEvaluationFile
	}

	public static Map<String, String> loadProperties(String fileName)
			throws IOException {

		Properties props = new Properties();
		FileInputStream fis = new FileInputStream(fileName);

		// loading properties from properties file
		props.load(fis);

		// reading property
		Map<String, String> prop = new HashMap<String, String>();

		if(props.containsKey(PropertyFileReader.keywords.dataExtraction.toString()))
			prop.put(PropertyFileReader.keywords.dataExtraction.toString(), 
					props.getProperty(PropertyFileReader.keywords.dataExtraction.toString()));
		
		if(props.containsKey(PropertyFileReader.keywords.itemPathExtraction.toString()))
			prop.put(PropertyFileReader.keywords.itemPathExtraction.toString(),
				props.getProperty(PropertyFileReader.keywords.itemPathExtraction.toString()));
		
		if(props.containsKey(PropertyFileReader.keywords.userPathExtraction.toString()))
			prop.put(PropertyFileReader.keywords.userPathExtraction.toString(),
				props.getProperty(PropertyFileReader.keywords.userPathExtraction.toString()));

		if(props.containsKey(PropertyFileReader.keywords.itemGraphEmbedding.toString()))
			prop.put(PropertyFileReader.keywords.itemGraphEmbedding.toString(),
				props.getProperty(PropertyFileReader.keywords.itemGraphEmbedding.toString()));

		if(props.containsKey(PropertyFileReader.keywords.n_splits.toString()))
			prop.put(PropertyFileReader.keywords.n_splits.toString(), 
				props.getProperty(PropertyFileReader.keywords.n_splits.toString()));

		if(props.containsKey(PropertyFileReader.keywords.rankLib.toString()))
			prop.put(PropertyFileReader.keywords.rankLib.toString(), 
				props.getProperty(PropertyFileReader.keywords.rankLib.toString()));
		
		if(props.containsKey(PropertyFileReader.keywords.libLinear.toString()))
			prop.put(PropertyFileReader.keywords.libLinear.toString(), 
				props.getProperty(PropertyFileReader.keywords.libLinear.toString()));

		if(props.containsKey(PropertyFileReader.keywords.cmpRec.toString()))
			prop.put(PropertyFileReader.keywords.cmpRec.toString(), 
				props.getProperty(PropertyFileReader.keywords.cmpRec.toString()));

		if(props.containsKey(PropertyFileReader.keywords.eval.toString()))
			prop.put(PropertyFileReader.keywords.eval.toString(),
				props.getProperty(PropertyFileReader.keywords.eval.toString()));

		if(props.containsKey(PropertyFileReader.keywords.evalDir.toString()))
			prop.put(PropertyFileReader.keywords.evalDir.toString(),
				props.getProperty(PropertyFileReader.keywords.evalDir.toString()));

		if(props.containsKey(PropertyFileReader.keywords.workingDir.toString()))
			prop.put(PropertyFileReader.keywords.workingDir.toString(), 
				props.getProperty(PropertyFileReader.keywords.workingDir.toString()));

		if(props.containsKey(PropertyFileReader.keywords.trainRatingsFile.toString()))
			prop.put(PropertyFileReader.keywords.trainRatingsFile.toString(), 
				props.getProperty(PropertyFileReader.keywords.trainRatingsFile.toString()));
		
		if(props.containsKey(PropertyFileReader.keywords.validationRatingsFile.toString()))
			prop.put(PropertyFileReader.keywords.validationRatingsFile.toString(),
				props.getProperty(PropertyFileReader.keywords.validationRatingsFile.toString()));
		
		if(props.containsKey(PropertyFileReader.keywords.testRatingsFile.toString()))
			prop.put(PropertyFileReader.keywords.testRatingsFile.toString(), 
				props.getProperty(PropertyFileReader.keywords.testRatingsFile.toString()));
		
		if(props.containsKey(PropertyFileReader.keywords.rec_alg.toString()))
			prop.put(PropertyFileReader.keywords.rec_alg.toString(), 
				props.getProperty(PropertyFileReader.keywords.rec_alg.toString()));

		if(props.containsKey(PropertyFileReader.keywords.nLearningThreads.toString()))
			prop.put(PropertyFileReader.keywords.nLearningThreads.toString(), 
				props.getProperty(PropertyFileReader.keywords.nLearningThreads.toString()));

		if(props.containsKey(PropertyFileReader.keywords.nThreads.toString()))
			prop.put(PropertyFileReader.keywords.nThreads.toString(), 
				props.getProperty(PropertyFileReader.keywords.nThreads.toString()));

		if(props.containsKey(PropertyFileReader.keywords.topN.toString()))
			prop.put(PropertyFileReader.keywords.topN.toString(),
				props.getProperty(PropertyFileReader.keywords.topN.toString()));

		if(props.containsKey(PropertyFileReader.keywords.addNegValidationEx.toString()))
			prop.put(PropertyFileReader.keywords.addNegValidationEx.toString(),
				props.getProperty(PropertyFileReader.keywords.addNegValidationEx.toString()));

		if(props.containsKey(PropertyFileReader.keywords.recFile.toString()))
			prop.put(PropertyFileReader.keywords.recFile.toString(), 
				props.getProperty(PropertyFileReader.keywords.recFile.toString()));

		if(props.containsKey(PropertyFileReader.keywords.implicit.toString()))
			prop.put(PropertyFileReader.keywords.implicit.toString(), 
				props.getProperty(PropertyFileReader.keywords.implicit.toString()));

		// item metadata extraction
		if(props.containsKey(PropertyFileReader.extractionKeywords.endpoint.toString()))
			prop.put(PropertyFileReader.extractionKeywords.endpoint.toString(),
				props.getProperty(PropertyFileReader.extractionKeywords.endpoint.toString()));
		
		if(props.containsKey(PropertyFileReader.extractionKeywords.graphURI.toString()))
			prop.put(PropertyFileReader.extractionKeywords.graphURI.toString(),
				props.getProperty(PropertyFileReader.extractionKeywords.graphURI.toString()));
		
		if(props.containsKey(PropertyFileReader.extractionKeywords.datasetFile.toString()))
			prop.put(PropertyFileReader.extractionKeywords.datasetFile.toString(),
				props.getProperty(PropertyFileReader.extractionKeywords.datasetFile.toString()));
		
		if(props.containsKey(PropertyFileReader.extractionKeywords.TDBdirectory.toString()))
			prop.put(PropertyFileReader.extractionKeywords.TDBdirectory.toString(),
				props.getProperty(PropertyFileReader.extractionKeywords.TDBdirectory.toString()));
		
		if(props.containsKey(PropertyFileReader.extractionKeywords.inputURIfile.toString()))
			prop.put(PropertyFileReader.extractionKeywords.inputURIfile.toString(),
				props.getProperty(PropertyFileReader.extractionKeywords.inputURIfile.toString()));
		
		if(props.containsKey(PropertyFileReader.extractionKeywords.itemContentFile.toString()))
			prop.put(PropertyFileReader.extractionKeywords.itemContentFile.toString(),
				props.getProperty(PropertyFileReader.extractionKeywords.itemContentFile.toString()));
		
		if(props.containsKey(PropertyFileReader.extractionKeywords.directed.toString()))
			prop.put(PropertyFileReader.extractionKeywords.directed.toString(),
				props.getProperty(PropertyFileReader.extractionKeywords.directed.toString()));
		
		if(props.containsKey(PropertyFileReader.extractionKeywords.propsFile.toString()))
			prop.put(PropertyFileReader.extractionKeywords.propsFile.toString(),
				props.getProperty(PropertyFileReader.extractionKeywords.propsFile.toString()));
		
		if(props.containsKey(PropertyFileReader.extractionKeywords.outputTextFormat.toString()))
			prop.put(PropertyFileReader.extractionKeywords.outputTextFormat.toString(),
				props.getProperty(PropertyFileReader.extractionKeywords.outputTextFormat.toString()));
		
		if(props.containsKey(PropertyFileReader.extractionKeywords.outputBinaryFormat.toString()))
			prop.put(PropertyFileReader.extractionKeywords.outputBinaryFormat.toString(),
				props.getProperty(PropertyFileReader.extractionKeywords.outputBinaryFormat.toString()));
		
		if(props.containsKey(PropertyFileReader.extractionKeywords.jenatdb.toString()))
			prop.put(PropertyFileReader.extractionKeywords.jenatdb.toString(),
				props.getProperty(PropertyFileReader.extractionKeywords.jenatdb.toString()));
		
		if(props.containsKey(PropertyFileReader.extractionKeywords.caching.toString()))
			prop.put(PropertyFileReader.extractionKeywords.caching.toString(),
				props.getProperty(PropertyFileReader.extractionKeywords.caching.toString()));
		
		if(props.containsKey(PropertyFileReader.extractionKeywords.outputTripleFormat.toString()))
			prop.put(PropertyFileReader.extractionKeywords.outputTripleFormat.toString(),
				props.getProperty(PropertyFileReader.extractionKeywords.outputTripleFormat.toString()));
		
		if(props.containsKey(PropertyFileReader.extractionKeywords.append.toString()))
			prop.put(PropertyFileReader.extractionKeywords.append.toString(), 
				props.getProperty(PropertyFileReader.extractionKeywords.append.toString()));

		// paths/features extraction
		if(props.containsKey(PropertyFileReader.pathKeywords.itemsInMemory.toString()))
			prop.put(PropertyFileReader.pathKeywords.itemsInMemory.toString(),
				props.getProperty(PropertyFileReader.pathKeywords.itemsInMemory.toString()));
		
		if(props.containsKey(PropertyFileReader.pathKeywords.outputExtractionTextFormat.toString()))
			prop.put(PropertyFileReader.pathKeywords.outputExtractionTextFormat.toString(),
				props.getProperty(PropertyFileReader.pathKeywords.outputExtractionTextFormat.toString()));
		
		if(props.containsKey(PropertyFileReader.pathKeywords.outputExtractionBinaryFormat.toString()))
			prop.put(PropertyFileReader.pathKeywords.outputExtractionBinaryFormat.toString(),
				props.getProperty(PropertyFileReader.pathKeywords.outputExtractionBinaryFormat.toString()));
		
		if(props.containsKey(PropertyFileReader.pathKeywords.userItemsSampling.toString()))
			prop.put(PropertyFileReader.pathKeywords.userItemsSampling.toString(),
				props.getProperty(PropertyFileReader.pathKeywords.userItemsSampling.toString()));
		
		if(props.containsKey(PropertyFileReader.pathKeywords.ratingThreshold.toString()))
			prop.put(PropertyFileReader.pathKeywords.ratingThreshold.toString(),
				props.getProperty(PropertyFileReader.pathKeywords.ratingThreshold.toString()));
		
		if(props.containsKey(PropertyFileReader.pathKeywords.normalize.toString()))
			prop.put(PropertyFileReader.pathKeywords.normalize.toString(), 
				props.getProperty(PropertyFileReader.pathKeywords.normalize.toString()));

		// item paths extraction
		if(props.containsKey(PropertyFileReader.pathKeywords.computeTopPaths.toString()))
			prop.put(PropertyFileReader.pathKeywords.computeTopPaths.toString(),
				props.getProperty(PropertyFileReader.pathKeywords.computeTopPaths.toString()));
		
		if(props.containsKey(PropertyFileReader.pathKeywords.pathsInMemory.toString()))
			prop.put(PropertyFileReader.pathKeywords.pathsInMemory.toString(),
				props.getProperty(PropertyFileReader.pathKeywords.pathsInMemory.toString()));
		
		if(props.containsKey(PropertyFileReader.pathKeywords.numTopPaths.toString()))
			prop.put(PropertyFileReader.pathKeywords.numTopPaths.toString(), 
				props.getProperty(PropertyFileReader.pathKeywords.numTopPaths.toString()));
		
		if(props.containsKey(PropertyFileReader.pathKeywords.numItemTopPaths.toString()))
			prop.put(PropertyFileReader.pathKeywords.numItemTopPaths.toString(),
				props.getProperty(PropertyFileReader.pathKeywords.numItemTopPaths.toString()));
		
		if(props.containsKey(PropertyFileReader.pathKeywords.outputPathFile.toString()))
			prop.put(PropertyFileReader.pathKeywords.outputPathFile.toString(),
				props.getProperty(PropertyFileReader.pathKeywords.outputPathFile.toString()));
		
		if(props.containsKey(PropertyFileReader.pathKeywords.computeInversePaths.toString()))
			prop.put(PropertyFileReader.pathKeywords.computeInversePaths.toString(),
				props.getProperty(PropertyFileReader.pathKeywords.computeInversePaths.toString()));

		// hash rec
		if(props.containsKey(PropertyFileReader.graphEMbKeywords.embeddingOption.toString()))
			prop.put(PropertyFileReader.graphEMbKeywords.embeddingOption.toString(),
				props.getProperty(PropertyFileReader.graphEMbKeywords.embeddingOption.toString()));
		
		if(props.containsKey(PropertyFileReader.graphEMbKeywords.max_branch_length.toString()))
			prop.put(PropertyFileReader.graphEMbKeywords.max_branch_length.toString(),
				props.getProperty(PropertyFileReader.graphEMbKeywords.max_branch_length.toString()));
		
		if(props.containsKey(PropertyFileReader.graphEMbKeywords.entityBasedEmbFile.toString()))
			prop.put(PropertyFileReader.graphEMbKeywords.entityBasedEmbFile.toString(),
				props.getProperty(PropertyFileReader.graphEMbKeywords.entityBasedEmbFile.toString()));
		
		if(props.containsKey(PropertyFileReader.graphEMbKeywords.branchBasedEmbFile.toString()))
			prop.put(PropertyFileReader.graphEMbKeywords.branchBasedEmbFile.toString(),
				props.getProperty(PropertyFileReader.graphEMbKeywords.branchBasedEmbFile.toString()));

		if(props.containsKey(PropertyFileReader.graphEMbKeywords.listAlphaVals.toString()))
			prop.put(PropertyFileReader.graphEMbKeywords.listAlphaVals.toString(),
				props.getProperty(PropertyFileReader.graphEMbKeywords.listAlphaVals.toString()));

		if(props.containsKey(PropertyFileReader.graphEMbKeywords.nBits.toString()))
			prop.put(PropertyFileReader.graphEMbKeywords.nBits.toString(), 
				props.getProperty(PropertyFileReader.graphEMbKeywords.nBits.toString()));

		if(props.containsKey(PropertyFileReader.graphEMbKeywords.addCollabFeatures.toString()))
			prop.put(PropertyFileReader.graphEMbKeywords.addCollabFeatures.toString(),
				props.getProperty(PropertyFileReader.graphEMbKeywords.addCollabFeatures.toString()));
		
		if(props.containsKey(PropertyFileReader.graphEMbKeywords.onlyEntityBranches.toString()))
			prop.put(PropertyFileReader.graphEMbKeywords.onlyEntityBranches.toString(),
				props.getProperty(PropertyFileReader.graphEMbKeywords.onlyEntityBranches.toString()));
		
		if(props.containsKey(PropertyFileReader.graphEMbKeywords.topNUserFeatures.toString()))
			prop.put(PropertyFileReader.graphEMbKeywords.topNUserFeatures.toString(),
				props.getProperty(PropertyFileReader.graphEMbKeywords.topNUserFeatures.toString()));
		
		if(props.containsKey(PropertyFileReader.graphEMbKeywords.min_freq.toString()))
			prop.put(PropertyFileReader.graphEMbKeywords.min_freq.toString(),
				props.getProperty(PropertyFileReader.graphEMbKeywords.min_freq.toString()));
		
		if(props.containsKey(PropertyFileReader.graphEMbKeywords.max_freq.toString()))
			prop.put(PropertyFileReader.graphEMbKeywords.max_freq.toString(),
				props.getProperty(PropertyFileReader.graphEMbKeywords.max_freq.toString()));
		
		if(props.containsKey(PropertyFileReader.graphEMbKeywords.minmax_norm.toString()))
			prop.put(PropertyFileReader.graphEMbKeywords.minmax_norm.toString(),
				props.getProperty(PropertyFileReader.graphEMbKeywords.minmax_norm.toString()));
		
		if(props.containsKey(PropertyFileReader.graphEMbKeywords.sign.toString()))
			prop.put(PropertyFileReader.graphEMbKeywords.sign.toString(),
				props.getProperty(PropertyFileReader.graphEMbKeywords.sign.toString()));

		if(props.containsKey(PropertyFileReader.graphEMbKeywords.idf.toString()))
			prop.put(PropertyFileReader.graphEMbKeywords.idf.toString(),
				props.getProperty(PropertyFileReader.graphEMbKeywords.idf.toString()));
		
		if(props.containsKey(PropertyFileReader.graphEMbKeywords.length_normaliz.toString()))
			prop.put(PropertyFileReader.graphEMbKeywords.length_normaliz.toString(),
				props.getProperty(PropertyFileReader.graphEMbKeywords.length_normaliz.toString()));
		
		// learning
		if(props.containsKey(PropertyFileReader.learningKeywords.solverType.toString()))
			prop.put(PropertyFileReader.learningKeywords.solverType.toString(),
				props.getProperty(PropertyFileReader.learningKeywords.solverType.toString()));
		
		if(props.containsKey(PropertyFileReader.learningKeywords.rankerType.toString()))
			prop.put(PropertyFileReader.learningKeywords.rankerType.toString(),
				props.getProperty(PropertyFileReader.learningKeywords.rankerType.toString()));
		
		if(props.containsKey(PropertyFileReader.learningKeywords.nIterations.toString()))
			prop.put(PropertyFileReader.learningKeywords.nIterations.toString(),
				props.getProperty(PropertyFileReader.learningKeywords.nIterations.toString()));
		
		if(props.containsKey(PropertyFileReader.learningKeywords.nTrees.toString()))
			prop.put(PropertyFileReader.learningKeywords.nTrees.toString(),
				props.getProperty(PropertyFileReader.learningKeywords.nTrees.toString()));
		
		if(props.containsKey(PropertyFileReader.learningKeywords.nRoundToStopEarly.toString()))
			prop.put(PropertyFileReader.learningKeywords.nRoundToStopEarly.toString(),
				props.getProperty(PropertyFileReader.learningKeywords.nRoundToStopEarly.toString()));
		
		if(props.containsKey(PropertyFileReader.learningKeywords.nTreeLeaves.toString()))
			prop.put(PropertyFileReader.learningKeywords.nTreeLeaves.toString(),
				props.getProperty(PropertyFileReader.learningKeywords.nTreeLeaves.toString()));
		
		if(props.containsKey(PropertyFileReader.learningKeywords.learningRate.toString()))
			prop.put(PropertyFileReader.learningKeywords.learningRate.toString(),
				props.getProperty(PropertyFileReader.learningKeywords.learningRate.toString()));
		
		if(props.containsKey(PropertyFileReader.learningKeywords.minTrainEx.toString()))
			prop.put(PropertyFileReader.learningKeywords.minTrainEx.toString(),
				props.getProperty(PropertyFileReader.learningKeywords.minTrainEx.toString()));
		
		if(props.containsKey(PropertyFileReader.learningKeywords.nValidNegEx.toString()))
			prop.put(PropertyFileReader.learningKeywords.nValidNegEx.toString(),
				props.getProperty(PropertyFileReader.learningKeywords.nValidNegEx.toString()));
		
		if(props.containsKey(PropertyFileReader.learningKeywords.timesRealFb.toString()))
			prop.put(PropertyFileReader.learningKeywords.timesRealFb.toString(),
				props.getProperty(PropertyFileReader.learningKeywords.timesRealFb.toString()));
		
		if(props.containsKey(PropertyFileReader.learningKeywords.addNegValidationEx.toString()))
			prop.put(PropertyFileReader.learningKeywords.addNegValidationEx.toString(),
				props.getProperty(PropertyFileReader.learningKeywords.addNegValidationEx.toString()));
		 
		if(props.containsKey(PropertyFileReader.learningKeywords.c.toString()))
			prop.put(PropertyFileReader.learningKeywords.c.toString(), 
				props.getProperty(PropertyFileReader.learningKeywords.c.toString()));
		
		if(props.containsKey(PropertyFileReader.learningKeywords.eps.toString()))
			prop.put(PropertyFileReader.learningKeywords.eps.toString(),
				props.getProperty(PropertyFileReader.learningKeywords.eps.toString()));
		
		if(props.containsKey(PropertyFileReader.learningKeywords.p.toString()))
			prop.put(PropertyFileReader.learningKeywords.p.toString(), 
				props.getProperty(PropertyFileReader.learningKeywords.p.toString()));

		if(props.containsKey(PropertyFileReader.learningKeywords.silent.toString()))
			prop.put(PropertyFileReader.learningKeywords.silent.toString(), 
				props.getProperty(PropertyFileReader.learningKeywords.silent.toString()));

		// eval
		if(props.containsKey(PropertyFileReader.evalKeywords.thresh.toString()))
			prop.put(PropertyFileReader.evalKeywords.thresh.toString(), 
				props.getProperty(PropertyFileReader.evalKeywords.thresh.toString()));

		if(props.containsKey(PropertyFileReader.evalKeywords.negRatingThresh.toString()))
			prop.put(PropertyFileReader.evalKeywords.negRatingThresh.toString(), 
				props.getProperty(PropertyFileReader.evalKeywords.negRatingThresh.toString()));

		if(props.containsKey(PropertyFileReader.evalKeywords.relUnknownItems.toString()))
			prop.put(PropertyFileReader.evalKeywords.relUnknownItems.toString(),
				props.getProperty(PropertyFileReader.evalKeywords.relUnknownItems.toString()));
		
		if(props.containsKey(PropertyFileReader.evalKeywords.reccDirToEval.toString()))
			prop.put(PropertyFileReader.evalKeywords.reccDirToEval.toString(),
				props.getProperty(PropertyFileReader.evalKeywords.reccDirToEval.toString()));
		
		if(props.containsKey(PropertyFileReader.evalKeywords.itemMetadataEvalFile.toString()))
			prop.put(PropertyFileReader.evalKeywords.itemMetadataEvalFile.toString(),
				props.getProperty(PropertyFileReader.evalKeywords.itemMetadataEvalFile.toString()));
		
		if(props.containsKey(PropertyFileReader.evalKeywords.outputEvaluationFile.toString()))
			prop.put(PropertyFileReader.evalKeywords.outputEvaluationFile.toString(),
				props.getProperty(PropertyFileReader.evalKeywords.outputEvaluationFile.toString()));
		
		return prop;
	}
}
