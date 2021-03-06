package uk.gov.dvla.osg.calclocation.engine;

import static uk.gov.dvla.osg.common.enums.BatchType.*;

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.gov.dvla.osg.calclocation.comparators.CustomerComparatorWithLocation;
import uk.gov.dvla.osg.calclocation.main.AppConfig;
import uk.gov.dvla.osg.common.classes.Customer;
import uk.gov.dvla.osg.common.config.*;
import uk.gov.dvla.osg.common.enums.*;

/**
 * Sets and adjusts trays to ensure they are within UK Mail limits. 
 * Also partitions off batches according to batchMax levels and sets the JobId and PieceId values 
 * for docs within each batch.
 * @author OSG
 *
 */
public class BatchEngine {

	private static final Logger LOGGER = LogManager.getLogger();

	private int eightDigitJid;
	private int tenDigitJid;
	private int jidInc;

	private int minimumTrayVolume;
	private List<BatchType> ukmBatchTypes;
	private PapersizeLookup papersizeLookup;
	private double maxTraySize;
	private double maxTrayWeight;
	private int batchMax;
	private int pageCount;

	private ArrayList<Customer> ukMailCustomers = new ArrayList<>();
	private ArrayList<Customer> nonUkMailCustomers = new ArrayList<>();
	private Map<Integer, Counts> mscLookup = new HashMap<>();
	private InsertLookup insertLookup;
	private EnvelopeLookup envelopeLookup;
	private PresentationConfiguration presConfig;
	private ProductionConfiguration prodConfig;
	
	/**
	 * Instantiates a new batch engine.
	 * @param tenDigitJid the RPD jid
	 * @param eightDigitJid the IPW jid
	 */
	public BatchEngine(int tenDigitJid, int eightDigitJid) {
		LOGGER.trace("Starting Batch Engine");
		this.eightDigitJid = eightDigitJid;
		this.tenDigitJid = tenDigitJid;

		prodConfig = ProductionConfiguration.getInstance();
		presConfig = PresentationConfiguration.getInstance();

		papersizeLookup = PapersizeLookup.getInstance();
		envelopeLookup = EnvelopeLookup.getInstance();
		insertLookup = InsertLookup.getInstance();

		maxTraySize = prodConfig.getTraySize();
		minimumTrayVolume = PostageConfiguration.getInstance().getUkmMinimumTrayVolume();
		maxTrayWeight = PostageConfiguration.getInstance().getMaxTrayWeight();
		ukmBatchTypes = PostageConfiguration.getInstance().getUkmBatchTypes();
		jidInc = AppConfig.getInstance().getTenDigitJobIdIncrementValue();
	}

	public void batch(ArrayList<Customer> customers) {
	    
		countMscs(customers);
		adjustMultis(customers);
		Collections.sort(customers, new CustomerComparatorWithLocation());
		
		countMscs(customers);
        filterCustomers(customers);
        Collections.sort(customers, new CustomerComparatorWithLocation());

        // Throws indexOutOfBounds if empty - PB 04/06
        if (ukMailCustomers.size() > 0) {
            Collections.sort(ukMailCustomers, new CustomerComparatorWithLocation());
            countMscs(ukMailCustomers);
        }
		
		// Process ukMailCustomers
		int customerIndex = 0;
		pageCount = 0;
		boolean firstCustomer = true;
		Customer prev = ukMailCustomers.isEmpty() ? null : ukMailCustomers.get(0);

		for (Customer customer : ukMailCustomers) {
			batchMax = getBatchMax(customer.getFullBatchType(), customer.getPaperSize());
			boolean changeOfMsc = !customer.getMsc().equals(prev.getMsc());
			int endIndex = 0;
			try {
			endIndex = mscLookup.get(customer.getTransactionID()).getItemCount() + customerIndex;
			} catch (Exception ex) {
			  LOGGER.debug(customer.toString());
			  System.exit(1);
			}
			if (firstCustomer) {
				customer.setSob();
				ArrayList<Tray> trays = setTraysForMsc(customerIndex, endIndex);
				adjustTrays(trays);
				firstCustomer = false;
			} else if (changeOfMsc && prev.equals(customer)) {
				// Next MSC, Same Transaction Type
				ArrayList<Tray> trays = setTraysForMsc(customerIndex, endIndex);
				adjustTrays(trays);
			} else if (changeOfMsc && !prev.equals(customer)) {
				// NEW BATCH
				pageCount = 0;
				customer.setSob();
				// Next MSC, different Transaction Type
				ArrayList<Tray> trays = setTraysForMsc(customerIndex, endIndex);
				adjustTrays(trays);
			} else if (!changeOfMsc && !prev.equals(customer)) {
			    // Same MSC, different Stationery or Transaction Type
                pageCount = 0;
                customer.setSob();
                ArrayList<Tray> trays = setTraysForMsc(customerIndex, endIndex);
                adjustTrays(trays);
			} else {
				// Same MSC, Same Batch Type -> do nothing
			}

			// End of Loop - next customer
			prev = customer;
			customerIndex++;
		}
		
		// Added if block, PB - 06/04
		// Loop through nonUkMailCustomers if not empty
		if (!nonUkMailCustomers.isEmpty()) {
			firstCustomer = true;
			prev = nonUkMailCustomers.get(0);
			pageCount = 0;
			
			for (Customer customer : nonUkMailCustomers) {
				batchMax = getBatchMax(customer.getFullBatchType(), customer.getPaperSize());
				customer.setMsc("");
				if (firstCustomer) {
					customer.setSob();
					firstCustomer = false;
					pageCount = customer.getNoOfPages();					
				} else if (!prev.equals(customer) || customer.getNoOfPages() + pageCount > batchMax) {
					customer.setSob();
					pageCount = customer.getNoOfPages();
					prev.setEog();
				} else {
					pageCount += customer.getNoOfPages();
				}
				prev = customer;
			}
		}

		// Loop through all customers and set JID's, PID & batch sequence
		int pid = 1;
		int batchSequence = 0;
		
		for (Customer customer : customers) {
			if (customer.isSob()) {
				pid = 1;
				batchSequence++;
				tenDigitJid += jidInc;
			}
			
			customer.setSequenceInChild(pid);
			customer.setTenDigitJid(tenDigitJid);
			customer.setEightDigitJid(eightDigitJid);
			customer.setBatchSequence(batchSequence);
			
			pid++;
		}
		
	      //ukMailCustomers.stream().filter(c -> c.isEog()).forEach(c -> LOGGER.debug(c.toString()));
	}

