package uk.gov.dvla.osg.calclocation.location;

import uk.gov.dvla.osg.common.classes.Customer;

public class BatchTypeSingle extends AbstractBatchType {
	
	public BatchTypeSingle(String site) {
		super(site);
	}

	@Override
	public void calculate() {
		numberToFf = (int) (totalCustomers * percentToFf);
	}

	@Override
	public void addCustomer(Customer customer) {
		this.totalCustomers++;
	}

	@Override
	public String getSite(Customer customer) {
		if (tally < numberToFf) {
			increaseCount();
			return "F";
		}
			return "M";
	}
}
