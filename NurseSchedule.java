package nurse.scheduling;

import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author Sandra
 */
public class NurseSchedule implements Comparable<NurseSchedule> {

    private static final int MIN = 0;
    private static final int MAX = 1;

    public static final int COST_SURPLUS_NURSE = 20;
    public static final int COST_UNSATISFIED_COV_REQ = 100;
    public static final int COST_FAIRNESS = 5;

    //FIXED COSTS FOR NORMALIZATION
//    public static final int MIN_COST_PREFERENCE = 5;
//    public static final int MIN_COST_MINCOVREQ = 5;
//    public static final int MIN_COST_SURPLUSNURSES = 5;
//    public static final int MIN_COST_FAIRNESS = 5;
//    public static final int MIN_COST_REALCOSTS = 5;
//    public static final int MAX_COST_PREFERENCE = 5;
//    public static final int MAX_COST_MINCOVREQ = 5;
//    public static final int MAX_COST_SURPLUSNURSES = 5;
//    public static final int MAX_COST_FAIRNESS = 5;
//    public static final int MAX_COST_REALCOSTS = 5;    
    private final Scenario scenario;
    private final char department;
    private String name;
    private final int[] shifts; //early day late night (=5 shifts because also 0=free)
    private ArrayList<Nurse> setOfNursesUsed = null;
    private int[][] schedule = null; //per nurse per day which shift
    private final int hourSystemCost;
    private double wagesCost;
    private final int preferenceCost;
    private int covReqCost;
    private int surplusCost;
    private int fairnessCost;
    private int totalPenaltyCost; //high cost = negative!
    private double realCosts; //hourSystemCost + wagesCost
    private double allCosts;

    private double normalized_preferenceCost;//0-1
    private double normalized_covReqCost;//0-1
    private double normalized_surplusCost;//0-1
    private double normalized_fairnessCost;//0-1
    private double normalized_totalPenaltyCost; //high cost = negative! 0-4
    private double normalized_realCosts;//0-1
    private double normalized_allCosts;//0-5
    
    private double weighted_totalPenaltyCost;
    private double weighted_allCosts;

    public NurseSchedule(Scenario scenario, char department, String name, int[] minCovReq, ArrayList<Nurse> nurses, int[][] schedule) {
        this.scenario = scenario;
        this.department = department;
        this.name = name;
        this.shifts = scenario.getShifts();
        this.setOfNursesUsed = nurses;
        this.hourSystemCost = calculateHourSystemCost(scenario.HOUR_SYSTEM);
        this.schedule = schedule;
        this.preferenceCost = calculatePreferenceCost();
        calculateCovReqCost(minCovReq, schedule); //covReqCost AND surplusCost
        this.fairnessCost = calculateFairnessCost();
        this.totalPenaltyCost = preferenceCost + covReqCost + surplusCost + fairnessCost;
        calculateWagesCost();
        this.realCosts = round(hourSystemCost + wagesCost, 3);
        this.allCosts = round(totalPenaltyCost + realCosts, 3);
    }

    public NurseSchedule(NurseSchedule ns) {
        this.scenario = ns.scenario;
        this.department = ns.department;
        this.name = ns.name;
        this.shifts = ns.shifts.clone();
        this.hourSystemCost = ns.hourSystemCost;

        this.schedule = new int[ns.schedule.length][ns.schedule[0].length];
        for (int i = 0; i < ns.schedule.length; i++) {
            for (int j = 0; j < ns.schedule[0].length; j++) {
                this.schedule[i][j] = ns.schedule[i][j];
            }
        }

        this.setOfNursesUsed = new ArrayList<>();
        for (Nurse n : ns.getNurses()) {
            this.setOfNursesUsed.add(new Nurse(n)); //NIEUW: OPROEP COPY CONSTRUCTOR VAN KLASSE NURSE
        }

        this.wagesCost = ns.wagesCost;
        this.preferenceCost = ns.preferenceCost;
        this.covReqCost = ns.covReqCost;
        this.surplusCost = ns.surplusCost;
        this.fairnessCost = ns.fairnessCost;
        this.totalPenaltyCost = ns.totalPenaltyCost;
        this.realCosts = ns.realCosts;
        this.allCosts = ns.allCosts;

        this.normalized_preferenceCost = ns.normalized_preferenceCost;
        this.normalized_covReqCost = ns.normalized_covReqCost;
        this.normalized_surplusCost = ns.normalized_surplusCost;
        this.normalized_fairnessCost = ns.normalized_fairnessCost;
        this.normalized_totalPenaltyCost = ns.normalized_totalPenaltyCost;
        this.normalized_realCosts = ns.normalized_realCosts;
        this.normalized_allCosts = ns.normalized_allCosts;
        
        this.weighted_totalPenaltyCost = ns.weighted_totalPenaltyCost;
        this.weighted_allCosts = ns.weighted_allCosts;
    }

