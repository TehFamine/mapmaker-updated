package mapmaker;

import java.awt.*;
import java.util.*;

import mapmaker.mapcmd.*;
import util.*;

/** part of a Map
 */
public class CRoom 
  implements Room {

  // elements of exits may never be null
  protected Exit[] exits = new Exit[Dir.DIRNR];
  protected AreaMap map;
  protected boolean marked = false;
  protected NamedColor color = null;

  /** @param map must be map containing room
   */
  public CRoom(AreaMap map) {
    this.map = map;
    for (int i = 0; i < exits.length; i++)
      exits[i] = new Exit();
  } // CRoom

  /** executes the given command from the mapmaker.mapcmd package;
   * the following commands are supported:
   * RCmdSetExitBlocked,
   * RCmdKillLink,
   * RCmdSwapExits;
   * should only be delegated by Map object
   */
  public void execute(RoomCommand cmd) {
    if (cmd instanceof RCmdSetExitBlocked) {
      setExitBlocked(((RCmdSetExitBlocked)cmd).dir,
		     ((RCmdSetExitBlocked)cmd).blocked);
      return;
    }
    if (cmd instanceof RCmdKillLink) {
      killLink(((RCmdKillLink)cmd).dir);
      return;
    }
    if (cmd instanceof RCmdSwapExits) {
      swapExits(((RCmdSwapExits)cmd).dir1,
		((RCmdSwapExits)cmd).dir2);
      return;
    }
    if (cmd instanceof RCmdSetMarked) {
      setMarked(((RCmdSetMarked)cmd).marked);
      return;
    }
    if (cmd instanceof RCmdSetColor) {
      setColor(((RCmdSetColor)cmd).color);
    }
  } // execute

  /** creates a deep-copy of the given room's data (except links!)
   * and replaces its own data with it
   * @param orgRoom the room to be copied
   */
  public void deepCopyFrom(Room orgRoom) {
    for (int dir = 0; dir < Dir.DIRNR; dir++)
      exits[dir] = new Exit(orgRoom.exitBlocked(dir));
    setColor(orgRoom.getColor());
  } // deepCopyFrom

  public Room cloneRoom(AreaMap map) {
    Room room = new CRoom(map);
    room.deepCopyFrom(this);
    return room;
  } // cloneRoom

  /** returns the room's position on the map
   */
  public Point getPos() {
    return map.getRoomPos(this);
  } // getPos

  /** should only be called by Map or Link object
   */
  public void setLink(Link link, int dir) {
    if (!exitLinked(dir))
      exits[dir].link = link;
    else
      throw new IllegalArgumentException();
  } // setLink

  public Link getLink(int dir) {
    return exits[dir].link;
  } // getLink

  public int getExitDir(Link link) {
    for (int i = 0; i < Dir.DIRNR; i++)
      if ( link == exits[i].link)
	return i;
    throw new IllegalArgumentException();
  } // getExitDir

  public boolean exitLinked(int dir) {
    return (exits[dir].link != null);
  } // exitLinked

  public boolean exitBlocked(int dir) {
    return exits[dir].blocked;
  } // exitBlocked

  /** sets an exit to be blocked or free
   * @param dir exit to be configured
   */
  protected void setExitBlocked(int dir, boolean blocked) {
    if (exitLinked(dir)) {
      exits[dir].blocked = blocked;
    }
  } // setExitBlocked

  /** swaps the links at the exits given through dir1, dir2
   */
  protected void swapExits(int dir1, int dir2) {
    // shouldn't be called by map
    Exit buf = exits[dir1];
    exits[dir1] = exits[dir2];
    exits[dir2] = buf;
  } // swapExits

  public void unlink(Link link) {
    // sets the corresponding exit to null but does nothing else
    // called by a link when it self-destructs
    exits[getExitDir(link)] = new Exit();
  } // unlink

  /** removes the link at the exit indicated by dir if there
   * is one, otherwise does nothing
   */
  protected void killLink(int dir) {
    if (exitLinked(dir)) {
      exits[dir].link.suicide();
    }
  } // killLink
  
  /** should only be called by Map parent
   */
  public void suicide() {
    for (int i = 0; i < Dir.DIRNR; i++)
      killLink(i);
	map.removeRoom(this);
  } // suicide
  
  /* returns up or down direction if one or both are free;
   * if neither is free, returns -1;
   * used by bestFreeExit;
   */
  private int bestUpDownExitTo(Room target) {
    if (exitLinked(Dir.up) && exitLinked(Dir.down))
      return -1;
    if (exitLinked(Dir.up))
      return Dir.down;
    else if (exitLinked(Dir.down))
      return Dir.up;
    else {
      // now select one of the two, based on their screen position
      Point targetPos = target.getPos();
      Point pos = getPos();
      if (pos.y < targetPos.y ||
	  (pos.y <= targetPos.y && pos.x < targetPos.x)) {
	return Dir.down;
      }
      else
	return Dir.up;
    }
  } // bestUpDownExitTo

  /** returns the exit that points the "best" in the direction
   * of the give room among the free ones;
   * if no free exits returns -1
   */
  public int bestFreeExitTo(Room target) {

    // find smallest free exit
    int bestExit = 0;
    while (bestExit < Dir.DIRNR && exitLinked(bestExit))
      bestExit++;
    if (bestExit >= Dir.DIRNR)
      return -1;
    if (bestExit >= Dir.PLANEDIRNR)
      return bestUpDownExitTo(target);

    // norm how well an exit fits is the scalar product between
    // the wanted direction and the exit direction
    Point thisPos = getPos();
    Point targetPos = target.getPos();
    Point delta = new Point(targetPos.x - thisPos.x,
			    targetPos.y - thisPos.y);
    double[] scalars = new double[Dir.PLANEDIRNR];
    for (int i = 0; i < Dir.PLANEDIRNR; i++)
      scalars[i] = MapMath.dirScalar(delta, Dir.screenDir(i));
    
    // find best free exit
    for (int i = bestExit + 1; i < Dir.PLANEDIRNR; i++)
      if (!exitLinked(i) && scalars[i] > scalars[bestExit])
	bestExit = i;
    
    // if not a 'perfect' direction, return up/down exit
    if (!MapMath.positiveParallel(delta, Dir.screenDir(bestExit))) {
      int bestUpDown = bestUpDownExitTo(target);
      if (bestUpDown != -1)
	return bestUpDown;
    }

    return bestExit;
  } // bestFreeExitTo

  /** return the Room objects linked to the room
   * @param upDownToo if false, does not return rooms linked
   * over 'up' or 'down' exits
   */
  public Set<Node> neighbours(Object upDownToo) {
    // evaluate if up/down connections considered too
    int maxIndex = Dir.DIRNR - 2;
    if (((Boolean)upDownToo).booleanValue())
      maxIndex = Dir.DIRNR;

    Set<Node> neighbours = new HashSet<Node>(maxIndex);
    for (int i = 0; i < maxIndex; i++)
      if (exitLinked(i))
	neighbours.add(getLink(i).opposite(this));
    return neighbours;
  } // neighbours

  public boolean getMarked() {
    return marked;
  } // getMarked

  /** sets the room as marked
   */
  protected void setMarked(boolean marked) {
    this.marked = marked;
  } // setMarked

  public NamedColor getColor() {
    return color;
  }

  protected void setColor(NamedColor color) {
    this.color = color;
  }

} // Room

/** wrappes a link and its accessability
 */
class Exit {

  public Link link;
  public boolean blocked;

  public Exit() {
    this(null, false);
  } // Exit

  /** @param blocked the state of accessability
   */
  public Exit(boolean blocked) {
    this(null, blocked);
  } // Exit

  /** @param link the link to be wrapped
   */
  public Exit(Link link) {
    this(link, false);
  } // Exit

  /** @param link the link to be wrapped
   * @param blocked the state of accessability
   */
  public Exit(Link link, boolean blocked) {
    this.link = link;
    this.blocked = blocked;
  } // Exit

} // Exit
