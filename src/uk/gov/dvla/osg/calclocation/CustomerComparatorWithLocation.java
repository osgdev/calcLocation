package uk.gov.dvla.osg.calclocation;

import java.util.Comparator;

import uk.gov.dvla.osg.common.classes.Customer;

public class CustomerComparatorWithLocation implements Comparator<Customer> {

	@Override
	public int compare(Customer o1, Customer o2) {
		/*
		 * SORT ORDER IS: LOCATION -> LANGUAGE -> STATIONERY -> PRESENTATION_ORDER -> SUB_BATCH -> SORT_FIELD -> FLEET_NO -> MSC -> GRP_ID
		 */

		// First by LOCATION - stop if this gives a result.
		int locationResult = o1.getSite().compareTo(o2.getSite());
		if (locationResult != 0) {
			return locationResult;
		}

		// Next by LANGUAGE - stop if this gives a result.
		int langResult = o1.getLang().compareTo(o2.getLang());
		if (langResult != 0) {
			return langResult;
		}

		// Next by STATIONERY - stop if this gives a result.
		int statResult = o1.getStationery().compareTo(o2.getStationery());
		if (statResult != 0) {
			return statResult;
		}

		// Next by PRESENTATION_ORDER - stop if this gives a result.

		if (o1.getPresentationPriority() != null && o2.getPresentationPriority() != null) {
			int presResult = o1.getPresentationPriority().compareTo(o2.getPresentationPriority());
			if (presResult != 0) {
				return presResult;
			}
		}

		// Next by SUB_BATCH - stop if this gives a result.
		if (o1.getSubBatch() != null && o2.getSubBatch() != null) {
			int subBatchResult = o1.getSubBatch().compareTo(o2.getSubBatch());
			if (subBatchResult != 0) {
				return subBatchResult;
			}
		}

		// Next by SORT_FIELD
		int sortFieldResult = o1.getSortField().compareTo(o2.getSortField());
		if (sortFieldResult != 0) {
			return sortFieldResult;
		}
		// Next by FLEET_NO
		int fleetResult = o1.getFleetNo().compareTo(o2.getFleetNo());
		if (fleetResult != 0) {
			return fleetResult;
		}

		// Next by MSC
		int mscResult = o1.getMsc().compareTo(o2.getMsc());
		if (mscResult != 0) {
			return mscResult;
		}

		// Finally by GRP_ID
		if (o1.getGroupId() != null && o2.getGroupId() != null) {
			return o1.getGroupId().compareTo(o2.getGroupId());
		}
		return 0;
	}
}
