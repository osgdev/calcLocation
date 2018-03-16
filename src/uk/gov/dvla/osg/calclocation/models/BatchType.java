package uk.gov.dvla.osg.calclocation.models;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.gov.dvla.osg.common.classes.Customer;


public abstract class BatchType {
	
	private static final Logger LOGGER = LogManager.getLogger();

	// Set in config file
	protected double percentToFf;
	// Count of number of customers for this batchtype in the input file
	protected int totalCustomers;
	// total number of customers going to FF
	protected int numberToFf;	
	// Number of customers we keep adding til we hit numberToFf
	protected int tally;
	
	public BatchType(String site) {
		this.totalCustomers = 0;
		this.tally = 0;
		
		if (site.equalsIgnoreCase("f")) {
			this.percentToFf = 1.0;
		} else if (site.equalsIgnoreCase("m")) {
			this.percentToFf = 0.0;
		} else if (NumberUtils.isCreatable(site)) {
			this.percentToFf = Double.parseDouble(site) / 100;
		} else {
			LOGGER.warn("Invalid site entry in lookup file! Site is '{}'", site);
			//System.exit(1);
		}
	}
	
	public abstract void addCustomer(Customer customer);
	
	public int getCustomerCount() {
		return totalCustomers;
	}
	
	public void increaseCount() {
		tally++;
	}
	
	public int getToFf() {
		return numberToFf;
	}
	
	public int getCount() {
		return tally;
	}
	
	public abstract void calculate();
	public abstract String getSite(Customer customer);
}