    private int calculateHourSystemCost(int hourSystem) {
        int nbrShifts = 0;
        for (Nurse nurse : setOfNursesUsed) {
            for (int i = 0; i < nurse.getPersonalSchedule().length; i++) {
                if (nurse.getPersonalSchedule()[i] != 0) { //then she need to work that day
                    nbrShifts++;
                }
            }
        }
        int cost = (60000/48 * (shifts.length - 1)) + ((hourSystem - 1) * nbrShifts); //devide with 4 because 60000 is for all departments and devide by 12 (month instead of year)
        return cost;
    }

    private int calculatePreferenceCost() {
        int penalty = 0;
        for (Nurse nurse : setOfNursesUsed) {
            penalty += nurse.getPreferenceCost();
        }
//        if (penalty == 0) {
//            System.out.format("Preference cost for schedule is zero\n\n");
//        }
        return penalty;
    }

    public void calculateCovReqCost(int[] minCovReq, int[][] schedule) {
        int[][] nbrShiftsCovered = new int[NurseScheduling.NBR_DAYS_IN_MONTH][minCovReq.length];

        for (int j = 0; j < NurseScheduling.NBR_DAYS_IN_MONTH; j++) { //for first day
            for (int i = 0; i < setOfNursesUsed.size(); i++) { //for all nurses
                for (int k = 1; k < shifts.length; k++) { // k=1 means earlyShift
                    if (schedule[i][j] == shifts[k]) { //ex. early
                        nbrShiftsCovered[j][k - 1]++; //start at index 0 => nbrShiftsCovered[0]
                    }
                }
            }
            for (int m = 0; m < (shifts.length - 1); m++) {
                if ((nbrShiftsCovered[j][m] - minCovReq[m]) > 0) { //surplus
                    surplusCost += ((nbrShiftsCovered[j][m] - minCovReq[m]) * COST_SURPLUS_NURSE);
                } else {
                    covReqCost += ((minCovReq[m] - nbrShiftsCovered[j][m]) * COST_UNSATISFIED_COV_REQ);
                }
            }
        }
    }

    private int calculateFairnessCost() {
        int penalty = 0;
        int assignmentsAvailable = 0;
        int assignmentsAssigned = 0;

        for (int i = 0; i < setOfNursesUsed.size(); i++) {
            assignmentsAvailable = setOfNursesUsed.get(i).getAssignments();
            assignmentsAssigned = 0;
            for (int j = 0; j < NurseScheduling.NBR_DAYS_IN_MONTH; j++) {
                if (schedule[i][j] != 0) { //it means the nurse have to work that day
                    assignmentsAssigned++;
                }
            }
            penalty += Math.abs(assignmentsAssigned - assignmentsAvailable) * COST_FAIRNESS; //NEEM ABSOLUTE WAARDE! 
        }
        return penalty;
    }

    private void calculateWagesCost() {
        for (Nurse nurse : setOfNursesUsed) {
            wagesCost += nurse.getWagesCost();
        }
    }

    private int[] searchMinMax(int[] a, int[] b, int[] c) {
        if (c != null) {
            return new int[]{Math.min(Math.min(a[MIN], b[MIN]), c[MIN]), Math.max(Math.max(a[MAX], b[MAX]), c[MAX])};
        } else {
            return new int[]{Math.min(a[MIN], b[MIN]), Math.max(a[MAX], b[MAX])};
        }
    }

