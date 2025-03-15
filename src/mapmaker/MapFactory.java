package mapmaker;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.Border;

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
    CMap map = new CMap(20, 25, obs);
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

    // Create panel for room name
    JPanel labelBoxPanel = new JPanel();
    labelBoxPanel.setLayout(new BoxLayout(labelBoxPanel, BoxLayout.LINE_AXIS));

    // Label
    JLabel mapLabel = new JLabel("Room Name");

    // Box (e.g., a JTextField or JComboBox)
    JTextField textField = new JTextField();
    // Adjust sizes
    textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24)); // Allow width expansion
    textField.setPreferredSize(new Dimension(200, 24)); // Adjust text field appropriately


    // Add Label and Box to the panel
    labelBoxPanel.add(mapLabel);
    labelBoxPanel.add(Box.createVerticalStrut(25)); // Add spacing
    labelBoxPanel.add(Box.createHorizontalStrut(5)); // Add spacing
    Border blackBorder = BorderFactory.createLineBorder(Color.BLACK);
    textField.setBorder(blackBorder);
    labelBoxPanel.add(textField);

    // Add this panel to the view
    viewJStyle.add(labelBoxPanel, BorderLayout.WEST);

    // Create panel for room description
    JPanel descLabelBoxPanel = new JPanel();
    descLabelBoxPanel.setLayout(new BoxLayout(descLabelBoxPanel, BoxLayout.LINE_AXIS));

    // Label
    JLabel descLabel = new JLabel("Description ");

    // Box (e.g., a JTextField or JComboBox)
    JTextArea descTextField = new JTextArea();
    descTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE)); // Allow width expansion
    descTextField.setPreferredSize(new Dimension(200, 50)); // Adjust text field appropriately

    // Add Label and Box to the panel
    descLabelBoxPanel.add(descLabel);
    descLabelBoxPanel.add(Box.createVerticalStrut(25)); // Add spacing
    descLabelBoxPanel.add(Box.createHorizontalStrut(6)); // Add spacing
    descViewer.setBorder(blackBorder);
    descLabelBoxPanel.add(descViewer);

    // Add this panel to the view
    viewJStyle.add(descLabelBoxPanel, BorderLayout.WEST);


    //viewJStyle.add(new JScrollPane(descViewer));
    // add listener for hotkeys
    Jview.addKeyListener(new HotkeyListener(descViewer));
    return new ExtendedMapMVC(map, viewJStyle, controller, view);
  } // createMapMCV

} // MapFactory







