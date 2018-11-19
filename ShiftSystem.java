package nurse.scheduling;

/**
 *
 * @author Sandra
 */
public class ShiftSystem {
    private int[] startTimes;
    private int[] endTimes;
    private int[] valuesForReq;

    //int[][] req = new int[SurgeryScheduling.NBR_DAYS][SurgeryScheduling.NBR_SHIFTS];
    
    public ShiftSystem(int[] startTimes, int[] endTimes, int[] valuesForReq) {
        this.startTimes = startTimes;
        this.endTimes = endTimes;
        this.valuesForReq = valuesForReq;
    }
    
    @Override
    public String toString() {
        String s = "[" + this.getClass().getSimpleName() + "]\n";
        
        if (startTimes != null && endTimes != null) {
            for(int i = 0; i < startTimes.length; i++) {
                s += startTimes[i] + "u-" + endTimes[i] + "u\t";
            }
        }
        
        if (valuesForReq != null) {
            s += "\nRequirements per shift: ";
            for(int i = 0; i < valuesForReq.length; i++) {
                s += valuesForReq[i] + "\t";
            }        
        }
        
        return s;
    }
}
