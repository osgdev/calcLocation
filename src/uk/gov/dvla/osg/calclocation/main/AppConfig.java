package uk.gov.dvla.osg.calclocation.main;

import java.io.*;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Holds names of the document properties fields and
 * path information for configuration files.
 *
 */
public class AppConfig {
	
	private static final Logger LOGGER = LogManager.getLogger();
	
	private String documentReference, lookupReferenceFieldName, languageFieldName, stationeryFieldName,
			batchTypeFieldName, subBatchTypeFieldName, fleetNoFieldName, groupIdFieldName, paperSizeFieldName,
			mscFieldName, sortField, noOfPagesField, name1Field, name2Field, address1Field, address2Field,
			address3Field, address4Field, address5Field, postCodeField, dpsField, insertField, appNameField,
			mailingIdField, weightAndSizeField, runDate;

	private String siteFieldName, eightDigitJobIdFieldName, tenDigitJobIdFieldName, mailMarkBarcodeContent, eogField,
			eotField, childSequence, outerEnvelope, mailingProduct, insertHopperCodeField,
			mailMarkBarcodeCustomerContent, totalNumberOfPagesInGroupField;

	private String lookupFile, presentationPriorityConfigPath, presentationPriorityFileSuffix, productionConfigPath,
			productionFileSuffix, postageConfigPath, postageFileSuffix, insertLookup, envelopeLookup, stationeryLookup,
			papersizeLookup, mailmarkCompliancePath;

	private int tenDigitJobIdIncrementValue;
	private String presentationPriorityField;
	  /******************************************************************************************
	  *              SINGLETON PATTERN
	  ******************************************************************************************/
	 private static String filename;

	 private static class SingletonHelper {
	     private static final AppConfig INSTANCE = new AppConfig();
	 }

	 public static AppConfig getInstance() {
	     if (StringUtils.isBlank(filename)) {
	         throw new RuntimeException("Application Configuration not initialised before use");
	     }
	     return SingletonHelper.INSTANCE;
	 }

	 public static void init(String file) throws RuntimeException {
	     if (StringUtils.isBlank(filename)) {
	         if (new File(file).isFile()) {
	             filename = file;
	         } else {
	             throw new RuntimeException("Application Configuration File " + filename + " does not exist on filepath.");
	         }
	     } else {
	         throw new RuntimeException("Application Configuration has already been initialised");
	     }
	 }
	 /*****************************************************************************************/
	private AppConfig() {

		Properties prop = new Properties();
		try (InputStream input = new FileInputStream(filename)) {
			prop.load(input);
		} catch (IOException ex) {
			LOGGER.fatal("Unable to load Application Configuration from file - [{}] {}", filename, ex.getMessage());
			System.exit(1);
		}
		
		lookupReferenceFieldName = prop.getProperty("lookupReferenceFieldName");
		appNameField = prop.getProperty("appNameField");
		mailingIdField = prop.getProperty("mailingIdField");
		documentReference = prop.getProperty("documentReference");
		languageFieldName = prop.getProperty("languageFieldName");
		stationeryFieldName = prop.getProperty("stationeryFieldName");
		batchTypeFieldName = prop.getProperty("batchTypeFieldName");
		subBatchTypeFieldName = prop.getProperty("subBatchTypeFieldName");
		fleetNoFieldName = prop.getProperty("fleetNoFieldName");
		groupIdFieldName = prop.getProperty("groupIdFieldName");
		paperSizeFieldName = prop.getProperty("paperSizeFieldName");
		mscFieldName = prop.getProperty("mscFieldName");
		sortField = prop.getProperty("sortField");
		noOfPagesField = prop.getProperty("noOfPagesField");
		name1Field = prop.getProperty("name1Field");
		name2Field = prop.getProperty("name2Field");
		address1Field = prop.getProperty("address1Field");
		address2Field = prop.getProperty("address2Field");
		address3Field = prop.getProperty("address3Field");
		address4Field = prop.getProperty("address4Field");
		address5Field = prop.getProperty("address5Field");
		postCodeField = prop.getProperty("postCodeField");
		dpsField = prop.getProperty("dpsField");
		insertField = prop.getProperty("insertField");
		weightAndSizeField = prop.getProperty("weightAndSizeField");
		siteFieldName = prop.getProperty("siteFieldName");
		eightDigitJobIdFieldName = prop.getProperty("jobIdFieldName");
		tenDigitJobIdFieldName = prop.getProperty("tenDigitJobId");
		mailMarkBarcodeContent = prop.getProperty("mailMarkBarcodeContent");
		eogField = prop.getProperty("eogField");
		eotField = prop.getProperty("eotField");
		childSequence = prop.getProperty("childSequence");
		outerEnvelope = prop.getProperty("outerEnvelope");
		mailingProduct = prop.getProperty("mailingProduct");
		insertHopperCodeField = prop.getProperty("insertHopperCodeField");
		mailMarkBarcodeCustomerContent = prop.getProperty("mailMarkBarcodeCustomerContent");
		totalNumberOfPagesInGroupField = prop.getProperty("totalNumberOfPagesInGroupField");
		lookupFile = prop.getProperty("lookupFile");
		presentationPriorityConfigPath = prop.getProperty("presentationPriorityConfigPath");
		presentationPriorityFileSuffix = prop.getProperty("presentationPriorityFileSuffix");
		productionConfigPath = prop.getProperty("productionConfigPath");
		productionFileSuffix = prop.getProperty("productionFileSuffix");
		postageConfigPath = prop.getProperty("postageConfigPath");
		postageFileSuffix = prop.getProperty("postageFileSuffix");
		insertLookup = prop.getProperty("insertLookup");
		envelopeLookup = prop.getProperty("envelopeLookup");
		stationeryLookup = prop.getProperty("stationeryLookup");
		papersizeLookup = prop.getProperty("papersizeLookup");
		mailmarkCompliancePath = prop.getProperty("mailmarkCompliancePath");
		tenDigitJobIdIncrementValue = Integer.valueOf(prop.getProperty("tenDigitJobIdIncrementValue"));
		presentationPriorityField = prop.getProperty("presentationPriorityField");
		runDate = prop.getProperty("runDate");
	}
	
