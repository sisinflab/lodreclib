package it.poliba.sisinflab.LODRecommender.propertiesLearning;

import gnu.trove.map.hash.TObjectIntHashMap;
import it.poliba.sisinflab.LODRecommender.fileManager.ItemFileManager;
import it.poliba.sisinflab.LODRecommender.fileManager.TextFileManager;
import it.poliba.sisinflab.LODRecommender.utils.MemoryMonitor;
import it.poliba.sisinflab.LODRecommender.utils.PropertyFileReader;
import it.poliba.sisinflab.LODRecommender.utils.SynchronizedCounter;
import it.poliba.sisinflab.LODRecommender.utils.TextFileUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.tdb.TDBLoader;
import com.hp.hpl.jena.tdb.base.file.Location;
import com.hp.hpl.jena.tdb.sys.TDBInternal;
import com.hp.hpl.jena.util.FileManager;

/**
 * This class is part of the LOD Recommender
 * 
 * This class extracts RDF triples 
 * 
 * @author Vito Mastromarino
 */
public class RDFTripleExtractor {
	
	private String endpoint; // endpoint sparql address
	private String graphURI; // graph uri
	private String tdbDirectory; // local TDB directory
	private String datasetFile; // local dataset
	private String inputFile; // input filename
	private TObjectIntHashMap<String> URI_ID;
	private TObjectIntHashMap<String> props_index; // property index
	private SynchronizedCounter metadata_counter; // synchronized counter for metadata index
	private SynchronizedCounter properties_counter;
	private TObjectIntHashMap<String> metadata_index; // metadata index
	private Model model; // local dataset model
	private String metadataFile; // output metadata filename
	private boolean outputTextFormat;
	private int depth = 2;
	private int num_items = 2;
	private String textFile;
	
	private static Logger logger = LogManager.getLogger(RDFTripleExtractor.class.getName());
	
	/**
	 * Constuctor
	 */
	public RDFTripleExtractor() {
		
		Map<String, String> prop = null;
		try {
			
			// load config file
			prop = PropertyFileReader.loadProperties("config.properties");
			
			this.metadataFile = prop.get("outputFile");
			this.inputFile = prop.get("inputURIfile");
			this.endpoint = prop.get("endpoint");
			this.graphURI = prop.get("graphURI");
			this.tdbDirectory = prop.get("TDBdirectory");
			this.datasetFile = prop.get("datasetFile");
			this.outputTextFormat =  Boolean.valueOf(prop.get("outputTextFormat"));
			this.model = null;
			this.textFile = this.metadataFile + ".txt";
						
			// load input uri file
			loadInputFile();
									
			// if local=true load local dataset
			if(Boolean.valueOf(prop.get("local")))
				TDBloading();
						
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		prop.clear();
	}
	
	/**
	 * Load TDB
	 */
	private void TDBloading(){
		
		logger.info("TDB loading");
		
		// create model from tdb
        Dataset dataset = TDBFactory.createDataset(tdbDirectory);

		// assume we want the default model, or we could get a named model here
        dataset.begin(ReadWrite.READ);
		model = dataset.getDefaultModel();
		dataset.end() ;
		
		// if model is null load local dataset into TDB
		if(model == null)
			TDBloading(datasetFile);
		
	}
	
	/**
	 * Load local dataset into TDB
	 */
	private void TDBloading(String fileDump){
		
		logger.info("TDB creation");
		
		// create tdb from .nt local file 
		FileManager fm = FileManager.get();
		fm.addLocatorClassLoader(RDFTripleExtractor.class.getClassLoader());
		InputStream in = fm.open(fileDump);

		Location location = new Location (tdbDirectory);

		// load some initial data
		try{
			TDBLoader.load(TDBInternal.getBaseDatasetGraphTDB(TDBFactory.createDatasetGraph(location)), in, true);
		}
		catch(Exception e){
			logger.error("TDB loading error: " + e.getMessage());
		}
		
		logger.info("TDB loading");
		
		//create model from tdb
        Dataset dataset = TDBFactory.createDataset(tdbDirectory);

		// assume we want the default model, or we could get a named model here
        dataset.begin(ReadWrite.READ) ;
		model = dataset.getDefaultModel();
		dataset.end();
		
	}
	
	
	
	/**
	 * Load input file
	 */
	private void loadInputFile(){
		
		URI_ID = new TObjectIntHashMap<String>();
		// load uri--id from input file
		TextFileUtils.loadInputURIs(inputFile, URI_ID, false);
		logger.debug("Input URIs loading. " + URI_ID.size() + " URIs loaded.");
	}
	
	
	
	/**
	 * Start RDF triple extraction
	 */
	private void run(){
		
		// get processors number for multi-threading
		int n_threads =  Runtime.getRuntime().availableProcessors();
		n_threads = 4;
		logger.debug("Threads number: " + n_threads);
		
		ExecutorService executor;
		executor = Executors.newFixedThreadPool(n_threads);
		
		metadata_counter = new SynchronizedCounter();
		properties_counter = new SynchronizedCounter();
		metadata_index = new TObjectIntHashMap<String>();
		props_index = new TObjectIntHashMap<String>();
		
		logger.info("Risorse da interrogare: " + num_items);
		
		try{
			
			TextFileManager textWriter = null;
			if(outputTextFormat)
				textWriter = new TextFileManager(textFile);
			
			ItemFileManager fileManager = new ItemFileManager(metadataFile, ItemFileManager.WRITE);
			
			for(int i = 0; i < num_items; i++){
				
				String uri = (String) URI_ID.keys()[i];
				
				Runnable worker;
				
				if(model==null){
					// create worker thread - extraction from endpoint
					worker = new QueryExecutor(uri, URI_ID.get(uri), props_index, graphURI, 
							endpoint, metadata_counter, properties_counter, metadata_index, 
							textWriter, fileManager, depth);
				}
				else{
					// create worker thread - extraction from tdb local dataset
					worker = new QueryExecutor(uri, URI_ID.get(uri), props_index, graphURI, 
						endpoint, metadata_counter, properties_counter, metadata_index, 
						textWriter, fileManager, depth, model);
				}
				
				executor.execute(worker);
				
			}
			
			// This will make the executor accept no new threads
		    // and finish all existing threads in the queue
		    executor.shutdown();
		    // Wait until all threads are finish
		    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		    
		    if(textWriter!=null)
		    	textWriter.close();
		    
		    fileManager.close();
		    	
		}
		catch(Exception e){
			e.printStackTrace();
		}
	    
		// write metadata index file
		TextFileUtils.writeData(metadataFile + "_index", metadata_index);
		// write metadata index file
		TextFileUtils.writeData("props_index", props_index);

	}
	
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		RDFTripleExtractor m = new RDFTripleExtractor();
		
		long start = System.currentTimeMillis();
		
		m.run();
		
		long stop = System.currentTimeMillis();
		logger.info("Finished all threads. Data extraction terminated in [sec]: " 
				+ ((stop - start) / 1000));
				
		MemoryMonitor.stats();

	}

}
