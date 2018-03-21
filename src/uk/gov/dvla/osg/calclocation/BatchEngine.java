package uk.gov.dvla.osg.calclocation;

import static uk.gov.dvla.osg.common.classes.BatchType.*;
import static uk.gov.dvla.osg.common.classes.FullBatchType.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.gov.dvla.osg.calclocation.models.Tray;
import uk.gov.dvla.osg.common.classes.*;
import uk.gov.dvla.osg.common.config.EnvelopeLookup;
import uk.gov.dvla.osg.common.config.InsertLookup;
import uk.gov.dvla.osg.common.config.PapersizeLookup;
import uk.gov.dvla.osg.common.config.PostageConfiguration;
import uk.gov.dvla.osg.common.config.PresentationConfiguration;
import uk.gov.dvla.osg.common.config.ProductionConfiguration;

class BatchEngine {

	private static final Logger LOGGER = LogManager.getLogger();

	private int eightDigitJid;
	private int tenDigitJid;
	private int jidInc;

	private int minimumTrayVolume;

	private String ukmBatchTypes;
	private HashMap<String, PaperSize> papersizeLookup;
	private double maxTraySize;
	private double maxTrayWeight;
	private int batchMax;
	private int pageCount;
	private ArrayList<Customer> ukMailCustomers = new ArrayList<>();
	private ArrayList<Customer> nonUkMailCustomers = new ArrayList<>();
	private PresentationConfiguration presentationConfig;
	private Map<Integer, Integer> mscLookup = new HashMap<>();
	private HashMap<String, Insert> insertLookup;
	private HashMap<String, Envelope> envelopeLookup;
	private ProductionConfiguration prodConfig;

	BatchEngine(int tenDigitJid, int eightDigitJid, AppConfig appConfig) {

		LOGGER.trace("Starting Batch Engine");
		prodConfig = ProductionConfiguration.getInstance();
		presentationConfig = PresentationConfiguration.getInstance();
		papersizeLookup = PapersizeLookup.getInstance().getLookup();
		envelopeLookup = EnvelopeLookup.getInstance().getLookup();
		insertLookup = InsertLookup.getInstance().getLookup();
		minimumTrayVolume = PostageConfiguration.getInstance().getUkmMinimumTrayVolume();
		maxTraySize = ProductionConfiguration.getInstance().getTraySize();
		maxTrayWeight = PostageConfiguration.getInstance().getMaxTrayWeight();
		ukmBatchTypes = PostageConfiguration.getInstance().getUkmBatchTypes();
		jidInc = appConfig.getTenDigitJobIdIncrementValue();
		this.eightDigitJid = eightDigitJid;
		this.tenDigitJid = tenDigitJid;
	}

	public void batch(ArrayList<Customer> customers) {
		countMscs(customers);
		adjustMultis(customers);
		mscLookup.clear();
		Collections.sort(customers, new CustomerComparatorWithLocation());
		countMscs(customers);
		filterCustomers(customers);

		int customerIndex = 0;
		pageCount = 0;
		boolean firstCustomer = true;
		Customer prev = ukMailCustomers.isEmpty() ? null : ukMailCustomers.get(0);

		for (Customer customer : ukMailCustomers) {
			batchMax = getBatchMax(customer.getFullBatchType(), customer.getPaperSize());
			boolean changeOfMsc = !customer.getMsc().equals(prev.getMsc());
			int endIndex = mscLookup.get(customer.getTransactionID()) + customerIndex;

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
				// Same MSC, Different Batch Type
				LOGGER.fatal("NOT YET IMPLEMENTED METHOD! MSC {}", customer.getMsc());
			} else {
				// Same MSC, Same Batch Type
			}

			// End of Loop - next customer
			prev = customer;
			customerIndex++;
		}

