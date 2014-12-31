package it.poliba.sisinflab.LODRecommender.main;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import it.poliba.sisinflab.LODRecommender.itemPathExtractor.ItemPathExtractor;
import it.poliba.sisinflab.LODRecommender.learning.Learning;
import it.poliba.sisinflab.LODRecommender.learning.Predictor;
import it.poliba.sisinflab.LODRecommender.sparqlDataExtractor.RDFTripleExtractor;
import it.poliba.sisinflab.LODRecommender.userPathExtractor.UserPathExtractor;

public class Main {

	private static Logger logger = LogManager
			.getLogger(RDFTripleExtractor.class.getName());

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		boolean dataExtraction = false;
		boolean itemPathExtraction = true;
		boolean userPathExtraction = true;
		boolean learning = true;
		boolean predict = true;

		long start, stop;

		// RDFTripleExtractor

		if (dataExtraction) {

			RDFTripleExtractor m = new RDFTripleExtractor();

			start = System.currentTimeMillis();

			m.run();

			stop = System.currentTimeMillis();
			logger.info("Finished all threads. Data extraction terminated in [sec]: "
					+ ((stop - start) / 1000));

		}

		// ItemPathExtractor

		if (itemPathExtraction) {

			ItemPathExtractor pe = new ItemPathExtractor();

			start = System.currentTimeMillis();

			pe.start();

			stop = System.currentTimeMillis();
			logger.info("Item paths extraction terminated in [sec]: "
					+ ((stop - start) / 1000));

		}

		// UserPathExtractor

		if (userPathExtraction) {

			UserPathExtractor upe = new UserPathExtractor();

			start = System.currentTimeMillis();

			upe.start();

			stop = System.currentTimeMillis();
			logger.info("User path extraction terminated in [sec]: "
					+ ((stop - start) / 1000));

		}

		// Learning

		if (learning) {

			start = System.currentTimeMillis();

			Learning l = new Learning();
			l.start();

			stop = System.currentTimeMillis();
			logger.info("Learning terminated in [sec]: "
					+ ((stop - start) / 1000));

		}

		// Predictor

		if (predict) {

			start = System.currentTimeMillis();

			Predictor p = new Predictor();
			p.predict();

			stop = System.currentTimeMillis();
			logger.info("Prediction terminated in [sec]: "
					+ ((stop - start) / 1000));

		}

	}

}
