package it.poliba.sisinflab.LODRecommender.tree;

public class NTree {
	
	private NNode root;
	
	public NTree(){
		root = null;
	}
	
	public boolean isEmpty(){
		if(root==null)
			return false;
		else
			return true;
	}
	
	public void addRoot(NNode root){
		this.root = root;
	}
	
	public NNode getRoot(){
		return root;
	}
	
	public void stampa() {
        stampa(root, 0);
    }

    private void stampa(NNode v, int level) {
        
    	if (v == null) 
        	return;
        
    	for (int i = 0; i < level - 1; i++)
            System.out.print("   ");
        
    	if (level > 0)
            System.out.print(" |--");
    	
        System.out.println(v.getValue());
        
        for (NNode children : v.getChilds()) {
            stampa(children, level + 1);
        }
    }

}
