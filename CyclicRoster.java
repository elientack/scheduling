package nurse.scheduling;

/**
 *
 * @author Sandra
 */
public class CyclicRoster {

    private int nbrType1;
    private int nbrType2;
    private int[][] roster;
    private int nbrOfShifts;

    public CyclicRoster(int nbrType1, int nbrType2, int[][] roster) {
        this.nbrType1 = nbrType1;
        this.nbrType2 = nbrType2;
        this.roster = roster;
    }

    public int getNbrType1() {
        return nbrType1;
    }

    public int getNbrType2() {
        return nbrType2;
    }

    public int[][] getRoster() {
        return roster;
    }

    public int getNbrOfShifts() {
        return nbrOfShifts;
    }
    
    

    @Override
    public String toString() {
        String s = "[" + this.getClass().getSimpleName() + "]\n";

        if (roster != null) {
            for (int[] rij : roster) {
                for (int i : rij) {
                    s += i + " ";
                }
                s += "\n";
            }
        }
        return s;
    }
}
