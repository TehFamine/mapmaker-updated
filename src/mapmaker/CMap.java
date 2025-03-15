package mapmaker;

import java.util.*;
import java.awt.*;
import java.lang.*;

import mapmaker.mapcmd.*;
import util.*;

/** the Model in mapmaker's MVC pattern
 */
public class CMap 
  extends CDescObj
  implements AreaMap {
  
  Room[][] rooms;
  Link[] links;
  Room selected;

  Hashtable<Room,Point> roomPos; // used to get the position of a room
  BufferedObservable bufObs;
  MapFactory factory;
  
  /** @param sizeX the number of rooms stored in width
   * @param sizeY you figure it out ;)
   */
  public CMap (int sizeX, int sizeY, Observable obs) {
    rooms = new Room[sizeX][sizeY];
    roomPos = new Hashtable<Room,Point>(sizeX * sizeY);
    links = new Link[0]; // not links = null !!!
    factory = new MapFactory();
    bufObs = new BufferedObservable(obs);
  } // CMap

  /** deletes all rooms and links and the room description
   */
  protected void clearMap() {
    Dimension size = getSize();
    rooms = new Room[size.width][size.height];
    roomPos.clear();
    links = new Link[0]; // not links = null !!!    
    selected = null;
    setDesc(null);
  } // clearMap

  /** executes the given command from the mapmaker.mapcmd package;
   * the following commands are supported:
   * CompositeCommand,
   * RoomCommand,
   * CmdNewRoom,
   * CmdKillRoom,
   * CmdSelectRoom,
   * CmdUnselectRoom,
   * CmdSwapRooms,
   * CmdLinkRooms,
   * CmdClearMap,
   * CmdSetSize,
   * CmdSetDesc;
   * example: map.execute(new CmdNewRoom(new Point(x, y)));
   * notifies Observers only after the complete command is
   * executed in case of CompositeCommands
   */
  public void execute(MapCommand cmd) {
    bufObs.delayNotify();
    executeWithoutNotify(cmd);
    // make sure some notify occurs
    notifyOfChange(null);
    bufObs.fireNotify();
  } // execute

  protected void executeWithoutNotify(MapCommand cmd) {
    if (cmd instanceof CompositeCommand) {
      Iterator it = ((CompositeCommand)cmd).iterator();
      while (it.hasNext())
	executeWithoutNotify((MapCommand)it.next());
      return;
    }
    if (cmd instanceof RoomCommand) {
       Room target = getRoom(((RoomCommand)cmd).mapPos);
       if (target != null)
	 target.execute((RoomCommand)cmd);
       return;
    }
    if (cmd instanceof CmdNewRoom) {
      CmdNewRoom castCmd = (CmdNewRoom)cmd;
      newRoom(castCmd.pos, castCmd.roomType);
      return;
    }
    if (cmd instanceof CmdKillRoom) {
      killRoom(((CmdKillRoom)cmd).pos);
      return;
    } 
    if (cmd instanceof CmdSelectRoom) {
      selectRoom(((CmdSelectRoom)cmd).pos);
      return;
    } 
    if (cmd instanceof CmdUnselectRoom) {
      unselectRoom();
      return;
    } 
    if (cmd instanceof CmdSwapRooms) {
      swapRooms(((CmdSwapRooms)cmd).pos1,
		 ((CmdSwapRooms)cmd).pos2);
      return;
    } 
    if (cmd instanceof CmdLinkRooms) {
      CmdLinkRooms cmdLink = (CmdLinkRooms)cmd;
      if (cmdLink.autoLink)
	linkRooms(cmdLink.pos1, cmdLink.pos2);
      else {
	Room room1 = getRoom(cmdLink.pos1);
	Room room2 = getRoom(cmdLink.pos2);
	if (room1 == null || room2 == null)
	  return;
	linkRooms(room1, cmdLink.exit1, room2, cmdLink.exit2);
      }
      return;
    }
    if (cmd instanceof CmdClearMap) {
      clearMap();
      return;
    }
    if (cmd instanceof CmdSetSize) {
      setSize(((CmdSetSize)cmd).dim);
      return;
    }
    if (cmd instanceof CmdSetDesc) {
      setDesc(((CmdSetDesc)cmd).desc);
      return;
    }
  } // executeWithoutNotify

  /** makes a deep-copy of the given map's data (Rooms and Links only)
   * and replaces its own data with it
   * @original the map to be copied from
   */
  public void deepCopyFrom(AreaMap original) {
    // copy rooms
    roomPos.clear();
    Dimension dim = original.getSize();
    rooms = new Room[dim.width][dim.height];
    Room[] orgRooms = original.getRooms();
    for (int i = 0; i < orgRooms.length; i++) {
      Point pos = orgRooms[i].getPos();
      Room newRoom = orgRooms[i].cloneRoom(this);
      setRoom(pos, newRoom);
    }
    // copy selected
    Room orgSelected = original.getSelected();
    if (orgSelected == null)
      selected = null;
    else
      selected = getRoom(orgSelected.getPos());
    // copy links - must be AFTER rooms copied
    Link[] orgLinks = original.getLinks();
    links = new Link[orgLinks.length];
    for (int i = 0; i < orgLinks.length; i++) {
      links[i] = factory.createLink(this, orgLinks[i]);
    }
    // copy description
    setDesc((Text)original.getDesc().clone());
    notifyOfChange(MapEvent.ChangeAll);
  } // deepCopyFrom

  public Room getRoom(Point pos) {
    return rooms[pos.x][pos.y];
  } // getRoom

  void setRoom(Point pos, Room room) {
    // sets rooms[pos] to room,
    // automatically validates hashtable

    // check if room already stored -> exception
    if (room != null && roomPos.containsKey(room))
      throw new IllegalStateException();

    if (rooms[pos.x][pos.y] != null) 
      roomPos.remove(rooms[pos.x][pos.y]);
    rooms[pos.x][pos.y] = room;
    if (room != null)
      // use a clone to ensure that memorized object stays constant
      roomPos.put(room, new Point(pos));
  } // setRoom

  /** returns the map position of room
   */
  public Point getRoomPos(Room room) {
    // use Hashtable to speed things up
    // check for invalid hashtable too
    Point pos = (Point)roomPos.get(room);
    // use a clone to ensure that memorized object stays constant
    pos = new Point(pos.x, pos.y);
    return pos;
  } // getRoomPos

  /** updates the Hashtable storing the room positions
   */
  protected void updateRoomPos() {
    Dimension dim = getSize();
    roomPos.clear();
    for (int x = 0; x < dim.width; x++)
      for (int y = 0; y < dim.height; y++)
	if (rooms[x][y] != null)
	  roomPos.put(rooms[x][y], new Point(x, y));
  } // updateRoomPos

  /** creates a new Room if the position is empty
   * @param pos position on map where to create the new Room
   */
  protected void newRoom(Point pos, Object roomType) {
    if (getRoom(pos) == null) {
      setRoom(pos, factory.createRoom(this, roomType));
      notifyOfChange(MapEvent.ChangeRoom);
    }
  } // newRoom

  /** removes the room at give position if there is one
   */
  protected void killRoom(Point pos) {
    if (getRoom(pos) != null) {
      getRoom(pos).suicide();
      notifyOfChange(MapEvent.ChangeRoom);
    }
  } // killRoom

  /** returns the room currently selected
   */
  public Room getSelected() {
    return selected;
  } // getSelected

  protected void selectRoom(Point pos) {
    selected = getRoom(pos);
    notifyOfChange(MapEvent.ChangeSelect);
  } // selectRoom

  protected void unselectRoom() {
    selected = null;
    notifyOfChange(MapEvent.ChangeSelect);
  } // unselectRoom

  /* swaps the rooms at the given positions
   * ('moves' a room if one position is empty)
   */
  protected void swapRooms(Point pos1, Point pos2) {
    // swap the rooms at given positions
    Room room1 = getRoom(pos1);
    Room room2 = getRoom(pos2);
    if (room1 != null || room2 != null) {
      // make sure that no room is stored twice at any give time
      // messes up hashtable
      setRoom(pos2, null);
      setRoom(pos1, room2);
      setRoom(pos2, room1);
      notifyOfChange(MapEvent.ChangeRoom);
    }
  } // swapRooms

  int getLinkIndex(Link link) {
    for (int i = 0; i < links.length; i++)
      if (links[i] == link)
	return i;
    throw new IllegalArgumentException();
  } // getLinkIndex

  /** should only be called by Link objects committing suicide
   */
  public void removeLink(Link link) {
    // removes link from the links list
    // called by link when it self-destructs
    int index = getLinkIndex(link);
    Link[] linkBuf = new Link[links.length - 1];
    for (int i = 0; i < linkBuf.length; i++)
      linkBuf[i] = links[i];
    if (index < linkBuf.length)
      linkBuf[index] = links[linkBuf.length];
    links = linkBuf;
  } // removeLink

  /** should only be called by Room objects committing suicide
   */
  public void removeRoom(Room room) {
    // removes room from the rooms array
    // called by room when it self-destructs
    setRoom(getRoomPos(room), null);
    // check if selected
    if (selected == room)
      selected = null;
  } // removeRoom

  /** notifys the map's Observers of a certain change
   * @param change passed on to Observers
   */
  public void notifyOfChange(Object change) {
    bufObs.notifyObservers(change);
  } // notifyOfChange

  /** creates a link between the given rooms at give exits
   */
  protected void linkRooms(Room room1, int exit1, Room room2, int exit2) {
    if (room1.exitLinked(exit1) || room2.exitLinked(exit2))
      throw new IllegalArgumentException();
    else {
      Link newLink = factory.createLink(this, room1, exit1, room2, exit2);
      Link[] linkBuf = new Link[links.length + 1];
      for (int i = 0; i < links.length; i++)
	linkBuf[i] = links[i];
      linkBuf[links.length] = newLink;
      links = linkBuf;
      notifyOfChange(MapEvent.ChangeLink);
    }
  } // linkRooms

  /** creates a link between the given rooms
   * automatically chooses exits
   */
  protected void linkRooms(Room room1, Room room2) {
    // automatically choose exit1 and exit2
    if (room1 == null || room2 == null)
      return;
    int exit1 = room1.bestFreeExitTo(room2);
    int exit2 = room2.bestFreeExitTo(room1);
    if (exit1 == -1 || exit2 == -1)
      return;
    // try to make exit1 and exit2 oppose each other
    int opp1 = Dir.opposite(exit1);
    int opp2 = Dir.opposite(exit2);
    if (exit1 != opp2) {
      boolean opp1free = !room2.exitLinked(opp1);
      boolean opp2free = !room1.exitLinked(opp2);
      if (opp1free)
	exit2 = opp1;
      else
	if (opp2free)
	  exit1 = opp2;
    }
    linkRooms(room1, exit1, room2, exit2); 
  } // linkRooms

  /** links rooms at given positions if there are any
   */
  protected void linkRooms(Point pos1, Point pos2) {
    linkRooms(getRoom(pos1), getRoom(pos2));
  } // linkRooms

  /** returns an array containing all rooms of the map
   */
  public Room[] getRooms() {
    // count number of rooms
    int roomNr = 0;
    for (int x = 0; x < rooms.length; x++)
      for (int y = 0; y < rooms[x].length; y++)
	if (rooms[x][y] != null)
	  roomNr++;
    // create array containing rooms
    Room[] roomArray = new Room[roomNr];
    int roomArrayPos = 0;
    for (int x = 0; x < rooms.length; x++)
      for (int y = 0; y < rooms[x].length; y++)
	if (rooms[x][y] != null)
	  roomArray[roomArrayPos++] = rooms[x][y];
    return roomArray;
  } // getRooms
    
  /** returns an array containing all link of the map
   */
  public Link[] getLinks() {
    return links;
  } // getLinks

  /** returns the Dimension of the map in terms of room numbers
   */
  public Dimension getSize() {
    int height = 0;
    if (rooms.length > 0)
      height = rooms[0].length;
    return new Dimension(rooms.length, height);
  } // getSize

  /** changes the map's size to given dimensions if that is possible
   * without deleting rows or collums containing rooms; otherwise
   * reduces size to the minimum size possible without performing
   * such deletions
   */
  public void setSize(Dimension dim) {
    Dimension oldDim = getSize();
    // check which columns and rows are empty
    boolean[] rowEmpty = new boolean[oldDim.height];
    boolean[] columnEmpty = new boolean[oldDim.width];
    // check rows
    for (int row = 0; row < rowEmpty.length; row++) {
      rowEmpty[row] = true;
      for (int i = 0; i < oldDim.width; i++)
	if (rooms[i][row] != null)
	  rowEmpty[row] = false;
    }
    // check columns
    for (int column = 0; column < columnEmpty.length; column++) {
      columnEmpty[column] = true;
      for (int i = 0; i < oldDim.height; i++)
	if (rooms[column][i] != null)
	  columnEmpty[column] = false;
    }
    // calculate resize information
    int[] YIndex = Compressor.compress(rowEmpty, dim.height);
    int[] XIndex = Compressor.compress(columnEmpty, dim.width);
    Room[][] newRooms = new Room[XIndex.length][YIndex.length];
    // copy rooms into new room array
    for (int x = 0; x < XIndex.length; x++)
      if (XIndex[x] != -1)
	for (int y = 0; y < YIndex.length; y++)
	  if (YIndex[y] != -1)
	    newRooms[x][y] = rooms[XIndex[x]][YIndex[y]];
    // set new data
    rooms = newRooms;
    updateRoomPos();
    notifyOfChange(MapEvent.ChangeAll);
  } // setSize

} // ConcreteMap




