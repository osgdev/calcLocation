package uk.gov.dvla.osg.calclocation.models;

import java.util.ArrayList;

import uk.gov.dvla.osg.common.classes.Customer;

public class Tray {

	private ArrayList<Customer> list = new ArrayList<>();
	private boolean setSot;
	private double weight;
	private double size;
	
	public Tray(Customer customer) {
		customer.setSot();
		this.size += customer.getSize();
		this.weight += customer.getWeight();
		list.add(customer);
		setSot = false;
	}
	
	public Tray() {
		setSot = true;
	}
	
	public int getNoItems() {
		return list.size();
	}
	
	public double getWeight() {
		return this.weight;
	}
	
	public double getSize() {
		return this.size;
	}

	@Override
	public String toString() {
		return "Tray [noItems=" + getNoItems() + ", weight=" + getWeight() + ", size=" + getSize() + "]";
	}
	
	public ArrayList<Customer> getList() {
		return list;
	}

	public void addItem(Customer customer) {
		if (setSot) {
			customer.setSot();
			setSot = false;
		}
		this.size += customer.getSize();
		this.weight += customer.getWeight();
		list.add(customer);
	}
	
	public void removeItem() {
		list.remove(list.size() -1);
	}

}