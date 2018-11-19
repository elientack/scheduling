package nurse.scheduling;

/**
 *
 * @author Sandra
 */
public class Scenario {
    public static final int HOUR_SYSTEM = 9;
        
    private final String scenarioName;
    private final int[] shifts; //ex. 0 1 3 4 (no shift 2)
    private final int consecutiveDaysWork;
    private final int consecutiveDaysFree;
    private final boolean identical; //identical weekend = 1
    private final int[][] minCovReq;
    
    public Scenario(String scenarioName, int[] shifts, int consecutiveDaysWork, int consecutiveDaysFree, boolean identical, int[][] minCovReq) {
        this.scenarioName = scenarioName;
        this.shifts = shifts;
        this.consecutiveDaysWork = consecutiveDaysWork;
        this.consecutiveDaysFree = consecutiveDaysFree;
        this.identical = identical;
        this.minCovReq = minCovReq;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public int[] getShifts() {
        return shifts;
    }

    public int getConsecutiveDaysWork() {
        return consecutiveDaysWork;
    }

    public int getConsecutiveDaysFree() {
        return consecutiveDaysFree;
    }

    public boolean isIdentical() {
        return identical;
    }

    public int[][] getMinCovReq() {
        return minCovReq;
    }
    
}
