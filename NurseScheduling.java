package nurse.scheduling;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sandra
 */
public class NurseScheduling {

    public static final int MIN = 0;
    public static final int MAX = 1;

    public static final int NBR_DAYS_IN_MONTH = 28;
    public static final int NBR_DAYS_IN_WEEK = 7;
    public static final int NBR_DEP = 4;
    public static final int NBR_PREFERENCE_SHIFTS = 5; //needed when creating a new nurse her preferences
    public static final int NBR_RANDOMRUNS = 20000; //for finding a start schedule (see method)
    public static final int NBR_SCHEDULES_SAVED = 5; //how many start schedules will we save

    private static Scenario scenario;
    private static char department;
    private int[] shifts; //ex. 0 1 3 4 (no shift 2)
    private int[] minCovReq;
    private CyclicRoster cyclicRoster;
    private PersonnelCharacteristics personnelCharacteristics;
    private int[][] patterns;

    //BEST SCHEDULES BASED ON NORMALIZED COSTS
    private NurseSchedule bestSchedule_BasedOnPreference_caseD; //based on cyclic roster (case D)
    private NurseSchedule bestSchedule_BasedOnMinCovReq_caseD;
    private NurseSchedule bestSchedule_BasedOnSurplusNurses_caseD;
    private NurseSchedule bestSchedule_BasedOnFairness_caseD;
    private NurseSchedule bestSchedule_BasedOnRealCosts_caseD;
    private NurseSchedule bestSchedule_BasedOnAllCosts_caseD;

    private NurseSchedule bestSchedule_BasedOnPreference_differentShifts;
    private NurseSchedule bestSchedule_BasedOnMinCovReq_differentShifts;
    private NurseSchedule bestSchedule_BasedOnSurplusNurses_differentShifts;
    private NurseSchedule bestSchedule_BasedOnFairness_differentShifts;
    private NurseSchedule bestSchedule_BasedOnRealCosts_differentShifts;
    private NurseSchedule bestSchedule_BasedOnAllCosts_differentShifts;

    private NurseSchedule bestSchedule_BasedOnPreference_sameShifts;
    private NurseSchedule bestSchedule_BasedOnMinCovReq_sameShifts;
    private NurseSchedule bestSchedule_BasedOnSurplusNurses_sameShifts;
    private NurseSchedule bestSchedule_BasedOnFairness_sameShifts;
    private NurseSchedule bestSchedule_BasedOnRealCosts_sameShifts;
    private NurseSchedule bestSchedule_BasedOnAllCosts_sameShifts;

