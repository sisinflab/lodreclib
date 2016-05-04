package it.poliba.sisinflab.LODRec.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PropertyFileReader {

	public static Map<String, String> loadProperties(String fileName) throws IOException{
		
		Properties props = new Properties();
        FileInputStream fis = new FileInputStream(fileName);
        
        //loading properties from properties file
        props.load(fis);
        
        //reading property
        Map<String, String> prop = new HashMap<String, String>();
        
        // general settings 
        if(props.containsKey("recommendationAlgorithm"))
        	prop.put("recAlgorithm", props.getProperty("recAlgorithm"));
        if(props.containsKey("dataExtraction"))
        	prop.put("dataExtraction", props.getProperty("dataExtraction"));
        if(props.containsKey("itemGraphEmbedding"))
        	prop.put("itemGraphEmbedding", props.getProperty("itemGraphEmbedding"));
        if(props.containsKey("itemPathExtraction"))
        	prop.put("itemPathExtraction", props.getProperty("itemPathExtraction"));
        if(props.containsKey("userPathExtraction"))
        	prop.put("userPathExtraction", props.getProperty("userPathExtraction"));
        if(props.containsKey("computeRecommendation"))
        	prop.put("computeRec", props.getProperty("computeRecommendation"));
        if(props.containsKey("evaluation"))
        	prop.put("evaluation", props.getProperty("evaluation"));
        if(props.containsKey("evaluationDir"))
        	prop.put("evaluationDir", props.getProperty("evaluationDir"));
        
        if(props.containsKey("workingDir"))
        	prop.put("workingDir", props.getProperty("workingDir"));
        if(props.containsKey("nThreads"))
        	prop.put("nThreads", props.getProperty("nThreads"));
        if(props.containsKey("implicit"))
        	prop.put("implicit", props.getProperty("implicit"));
        
        if(props.containsKey("inputTrainRatingFile"))
        	prop.put("inputTrainRatingFile", props.getProperty("inputTrainRatingFile"));
        if(props.containsKey("inputValidationRatingFile"))
        	prop.put("inputValidationRatingFile", props.getProperty("inputValidationRatingFile"));
        if(props.containsKey("inputTestRatingFile"))
        	prop.put("inputTestRatingFile", props.getProperty("inputTestRatingFile"));
        
        // data extraction
        if(props.containsKey("endpoint"))
        	prop.put("endpoint", props.getProperty("endpoint"));
        if(props.containsKey("graphURI"))
        	prop.put("graphURI", props.getProperty("graphURI"));
        if(props.containsKey("localDatasetFile"))
        	prop.put("localDatasetFile", props.getProperty("localDatasetFile"));
        if(props.containsKey("tdbDirectory"))
        	prop.put("tdbDirectory", props.getProperty("tdbDirectory"));
        if(props.containsKey("inputItemURIsFile"))
        	prop.put("inputItemURIsFile", props.getProperty("inputItemURIsFile"));
        if(props.containsKey("directed"))
        	prop.put("directed", props.getProperty("directed"));
        if(props.containsKey("propertiesFile"))
        	prop.put("propertiesFile", props.getProperty("propertiesFile"));
        if(props.containsKey("itemsMetadataFile"))
        	prop.put("itemsMetadataFile", props.getProperty("itemsMetadataFile"));
        if(props.containsKey("outputTextFormat"))
        	prop.put("outputTextFormat", props.getProperty("outputTextFormat"));
        if(props.containsKey("outputBinaryFormat"))
        	prop.put("outputBinaryFormat", props.getProperty("outputBinaryFormat"));
        if(props.containsKey("jenatdb"))
        	prop.put("jenatdb", props.getProperty("jenatdb"));
        if(props.containsKey("caching"))
        	prop.put("caching", props.getProperty("caching"));
        if(props.containsKey("append"))
        	prop.put("append", props.getProperty("append"));
        
        // item graph embedding
        if(props.containsKey("embeddingOption"))
        	prop.put("embeddingOption", props.getProperty("embeddingOption"));
        if(props.containsKey("maxBranchLength"))
        	prop.put("maxBranchLength", props.getProperty("maxBranchLength"));
        if(props.containsKey("addCollabFeatures"))
        	prop.put("addCollabFeatures", props.getProperty("addCollabFeatures"));
        if(props.containsKey("listAlphaVals"))
        	prop.put("listAlphaVals", props.getProperty("listAlphaVals"));
        if(props.containsKey("minMaxNorm"))
        	prop.put("minMaxNorm", props.getProperty("minMaxNorm"));
        if(props.containsKey("minFreq"))
        	prop.put("minFreq", props.getProperty("minFreq"));
        if(props.containsKey("maxFreq"))
        	prop.put("maxFreq", props.getProperty("maxFreq"));
        if(props.containsKey("idf"))
        	prop.put("idf", props.getProperty("idf"));
        if(props.containsKey("lengthNorm"))
        	prop.put("lengthNorm", props.getProperty("lengthNorm"));
        if(props.containsKey("onlyEntityBranches"))
        	prop.put("onlyEntityBranches", props.getProperty("onlyEntityBranches"));
        if(props.containsKey("entityBasedEmbFile"))
        	prop.put("entityBasedEmbFile", props.getProperty("entityBasedEmbFile"));
        if(props.containsKey("branchBasedEmbFile"))
        	prop.put("branchBasedEmbFile", props.getProperty("branchBasedEmbFile"));
	    
	    // paths extraction
        if(props.containsKey("computeTopPaths"))
        	prop.put("computeTopPaths", props.getProperty("computeTopPaths"));
        if(props.containsKey("pathsInMemory"))
        	prop.put("pathsInMemory", props.getProperty("pathsInMemory"));
        if(props.containsKey("nTopPaths"))
        	prop.put("nTopPaths", props.getProperty("nTopPaths"));
        if(props.containsKey("nItemTopPaths"))
        	prop.put("nItemTopPaths", props.getProperty("nItemTopPaths"));
        if(props.containsKey("pathsFile"))
        	prop.put("pathsFile", props.getProperty("pathsFile"));
        if(props.containsKey("outputPathsTextFormat"))
        	prop.put("outputPathsTextFormat", props.getProperty("outputPathsTextFormat"));
        if(props.containsKey("outputPathsBinaryFormat"))
        	prop.put("outputPathsBinaryFormat", props.getProperty("outputPathsBinaryFormat"));
        if(props.containsKey("computeInversePaths"))
        	prop.put("computeInversePaths", props.getProperty("computeInversePaths"));
	    
	    // user paths extraction
        if(props.containsKey("itemsInMemory"))
        	prop.put("itemsInMemory", props.getProperty("itemsInMemory"));
        if(props.containsKey("userItemsSampling"))
        	prop.put("userItemsSampling", props.getProperty("userItemsSampling"));
        if(props.containsKey("ratesThreshold"))
        	prop.put("ratesThreshold", props.getProperty("ratesThreshold"));
        if(props.containsKey("normalize"))
        	prop.put("normalize", props.getProperty("normalize"));
        if(props.containsKey("splitTestSet"))
        	prop.put("splitTestSet", props.getProperty("splitTestSet"));
	    
	    // learning
        if(props.containsKey("libLinear"))
        	prop.put("libLinear", props.getProperty("libLinear"));
        if(props.containsKey("rankLib"))
        	prop.put("rankLib", props.getProperty("rankLib"));
        if(props.containsKey("silentLearning"))
        	prop.put("silentLearning", props.getProperty("silentLearning"));
        
	    // liblinear
        if(props.containsKey("listSolverType"))
        	prop.put("listSolverType", props.getProperty("listSolverType"));
        if(props.containsKey("listC"))
        	prop.put("listC", props.getProperty("listC"));
        if(props.containsKey("listEps"))
        	prop.put("listEps", props.getProperty("listEps"));
        if(props.containsKey("listP"))
        	prop.put("listP", props.getProperty("listP"));
	    
        // ranklib
        if(props.containsKey("rankerType"))
        	prop.put("rankerType", props.getProperty("rankerType"));
	    if(props.containsKey("nIteration"))
	    	prop.put("nIteration", props.getProperty("nIteration"));
	    if(props.containsKey("tolerance")) 
	    	prop.put("tolerance", props.getProperty("tolerance"));
	    if(props.containsKey("nThreshold"))
	    	prop.put("nThreshold", props.getProperty("nThreshold"));
	    if(props.containsKey("nTrees"))
	    	prop.put("nTrees", props.getProperty("nTrees"));
	    if(props.containsKey("nTreeLeaves"))
	    	prop.put("nTreeLeaves", props.getProperty("nTreeLeaves"));
	    if(props.containsKey("learningRate"))
	    	prop.put("learningRate", props.getProperty("learningRate"));
	    if(props.containsKey("minLeafSupport"))
	    	prop.put("minLeafSupport", props.getProperty("minLeafSupport"));
	    if(props.containsKey("maxSelCount"))
	    	prop.put("maxSelCount", props.getProperty("maxSelCount"));
	    if(props.containsKey("nMaxIteration"))
	    	prop.put("nMaxIteration", props.getProperty("nMaxIteration"));
	    if(props.containsKey("nRestart"))
	    	prop.put("nRestart", props.getProperty("nRestart"));
	    if(props.containsKey("regularized"))
	    	prop.put("regularized", props.getProperty("regularized"));
	    if(props.containsKey("nRoundToStopEarly"))
	    	prop.put("nRoundToStopEarly", props.getProperty("nRoundToStopEarly"));
	    if(props.containsKey("nBag"))
	    	prop.put("nBag", props.getProperty("nBag"));
	    if(props.containsKey("featureSamplingRate"))
	    	prop.put("featureSamplingRate", props.getProperty("featureSamplingRate"));
	    if(props.containsKey("subSamplingRate"))
	    	prop.put("subSamplingRate", props.getProperty("subSamplingRate"));
	    
	    //evaluation
	    if(props.containsKey("evalMetric"))
        	prop.put("evalMetric", props.getProperty("evalMetric"));
        if(props.containsKey("evalRatingThresh"))
        	prop.put("evalRatingThresh", props.getProperty("evalRatingThresh"));
        if(props.containsKey("relUnknownItems"))
        	prop.put("relUnknownItems", props.getProperty("relUnknownItems"));
        if(props.containsKey("negRatingThresh"))
        	prop.put("negRatingThresh", props.getProperty("negRatingThresh"));
        if(props.containsKey("itemsMetadataEvalFile"))
        	prop.put("itemsMetadataEvalFile", props.getProperty("itemsMetadataEvalFile"));
        if(props.containsKey("outputEvaluationFile"))
        	prop.put("outputEvaluationFile", props.getProperty("outputEvaluationFile"));
        if(props.containsKey("recDirToEval"))
        	prop.put("recDirToEval", props.getProperty("recDirToEval"));
	    	    
	    //recommendation
	    if(props.containsKey("recommendationsFile"))
	    	prop.put("recommendationsFile", props.getProperty("recommendationsFile"));
	    if(props.containsKey("topN"))
	    	prop.put("topN", props.getProperty("topN"));
	    
    
    return prop;
}
	
}
