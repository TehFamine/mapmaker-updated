package mapmaker;

/** links two rooms in a map
 */
public class Link {

  private final Room[] rooms;
  private final AreaMap map;

  /** Automatically links room1 and room2 at the given exits */
  public Link(AreaMap map, Room room1, int exit1, Room room2, int exit2) {
    if (map == null || room1 == null || room2 == null) {
      throw new IllegalArgumentException("Map and rooms cannot be null");
    }
    this.map = map;
    this.rooms = new Room[] { room1, room2 };
    notifyRooms(exit1, exit2);
  } // Link

  /** Initializes as a deep copy of orgLink */
  public Link(AreaMap map, Link orgLink) {
    if (map == null || orgLink == null) {
      throw new IllegalArgumentException("Map and original Link cannot be null");
    }
    this.map = map;
    this.rooms = new Room[2];
    Room[] orgRooms = orgLink.getRooms();
    int[] exits = new int[2];
    for (int i = 0; i < 2; i++) {
      Room mappedRoom = map.getRoom(orgRooms[i].getPos());
      if (mappedRoom == null) {
        throw new IllegalStateException("Room mapping not found in the new map");
      }
      rooms[i] = mappedRoom;
      exits[i] = orgRooms[i].getExitDir(orgLink);
    }
    notifyRooms(exits[0], exits[1]);
  } // Link

  public Room[] getRooms() {
    return rooms.clone();  // Return a safe copy
  } // getRooms

  private void notifyRooms(int exit1, int exit2) {
    rooms[0].setLink(this, exit1);
    rooms[1].setLink(this, exit2);
  } // notifyRooms

  public void suicide() {
    unlinkRooms();
    map.removeLink(this);
  } // suicide

  private void unlinkRooms() {
    for (Room room : rooms) {
      room.unlink(this);
    }
  } // unlinkRooms

  public Room opposite(Room room) {
    if (rooms[0] == room) return rooms[1];
    if (rooms[1] == room) return rooms[0];
    throw new IllegalArgumentException("The specified room is not linked by this object");
  }
} // Link



