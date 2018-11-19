package nurse.scheduling;

/**
 *
 * @author Sandra
 */
public class MonthlyRosterRules {

    private int[] data;

    public MonthlyRosterRules(int[] data) {
        this.data = data;
    }
    
    @Override
    public String toString() {
        String s = "[" + this.getClass().getSimpleName() + "]\n";
        return s;
    }    
}
