package uk.gov.dvla.osg.calclocation.location;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.gov.dvla.osg.common.classes.Customer;
import uk.gov.dvla.osg.common.enums.FullBatchType;

public class LocationCalculator {

	private static final Logger LOGGER = LogManager.getLogger();
	
	private final Map<FullBatchType, AbstractBatchType> batchMap = BatchMapFactory.newInstance();

	public void calculate(List<Customer> customers) {
		LOGGER.trace("Count Customers...");
		
		// Count how many customers there are for each batch type
		customers.forEach(customer -> batchMap.get(customer.getFullBatchType()).addCustomer(customer));
		LOGGER.trace("Calculate Number To Ff...");
		
		// Loop through all batch types and have them calculate the number of customers to each site
		batchMap.values().forEach(batchType -> batchType.calculate());
		LOGGER.trace("Set Sites...");
		
		// Sites are switched when the cut off is reached
		customers.forEach(customer -> customer.setSite(batchMap.get(customer.getFullBatchType()).getSite(customer)));
	}

}
