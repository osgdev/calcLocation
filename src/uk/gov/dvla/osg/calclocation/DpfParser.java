package uk.gov.dvla.osg.calclocation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;

import com.univocity.parsers.common.processor.ConcurrentRowProcessor;
import com.univocity.parsers.common.processor.RowListProcessor;
import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;
import com.univocity.parsers.tsv.TsvWriter;
import com.univocity.parsers.tsv.TsvWriterSettings;

import uk.gov.dvla.osg.common.classes.Customer;

class DpfParser {

	//Input variables
	private String inputFile;
	// Throughput variables
	private String[] headers;
	private String outputFile;
	private AppConfig appConfig;
	
	/**
	 * Extracts DocumentProperties from a dpf data file.
	 * @param inputFile dpf input file
	 * @param outputFile dpf output file
	 * @param appConfig supplies column names for mapping
	 */
	DpfParser (String inputFile, String outputFile, AppConfig appConfig) {
		this.inputFile = inputFile;
		this.outputFile = outputFile;
		this.appConfig = appConfig;
	}
	
	/**
	 * Reads the input file and maps each row to a customer object.
	 * @return a list of customers
	 */
	ArrayList<Customer> Load() {
		ArrayList<Customer> customers = new ArrayList<>();
		
		// Keep track of customer number so we can output in same order
		AtomicInteger counter = new AtomicInteger(0);
		TsvParser parser = createParser();
		parser.parseAllRecords(new File(inputFile)).forEach(record -> {
			
			Customer customer = new Customer(counter.getAndIncrement());
			customer.setDocRef(record.getString(appConfig.getDocumentReference()));
			customer.setSortField(record.getString(appConfig.getSortField()));
			customer.setSelectorRef(record.getString(appConfig.getAppNameField()));
			customer.setLang(record.getString(appConfig.getLanguageFieldName()));
			customer.setStationery(record.getString(appConfig.getStationeryFieldName()));
			customer.setBatchType(record.getString(appConfig.getBatchTypeFieldName()));
			customer.setSubBatch(record.getString(appConfig.getSubBatchTypeFieldName()));
			customer.setFleetNo(record.getString(appConfig.getFleetNoFieldName()));
			Integer groupId = record.getString(appConfig.getGroupIdFieldName()).equals("")?null:record.getInt(appConfig.getGroupIdFieldName());
			customer.setGroupId(groupId);
			customer.setPaperSize(record.getString(appConfig.getPaperSizeFieldName()));
			customer.setMsc(record.getString(appConfig.getMscFieldName()));
			
			customer.setName1(record.getString(appConfig.getName1Field()));
			customer.setName2(record.getString(appConfig.getName2Field()));
			customer.setAdd1(record.getString(appConfig.getAddress1Field()));
			customer.setAdd2(record.getString(appConfig.getAddress2Field()));
			customer.setAdd3(record.getString(appConfig.getAddress3Field()));
			customer.setAdd4(record.getString(appConfig.getAddress4Field()));
			customer.setAdd5(record.getString(appConfig.getAddress5Field()));
			customer.setPostcode(record.getString(appConfig.getPostCodeField()));
			
			customer.setDps(record.getString(appConfig.getDpsField()));
			customer.setInsertRef(record.getString(appConfig.getInsertField()));
			customer.setMmCustomerContent(record.getString(appConfig.getMailMarkBarcodeContent()));
			customer.setNoOfPages(record.getInt(appConfig.getNoOfPagesField()));
			customer.setSot(record.getString(appConfig.getEotField()));
			customer.setEog(record.getString(appConfig.getEogField()));
			String[] str = StringUtils.split(record.getString(appConfig.getWeightAndSizeField()), "|");
			customer.setWeight(Double.parseDouble(str[0]));
			customer.setThickness(Double.parseDouble(str[1]));
			customer.setEnvelope(record.getString(appConfig.getOuterEnvelope()));
			customer.setProduct(record.getString(appConfig.getMailingProduct()));
			customer.setAppName(record.getString(appConfig.getAppNameField()));
			customer.setPresentationPriority(record.getInt(appConfig.getPresentationPriorityField()));
			customers.add(customer);
		});
		headers = parser.getRecordMetadata().headers();
		return customers;
	}
	
	/**
	 * Saves the dpf file with the amended document properties.
	 * @param customers the amended customer data
	 * @throws IOException unable to write output file to the supplied path
	 */
	void Save(ArrayList<Customer> customers) throws IOException {
		try (FileWriter fw = new FileWriter(new File(outputFile))) {
			// Create an instance of TsvWriter with the default settings
			TsvWriter writer = new TsvWriter(fw, new TsvWriterSettings());
			// Writes the file headers
			writer.writeHeaders(headers);
			// Keep track of which customer we are writing
			AtomicInteger counter = new AtomicInteger(0);
			// Build a parser that loops through the original dpf file
			TsvParser parser = createParser();
			parser.parseAll(new File(inputFile)).forEach(record -> {
				// Write out the original row of data
				writer.addValues((Object[]) record);
				// Replace changed values - map fields in dpf to properties in customer
				Customer customer = customers.get(counter.getAndIncrement());
				writer.addValue(appConfig.getEightDigitJobIdFieldName(), customer.getRpdJid());
				writer.addValue(appConfig.getTenDigitJobIdFieldName(), customer.getTenDigitJid());
				writer.addValue(appConfig.getSiteFieldName(), customer.getSite().name().toLowerCase());
				writer.addValue(appConfig.getEogField(), customer.getEog());
				writer.addValue(appConfig.getEotField(), customer.getSot());
				writer.addValue(appConfig.getMailMarkBarcodeContent(), customer.getMmBarcodeContent());
				writer.addValue(appConfig.getChildSequence(), customer.getSequenceInChild());
				writer.addValue(appConfig.getOuterEnvelope(), customer.getEnvelope());
				writer.addValue(appConfig.getMailingProduct(), customer.getProduct());
				writer.addValue(appConfig.getBatchTypeFieldName(), customer.getBatchType());
				writer.addValue(appConfig.getTotalNumberOfPagesInGroupField(), customer.getTotalPagesInGroup());
				writer.addValue(appConfig.getInsertHopperCodeField(), customer.getInsertRef());
				writer.addValue(appConfig.getMscFieldName(), customer.getMsc());
				writer.addValue(appConfig.getDpsField(), customer.getDps());
				// clear weight and size from dpf
				writer.addValue(appConfig.getWeightAndSizeField(), "");
				writer.addValue(appConfig.getPresentationPriorityField(), "");
				writer.writeValuesToRow();
			});
			// Flushes and closes the writer
			writer.close();
		}
	}		
	
	/**
	 * Create a new instance of a TsvParser
	 * @return A TsvParser set to handle header rows
	 */
	private TsvParser createParser() {
		TsvParserSettings parserSettings = new TsvParserSettings();
		parserSettings.setNullValue("");
		parserSettings.setProcessor(new ConcurrentRowProcessor(new RowListProcessor()));
		parserSettings.setLineSeparatorDetectionEnabled(true);
		parserSettings.setHeaderExtractionEnabled(true);
		return new TsvParser(parserSettings);
	}
	
	
}
