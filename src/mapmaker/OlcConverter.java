package mapmaker;

import util.*;

/** used to convert a map into a OLC file
 */
public class OlcConverter 
  implements MapConverter {
  
  private static final String[] olcDirNames = 
  { "north", "east", "south", "west",
    "northeast", "southeast", "southwest", "northwest", "up", "down"};

  private static final String[] olcSectorNames =
  { "inside", "city", "field", "forest", "hills", "mountain",
    "shallow", "deep", "underwater", "air", "desert" };

  private static final String[] olcFlagNames =
  { "dark", "no_mob", "indoors", "private", "safe", "solitary",
    "pet_shop", "no_recall" };
  
  protected String olcDirName(int dir) {
    return olcDirNames[dir];
  } // olcDirName

  protected String olcSectorName(int sector) {
    return olcSectorNames[sector];
  } // olcSectorName

  protected String olcFlagName(int flag) {
    return olcFlagNames[flag];
  } // olcFlagName

  /** converts the given map into an array of strings that
   * represents the map in Rom format
   */
  public Text mapToText(AreaMap map, int startVnum, String fileName) {
    Room[] rooms = map.getRooms();
    NoEqualValuesMap<Room,Integer> roomVnums = 
      RomConverterTool.assignVnums(rooms, startVnum);
    Text out = new Text();
    
    // create rooms
      for (Room value : rooms)
          if (!(value instanceof VirtualRoom))
              out.append("redit create " + roomVnums.get(value));

    // for each room set the data
      for (Room room : rooms) {
          if (!(room instanceof VirtualRoom)) {
              // go to room
              out.append("goto " + roomVnums.get(room));
              // create links
              for (int dir = 0; dir < Dir.DIRNR; dir++)
                  if (room.exitLinked(dir) && !room.exitBlocked(dir)) {
                      Room exitRoom =
                              RomConverterTool.getExitRoom(room, room.getLink(dir));
                      if (exitRoom != null)
                          out.append(olcDirName(dir) + " room " + roomVnums.get(exitRoom));
                  }
              // set room name
              String roomName = RomConverterTool.getRoomName(room);
              if (!roomName.isEmpty())
                  out.append("name " + roomName);
              // set room description
              Text roomDesc = RomConverterTool.getRoomDesc(room);
              if (roomDesc.getLineCount() > 0) {
                  out.append("desc");
                  out.append(roomDesc);
                  out.append(".q");
              }
              // set room sector
              int sector = RomConverterTool.getRoomSector(room);
              if (sector != 0)
                  out.append("sector " + olcSectorName(sector));
              // set room flags
              boolean[] flags = RomConverterTool.getRoomFlags(room);
              for (int flag = 0; flag < olcFlagNames.length; flag++)
                  if (flags[flag])
                      out.append("room " + olcFlagName(flag));
          }
      }

    // save and exit olc
    out.append("asave area");
    out.append("done");

    return out;
  } // mapToText

} // OlcConverter


