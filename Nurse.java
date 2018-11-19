package nurse.scheduling;

/**
 *
 * @author Sandra
 */
public class Nurse implements Comparable<Nurse> {

    public static final int NBR_ASSIGMENTS_FTE = 20; //20 assigments = 1 FTE (100%); 10 assigments = 0.5 FTE (50% = part-time)
    public static final int PREFERENCE_SCORE_NEWNURSE = 5; //assumption when hiring a new nurse
    public static final double EMPLOYMENT_RATE_NEWNURSE = 1.00;

    private final String id; //nurseID
    private final int[][] preferenceData; //preferencePenalties PER DAY PER SHIFT
    private final double employmentRate;
    private final int assignments; //per month
    private final NurseType nurseType;
    private int[] personalSchedule; //info about which day which shift s/he need to work
    private int preferenceCost;
    private double wagesCost;

    public Nurse(String id, int[][] preferenceData, double rate, NurseType type) {
        this.id = id;
        this.preferenceData = preferenceData;
        this.employmentRate = rate;
        this.assignments = (int) (rate * NBR_ASSIGMENTS_FTE);
        this.nurseType = type;
        this.personalSchedule = new int[NurseScheduling.NBR_DAYS_IN_MONTH];
    }

//    public Nurse(String id, double employmentRate, NurseType nurseType) {
//        this.id = id;
//        this.employmentRate = employmentRate;
//        this.nurseType = nurseType;
//        this.preferenceData = new int[NurseScheduling.NBR_DAYS_IN_MONTH][NurseScheduling.NBR_PREFERENCE_SHIFTS]; //default
//
//        for (int i = 0; i < NurseScheduling.NBR_DAYS_IN_MONTH; i++) { //every day every shift get preference score = 5
//            for (int j = 0; j < NurseScheduling.NBR_PREFERENCE_SHIFTS; j++) {
//                preferenceData[i][j] = PREFERENCE_SCORE_NEWNURSE;
//            }
//        }
//        this.assignments = (int) (employmentRate * NBR_ASSIGMENTS_FTE);
//        this.personalSchedule = new int[NurseScheduling.NBR_DAYS_IN_MONTH];
//    }
    public Nurse(String id, NurseType type) {
        this.id = id;
        this.preferenceData = new int[NurseScheduling.NBR_DAYS_IN_MONTH][NurseScheduling.NBR_PREFERENCE_SHIFTS]; //default

        for (int i = 0; i < NurseScheduling.NBR_DAYS_IN_MONTH; i++) { //every day every shift get preference score = 5
            for (int j = 0; j < NurseScheduling.NBR_PREFERENCE_SHIFTS; j++) {
                preferenceData[i][j] = PREFERENCE_SCORE_NEWNURSE;
            }
        }

        this.employmentRate = EMPLOYMENT_RATE_NEWNURSE;
        this.assignments = (int) (EMPLOYMENT_RATE_NEWNURSE * NBR_ASSIGMENTS_FTE);
        this.nurseType = type;
        this.personalSchedule = new int[NurseScheduling.NBR_DAYS_IN_MONTH];
    }

    public Nurse(Nurse n) {
        this.id = n.id;
        this.employmentRate = n.employmentRate;
        this.assignments = n.assignments;
        this.nurseType = n.nurseType;
        this.preferenceCost = n.preferenceCost;
        this.wagesCost = n.wagesCost;

        this.preferenceData = new int[n.preferenceData.length][n.preferenceData[0].length];
        for (int i = 0; i < preferenceData.length; i++) {
            for (int j = 0; j < preferenceData[0].length; j++) {
                preferenceData[i][j] = n.preferenceData[i][j];
            }
        }

        this.personalSchedule = new int[n.personalSchedule.length];
        for (int i = 0; i < personalSchedule.length; i++) {
            personalSchedule[i] = n.personalSchedule[i];
        }
    }

    public String getId() {
        return id;
    }

    public int[][] getPenaltyPreferenceData() {
        return preferenceData;
    }

