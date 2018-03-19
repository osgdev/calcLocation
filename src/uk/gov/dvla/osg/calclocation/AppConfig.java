package uk.gov.dvla.osg.calclocation;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

class AppConfig {

	private String documentReference, lookupReferenceFieldName, languageFieldName, stationeryFieldName,
			batchTypeFieldName, subBatchTypeFieldName, fleetNoFieldName, groupIdFieldName, paperSizeFieldName,
			mscFieldName, sortField, noOfPagesField, name1Field, name2Field, address1Field, address2Field,
			address3Field, address4Field, address5Field, postCodeField, dpsField, insertField, appNameField,
			weightAndSizeField;

	private String siteFieldName, eightDigitJobIdFieldName, tenDigitJobIdFieldName, mailMarkBarcodeContent, eogField,
			eotField, childSequence, outerEnvelope, mailingProduct, insertHopperCodeField,
			mailMarkBarcodeCustomerContent, totalNumberOfPagesInGroupField;

	private String lookupFile, presentationPriorityConfigPath, presentationPriorityFileSuffix, productionConfigPath,
			productionFileSuffix, postageConfigPath, postageFileSuffix, insertLookup, envelopeLookup, stationeryLookup,
			papersizeLookup, mailmarkCompliancePath;

	private int tenDigitJobIdIncrementValue;
	private String presentationPriorityField;
	
	public AppConfig(String fileName) throws Exception {
		Properties prop = new Properties();
		InputStream input = new FileInputStream(fileName);
		prop.load(input);
		
		documentReference = prop.getProperty("documentReference");
		lookupReferenceFieldName = prop.getProperty("lookupReferenceFieldName");
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
		appNameField = prop.getProperty("appNameField");
		weightAndSizeField = prop.getProperty("weightAndSizeField");
		siteFieldName = prop.getProperty("siteFieldName");
		eightDigitJobIdFieldName = prop.getProperty("eightDigitJobIdFieldName");
		tenDigitJobIdFieldName = prop.getProperty("tenDigitJobIdFieldName");
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

	public void setPresentationPriorityField(String presentationPriorityField) {
		this.presentationPriorityField = presentationPriorityField;
	}

}
