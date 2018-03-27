package uk.gov.dvla.osg.calclocation;

import static uk.gov.dvla.osg.common.classes.FullBatchType.*;

import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.gov.dvla.osg.calclocation.models.BatchType;
import uk.gov.dvla.osg.calclocation.models.BatchTypeGroup;
import uk.gov.dvla.osg.calclocation.models.BatchTypeSingle;
import uk.gov.dvla.osg.common.classes.Customer;
import uk.gov.dvla.osg.common.classes.FullBatchType;
import uk.gov.dvla.osg.common.config.ProductionConfiguration;

public class LocationCalculator {

	private static final Logger LOGGER = LogManager.getLogger();
	private final HashMap<FullBatchType, BatchType> batchMap = new HashMap<>();
	private List<Customer> input;

	{
		ProductionConfiguration prodConfig = ProductionConfiguration.getInstance();
		// Singles
		if (isNotIgnore(SORTEDE)) {
			batchMap.put(SORTEDE, new BatchTypeSingle(prodConfig.getSite(SORTEDE)));
		}
		if (isNotIgnore(SORTEDW)) {
			batchMap.put(SORTEDW, new BatchTypeSingle(prodConfig.getSite(SORTEDW)));
		}
		if (isNotIgnore(UNSORTEDE)) {
			batchMap.put(UNSORTEDE, new BatchTypeSingle(prodConfig.getSite(UNSORTEDE)));
		}
		if (isNotIgnore(UNSORTEDW)) {
			batchMap.put(UNSORTEDW, new BatchTypeSingle(prodConfig.getSite(UNSORTEDW)));
		}
		if (isNotIgnore(SORTINGE)) {
			batchMap.put(SORTINGE, new BatchTypeSingle(prodConfig.getSite(SORTINGE)));
		}
		if (isNotIgnore(SORTINGW)) {
			batchMap.put(SORTINGW, new BatchTypeSingle(prodConfig.getSite(SORTINGW)));
		}
		if (isNotIgnore(REJECTE)) {
			batchMap.put(REJECTE, new BatchTypeSingle(prodConfig.getSite(REJECTE)));
		}
		if (isNotIgnore(REJECTW)) {
			batchMap.put(REJECTW, new BatchTypeSingle(prodConfig.getSite(REJECTW)));
		}
		if (isNotIgnore(UNCODEDE)) {
			batchMap.put(UNCODEDE, new BatchTypeSingle(prodConfig.getSite(UNCODEDE)));
		}
		if (isNotIgnore(UNCODEDW)) {
			batchMap.put(UNCODEDW, new BatchTypeSingle(prodConfig.getSite(UNCODEDW)));
		}
		
		// Groups
		if (isNotIgnore(FLEETE)) {
			batchMap.put(FLEETE, new BatchTypeGroup(prodConfig.getSite(FLEETE)));
		}
		if (isNotIgnore(FLEETW)) {
			batchMap.put(FLEETW, new BatchTypeGroup(prodConfig.getSite(FLEETW)));
		}
		if (isNotIgnore(CLERICALE)) {
			batchMap.put(CLERICALE, new BatchTypeGroup(prodConfig.getSite(CLERICALE)));
		}
		if (isNotIgnore(CLERICALW)) {
			batchMap.put(CLERICALW, new BatchTypeGroup(prodConfig.getSite(CLERICALW)));
		}
		if (isNotIgnore(MULTIE)) {
			batchMap.put(MULTIE, new BatchTypeGroup(prodConfig.getSite(MULTIE)));
		}
		if (isNotIgnore(MULTIW)) {
			batchMap.put(MULTIW, new BatchTypeGroup(prodConfig.getSite(MULTIW)));
		}
	}

	public void calculate(List<Customer> customers) {
		this.input = customers;
		LOGGER.trace("Count Customers...");
		// Count how many customers there are for each batch type
		input.forEach(customer -> batchMap.get(customer.getFullBatchType()).addCustomer(customer));
		LOGGER.trace("Calculate Number To Ff...");
		// Loop through all batch types and have them calculate the number of customers to each site
		batchMap.values().forEach(batchType -> batchType.calculate());
		LOGGER.trace("Set Sites...");
		// Sites are switched when the cut off is reached
		input.forEach(customer -> customer.setSite(batchMap.get(customer.getFullBatchType()).getSite(customer)));
	}

	private boolean isNotIgnore(FullBatchType batchType) {
		return !ProductionConfiguration.getInstance().getSite(batchType).equalsIgnoreCase("x");
	}
}
