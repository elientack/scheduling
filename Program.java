package nurse.scheduling;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sandra
 */
public class Program {

    static final int NBR_OF_DEPARTMENTS = 4;
    static final int NBR_OF_SCENARIOS = 4;

    static String patterns = "./input_files/Patterns.txt";
    static Scenario[] scenarios = new Scenario[NBR_OF_SCENARIOS];

    public static final int MIN = 0; //idx0 = min
    public static final int MAX = 1; // idx1 = max

    public static int[] PREF_COST_CASE_D = {Integer.MAX_VALUE, Integer.MIN_VALUE}; //idx0 = min (initialised with max value) ; idx1 = max (initialised with min value)
    public static int[] COV_REQ_COST_CASE_D = {Integer.MAX_VALUE, Integer.MIN_VALUE};
    public static int[] SURPLUS_COST_CASE_D = {Integer.MAX_VALUE, Integer.MIN_VALUE};
    public static int[] FAIRNESS_COST_CASE_D = {Integer.MAX_VALUE, Integer.MIN_VALUE};
    public static double[] REAL_COST_CASE_D = {Double.MAX_VALUE, Double.MIN_VALUE};

    public static int[] PREF_COST_DIFF_SHIFTS = {Integer.MAX_VALUE, Integer.MIN_VALUE};
    public static int[] COV_REQ_COST_DIFF_SHIFTS = {Integer.MAX_VALUE, Integer.MIN_VALUE};
    public static int[] SURPLUS_COST_DIFF_SHIFTS = {Integer.MAX_VALUE, Integer.MIN_VALUE};
    public static int[] FAIRNESS_COST_DIFF_SHIFTS = {Integer.MAX_VALUE, Integer.MIN_VALUE};
    public static double[] REAL_COST_DIFF_SHIFTS = {Double.MAX_VALUE, Double.MIN_VALUE};

    public static int[] PREF_COST_SAME_SHIFTS = {Integer.MAX_VALUE, Integer.MIN_VALUE};
    public static int[] COV_REQ_COST_SAME_SHIFTS = {Integer.MAX_VALUE, Integer.MIN_VALUE};
    public static int[] SURPLUS_COST_SAME_SHIFTS = {Integer.MAX_VALUE, Integer.MIN_VALUE};
    public static int[] FAIRNESS_COST_SAME_SHIFTS = {Integer.MAX_VALUE, Integer.MIN_VALUE};
    public static double[] REAL_COST_SAME_SHIFTS = {Double.MAX_VALUE, Double.MIN_VALUE};

    static boolean normalizationLoop;

    static final int CASE_D = 0;
    static final int DIFF_SHIFTS = 1;
    static final int SAME_SHIFTS = 2;

    static NurseSchedule[][][] allBestSchedules = new NurseSchedule[NBR_OF_SCENARIOS][NBR_OF_DEPARTMENTS][];

    public static void main(String[] args) throws IOException {
        clearDirectoryStructure();
        readScenarios();

        generateSchedules(true);     //FILL IN MIN EN MAX
        generateSchedules(false);    //GENERATE NORMALIZED BEST SCHEDULES

        findAndStoreBestSchedules();
    }

    private static void readScenarios() {
        for (int i = 0; i < NBR_OF_SCENARIOS; i++) {
            String scenarioName = "Scenario" + (i + 1);
            String scenarioFile = "./input_files/" + scenarioName + "/Scenario_geg.txt";

            try {
                scenarios[i] = IO.readScenario(scenarioFile);
            } catch (IOException ex) {
                Logger.getLogger(Scenario.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private static void generateSchedules(boolean normalize) {
        for (int i = 0; i < NBR_OF_SCENARIOS; i++) {
            String scenarioName = "Scenario" + (i + 1);

            System.out.println("-------------");
            System.out.println(scenarioName + (normalize ? " (define min and max)" : " (finding best schedules)")); //E: Als links van ? true is wordt tss ? en : uitgevoerd, anders na :
            System.out.println("-------------");

            for (int j = 0; j < NBR_OF_DEPARTMENTS; j++) {
                char dept = (char) ('A' + j); //first A, then B, then C and then D

                //String shift_system = "./files/Shift_system_dpt_" + dept + ".txt";
                //String monthly_roster_rules = "./files/Constraints_dpt_" + dept + ".txt";
                String cyclic_roster = "./input_files/" + scenarioName + "/Cyclic_roster_dpt_" + dept + ".txt";
                String personnel = "./input_files/Personnel_dpt_" + dept + ".txt";
                //String scenario = "./files/" + scenarioName + dept + "/.txt";

                NurseScheduling nsForDept = new NurseScheduling(scenarios[i], dept, cyclic_roster, patterns, personnel, normalize);
                System.out.println(nsForDept.toString());

                if (!normalize) {
                    allBestSchedules[i][j] = nsForDept.getBestSchedules_BasedOnAllCosts();
                }
            }
        }
    }

    private static void findAndStoreBestSchedules() throws IOException {
        for (int i = 0; i < NBR_OF_SCENARIOS; i++) {
            double sumDiffShifts = 0;
            double sumSameShifts = 0;

            for (int j = 0; j < NBR_OF_DEPARTMENTS; j++) {
                sumDiffShifts += allBestSchedules[i][j][DIFF_SHIFTS].getNormalized_allCosts();
                sumSameShifts += allBestSchedules[i][j][SAME_SHIFTS].getNormalized_allCosts();
            }
            if (sumDiffShifts < sumSameShifts) {
                for (int j = 0; j < NBR_OF_DEPARTMENTS; j++) {
                    NurseScheduling.writeOutputAllCosts(allBestSchedules[i][j][DIFF_SHIFTS]);
                }
            } else if (sumSameShifts < sumDiffShifts) {
                for (int j = 0; j < NBR_OF_DEPARTMENTS; j++) {
                    NurseScheduling.writeOutputAllCosts(allBestSchedules[i][j][SAME_SHIFTS]);
                }
            }
        }
    }

    private static void clearDirectoryStructure() {
        String scenarioName;
        String dep;
        File dir;

        String[] wayOfMakingSchedules = {"caseD", "differentShifts", "sameShifts"};

        for (int i = 0; i < NBR_OF_SCENARIOS; i++) {
            scenarioName = "Scenario" + (i + 1);

            for (int j = 0; j < NBR_OF_DEPARTMENTS; j++) {
                dep = "dpt_" + (char) ('A' + j);

                for (int k = 0; k < wayOfMakingSchedules.length; k++) {
                    dir = new File("./output_files/" + scenarioName + "/" + dep + "/" + wayOfMakingSchedules[k]);
                    for (File f : dir.listFiles()) {
                        f.delete();
                    }
                }
            }

            dir = new File("./output_files/" + scenarioName + "/BestSchedules_BasedOnAllCosts");
            for (File f : dir.listFiles()) {
                f.delete();
            }
        }
    }
}
