package uk.gov.dvla.osg.calclocation.models;

public class Counts {

    
    private int groupCount;
    private int itemCount;

    public Counts(int groupCount, int itemCount) {
        this.setGroupCount(groupCount);
        this.setItemCount(itemCount);
        
    }

    public int getGroupCount() {
        return groupCount;
    }

    public void setGroupCount(int groupCount) {
        this.groupCount = groupCount;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

}