    public double getEmploymentRate() {
        return employmentRate;
    }

    public void setPersonalSchedule(int[] personalSchedule) {
        this.personalSchedule = personalSchedule;
        this.preferenceCost = calculatePreferenceCost();
        this.wagesCost = calculateWagesCost();
    }

    public int[] getPersonalSchedule() {
        return personalSchedule;
    }

    private int calculatePreferenceCost() {
        int cost = 0;
        int shift = 0;

        for (int i = 0; i < NurseScheduling.NBR_DAYS_IN_MONTH; i++) { //which day
            if (personalSchedule[i] != 0) { //Does s/he have to work
                shift = personalSchedule[i]; //which shift
                cost += preferenceData[i][indexOf(shift)];
            }
        }
//        if (cost == 0) {
//            System.out.format("Preference cost for nurse id%s is %d \n", this.id, cost);
//        }

        return cost;
    }

    private double calculateWagesCost() {
        double wagesCost = 0;

        for (int i = 0; i < personalSchedule.length; i++) {
            if (nurseType == NurseType.Type1) {
                if (i == 5 || i == 6 || i == 12 || i == 13 || i == 19 || i == 20 || i == 26 || i == 27) { //weekend
                    if (personalSchedule[i] == 1) { //early
                        wagesCost += 216;
                    } else if (personalSchedule[i] == 2) { //day
                        wagesCost += 216;
                    } else if (personalSchedule[i] == 3) { //late
                        wagesCost += 237.6;
                    } else if (personalSchedule[i] == 4) { //night
                        wagesCost += 291.6;
                    }
                } else { //week
                    if (personalSchedule[i] == 1) { //early
                        wagesCost += 160;
                    } else if (personalSchedule[i] == 2) { //day
                        wagesCost += 160;
                    } else if (personalSchedule[i] == 3) { //late
                        wagesCost += 176;
                    } else if (personalSchedule[i] == 4) { //night
                        wagesCost += 216;
                    }
                }
            } else { //type 2 nurse
                if (i == 5 || i == 6 || i == 12 || i == 13 || i == 19 || i == 20 || i == 26 || i == 27) { //weekend
                    if (personalSchedule[i] == 1) { //early
                        wagesCost += 162;
                    } else if (personalSchedule[i] == 2) { //day
                        wagesCost += 162;
                    } else if (personalSchedule[i] == 3) { //late
                        wagesCost += 178.2;
                    } else if (personalSchedule[i] == 4) { //night
                        wagesCost += 218.7;
                    }
                } else { //week
                    if (personalSchedule[i] == 1) { //early
                        wagesCost += 120;
                    } else if (personalSchedule[i] == 2) { //day
                        wagesCost += 120;
                    } else if (personalSchedule[i] == 3) { //late
                        wagesCost += 132;
                    } else if (personalSchedule[i] == 4) { //night
                        wagesCost += 162;
                    }
                }
            }

        }
        return wagesCost;
    }

    private int indexOf(int shift) {
        return (shift + 4) % 5; //early(1) = 0; day(2) = 1; late(3) = 2; night(4) = 3; free(0) = 4
    }

    public int getPreferenceCost() {
        return preferenceCost;
    }

    public NurseType getNurseType() {
        return nurseType;
    }

    public int getAssignments() {
        return assignments;
    }

    public double getWagesCost() {
        return wagesCost;
    }

    public int[][] getPreferenceData() {
        return preferenceData;
    }

    public String toStringPreferenceData() {
        String s = id + ": ";

        for (int[] row : preferenceData) {
            for (int elt : row) {
                s += elt + "\t";
            }
        }
        s += employmentRate + "\t" + nurseType + "\n";
        return s;
    }

    @Override
    public String toString() {
        String s = id + ": ";

        for (int elt : personalSchedule) {
            s += elt + "\t";
        }
        s += employmentRate + "\t" + nurseType + "\n";
        return s;
    }

    @Override
    public int compareTo(Nurse nurse) {
        return this.id.compareTo(nurse.id);
    }

}