    private double[] searchMinMax(double[] a, double[] b, double[] c) {
        if (c != null) {
            return new double[]{Math.min(Math.min(a[MIN], b[MIN]), c[MIN]), Math.max(Math.max(a[MAX], b[MAX]), c[MAX])};
        } else {
            return new double[]{Math.min(a[MIN], b[MIN]), Math.max(a[MAX], b[MAX])};
        }
    }

    private double calculateNormalizedPreferenceCost() {
        int[] costs = searchMinMax(Program.PREF_COST_DIFF_SHIFTS, Program.PREF_COST_SAME_SHIFTS, Program.PREF_COST_CASE_D);
        if (costs[MIN] == 0 && costs[MAX] == 0) {
            return 0;
        } else {
            return (double) (preferenceCost - costs[MIN]) / (costs[MAX] - costs[MIN]);
        }
    }

    private double calculateNormalizedCovReqCost() {
        int[] costs = searchMinMax(Program.COV_REQ_COST_DIFF_SHIFTS, Program.COV_REQ_COST_SAME_SHIFTS, Program.COV_REQ_COST_CASE_D);
        if (costs[MIN] == 0 && costs[MAX] == 0) {
            return 0;
        } else {
            return (double) (covReqCost - costs[MIN]) / (costs[MAX] - costs[MIN]);
        }
    }

    private double calculateNormalizedSurplusCost() {
        int[] costs = searchMinMax(Program.SURPLUS_COST_DIFF_SHIFTS, Program.SURPLUS_COST_SAME_SHIFTS, Program.SURPLUS_COST_CASE_D);
        if (costs[MIN] == 0 && costs[MAX] == 0) {
            return 0;
        } else {
            return (double) (surplusCost - costs[MIN]) / (costs[MAX] - costs[MIN]);
        }
    }

    private double calculateNormalizedFairnessCost() {
        int[] costs = searchMinMax(Program.FAIRNESS_COST_DIFF_SHIFTS, Program.FAIRNESS_COST_SAME_SHIFTS, Program.FAIRNESS_COST_CASE_D);
        if (costs[MIN] == 0 && costs[MAX] == 0) {
            return 0;
        } else {
            return (double) (fairnessCost - costs[MIN]) / (costs[MAX] - costs[MIN]);
        }
    }

    private double calculateNormalizedRealCost() {
        double[] costs = searchMinMax(Program.REAL_COST_DIFF_SHIFTS, Program.REAL_COST_SAME_SHIFTS, Program.REAL_COST_CASE_D);
        if (costs[MIN] == 0 && costs[MAX] == 0) {
            return 0;
        } else {
            return (double) (realCosts - costs[MIN]) / (costs[MAX] - costs[MIN]);
        }
    }

    private double round(double value, int digits) {
        double factor = Math.pow(10, digits);
        return (int) (value * factor) / (double) factor;
    }

    public void calculateNormalizedCosts() { // (x - min)/(max-min)
        int digits = 3;
        normalized_preferenceCost = round(calculateNormalizedPreferenceCost(), digits);
        normalized_covReqCost = round(calculateNormalizedCovReqCost(), digits);
        normalized_surplusCost = round(calculateNormalizedSurplusCost(), digits);
        normalized_fairnessCost = round(calculateNormalizedFairnessCost(), digits);
        normalized_realCosts = round(calculateNormalizedRealCost(), digits);

        normalized_totalPenaltyCost = round(normalized_preferenceCost + normalized_covReqCost + normalized_surplusCost + normalized_fairnessCost, digits);
        weighted_totalPenaltyCost = round(0.1*normalized_preferenceCost + 0.35*normalized_covReqCost + 0.05*normalized_surplusCost + 0.15*normalized_fairnessCost, digits);
        normalized_allCosts = round(normalized_totalPenaltyCost + normalized_realCosts, digits);
        weighted_allCosts = round(weighted_totalPenaltyCost + 0.35*normalized_realCosts, digits);
    }

