package mapmaker;

import java.awt.*;
import java.awt.event.*;
import util.*;

/** the View in mapmaker's MVC pattern
 */
public class MapGraphics implements MapViewer {

  AreaMap map;
  MapEventHandler meh;
  Graphics g; // stores current Graphics objects to draw on

  Rectangle mapBounds; // stores the bounds of g in room coordinates

  int paintSize = 4;
  // must never be null
  StateController virtualState =
          new ConstStateController(Boolean.FALSE);

    /** @param map the Model
   * @param meh the Controller
   */
  public MapGraphics(AreaMap map, MapEventHandler meh) {
    this.map = map;
    this.meh = meh;
  } // MapGraphics
  
  /** sets the (abstract) size in which the map will be painted
   */
  public void setPaintSize(int paintSize) {
    if (paintSize < 1)
      paintSize = 1;
    this.paintSize = paintSize;
    // inform map that something changed, although the change is not
    // within the map data ..
    map.notifyOfChange(MapEvent.ChangeView);
  } // setPaintSize

    public void setVirtualStateController(StateController state) {
        if (state == null) {
            throw new IllegalArgumentException("StateController must not be null");
        }
        virtualState = state;
    } // setVirtualStateController

  public int getPaintSize() {
    return paintSize;
  } // getPaintSize

  public void paint(Graphics g) {
    // set current Graphics object
    this.g = g;
    // only draw if rooms and links lie within the visible area
    // translate visible area into map coordinates
    Rectangle bounds = g.getClipBounds();
    if (bounds == null)
      mapBounds = new Rectangle(map.getSize());
    else {
      int sizePerRoom = sizePerRoom();
      int mapMinX = bounds.x / sizePerRoom;
      int mapMinY = bounds.y / sizePerRoom;
      // mapMaxX and mapMaxY are exclusive
      int mapMaxX = (bounds.x + bounds.width) / sizePerRoom + 1;
      int mapMaxY = (bounds.y + bounds.height) / sizePerRoom + 1;
      mapBounds = 
	new Rectangle(mapMinX, mapMinY, mapMaxX - mapMinX, mapMaxY - mapMinY);
    }
    // paint rooms
    Room[] rooms = map.getRooms();
    for (int i = 0; i < rooms.length; i++)
      paintRoom(rooms[i]);
    // paint links
    Link[] links = map.getLinks();
    for (int i = 0; i < links.length; i++)
      paintLink(links[i]);
    // paint selected room if there is one
    Room selected = map.getSelected();
    if (selected != null)
      paintSelectedRoom(selected);
  } // paint

  int roomSquareSize() {
    // returns the size of the square that holds a room
    // without border
    return 3 + 2 * paintSize;
  } // room SquareSize

  public int sizePerRoom() {
    // returns the size of the square that holds a room
    // including its border
    return 3 * roomSquareSize();
  } // sizePerRoom

  public Point roomPaintPos(Room room) {
    // returns starting opsition for room painting
    int sizePerRoom = sizePerRoom();
    Point pos = room.getPos();
    return new Point(pos.x * sizePerRoom, pos.y * sizePerRoom);
  } // roomPaintPos

  Point squarePaintPos(Room room) {
    // returns position where to start painting of rooms outlines
    int sizePerRoom = sizePerRoom();
    int borderSize = (sizePerRoom - roomSquareSize()) / 2;
    Point pos = roomPaintPos(room);
    return new Point(pos.x + borderSize, pos.y + borderSize);
  } // squarePaintPos

  Point centerPaintPos(Room room) {
    // returns center position of room for painting
    Point pos = roomPaintPos(room);
    int add = (sizePerRoom() - 1) / 2;
    return new Point(pos.x + add, pos.y + add);
  } // centerPaintPos

  int exitPaintSize() {
    return 1 + paintSize/2;
  } // exitPaintSize

  int centerToEdge() {
    // returns the distance of the edge from center for enlarged room
    return (sizePerRoom() - 1) / 2 - exitPaintSize();
  } // centerToEdge