	/**
	 * Loop throuh all customers in the given range, ensuring that they are within the limits of trayWeight and traySize. 
	 * When limits are reached a new tray is started. 
	 * When the batchMax is reached a new batch and new tray is started. 
	 * Note - changing trays when a limit is reached could lead to a tray being under the minimum volume required by UK Mail. 
	 * This scenario is handled in the adjustTrays method.
	 * @param startIndex - first customer with MSC
	 * @param endIndex - final customer with MSC
	 * @return - all customers for the range divided into trays
	 */
	private ArrayList<Tray> setTraysForMsc(int startIndex, int endIndex) {

	    
		ArrayList<Tray> trays = new ArrayList<>();
		// NEW MSC start fresh tray
		Tray tray = new Tray();
		
		List<Group> envelopes = new ArrayList<>();
		Group group = new Group();
		
		for (Customer customer : ukMailCustomers.subList(startIndex, endIndex)) {
		    group.add(customer);
		    if (customer.isEog()) {
		        envelopes.add(group);
		        group = new Group();
		    }
		}
		
		for (Group customer : envelopes) {
			if (customer.getNoOfPages() + pageCount <= batchMax) {
				// Same Batch - check if weight and size would put tray over limits
				if (customer.getSize() + tray.getSize() > maxTraySize
						|| customer.getWeight() + tray.getWeight() > maxTrayWeight) {
					// Tray limits exceeded - start a new tray
					trays.add(tray);
					tray = new Tray(customer);
				} else {
					// Within limits so add to current tray
					tray.addItem(customer);
				}
				pageCount += customer.getNoOfPages();
			} else {
				// New Batch = set customer as SOB and start next tray
				pageCount = customer.getNoOfPages();
				customer.setSob();
				if (customer.getSize() + tray.getSize() > maxTraySize
						|| customer.getWeight() + tray.getWeight() > maxTrayWeight) {
					// Tray limits exceeded - start a new tray
					trays.add(tray);
					tray = new Tray(customer);
				} else {
					// Within limits so add to current tray
					tray.addItem(customer);
				}
			}
		}
		// Add final tray to list
		trays.add(tray);
		return trays;
	}

