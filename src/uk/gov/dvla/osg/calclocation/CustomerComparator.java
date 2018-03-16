package uk.gov.dvla.osg.calclocation;

import java.util.Comparator;

import uk.gov.dvla.osg.common.classes.Customer;

public class CustomerComparator implements Comparator<Customer>{

	@Override
	public int compare(Customer o1, Customer o2) {
		/* SORT ORDER IS:
		 * LANGUAGE
		 * PRESENTATION_ORDER
		 * SUB_BATCH
		 * FLEET_NO
		 * MSC
		 * GRP_ID
		 */

		// First by LOCATION - stop if this gives a result.
		//int locationResult = o1.getSite().compareTo(o2.getSite());
        //if (locationResult != 0){
        //	return locationResult;
       	//}
		
	
		
		// Next by PRESENTATION_ORDER - stop if this gives a result.
		if (o1.getPresentationPriority() != null && o2.getPresentationPriority() != null) {
	        int presResult = o1.getPresentationPriority().compareTo(o2.getPresentationPriority());
	        if (presResult != 0)
	        {
	            return presResult;
	        }
		}

        return o1.getLang().compareTo(o2.getLang());

	}

}
