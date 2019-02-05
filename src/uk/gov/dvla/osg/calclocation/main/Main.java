package uk.gov.dvla.osg.calclocation.main;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.gov.dvla.osg.calclocation.comparators.CustomerComparator;
import uk.gov.dvla.osg.calclocation.comparators.CustomerComparatorOriginalOrder;
import uk.gov.dvla.osg.calclocation.comparators.CustomerComparatorWithLocation;
import uk.gov.dvla.osg.calclocation.engine.BatchEngine;
import uk.gov.dvla.osg.calclocation.location.LocationCalculator;
import uk.gov.dvla.osg.calclocation.methods.CalculateEndOfGroups;
import uk.gov.dvla.osg.calclocation.methods.TotalPagesInGroup;
import uk.gov.dvla.osg.common.classes.Customer;
import uk.gov.dvla.osg.common.classes.Selector;
import uk.gov.dvla.osg.common.config.*;
import uk.gov.dvla.osg.common.enums.BatchType;
import uk.gov.dvla.osg.royalmail.resources.CreateRoyalMailResources;
import uk.gov.dvla.osg.ukmail.resources.CreateUkMailResources;

public class Main {

    private static final Logger LOGGER = LogManager.getLogger();
    //private static AppConfig appConfig;
    private static final int EXPECTED_NO_OF_ARGS = 6;
    //Argument Strings
    private static String applicationConfigFile;
    private static String inputDpfFile;
    private static String outputDpfFile;    
    private static String runNo;
    private static int tenDigitJid;
    private static int eightDigitJid;

    public static void main(String[] args) throws Exception {
        LOGGER.info("---- CalcLocation Started ----");

        try {
            // assign & validate command line args
            LOGGER.trace("Assigning Args...");
            assignArgs(args);
            // load the Application Configuration file
            LOGGER.trace("Loading AppConfig...");
            AppConfig.init(applicationConfigFile);
            // load customers from dpf file
            LOGGER.trace("Initialising DPF Parser...");
            DpfParser dpf = new DpfParser(inputDpfFile, outputDpfFile);
            LOGGER.trace("Loading customers...");
            ArrayList<Customer> customers = dpf.Load();
            
            // Load Selector Lookup & Production Config files
            LOGGER.trace("Loading Lookup Files...");
            String selRef = customers.get(0).getSelectorRef();
            loadLookupFiles(selRef);
            // Sort Order: Language -> Presentation Priority
            LOGGER.trace("Sorting input...");
            customers.sort(new CustomerComparator());
            // Calculate sites for every customer
            LOGGER.trace("Starting CalcLocation...");
            LocationCalculator calculateLocation = new LocationCalculator();
            LOGGER.trace("Running calculate...");
            calculateLocation.calculate(customers);
            
            // Sort order: LOCATION -> LANGUAGE -> STATIONERY -> PRESENTATION_ORDER -> SUB_BATCH -> SORT_FIELD -> FLEET_NO -> MSC -> GRP_ID
            LOGGER.trace("Sorting input...");
            customers.sort(new CustomerComparatorWithLocation());
            // Calculate EOGs ready for the batch engine
            LOGGER.trace("Calculating EOGs...");
            CalculateEndOfGroups eogs = new CalculateEndOfGroups();
            eogs.calculate(customers);
            TotalPagesInGroup tpig = new TotalPagesInGroup();
            tpig.calculate(customers);
            
            // Sort order: LOCATION -> LANGUAGE -> STATIONERY -> PRESENTATION_ORDER -> SUB_BATCH -> SORT_FIELD -> FLEET_NO -> MSC -> GRP_ID
            LOGGER.trace("Sorting input...");
            customers.sort(new CustomerComparatorWithLocation());
            
            String mailProvider = PostageConfiguration.getInstance().getMailProvider();
            
            // Exclude UNSORTED items when provider is Royal Mail
            if (!"RM".equalsIgnoreCase(mailProvider)) {
                // Set MSC to 99999 on Unsorted to enable batching and tray sorting
                LOGGER.trace("Adding temp MSC to UNSORTED...");
                setMscOnUnsorted(customers);
            }
            
            // Putting into batches that are above the 25 tray minimum
            LOGGER.trace("Running Batch Engine...");
            BatchEngine be = new BatchEngine(tenDigitJid, eightDigitJid);
            be.batch(customers);
            
            // Royal Mail & Uk Mail require different data for Consignor and SOAP files
            if ("RM".equalsIgnoreCase(mailProvider)) {
                LOGGER.trace("Creating Royal Mail Resources...");
                CreateRoyalMailResources rmResources = new CreateRoyalMailResources(runNo);
                rmResources.method(customers);
            } else {
                LOGGER.trace("Creating UkMail Resources...");
                CreateUkMailResources ukm = new CreateUkMailResources(customers, runNo);
                ukm.method();
            }
            
            // MSC's not set on UNSORTED items when provider is Royal Mail
            if (!"RM".equalsIgnoreCase(mailProvider)) {
                LOGGER.trace("Removing temp MSC's from UNSORTED...");
                removeMscOnUnsorted(customers);
            }

            // Return to original order to map records row by row
            LOGGER.trace("Sorting back to original order...");
            customers.sort(new CustomerComparatorOriginalOrder());
            // Dpf saves the changed details to the output file
            LOGGER.trace("Saving DPF file...");
            dpf.Save(customers);
            LOGGER.trace("Data saved to: {}", outputDpfFile);
            String summary = summaryPrint(customers);
            LOGGER.debug(summary);
        } catch (Exception ex) {
            LOGGER.fatal(ExceptionUtils.getStackTrace(ex));
            System.exit(1);
        }
        LOGGER.info("---- CalcLocation Finished ----");
    }

