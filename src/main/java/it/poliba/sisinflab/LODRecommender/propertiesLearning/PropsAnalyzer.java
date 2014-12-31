package it.poliba.sisinflab.LODRecommender.propertiesLearning;

import it.poliba.sisinflab.LODRecommender.fileManager.ItemFileManager;
import it.poliba.sisinflab.LODRecommender.fileManager.StringFileManager;
import it.poliba.sisinflab.LODRecommender.itemManager.ItemTree;
import it.poliba.sisinflab.LODRecommender.utils.ItemUtils;
import it.poliba.sisinflab.LODRecommender.utils.PropertyFileReader;
import it.poliba.sisinflab.LODRecommender.utils.StringUtils;
import it.poliba.sisinflab.LODRecommender.utils.TextFileUtils;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class PropsAnalyzer {
	
	private String items_file;
	private String paths_file;
	private TIntObjectHashMap<String> props_index; // property index
	private TIntObjectHashMap<String> paths_index; // path index
	private THashMap<String, ArrayList<String>> results;
	
	
	public PropsAnalyzer(){
		
		// load config file
		Map<String, String> prop = null;
				
		try {
			prop = PropertyFileReader.loadProperties("config.properties");
			this.items_file = prop.get("itemsFile");
			this.paths_file = prop.get("outputPathFile");
			loadPropsIndex();
			loadPathsIndex();
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
	
	private void loadPathsIndex(){
		paths_index = new TIntObjectHashMap<String>();
		TextFileUtils.loadIndex("path_index", paths_index);
	}
	
	private void analyze(){
		
		results = new THashMap<String, ArrayList<String>>();
		DecimalFormat form = new DecimalFormat("0.000000"); 
		
		try{
			
			ItemFileManager itemReader = new ItemFileManager(items_file, ItemFileManager.READ);
			StringFileManager pathReader = new StringFileManager(paths_file, ItemFileManager.READ);
			
			ArrayList<String> paths_id = new ArrayList<String>(pathReader.getKeysIndex());
			
			for(String item_pair : paths_id){
				
				String[] items = item_pair.split("-");
				String paths = pathReader.read(item_pair);
				
				ItemTree item_a = itemReader.read(items[0]);
				ItemTree item_b = itemReader.read(items[1]);
				//System.out.println(item_a.getItemId() + "-" + item_b.getItemId());
				
				for(String s : paths.split(",")){
					
					String path = paths_index.get(Integer.parseInt(s.split("=")[0]));
					int path_freq = Integer.parseInt(s.split("=")[1]);
					//System.out.println(path + ": " + path_freq);
					
					// scartiamo items direttamente collegati
					if(path.split("#").length>1){
						String path_a = path.split("#")[0];
						String path_b = StringUtils.reverse(path.split("#")[1]);
						
						int freq_a = item_a.getBranches().get(path_a).size();
						int freq_b = item_b.getBranches().get(path_b).size();
						
						//System.out.println(path_a + ": " + freq_a);
						//System.out.println(path_b + ": " + freq_b);
						
						results.putIfAbsent(path_a, new ArrayList<String>());
						results.get(path_a).add(path_freq + ":" + freq_a*freq_b);
						results.putIfAbsent(path_b, new ArrayList<String>());
						results.get(path_b).add(path_freq + ":" + freq_a*freq_b);
						
					}
				}
				
			}
			
			itemReader.close();
			pathReader.close();
			
			HashMap<String, Float> path_probability = new HashMap<String, Float>();
			
			
			for(String s : results.keySet()){
				
				int freq = 0;
				long norm = 0;
				
				System.out.println(s + ": " + results.get(s));
				
				for(String ss : results.get(s)){
					
					freq += Integer.parseInt(ss.split(":")[0]);
					norm += Long.parseLong(ss.split(":")[1]);
					
				}
				
				float f = (float) freq/norm;
				path_probability.put(s, f);
			}
			
			Map<String, Float> sorted_paths = ItemUtils.sortByValues(path_probability);
			
			int i = 0;
			for(String p : sorted_paths.keySet()){
				
				if(i < 50){
					
					String[] props = p.split("-");
					String props_label = "";
					
					for(String pp : props)
						props_label += props_index.get(Integer.parseInt(pp)) + "-";
					
					System.out.println(props_label.substring(0, props_label.length()-1) 
							+ ": " + form.format(sorted_paths.get(p)));
					i++;
				}
				else
					break;
			}
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		PropsAnalyzer pa = new PropsAnalyzer();
		pa.analyze();
		
	}

}
