package it.poliba.sisinflab.LODRecommender.utils;

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
        
        prop.put("endpoint", props.getProperty("endpoint"));
        prop.put("graphURI", props.getProperty("graphURI"));
        prop.put("datasetFile", props.getProperty("datasetFile"));
        prop.put("TDBdirectory", props.getProperty("TDBdirectory"));
        prop.put("inputURIfile", props.getProperty("inputURIfile"));
        prop.put("outputFile", props.getProperty("outputFile"));
        prop.put("directed", props.getProperty("directed"));
        prop.put("propsFile", props.getProperty("propsFile"));
        prop.put("outputTextFormat", props.getProperty("outputTextFormat"));
        prop.put("outputBinaryFormat", props.getProperty("outputBinaryFormat"));
        prop.put("jenatdb", props.getProperty("jenatdb"));
	    prop.put("caching", props.getProperty("caching"));
	    prop.put("outputTripleFormat", props.getProperty("outputTripleFormat"));
	    prop.put("append", props.getProperty("append"));
	    
	    // paths/features extraction
	    prop.put("inputRatingsFile", props.getProperty("inputRatingsFile"));
	    prop.put("itemsInMemory", props.getProperty("itemsInMemory"));
	    prop.put("itemsFile", props.getProperty("itemsFile"));
	    prop.put("outputExtractionTextFormat", props.getProperty("outputExtractionTextFormat"));
	    prop.put("outputExtractionBinaryFormat", props.getProperty("outputExtractionBinaryFormat"));
	    prop.put("userItemsSampling", props.getProperty("userItemsSampling"));
	    prop.put("ratesThreshold", props.getProperty("ratesThreshold"));
	    prop.put("normalize", props.getProperty("normalize"));
	    
	    // paths extraction
	    prop.put("computeTopPaths", props.getProperty("computeTopPaths"));
	    prop.put("pathsInMemory", props.getProperty("pathsInMemory"));
	    prop.put("numTopPaths", props.getProperty("numTopPaths"));
	    prop.put("numItemTopPaths", props.getProperty("numItemTopPaths"));
	    prop.put("outputPathFile", props.getProperty("outputPathFile"));
	    prop.put("computeInversePaths", props.getProperty("computeInversePaths"));
	    
	    // features extraction
	    prop.put("outputFeaturesFile", props.getProperty("outputFeaturesFile"));
	    prop.put("computeTopFeatures", props.getProperty("computeTopFeatures"));
	    prop.put("lowerLimit", props.getProperty("lowerLimit"));
	    prop.put("upperLimit", props.getProperty("upperLimit"));
	    prop.put("numItemTopFeatures", props.getProperty("numItemTopFeatures"));
	    
	    //learning
	    prop.put("solverType", props.getProperty("solverType"));
	    prop.put("c", props.getProperty("c"));
	    prop.put("eps", props.getProperty("eps"));
	    prop.put("topItems", props.getProperty("topItems"));
	    
    
    return prop;
}
	
}
