package uk.gov.dvla.osg.calclocation.comparators;

import java.util.Comparator;

import uk.gov.dvla.osg.common.classes.Customer;

public class CustomerComparatorOriginalOrder implements Comparator<Customer>{

	@Override
	public int compare(Customer o1, Customer o2) {
		/* SORT ORDER IS:
		 * LANGUAGE
		 * PRESENTATION_ORDER
		 * SUB_BATCH
		 * FLEET_NO
		 * MSC
		 * GRP_ID*/

		// First by LOCATION - stop if this gives a result.
		//int locationResult = o1.getSite().compareTo(o2.getSite());
        //if (locationResult != 0){
        //	return locationResult;
       	//}
		
	
		
		// Next by PRESENTATION_ORDER - stop if this gives a result.
        int result = o1.getOriginalIdx().compareTo(o2.getOriginalIdx());
        if (result != 0)
        {
            return result;
        }
		

        return o1.getOriginalIdx().compareTo(o2.getOriginalIdx());

	}

}
