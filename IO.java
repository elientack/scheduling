package nurse.scheduling;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author Sandra
 */
public class IO {

    public static Scenario readScenario(String scenarioFile) throws IOException {
        BufferedReader br = null;

        String scenarioName = "";
        int nbrShifts = 0;
        int[] shifts = null;
        int consecutiveDaysWork = 0;
        int consecutiveDaysFree = 0;
        boolean identical = false;
        int[][] minCovReq = null; //per shift per department

        try {
            br = new BufferedReader(new FileReader(scenarioFile));
            scenarioName = scenarioFile.substring(14, 23);

            String shiftInfo = br.readLine();
            String[] shiftsString = shiftInfo.split("\t");

            nbrShifts = Integer.parseInt(shiftsString[0]);
            shifts = new int[nbrShifts];

            for (int i = 0; i < nbrShifts; i++) {
                shifts[i] = Integer.parseInt(shiftsString[i + 1]); // without shiftsString[0]
            }

            String nextLine = br.readLine();
            String[] consecutiveDays = nextLine.split("\t");

            consecutiveDaysWork = Integer.parseInt(consecutiveDays[0]);
            consecutiveDaysFree = Integer.parseInt(consecutiveDays[1]);

            String weekendLine = br.readLine();
            String[] weekInfo = weekendLine.split("\t");
            identical = Boolean.parseBoolean(weekInfo[0]);

            String minCovReqInfo;
            minCovReq = new int[nbrShifts-1][NurseScheduling.NBR_DEP]; //ex. per shift per department
            for (int i = 0; i < (nbrShifts-1); i++) {
                minCovReqInfo = br.readLine();
                String[] minCovReqString = minCovReqInfo.split("\t");
                for (int j = 0; j < NurseScheduling.NBR_DEP; j++) {
                    minCovReq[i][j] = Integer.parseInt(minCovReqString[j]); //same shift all departments
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getClass().getSimpleName() + ":" + e.getMessage());
        } finally {
            if (br != null) {
                br.close();
            }
        }

        return new Scenario(scenarioName, shifts, consecutiveDaysWork, consecutiveDaysFree, identical, minCovReq);
    }
    
    public static int[][] readPattern(String patterns) throws IOException {
        BufferedReader br = null;
        int[][] pattern = new int[7][NurseScheduling.NBR_DAYS_IN_WEEK]; //per pattern 5 times 1; 2 times 0;

        try {
            br = new BufferedReader(new FileReader(patterns));

            for (int i = 0; i < 7; i++) { //7 patterns
                String nextLine = br.readLine();
                String[] infoPattern = nextLine.split("\t");
                for (int j = 0; j < 7; j++) { //7 days in a week
                    pattern[i][j] = Integer.parseInt(infoPattern[j]);
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getClass().getSimpleName() + ":" + e.getMessage());
        } finally {
            if (br != null) {
                br.close();
            }
        }
        return pattern;
    }

    
    public static ShiftSystem readShiftSystem(String bestand) throws IOException {
        BufferedReader br = null;

        int[] startTimes = null;
        int[] endTimes = null;
        int[] valuesForReq = null;

        try {
            br = new BufferedReader(new FileReader(bestand));

            String firstLine = br.readLine();
            String[] info = firstLine.split("\t");

            int nbrShifts = Integer.parseInt(info[0]);
            int duration = Integer.parseInt(info[1]);
            startTimes = new int[nbrShifts];
            endTimes = new int[nbrShifts];
            valuesForReq = new int[nbrShifts];

            String secondLine = br.readLine();
            String[] startShifts = secondLine.split("\t");

            for (int i = 0; i < nbrShifts; i++) {
                startTimes[i] = Integer.parseInt(startShifts[i]);
                endTimes[i] = (startTimes[i] + duration) % 24;
            }

            String thirdLine = br.readLine();
            String[] req = thirdLine.split("\t");

            for (int i = 0; i < nbrShifts; i++) {
                valuesForReq[i] = Integer.parseInt(req[i]);
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getClass().getSimpleName() + ":" + e.getMessage());
        } finally {
            if (br != null) {
                br.close();
            }
        }

        return new ShiftSystem(startTimes, endTimes, valuesForReq);
    }

    public static CyclicRoster readCyclicRoster(String bestand) throws IOException {
        BufferedReader br = null;
        String line;
        String[] data;

        int nbrType1 = 0;
        int nbrType2 = 0;
        int[][] roster = null;

        try {
            br = new BufferedReader(new FileReader(bestand));

            nbrType1 = Integer.parseInt(br.readLine().trim());
            nbrType2 = Integer.parseInt(br.readLine().trim());

            roster = new int[nbrType1 + nbrType2][NurseScheduling.NBR_DAYS_IN_MONTH]; //per nurse per day requirements
            for (int i = 0; i < roster.length; i++) { //for every nurse
                line = br.readLine();
                if (line != null) {
                    data = line.split("\t");
                    for (int j = 0; j < roster[i].length; j++) { //for specific nurse fill in data
                        roster[i][j] = Integer.parseInt(data[j]); //TODO: vervang door shift equivalent
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getClass().getSimpleName() + ":" + e.getMessage());
        } finally {
            if (br != null) {
                br.close();
            }
        }

        return new CyclicRoster(nbrType1, nbrType2, roster);
    }

    public static PersonnelCharacteristics readPersonnelCharacteristics(String bestand) throws IOException {
        ArrayList<Nurse> pdlist = new ArrayList<>();

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(bestand));
            String line;

            while ((line = br.readLine()) != null) { //whole line of text
                String[] info = line.split("\t");
                if (info[0].length() > 0) {
                    int[][] values = new int[NurseScheduling.NBR_DAYS_IN_MONTH][NurseScheduling.NBR_PREFERENCE_SHIFTS]; //ex. per day 3 shifts = [28][3]
                    for (int i = 0; i < NurseScheduling.NBR_DAYS_IN_MONTH; i++) {
                        for (int j = 0; j < NurseScheduling.NBR_PREFERENCE_SHIFTS; j++) {
                            values[i][j] = Integer.parseInt(info[(i * NurseScheduling.NBR_PREFERENCE_SHIFTS + j) + 1]); //+1 because [0] is nurseCode
                        }
                    }

                    Nurse pd = new Nurse(info[0],
                            values,
                            Double.parseDouble(info[info.length - 2].replace(',', '.')),
                            NurseType.values()[Integer.parseInt(info[info.length - 1]) - 1]);
                    pdlist.add(pd);
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getClass().getSimpleName() + ":" + e.getMessage());
        } finally {
            if (br != null) {
                br.close();
            }
        }

        return new PersonnelCharacteristics(pdlist);
    }

    public static MonthlyRosterRules readMonthlyRosterRules(String bestand) throws IOException {
        int[] data = null;
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(bestand));

            String line;

            while ((line = br.readLine()) != null) { //whole line of text
                String[] info = line.split("\t"); //fill String info with data
                data = new int[info.length];
                for (int i = 0; i < ((info.length) - 2); i++) {
                    data[i] = Integer.parseInt(info[i]);
                }
                if (info[(info.length) - 1].equalsIgnoreCase("yes")) {
                    data[(info.length) - 1] = 1;
                } else {
                    data[(info.length) - 1] = 0;
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getClass().getSimpleName() + ":" + e.getMessage());
        } finally {
            if (br != null) {
                br.close();
            }
        }

        return new MonthlyRosterRules(data);
    }
    
    public static void writeSchedule(NurseSchedule nurseSchedule) throws IOException {
        BufferedWriter writer = null;
        String scenarioName = nurseSchedule.getScenario().getScenarioName();
        String dep = "dpt_" + nurseSchedule.getDepartment();
        String filename = scenarioName + "_" + dep + "_" + nurseSchedule.getName() + ".csv";
        String info = nurseSchedule.toString();
        
        //C:\Users\Sandra\Documents\NetBeansProjects\Nurse Schedule V1.10\output_files\Scenario1\dpt_A\caseD
        String wayOfMakingSchedules = nurseSchedule.getName().substring(nurseSchedule.getName().lastIndexOf("_") + 1);
        
        try {
            writer = new BufferedWriter(new FileWriter("./output_files/" + scenarioName + "/" + dep + "/" 
                                                        + wayOfMakingSchedules + "/" + filename));
            writer.write(info);
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getClass().getSimpleName() + ":" + e.getMessage());
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
    
    public static void writeSchedule(String folder, NurseSchedule nurseSchedule) throws IOException {
        char department = nurseSchedule.getDepartment();
        String scenarioName = nurseSchedule.getScenario().getScenarioName();
        
        BufferedWriter writer = null;
        String info = nurseSchedule.toString();
        String filename = scenarioName + "_dpt_" + department + "_" + nurseSchedule.getName() + ".csv";
        
        try {
            writer = new BufferedWriter(new FileWriter(folder + filename));
            writer.write(info);
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getClass().getSimpleName() + ":" + e.getMessage());
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
    
}