		// Loop through nonUkMailCustomers
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
		Collections.sort(customers, new CustomerComparatorWithLocation());

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

	}

	private ArrayList<Tray> setTraysForMsc(int startIndex, int endIndex) {

		ArrayList<Tray> trays = new ArrayList<>();
		// NEW MSC start fresh tray
		Tray tray = new Tray();

		for (Customer customer : ukMailCustomers.subList(startIndex, endIndex)) {
			if (customer.getNoOfPages() + pageCount <= batchMax) {
				// Same Batch - check if weight and size would put tray over limits
				if (customer.getThickness() + tray.getSize() > maxTraySize
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
				if (customer.getThickness() + tray.getSize() > maxTraySize
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

	private void adjustTrays(ArrayList<Tray> trays) {

		int numberOfTrays = trays.size();
		boolean adjust = false;

		// check if any Tray is below minimum size
		for (Tray tray : trays) {
			//LOGGER.debug("{}", tray.toString());
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

	private void adjustMultis(ArrayList<Customer> allCustomers) {
		for (Customer customer : allCustomers) {
			double weight = 0;
			double size = 0;
			if (BatchType.MULTI.equals(customer.getBatchType())
					&& mscLookup.get(customer.getTransactionID()) >= minimumTrayVolume) {
				if (!customer.isEog()) {
					weight += customer.getWeight();
					size += customer.getThickness();
				} else {
					// change envelope
					if (customer.getLang().equals(Language.E)) {
						customer.setEnvelope(ProductionConfiguration.getInstance().getEnvelopeEnglishMm());
					} else {
						customer.setEnvelope(ProductionConfiguration.getInstance().getEnvelopeWelshMm());
					}
					double envelopeSize = envelopeLookup.get(customer.getEnvelope()).getThickness();
					double envelopeWeight = envelopeLookup.get(customer.getEnvelope()).getWeight();
					double insertSize = 0;
					double insertWeight = 0;
					if (!customer.getInsertRef().isEmpty()) {
						insertSize = insertLookup.get(customer.getInsertRef()).getThickness();
						insertWeight = insertLookup.get(customer.getInsertRef()).getWeight();
					}
					customer.setWeight(customer.getWeight() + weight + envelopeWeight + insertWeight);
					customer.setThickness(customer.getThickness() + size + envelopeSize + insertSize);

				}
			} else if (customer.getBatchType().equals(BatchType.MULTI)) {
				if (customer.getLang().equals(Language.E)) {
					if (!prodConfig.getSite(SORTEDE).equals("X")) {
						customer.setBatchType(SORTED);
					} else {
						customer.setBatchType(UNSORTED);
					}
				} else {
					if (!prodConfig.getSite(SORTEDW).equals("X")) {
						customer.setBatchType(SORTED);
					} else {
						customer.setBatchType(UNSORTED);
					}
				}
				customer.setEog();
				customer.setGroupId(null);
				customer.setPresentationPriority(presentationConfig.lookupRunOrder(customer.getBatchName()));
				// change envelope
				if (customer.getLang().equals(Language.E)) {
					customer.setEnvelope(ProductionConfiguration.getInstance().getEnvelopeEnglishMm());
				} else {
					customer.setEnvelope(ProductionConfiguration.getInstance().getEnvelopeWelshMm());
				}
				double envelopeSize = envelopeLookup.get(customer.getEnvelope()).getThickness();
				double envelopeWeight = envelopeLookup.get(customer.getEnvelope()).getWeight();
				double insertSize = 0;
				double insertWeight = 0;
				if (!customer.getInsertRef().isEmpty()) {
					insertSize = insertLookup.get(customer.getInsertRef()).getThickness();
					insertWeight = insertLookup.get(customer.getInsertRef()).getWeight();
				}
				customer.setWeight(customer.getWeight() + weight + envelopeWeight + insertWeight);
				customer.setThickness(customer.getThickness() + size + envelopeSize + insertSize);
				customer.setSite(ProductionConfiguration.getInstance().getSite(customer.getFullBatchType()));
			}
		}
	}

	private void filterCustomers(ArrayList<Customer> allCustomers) {
		for (Customer customer : allCustomers) {
			if (!customer.getBatchType().equals(BatchType.UNSORTED)
					&& ukmBatchTypes.contains(customer.getBatchName())) {
				if (mscLookup.get(customer.getTransactionID()) < minimumTrayVolume) {
					// MSCS are under minimum tray volume so move to unsorted list
					customer.setBatchType(BatchType.UNSORTED);
					customer.setEog();
					customer.setPresentationPriority(presentationConfig.lookupRunOrder(customer.getBatchName()));
					// Change location
					customer.setSite(prodConfig.getSite(customer.getFullBatchType()));
					// change product
					customer.setProduct(Product.UNSORTED);
					// change envelope
					if (customer.getLang().equals(Language.E)) {
						customer.setEnvelope(prodConfig.getEnvelopeEnglishUnsorted());
					} else {
						customer.setEnvelope(prodConfig.getEnvelopeWelshUnsorted());
					}
					nonUkMailCustomers.add(customer);
				} else {
					ukMailCustomers.add(customer);
				}
			} else {
				nonUkMailCustomers.add(customer);
			}
		}
	}

	private void countMscs(ArrayList<Customer> input) {
		// Calculate the start time
		int transactionID = 1;
		int groupCount = 0;
		Customer prev = input.get(0);
		for (Customer customer : input) {
			// Check if customer has an MSC
			if (StringUtils.isNotBlank(customer.getMsc()) && !customer.getBatchType().equals(BatchType.UNSORTED)
					&& ukmBatchTypes.contains(customer.getBatchName())) {

				if (!customer.equals(prev) || !customer.getMsc().equals(prev.getMsc())) {
					transactionID++;
					groupCount = 0;
				}

				// Add result to lookup map
				if (customer.isEog()) {
					groupCount++;
				}
				customer.setTransactionID(transactionID);
				mscLookup.put(transactionID, groupCount);
				prev = customer;
			}
		}
	}

	private int getBatchMax(FullBatchType fullBatchType, String paperSize) {
		int batchmax = ProductionConfiguration.getInstance().getBatchMax(fullBatchType);
		return papersizeLookup.containsKey(paperSize)
				? (int) (batchmax * papersizeLookup.get(paperSize).getMultiplier())
				: batchmax;
	}

}
