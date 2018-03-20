package uk.gov.dvla.osg.calclocation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import uk.gov.dvla.osg.common.classes.Customer;
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
	private static PostageConfiguration postageConfig;
	//Argument Strings
	private static String inputFile, outputFile, propsFile, runNo;
	static int tenDigitJid;
	static int eightDigitJid;
	private static InsertLookup insertLookup;
	private static EnvelopeLookup envelopeLookup;

	public static void main(String[] args) throws Exception {

		LOGGER.info("Starting uk.gov.dvla.osg.batch.Main");
		// assign & validate command line args
		LOGGER.trace("Assigning Args...");
		assignArgs(args);
		// load the Application Configuration file
		LOGGER.trace("Loading AppConfig...");
		AppConfig appConfig = new AppConfig(propsFile);
		// load customers from dpf file
		LOGGER.trace("Initialising DPF Parser...");
		DpfParser dpf = new DpfParser(inputFile, outputFile, appConfig);
		LOGGER.trace("Loading customers...");
		ArrayList<Customer> customers = dpf.Load();
		// Load Selector Lookup & Production Config files
		LOGGER.trace("Loading Lookup Files...");
		loadLookupFiles(appConfig, customers);
		PapersizeLookup pl = new PapersizeLookup(appConfig.getPapersizeLookup());
		
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
		BatchEngine be = new BatchEngine(postageConfig, tenDigitJid, eightDigitJid, appConfig, pl, envelopeLookup, insertLookup);
		be.batch(customers);
		
		LOGGER.trace("Creating UkMail Resources..."); 
		CreateUkMailResources ukm = new CreateUkMailResources(customers, postageConfig, runNo, "MM"); 
		ukm.method(); 
		
		// Return to original order to map records row by row
		LOGGER.trace("Sorting back to original order...");
		sortCustomers(customers, new CustomerComparatorOriginalOrder());
		// Dpf saves the changed details to the output file
		LOGGER.trace("Saving DPF file...");
		dpf.Save(customers, insertLookup);
	}

	private static void assignArgs(String[] args) {
		if (args.length != EXPECTED_NO_OF_ARGS) {
			LOGGER.fatal(
					"Incorrect number of args parsed '{}' expecting '{}'. Args are 1.input file, 2.output file, 3.props file, 4.jobId, 5.Runno 6.ParentJid.",
					args.length, EXPECTED_NO_OF_ARGS);
			System.exit(1);
		}
		propsFile = args[0];
		if (!(new File(propsFile).exists())) {
			LOGGER.fatal("File '{}' doesn't exist", propsFile);
			System.exit(1);
		}

		inputFile = args[1];
		if (!(new File(inputFile).exists())) {
			LOGGER.fatal("File '{}' doesn't exist", inputFile);
			System.exit(1);
		}

		outputFile = args[2];
		runNo = args[3];
		eightDigitJid = Integer.parseInt(args[4]);
		tenDigitJid = Integer.parseInt(args[5]);
	}

/*	private static AppConfig loadPropertiesFile() throws Exception {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
		return mapper.readValue(new File(propsFile), AppConfig.class);
	}*/

	private static void loadLookupFiles(AppConfig appConfig, ArrayList<Customer> customers) throws Exception {
		String selectorRef = getSelectorRef(customers);
		SelectorLookup lookup = new SelectorLookup(appConfig.getLookupFile());
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
		
		ProductionConfiguration.init(appConfig.getProductionConfigPath() + lookup.get(selectorRef).getProductionConfig()
				+ appConfig.getProductionFileSuffix());
		
		postageConfig = new PostageConfiguration(appConfig.getPostageConfigPath()
				+ lookup.get(selectorRef).getPostageConfig() + appConfig.getPostageFileSuffix());

		PresentationConfiguration.init(appConfig.getPresentationPriorityConfigPath() + lookup.get(selectorRef).getPresentationConfig()
						+ appConfig.getPresentationPriorityFileSuffix());
		insertLookup = new InsertLookup(appConfig.getInsertLookup());
		envelopeLookup = new EnvelopeLookup(appConfig.getEnvelopeLookup());
	}

	private static void sortCustomers(ArrayList<Customer> list, Comparator comparator) {
		try {
			Collections.sort(list, comparator);
		} catch (Exception e) {
			LOGGER.fatal("Error when sorting: '{}'", e);
			System.exit(1);
		}
	}

	private static String getSelectorRef(ArrayList<Customer> customers) {
		return customers.get(0).getSelectorRef();
	}
}
