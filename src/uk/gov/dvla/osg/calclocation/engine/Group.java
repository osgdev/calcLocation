package uk.gov.dvla.osg.calclocation.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import uk.gov.dvla.osg.common.classes.Customer;

public class Group {

    private List<Customer> customers = new ArrayList<>();
    
    public void add(Customer customer) {
        this.customers.add(customer);
    }

    public int getNoOfPages() {
        return customers.stream().collect(Collectors.summingInt(Customer::getNoOfPages));
    }

    public double getSize() {
        return customers.stream().collect(Collectors.summingDouble(Customer::getSize));
    }

    public double getWeight() {
        return customers.stream().collect(Collectors.summingDouble(Customer::getWeight));
    }

    public void setSob() {
        customers.get(0).setSob();
    }

    public void setSot() {
        customers.get(0).setSot();
    }
    
    public List<Customer> getList() {
        return customers;
    }
}