  Point linkPaintPos(Room room, int dir) {
    // returns position where a link at given direction would start
    int halfSize = (roomSquareSize() - 1) / 2;
    Point center = centerPaintPos(room);
    // check for up/down - special position
    int upDownSize = 1 + paintSize / 3;
    if (dir == Dir.up)
      return new Point(center.x, center.y - upDownSize);
    if (dir == Dir.down)
      return new Point(center.x, center.y + upDownSize);
    // now calculate 'normal' directions
    Point screenDir = Dir.screenDir(dir);
    int moveX = screenDir.x * halfSize;
    int moveY = screenDir.y * halfSize;
    return new Point(center.x + moveX, center.y + moveY);
  } // linkpaintPos

  /** draws the edges of the specified rectangel;
   * used only by paintRoom
   */
  private void drawEdges(int x, int y, int dx, int dy) {
    int l = 1; // length
    g.drawLine(x, y, x + l, y);
    g.drawLine(x, y, x, y + l);

    g.drawLine(x + dx, y, x + dx - l, y);
    g.drawLine(x + dx, y, x + dx, y + l);

    g.drawLine(x, y + dy, x + l, y + dy);
    g.drawLine(x, y + dy, x, y + dy - l);

    g.drawLine(x + dx, y + dy, x + dx - l, y + dy);
    g.drawLine(x + dx, y + dy, x + dx, y + dy - l);
  } // drawEdges

  void paintRoom(Room room) {
    // only draw if within bounds
    if (!mapBounds.contains(room.getPos()))
      return;
    Point start = squarePaintPos(room);
    int roomSize = roomSquareSize();

    Color oldColor = g.getColor();

    // draw marked rooms with red border
    if (room.getMarked()) {
      g.setColor(Color.red);
      g.drawRect(start.x - 1, start.y - 1, roomSize + 1, roomSize + 1);
      g.setColor(oldColor);
    }

    // draw room in its color
    Color roomColor = room.getColor();
    if (roomColor != null)
      g.setColor(roomColor);

    if (room instanceof VirtualRoom) {
      if (((Boolean)virtualState.state()).booleanValue())
	drawEdges(start.x, start.y, roomSize - 1, roomSize - 1);
    }
    else
      g.drawRect(start.x, start.y, roomSize - 1, roomSize - 1);

    g.setColor(oldColor);
  } // paint

  void paintExit(Point pos, boolean linked, boolean blocked) {
    // used only by paintSelectedRoom
    int size = exitPaintSize();
    if (linked)
      if (blocked) {
	g.clearRect(pos.x - size, pos.y - size, 2 * size, 2 * size);
	g.drawRect(pos.x - size, pos.y - size, 2 * size, 2 * size);
	// draw cross
	g.drawLine(pos.x - size, pos.y - size, pos.x + size, pos.y + size);
	g.drawLine(pos.x + size, pos.y - size, pos.x - size, pos.y + size);
      }
      else
	g.fillRect(pos.x - size, pos.y - size, 2 * size, 2 * size);
    else {
      g.clearRect(pos.x - size, pos.y - size, 2 * size, 2 * size);
      g.drawRect(pos.x - size, pos.y - size, 2 * size, 2 * size);
    } // else
  } // paintExit

  Point exitPaintPos(int dir) {
    // returns the screenPosition where the exit indicated by dir
    // should be painted in a selected (enlarged) room,
    // relative to the center of the room
    if (dir < Dir.PLANEDIRNR) {
      Point screenDir = Dir.screenDir(dir);
      int stretch = centerToEdge();
      return new Point(screenDir.x * stretch, screenDir.y * stretch);
    } 
    else
      if (dir == Dir.up)
	return new Point(0, -(1 + exitPaintSize()));
      else
	return new Point(0, 1 + exitPaintSize());
  } // exitPaintPos

  void paintSelectedRoom(Room room) {
    // only draw if within bounds
    if (!mapBounds.contains(room.getPos()))
      return;
    // draws the room enlarged
    Point center = centerPaintPos(room);
    Point start = roomPaintPos(room);
    int size = sizePerRoom();
    int centerToEdge = centerToEdge();

    // draw room in its color
    Color oldColor = g.getColor();
    Color roomColor = room.getColor();
    if (roomColor != null)
      g.setColor(roomColor);

    // clear background
    g.clearRect(start.x, start.y, size - 1, size - 1);
    // draw border
    g.drawRect(center.x - centerToEdge, center.y - centerToEdge,
	       centerToEdge * 2, centerToEdge * 2);
    // draw exits
    for (int i = 0; i < Dir.DIRNR; i++) {
      Point pos = exitPaintPos(i);
      pos.translate(center.x, center.y);
      paintExit(pos, room.exitLinked(i), room.exitBlocked(i));
    }

    g.setColor(oldColor);
  } // paintSelectedRoom

