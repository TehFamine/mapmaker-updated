package mapmaker;

import java.util.Arrays;

public class CDirFilter
  implements DirFilter {

  private boolean[] acceptFlags = new boolean[Dir.DIRNR];

  /** accepts all or nothing, depending on initState
   * @param initState returned by accept unless changed
   * with setAccept
   */
  public CDirFilter(boolean initState) {
      Arrays.fill(acceptFlags, initState);
  } // CDirFilter

  /** initially accepts all
   */
  public CDirFilter() {
    this(true);
  } // CDirFilter

  public boolean accept(int dir) {
    return acceptFlags[dir];
  } // accept

  /** sets wether to accept a given direction
   */
  public void setAccept(int dir, boolean state) {
    acceptFlags[dir] = state;
  } // setAccept

} // CDirFilter
