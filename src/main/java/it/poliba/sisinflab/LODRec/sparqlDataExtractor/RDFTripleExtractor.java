package it.poliba.sisinflab.LODRec.sparqlDataExtractor;

import gnu.trove.map.hash.TObjectIntHashMap;
import it.poliba.sisinflab.LODRec.fileManager.ItemFileManager;
import it.poliba.sisinflab.LODRec.fileManager.TextFileManager;
import it.poliba.sisinflab.LODRec.tree.NTree;
import it.poliba.sisinflab.LODRec.utils.SynchronizedCounter;
import it.poliba.sisinflab.LODRec.utils.TextFileUtils;
import it.poliba.sisinflab.LODRec.utils.XMLUtils;

import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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

	private String workingDir; // working directory
	private int nThreads; // threads number
	private boolean jenatdb; // use jena tdb
	private String endpoint; // endpoint sparql address
	private String graphURI; // graph uri
	private String tdbDirectory; // local TDB directory
	private String datasetFile; // local dataset
	private Model model; // local dataset model
	private String inputFile; // input filename
	private String propsFile; // properties filename
	private boolean outputBinaryFormat; // output metadata format
	private String metadataFile; // output metadata filename
	private boolean outputTextFormat; // output text format
	private String textFile; // metadata text file
	
	private boolean inverseProps; // directed property
	private boolean caching; // caching
	private boolean append; // append to previous extraction
	
	private TObjectIntHashMap<String> URI_ID;
	private TObjectIntHashMap<String> props_index; // properties index
	private NTree props; // properties map
	private TObjectIntHashMap<String> metadata_index; // metadata index
		
	private String uriIdIndexFile; // uri-id index file
	private String propsIndexFile; // properties index file
	private String metadataIndexFile; // metadata index file

	private SynchronizedCounter counter; // synchronized counter for metadata index
	public static ConcurrentHashMap<String, ConcurrentHashMap<String, CopyOnWriteArrayList<String>>> cache;
	private static Logger logger = LogManager
			.getLogger(RDFTripleExtractor.class.getName());

	
	/**
	 * Constuctor
	 */
	public RDFTripleExtractor(String workingDir, String itemMetadataFile,
			String inputItemURIsFile, String endpoint, String graphURI,
			String tdbDirectory, String datasetFile, Boolean inverseProps,
			Boolean outputTextFormat, Boolean outputBinaryFormat,
			String propsFile, Boolean caching, Boolean append, int nThreads,
			boolean jenatdb) {

		this.workingDir=workingDir;
		this.metadataFile = itemMetadataFile;
		this.append = append;
		this.caching = caching;
		this.datasetFile = datasetFile;
		this.endpoint = endpoint;
		this.graphURI = graphURI;
		this.inputFile = inputItemURIsFile;
		this.tdbDirectory = tdbDirectory;
		this.outputBinaryFormat = outputBinaryFormat;
		this.outputTextFormat = outputTextFormat;
		this.propsFile = propsFile;
		this.inverseProps = inverseProps;
		this.nThreads = nThreads;
		this.jenatdb = jenatdb;

		init();
	}

	private void init() {

		this.model = null;
		this.textFile = this.metadataFile;
		this.propsIndexFile = this.workingDir + "props_index";
		this.metadataIndexFile = metadataFile + "_index";
		this.uriIdIndexFile = workingDir + "input_uri_id";
		
		// load input uri file
		loadInputFile();
		// load properties file
		loadProps();
		// load metadata index
		loadMetadataIndex();

		// if jenatdb is true load local dataset
		if (jenatdb) {
			TDBloading();
			logger.debug("Using TDB.");
		}

		// if caching is true initialize cache hash map
		if (caching) {
			cache = new ConcurrentHashMap<String, ConcurrentHashMap<String, CopyOnWriteArrayList<String>>>();
			logger.debug("Caching enabled.");
		}

	}
	
	/**
	 * Load jena TDB
	 */
	private void TDBloading(){

		logger.info("TDB loading");

		// create model from tdb
		Dataset dataset = TDBFactory.createDataset(tdbDirectory);

		// assume we want the default model, or we could get a named model here
		dataset.begin(ReadWrite.READ);
		model = dataset.getDefaultModel();
		dataset.end() ;

		// if model is null load local dataset into jena TDB
		if(model == null)
			TDBloading(datasetFile);

	}

	/**
	 * Load local dataset into jena TDB
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
	 * Load properties from XML file
	 */
	private void loadProps(){

		props = new NTree();
		props_index = new TObjectIntHashMap<String>();

		try {
			// load properties map from XML file
			XMLUtils.parseXMLFile(propsFile, props_index, props, inverseProps);
			logger.debug("Properties tree loading.");

			// write properties index file
			TextFileUtils.writeData(propsIndexFile, props_index);

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	/**
	 * Load input items file
	 */
	private void loadInputFile(){

		URI_ID = new TObjectIntHashMap<String>();
		// load [uri: id] from input file
		TextFileUtils.loadInputURIs(inputFile, URI_ID, append, uriIdIndexFile);
		logger.debug("Input items loading: " + URI_ID.size() + " URIs loaded.");
	}

	/**
	 * Load metadata index
	 */
	private void loadMetadataIndex(){

		metadata_index = new TObjectIntHashMap<String>();

		if(append){
			TextFileUtils.loadIndex(metadataIndexFile, metadata_index);
			logger.debug("Metadata index loading: " + metadata_index.size() 
				+ " metadata loaded.");
		}

		counter = new SynchronizedCounter(metadata_index.size());

	}


	/**
	 * Run RDF triple extraction
	 */
	public void run(){

		//nThreads = 4;
		logger.debug("Threads number: " + nThreads);

		ExecutorService executor;
		executor = Executors.newFixedThreadPool(nThreads);

		logger.info("Resources to be queried: " + this.URI_ID.size());

		try{
			TextFileManager textWriter = null;
			if(outputTextFormat)
				textWriter = new TextFileManager(textFile, append);

			ItemFileManager fileManager = null;
			if(outputBinaryFormat){
				if(append)
					fileManager = new ItemFileManager(metadataFile, ItemFileManager.APPEND);
				else
					fileManager = new ItemFileManager(metadataFile, ItemFileManager.WRITE);
			}

			for (String uri : this.URI_ID.keySet()) {

				Runnable worker;

				// create worker thread
				worker = new QueryExecutor(uri, URI_ID.get(uri), props, 
						props_index, graphURI, endpoint, counter, metadata_index, 
						textWriter, fileManager, inverseProps, caching, model);
				
				executor.execute(worker);

			}

			// This will make the executor accept no new threads
			// and finish all existing threads in the queue
			executor.shutdown();
			// Wait until all threads are done
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

			if(textWriter!=null)
				textWriter.close();

			if(fileManager!=null)
				fileManager.close();

		}
		catch(Exception e){
			e.printStackTrace();
		}

		// write metadata index file
		TextFileUtils.writeData(metadataIndexFile, metadata_index);

	}

}