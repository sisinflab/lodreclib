package it.poliba.sisinflab.LODRec.utils;

import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import it.poliba.sisinflab.LODRec.tree.NNode;
import it.poliba.sisinflab.LODRec.tree.NTree;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class XMLUtils {
	
	public static void parseXMLFile(String file, TObjectIntHashMap<String> props_index, NTree props_tree, 
			boolean directed) throws ParserConfigurationException, SAXException, IOException{
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(new File(file));
		Node root = doc.getFirstChild();
		
		HashSet<String> props = new HashSet<String>();
		NNode root_node = new NNode("props");
		walkTree(root, root_node, props, directed);
		props_tree.addRoot(root_node);
		
		int index=0;
		
		for(String s : props)
			props_index.put(s, index++);
		
		props.clear();
		
		if(root.hasAttributes()){
			
			int lev = Integer.parseInt(root.getAttributes().item(0).getNodeValue());
			if(lev>1){
				ArrayList<NNode> props_to_reply = new ArrayList<NNode>();
				
				for(NNode node : root_node.getChilds())
					props_to_reply.add(new NNode(node.getValue()));
				
				constructTree(root_node.getChilds(), props_to_reply, lev-1);
			}
		}
		
		
	}
	
	public static void constructTree(ArrayList<NNode> nodes_to_expand, ArrayList<NNode> props_to_reply, 
			int lev){
		
		
		for(NNode n : nodes_to_expand)
				n.addChilds(constructBranches(props_to_reply));
		
		lev--;
		if(lev>0){
			
			ArrayList<NNode> new_nodes_to_expand = new ArrayList<NNode>();
			for(NNode n : nodes_to_expand)
				new_nodes_to_expand.addAll(n.getChilds());
			
			constructTree(new_nodes_to_expand, props_to_reply, lev);
			
		}
			
		
	}
	
	public static ArrayList<NNode> constructBranches(ArrayList<NNode> props_to_reply){
		
		ArrayList<NNode> res = new ArrayList<NNode>();
		
			for(NNode n : props_to_reply)
				res.add(new NNode(n.getValue()));
		
		return res;
		
	}
	
	public static void walkTree(Node node, NNode nnode, HashSet<String> props, boolean directed){
		
		if(node.hasChildNodes()){
			
			ArrayList<NNode> childs = new ArrayList<NNode>();
			
			for(int i=0; i<node.getChildNodes().getLength();i++){
	        
				if(node.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE){
					
					String p = new String(node.getChildNodes().item(i).getAttributes().item(0).getNodeValue());
					
					props.add(p);
					if(directed)
						props.add("inv_" + p);
					
					NNode n = new NNode(p);
					walkTree(node.getChildNodes().item(i), n, props, directed);
					childs.add(n);
				}
			}
			
			nnode.addChilds(childs);
	    }
	}
}
