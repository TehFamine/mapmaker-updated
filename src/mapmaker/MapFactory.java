package mapmaker;

import java.util.*;
import javax.swing.*;

import util.*;

public class MapFactory {

  public static final Object
    type_DescRoom = "DescRoom",
    type_VirtualRoom = "VirtualRoom";

  public Room createRoom(AreaMap map, Object roomType) {
    if (roomType == type_VirtualRoom)
      return new VirtualRoom(map);
    if (roomType == type_DescRoom)
      return new DescRoom(map);
    return null;
  } // createRoom

  public Link createLink(AreaMap map, 
			 Room room1, int exit1, 
			 Room room2, int exit2) {
    return new Link(map, room1, exit1, room2, exit2);
  } // createLink

  public Link createLink(AreaMap map, Link orgLink) {
    return new Link(map, orgLink);
  } // createLink

  /** sets up a working MVC pattern and returns a MapMVC
   * object containing its elements;
   */
  public static ExtendedMapMVC createMapMVC() {
    Observable obs = new MyObservable();
    CMap map = new CMap(20, 15, obs);
    MapEventHandler controller = new SimpleHandler(map);
    MapGraphics view = new MapGraphics(map, controller);
    JMap Jview = new JMap(obs, view);
    JComponent mapViewJStyle = new JScrollPane(Jview);
    DescViewer descViewer = new DescViewer(map);
    obs.addObserver(descViewer);
    // put mapView and descView into one Component
    JComponent viewJStyle = new JPanel();
    // add map view and description view from top to bottom
    viewJStyle.setLayout(new BoxLayout(viewJStyle, BoxLayout.Y_AXIS));
    viewJStyle.add(mapViewJStyle);
    viewJStyle.add(new JScrollPane(descViewer));
    // add listener for hotkeys
    Jview.addKeyListener(new HotkeyListener(descViewer));
    return new ExtendedMapMVC(map, viewJStyle, controller, view);
  } // createMapMCV

} // MapFactory







