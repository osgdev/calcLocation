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
				
				if (currentCustomer.getGroupId() != null) {
					if (currentCustomer.getGroupId().equals(nextCustomer.getGroupId())) {
						pageCount = pageCount + currentCustomer.getNoOfPages();
						if (pageCount + nextCustomer.getNoOfPages() > maxPages) {
							currentCustomer.setEog();
							pageCount = 0;
						}
					} else {
						currentCustomer.setEog();
						pageCount = 0;
					}
				} else {
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