	public String getMailingIdField() {
		return mailingIdField;
	}
	
	public String getWeightAndSizeField() {
		return weightAndSizeField;
	}

	public String getDocumentReference() {
		return documentReference;
	}

	public String getLookupReferenceFieldName() {
		return lookupReferenceFieldName;
	}

	public String getLanguageFieldName() {
		return languageFieldName;
	}

	public String getStationeryFieldName() {
		return stationeryFieldName;
	}

	public String getBatchTypeFieldName() {
		return batchTypeFieldName;
	}

	public String getSubBatchTypeFieldName() {
		return subBatchTypeFieldName;
	}

	public String getFleetNoFieldName() {
		return fleetNoFieldName;
	}

	public String getGroupIdFieldName() {
		return groupIdFieldName;
	}

	public String getPaperSizeFieldName() {
		return paperSizeFieldName;
	}

	public String getMscFieldName() {
		return mscFieldName;
	}

	public String getSortField() {
		return sortField;
	}

	public String getNoOfPagesField() {
		return noOfPagesField;
	}

	public String getName1Field() {
		return name1Field;
	}

	public String getName2Field() {
		return name2Field;
	}

	public String getAddress1Field() {
		return address1Field;
	}

	public String getAddress2Field() {
		return address2Field;
	}

	public String getAddress3Field() {
		return address3Field;
	}

	public String getAddress4Field() {
		return address4Field;
	}

	public String getAddress5Field() {
		return address5Field;
	}

	public String getPostCodeField() {
		return postCodeField;
	}

	public String getDpsField() {
		return dpsField;
	}

	public String getInsertField() {
		return insertField;
	}

	public String getAppNameField() {
		return appNameField;
	}

	public String getSiteFieldName() {
		return siteFieldName;
	}

	public String getEightDigitJobIdFieldName() {
		return eightDigitJobIdFieldName;
	}

	public String getTenDigitJobIdFieldName() {
		return tenDigitJobIdFieldName;
	}

	public String getMailMarkBarcodeContent() {
		return mailMarkBarcodeContent;
	}

	public String getEogField() {
		return eogField;
	}

	public String getEotField() {
		return eotField;
	}

	public String getChildSequence() {
		return childSequence;
	}

	public String getOuterEnvelope() {
		return outerEnvelope;
	}

	public String getMailingProduct() {
		return mailingProduct;
	}

	public String getInsertHopperCodeField() {
		return insertHopperCodeField;
	}

	public String getMailMarkBarcodeCustomerContent() {
		return mailMarkBarcodeCustomerContent;
	}

	public String getTotalNumberOfPagesInGroupField() {
		return totalNumberOfPagesInGroupField;
	}

	public String getLookupFile() {
		return lookupFile;
	}

	public String getPresentationPriorityConfigPath() {
		return presentationPriorityConfigPath;
	}

	public String getPresentationPriorityFileSuffix() {
		return presentationPriorityFileSuffix;
	}

	public String getProductionConfigPath() {
		return productionConfigPath;
	}

	public String getProductionFileSuffix() {
		return productionFileSuffix;
	}

	public String getPostageConfigPath() {
		return postageConfigPath;
	}

	public String getPostageFileSuffix() {
		return postageFileSuffix;
	}

	public String getInsertLookup() {
		return insertLookup;
	}

	public String getEnvelopeLookup() {
		return envelopeLookup;
	}

	public String getStationeryLookup() {
		return stationeryLookup;
	}

	public String getPapersizeLookup() {
		return papersizeLookup;
	}

	public int getTenDigitJobIdIncrementValue() {
		return tenDigitJobIdIncrementValue;
	}

	public String getMailmarkCompliancePath() {
		return mailmarkCompliancePath;
	}

	public String getPresentationPriorityField() {
		return presentationPriorityField;
	}

	public String getRunDate() {
		return runDate;
	}

}
