package it.poliba.sisinflab.LODRec.utils;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Arrays;
import java.util.Collections;

public class StringUtils {

	
	public static String extractRate(float rate, float ratesThreshold){
		
		if(rate > ratesThreshold)
			return "r1";
		else
			return "r0";
	}
	
	public static String reverse(String ss) {
		
		String[] vals = ss.split("-");
		Collections.reverse(Arrays.asList(vals));
		
		String res = "";
		
		for(String s : vals){
			res += s.trim() + "-";
		}
		
		return res.substring(0, res.length()-1);
	}
	
	public static String reverseDirected(String ss, TIntObjectHashMap<String> props_index){
		
		String p = reverse(ss);
		
		String[] vals = p.split("-");
		
		String out = "";
		String prop = "";
		String to_search = "";
		
		for(String s : vals){
			
			prop = props_index.get(Integer.parseInt(s));
			to_search = "";
			if(prop.startsWith("inv_"))
				to_search = prop.substring(4);
			else
				to_search = "inv_" + prop;
			
			for(int i : props_index.keys()){
				if(props_index.get(i).equals(to_search))
					out += i + "-";
			}
			
		}
		
		return out.substring(0, out.length()-1);
		
	}
	
}
