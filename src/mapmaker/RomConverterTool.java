package mapmaker;
import util.*;

/**
 * a tool for converting a map into other formats (ROM)
 */
public class RomConverterTool {

    public static final String
            CMD_STRING = MapDescParser.CMD_STRING,
            VNUM_CMD = CMD_STRING + "VNUM",
            SECTOR_CMD = CMD_STRING + "SECTOR";

    private static final String[] sectorNames =
            {"INSIDE", "CITY", "FIELD", "FOREST", "HILLS", "MOUNTAINS",
                    "WATER", "DEEP_WATER", "UNDERWATER", "AIR", "DESERT"};

    private static final String[] ROOM_FLAG_CMDS =
            {CMD_STRING + "DARK",
                    CMD_STRING + "NO_MOB",
                    CMD_STRING + "INDOORS",
                    CMD_STRING + "PRIVATE",
                    CMD_STRING + "SAFE",
                    CMD_STRING + "SOLITARY",
                    CMD_STRING + "PET_SHOP",
                    CMD_STRING + "NO_RECALL"};

    /**
     * returns an array of all sector names
     */
    public static String[] getSectorNames() {
        return sectorNames.clone();
    } // getSectornames

    /**
     * returns an array of all room flag commands
     */
    public static String[] getRoomFlagCommands() {
        return (String[]) ROOM_FLAG_CMDS.clone();
    } // getRoomFlagCommands

    /**
     * returns the name of the room
     */
    public static String getRoomName(Room room) {
        String name = "";
        if (room instanceof DescObj)
            name = MapDescParser.get().getName(((DescObj) room).getDesc());
        return name;
    } // getRoomName

    /**
     * returns the description of the room, excluding the room name
     * and and comments or commands, and word-wrapped
     */
    public static Text getRoomDesc(Room room) {
        Text roomDesc = new Text();
        if (room instanceof DescObj) {
            roomDesc = MapDescParser.get().filterDesc(((DescObj) room).getDesc());
            if (roomDesc.getLineCount() > 0)
                roomDesc.removeLine(0);
            else
                return roomDesc;

      /* ROM adds two blank characters at the beginning of a room description;
	 this must be formated correctly */
            if (roomDesc.getLineCount() > 0)
                roomDesc.replaceLine(0, "  " + roomDesc.getLine(0));
            else
                return roomDesc;
            // now wrap it, considering colors..
            WordWrapper wrapper = new WordWrapper(79) {
                protected int charWeight(char ch) {
                    if (ch == '{')
                        return -1;
                    else
                        return 1;
                }
            };
            roomDesc = wrapper.wrap(roomDesc);
            //roomDesc = StringUtil.wordWrapLines(roomDesc, 79);
            roomDesc.replaceLine(0, roomDesc.getLine(0).substring(2));
        }
        return roomDesc;
    } // getRoomDesc

    /**
     * returns the parameters of the first instance of cmd found
     * in the description of room, or null if not found
     */
    public static String getCommand(Room room, String cmd) {
        if (!(room instanceof DescObj))
            return null;
        Text text = ((DescObj) room).getDesc();
        for (int i = 0; i < text.getLineCount(); i++)
            if (text.getLine(i).startsWith(cmd))
                return text.getLine(i).substring(cmd.length()).trim();
        return null;
    } // getCommand

    /**
     * returns the room at the other end of the link, skipping
     * virtual rooms;
     * if no non-vitual room at end of link chain, returns null
     *
     * @param room the room to start the search from
     * @param link the link to start from, must be connected to room
     */
    public static Room getExitRoom(Room room, Link link) {
        Room nextRoom = link.opposite(room);
        if (!(nextRoom instanceof VirtualRoom))
            return nextRoom;
        // now we got a virtual room...
        Link nextLink = ((VirtualRoom) nextRoom).opposite(link);
        if (nextLink == null)
            return null;
        else
            return getExitRoom(nextRoom, nextLink);
    } // getExitRoom

    /**
     * return a Map maping each room to an Integer representing
     * the room's vnum
     *
     * @param startVnum the base for assigning relative vnums
     */
    public static NoEqualValuesMap<Room, Integer> assignVnums(Room[] rooms, int startVnum) {
        // store vnums of rooms in roomVnums with rooms as keys
        // only store rooms that aren't virtual
        NoEqualValuesMap<Room, Integer> roomVnums = new NoEqualValuesMap<>();
        // first, check all rooms for vnum command
        for (Room room : rooms) {
            if (!(room instanceof VirtualRoom)) {
                String param = getCommand(room, VNUM_CMD);
                if (param != null)
                    try {
                        Integer vnum = Integer.parseInt(param) + startVnum;
                        // check if a room was already assigned that vnum
                        if (roomVnums.containsEqual(vnum))
                            throw new Exception(vnum + " double");
                        roomVnums.put(room, vnum);
                    } catch (Exception e) {
                        System.out.println("Error parsing room vnums: " + e);
                    }
            }
        }
        // now add vnums for rest of rooms
        int nextVnum = startVnum;
        for (Room room : rooms)
            if (!roomVnums.containsKey(room) &&
                    !(room instanceof VirtualRoom)) {
                // find free vnum
                while (roomVnums.containsEqual(nextVnum)) { // Auto-boxing, no need for new Integer()
                    nextVnum++;
                }
                roomVnums.put(room, nextVnum++); // Auto-boxing here as well
            }
        return roomVnums;
    } // assignVnums

    /**
     * returns a sector id for the given room
     */
    public static int getRoomSector(Room room) {
        String param = getCommand(room, SECTOR_CMD);
        if (param == null)
            return 0;
        return parseSector(param);
    } // getRoomSector

    /**
     * returns an integer corresponding to the given sector;
     * if sector String invalid, returns 0 as default sector
     */
    private static int parseSector(String sector) {
        // try to parse as int
        try {
            return Integer.parseInt(sector);
        } catch (NumberFormatException e) {
            System.out.println("Error parsing sectors: " + e);
        }

        for (int i = 0; i < sectorNames.length; i++)
            if (sectorNames[i].equals(sector))
                return i;
        System.out.println("warning: unknown sector: " + sector);
        return 0;
    } // parseSector

    /**
     * returns an array containing the flags of the room;
     * array[i] is true exactly when the flag i is set
     */
    public static boolean[] getRoomFlags(Room room) {
        boolean[] flags = new boolean[ROOM_FLAG_CMDS.length];
        for (int i = 0; i < ROOM_FLAG_CMDS.length; i++)
            flags[i] = (getCommand(room, ROOM_FLAG_CMDS[i]) != null);
        return flags;
    } // getRoomFlags

} // RomConverterTool