	/**
	 * If any trays are below the minimum volume accepted by UK Mail, 
	 * then all envelopes are put into a temporary list which is divided up according to the number of trays that were passed into the method. 
	 * The result is that all items are evenly spread across all trays without going over the tray limits. 
	 * A side-effect of this way of splitting documents is that SOT and SOB markers may move out of position, 
	 * so extra checks are utilised that move markers to the start of the relevant tray and page-counts are re-started to keep batches synchronised.
	 * @param trays Trays for same mailsort code.
	 */
	private void adjustTrays(ArrayList<Tray> trays) {

	    int numberOfTrays = trays.size();
		boolean adjust = false;

		// check if any Tray is below minimum size
		for (Tray tray : trays) {
			if (tray.getNoItems() < minimumTrayVolume) {
				adjust = true;
			}
		}

		if (adjust) {
			double totalWeight = 0;
			double totalSize = 0;
			double totalItems = 0;

			for (Tray tray : trays) {
				totalWeight += tray.getWeight();
				totalSize += tray.getSize();
				totalItems += tray.getNoItems();
			}

			double averageWeight = totalWeight / numberOfTrays;
			double averageSize = totalSize / numberOfTrays;
			// If averages are above limit we need an extra tray :(
			if (averageWeight > maxTrayWeight || averageSize > maxTraySize) numberOfTrays++;
			// We use ceiling to avoid the possiblity of having a tray with 1 item
			int averageItems = (int) Math.ceil(totalItems / numberOfTrays);

			// Weight & Size OK, so unpack all trays into temp array
			ArrayList<Customer> temp = new ArrayList<>();
			trays.forEach(tray -> temp.addAll(tray.getList()));
			// clear SOT markers for every customer
			temp.forEach(customer -> customer.clearSot());
			// Set SOT according to the computed average
			int counter = 0;
			for (Customer customer : temp) {
				if (counter % averageItems == 0) {
					customer.setSot();
				}
				if (customer.isSob()) {
					// Tray has SOB mid-way through, move to start of tray
					customer.clearSob();
					int sot = counter - (counter % averageItems);
					temp.get(sot).setSob();
				}
				counter++;
			}
		} else {
			// Trays don't need adjusting - check for SOB
			boolean restartPageCount = false;

			for (int trayIdx = 0; trayIdx < trays.size(); trayIdx++) {
				for (Customer customer : trays.get(trayIdx).getList()) {
					if (customer.isSob()) {
						restartPageCount = true;
						// Move sob to start of tray
						customer.clearSob();
						trays.get(trayIdx).getList().get(0).setSob();
						// Re-set Page Count
						pageCount = 0;
					}
				}
				if (restartPageCount) {
					// New batch was started - add the customers in all trays to the page count
					for (Customer c : trays.get(trayIdx).getList()) {
						pageCount += c.getNoOfPages();
					}
				}
			}
		}
	}

	/**
	 * Set weight and size for multi customer when above tray minimum. 
	 * Set multis to Sorted (unsorted if sorted is ignored for Selector), 
	 * when there are not enough envelopes (EOG's) to meet the minimumTrayVolume for Uk Mail.
	 * @param allCustomers
	 */
	private void adjustMultis(ArrayList<Customer> allCustomers) {
		
		for (Customer customer : allCustomers) {
			double weight = 0;
			double size = 0;
			// Multi Customer - volume OK to send via UK Mail
			if (MULTI.equals(customer.getBatchType()) && mscLookup.get(customer.getTransactionID()).getGroupCount() >= minimumTrayVolume) {
				if (!customer.isEog()) {
					weight += customer.getWeight();
					size += customer.getSize();
				} else {
					// change envelope
					if (customer.getLang().equals(Language.E)) {
						customer.setEnvelope(prodConfig.getEnvelopeEnglishMm());
					} else {
						customer.setEnvelope(prodConfig.getEnvelopeWelshMm());
					}
					double envelopeSize = envelopeLookup.get(customer.getEnvelope()).getThickness();
					double envelopeWeight = envelopeLookup.get(customer.getEnvelope()).getWeight();
					double insertSize = 0;
					double insertWeight = 0;
					// Check if an insert is required.
					if (StringUtils.isNotBlank(customer.getInsertRef())) {
						insertSize = insertLookup.get(customer.getInsertRef()).getThickness();
						insertWeight = insertLookup.get(customer.getInsertRef()).getWeight();
					}
					customer.setWeight(customer.getWeight() + weight + envelopeWeight + insertWeight);
					customer.setSize(customer.getSize() + size + envelopeSize + insertSize);

				}
			} else if (MULTI.equals(customer.getBatchType())) {
			    // Multi Customer - volume below tray limit so send as Single instead
				// Sorted if MailMark product & switched on in config file - PB 24/04/18
				if (!prodConfig.getSite(FullBatchType.valueOf(SORTED + customer.getLang().name())).equals("X")) {
					customer.setBatchType(SORTED);
					customer.setPresentationPriority(presConfig.lookupRunOrder("SORTED"));
	                // PB 19/02/19 - envelope type changed to MM
                    if (customer.getLang().equals(Language.E)) {
                        customer.setEnvelope(prodConfig.getEnvelopeEnglishMm());
                    } else {
                        customer.setEnvelope(prodConfig.getEnvelopeWelshMm());
                    }
				} else {
					customer.setBatchType(UNSORTED);
					customer.setPresentationPriority(presConfig.lookupRunOrder("UNSORTED"));
					customer.setProduct(Product.UNSORTED);
					customer.setMsc("99999");
		            // PB 19/02/19 - envelope type changed from MM to UNSORTED
	                if (customer.getLang().equals(Language.E)) {
	                    customer.setEnvelope(prodConfig.getEnvelopeEnglishUnsorted());
	                } else {
	                    customer.setEnvelope(prodConfig.getEnvelopeWelshUnsorted());
	                }
				}
				
				customer.setSite(prodConfig.getSite(customer.getFullBatchType()));

                double envelopeSize = envelopeLookup.get(customer.getEnvelope()).getThickness();
                double envelopeWeight = envelopeLookup.get(customer.getEnvelope()).getWeight();
                double insertSize = 0;
                double insertWeight = 0;
                
                if (StringUtils.isNotBlank(customer.getInsertRef())) {
                    insertSize = insertLookup.get(customer.getInsertRef()).getThickness();
                    insertWeight = insertLookup.get(customer.getInsertRef()).getWeight();
                }
                
                // PB 19/02/19 : Switch grouped items to singles when set in production config, otherwise leave as grouped
                if (!prodConfig.isMultiUnsorted()) {
                    customer.setGroupId(null);
                    customer.setTotalPagesInGroup(customer.getNoOfPages());
                    customer.setEog();
                    customer.setWeight(customer.getWeight() + weight + envelopeWeight + insertWeight);
                    customer.setSize(customer.getSize() + size + envelopeSize + insertSize);
                    String msc = customer.getLang().name() + customer.getBatchType().name() + customer.getSubBatch() + customer.getMsc();
                    LOGGER.debug("Groups in UNSORTED switched on in config. MULTI changed  to {} & Group ID kept for customer with MSC {}", customer.getBatchName(), msc);
                }
                
			}
		}
	}