    public void checkAllocationNurses() {
        int sum;
        Nurse nurse;
        for (int i = 0; i < setOfNursesUsed.size(); i++) {
            sum = 0;
            nurse = setOfNursesUsed.get(i);
            for (int shift : nurse.getPersonalSchedule()) {
                sum += shift;
            }
            if (sum == 0) {
                setOfNursesUsed.remove(nurse);
                schedule = deleteRow(schedule, i);
            }
        }
    }

    private int[][] deleteRow(int[][] schedule, int i) {
        int[][] newSchedule = new int[schedule.length - 1][schedule[0].length];
        int idx = 0;
        while (idx != i) {
            newSchedule[idx] = schedule[idx];
            idx++;
        }
        while (idx < newSchedule.length) {
            newSchedule[idx] = schedule[idx + 1];
            idx++;
        }
        return newSchedule;
    }

    public int getPreferenceCost() {
        return preferenceCost;
    }

    public int getCovReqCost() {
        return covReqCost;
    }

    public int getSurplusCost() {
        return surplusCost;
    }

    public int getFairnessCost() {
        return fairnessCost;
    }

    public int getTotalPenaltyCost() {
        return totalPenaltyCost;
    }

    public double getRealCosts() {
        return realCosts;
    }

    public double getAllCosts() {
        return allCosts;
    }

    public ArrayList<Nurse> getNurses() {
        return setOfNursesUsed;
    }

    public double getNormalized_preferenceCost() {
        return normalized_preferenceCost;
    }

    public double getNormalized_covReqCost() {
        return normalized_covReqCost;
    }

    public double getNormalized_surplusCost() {
        return normalized_surplusCost;
    }

    public double getNormalized_fairnessCost() {
        return normalized_fairnessCost;
    }

    public double getNormalized_totalPenaltyCost() {
        return normalized_totalPenaltyCost;
    }

    public double getNormalized_realCosts() {
        return normalized_realCosts;
    }

    public double getNormalized_allCosts() {
        return normalized_allCosts;
    }

    public double getWeighted_allCosts() {
        return weighted_allCosts;
    }
    
    

    public Scenario getScenario() {
        return scenario;
    }

    public char getDepartment() {
        return department;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int[][] getSchedule() {
        return schedule;
    }

    @Override
    public int compareTo(NurseSchedule other) {
        return this.totalPenaltyCost - other.totalPenaltyCost;
    }

//    @Override
//    public String toString() {
//        return "Total penalty: " + this.totalPenaltyCost
//                + "\t preferenceCost = " + preferenceCost
//                + "\t covReqCost = " + covReqCost
//                + "\t surplusCost = " + surplusCost
//                + "\t fairnessCost = " + fairnessCost;
//    }
    @Override
    public String toString() {
        String s = "\nTotal penalty;" + totalPenaltyCost
                + "\npreferenceCost;" + preferenceCost
                + "\ncovReqCost;" + covReqCost
                + "\nsurplusCost;" + surplusCost
                + "\nfairnessCost;" + fairnessCost
                + "\nrealCost;" + realCosts
                + "\nnormalized_preferenceCost;" + normalized_preferenceCost
                + "\nnormalized_covReqCost;" + normalized_covReqCost
                + "\nnormalized_surplusCost;" + normalized_surplusCost
                + "\nnormalized_fairnessCost;" + normalized_fairnessCost
                + "\nnormalized_totalPenaltyCost;" + normalized_totalPenaltyCost
                + "\nnormalized_realCosts;" + normalized_realCosts
                + "\nnormalized_allCosts;" + normalized_allCosts
                + "\nweighted_totalPenaltyCost;" + weighted_totalPenaltyCost
                + "\nweighted_allCosts;" + weighted_allCosts
                
                + "\n\n NURSES\n";

        Collections.sort(setOfNursesUsed);
        for (Nurse nurse : setOfNursesUsed) {
            s += "\n" + nurse.getId() + ";";
            for (int elt : nurse.getPersonalSchedule()) {
                s += elt + ";";
            }
        }
        return s;
    }

}
