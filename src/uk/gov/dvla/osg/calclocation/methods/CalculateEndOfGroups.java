package uk.gov.dvla.osg.calclocation.methods;

import java.util.List;

import uk.gov.dvla.osg.common.classes.Customer;
import uk.gov.dvla.osg.common.config.ProductionConfiguration;

public class CalculateEndOfGroups {
	
	public void calculate(List<Customer> input) {
		Customer currentCustomer;
		Customer nextCustomer;
		int pageCount = 0;
		int maxPages = 0;
		int nextCustIdx = 0;
		ProductionConfiguration pc = ProductionConfiguration.getInstance();
		
		for (int curCustIdx = 0; curCustIdx < input.size(); curCustIdx++) {
		    
			if (curCustIdx + 1 < input.size()) {
				nextCustIdx = curCustIdx + 1;
				currentCustomer = input.get(curCustIdx);
				nextCustomer = input.get(nextCustIdx);
				maxPages = pc.getGroupMax(currentCustomer.getFullBatchType());
				// Calculate EOG's on groups only, set EOG marker on all singles
				if (currentCustomer.getGroupId() != null) {
				    // Check if customers belong to same group, if not set EOG on the current customer
					if (currentCustomer.getGroupId().equals(nextCustomer.getGroupId())) {
						pageCount = pageCount + currentCustomer.getNoOfPages();
						// Set the EOG marker when the maxPages limit is reached
						if (pageCount + nextCustomer.getNoOfPages() > maxPages) {
							currentCustomer.setEog();
							pageCount = 0;
						}
					} else {
					    // Next customer is part of a new group so set EOG
						currentCustomer.setEog();
						pageCount = 0;
					}
				} else {
				    // Single item, not group, so set EOG
					currentCustomer.setEog();
					pageCount = 0;
				}
			} else {
				//Last customer
				currentCustomer = input.get(curCustIdx);
				currentCustomer.setEog();
			}
		}
	}
}
