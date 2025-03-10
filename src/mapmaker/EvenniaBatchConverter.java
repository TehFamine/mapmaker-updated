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
        return null;
    }

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
