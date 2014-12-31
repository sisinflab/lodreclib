package it.poliba.sisinflab.LODRecommender.graphEmbedding;

import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import it.poliba.sisinflab.LODRecommender.fileManager.ItemFileManager;
import it.poliba.sisinflab.LODRecommender.itemManager.ItemTree;
import it.poliba.sisinflab.LODRecommender.utils.MemoryMonitor;
import it.poliba.sisinflab.LODRecommender.utils.PropertyFileReader;
import it.poliba.sisinflab.LODRecommender.utils.TextFileUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class BranchesMapping {
	
	private String items_file;
	private TIntObjectHashMap<String> props_index; // property index
	private TIntObjectHashMap<String> metadata_index; // metadata index
	
	private static Logger logger = LogManager.getLogger(BranchesMapping.class.getName());
	
	public BranchesMapping(){
		
		// load config file
		Map<String, String> prop = null;
		
		try {
			prop = PropertyFileReader.loadProperties("config.properties");
			this.items_file = prop.get("itemsFile");
			loadPropsIndex();
			loadMetadataIndex();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		prop.clear();
		
	}
	
	private void loadPropsIndex(){
		props_index = new TIntObjectHashMap<String>();
		TextFileUtils.loadIndex("props_index", props_index);
	}
	
	private void loadMetadataIndex(){
		metadata_index = new TIntObjectHashMap<String>();
		TextFileUtils.loadIndex("metadata_index", metadata_index);
	}
	
	private void mapping(){
		
		try{
			
			BufferedWriter br = new BufferedWriter(new FileWriter("metadata_branches"));
			
			ItemFileManager itemReader = new ItemFileManager(items_file, ItemFileManager.READ);
			ArrayList<String> items_id = new ArrayList<String>(itemReader.getKeysIndex());
			
			THashMap<String, TIntIntHashMap> branches = null;
			ItemTree item = null;
			StringBuffer str = null;
			TObjectIntHashMap<String> res = null;
			
			for(String item_id : items_id){
				
				res = new TObjectIntHashMap<String>();
				item = itemReader.read(item_id);
				
				branches = item.getBranches();
				
				logger.info("Mapping item " + item_id);
				
				str = new StringBuffer();
				str.append(item_id + "\t");
				
				for(String s : branches.keySet()){
					
					if(!isPrefix(s, branches.keySet())){
					
						String b[] = s.split("-");
						
						for(int i = 0; i < b.length; i++){
							
							String path = "";
							String features = "";
							
							for(int j = i; j < b.length; j++){
								
								String[] bb = b[j].split("#");
								String[] p = props_index.get(Integer.parseInt(bb[0])).split("/");
								String[] f = metadata_index.get(Integer.parseInt(bb[1])).split("/");
								path += p[p.length-1].replaceAll("[{'.,;:/\\-}]", "_") + "-" + 
								f[f.length-1].replaceAll("[{'.,;:/\\-}]", "_") + "-";
								features += f[f.length-1].replaceAll("[{'.,;:/\\-}]", "_") + "-";
								
//								path += bb[0] + "-" + bb[1] + "-";
//								features += bb[1] + "-";
								
							}
							
							res.adjustOrPutValue(path.substring(0, path.length()-1), 1, 1);
							res.adjustOrPutValue(features.substring(0, features.length()-1), 1, 1);
							
						}
					
					}
					
				}
					
				for(String s : res.keySet())
					str.append(s + ":" + res.get(s) + " ");
				
				br.append(str);
				br.newLine();
				
			}
			
			br.flush();
			br.close();
			
			itemReader.close();
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	private boolean isPrefix(String s, Set<String> list){
		
		//System.out.println(s + "-" + list);
		
		for(String ss : list){
			
			if(!ss.contentEquals(s) && ss.startsWith(s)){
				//System.out.println(s + " - " + ss);
				return true;
			}
		}
		
		return false;
		
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		BranchesMapping bm = new BranchesMapping();
		
		long start = System.currentTimeMillis();
		
		bm.mapping();
		
		long stop = System.currentTimeMillis();
		logger.info("Conversion terminated in [sec]: " 
				+ ((stop - start) / 1000));
		
		MemoryMonitor.stats();
		
	}

}
