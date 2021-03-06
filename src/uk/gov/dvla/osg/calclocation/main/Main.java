package uk.gov.dvla.osg.calclocation.main;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import uk.gov.dvla.osg.common.classes.Utils;
import uk.gov.dvla.osg.common.config.*;
import uk.gov.dvla.osg.common.enums.BatchType;
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
        LOGGER.info("---- CalcLocation Started ----");

        try {
            // assign & validate command line args
            LOGGER.trace("Assigning Args...");
            assignArgs(args);
            // load the Application Configuration file
            LOGGER.trace("Loading AppConfig...");
            AppConfig.init(propsFile);
            // load customers from dpf file
            LOGGER.trace("Initialising DPF Parser...");
            DpfParser dpf = new DpfParser(inputFile, outputFile);
            LOGGER.trace("Loading customers from {}...", inputFile);
            ArrayList<Customer> customers = dpf.Load();
            // Summary Print to check batch volumes before and after
            String summaryBefore = Utils.summaryPrint(customers);
            LOGGER.info(summaryBefore);
            
            // Load Selector Lookup & Production Config files
            String selRef = customers.get(0).getSelectorRef();
            LOGGER.trace("Loading Lookup Files For Selector {}...", selRef);
            loadLookupFiles(selRef);
            
            // Sort Order: Language -> Presentation Priority
            LOGGER.trace("Sorting input...");
            sortCustomers(customers, new CustomerComparator());
            
            // Calculate sites for every customer
            LOGGER.trace("Starting CalcLocation...");
            LocationCalculator calculateLocation = new LocationCalculator();
            LOGGER.trace("Running calculate...");
            calculateLocation.calculate(customers);
           
            /*
             * Sort order: LOCATION -> LANGUAGE -> STATIONERY -> PRESENTATION_ORDER -> SUB_BATCH -> SORT_FIELD -> FLEET_NO -> MSC -> GRP_ID
             */
            LOGGER.trace("Sorting input...");
            sortCustomers(customers, new CustomerComparatorWithLocation());
            
            // Calculate EOGs ready for the batch engine
            LOGGER.trace("Calculating EOGs...");
            CalculateEndOfGroups eogs = new CalculateEndOfGroups();
            eogs.calculate(customers);
            TotalPagesInGroup tpig = new TotalPagesInGroup();
            tpig.calculate(customers);
            
            /*
             * Sort order: LOCATION -> LANGUAGE -> STATIONERY -> PRESENTATION_ORDER -> SUB_BATCH -> SORT_FIELD -> FLEET_NO -> MSC -> GRP_ID
             */
            LOGGER.trace("Sorting input...");
            sortCustomers(customers, new CustomerComparatorWithLocation());
            // Set MSC to 99999 on Unsorted to enable batching and tray sorting
            LOGGER.trace("Adding temp MSC to UNSORTED...");
            setMscOnUnsorted(customers);
            // Putting into batches that are above the 25 tray minimum
            LOGGER.trace("Running Batch Engine...");
            BatchEngine be = new BatchEngine(tenDigitJid, eightDigitJid);
            be.batch(customers);
            LOGGER.trace("Creating UkMail Resources...");
            CreateUkMailResources ukm = new CreateUkMailResources(customers, runNo);
            ukm.method();
            // Remove the 99999 MSC that was set on Unsorted
            removeMscOnUnsorted(customers);
            // Return to original order to map records row by row
            LOGGER.trace("Sorting back to original order...");
            sortCustomers(customers, new CustomerComparatorOriginalOrder());
            // Dpf saves the changed details to the output file
            LOGGER.trace("Saving DPF file...");
            dpf.Save(customers);
            LOGGER.trace("Data saved to: {}", outputFile);
            // Summary Print to check batch volumes before and after
            String summaryAfter = Utils.summaryPrint(customers);
            LOGGER.info(summaryAfter);
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

        propsFile = args[0];
        boolean propsFileExists = new File(propsFile).exists();
        if (!propsFileExists) {
            LOGGER.fatal("Properties File '{}' doesn't exist", propsFile);
            System.exit(1);
        }

        inputFile = args[1];
        boolean inputFileExists = new File(inputFile).exists();
        if (!inputFileExists) {
            LOGGER.fatal("Input File '{}' doesn't exist on the filepath.", inputFile);
            System.exit(1);
        }

        outputFile = args[2];

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

        AppConfig appConfig = AppConfig.getInstance();
        SelectorLookup.init(appConfig.getLookupFile());
        Selector selector = null;

        if (SelectorLookup.getInstance().isPresent(selRef)) {
            selector = SelectorLookup.getInstance().getSelector(selRef);
        } else {
            LOGGER.fatal("Selector [{}] is not present in the lookupFile.", selRef);
            System.exit(1);
        }

        String prodConfigFile = appConfig.getProductionConfigPath() + selector.getProductionConfig() + appConfig.getProductionFileSuffix();
        if (new File(prodConfigFile).isFile()) {
            ProductionConfiguration.init(prodConfigFile);
        } else {
            LOGGER.fatal("Production Configuration File [{}] doesn't exist on the filepath.", prodConfigFile);
            System.exit(1);
        }

        String postConfigFile = appConfig.getPostageConfigPath() + selector.getPostageConfig() + appConfig.getPostageFileSuffix();
        if (new File(postConfigFile).isFile()) {
            PostageConfiguration.init(postConfigFile);
        } else {
            LOGGER.fatal("Postage Configuration File [{}] doesn't exist on the filepath.", postConfigFile);
            System.exit(1);
        }

       
        String presConfigFile = appConfig.getPresentationPriorityConfigPath() + selector.getPresentationConfig() + appConfig.getPresentationPriorityFileSuffix();
        if (new File(presConfigFile).isFile()) {
            PresentationConfiguration.init(presConfigFile);
        } else {
            LOGGER.fatal("Presentation Configuration File [{}] doesn't exist on the filepath.", presConfigFile);
            System.exit(1);
        }
        
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

    private static void setMscOnUnsorted(ArrayList<Customer> customers) {
        customers.stream().filter(customer -> BatchType.UNSORTED.equals(customer.getBatchType()))
                .forEach(customer -> customer.setMsc("99999"));
    }

    private static void removeMscOnUnsorted(ArrayList<Customer> customers) {
        customers.stream()
                 .filter(customer -> BatchType.UNSORTED.equals(customer.getBatchType()))
                 .forEach(customer -> customer.setMsc(""));
    }

    /**
     * Prints a summary of the number of items for each batch type by item and by group
     * @param docProps
     */
    private static String summaryPrint(ArrayList<Customer> customers) {
        String customerCount = customers.stream()
                                        .collect(Collectors.groupingBy(Customer::getFullBatchName, Collectors.counting()))
                                        .toString();

        String groupCount = customers.stream()
                                     .filter(item -> item.isEog())
                                     .collect(Collectors.groupingBy(Customer::getFullBatchName, Collectors.counting()))
                                     .toString();
        
        return String.format("Customer Summary {}, Group Summary {}", customerCount, groupCount);
    }

}
