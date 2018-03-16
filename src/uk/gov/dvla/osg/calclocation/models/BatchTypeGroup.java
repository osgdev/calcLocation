package uk.gov.dvla.osg.calclocation.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uk.gov.dvla.osg.common.classes.Customer;


public class BatchTypeGroup extends BatchType {
	
	private List<Integer> groupIDs = new ArrayList<Integer>();
	
	public BatchTypeGroup(String site) {
		super(site);
	}
	
	public void addGroupID(String groupID) {
		increaseCount();
	}

	@Override
	public void calculate() {
		numberToFf = (int) (totalCustomers * percentToFf);
		Collections.sort(groupIDs);
		groupIDs = groupIDs.subList(0, numberToFf);
	}

	@Override
	public void addCustomer(Customer customer) {
		this.totalCustomers++;
		groupIDs.add(customer.getGroupId());
	}

	@Override
	public String getSite(Customer customer) {
		return groupIDs.contains(customer.getGroupId()) ? "F" : "M";
	}
}
