package nurse.scheduling;

import java.util.ArrayList;
import static nurse.scheduling.NurseScheduling.NBR_DAYS_IN_MONTH;

/**
 *
 * @author Sandra
 */
public class PersonnelCharacteristics {
        private ArrayList<Nurse> nurses;
        
        public PersonnelCharacteristics(ArrayList<Nurse> nurses) {
            this.nurses = nurses;
        }

    public ArrayList<Nurse> getNurses() {
        return new ArrayList<>(nurses);
    }
      
    public ArrayList<Nurse> getNurses(NurseType type) {
        ArrayList<Nurse> nursesOfType = new ArrayList<>();
        for (Nurse nurse : nurses){
            if (nurse.getNurseType() == type){
                nursesOfType.add(nurse);
            }
        }
        return nursesOfType;
    }

    public void setNurses(ArrayList<Nurse> nurses) {
        this.nurses = nurses;
    }
    
    public void addNurse(Nurse nurse) {
        this.nurses.add(nurse);
    }

    @Override
    public String toString() {
        String s = "[" + this.getClass().getSimpleName() + "]\n";
        
        if (this.nurses.isEmpty()) {
            return s;
        }
        s += "\t";
        for (int i = 0; i < NBR_DAYS_IN_MONTH; i++)
            s += "Day" + (i+1) + "\t \t \t \t \t";
        s += "\n";
        s += "\t";
        
        for(int i = 0; i < NBR_DAYS_IN_MONTH; i++) {
            for (ShiftType shift : ShiftType.values()) {
                s += shift.toString().charAt(0) + "\t";
            }
        }
        s += "\n";
        
        for(Nurse pd : this.nurses) {
            s += pd.toStringPreferenceData();
        }
        return s;
    }     
}