    public NurseScheduling(Scenario scenario, char department, String cyclicRosterFile, String pattern, String personnelFile, boolean setMinAndMax) {
        this.scenario = scenario;
        this.department = department;
        this.shifts = scenario.getShifts();
        try {
            //shiftSystem = IO.readShiftSystem(shiftSystemFile);
            personnelCharacteristics = IO.readPersonnelCharacteristics(personnelFile);
            cyclicRoster = IO.readCyclicRoster(cyclicRosterFile);
            patterns = IO.readPattern(pattern);
            //finalNurseSchedule = new NurseSchedule(scenario, department);
            calculateMinCovReq(scenario, department);

            if (setMinAndMax) {
                calculateMinAndMax_BasedOnCaseD();
                calculateMinimaAndMaxima();
            } else {
                calculateBestSchedule_BasedOnCaseD(); //cyclic roster (case D)
                calculateBestSchedules_BasedOnCriteria();

//                System.out.println("OVERVIEW TOTAL COSTS");
//                System.out.println("\nTOTAL COST based on bestSchedule_BasedOnPreference (case D): " + bestSchedule_BasedOnPreference_caseD);
//                System.out.println("\nTOTAL COST based on bestSchedule_BasedOnPreference2_differentShifts: " + bestSchedule_BasedOnPreference_differentShifts);
//                System.out.println("\nTOTAL COST based on bestSchedule_BasedOnMinCovReq_differentShifts: " + bestSchedule_BasedOnMinCovReq_differentShifts);
//                System.out.println("\nTOTAL COST based on bestSchedule_BasedOnSurplusNurses_differentShifts: " + bestSchedule_BasedOnSurplusNurses_differentShifts);
//                System.out.println("\nTOTAL COST based on bestSchedule_BasedOnFairness_differentShifts: " + bestSchedule_BasedOnFairness_differentShifts);
//                
                writeOutput();
            }

        } catch (IOException ex) {
            Logger.getLogger(NurseScheduling.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void writeOutputAllCosts(NurseSchedule schedule) throws IOException {
        String folder = "./output_files/" + schedule.getScenario().getScenarioName() + "/BestSchedules_BasedOnAllCosts/";
        IO.writeSchedule(folder, schedule);
    }

    private void writeOutput() throws IOException {
        IO.writeSchedule(bestSchedule_BasedOnPreference_caseD);
        IO.writeSchedule(bestSchedule_BasedOnMinCovReq_caseD);
        IO.writeSchedule(bestSchedule_BasedOnSurplusNurses_caseD);
        IO.writeSchedule(bestSchedule_BasedOnFairness_caseD);
        IO.writeSchedule(bestSchedule_BasedOnRealCosts_caseD);
        IO.writeSchedule(bestSchedule_BasedOnAllCosts_caseD);

        IO.writeSchedule(bestSchedule_BasedOnPreference_differentShifts);
        IO.writeSchedule(bestSchedule_BasedOnMinCovReq_differentShifts);
        IO.writeSchedule(bestSchedule_BasedOnSurplusNurses_differentShifts);
        IO.writeSchedule(bestSchedule_BasedOnFairness_differentShifts);
        IO.writeSchedule(bestSchedule_BasedOnRealCosts_differentShifts);
        IO.writeSchedule(bestSchedule_BasedOnAllCosts_differentShifts);

        IO.writeSchedule(bestSchedule_BasedOnPreference_sameShifts);
        IO.writeSchedule(bestSchedule_BasedOnMinCovReq_sameShifts);
        IO.writeSchedule(bestSchedule_BasedOnSurplusNurses_sameShifts);
        IO.writeSchedule(bestSchedule_BasedOnFairness_sameShifts);
        IO.writeSchedule(bestSchedule_BasedOnRealCosts_sameShifts);
        IO.writeSchedule(bestSchedule_BasedOnAllCosts_sameShifts);
    }

    public NurseSchedule[] getBestSchedules_BasedOnAllCosts() {
        return new NurseSchedule[]{bestSchedule_BasedOnAllCosts_caseD,
            bestSchedule_BasedOnAllCosts_differentShifts,
            bestSchedule_BasedOnAllCosts_sameShifts};
    }

    private void calculateMinAndMax_BasedOnCaseD() {
        NurseSchedule randomSchedule;

        for (int i = 0; i < NBR_RANDOMRUNS; i++) {
            randomSchedule = makeRandomSchedule_BasedOnCyclicRoster();
            randomSchedule.checkAllocationNurses();

            adaptMinMax(randomSchedule.getPreferenceCost(), Program.PREF_COST_CASE_D);
            adaptMinMax(randomSchedule.getCovReqCost(), Program.COV_REQ_COST_CASE_D);
            adaptMinMax(randomSchedule.getSurplusCost(), Program.SURPLUS_COST_CASE_D);
            adaptMinMax(randomSchedule.getFairnessCost(), Program.FAIRNESS_COST_CASE_D);
            adaptMinMax(randomSchedule.getRealCosts(), Program.REAL_COST_CASE_D);
        }

        printMinimaAndMaxima_CaseD();
    }

    private void adaptMinMax(int cost, int[] costs) {
        if (cost < costs[Program.MIN]) {
            costs[Program.MIN] = cost;
        } else if (cost > costs[Program.MAX]) {
            costs[Program.MAX] = cost;
        }
    }

    private void adaptMinMax(double cost, double[] costs) {
        if (cost < costs[Program.MIN]) {
            costs[Program.MIN] = cost;
        } else if (cost > costs[Program.MAX]) {
            costs[Program.MAX] = cost;
        }
    }

    private void calculateMinimaAndMaxima() {
        NurseSchedule randomSchedule;

        for (int i = 0; i < NBR_RANDOMRUNS; i++) {
            //MIN AND MAX based on different shifts
            randomSchedule = makeRandomSchedule_differentShifts();
            randomSchedule.checkAllocationNurses();
            adaptMinMax(randomSchedule.getPreferenceCost(), Program.PREF_COST_DIFF_SHIFTS);
            adaptMinMax(randomSchedule.getCovReqCost(), Program.COV_REQ_COST_DIFF_SHIFTS);
            adaptMinMax(randomSchedule.getSurplusCost(), Program.SURPLUS_COST_DIFF_SHIFTS);
            adaptMinMax(randomSchedule.getFairnessCost(), Program.FAIRNESS_COST_DIFF_SHIFTS);
            adaptMinMax(randomSchedule.getRealCosts(), Program.REAL_COST_DIFF_SHIFTS);

            //MIN AND MAX based on same shifts
            randomSchedule = makeRandomSchedule_sameShifts();
            randomSchedule.checkAllocationNurses();

            adaptMinMax(randomSchedule.getPreferenceCost(), Program.PREF_COST_SAME_SHIFTS);
            adaptMinMax(randomSchedule.getCovReqCost(), Program.COV_REQ_COST_SAME_SHIFTS);
            adaptMinMax(randomSchedule.getSurplusCost(), Program.SURPLUS_COST_SAME_SHIFTS);
            adaptMinMax(randomSchedule.getFairnessCost(), Program.FAIRNESS_COST_SAME_SHIFTS);
            adaptMinMax(randomSchedule.getRealCosts(), Program.REAL_COST_SAME_SHIFTS);
        }

        printMinimaAndMaxima_DiffShifts();
        printMinimaAndMaxima_SameShifts();
    }

    private void printMinimaAndMaxima_CaseD() {
        System.out.format("PREFERENCE COST CASE D: MIN = %d, MAX = %d\n", Program.PREF_COST_CASE_D[MIN], Program.PREF_COST_CASE_D[MAX]);
        System.out.format("COVREQ COST CASE D: MIN = %d, MAX = %d\n", Program.COV_REQ_COST_CASE_D[MIN], Program.COV_REQ_COST_CASE_D[MAX]);
        System.out.format("SURPLUS COST CASE D: MIN = %d, MAX = %d\n", Program.SURPLUS_COST_CASE_D[MIN], Program.SURPLUS_COST_CASE_D[MAX]);
        System.out.format("FAIRNESS COST CASE D: MIN = %d, MAX = %d\n", Program.FAIRNESS_COST_CASE_D[MIN], Program.FAIRNESS_COST_CASE_D[MAX]);
        System.out.format("REAL COST CASE D: MIN = %.2f, MAX = %.2f\n", Program.REAL_COST_CASE_D[MIN], Program.REAL_COST_CASE_D[MAX]);
        System.out.println();
    }

    private void printMinimaAndMaxima_DiffShifts() {
        System.out.format("PREFERENCE COST DIFF SHIFTS: MIN = %d, MAX = %d\n", Program.PREF_COST_DIFF_SHIFTS[MIN], Program.PREF_COST_DIFF_SHIFTS[MAX]);
        System.out.format("COVREQ COST DIFF SHIFTS: MIN = %d, MAX = %d\n", Program.COV_REQ_COST_DIFF_SHIFTS[MIN], Program.COV_REQ_COST_DIFF_SHIFTS[MAX]);
        System.out.format("SURPLUS COST DIFF SHIFTS: MIN = %d, MAX = %d\n", Program.SURPLUS_COST_DIFF_SHIFTS[MIN], Program.SURPLUS_COST_DIFF_SHIFTS[MAX]);
        System.out.format("FAIRNESS COST DIFF SHIFTS: MIN = %d, MAX = %d\n", Program.FAIRNESS_COST_DIFF_SHIFTS[MIN], Program.FAIRNESS_COST_DIFF_SHIFTS[MAX]);
        System.out.format("REAL COST DIFF SHIFTS: MIN = %.2f, MAX = %.2f\n", Program.REAL_COST_DIFF_SHIFTS[MIN], Program.REAL_COST_DIFF_SHIFTS[MAX]);
        System.out.println();
    }

    private void printMinimaAndMaxima_SameShifts() {
        System.out.format("PREFERENCE COST SAME SHIFTS: MIN = %d, MAX = %d\n", Program.PREF_COST_SAME_SHIFTS[MIN], Program.PREF_COST_SAME_SHIFTS[MAX]);
        System.out.format("COVREQ COST SAME SHIFTS: MIN = %d, MAX = %d\n", Program.COV_REQ_COST_SAME_SHIFTS[MIN], Program.COV_REQ_COST_SAME_SHIFTS[MAX]);
        System.out.format("SURPLUS COST SAME SHIFTS: MIN = %d, MAX = %d\n", Program.SURPLUS_COST_SAME_SHIFTS[MIN], Program.SURPLUS_COST_SAME_SHIFTS[MAX]);
        System.out.format("FAIRNESS COST SAME SHIFTS: MIN = %d, MAX = %d\n", Program.FAIRNESS_COST_SAME_SHIFTS[MIN], Program.FAIRNESS_COST_SAME_SHIFTS[MAX]);
        System.out.format("REAL COST SAME SHIFTS: MIN = %.2f, MAX = %.2f\n", Program.REAL_COST_SAME_SHIFTS[MIN], Program.REAL_COST_SAME_SHIFTS[MAX]);
        System.out.println();
    }

    private void calculateBestSchedule_BasedOnCaseD() {
        NurseSchedule randomSchedule;

        bestSchedule_BasedOnPreference_caseD = makeRandomSchedule_BasedOnCyclicRoster();
        bestSchedule_BasedOnMinCovReq_caseD = makeRandomSchedule_BasedOnCyclicRoster();
        bestSchedule_BasedOnSurplusNurses_caseD = makeRandomSchedule_BasedOnCyclicRoster();
        bestSchedule_BasedOnFairness_caseD = makeRandomSchedule_BasedOnCyclicRoster();
        bestSchedule_BasedOnRealCosts_caseD = makeRandomSchedule_BasedOnCyclicRoster();
        bestSchedule_BasedOnAllCosts_caseD = makeRandomSchedule_BasedOnCyclicRoster();

        bestSchedule_BasedOnPreference_caseD.checkAllocationNurses();
        bestSchedule_BasedOnMinCovReq_caseD.checkAllocationNurses();
        bestSchedule_BasedOnSurplusNurses_caseD.checkAllocationNurses();
        bestSchedule_BasedOnFairness_caseD.checkAllocationNurses();
        bestSchedule_BasedOnRealCosts_caseD.checkAllocationNurses();
        bestSchedule_BasedOnAllCosts_caseD.checkAllocationNurses();

        bestSchedule_BasedOnPreference_caseD.calculateNormalizedCosts();
        bestSchedule_BasedOnMinCovReq_caseD.calculateNormalizedCosts();
        bestSchedule_BasedOnSurplusNurses_caseD.calculateNormalizedCosts();
        bestSchedule_BasedOnFairness_caseD.calculateNormalizedCosts();
        bestSchedule_BasedOnRealCosts_caseD.calculateNormalizedCosts();
        bestSchedule_BasedOnAllCosts_caseD.calculateNormalizedCosts();

        for (int i = 0; i < NBR_RANDOMRUNS; i++) {
            randomSchedule = makeRandomSchedule_BasedOnCyclicRoster();
            randomSchedule.checkAllocationNurses();
            randomSchedule.calculateNormalizedCosts();
            if (randomSchedule.getNormalized_preferenceCost() < bestSchedule_BasedOnPreference_caseD.getNormalized_preferenceCost()) {
                bestSchedule_BasedOnPreference_caseD = new NurseSchedule(randomSchedule);
            }
            if (randomSchedule.getNormalized_covReqCost() < bestSchedule_BasedOnMinCovReq_caseD.getNormalized_covReqCost()) {
                bestSchedule_BasedOnMinCovReq_caseD = new NurseSchedule(randomSchedule);
            }
            if (randomSchedule.getNormalized_surplusCost() < bestSchedule_BasedOnSurplusNurses_caseD.getNormalized_surplusCost()) {
                bestSchedule_BasedOnSurplusNurses_caseD = new NurseSchedule(randomSchedule);
            }
            if (randomSchedule.getNormalized_fairnessCost() < bestSchedule_BasedOnFairness_caseD.getNormalized_fairnessCost()) {
                bestSchedule_BasedOnFairness_caseD = new NurseSchedule(randomSchedule);
            }
            if (randomSchedule.getNormalized_realCosts() < bestSchedule_BasedOnRealCosts_caseD.getNormalized_realCosts()) {
                bestSchedule_BasedOnRealCosts_caseD = new NurseSchedule(randomSchedule);
            }
            if (randomSchedule.getWeighted_allCosts() == 0.0) {
                System.out.println("NOW!");
            }
            if (randomSchedule.getWeighted_allCosts() < bestSchedule_BasedOnAllCosts_caseD.getWeighted_allCosts()) {
                bestSchedule_BasedOnAllCosts_caseD = new NurseSchedule(randomSchedule);
            }
        }

        bestSchedule_BasedOnPreference_caseD.setName("bestSchedule_basedOnPreference_caseD");
        bestSchedule_BasedOnMinCovReq_caseD.setName("bestSchedule_basedOnMinCovReq_caseD");
        bestSchedule_BasedOnSurplusNurses_caseD.setName("bestSchedule_basedOnSurplusNurses_caseD");
        bestSchedule_BasedOnFairness_caseD.setName("bestSchedule_basedOnFairness_caseD");
        bestSchedule_BasedOnRealCosts_caseD.setName("bestSchedule_basedOnRealCosts_caseD");
        bestSchedule_BasedOnAllCosts_caseD.setName("bestSchedule_basedOnAllCosts_caseD");
    }

    private NurseSchedule makeRandomSchedule_BasedOnCyclicRoster() {
        int[][] schedule = null;
        int nbrNursesType1Needed = cyclicRoster.getNbrType1(); //nbrType1 FTE nurses needed
        int nbrNursesType2Needed = cyclicRoster.getNbrType2(); //nbrType2 FTE nurses needed
        int nbrNursesNeeded = nbrNursesType1Needed + nbrNursesType2Needed; //FTE nbr nurses needed

        //ArrayList<Nurse> nursesAvailable = personnelCharacteristics.getNurses();
        ArrayList<Nurse> nursesAvailable = new ArrayList<>();
        for (Nurse n : personnelCharacteristics.getNurses()) {
            nursesAvailable.add(new Nurse(n));
        }

        ArrayList<Nurse> nursesForSchedule = new ArrayList<>();

        int random = 0;
        Nurse randomNurse;
        Nurse newNurse;

        try {
            while (nursesAvailable.size() < nbrNursesNeeded) {
                int nbr = (nursesAvailable.size() + 1);
                String id = "302" + department + (nbr / 100 == 0 ? "0" : "") + nbr + "*";
                newNurse = new Nurse(id, NurseType.Type2);
                nursesAvailable.add(newNurse);
            }

            for (int i = 0; i < nbrNursesNeeded; i++) {
                random = (int) (Math.random() * nursesAvailable.size());
                randomNurse = nursesAvailable.get(random);

                //newNurse = new Nurse(randomNurse.getId(), randomNurse.getPenaltyPreferenceData(), randomNurse.getEmploymentRate(), randomNurse.getNurseType());
                //newNurse.setPersonalSchedule(cyclicRoster.getRoster()[i]);//give whole row with shifts to work to the newNurse
                randomNurse.setPersonalSchedule(cyclicRoster.getRoster()[i]);
                nursesForSchedule.add(randomNurse);
                nursesAvailable.remove(randomNurse); //to avoid duplicate allocation
            }

            schedule = new int[nursesForSchedule.size()][NBR_DAYS_IN_MONTH];
            for (int i = 0; i < nursesForSchedule.size(); i++) { //Array with nurses for this schedule
                schedule[i] = nursesForSchedule.get(i).getPersonalSchedule(); //info about every day which shift this nurse need to work
            }

        } catch (Exception e) {
            System.out.println("ERROR: " + e.toString());
        }

        String name = "caseD";
        return new NurseSchedule(scenario, department, name, minCovReq, nursesForSchedule, schedule);
    }

    private void calculateBestSchedules_BasedOnCriteria() {
        NurseSchedule randomSchedule_differentShifts;
        NurseSchedule randomSchedule_sameShifts;

        bestSchedule_BasedOnPreference_differentShifts = makeRandomSchedule_differentShifts();
        bestSchedule_BasedOnMinCovReq_differentShifts = makeRandomSchedule_differentShifts();
        bestSchedule_BasedOnSurplusNurses_differentShifts = makeRandomSchedule_differentShifts();
        bestSchedule_BasedOnFairness_differentShifts = makeRandomSchedule_differentShifts();
        bestSchedule_BasedOnRealCosts_differentShifts = makeRandomSchedule_differentShifts();
        bestSchedule_BasedOnAllCosts_differentShifts = makeRandomSchedule_differentShifts();
        bestSchedule_BasedOnPreference_differentShifts.checkAllocationNurses();
        bestSchedule_BasedOnMinCovReq_differentShifts.checkAllocationNurses();
        bestSchedule_BasedOnSurplusNurses_differentShifts.checkAllocationNurses();
        bestSchedule_BasedOnFairness_differentShifts.checkAllocationNurses();
        bestSchedule_BasedOnRealCosts_differentShifts.checkAllocationNurses();
        bestSchedule_BasedOnAllCosts_differentShifts.checkAllocationNurses();

        bestSchedule_BasedOnPreference_sameShifts = makeRandomSchedule_sameShifts();
        bestSchedule_BasedOnMinCovReq_sameShifts = makeRandomSchedule_sameShifts();
        bestSchedule_BasedOnSurplusNurses_sameShifts = makeRandomSchedule_sameShifts();
        bestSchedule_BasedOnFairness_sameShifts = makeRandomSchedule_sameShifts();
        bestSchedule_BasedOnRealCosts_sameShifts = makeRandomSchedule_sameShifts();
        bestSchedule_BasedOnAllCosts_sameShifts = makeRandomSchedule_sameShifts();
        bestSchedule_BasedOnPreference_sameShifts.checkAllocationNurses();
        bestSchedule_BasedOnMinCovReq_sameShifts.checkAllocationNurses();
        bestSchedule_BasedOnSurplusNurses_sameShifts.checkAllocationNurses();
        bestSchedule_BasedOnFairness_sameShifts.checkAllocationNurses();
        bestSchedule_BasedOnRealCosts_sameShifts.checkAllocationNurses();
        bestSchedule_BasedOnAllCosts_sameShifts.checkAllocationNurses();

        bestSchedule_BasedOnPreference_differentShifts.calculateNormalizedCosts();
        bestSchedule_BasedOnMinCovReq_differentShifts.calculateNormalizedCosts();
        bestSchedule_BasedOnSurplusNurses_differentShifts.calculateNormalizedCosts();
        bestSchedule_BasedOnFairness_differentShifts.calculateNormalizedCosts();
        bestSchedule_BasedOnRealCosts_differentShifts.calculateNormalizedCosts();
        bestSchedule_BasedOnAllCosts_differentShifts.calculateNormalizedCosts();

        bestSchedule_BasedOnPreference_sameShifts.calculateNormalizedCosts();
        bestSchedule_BasedOnMinCovReq_sameShifts.calculateNormalizedCosts();
        bestSchedule_BasedOnSurplusNurses_sameShifts.calculateNormalizedCosts();
        bestSchedule_BasedOnFairness_sameShifts.calculateNormalizedCosts();
        bestSchedule_BasedOnRealCosts_sameShifts.calculateNormalizedCosts();
        bestSchedule_BasedOnAllCosts_sameShifts.calculateNormalizedCosts();

        for (int i = 0; i < NBR_RANDOMRUNS; i++) {
            randomSchedule_differentShifts = makeRandomSchedule_differentShifts();
            randomSchedule_sameShifts = makeRandomSchedule_sameShifts();

            randomSchedule_differentShifts.checkAllocationNurses();
            randomSchedule_sameShifts.checkAllocationNurses();

            randomSchedule_differentShifts.calculateNormalizedCosts();
            randomSchedule_sameShifts.calculateNormalizedCosts();

            //BEST SCHEDULES different shifts
            if (randomSchedule_differentShifts.getNormalized_preferenceCost() < bestSchedule_BasedOnPreference_differentShifts.getNormalized_preferenceCost()) {
                bestSchedule_BasedOnPreference_differentShifts = new NurseSchedule(randomSchedule_differentShifts);
            }
            if (randomSchedule_differentShifts.getNormalized_covReqCost() < bestSchedule_BasedOnMinCovReq_differentShifts.getNormalized_covReqCost()) {
                bestSchedule_BasedOnMinCovReq_differentShifts = new NurseSchedule(randomSchedule_differentShifts);
            }
            if (randomSchedule_differentShifts.getNormalized_surplusCost() < bestSchedule_BasedOnSurplusNurses_differentShifts.getNormalized_surplusCost()) {
                bestSchedule_BasedOnSurplusNurses_differentShifts = new NurseSchedule(randomSchedule_differentShifts);
            }
            if (randomSchedule_differentShifts.getNormalized_fairnessCost() < bestSchedule_BasedOnFairness_differentShifts.getNormalized_fairnessCost()) {
                bestSchedule_BasedOnFairness_differentShifts = new NurseSchedule(randomSchedule_differentShifts);
            }
            if (randomSchedule_differentShifts.getNormalized_realCosts() < bestSchedule_BasedOnRealCosts_differentShifts.getNormalized_realCosts()) {
                bestSchedule_BasedOnRealCosts_differentShifts = new NurseSchedule(randomSchedule_differentShifts);
            }
            if (randomSchedule_differentShifts.getWeighted_allCosts() < bestSchedule_BasedOnAllCosts_differentShifts.getWeighted_allCosts()) {
                bestSchedule_BasedOnAllCosts_differentShifts = new NurseSchedule(randomSchedule_differentShifts);
            }

            //BEST SCHEDULES same shifts
            if (randomSchedule_sameShifts.getNormalized_preferenceCost() < bestSchedule_BasedOnPreference_sameShifts.getNormalized_preferenceCost()) {
                bestSchedule_BasedOnPreference_sameShifts = new NurseSchedule(randomSchedule_sameShifts);
            }
            if (randomSchedule_sameShifts.getNormalized_covReqCost() < bestSchedule_BasedOnMinCovReq_sameShifts.getNormalized_covReqCost()) {
                bestSchedule_BasedOnMinCovReq_sameShifts = new NurseSchedule(randomSchedule_sameShifts);
            }
            if (randomSchedule_sameShifts.getNormalized_surplusCost() < bestSchedule_BasedOnSurplusNurses_sameShifts.getNormalized_surplusCost()) {
                bestSchedule_BasedOnSurplusNurses_sameShifts = new NurseSchedule(randomSchedule_sameShifts);
            }
            if (randomSchedule_sameShifts.getNormalized_fairnessCost() < bestSchedule_BasedOnFairness_sameShifts.getNormalized_fairnessCost()) {
                bestSchedule_BasedOnFairness_sameShifts = new NurseSchedule(randomSchedule_sameShifts);
            }
            if (randomSchedule_sameShifts.getNormalized_realCosts() < bestSchedule_BasedOnRealCosts_sameShifts.getNormalized_realCosts()) {
                bestSchedule_BasedOnRealCosts_sameShifts = new NurseSchedule(randomSchedule_sameShifts);
            }
            if (randomSchedule_sameShifts.getWeighted_allCosts() < bestSchedule_BasedOnAllCosts_sameShifts.getWeighted_allCosts()) {
                bestSchedule_BasedOnAllCosts_sameShifts = new NurseSchedule(randomSchedule_sameShifts);
            }
        }
        bestSchedule_BasedOnPreference_differentShifts.setName("bestSchedule_basedOnPreference_differentShifts");
        bestSchedule_BasedOnMinCovReq_differentShifts.setName("bestSchedule_basedOnMinCovReq_differentShifts");
        bestSchedule_BasedOnSurplusNurses_differentShifts.setName("bestSchedule_basedOnSurplusNurses_differentShifts");
        bestSchedule_BasedOnFairness_differentShifts.setName("bestSchedule_basedOnFairness_differentShifts");
        bestSchedule_BasedOnRealCosts_differentShifts.setName("bestSchedule_basedOnRealCosts_differentShifts");
        bestSchedule_BasedOnAllCosts_differentShifts.setName("bestSchedule_basedOnAllCosts_differentShifts");

        bestSchedule_BasedOnPreference_sameShifts.setName("bestSchedule_basedOnPreference_sameShifts");
        bestSchedule_BasedOnMinCovReq_sameShifts.setName("bestSchedule_basedOnMinCovReq_sameShifts");
        bestSchedule_BasedOnSurplusNurses_sameShifts.setName("bestSchedule_basedOnSurplusNurses_sameShifts");
        bestSchedule_BasedOnFairness_sameShifts.setName("bestSchedule_basedOnFairness_sameShifts");
        bestSchedule_BasedOnRealCosts_sameShifts.setName("bestSchedule_basedOnRealCosts_sameShifts");
        bestSchedule_BasedOnAllCosts_sameShifts.setName("bestSchedule_basedOnAllCosts_sameShifts");
    }

    private NurseSchedule makeRandomSchedule_differentShifts() {
        int random;
        Nurse randomNurse;
        int[][] schedule = null;
        NurseSchedule nurseSchedule = null;
        int nbrNursesType1Needed = cyclicRoster.getNbrType1(); //nbrType1 FTE nurses needed
        int nbrNursesType2Needed = cyclicRoster.getNbrType2(); //nbrType2 FTE nurses needed
        int nbrNursesNeeded = nbrNursesType1Needed + nbrNursesType2Needed; //FTE nbr nurses needed

        //ArrayList<Nurse> nursesAvailable = personnelCharacteristics.getNurses(); //DEZE AANPASSEN MAG NIET!
        ArrayList<Nurse> nursesAvailable = new ArrayList<>();
        for (Nurse n : personnelCharacteristics.getNurses()) {
            nursesAvailable.add(new Nurse(n));
        }

        ArrayList<Nurse> setOfNursesUsed = new ArrayList<>();
        //Nurse[] nursesForSchedule = null;
        Nurse newNurse;

        int totalAssignmentsAvailable = 0; //default
        int totalAssignmentsNeeded = nbrNursesNeeded * Nurse.NBR_ASSIGMENTS_FTE;

        try {
            for (Nurse nurse : nursesAvailable) {
                totalAssignmentsAvailable += nurse.getAssignments();
            }

            if (totalAssignmentsAvailable > totalAssignmentsNeeded) { //too much nurses
                int totalAssignments = 0;
                while (totalAssignments < totalAssignmentsNeeded) {
                    random = (int) (Math.random() * nursesAvailable.size());
                    randomNurse = nursesAvailable.get(random);
                    totalAssignments += randomNurse.getAssignments();
                    setOfNursesUsed.add(new Nurse(randomNurse)); //copy constructor of class Nurse
                    nursesAvailable.remove(randomNurse);
                }
            } else { //too little or equal amount
                setOfNursesUsed.addAll(nursesAvailable);
                nursesAvailable.removeAll(nursesAvailable);
                while (totalAssignmentsAvailable < totalAssignmentsNeeded) { //too little
                    int nbr = (setOfNursesUsed.size() + 1);
                    String id = "302" + department + (nbr / 100 == 0 ? "0" : "") + nbr + "*"; //ex. 036 ipv 36

                    if ((totalAssignmentsNeeded > totalAssignmentsAvailable)) {
                        newNurse = new Nurse(id, NurseType.Type2);
                        setOfNursesUsed.add(newNurse);
                        totalAssignmentsAvailable += newNurse.getAssignments();
                    }
                }
            }
            randomAllocationDifferentShifts(setOfNursesUsed);
            schedule = new int[setOfNursesUsed.size()][NBR_DAYS_IN_MONTH];
            for (int i = 0; i < setOfNursesUsed.size(); i++) { //ArrayList with nurses for this schedule
                schedule[i] = setOfNursesUsed.get(i).getPersonalSchedule(); //info about every day which shift this nurse need to work
            }

            String name = "differentShifts";
            nurseSchedule = new NurseSchedule(scenario, department, name, minCovReq, setOfNursesUsed, schedule);
            nurseSchedule.calculateCovReqCost(minCovReq, schedule);
            while (checkMinCovReq(nurseSchedule) == false) { //while there is still a shift with more than 1 nurse too little
                if (nursesAvailable.size() > 0) { //still nurses available
                    random = (int) (Math.random() * nursesAvailable.size());
                    randomNurse = nursesAvailable.get(random);
                    randomNurse.setPersonalSchedule(calculatePersonalSchedule_differentShifts_optimizeMinCovReq(randomNurse, minCovReq, setOfNursesUsed, schedule));
                    nursesAvailable.remove(randomNurse);
                    setOfNursesUsed.add(new Nurse(randomNurse)); //copy constructor of class Nurse
                } else { //no more nursesAvailable
                    int nbr = (setOfNursesUsed.size() + 1);
                    String id = "302" + department + (nbr / 100 == 0 ? "0" : "") + nbr + "*"; //ex. 036 ipv 36

                    newNurse = new Nurse(id, NurseType.Type2);
                    newNurse.setPersonalSchedule(calculatePersonalSchedule_differentShifts_optimizeMinCovReq(newNurse, minCovReq, setOfNursesUsed, schedule));
                    setOfNursesUsed.add(newNurse);
                }
                schedule = new int[setOfNursesUsed.size()][NBR_DAYS_IN_MONTH];
                for (int i = 0; i < setOfNursesUsed.size(); i++) { //ArrayList with nurses for this schedule
                    schedule[i] = setOfNursesUsed.get(i).getPersonalSchedule(); //info about every day which shift this nurse need to work
                }

                nurseSchedule = new NurseSchedule(scenario, department, name, minCovReq, setOfNursesUsed, schedule);
                nurseSchedule.calculateCovReqCost(minCovReq, schedule);
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.toString());
        }
        return nurseSchedule;
    }

    private boolean checkMinCovReq(NurseSchedule nurseSchedule) {
        int[][] nbrShiftsCovered = new int[NurseScheduling.NBR_DAYS_IN_MONTH][minCovReq.length];

        for (int j = 0; j < NurseScheduling.NBR_DAYS_IN_MONTH; j++) { //for first day
            for (int i = 0; i < nurseSchedule.getNurses().size(); i++) { //for all previous nurses
                for (int k = 1; k < shifts.length; k++) { // k=1 means earlyShift
                    if (nurseSchedule.getSchedule()[i][j] == shifts[k]) { //ex. early
                        nbrShiftsCovered[j][k - 1]++; //start at index 0 => nbrShiftsCovered[0]
                    }
                }
            }
            for (int m = 0; m < (shifts.length - 1); m++) {
                if (nbrShiftsCovered[j][m] < (minCovReq[m] - 2)) {
                    return false;
                }
            }
        }
        return true;
    }

    private NurseSchedule makeRandomSchedule_sameShifts() {
        NurseSchedule nurseSchedule = null;
        int[][] schedule = null;
        int random;
        Nurse randomNurse;
        int nbrNursesType1Needed = cyclicRoster.getNbrType1(); //nbrType1 FTE nurses needed
        int nbrNursesType2Needed = cyclicRoster.getNbrType2(); //nbrType2 FTE nurses needed
        int nbrNursesNeeded = nbrNursesType1Needed + nbrNursesType2Needed; //FTE nbr nurses needed

        //ArrayList<Nurse> nursesAvailable = personnelCharacteristics.getNurses(); //DEZE AANPASSEN MAG NIET!
        ArrayList<Nurse> nursesAvailable = new ArrayList<>();
        for (Nurse n : personnelCharacteristics.getNurses()) {
            nursesAvailable.add(new Nurse(n));
        }

        ArrayList<Nurse> setOfNursesUsed = new ArrayList<>();
        //Nurse[] nursesForSchedule = null;
        Nurse newNurse = null;

        int totalAssignmentsAvailable = 0; //default
        int totalAssignmentsNeeded = nbrNursesNeeded * Nurse.NBR_ASSIGMENTS_FTE;

        try {
            for (Nurse nurse : nursesAvailable) {
                totalAssignmentsAvailable += nurse.getAssignments();
            }

            if (totalAssignmentsAvailable > totalAssignmentsNeeded) { //too much nurses
                int totalAssignments = 0;
                while (totalAssignments < totalAssignmentsNeeded) {
                    random = (int) (Math.random() * nursesAvailable.size());
                    randomNurse = nursesAvailable.get(random);
                    totalAssignments += randomNurse.getAssignments();
                    setOfNursesUsed.add(new Nurse(randomNurse)); //copy constructor of class Nurse
                    nursesAvailable.remove(randomNurse);
                }
            } else { //too little or equal amount
                setOfNursesUsed.addAll(nursesAvailable);
                nursesAvailable.removeAll(nursesAvailable);
                while (totalAssignmentsAvailable < totalAssignmentsNeeded) { //too little
                    int nbr = (setOfNursesUsed.size() + 1);
                    String id = "302" + department + (nbr / 100 == 0 ? "0" : "") + nbr + "*"; //ex. 036 ipv 36
                    newNurse = new Nurse(id, NurseType.Type2);
                    setOfNursesUsed.add(newNurse);
                    totalAssignmentsAvailable += newNurse.getAssignments();
                }
            }
            randomAllocationSameShifts(setOfNursesUsed);
            schedule = new int[setOfNursesUsed.size()][NBR_DAYS_IN_MONTH];
            for (int i = 0; i < setOfNursesUsed.size(); i++) { //ArrayList with nurses for this schedule
                schedule[i] = setOfNursesUsed.get(i).getPersonalSchedule(); //info about every day which shift this nurse need to work
            }

            String name = "sameShifts";
            nurseSchedule = new NurseSchedule(scenario, department, name, minCovReq, setOfNursesUsed, schedule);
            nurseSchedule.calculateCovReqCost(minCovReq, schedule);
            while (checkMinCovReq(nurseSchedule) == false) {
                if (nursesAvailable.size() > 0) {
                    random = (int) (Math.random() * nursesAvailable.size());
                    randomNurse = nursesAvailable.get(random);
                    randomNurse.setPersonalSchedule(calculatePersonalSchedule_sameShifts_optimizeMinCovReq(randomNurse, minCovReq, setOfNursesUsed, schedule));
//                    if (randomNurse.getPreferenceCost() == 0) {
//                        System.out.println("In makeRandomSchedule_sameShifts()");
//                    }
                    nursesAvailable.remove(randomNurse);
                    setOfNursesUsed.add(new Nurse(randomNurse)); //copy constructor of class Nurse
                } else { //no more nursesAvailable
                    int nbr = (setOfNursesUsed.size() + 1);
                    String id = "302" + department + (nbr / 100 == 0 ? "0" : "") + nbr + "*"; //ex. 036 ipv 36

                    newNurse = new Nurse(id, NurseType.Type2);
                    newNurse.setPersonalSchedule(calculatePersonalSchedule_sameShifts_optimizeMinCovReq(newNurse, minCovReq, setOfNursesUsed, schedule));
                    setOfNursesUsed.add(newNurse);
                }
                schedule = new int[setOfNursesUsed.size()][NBR_DAYS_IN_MONTH];
                for (int i = 0; i < setOfNursesUsed.size(); i++) { //ArrayList with nurses for this schedule
                    schedule[i] = setOfNursesUsed.get(i).getPersonalSchedule(); //info about every day which shift this nurse need to work
                }

                nurseSchedule = new NurseSchedule(scenario, department, name, minCovReq, setOfNursesUsed, schedule);
                nurseSchedule.calculateCovReqCost(minCovReq, schedule);
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.toString());
        }

        return nurseSchedule;
    }

    private void randomAllocationDifferentShifts(ArrayList<Nurse> nurses) {
        for (int i = 0; i < nurses.size(); i++) {
            nurses.get(i).setPersonalSchedule(calculatePersonalSchedule_differentShifts(nurses.get(i)));//give whole row with shifts to work to the nurse
            if (nurses.get(i).getPreferenceCost() == 0) {
                System.out.println("In randomAllocationDifferentShifts");
            }
        }
    }

    private void randomAllocationSameShifts(ArrayList<Nurse> nurses) {
        for (int i = 0; i < nurses.size(); i++) {
            nurses.get(i).setPersonalSchedule(calculatePersonalSchedule_sameShifts(nurses.get(i)));//give whole row with shifts to work to the nurse
//            if (nurses.get(i).getPreferenceCost() == 0) {
//                System.out.println("In randomAllocationSameShifts \n");
//            }
        }
    }

    private int[] calculatePersonalSchedule_differentShifts(Nurse nurse) {
        int[] schedule = new int[NBR_DAYS_IN_MONTH];
        int randomIndex;
        double employmentRate = nurse.getEmploymentRate();
        boolean identicalWeekend = scenario.isIdentical(); //last 2 patterns non-identical

        int[] scheduleForSevenDays = patterns[getRandomPatternIndex(identicalWeekend)];
        for (int i = 0; i < NBR_DAYS_IN_MONTH / NBR_DAYS_IN_WEEK; i++) {//4 weeks
            for (int j = 0; j < NBR_DAYS_IN_WEEK; j++) {
                schedule[(i * NBR_DAYS_IN_WEEK) + j] = scheduleForSevenDays[j];
            }
        }

        for (int i = 0; i < schedule.length; i++) {
            if (schedule[i] == 1) {
                schedule[i] = shifts[getRandomShiftIndex(employmentRate)]; //random shift
            }
        }

        for (int i = 0; i < schedule.length - 1; i++) {
            if (identicalWeekend && ((i + 2) % NBR_DAYS_IN_WEEK == 0)) { //zaterdag (i == 5,12,19,26)
                schedule[i + 1] = schedule[i]; //zondag (i == 6,13,20,27)
            }
            while (schedule[i] == 3 && schedule[i + 1] == 1) {
                randomIndex = ((int) (Math.random() * shifts.length - 1)) + 1;
                schedule[i + 1] = shifts[randomIndex];
            }
            while (schedule[i] == 4 && (schedule[i + 1] == 1 || schedule[i + 1] == 2)) {
                randomIndex = ((int) (Math.random() * shifts.length - 1)) + 1;
                schedule[i + 1] = shifts[randomIndex];
            }
        }

        if (calculateSum(schedule) == 0) {
            System.out.println("ERROR DIFFERENT SHIFT for nurse " + nurse.getId());
        }

        return schedule;
    }

    private int[] calculatePersonalSchedule_differentShifts_optimizeMinCovReq(Nurse nurse, int[] minCovReq, ArrayList<Nurse> nurses, int[][] schedule) {
        int[] personalSchedule = new int[NBR_DAYS_IN_MONTH];
        int randomIndex;
        double employmentRate = nurse.getEmploymentRate();
        boolean identicalWeekend = scenario.isIdentical(); //last 2 patterns non-identical

        int[] scheduleForSevenDays = patterns[getRandomPatternIndex(identicalWeekend)];
        for (int i = 0; i < NBR_DAYS_IN_MONTH / NBR_DAYS_IN_WEEK; i++) {//4 weeks
            for (int j = 0; j < NBR_DAYS_IN_WEEK; j++) {
                personalSchedule[(i * NBR_DAYS_IN_WEEK) + j] = scheduleForSevenDays[j];
            }
        }

        for (int i = 0; i < personalSchedule.length; i++) {
            if (personalSchedule[i] == 1) {
                personalSchedule[i] = shifts[getRandomShiftIndex(employmentRate)]; //random shift
            }
        }

        for (int i = 0; i < personalSchedule.length - 1; i++) {
            if (identicalWeekend && ((i + 2) % NBR_DAYS_IN_WEEK == 0)) { //zaterdag (i == 5,12,19,26)
                personalSchedule[i + 1] = personalSchedule[i]; //zondag (i == 6,13,20,27)
            }
            while (personalSchedule[i] == 3 && personalSchedule[i + 1] == 1) {
                randomIndex = ((int) (Math.random() * shifts.length - 1)) + 1;
                personalSchedule[i + 1] = shifts[randomIndex];
            }
            while (personalSchedule[i] == 4 && (personalSchedule[i + 1] == 1 || personalSchedule[i + 1] == 2)) {
                randomIndex = ((int) (Math.random() * shifts.length - 1)) + 1;
                personalSchedule[i + 1] = shifts[randomIndex];
            }
        }

        if (calculateSum(personalSchedule) == 0) {
            System.out.println("ERROR DIFFERENT SHIFT for nurse " + nurse.getId());
        }
        int[][] nbrShiftsCovered = new int[NurseScheduling.NBR_DAYS_IN_MONTH][minCovReq.length];

        for (int j = 0; j < NurseScheduling.NBR_DAYS_IN_MONTH; j++) { //for first day
            for (int i = 0; i < nurses.size(); i++) { //for all previous nurses
                for (int k = 1; k < shifts.length; k++) { // k=1 means earlyShift
                    if (schedule[i][j] == shifts[k]) { //ex. early
                        nbrShiftsCovered[j][k - 1]++; //start at index 0 => nbrShiftsCovered[0]
                    }
                }
            }
            for (int m = 0; m < (shifts.length - 1); m++) {
                if (((nbrShiftsCovered[j][m] < minCovReq[m])) && personalSchedule[j] != 0) {
                    personalSchedule[j] = shifts[m + 1];
                }
            }
        }
        return personalSchedule;
    }

    private int[] calculatePersonalSchedule_sameShifts(Nurse nurse) {
        int[] schedule = new int[NBR_DAYS_IN_MONTH];
        int randomPattern;
        double employmentRate = nurse.getEmploymentRate();
        boolean identicalWeekend = scenario.isIdentical(); //last 2 patterns non-identical

        if (!identicalWeekend) { //choose out of all 7 patterns
            randomPattern = (int) (Math.random() * patterns.length);
            if (employmentRate == 1) { //full time nurse
                if (randomPattern == 0) {
                    schedule[0] = schedule[1] = schedule[2] = schedule[3] = schedule[4] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[7] = schedule[8] = schedule[9] = schedule[10] = schedule[11] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[14] = schedule[15] = schedule[16] = schedule[17] = schedule[18] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[21] = schedule[22] = schedule[23] = schedule[24] = schedule[25] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                } else if (randomPattern == 1) {
                    schedule[2] = schedule[3] = schedule[4] = schedule[5] = schedule[6] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[9] = schedule[10] = schedule[11] = schedule[12] = schedule[13] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[16] = schedule[17] = schedule[18] = schedule[19] = schedule[20] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[23] = schedule[24] = schedule[25] = schedule[26] = schedule[27] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                } else if (randomPattern == 2) {
                    schedule[0] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[3] = schedule[4] = schedule[5] = schedule[6] = schedule[7] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[10] = schedule[11] = schedule[12] = schedule[13] = schedule[14] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[17] = schedule[18] = schedule[19] = schedule[20] = schedule[21] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[24] = schedule[25] = schedule[26] = schedule[27] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                } else if (randomPattern == 3) {
                    schedule[0] = schedule[1] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[4] = schedule[5] = schedule[6] = schedule[7] = schedule[8] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[11] = schedule[12] = schedule[13] = schedule[14] = schedule[15] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[18] = schedule[19] = schedule[20] = schedule[21] = schedule[22] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[25] = schedule[26] = schedule[27] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                } else if (randomPattern == 4) {
                    schedule[0] = schedule[1] = schedule[2] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[5] = schedule[6] = schedule[7] = schedule[8] = schedule[9] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[12] = schedule[13] = schedule[14] = schedule[15] = schedule[16] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[19] = schedule[20] = schedule[21] = schedule[22] = schedule[23] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[26] = schedule[27] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                } else if (randomPattern == 5) {
                    schedule[1] = schedule[2] = schedule[3] = schedule[4] = schedule[5] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[8] = schedule[9] = schedule[10] = schedule[11] = schedule[12] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[15] = schedule[16] = schedule[17] = schedule[18] = schedule[19] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[22] = schedule[23] = schedule[24] = schedule[25] = schedule[26] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                } else if (randomPattern == 6) {
                    schedule[0] = schedule[1] = schedule[2] = schedule[3] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[6] = schedule[7] = schedule[8] = schedule[9] = schedule[10] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[13] = schedule[14] = schedule[15] = schedule[16] = schedule[17] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[20] = schedule[21] = schedule[22] = schedule[23] = schedule[24] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[27] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                }
            } else { //employment rate != 1
                if (randomPattern == 0) {
                    schedule[0] = schedule[1] = schedule[2] = schedule[3] = schedule[4] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[7] = schedule[8] = schedule[9] = schedule[10] = schedule[11] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[14] = schedule[15] = schedule[16] = schedule[17] = schedule[18] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[21] = schedule[22] = schedule[23] = schedule[24] = schedule[25] = shifts[(int) (Math.random() * shifts.length)];
                } else if (randomPattern == 1) {
                    schedule[2] = schedule[3] = schedule[4] = schedule[5] = schedule[6] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[9] = schedule[10] = schedule[11] = schedule[12] = schedule[13] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[16] = schedule[17] = schedule[18] = schedule[19] = schedule[20] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[23] = schedule[24] = schedule[25] = schedule[26] = schedule[27] = shifts[(int) (Math.random() * shifts.length)];
                } else if (randomPattern == 2) {
                    schedule[0] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[3] = schedule[4] = schedule[5] = schedule[6] = schedule[7] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[10] = schedule[11] = schedule[12] = schedule[13] = schedule[14] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[17] = schedule[18] = schedule[19] = schedule[20] = schedule[21] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[24] = schedule[25] = schedule[26] = schedule[27] = shifts[(int) (Math.random() * shifts.length)];
                } else if (randomPattern == 3) {
                    schedule[0] = schedule[1] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[4] = schedule[5] = schedule[6] = schedule[7] = schedule[8] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[11] = schedule[12] = schedule[13] = schedule[14] = schedule[15] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[18] = schedule[19] = schedule[20] = schedule[21] = schedule[22] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[25] = schedule[26] = schedule[27] = shifts[(int) (Math.random() * shifts.length)];
                } else if (randomPattern == 4) {
                    schedule[0] = schedule[1] = schedule[2] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[5] = schedule[6] = schedule[7] = schedule[8] = schedule[9] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[12] = schedule[13] = schedule[14] = schedule[15] = schedule[16] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[19] = schedule[20] = schedule[21] = schedule[22] = schedule[23] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[26] = schedule[27] = shifts[(int) (Math.random() * shifts.length)];
                } else if (randomPattern == 5) {
                    schedule[1] = schedule[2] = schedule[3] = schedule[4] = schedule[5] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[8] = schedule[9] = schedule[10] = schedule[11] = schedule[12] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[15] = schedule[16] = schedule[17] = schedule[18] = schedule[19] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[22] = schedule[23] = schedule[24] = schedule[25] = schedule[26] = shifts[(int) (Math.random() * shifts.length)];
                } else if (randomPattern == 6) {
                    schedule[0] = schedule[1] = schedule[2] = schedule[3] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[6] = schedule[7] = schedule[8] = schedule[9] = schedule[10] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[13] = schedule[14] = schedule[15] = schedule[16] = schedule[17] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[20] = schedule[21] = schedule[22] = schedule[23] = schedule[24] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[27] = shifts[(int) (Math.random() * shifts.length)];
                }
            }
        } else { //choose out of the first 5 patterns
            randomPattern = (int) (Math.random() * (patterns.length - 2)); //without the last two who are non-identical
            if (employmentRate == 1) { //full time nurse
                if (randomPattern == 0) {
                    schedule[0] = schedule[1] = schedule[2] = schedule[3] = schedule[4] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[7] = schedule[8] = schedule[9] = schedule[10] = schedule[11] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[14] = schedule[15] = schedule[16] = schedule[17] = schedule[18] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[21] = schedule[22] = schedule[23] = schedule[24] = schedule[25] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                } else if (randomPattern == 1) {
                    schedule[2] = schedule[3] = schedule[4] = schedule[5] = schedule[6] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[9] = schedule[10] = schedule[11] = schedule[12] = schedule[13] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[16] = schedule[17] = schedule[18] = schedule[19] = schedule[20] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[23] = schedule[24] = schedule[25] = schedule[26] = schedule[27] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                } else if (randomPattern == 2) {
                    schedule[0] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[3] = schedule[4] = schedule[5] = schedule[6] = schedule[7] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[10] = schedule[11] = schedule[12] = schedule[13] = schedule[14] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[17] = schedule[18] = schedule[19] = schedule[20] = schedule[21] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[24] = schedule[25] = schedule[26] = schedule[27] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                } else if (randomPattern == 3) {
                    schedule[0] = schedule[1] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[4] = schedule[5] = schedule[6] = schedule[7] = schedule[8] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[11] = schedule[12] = schedule[13] = schedule[14] = schedule[15] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[18] = schedule[19] = schedule[20] = schedule[21] = schedule[22] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[25] = schedule[26] = schedule[27] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                } else if (randomPattern == 4) {
                    schedule[0] = schedule[1] = schedule[2] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[5] = schedule[6] = schedule[7] = schedule[8] = schedule[9] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[12] = schedule[13] = schedule[14] = schedule[15] = schedule[16] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[19] = schedule[20] = schedule[21] = schedule[22] = schedule[23] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[26] = schedule[27] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                }
            } else { //employment rate != 1
                if (randomPattern == 0) {
                    schedule[0] = schedule[1] = schedule[2] = schedule[3] = schedule[4] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[7] = schedule[8] = schedule[9] = schedule[10] = schedule[11] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[14] = schedule[15] = schedule[16] = schedule[17] = schedule[18] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[21] = schedule[22] = schedule[23] = schedule[24] = schedule[25] = shifts[(int) (Math.random() * shifts.length)];
                } else if (randomPattern == 1) {
                    schedule[2] = schedule[3] = schedule[4] = schedule[5] = schedule[6] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[9] = schedule[10] = schedule[11] = schedule[12] = schedule[13] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[16] = schedule[17] = schedule[18] = schedule[19] = schedule[20] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[23] = schedule[24] = schedule[25] = schedule[26] = schedule[27] = shifts[(int) (Math.random() * shifts.length)];
                } else if (randomPattern == 2) {
                    schedule[0] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                    schedule[3] = schedule[4] = schedule[5] = schedule[6] = schedule[7] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[10] = schedule[11] = schedule[12] = schedule[13] = schedule[14] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[17] = schedule[18] = schedule[19] = schedule[20] = schedule[21] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[24] = schedule[25] = schedule[26] = schedule[27] = shifts[(int) (Math.random() * shifts.length)];
                } else if (randomPattern == 3) {
                    schedule[0] = schedule[1] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[4] = schedule[5] = schedule[6] = schedule[7] = schedule[8] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[11] = schedule[12] = schedule[13] = schedule[14] = schedule[15] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[18] = schedule[19] = schedule[20] = schedule[21] = schedule[22] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[25] = schedule[26] = schedule[27] = shifts[(int) (Math.random() * shifts.length)];
                } else if (randomPattern == 4) {
                    schedule[0] = schedule[1] = schedule[2] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[5] = schedule[6] = schedule[7] = schedule[8] = schedule[9] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[12] = schedule[13] = schedule[14] = schedule[15] = schedule[16] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[19] = schedule[20] = schedule[21] = schedule[22] = schedule[23] = shifts[(int) (Math.random() * shifts.length)];
                    schedule[26] = schedule[27] = shifts[(int) (Math.random() * shifts.length)];
                }
            }
        }
        return schedule;
    }

    private int[] calculatePersonalSchedule_sameShifts_optimizeMinCovReq(Nurse nurse, int[] minCovReq, ArrayList<Nurse> nurses, int[][] schedule) {
        int[] personalSchedule = new int[NBR_DAYS_IN_MONTH];
        int randomPattern;
        double employmentRate = nurse.getEmploymentRate();
        boolean identicalWeekend = scenario.isIdentical(); //last 2 patterns non-identical

        if (!identicalWeekend) { //choose out of all 7 patterns
            randomPattern = (int) (Math.random() * patterns.length);
            for (int i = 0; i < NBR_DAYS_IN_MONTH / NBR_DAYS_IN_WEEK; i++) {//4 weeks
                for (int j = 0; j < NBR_DAYS_IN_WEEK; j++) {
                    personalSchedule[(i * NBR_DAYS_IN_WEEK) + j] = patterns[randomPattern][j];
                    if (employmentRate == 1) { //full time nurse
                        if (randomPattern == 0) {
                            personalSchedule[0] = personalSchedule[1] = personalSchedule[2] = personalSchedule[3] = personalSchedule[4] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[7] = personalSchedule[8] = personalSchedule[9] = personalSchedule[10] = personalSchedule[11] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[14] = personalSchedule[15] = personalSchedule[16] = personalSchedule[17] = personalSchedule[18] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[21] = personalSchedule[22] = personalSchedule[23] = personalSchedule[24] = personalSchedule[25] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                        } else if (randomPattern == 1) {
                            personalSchedule[2] = personalSchedule[3] = personalSchedule[4] = personalSchedule[5] = personalSchedule[6] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[9] = personalSchedule[10] = personalSchedule[11] = personalSchedule[12] = personalSchedule[13] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[16] = personalSchedule[17] = personalSchedule[18] = personalSchedule[19] = personalSchedule[20] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[23] = personalSchedule[24] = personalSchedule[25] = personalSchedule[26] = personalSchedule[27] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                        } else if (randomPattern == 2) {
                            personalSchedule[0] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[3] = personalSchedule[4] = personalSchedule[5] = personalSchedule[6] = personalSchedule[7] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[10] = personalSchedule[11] = personalSchedule[12] = personalSchedule[13] = personalSchedule[14] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[17] = personalSchedule[18] = personalSchedule[19] = personalSchedule[20] = personalSchedule[21] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[24] = personalSchedule[25] = personalSchedule[26] = personalSchedule[27] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                        } else if (randomPattern == 3) {
                            personalSchedule[0] = personalSchedule[1] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[4] = personalSchedule[5] = personalSchedule[6] = personalSchedule[7] = personalSchedule[8] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[11] = personalSchedule[12] = personalSchedule[13] = personalSchedule[14] = personalSchedule[15] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[18] = personalSchedule[19] = personalSchedule[20] = personalSchedule[21] = personalSchedule[22] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[25] = personalSchedule[26] = personalSchedule[27] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                        } else if (randomPattern == 4) {
                            personalSchedule[0] = personalSchedule[1] = personalSchedule[2] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[5] = personalSchedule[6] = personalSchedule[7] = personalSchedule[8] = personalSchedule[9] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[12] = personalSchedule[13] = personalSchedule[14] = personalSchedule[15] = personalSchedule[16] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[19] = personalSchedule[20] = personalSchedule[21] = personalSchedule[22] = personalSchedule[23] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[26] = personalSchedule[27] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                        } else if (randomPattern == 5) {
                            personalSchedule[1] = personalSchedule[2] = personalSchedule[3] = personalSchedule[4] = personalSchedule[5] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[8] = personalSchedule[9] = personalSchedule[10] = personalSchedule[11] = personalSchedule[12] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[15] = personalSchedule[16] = personalSchedule[17] = personalSchedule[18] = personalSchedule[19] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[22] = personalSchedule[23] = personalSchedule[24] = personalSchedule[25] = personalSchedule[26] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                        } else if (randomPattern == 6) {
                            personalSchedule[0] = personalSchedule[1] = personalSchedule[2] = personalSchedule[3] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[6] = personalSchedule[7] = personalSchedule[8] = personalSchedule[9] = personalSchedule[10] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[13] = personalSchedule[14] = personalSchedule[15] = personalSchedule[16] = personalSchedule[17] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[20] = personalSchedule[21] = personalSchedule[22] = personalSchedule[23] = personalSchedule[24] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[27] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                        }
                    } else { //employment rate != 1
                        if (randomPattern == 0) {
                            personalSchedule[0] = personalSchedule[1] = personalSchedule[2] = personalSchedule[3] = personalSchedule[4] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[7] = personalSchedule[8] = personalSchedule[9] = personalSchedule[10] = personalSchedule[11] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[14] = personalSchedule[15] = personalSchedule[16] = personalSchedule[17] = personalSchedule[18] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[21] = personalSchedule[22] = personalSchedule[23] = personalSchedule[24] = personalSchedule[25] = shifts[(int) (Math.random() * shifts.length)];
                        } else if (randomPattern == 1) {
                            personalSchedule[2] = personalSchedule[3] = personalSchedule[4] = personalSchedule[5] = personalSchedule[6] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[9] = personalSchedule[10] = personalSchedule[11] = personalSchedule[12] = personalSchedule[13] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[16] = personalSchedule[17] = personalSchedule[18] = personalSchedule[19] = personalSchedule[20] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[23] = personalSchedule[24] = personalSchedule[25] = personalSchedule[26] = personalSchedule[27] = shifts[(int) (Math.random() * shifts.length)];
                        } else if (randomPattern == 2) {
                            personalSchedule[0] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[3] = personalSchedule[4] = personalSchedule[5] = personalSchedule[6] = personalSchedule[7] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[10] = personalSchedule[11] = personalSchedule[12] = personalSchedule[13] = personalSchedule[14] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[17] = personalSchedule[18] = personalSchedule[19] = personalSchedule[20] = personalSchedule[21] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[24] = personalSchedule[25] = personalSchedule[26] = personalSchedule[27] = shifts[(int) (Math.random() * shifts.length)];
                        } else if (randomPattern == 3) {
                            personalSchedule[0] = personalSchedule[1] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[4] = personalSchedule[5] = personalSchedule[6] = personalSchedule[7] = personalSchedule[8] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[11] = personalSchedule[12] = personalSchedule[13] = personalSchedule[14] = personalSchedule[15] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[18] = personalSchedule[19] = personalSchedule[20] = personalSchedule[21] = personalSchedule[22] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[25] = personalSchedule[26] = personalSchedule[27] = shifts[(int) (Math.random() * shifts.length)];
                        } else if (randomPattern == 4) {
                            personalSchedule[0] = personalSchedule[1] = personalSchedule[2] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[5] = personalSchedule[6] = personalSchedule[7] = personalSchedule[8] = personalSchedule[9] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[12] = personalSchedule[13] = personalSchedule[14] = personalSchedule[15] = personalSchedule[16] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[19] = personalSchedule[20] = personalSchedule[21] = personalSchedule[22] = personalSchedule[23] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[26] = personalSchedule[27] = shifts[(int) (Math.random() * shifts.length)];
                        } else if (randomPattern == 5) {
                            personalSchedule[1] = personalSchedule[2] = personalSchedule[3] = personalSchedule[4] = personalSchedule[5] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[8] = personalSchedule[9] = personalSchedule[10] = personalSchedule[11] = personalSchedule[12] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[15] = personalSchedule[16] = personalSchedule[17] = personalSchedule[18] = personalSchedule[19] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[22] = personalSchedule[23] = personalSchedule[24] = personalSchedule[25] = personalSchedule[26] = shifts[(int) (Math.random() * shifts.length)];
                        } else if (randomPattern == 6) {
                            personalSchedule[0] = personalSchedule[1] = personalSchedule[2] = personalSchedule[3] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[6] = personalSchedule[7] = personalSchedule[8] = personalSchedule[9] = personalSchedule[10] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[13] = personalSchedule[14] = personalSchedule[15] = personalSchedule[16] = personalSchedule[17] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[20] = personalSchedule[21] = personalSchedule[22] = personalSchedule[23] = personalSchedule[24] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[27] = shifts[(int) (Math.random() * shifts.length)];
                        }
                    }
                }
            }
        } else { //choose out of the first 5 patterns
            randomPattern = (int) (Math.random() * (patterns.length - 2)); //without the last two who are non-identical
            for (int i = 0; i < NBR_DAYS_IN_MONTH / NBR_DAYS_IN_WEEK; i++) {//4 weeks
                for (int j = 0; j < NBR_DAYS_IN_WEEK; j++) {
                    personalSchedule[(i * NBR_DAYS_IN_WEEK) + j] = patterns[randomPattern][j];
                    if (employmentRate == 1) { //full time nurse
                        if (randomPattern == 0) {
                            personalSchedule[0] = personalSchedule[1] = personalSchedule[2] = personalSchedule[3] = personalSchedule[4] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[7] = personalSchedule[8] = personalSchedule[9] = personalSchedule[10] = personalSchedule[11] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[14] = personalSchedule[15] = personalSchedule[16] = personalSchedule[17] = personalSchedule[18] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[21] = personalSchedule[22] = personalSchedule[23] = personalSchedule[24] = personalSchedule[25] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                        } else if (randomPattern == 1) {
                            personalSchedule[2] = personalSchedule[3] = personalSchedule[4] = personalSchedule[5] = personalSchedule[6] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[9] = personalSchedule[10] = personalSchedule[11] = personalSchedule[12] = personalSchedule[13] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[16] = personalSchedule[17] = personalSchedule[18] = personalSchedule[19] = personalSchedule[20] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[23] = personalSchedule[24] = personalSchedule[25] = personalSchedule[26] = personalSchedule[27] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                        } else if (randomPattern == 2) {
                            personalSchedule[0] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[3] = personalSchedule[4] = personalSchedule[5] = personalSchedule[6] = personalSchedule[7] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[10] = personalSchedule[11] = personalSchedule[12] = personalSchedule[13] = personalSchedule[14] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[17] = personalSchedule[18] = personalSchedule[19] = personalSchedule[20] = personalSchedule[21] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[24] = personalSchedule[25] = personalSchedule[26] = personalSchedule[27] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                        } else if (randomPattern == 3) {
                            personalSchedule[0] = personalSchedule[1] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[4] = personalSchedule[5] = personalSchedule[6] = personalSchedule[7] = personalSchedule[8] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[11] = personalSchedule[12] = personalSchedule[13] = personalSchedule[14] = personalSchedule[15] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[18] = personalSchedule[19] = personalSchedule[20] = personalSchedule[21] = personalSchedule[22] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[25] = personalSchedule[26] = personalSchedule[27] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                        } else if (randomPattern == 4) {
                            personalSchedule[0] = personalSchedule[1] = personalSchedule[2] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[5] = personalSchedule[6] = personalSchedule[7] = personalSchedule[8] = personalSchedule[9] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[12] = personalSchedule[13] = personalSchedule[14] = personalSchedule[15] = personalSchedule[16] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[19] = personalSchedule[20] = personalSchedule[21] = personalSchedule[22] = personalSchedule[23] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[26] = personalSchedule[27] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                        }
                    } else { //employment rate != 1
                        if (randomPattern == 0) {
                            personalSchedule[0] = personalSchedule[1] = personalSchedule[2] = personalSchedule[3] = personalSchedule[4] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[7] = personalSchedule[8] = personalSchedule[9] = personalSchedule[10] = personalSchedule[11] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[14] = personalSchedule[15] = personalSchedule[16] = personalSchedule[17] = personalSchedule[18] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[21] = personalSchedule[22] = personalSchedule[23] = personalSchedule[24] = personalSchedule[25] = shifts[(int) (Math.random() * shifts.length)];
                        } else if (randomPattern == 1) {
                            personalSchedule[2] = personalSchedule[3] = personalSchedule[4] = personalSchedule[5] = personalSchedule[6] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[9] = personalSchedule[10] = personalSchedule[11] = personalSchedule[12] = personalSchedule[13] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[16] = personalSchedule[17] = personalSchedule[18] = personalSchedule[19] = personalSchedule[20] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[23] = personalSchedule[24] = personalSchedule[25] = personalSchedule[26] = personalSchedule[27] = shifts[(int) (Math.random() * shifts.length)];
                        } else if (randomPattern == 2) {
                            personalSchedule[0] = shifts[((int) (Math.random() * shifts.length - 1)) + 1];
                            personalSchedule[3] = personalSchedule[4] = personalSchedule[5] = personalSchedule[6] = personalSchedule[7] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[10] = personalSchedule[11] = personalSchedule[12] = personalSchedule[13] = personalSchedule[14] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[17] = personalSchedule[18] = personalSchedule[19] = personalSchedule[20] = personalSchedule[21] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[24] = personalSchedule[25] = personalSchedule[26] = personalSchedule[27] = shifts[(int) (Math.random() * shifts.length)];
                        } else if (randomPattern == 3) {
                            personalSchedule[0] = personalSchedule[1] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[4] = personalSchedule[5] = personalSchedule[6] = personalSchedule[7] = personalSchedule[8] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[11] = personalSchedule[12] = personalSchedule[13] = personalSchedule[14] = personalSchedule[15] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[18] = personalSchedule[19] = personalSchedule[20] = personalSchedule[21] = personalSchedule[22] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[25] = personalSchedule[26] = personalSchedule[27] = shifts[(int) (Math.random() * shifts.length)];
                        } else if (randomPattern == 4) {
                            personalSchedule[0] = personalSchedule[1] = personalSchedule[2] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[5] = personalSchedule[6] = personalSchedule[7] = personalSchedule[8] = personalSchedule[9] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[12] = personalSchedule[13] = personalSchedule[14] = personalSchedule[15] = personalSchedule[16] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[19] = personalSchedule[20] = personalSchedule[21] = personalSchedule[22] = personalSchedule[23] = shifts[(int) (Math.random() * shifts.length)];
                            personalSchedule[26] = personalSchedule[27] = shifts[(int) (Math.random() * shifts.length)];
                        }
                    }
                }
            }
        }
        int[][] nbrShiftsCovered = new int[NurseScheduling.NBR_DAYS_IN_MONTH][minCovReq.length];

        for (int j = 0; j < NurseScheduling.NBR_DAYS_IN_MONTH; j++) { //for first day
            for (int i = 0; i < nurses.size(); i++) { //for all previous nurses
                for (int k = 1; k < shifts.length; k++) { // k=1 means earlyShift
                    if (schedule[i][j] == shifts[k]) { //ex. early
                        nbrShiftsCovered[j][k - 1]++; //start at index 0 => nbrShiftsCovered[0]
                    }
                }
            }
            for (int m = 0; m < (shifts.length - 1); m++) {
                if ((nbrShiftsCovered[j][m] < minCovReq[m]) && personalSchedule[j] != 0) {
                    personalSchedule[j] = shifts[m + 1];
                }
            }
        }
        return personalSchedule;
    }

    private int getRandomPatternIndex(boolean identicalWeekend) {
        int randomPattern;

        if (!identicalWeekend) { //choose out of all 7 patterns
            randomPattern = (int) (Math.random() * patterns.length);
        } else { //choose out of the first 5 patterns
            randomPattern = (int) (Math.random() * (patterns.length - 2)); //without the last two who are non-identical
        }
        return randomPattern;
    }

    private int getRandomShiftIndex(double employmentRate) {
        int randomIndex;

        if (employmentRate == 1) { //full time nurse
            randomIndex = ((int) (Math.random() * shifts.length - 1)) + 1;//min value 1
        } else { //NOT a full time nurse
            randomIndex = (int) (Math.random() * shifts.length);
        }

        return randomIndex;
    }

    //    private int[] calculatePersonalSchedule_differentShifts(Nurse nurse) {
//        int[] schedule = new int[NBR_DAYS_IN_MONTH];
//        int randomPattern;
//        double employmentRate = nurse.getEmploymentRate();
//        boolean identicalWeekend = scenario.isIdentical(); //last 2 patterns non-identical
//
//        if (!identicalWeekend) { //choose out of all 7 patterns
//            randomPattern = (int) (Math.random() * patterns.length);
//            for (int i = 0; i < NBR_DAYS_IN_MONTH / NBR_DAYS_IN_WEEK; i++) {//4 weeks
//                for (int j = 0; j < NBR_DAYS_IN_WEEK; j++) {
//                    schedule[(i * NBR_DAYS_IN_WEEK) + j] = patterns[randomPattern][j];
//                }
//            }
//        } else { //choose out of the first 5 patterns
//            randomPattern = (int) (Math.random() * (patterns.length - 2)); //without the last two who are non-identical
//            for (int i = 0; i < NBR_DAYS_IN_MONTH / NBR_DAYS_IN_WEEK; i++) {//4 weeks
//                for (int j = 0; j < NBR_DAYS_IN_WEEK; j++) {
//                    schedule[(i * NBR_DAYS_IN_WEEK) + j] = patterns[randomPattern][j];
//                }
//            }
//        }
//
//        if (employmentRate == 1) { //full time nurse
//            for (int i = 0; i < schedule.length; i++) {
//                if (schedule[i] == 1) {
//                    int randomIndex = ((int) (Math.random() * shifts.length - 1)) + 1;//min value 1
//                    schedule[i] = shifts[randomIndex]; //random shift
//                }
//            }
//            for (int i = 0; i < (schedule.length-1); i++) {
//                if (identicalWeekend) {
//                    for (int j = 0; j < NBR_DAYS_IN_MONTH / NBR_DAYS_IN_WEEK; j++) {
//                        if (i == ((NBR_DAYS_IN_WEEK - 2) * j)) {
//                            schedule[i+1] = schedule[i];
//                        }
//                    }
//                }
//                while (schedule[i] == 3 && schedule[i+1] == 1) {
//                    int randomIndex = ((int) (Math.random() * shifts.length - 1)) + 1;
//                    schedule[i+1] = shifts[randomIndex];
//                }
//                while (schedule[i] == 4 && (schedule[i+1] == 1 || schedule[i+1] == 2)) {
//                    int randomIndex = ((int) (Math.random() * shifts.length - 1)) + 1;
//                    schedule[i+1] = shifts[randomIndex];
//                }
//            }
//        } else { //NOT a full time nurse
//            for (int i = 0; i < schedule.length; i++) {
//                if (schedule[i] == 1) {
//                    int randomIndex = (int) (Math.random() * shifts.length);
//                    schedule[i] = shifts[randomIndex]; //random shift
//                }
//                if (identicalWeekend == true) {
//                    for (int j = 0; j < NBR_DAYS_IN_MONTH / NBR_DAYS_IN_WEEK; j++) {
//                        if (i == ((NBR_DAYS_IN_WEEK - 2) * j)) {
//                            if (schedule[i] != 0 && schedule[i + 1] != 0) {
//                                schedule[i + 1] = schedule[i];
//                            }
//                        }
//                    }
//                }
//                while (schedule[i] == 3 && schedule[i + 1] == 1) {
//                    int randomIndex = ((int) (Math.random() * shifts.length - 1)) + 1;
//                    schedule[i + 1] = shifts[randomIndex];
//                }
//                while (schedule[i] == 4 && (schedule[i + 1] == 1 || schedule[i + 1] == 2)) {
//                    int randomIndex = ((int) (Math.random() * shifts.length - 1)) + 1;
//                    schedule[i + 1] = shifts[randomIndex];
//                }
//            }
//        }
//        return schedule;
//    }
    //    private NurseSchedule makeRandomSchedule_BasedOnCyclicRoster() {
//        int[][] schedule = null;
//        int nbrNursesType1Needed = cyclicRoster.getNbrType1(); //nbrType1 FTE nurses needed
//        int nbrNursesType2Needed = cyclicRoster.getNbrType2(); //nbrType2 FTE nurses needed
//        int nbrNursesNeeded = nbrNursesType1Needed + nbrNursesType2Needed; //FTE nbr nurses needed
//
//        //ArrayList<Nurse> nursesType1 = personnelCharacteristics.getNurses(NurseType.Type1); //nbrType1 nurses (NOT FTE) available
//        //ArrayList<Nurse> nursesType2 = personnelCharacteristics.getNurses(NurseType.Type2); //nbrType2 nurses (NOT FTE) available
//        ArrayList<Nurse> nursesAvailable = personnelCharacteristics.getNurses();
//        Nurse[] nursesForSchedule = new Nurse[nbrNursesType1Needed + nbrNursesType2Needed];
//
//        int random = 0;
//        Nurse randomNurse;
//        Nurse newNurse;
//        //int totalAssignmentsAvailableType1 = 0;
//        //int totalAssignmentsAvailableType2 = 0;
//        int totalAssignmentsAvailable = 0; //default
//        //int totalAssignmentsNeededType1 = nbrNursesType1Needed * Nurse.NBR_ASSIGMENTS_FTE;
//        //int totalAssignmentsNeededType2 = nbrNursesType2Needed * Nurse.NBR_ASSIGMENTS_FTE;
//        int totalAssignmentsNeeded = nbrNursesNeeded * Nurse.NBR_ASSIGMENTS_FTE;
//
//        try {
//            for (Nurse nurse : nursesAvailable) {
//                totalAssignmentsAvailable += nurse.getAssignments();
//            }
//
//            while (totalAssignmentsAvailable < totalAssignmentsNeeded) {
//                int nbr = (nursesAvailable.size() + 1);
//                String id = "new nurse nbr " + (nbr / 100 == 0 ? "0" : "") + nbr; //ex. 036 ipv 36
//
//                if ((totalAssignmentsNeeded - totalAssignmentsAvailable) > Nurse.NBR_ASSIGMENTS_FTE) {
//                    newNurse = new Nurse(id, NurseType.Type2);
//                } else {
//                    double employmentrate = (double) (totalAssignmentsNeeded - totalAssignmentsAvailable) / Nurse.NBR_ASSIGMENTS_FTE;
//                    newNurse = new Nurse(id, employmentrate, NurseType.Type2);
//                }
//                //UPDATE
//                totalAssignmentsAvailable += newNurse.getAssignments();
//                nursesAvailable.add(newNurse);
//            }
//
//            for (int i = 0; i < nbrNursesNeeded; i++) {
//                random = (int) (Math.random() * nursesAvailable.size());
//                randomNurse = nursesAvailable.get(random);
//
//                newNurse = new Nurse(randomNurse.getId(), randomNurse.getPenaltyPreferenceData(), randomNurse.getEmploymentRate(), randomNurse.getNurseType());
//                newNurse.setPersonalSchedule(cyclicRoster.getRoster()[i]);//give whole row with shifts to work to the newNurse
//                nursesForSchedule[i] = newNurse;
//                nursesAvailable.remove(randomNurse); //to avoid duplicate allocation
//            }
//
//            schedule = new int[nursesForSchedule.length][NBR_DAYS_IN_MONTH];
//            for (int i = 0; i < nursesForSchedule.length; i++) { //Array with nurses for this schedule
//                schedule[i] = nursesForSchedule[i].getPersonalSchedule(); //info about every day which shift this nurse need to work
//            }
//
//        } catch (Exception e) {
//            System.out.println("ERROR: " + e.toString());
//        }
//
//        return new NurseSchedule(scenario, department, minCovReq, nursesForSchedule, schedule);
//    }
//    private NurseSchedule calculateBestSchedule_BasedOnSurplusNurses() {
//        NurseSchedule randomSchedule;
//        bestSchedule_BasedOnSurplusNurses = makeRandomSchedule();
//
//        for (int i = 0; i < NBR_RANDOMRUNS; i++) {
//            randomSchedule = makeRandomSchedule();
//            if (randomSchedule.getSurplusCost() < bestSchedule_BasedOnSurplusNurses.getSurplusCost()) {
//                bestSchedule_BasedOnSurplusNurses = randomSchedule;
//            }
//        }
//        return bestSchedule_BasedOnSurplusNurses;
//    }
//    private NurseSchedule calculateBestSchedule_BasedOnFairness() {
//        NurseSchedule randomSchedule;
//        bestSchedule_BasedOnFairness = makeRandomSchedule();
//
//        for (int i = 0; i < NBR_RANDOMRUNS; i++) {
//            randomSchedule = makeRandomSchedule();
//            if (randomSchedule.getFairnessCost() < bestSchedule_BasedOnFairness.getFairnessCost()) {
//                bestSchedule_BasedOnFairness = randomSchedule;
//            }
//        }
//        return bestSchedule_BasedOnFairness;
//    }
//    private NurseSchedule[] makeStartSchedules_BasedOnPreference() {
//        startSchedules_BasedOnPreference = new NurseSchedule[NBR_SCHEDULES_SAVED];
//        NurseSchedule randomSchedule;
//        int indexOfWorstSchedule = -1;
//
//        for (int i = 0; i < NBR_RANDOMRUNS; i++) {
//            if (i < NBR_SCHEDULES_SAVED) {
//                randomSchedule = makeRandomSchedule_BasedOnPreference();
//                startSchedules_BasedOnPreference[i] = randomSchedule;
//            } else {
//                randomSchedule = makeRandomSchedule_BasedOnPreference();
//                indexOfWorstSchedule = searchIndexOfMaxPenalty(startSchedules_BasedOnPreference);
//                if (randomSchedule.getPenaltyPreferenceCost() < startSchedules_BasedOnPreference[indexOfWorstSchedule].getPenaltyPreferenceCost()) {
//                    startSchedules_BasedOnPreference[indexOfWorstSchedule] = randomSchedule;
//                }
//            }
//
//        }
//        Arrays.sort(startSchedules_BasedOnPreference, new Comparator<NurseSchedule>() {
//
//            @Override
//            public int compare(NurseSchedule ns1, NurseSchedule ns2) {
//                return ns1.getPenaltyPreferenceCost() - ns2.getPenaltyPreferenceCost();
//            }
//        });
//        return startSchedules_BasedOnPreference;
//    }
//    private NurseSchedule[] makeStartSchedules_BasedOnMinCovReq() {
//        NurseSchedule[] startSchedules = new NurseSchedule[NBR_SCHEDULES_SAVED];
//        NurseSchedule randomSchedule;
//        int indexOfWorstSchedule = -1;
//
//        for (int i = 0; i < NBR_RANDOMRUNS; i++) {
//            if (i < NBR_SCHEDULES_SAVED) {
//                randomSchedule = makeRandomSchedule_BasedOnMinCovReq();
//                startSchedules[i] = randomSchedule;
//            } else {
//                randomSchedule = makeRandomSchedule_BasedOnMinCovReq();
//                indexOfWorstSchedule = searchIndexOfMaxCovReqCost(startSchedules);
//                if (randomSchedule.getCovReqCost() < startSchedules[indexOfWorstSchedule].getCovReqCost()) {
//                    startSchedules[indexOfWorstSchedule] = randomSchedule;
//                }
//            }
//        }
//        Arrays.sort(startSchedules, new Comparator<NurseSchedule>() {
//
//            @Override
//            public int compare(NurseSchedule ns1, NurseSchedule ns2) {
//                return ns1.getCovReqCost() - ns2.getCovReqCost();
//            }
//        });
//        return startSchedules;
//    }
//    
//    
//    private NurseSchedule makeRandomSchedule_BasedOnMinCovReq() {
//        Nurse[] nurses = bestStartSchedule_BasedOnPreference.getNurses();
//        int[][] schedule_basedOnMinCovReq = null;
//        
//        return new NurseSchedule(scenario, department, minCovReq, nurses, schedule_basedOnMinCovReq);
//    }
//
//    private int searchIndexOfMaxPenalty(NurseSchedule[] startSchedules) {
//        int maxPenalty = 0;
//        int indexOfMaxPenalty = -1;
//        int penalty;
//
//        for (int i = 0; i < startSchedules.length; i++) {
//            penalty = startSchedules[i].getPenaltyPreferenceCost();
//            if (penalty > maxPenalty) {
//                maxPenalty = penalty;
//                indexOfMaxPenalty = i;
//            }
//        }
//        return indexOfMaxPenalty;
//    }
//
//    private int searchIndexOfMaxCovReqCost(NurseSchedule[] startSchedules) {
//        int maxPenalty = 0;
//        int indexOfMaxPenalty = -1;
//        int penalty;
//
//        for (int i = 0; i < startSchedules.length; i++) {
//            penalty = startSchedules[i].getCovReqCost();
//            if (penalty > maxPenalty) {
//                maxPenalty = penalty;
//                indexOfMaxPenalty = i;
//            }
//        }
//        return indexOfMaxPenalty;
//    }
    private void calculateMinCovReq(Scenario scenario, char department) {
        int[][] minCovReqAllDep = scenario.getMinCovReq();
        int dep = department - 'A';

        minCovReq = new int[(shifts.length - 1)];

        for (int i = 0; i < (shifts.length - 1); i++) {
            minCovReq[i] = minCovReqAllDep[i][dep];
            //finalNurseSchedule.setMinCovReq(minCovReq);
        }
    }

    @Override
    public String toString() {
        String outputNurseScheduling = "**********************\n"
                + "**** DEPARTMENT_" + this.department + " ****\n"
                + "**********************\n\n"
                + (cyclicRoster != null ? cyclicRoster.toString() : "") + "\n\n"
                + (personnelCharacteristics != null ? personnelCharacteristics.toString() : "") + "\n\n";

        for (Nurse nurse : personnelCharacteristics.getNurses()) {
            outputNurseScheduling += nurse.toString();
        }

        return outputNurseScheduling;
    }

    private int calculateSum(int[] schedule) {
        int sum = 0;
        for (int i : schedule) {
            sum += i;
        }
        return sum;
    }
}
