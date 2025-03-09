package mapmaker;

import java.awt.*;
import java.util.*;

import mapmaker.mapcmd.*;
import util.*;

/** part of a Map
 */
public interface Room 
  extends Node {
  
  /** executes the given RoomCommand; which commands are accepted
   * depends on the implementing class
   */
  public void execute(RoomCommand cmd);

  /** creates a deep-copy of the given room's data (except links!)
   * and replaces its own data with it
   * @param orgRoom the room to be copied
   */
  public void deepCopyFrom(Room orgRoom);

  /** creates a new instance of its class, initializes it by calling
   * deepCopyFrom and returns it
   * @param map the map of the new room
   */
  public Room cloneRoom(AreaMap map);

  /** returns the room's position on the map
   */
  public Point getPos();

  /** returns the link at the given direction
   */
  public Link getLink(int dir);

  /** should only be called by Map or Link object;
   */
  public void setLink(Link link, int dir);

  public boolean exitLinked(int dir);

  public boolean exitBlocked(int dir);

  /** returns the direction of the exit that link is stored at
   * @param link must be one of the links connected to room
   */
  public int getExitDir(Link link);
  
  /** should only be called by a link for self-destruction
   */
  public void unlink(Link link);

  /** should only be called by Map parent
   */
  public void suicide();
  
  /** returns the exit that points the "best" in the direction
   * of the give room among the free ones;
   * if no free exits returns -1
   */
  public int bestFreeExitTo(Room target);

  /** return the Room objects linked to the room
   * @param upDownToo if false, does not return rooms linked
   * over 'up' or 'down' exits
   */
  public Set<Node> neighbours(Object upDownToo);

  /** returns wether the room is marked
   */
  public boolean getMarked();

  public NamedColor getColor();

} // Room

