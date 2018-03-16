package uk.gov.dvla.osg.calclocation;
/*package uk.gov.dvla.osg.batch;

import static uk.gov.dvla.osg.common.classes.BatchType.*;
import static uk.gov.dvla.osg.common.classes.FullBatchType.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.gov.dvla.osg.batch.models.Tray;
import uk.gov.dvla.osg.common.classes.BatchType;
import uk.gov.dvla.osg.common.classes.Customer;
import uk.gov.dvla.osg.common.classes.FullBatchType;
import uk.gov.dvla.osg.common.classes.Language;
import uk.gov.dvla.osg.common.classes.Product;
import uk.gov.dvla.osg.common.config.EnvelopeLookup;
import uk.gov.dvla.osg.common.config.InsertLookup;
import uk.gov.dvla.osg.common.config.PapersizeLookup;
import uk.gov.dvla.osg.common.config.PostageConfiguration;
import uk.gov.dvla.osg.common.config.PresentationConfiguration;
import uk.gov.dvla.osg.common.config.ProductionConfiguration;

class BatchEngineBackup {

	private static final Logger LOGGER = LogManager.getLogger();

	private int eightDigitJid;
	private int tenDigitJid;
	private int jidInc;

	private int minimumTrayVolume;

	private String ukmBatchTypes;
	private HashMap<String, PapersizeLookup> papersizeLookup;

	private double maxTraySize;
	private double maxTrayWeight;
	private int batchMax;
	private int pageCount;

	private ArrayList<Customer> ukMailCustomers = new ArrayList<>();
	private ArrayList<Customer> nonUkMailCustomers = new ArrayList<>();
	private PresentationConfiguration presentationConfig;
	private Map<Integer, Integer> mscLookup = new HashMap<>();

	private HashMap<String, InsertLookup> insertLookup;

	private HashMap<String, EnvelopeLookup> envelopeLookup;

	private ProductionConfiguration prodConfig;

	BatchEngineBackup(PostageConfiguration postConfig, int tenDigitJid, int eightDigitJid, AppConfig appConfig,
			PapersizeLookup pl, EnvelopeLookup envelopeLookup, InsertLookup insertLookup) {

		LOGGER.trace("Starting Batch Engine");
		this.prodConfig = ProductionConfiguration.getInstance();
		this.presentationConfig = PresentationConfiguration.getInstance();
		this.papersizeLookup = pl.getLookup();
		this.eightDigitJid = eightDigitJid;
		this.tenDigitJid = tenDigitJid;
		this.jidInc = appConfig.getTenDigitJobIdIncrementValue();
		this.envelopeLookup = envelopeLookup.getLookup();
		this.insertLookup = insertLookup.getLookup();
		minimumTrayVolume = postConfig.getUkmMinimumTrayVolume();
		maxTraySize = (double) ProductionConfiguration.getInstance().getTraySize();
		maxTrayWeight = (double) appConfig.getMaxWeight();
		ukmBatchTypes = postConfig.getUkmBatchTypes();
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
		Customer prev = ukMailCustomers.get(0);

		for (Customer customer : ukMailCustomers) {
			batchMax = getBatchMax(customer.getFullBatchType(), customer.getPaperSize());
			boolean changeOfMsc = !customer.getMsc().equals(prev.getMsc());
			int endIndex = mscLookup.get(customer.getOriginalIdx()) + customerIndex;

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
				LOGGER.debug("MSC {}", customer.getMsc());
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
		AtomicBoolean adjust = new AtomicBoolean(false);

		// check if any Tray is below minimum size
		trays.forEach(tray -> {
			if (tray.getNoItems() < minimumTrayVolume) adjust.set(true);
		});

		if (adjust.get()) {
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
			int averageItems = (int) Math.ceil(totalItems / numberOfTrays);

			// Weight & Size OK, so unpack all trays into temp array
			ArrayList<Customer> temp = new ArrayList<>();
			trays.forEach(tray -> temp.addAll(tray.getList()));
			// clear SOT markers for every customer
			temp.forEach(customer -> customer.clearSot());
			// set SOT at average
			int counter = 0;
			for (Customer customer : temp) {
				if (counter % averageItems == 0) {
					customer.setSot("X");
				}
				if (customer.isSob()) {
					// Tray has SOB mid-way through, move to start of tray
					int sot = counter - (counter % averageItems);
					temp.get(sot).setSob();
					// Clear SOB marker from current item
					customer.clearSob();
				}
				counter++;
			}
		} else {
			// Trays don't need adjusting - check for SOB
			for (int trayIdx = 0; trayIdx < trays.size(); trayIdx++) {
				for (Customer customer : trays.get(trayIdx).getList()) {
					if (customer.isSob()) {
						//adjust tray
						customer.clearSob();
						trays.get(trayIdx).getList().get(0).setSob();
						// RE-set PAGE Count
						int pc = 0;
						for (Customer c : trays.get(trayIdx).getList()) {
							pc += c.getNoOfPages();
						}
						pageCount = pc;
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
					&& mscLookup.get(customer.getOriginalIdx()) >= minimumTrayVolume) {
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
						float insertSize = 0;
						float insertWeight = 0;
						if (!customer.getInsertRef().isEmpty()) {
							insertSize = insertLookup.get(customer.getInsertRef()).getThickness();
							insertWeight = insertLookup.get(customer.getInsertRef()).getWeight();
						}
						customer.setWeight(customer.getWeight() + weight + envelopeWeight + insertWeight);
						customer.setThickness(customer.getThickness() + size + envelopeSize + insertSize);

					}
			} else if (customer.getBatchType().equals(BatchType.MULTI)
					&& mscLookup.get(customer.getOriginalIdx()) < minimumTrayVolume) {
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
				customer.setGroupId("");
				customer.setPresentationPriority(presentationConfig.lookupRunOrder(customer.getBatchName()));
				// change envelope
				if (customer.getLang().equals(Language.E)) {
					customer.setEnvelope(ProductionConfiguration.getInstance().getEnvelopeEnglishMm());
				} else {
					customer.setEnvelope(ProductionConfiguration.getInstance().getEnvelopeWelshMm());
				}
				double envelopeSize = envelopeLookup.get(customer.getEnvelope()).getThickness();
				double envelopeWeight = envelopeLookup.get(customer.getEnvelope()).getWeight();
				float insertSize = 0;
				float insertWeight = 0;
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
			if (!customer.getBatchType().equals(BatchType.UNSORTED) && ukmBatchTypes.contains(customer.getBatchName())) {
				if (mscLookup.get(customer.getOriginalIdx()) < minimumTrayVolume) {
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
		for (Customer customer : input) {
			// Check if customer has an MSC
			if (StringUtils.isNotBlank(customer.getMsc())) {
				// Atomic counter required for lambda foreach loop
				AtomicInteger mscsCount = new AtomicInteger(0);
				// Start to iterate from the index position of current customer
				input.listIterator(input.indexOf(customer)).forEachRemaining(c -> {
					if (c.getMsc().equals(customer.getMsc()) && c.equals(customer) && c.isEog()) {
						mscsCount.incrementAndGet();
					}
				});
				// Add result to lookup map
				mscLookup.put(customer.getOriginalIdx(), mscsCount.get());
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
*/