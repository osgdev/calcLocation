package uk.gov.dvla.osg.calclocation.location;

import static uk.gov.dvla.osg.common.enums.FullBatchType.*;

import java.util.HashMap;

import uk.gov.dvla.osg.common.config.ProductionConfiguration;
import uk.gov.dvla.osg.common.enums.FullBatchType;

class BatchMapFactory {

    public static HashMap<FullBatchType, AbstractBatchType> newInstance() {
        HashMap<FullBatchType, AbstractBatchType> batchMap = new HashMap<>();
        ProductionConfiguration prodConfig = ProductionConfiguration.getInstance();
        
        // Singles
        if (isNotIgnore(SORTEDE)) {
            batchMap.put(SORTEDE, new BatchTypeSingle(prodConfig.getSite(SORTEDE)));
        }
        if (isNotIgnore(SORTEDW)) {
            batchMap.put(SORTEDW, new BatchTypeSingle(prodConfig.getSite(SORTEDW)));
        }
        if (isNotIgnore(UNSORTEDE)) {
            batchMap.put(UNSORTEDE, new BatchTypeSingle(prodConfig.getSite(UNSORTEDE)));
        }
        if (isNotIgnore(UNSORTEDW)) {
            batchMap.put(UNSORTEDW, new BatchTypeSingle(prodConfig.getSite(UNSORTEDW)));
        }
        if (isNotIgnore(SORTINGE)) {
            batchMap.put(SORTINGE, new BatchTypeSingle(prodConfig.getSite(SORTINGE)));
        }
        if (isNotIgnore(SORTINGW)) {
            batchMap.put(SORTINGW, new BatchTypeSingle(prodConfig.getSite(SORTINGW)));
        }
        if (isNotIgnore(REJECTE)) {
            batchMap.put(REJECTE, new BatchTypeSingle(prodConfig.getSite(REJECTE)));
        }
        if (isNotIgnore(REJECTW)) {
            batchMap.put(REJECTW, new BatchTypeSingle(prodConfig.getSite(REJECTW)));
        }
        if (isNotIgnore(UNCODEDE)) {
            batchMap.put(UNCODEDE, new BatchTypeSingle(prodConfig.getSite(UNCODEDE)));
        }
        if (isNotIgnore(UNCODEDW)) {
            batchMap.put(UNCODEDW, new BatchTypeSingle(prodConfig.getSite(UNCODEDW)));
        }
        
        // Groups
        if (isNotIgnore(FLEETE)) {
            batchMap.put(FLEETE, new BatchTypeGroup(prodConfig.getSite(FLEETE)));
        }
        if (isNotIgnore(FLEETW)) {
            batchMap.put(FLEETW, new BatchTypeGroup(prodConfig.getSite(FLEETW)));
        }
        if (isNotIgnore(CLERICALE)) {
            batchMap.put(CLERICALE, new BatchTypeGroup(prodConfig.getSite(CLERICALE)));
        }
        if (isNotIgnore(CLERICALW)) {
            batchMap.put(CLERICALW, new BatchTypeGroup(prodConfig.getSite(CLERICALW)));
        }
        if (isNotIgnore(MULTIE)) {
            batchMap.put(MULTIE, new BatchTypeGroup(prodConfig.getSite(MULTIE)));
        }
        if (isNotIgnore(MULTIW)) {
            batchMap.put(MULTIW, new BatchTypeGroup(prodConfig.getSite(MULTIW)));
        }
        return batchMap;
    }
    
    private static boolean isNotIgnore(FullBatchType batchType) {
        return !ProductionConfiguration.getInstance().getSite(batchType).equalsIgnoreCase("x");
    }

}
