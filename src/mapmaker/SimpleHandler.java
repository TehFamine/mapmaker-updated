package mapmaker;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import mapmaker.mapcmd.*;
import util.*;

/** the Controller of mapmaker's MVC pattern
 */
public class SimpleHandler extends MapEventHandler {  

  public static final Object
    STATE_DRAG = "STATE_DRAG",
    STATE_LINK = "STATE_LINK";

  AreaMap map;
  NodeCollector collector;
  StateController
          dragState = new ConstStateController(true),
          linkState = dragState;

  /** @param map the map to be controlled
   */
  public SimpleHandler(AreaMap map) {
    this.map = map;
    collector = new NodeCollector();
  } // Simplehandler

  /** @param pos the map position of the room that was clicked upon
   */
  public void roomClicked(MouseEvent e, Point pos) {
    if (!e.isMetaDown()) { // left click
      if (!e.isControlDown()) {
	if (!e.isShiftDown())
	  map.execute(new CmdNewRoom(pos, MapFactory.type_DescRoom));
	else {
	  // mark/unmark the room clicked on
	  Room room = map.getRoom(pos);
	  if (room == null)
	    return;
	  map.execute(new RCmdSetMarked(room.getPos(), !room.getMarked()));
	}
      }
    }
    else {
      if (e.isControlDown()) {
	// copy room description
	Room source = map.getSelected();
	Room target = map.getRoom(pos);
	if (target != null && source != target &&
	    target instanceof DescObj) {
	  if (source == null || !(source instanceof DescObj))
	    ((DescObj)target).setDesc(null);
	  else
	    ((DescObj)target).setDesc(((DescObj)source).getDesc());
	  map.notifyOfChange(MapEvent.ChangeDesc);
	}
	return;
      }
      // change select state
      Room room = map.getRoom(pos);
      if (room == null || room == map.getSelected())
	map.execute(new CmdUnselectRoom());
      else
	map.execute(new CmdSelectRoom(pos));
    }
  } // roomClicked

  /** @param pressed the map position of the room where the
   * mousekey was pressed down
   * @param e where it was released
   */
  public void roomDragged(MouseEvent e, Point pressed, Point released) {
    if (!e.isMetaDown()) { // left click
      if (e.isControlDown()) {
	// move rooms
	Room root = map.getRoom(pressed);
	if (root == null)
	  return;
	Point dir = new Point(released.x - pressed.x, 
			      released.y - pressed.y);
	Set<Room> rooms;
	if (root.getMarked()) {
	  // move all marked rooms
	  Room[] roomArr = MapUtil.getMarkedRooms(map);
	  rooms = new HashSet<Room>();
	  for (int i = 0; i < roomArr.length; i++)
	    rooms.add(roomArr[i]);
	}
	else {
      // move all rooms connected
      rooms = new HashSet<Room>();
      for (Node room : collector.getNodeCollection(root, dragState.state()))
        rooms.add((Room)room);
	}
	if (MapUtil.isMoveable(map, rooms, dir))
	  MapUtil.moveRooms(map, rooms, dir);
	return;
      }
      if (e.isShiftDown()) {
	// mark all rooms in drag rectangel
	// determin drag rectangel
	int minX = Math.min(pressed.x, released.x);
	int maxX = Math.max(pressed.x, released.x);
	int minY = Math.min(pressed.y, released.y);
	int maxY = Math.max(pressed.y, released.y);
	// create and execute mapcommand to mark all rooms
	CompositeCommand markCmd = new CompositeCommand();
	for (int x = minX; x <= maxX; x++)
	  for (int y = minY; y <= maxY; y++)
	    markCmd.add(new RCmdSetMarked(new Point(x, y), true));
	map.execute(markCmd);
	return;
      }
      // only swap if released Point is empty
      if (map.getRoom(released) == null)
	map.execute(new CmdSwapRooms(pressed, released));
    }
    else { 
      // link rooms
      if (((Boolean)linkState.state()).booleanValue()) {
	CompositeCommand cc = new CompositeCommand();
	Point[] meanRooms = MapUtil.meanRooms(map, pressed, released);
	for (int i = 1; i < meanRooms.length; i++)
	  cc.add(new CmdLinkRooms(meanRooms[i - 1], meanRooms[i]));
	map.execute(cc);
      }
      else
	map.execute(new CmdLinkRooms(pressed, released));
    }
  } // roomDragged

  /** @param room the Room that contains the exit that was clicked upon
   * @param exit the direction of the exit that was clicked upon
   */
  public void linkClicked(MouseEvent e, Point room, int exit) {
    if (!e.isMetaDown()) { // left clicked
      boolean blockState = map.getRoom(room).exitBlocked(exit);
      map.execute(new RCmdSetExitBlocked(room, exit, !blockState));
    }
  } // linkClicked

  /** @param room the room containing the exits that were dragged
   * @param pressed the direction of the exit where the dragging started
   * @param released dito
   */
  public void linkDragged(MouseEvent e, Point room, 
			  int pressed, int released) {
    if (!e.isMetaDown()) // left click
      map.execute(new RCmdSwapExits(room, pressed, released));
  } // linkDragged

  /** @param pos the map position of the room that was below the
   * mouse pointer when the key was typed
   */
  public void keyTypedOnRoom(KeyEvent e, Point pos) {
    if (e.getKeyCode() == KeyEvent.VK_DELETE &&
	!(map.getSelected() != null &&
	  map.getSelected().getPos().equals(pos))) {
      map.execute(new CmdKillRoom(pos));
      return;
    }
    if (e.getKeyCode() == KeyEvent.VK_V) {
      map.execute(new CmdNewRoom(pos, MapFactory.type_VirtualRoom));
      return;
    }
  } // keyTypedOnRoom

  /** @param room the Room that contains the exit that was below the
   * mouse pointer when the key was typed
   * @param exit the direction of the exit below the mouse
   */
  public void keyTypedOnLink(KeyEvent e, Point room, int exit) {
    if (e.getKeyCode() == KeyEvent.VK_DELETE)
      map.execute(new RCmdKillLink(room, exit));
  } // keyTypedOnLink
  
  /** adds a new StateController that the SimpleHandler can use
   * to determin its state;
   * @param index = STATE_DRAG: state will be used to determin 
   * wether up/down connections should be dragged or not
   * @param index = STATE_LINK: state will be used to determin
   * if mean rooms should be connected too when linking rooms
   * @param state must be of type Boolean
   */
  public void addStateController(StateController state,
					  Object index) {
    if (index == STATE_DRAG)
      dragState = state;
    else if (index == STATE_LINK)
      linkState = state;
    else
      throw new IllegalArgumentException("unknown state index");
  } // addStateController

} // SimpleHandler