  void paintLink(Link link) {
    Room[] rooms = link.getRooms();
    // only draw if within bounds
    if (AwtUtil.outsideSameSide(mapBounds, 
				rooms[0].getPos(), 
				rooms[1].getPos()))
      return;
    int connectSize = paintSize + 1;
    Point[] connect = new Point[2]; // positions after direction indicators
    int[] exits = new int[2];

    for (int i = 0; i < 2; i++) {
      int exit = exits[i] = rooms[i].getExitDir(link);
      Point start = linkPaintPos(rooms[i], exit);

      // no indicators drawn if up/down direction
      // instead draw little square (unless blocked)
      if (exit == Dir.up || exit == Dir.down) {
	if (!rooms[i].exitBlocked(exit)) {
	  int exitSize = 1 + paintSize / 10;
	  g.fillRect(start.x - exitSize, start.y - exitSize, 
		     exitSize * 2, exitSize * 2);
	}
	connect[i] = start;
      }

      else { // normal directions (not up/down)
	// draw direction indicator
	Point screenDir = Dir.screenDir(exit);
	int connectX = start.x + screenDir.x * connectSize;
	int connectY = start.y + screenDir.y * connectSize;
	// draw links to virtualRooms differently
	if (!(rooms[i] instanceof VirtualRoom))
	  g.drawLine(start.x, start.y, connectX, connectY);
	else {
	  Point center = centerPaintPos(rooms[i]);
	  g.drawLine(center.x, center.y, connectX, connectY);
	}

	// if blocked, draw arrowhead
	if (rooms[i].exitBlocked(exit)) {
	  Point[] arrow = new Point[2];
	  arrow[0] = Dir.screenDir(Dir.turnClockwise(exit));
	  arrow[1] = Dir.screenDir(Dir.turnCounterClockwise(exit));
	  int arrowSize = 1 + paintSize / 3;
	  for (int a = 0; a < 2; a++) {
	    arrow[a].x *= arrowSize;
	    arrow[a].y *= arrowSize;
	    arrow[a].translate(start.x, start.y);
	    g.drawLine(start.x, start.y, arrow[a].x, arrow[a].y);
	  }
	} // if blocked
	
	connect[i] = new Point(connectX, connectY);
      }
    } // for

    // connect the 2 connection points
    connectPoints(connect[0], exits[0], connect[1], exits[1]);
  } // paintLink
  
  /** returns wether a direct connection from p1 to p2 would
   * oppose to exit1 at point p1;
   * used by connectPoints
   */
  boolean validConnect(Point p1, Point p2, int exit1) {
    Point lineDir = new Point(p2.x - p1.x, p2.y - p1.y);
    // order of parameters IS important!
    return !MapMath.opposeQuadrant(Dir.screenDir(exit1), lineDir);
  } // validConnect

  /** connects the given points, respecting the exits they come from
   */
  void connectPoints(Point p1, int exit1, Point p2, int exit2) {
    // if the direct connection would be at an invalid angle,
    // try to use two connection lines
    if (!validConnect(p1, p2, exit1) || 
	!validConnect(p2, p1, exit2)) {
      // try to get a valid connection by connecting with two lines
      Point middle = new Point(p1.x, p2.y);
      // if middle not valid, try other middle point
      if (!validConnect(p1, middle, exit1) ||
	  !validConnect(p2, middle, exit2))
	middle = new Point(p2.x, p1.y);
      // if now valid, draw two lines and return
      if (validConnect(p1, middle, exit1) &&
	  validConnect(p2, middle, exit2)) {
	g.drawLine(p1.x, p1.y, middle.x, middle.y);
	g.drawLine(p2.x, p2.y, middle.x, middle.y);
	return;
      }
    }
    g.drawLine(p1.x, p1.y, p2.x, p2.y);
  } // connectPoints

