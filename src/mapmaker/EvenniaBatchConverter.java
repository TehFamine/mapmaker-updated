package mapmaker;
import util.*;

public class EvenniaBatchConverter
        implements MapConverter {

    private static final String[] evDirNames =
            { "north", "east", "south", "west",
                    "northeast", "southeast", "southwest", "northwest", "up", "down"};

    private static final String[] evShortDirNames =
            { "n", "e", "s", "w", "ne", "se", "sw", "nw", "u", "d"};



    protected String evDirName(int dir) {
        return evDirNames[dir];
    } // evDirName

    protected String evShortDirName(int dir) {
        return evShortDirNames[dir];
    } // evShortDirName

    public Text mapToText(AreaMap map, int startVnum, String fileName) {
        Room[] rooms = map.getRooms();

        // Define room names as vnums (numbers) to make room names unique
        NoEqualValuesMap<Room,Integer> roomVnums =
                RomConverterTool.assignVnums(rooms, startVnum);
        Text out = new Text();

        // create rooms
        for (Room value : rooms)
            if (!(value instanceof VirtualRoom))
                // Dig to vnum number to make it easier to link to in Evennia
                out.append("dig " + roomVnums.get(value));

        // for each room set the data
        for (Room room : rooms) {
            if (!(room instanceof VirtualRoom)) {
                // go to room
                out.append("teleport " + roomVnums.get(room));
                // create links
                for (int dir = 0; dir < Dir.DIRNR; dir++)
                    if (room.exitLinked(dir) && !room.exitBlocked(dir)) {
                        Room exitRoom =
                                RomConverterTool.getExitRoom(room, room.getLink(dir));
                        String direction = evDirName(dir);
                        String shortDir = evShortDirName(dir);
                        String roomExitNumber = roomVnums.get(exitRoom).toString();

                        if (exitRoom != null) {
                            String openCmd = String.format("open %s (%s);%s;%s = %s",
                                    roomExitNumber, direction, shortDir, direction, roomExitNumber);
                            String renameCmd = String.format("name %s (%s) = %s", roomExitNumber, direction, direction);

                            out.append(openCmd);
                            out.append(renameCmd);
                        }
                        else {
                            String roomNumber = roomVnums.get(room).toString();
                            String openCmd = String.format("open %s (%s);%s;%s = %s",
                                    roomNumber, direction, shortDir, direction, roomNumber);
                            String renameCmd = String.format("name %s (%s) = %s", roomNumber, direction, direction);

                            out.append(openCmd);
                            out.append(renameCmd);
                        }
                    }
                // set room name
                String roomName = RomConverterTool.getRoomName(room);
                String renameCmd = String.format("name %s = %s", roomVnums.get(room), roomName);
                if (!roomName.isEmpty())
                    out.append(renameCmd);
            }
        }
        return out;
    } // mapToText

} // EvenniaBatchConverter
