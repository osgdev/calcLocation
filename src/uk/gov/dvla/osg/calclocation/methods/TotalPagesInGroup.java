package uk.gov.dvla.osg.calclocation.methods;

import java.util.ArrayList;

import uk.gov.dvla.osg.common.classes.Customer;

public class TotalPagesInGroup {
	
	private ArrayList<Customer> group = new ArrayList<Customer>();
	private int pageInGroupCount;
	
	public void calculate(ArrayList<Customer> customers) {
		for (Customer customer : customers) {
			pageInGroupCount += customer.getNoOfPages();
			group.add(customer);
			calcTotalPagesInGroup(customer.isEog());
		}
	}

	private void calcTotalPagesInGroup(boolean isEog) {
		if (isEog) {
			group.forEach(customer -> customer.setTotalPagesInGroup(pageInGroupCount));
			pageInGroupCount = 0;
			group.clear();
		}
	}
}