	/**
	 * Divides the original input into lists of Uk Mail customers and Non UK Mail customers. 
	 * If number for the MSC is below the minimum volume, the batch type is changed to UnSorted.
	 * @param allCustomers
	 */
	private void filterCustomers(ArrayList<Customer> allCustomers) {
		for (Customer customer : allCustomers) {
			// if (processUkMail && ukmBatchTypes.contains(customer.getBatchType())) {
			if (ukmBatchTypes.contains(customer.getBatchType())) {
				if (mscLookup.get(customer.getTransactionID()).getGroupCount() < minimumTrayVolume) {
					// MSCS are under minimum tray volume so move to unsorted list
					customer.setBatchType(BatchType.UNSORTED);
					customer.setProduct(Product.UNSORTED);
					customer.setMsc("99999");
					customer.setEog();
					customer.setPresentationPriority(presConfig.lookupRunOrder(customer.getBatchName()));
					// Change location
					customer.setSite(prodConfig.getSite(customer.getFullBatchType()));
					// change envelope
					if (customer.getLang().equals(Language.E)) {
						customer.setEnvelope(prodConfig.getEnvelopeEnglishUnsorted());
					} else {
						customer.setEnvelope(prodConfig.getEnvelopeWelshUnsorted());
					}
					if (ukmBatchTypes.contains(BatchType.UNSORTED)) {
					    ukMailCustomers.add(customer);
					} else {
					    nonUkMailCustomers.add(customer);
					}
				} else {
					ukMailCustomers.add(customer);
				}
			} else {
				nonUkMailCustomers.add(customer);
			}
		}
	}

	/**
	 * Count number of records with same transaciton type & same MSC. 
	 * Customers of same type are assigned a transaction ID and this is stored in their properties. 
	 * This allows for the group count to be looked up for each individual customer.
	 * @param input
	 */
	private void countMscs(ArrayList<Customer> input) {
	    
	    mscLookup.clear();  // Reset Map
	    
		int transactionID = 1;
		int groupCount = 0;
		int itemCount = 0;
		
		Customer prev = input.get(0);
		
		for (Customer customer : input) {
			// Check if customer has an MSC
			if (StringUtils.isNotBlank(customer.getMsc()) && ukmBatchTypes.contains(customer.getBatchType())) {
			    
			    
				if (!customer.equals(prev) || !customer.getMsc().equals(prev.getMsc())) {
					transactionID++;
					groupCount = 0;
					itemCount = 0;
				} 

				if (customer.isEog()) {
					groupCount++;
				}

				itemCount++;
				
				// Add result to lookup map
				customer.setTransactionID(transactionID);
				mscLookup.put(transactionID, new Counts(groupCount, itemCount));
				prev = customer;
			}
		}
	}

	/**
	* Batch max is increased by the value of the multiplier when docs are folded.
	* @param fullBatchType
	* @param paperSize
	* @return
	*/
	private int getBatchMax(FullBatchType fullBatchType, String paperSize) {
		int batchmax = ProductionConfiguration.getInstance().getBatchMax(fullBatchType);
		return papersizeLookup.containsKey(paperSize)
				? (int) (batchmax * papersizeLookup.get(paperSize).getMultiplier())
				: batchmax;
	}

}
