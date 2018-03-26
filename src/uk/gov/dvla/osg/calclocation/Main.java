package uk.gov.dvla.osg.calclocation;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.gov.dvla.osg.common.classes.Customer;
import uk.gov.dvla.osg.common.classes.Selector;
import uk.gov.dvla.osg.common.config.EnvelopeLookup;
import uk.gov.dvla.osg.common.config.InsertLookup;
import uk.gov.dvla.osg.common.config.PapersizeLookup;
import uk.gov.dvla.osg.common.config.PostageConfiguration;
import uk.gov.dvla.osg.common.config.PresentationConfiguration;
import uk.gov.dvla.osg.common.config.ProductionConfiguration;
import uk.gov.dvla.osg.common.config.SelectorLookup;
import uk.gov.dvla.osg.ukmail.resources.CreateUkMailResources;

public class Main {

	private static final Logger LOGGER = LogManager.getLogger();
	//private static AppConfig appConfig;
	private static final int EXPECTED_NO_OF_ARGS = 6;
	//Argument Strings
	private static String inputFile, outputFile, propsFile, runNo;
	private static int tenDigitJid;
	private static int eightDigitJid;
	
	public static void main(String[] args) throws Exception {

		LOGGER.info("Starting uk.gov.dvla.osg.batch.Main");
		// assign & validate command line args
		LOGGER.trace("Assigning Args...");
		assignArgs(args);
		// load the Application Configuration file
		LOGGER.trace("Loading AppConfig...");
		AppConfig.init(propsFile);
		// load customers from dpf file
		LOGGER.trace("Initialising DPF Parser...");
		DpfParser dpf = new DpfParser(inputFile, outputFile);
		LOGGER.trace("Loading customers...");
		ArrayList<Customer> customers = dpf.Load();
		// Load Selector Lookup & Production Config files
		LOGGER.trace("Loading Lookup Files...");
		loadLookupFiles(customers);
		// Sort Order: Language -> Presentation Priority
		LOGGER.trace("Sorting input...");
		sortCustomers(customers, new CustomerComparator());
		// Calculate sites for every customer
		LOGGER.trace("Starting CalcLocation...");
		LocationCalculator calculateLocation = new LocationCalculator();
		LOGGER.trace("Running calculate...");
		calculateLocation.calculate(customers);
		/*
		 * Sort order: 
		 * LOCATION -> LANGUAGE -> STATIONERY -> PRESENTATION_ORDER -> 
		 * SUB_BATCH -> SORT_FIELD -> FLEET_NO -> MSC -> GRP_ID
		 */
		LOGGER.trace("Sorting input...");
		sortCustomers(customers, new CustomerComparatorWithLocation());
		// Calculate EOGs ready for the batch engine
		LOGGER.trace("Calculating EOGs...");
		CalculateEndOfGroups eogs = new CalculateEndOfGroups();
		eogs.calculate(customers);
		/*
		 * Sort order: 
		 * LOCATION -> LANGUAGE -> STATIONERY -> PRESENTATION_ORDER ->
		 *  SUB_BATCH -> SORT_FIELD -> FLEET_NO -> MSC -> GRP_ID
		 */
		LOGGER.trace("Sorting input...");
		sortCustomers(customers, new CustomerComparatorWithLocation());
		// Putting into batches that are above the 25 tray minimum
		LOGGER.trace("Running Batch Engine...");
		BatchEngine be = new BatchEngine(tenDigitJid, eightDigitJid);
		be.batch(customers);
	
		LOGGER.trace("Creating UkMail Resources..."); 
		CreateUkMailResources ukm = new CreateUkMailResources(customers, runNo, customers.get(0).getProduct()); 
		ukm.method(); 
		
		// Return to original order to map records row by row
		LOGGER.trace("Sorting back to original order...");
		sortCustomers(customers, new CustomerComparatorOriginalOrder());
		// Dpf saves the changed details to the output file
		LOGGER.trace("Saving DPF file...");
		dpf.Save(customers);
		LOGGER.trace("Data saved to: {}", outputFile);
	}

	private static void assignArgs(String[] args) {
		if (args.length != EXPECTED_NO_OF_ARGS) {
			LOGGER.fatal(
					"Incorrect number of args parsed '{}' expecting '{}'. Args are 1.input file, 2.output file, 3.props file, 4.jobId, 5.Runno 6.ParentJid.",
					args.length, EXPECTED_NO_OF_ARGS);
			System.exit(1);
		}
		
		inputFile = args[0];
		if (!(new File(inputFile).exists())) {
			LOGGER.fatal("File '{}' doesn't exist", inputFile);
			System.exit(1);
		}

		outputFile = args[1];
		if (!Files.isWritable(Paths.get(outputFile))) {
			LOGGER.fatal("Unable to create output file in filepath {}", outputFile);
			System.exit(1);
		}
		
		propsFile = args[2];
		if (!(new File(propsFile).exists())) {
			LOGGER.fatal("File '{}' doesn't exist", propsFile);
			System.exit(1);
		}
		runNo = args[3];
		eightDigitJid = Integer.parseInt(args[4]);
		tenDigitJid = Integer.parseInt(args[5]);
	}

/*	private static AppConfig loadPropertiesFile() throws Exception {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
		return mapper.readValue(new File(propsFile), AppConfig.class);
	}*/

	private static void loadLookupFiles(ArrayList<Customer> customers) throws Exception {
		
		AppConfig appConfig = AppConfig.getInstance();
		
		SelectorLookup.init(appConfig.getLookupFile());
		Selector selector = SelectorLookup.getInstance().getLookup().get(customers.get(0).getSelectorRef());
		
		ProductionConfiguration.init(appConfig.getProductionConfigPath() + selector.getProductionConfig()
				+ appConfig.getProductionFileSuffix());
		
		PostageConfiguration.init(appConfig.getPostageConfigPath()
				+ selector.getPostageConfig() + appConfig.getPostageFileSuffix());

		PresentationConfiguration.init(appConfig.getPresentationPriorityConfigPath() 
						+ selector.getPresentationConfig()
						+ appConfig.getPresentationPriorityFileSuffix());
		
		InsertLookup.init(appConfig.getInsertLookup());
		EnvelopeLookup.init(appConfig.getEnvelopeLookup());
		PapersizeLookup.init(appConfig.getPapersizeLookup());
	}

	private static void sortCustomers(ArrayList<Customer> list, Comparator comparator) {
		try {
			Collections.sort(list, comparator);
		} catch (Exception e) {
			LOGGER.fatal("Error when sorting: '{}'", e);
			System.exit(1);
		}
	}
}