    private static void assignArgs(String[] args) {
        if (args.length != EXPECTED_NO_OF_ARGS) {
            LOGGER.fatal("Incorrect number of args parsed '{}' expecting '{}'. " 
                    + "Args are " + "1. Props file, "
                    + "2. Input file, " + "3. Output file, " + "4. Run No, " 
                    + "5. 8 Digit Job Id, "
                    + "6. 10 Digit Parent Jid.", args.length, EXPECTED_NO_OF_ARGS);
            System.exit(1);
        }

        applicationConfigFile = args[0];
        boolean propsFileExists = new File(applicationConfigFile).exists();
        if (!propsFileExists) {
            LOGGER.fatal("Properties File '{}' doesn't exist", applicationConfigFile);
            System.exit(1);
        }

        inputDpfFile = args[1];
        boolean inputFileExists = new File(inputDpfFile).exists();
        if (!inputFileExists) {
            LOGGER.fatal("Input File '{}' doesn't exist on the filepath.", inputDpfFile);
            System.exit(1);
        }

        outputDpfFile = args[2];

        runNo = args[3];
        boolean runNoIsNumeric = StringUtils.isNumeric(runNo);
        if (!runNoIsNumeric) {
            LOGGER.fatal("Invalid character in Run No. [{}]", runNo);
            System.exit(1);
        }

        String ipwJid = args[4];
        boolean ipwJidIsNumeric = StringUtils.isNumeric(ipwJid);
        if (ipwJidIsNumeric) {
            eightDigitJid = Integer.parseInt(ipwJid);
        } else {
            LOGGER.fatal("Invalid character in Eight Digit JID [{}]", ipwJid);
            System.exit(1);
        }

        String rpdJid = args[5];
        boolean rpdJidIsNumeric = StringUtils.isNumeric(rpdJid);
        if (rpdJidIsNumeric) {
            tenDigitJid = Integer.parseInt(rpdJid);
        } else {
            LOGGER.fatal("Invalid character in Ten Digit JID [{}]", rpdJid);
            System.exit(1);
        }
    }

    private static void loadLookupFiles(String selRef) {

        // APPLICATION CONFIGURATION
        AppConfig appConfig = AppConfig.getInstance();
        
        // LOOKUP FILES
        InsertLookup.init(appConfig.getInsertLookup());
        EnvelopeLookup.init(appConfig.getEnvelopeLookup());
        PapersizeLookup.init(appConfig.getPapersizeLookup());
        
        // SELECTOR LOOKUP
        SelectorLookup.init(appConfig.getLookupFile());
        if (!SelectorLookup.getInstance().isPresent(selRef)) {
            LOGGER.fatal("Selector [{}] is not present in the lookupFile.", selRef);
            System.exit(1);
        }
        
        // SELECTOR
        Selector selector = SelectorLookup.getInstance().getSelector(selRef);
        
        // PRODUCTION CONFIGURATION FILE
        String prodConfigFile = appConfig.getProductionConfigPath() + selector.getProductionConfig() + appConfig.getProductionFileSuffix();
        if (!new File(prodConfigFile).isFile()) {
            LOGGER.fatal("Production Configuration File [{}] doesn't exist on the filepath.", prodConfigFile);
            System.exit(1);
        }        
        ProductionConfiguration.init(prodConfigFile);
        
        // POSTAGE CONFIGUATION FILE
        String postConfigFile = appConfig.getPostageConfigPath() + selector.getPostageConfig() + appConfig.getPostageFileSuffix();
        if (!new File(postConfigFile).isFile()) {
            LOGGER.fatal("Postage Configuration File [{}] doesn't exist on the filepath.", postConfigFile);
            System.exit(1);
        }
        PostageConfiguration.init(postConfigFile);
        
        // PRESENTATION CONFIGURATION FILE
        String presConfigFile = appConfig.getPresentationPriorityConfigPath() + selector.getPresentationConfig() + appConfig.getPresentationPriorityFileSuffix();
        if (!new File(presConfigFile).isFile()) {
            LOGGER.fatal("Presentation Configuration File [{}] doesn't exist on the filepath.", presConfigFile);
            System.exit(1);
        }
        PresentationConfiguration.init(presConfigFile);

    }

    private static void setMscOnUnsorted(ArrayList<Customer> customers) {
        customers.stream().filter(customer -> BatchType.UNSORTED.equals(customer.getBatchType()))
                .forEach(customer -> customer.setMsc("99999"));
    }

    private static void removeMscOnUnsorted(ArrayList<Customer> customers) {
        customers.stream().filter(customer -> BatchType.UNSORTED.equals(customer.getBatchType()))
                .forEach(customer -> customer.setMsc(""));
    }

    /**
     * Prints a summary of the number of items for each batch type.
     * @param docProps
     */
    private static String summaryPrint(ArrayList<Customer> customers) {
        Map<String, Long> counting = customers.stream()
                .collect(Collectors.groupingBy(Customer::getFullBatchName, Collectors.counting()));

        return counting.toString();
    }

}
