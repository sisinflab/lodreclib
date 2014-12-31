package it.poliba.sisinflab.LODRecommender.learning;

import gnu.trove.iterator.TDoubleIterator;
import gnu.trove.list.array.TDoubleArrayList;
import it.poliba.sisinflab.LODRecommender.utils.PropertyFileReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;

public class Learning {
	
	private String training_file;
	private String user_path_index_file;
	private int num_features;
	private FeatureNode[][] training_set;
	private double[] y;
	private Problem problem;
	private Model model;
	private double c;
	private double eps;
	private int solver_type;
	
	private static Logger logger = LogManager.getLogger(Learning.class.getName());
	
	public Learning(){
		
		// load config file
		Map<String, String> prop = null;
				
		try {
			
			prop = PropertyFileReader.loadProperties("config.properties");
			this.solver_type = Integer.parseInt(prop.get("solverType"));
			this.c = Double.parseDouble(prop.get("c"));
			this.eps = Double.parseDouble(prop.get("eps"));
			this.training_file = "training_set";
			this.user_path_index_file = "user_path_index";
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		prop.clear();
		
		countNumFeatures();
		training_set = loadDataset();
		y = loadLabels();
		createProblem();
		
	}
	
	private void countNumFeatures(){
		
		try{
			
			int count = 0;
			
			BufferedReader br = new BufferedReader(new FileReader(user_path_index_file));
			while(br.readLine()!=null)
				count++;
			
			num_features = count;
			br.close();
			
		}
		catch(Exception e){
			e.printStackTrace();
			num_features = 0;
		}
		
	}
    
    
    private FeatureNode[][] loadDataset(){
    	
    	logger.info("Loading dataset");
    	
    	try{
			BufferedReader br = new BufferedReader(new FileReader(training_file));
			ArrayList<FeatureNode[]> ff = new ArrayList<FeatureNode[]>();
			
			String line = null;
			while((line=br.readLine()) != null){
				
				String[] vals = line.split(" ");
				FeatureNode[] f = new FeatureNode[vals.length-3];
				for(int i = 3; i < vals.length; i++){
					String[] ss = vals[i].split(":");
					int key = Integer.parseInt(ss[0]);
					double value = Double.parseDouble(ss[1]);
					f[i-3] = new FeatureNode(key, value);
				}
				
				ff.add(f);
			}
			
			br.close();
			
			FeatureNode[][] training_set = new FeatureNode[ff.size()][];
			int i = 0;
			for(FeatureNode[] n : ff){
				training_set[i] = new FeatureNode[n.length];
				training_set[i] = n;
				i++;
				
			}
			
			return training_set;
			
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
    	
    }
    
    private double[] loadLabels(){
    	
    	logger.info("Loading labels");
    	
    	try{
			BufferedReader br = new BufferedReader(new FileReader(training_file));
			TDoubleArrayList ff = new TDoubleArrayList();
			String line = null;
			
			while((line=br.readLine()) != null){
				String[] vals = line.split(" ");
				ff.add(Double.parseDouble(vals[0]));
			}
			
			br.close();
			
			double[] res = new double[ff.size()];
			int i = 0;
			
			TDoubleIterator it = ff.iterator();
			while(it.hasNext()){
				res[i] = it.next();
				i++;
			}
			
			return res;
			
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
    	
    }
    
    private void createProblem(){
    	
    	logger.info("Creating problem");
    	
    	problem = new Problem();
    	problem.l = training_set.length - 1; // number of training examples
		problem.n = num_features + 1; // number of features
		problem.x = training_set; // feature nodes
		problem.y = y; // target values
		
		logger.info("Number of training examples: " + problem.l);
		logger.info("Number of features: " + problem.n);
    	
    }
    
    public void start(){
    	
    	logger.info("Start training");
    	logger.info("Solver type: " + SolverType.getById(solver_type));
    	logger.info("c: " + c);
    	logger.info("eps: " + eps);
    	
    	SolverType solver = SolverType.getById(solver_type);
		
		try{
			Parameter parameter = new Parameter(solver, c, eps);
			model = Linear.train(problem, parameter);
			File modelFile = new File("model");
			model.save(modelFile);
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
		
		long start = System.currentTimeMillis();
		
		Learning l = new Learning();
		l.start();
		
		long stop = System.currentTimeMillis();
		logger.info("Learning terminated in [sec]: " 
				+ ((stop - start) / 1000));


	}

}