  Point mapPos(Point screenPos) {
    // returns the map-position of the room that covers 
    // screenPos when painted
    int sizePerRoom = sizePerRoom();
    return new Point(screenPos.x / sizePerRoom, screenPos.y / sizePerRoom);
  } // mapPos

  int exitPos(Point screenPos) {
    // if screenPos is on an exit field of selected room,
    // returns this exit
    // otherwise returns -1
    Room selected = map.getSelected();
    if (selected == null || !(selected.getPos().equals(mapPos(screenPos))))
      return -1;
    // now check if screenPos is on exit
    Point centerPos = centerPaintPos(selected);
    Point relPos = new Point(screenPos.x - centerPos.x, 
			     screenPos.y - centerPos.y);
    int vary = exitPaintSize();
    for (int dir = 0; dir < Dir.DIRNR; dir++) {
      Point paintPos = exitPaintPos(dir);
      if (Math.abs(paintPos.x - relPos.x) <= vary &&
	  Math.abs(paintPos.y - relPos.y) <= vary)
	return dir;
    }
    return -1;
  } // exitPos

  /** adds some listeners to speaker-parameter which delegate
   * events to the Controller given in Constructor
   * @param speaker should be the awt/swing - Component 
   * wrapping the View
   */
  public void addListeners(Component speaker) {
    MouseListener ml =  new MouseAdapter() {
	
	Point mousePressedPos = new Point(0,0);
	
	private boolean inScreen(Point pos) {
	  // returns wether pos lies within the visible range of
	  // the map
	  return AwtUtil.contains(getSize(), pos);
	} // inScreen
	
	public void mouseClicked(MouseEvent e) {
	  if (!inScreen(e.getPoint()))
	    return;
	  Point mapPos = mapPos(e.getPoint());
	  // check if click on link
	  int exit = exitPos(e.getPoint());
	  if (exit != -1)
	    meh.linkClicked(e, mapPos, exit);
	  else
	    meh.roomClicked(e, mapPos);
	} // mouseClicked

	public void mousePressed(MouseEvent e) {
	  if (!inScreen(e.getPoint()))
	    return;
	  mousePressedPos = e.getPoint();
	} // mousePressed

	public void mouseReleased(MouseEvent e) {
	  Point pos = e.getPoint();
	  if (!inScreen(pos) ||
	      !inScreen(mousePressedPos))
	    return;
	  // check if drag from link to link
	  int oldExit = exitPos(mousePressedPos);
	  int newExit = exitPos(pos);
	  if (oldExit != -1 && newExit != -1 && oldExit != newExit) {
	    meh.linkDragged(e, mapPos(pos), oldExit, newExit);
	    return;
	  }
	  // check if drag from different rooms
	  Point mapPos1 = mapPos(mousePressedPos);
	  Point mapPos2 = mapPos(pos);
	  if (!mapPos1.equals(mapPos2))
	    meh.roomDragged(e, mapPos1, mapPos2);
	} // mouseReleased

      };
    
    speaker.addMouseListener(new ClickToleranceFilter(ml));

    // create MouseLocator to get current mouse position
    final MouseLocator mouseLocator = new MouseLocator();
    speaker.addMouseMotionListener(mouseLocator);

    speaker.addKeyListener(new KeyAdapter() {

        MouseLocator mouser = mouseLocator;

        // use keyReleased, not keyTyped, as not all keys
	// generate a keyTyped event!!!
	public void keyReleased(KeyEvent e) {
	  // set pos to current mouse position
	  Point pos = new Point(mouser.getX(), mouser.getY()); 
	  Point mapPos = mapPos(pos);
	  // check if on link
	  int exit = exitPos(pos);
	  if (exit != -1)
	    meh.keyTypedOnLink(e, mapPos, exit);
	  else
	    meh.keyTypedOnRoom(e, mapPos);
	} // keyReleased

      });
  } // addListeners
    
  /** returns the Dimension that the map will occupy (in Pixels)
   */
  public Dimension getSize() {
    int factor = sizePerRoom();
    Dimension roomSize = map.getSize();
    return new Dimension(roomSize.width * factor, 
			 roomSize.height * factor);
  } // getSize

} // MapGraphics


