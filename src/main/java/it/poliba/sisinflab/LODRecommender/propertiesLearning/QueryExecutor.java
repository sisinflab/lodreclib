package it.poliba.sisinflab.LODRecommender.propertiesLearning;

import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.THashSet;
import it.poliba.sisinflab.LODRecommender.fileManager.ItemFileManager;
import it.poliba.sisinflab.LODRecommender.fileManager.TextFileManager;
import it.poliba.sisinflab.LODRecommender.itemManager.ItemTree;
import it.poliba.sisinflab.LODRecommender.itemManager.PropertyIndexedItemTree;
import it.poliba.sisinflab.LODRecommender.utils.SynchronizedCounter;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * This class is part of the LOD Recommender
 * 
 * This class is used by RDFTripleExtractor for multi-threading RDF triples extraction 
 * 
 * @author Vito Mastromarino
 */
public class QueryExecutor implements Runnable {
	
	private String uri; // uri resource
	private TObjectIntHashMap<String> props_index; // property index
	private String graphURI; // graph uri
	private String endpoint; // endpoint address
	private SynchronizedCounter metadata_counter; // synchronized counter for metadata index
	private SynchronizedCounter properties_counter;
	private TObjectIntHashMap<String> metadata_index; // metadata index
	private Model model; // local dataset model
	private ItemTree itemTree; // item tree
	private ItemFileManager fileManager;
	private int depth;
	private TextFileManager textWriter;
		
	private static Logger logger = LogManager.getLogger(QueryExecutor.class.getName());
	
	
	/**
	 * Constuctor
	 */
	public QueryExecutor(String uri, int uri_id,  
			TObjectIntHashMap<String> props_index, String graphURI, String endpoint, 
			SynchronizedCounter metadata_counter, SynchronizedCounter properties_counter, 
			TObjectIntHashMap<String> metadata_index, TextFileManager textWriter, 
			ItemFileManager fileManager, int depth){
		this.uri = uri;
		this.props_index = props_index;
		this.graphURI = graphURI;
		this.endpoint = endpoint;
		this.metadata_counter = metadata_counter;
		this.properties_counter = properties_counter;
		this.metadata_index = metadata_index;
		this.model = null;
		this.fileManager = fileManager;
		this.itemTree = new PropertyIndexedItemTree(uri_id);
		this.depth = depth;
		this.textWriter = textWriter;
	}
	
	/**
	 * Constuctor for local dataset query
	 */
	public QueryExecutor(String uri, int uri_id, TObjectIntHashMap<String> props_index, 
			String graphURI, String endpoint, SynchronizedCounter metadata_counter, 
			SynchronizedCounter properties_counter, TObjectIntHashMap<String> metadata_index, 
			TextFileManager textWriter, ItemFileManager fileManager,int depth, Model model){

		this(uri, uri_id, props_index, graphURI, endpoint, metadata_counter, properties_counter, 
				metadata_index, textWriter, fileManager, depth);
		this.model = model;
	}
	
	/**
	 * Start RDF triple extraction
	 */
	public void run(){
		
		//System.out.println("Start query for resource: " + uri);
		logger.info(uri + ": start data extraction");
		
		long start = System.currentTimeMillis();
				
		exec("", uri, 0);
		
		//qexec.close();
		
		if(itemTree.size()>0){
			
			// text file writing
			if(textWriter != null)
				textWriter.write(itemTree.serialize()); 
			
			// binary file writing
			if(fileManager != null)
				fileManager.write(itemTree);
		}
		
		long stop = System.currentTimeMillis();
		logger.info(uri + ": data extraction terminated in [sec]: " 
				+ ((stop - start) / 1000));
		
	}
	
	
	/**
	 * Execute RDF triple extraction
	 */
	private void exec(String list_props, String uri, int d){
		
		if(d < depth){
			
			d++;
		
			THashMap<String, THashSet<String>> result = new THashMap<String, THashSet<String>>();
			result.putAll(runQuery(uri));
			
			if(result.size()>0){
				
				for(String prop : result.keySet()){
					
					String p_index = String.valueOf(extractPropertyKey(prop));
					
					//logger.info(prop + ": " + result.get(prop).size());
					
					for(String uri_res : result.get(prop)){
						
						if(list_props.length()>0){
							itemTree.addBranches(list_props + "-" + p_index, extractMetadataKey(uri_res));
							exec(list_props + "-" + p_index, uri_res, d);
						}
						else{
							itemTree.addBranches(p_index, extractMetadataKey(uri_res));
							exec(p_index, uri_res, d);
						}
						
					}
					
				}
				
			}
		
		}
		
	}
	
	
	
	
	/**
	 * Run SPARQL query 
	 * @param     uri  uri resource
	 * @param     p  property
	 * @return    results map: uri-s (if uri is a subject), uri-o (if uri is an object)
	 */
	private THashMap<String, THashSet<String>> runQuery(String uri){
		
		THashMap<String, THashSet<String>> ret = new THashMap<String, THashSet<String>>();
		
		Query query;
		String q;
		
		q = "SELECT * WHERE {{?s ?p <" + uri + ">. FILTER isIRI(?s). } UNION " +
							"{<" + uri + "> ?p ?o ." + " FILTER isIRI(?o). }} ";
		
		logger.debug(q);
		
		try 
		{	
			query = QueryFactory.create(q);
			ret = executeQuery(query);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}

		return ret;
	}
	
	/**
	 * Execute SPARQL query 
	 * @param     query  sparql query
	 * @param     p  property
	 * @return    results map: uri-s (if uri is a subject), uri-o (if uri is an object)
	 */
	private THashMap<String, THashSet<String>> executeQuery(Query query) {
		
		THashMap<String, THashSet<String>> ret = new THashMap<String, THashSet<String>>();
		QueryExecution qexec = null;
		
		if(model==null){
			if(graphURI == null)
				qexec = QueryExecutionFactory.sparqlService(endpoint, query); // remote query
			else
				qexec = QueryExecutionFactory.sparqlService(endpoint, query, graphURI); // remote query
		}	
		else
			qexec = QueryExecutionFactory.create(query, model); // local query
			
		try{
		
			ResultSet results = ResultSetFactory.copyResults(qexec.execSelect());
			
			QuerySolution qs;
			String n;
			String p;
			
			while(results.hasNext()){
				
				qs = results.next();
				p = qs.get("p").toString();
				
				ret.putIfAbsent(p, new THashSet<String>());
				
				if (qs.get("o") == null)
					n = qs.get("s").toString();
				else
					n = qs.get("o").toString();
				
				if (!p.contains("type"))
					ret.get(p).add(n); 
				else 
				{
					if (n.contains("yago"))
						ret.get(p).add(n);
				}
				
			}
		}
		
		catch(Exception e){
			e.printStackTrace();
		}
		finally{
			qexec.close();
		}
		
		return ret;
		
	}
	
	/**
	 * Extract key from metadata index
	 * @param     s  string to index
	 * @return    index of s
	 */
	private int extractMetadataKey(String s) {
		synchronized (metadata_index) {
			
			if(metadata_index.containsKey(s)){
				return metadata_index.get(s);
			}
			else{
				int id = metadata_counter.value();
				metadata_index.put(s, id);
				return id;
			}

		}

	}
	
	/**
	 * Extract key from metadata index
	 * @param     s  string to index
	 * @return    index of s
	 */
	private int extractPropertyKey(String s) {
		synchronized (props_index) {
			
			if(props_index.containsKey(s)){
				return props_index.get(s);
			}
			else{
				int id = properties_counter.value();
				props_index.put(s, id);
				return id;
			}

		}

	}

}
