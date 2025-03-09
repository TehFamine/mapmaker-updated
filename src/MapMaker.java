import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;

import mapmaker.*;
import mapmaker.mapcmd.*;
import util.*;

public class MapMaker {

  static FileWrapper loadSaveFile;
  static JFileChooser loadSaveFileChooser;
  static JFileChooser imageChooser;
  static ExtendedMapMVC mapMVC;
  
  /**
   * create file chooser opening in current working directory
   */
  private static JFileChooser createFileChooser() {
    File workingDirectory = new File(System.getProperty("user.dir"));
    JFileChooser chooser = new JFileChooser();
    chooser.setCurrentDirectory(workingDirectory);
    return chooser;
  }

  /** createJMenuBar; must be non-static to allow the action listeners
   * to be inner classes
   */
  JMenuBar createJMenuBar() {
    JMenuBar menuBar = new JMenuBar();
    JMenu menu, submenu;
    JMenuItem menuItem;
    JRadioButtonMenuItem rbMenuItem;

    // create file menu
    menu = new JMenu("File");
    menu.setMnemonic(KeyEvent.VK_F);
    menuBar.add(menu);
    // create menu items
    menuItem = new JMenuItem("New", KeyEvent.VK_N);
    menuItem.addActionListener(new NewMapAction());
    menu.add(menuItem);
    menuItem = new JMenuItem("Open", KeyEvent.VK_O);
    menuItem.addActionListener(new LoadAction());
    menu.add(menuItem);
    menuItem = new JMenuItem("Save", KeyEvent.VK_S);
    menuItem.addActionListener(new SaveAction());
    menuItem.setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));    
    menu.add(menuItem);
    menuItem = new JMenuItem("Save as", KeyEvent.VK_A);
    menuItem.addActionListener(new SaveAsAction());
    menu.add(menuItem);
    menu.addSeparator();
    menuItem = new JMenuItem("Exit", KeyEvent.VK_X);
    menuItem.addActionListener(new ExitAction());
    menuItem.setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.ALT_MASK));    
    menu.add(menuItem);

    // create edit menu
    menu = new JMenu("Edit");
    menu.setMnemonic(KeyEvent.VK_E);
    menuBar.add(menu);
    // create menu items
    menuItem = new JMenuItem("Clear", KeyEvent.VK_C);
    menuItem.addActionListener(new ClearAction());
    menu.add(menuItem);
    menuItem = new JMenuItem("Map Size", KeyEvent.VK_M);
    menuItem.addActionListener(new ResizeMapAction());
    menu.add(menuItem);
    menu.addSeparator();
    menuItem = new JMenuItem("Search Rooms", KeyEvent.VK_S);
    menuItem.addActionListener(new SearchRoomsAction());
    menu.add(menuItem);
    menuItem = new JMenuItem("Unmark Rooms", KeyEvent.VK_U);
    menuItem.addActionListener(new UnmarkRoomsAction());
    menu.add(menuItem);
    // color rooms submenue
    submenu = new JMenu("Color Rooms");
    submenu.setMnemonic(KeyEvent.VK_O);
    menu.add(submenu);
    // add 'clear' color
    menuItem = new JMenuItem("clear");
    menuItem.addActionListener(new ColorRoomsAction(null));
    submenu.add(menuItem);
    // add real colors
    for (int i = 0; i < NamedColor.colors.length; i++) {
      menuItem = new JMenuItem(NamedColor.colors[i].toString());
      menuItem.addActionListener(new ColorRoomsAction(NamedColor.colors[i]));
      submenu.add(menuItem);
    }
    menu.addSeparator();
    menuItem = new JCheckBoxMenuItem("drag up/down", true);
    // inform controller about this checkbox
    StateController dragState =
      new ButtonStateController((JCheckBoxMenuItem)menuItem);
    mapMVC.controller.addStateController(dragState, 
					 SimpleHandler.STATE_DRAG);
    menuItem.setMnemonic(KeyEvent.VK_D);
    menu.add(menuItem);    
    menuItem = new JCheckBoxMenuItem("link mean rooms", true);
    // inform controller about this checkbox
    StateController linkState =
      new ButtonStateController((JCheckBoxMenuItem)menuItem);
    mapMVC.controller.addStateController(linkState,
					 SimpleHandler.STATE_LINK);
    menuItem.setMnemonic(KeyEvent.VK_M);
    menu.add(menuItem);    

    // create view menu
    menu = new JMenu("View");
    menu.setMnemonic(KeyEvent.VK_V);
    menuBar.add(menu);
    // create menu items
    // view size submenu
    submenu = new JMenu("View Size");
    submenu.setMnemonic(KeyEvent.VK_V);
    menu.add(submenu);
    menuItem = new JMenuItem("Custom", KeyEvent.VK_C);
    menuItem.addActionListener(new ViewSizeAction());
    submenu.add(menuItem);
    menuItem = new JMenuItem("Small", KeyEvent.VK_S);
    menuItem.addActionListener(new ViewSizeAction(3));
    submenu.add(menuItem);
    menuItem = new JMenuItem("Medium", KeyEvent.VK_M);
    menuItem.addActionListener(new ViewSizeAction(4));
    submenu.add(menuItem);
    /*
    menuItem = new JMenuItem("Large", KeyEvent.VK_L);
    menuItem.addActionListener(new ViewSizeAction(5));
    submenu.add(menuItem);
    */
    // other view menu items
    menuItem = new JMenuItem("Maximize", KeyEvent.VK_X);
    menuItem.addActionListener(new UpdateViewAction());
    menu.add(menuItem);
    menu.addSeparator();
    menuItem = new JCheckBoxMenuItem("Show Virtual Rooms", true);
    // inform view about this checkbox
    StateController virtualState =
      new ButtonStateController((JCheckBoxMenuItem)menuItem);
    mapMVC.mapGraphics.setVirtualStateController(virtualState);
    menuItem.addActionListener(new MapNotifyAction(mapMVC.model,
						   MapEvent.ChangeView));
    menuItem.setMnemonic(KeyEvent.VK_V);
    menu.add(menuItem);

    // create image menu
    menu = new JMenu("Image");
    menu.setMnemonic(KeyEvent.VK_I);
    menuBar.add(menu);
    // create menu items
    // add radio buttons for right/below legend
    {
      ButtonGroup buttonGroup = new ButtonGroup();
      StateConverter legendController = 
	new StateConverter(new ButtonGroupStateController(buttonGroup));
      rbMenuItem = new JRadioButtonMenuItem("Legend Right of Map");
      // use getModel since ButtonGroup returns only the model
      legendController.convert(rbMenuItem.getModel(), 
			       new Integer(MapImagePainter.LEGEND_RIGHT));
      rbMenuItem.setSelected(true);
      rbMenuItem.setMnemonic(KeyEvent.VK_R);
      buttonGroup.add(rbMenuItem);
      menu.add(rbMenuItem);
      rbMenuItem = new JRadioButtonMenuItem("Legend Below Map");
      legendController.convert(rbMenuItem.getModel(), 
			       new Integer(MapImagePainter.LEGEND_BELOW));
      rbMenuItem.setMnemonic(KeyEvent.VK_B);
      buttonGroup.add(rbMenuItem);
      menu.add(rbMenuItem);
      menu.addSeparator();
      menuItem = new JMenuItem("Save as Image", KeyEvent.VK_S);
      menuItem.addActionListener(new ImageAction(legendController));
      menu.add(menuItem);
      menuItem = new JMenuItem("Preview Image", KeyEvent.VK_P);
      menuItem.addActionListener(new PreviewAction(legendController));
      menu.add(menuItem);
    }

    // create extra menu
    menu = new JMenu("ROM");
    menu.setMnemonic(KeyEvent.VK_R);
    menuBar.add(menu);
    // create menu items
    menuItem = new JMenuItem("Set Sector", KeyEvent.VK_C);
    menuItem.addActionListener(new SetSectorAction());
    menu.add(menuItem);
    menuItem = new JMenuItem("Set Room Flag", KeyEvent.VK_S);
    menuItem.addActionListener(new SetRoomFlagAction());
    menu.add(menuItem);
    menuItem = new JMenuItem("Remove Room Flag", KeyEvent.VK_V);
    menuItem.addActionListener(new RemoveRoomFlagAction());
    menu.add(menuItem);
    menu.addSeparator();
    menuItem = new JMenuItem("Create ROM Area", KeyEvent.VK_R);
    menuItem.addActionListener(
      new MapConvertAction(new RomConverter(), "are"));
    menu.add(menuItem);
    menuItem = new JMenuItem("Create OLC code", KeyEvent.VK_O);
    menuItem.addActionListener(
      new MapConvertAction(new OlcConverter(), "olc"));
    menu.add(menuItem);
    menuItem.addActionListener(
            new MapConvertAction(new EvenniaBatchConverter(), "evbatch"));
    menu.add(menuItem);

    // create help menu
    menu = new JMenu("Help");
    menu.setMnemonic(KeyEvent.VK_H);
    menuBar.add(menu);
    // create menu items
    menuItem = new JMenuItem("General", KeyEvent.VK_G);
    menuItem.addActionListener(new InfoAction("General Help", 
					      "doc/general.help"));
    menu.add(menuItem);
    menuItem = new JMenuItem("Edit", KeyEvent.VK_E);
    menuItem.addActionListener(new InfoAction("Edit Help", 
					      "doc/edit.help"));
    menu.add(menuItem);
    menuItem = new JMenuItem("Menu", KeyEvent.VK_M);
    menuItem.addActionListener(new InfoAction("Menu Help", 
					      "doc/menu.help"));
    menu.add(menuItem);
    menuItem = new JMenuItem("ROM", KeyEvent.VK_R);
    menuItem.addActionListener(new InfoAction("ROM Help", 
					      "doc/rom.help"));
    menu.add(menuItem);
    menu.addSeparator();
    menuItem = new JMenuItem("About", KeyEvent.VK_A);
    menuItem.addActionListener(new AboutAction());
    menu.add(menuItem);
    
    return menuBar;
  } // createJMenuBar

  public static void main(String[] args) {
    JFrame frame = new JFrame();
    // register frame as central frame
    MainFrame.set(frame);
    // ensure proper exit
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
	System.exit(0);
      }
    });

    // set up loadSaveFile
    loadSaveFile = new FileWrapper();
    loadSaveFile.addObserver(new FileTitleHandler(frame, "MapMaker"));
    // set up fileChooser
    loadSaveFileChooser = createFileChooser();
    loadSaveFile.addObserver(new FileChooserSynchronizer(loadSaveFileChooser));
    /* wait for swing 1.0.3
    FileFilter mapFilter = new ExtensionFileFilter("map");
    loadSaveFileChooser.addChoosableFileFilter(mapFilter);
    loadSaveFileChooser.setFileFilter(mapFilter);
    */
    imageChooser = createFileChooser();
    // set up MVC
    mapMVC = MapFactory.createMapMVC();
    JMenuBar menuBar = new MapMaker().createJMenuBar();
    frame.setJMenuBar(menuBar);
    frame.getContentPane().add(mapMVC.view, BorderLayout.CENTER);
    frame.pack();
    frame.show();
  } // main

  /** saves the map to the file specified in loadSaveFile;
   * does no error checking
   */
  static void saveMap() {
    Text output = new MapParser().mapToText(mapMVC.model);
    TextFileHandler.saveText(output, loadSaveFile.get());
    // make sure fileChooser is up to date on file list
    loadSaveFileChooser.rescanCurrentDirectory();
  } // saveMap

  /** loads the map from the file specified in loadSaveFile;
   * does no error checking
   */
  static void loadMap() {
    File loadFile = loadSaveFile.get();
    if (!loadFile.exists())
      return;
    Text input = TextFileHandler.loadText(loadFile);
    if (input != null) {
      new MapParser().restoreMap(mapMVC.model, input);
      // if new view would prefere a smaller size, grant it
      checkPack();
      // update imageChooser to new default filename
      String fileName = loadSaveFile.get().getName();
      String imageName = FileUtil.substituteExtension(fileName, "png");
      imageChooser.setSelectedFile(new File(imageName));
    }
  } // loadMap

  // the action listeners ...

  /** checks if the main frame need to be packed:
   * if components want to be smaller than they are => pack
   */
  static void checkPack() {
    Dimension wishSize = mapMVC.view.getPreferredSize();
    Dimension isSize = mapMVC.view.getSize();
    /*
    if ((wishSize.width < isSize.width &&
	wishSize.height <= isSize.height) ||
	(wishSize.width <= isSize.width &&
	 wishSize.height < isSize.height)) {
      MainFrame.get().pack();
    }
    */
    if (wishSize.width < isSize.width ||
	wishSize.height < isSize.height)
	MainFrame.get().pack();
  } // checkPack

  class NewMapAction implements ActionListener {

    public void actionPerformed(ActionEvent e) {
      loadSaveFile.set(null);
      mapMVC.model.execute(new CmdClearMap());
    } // actionPerformed
   
  } // NewMapAction

  class LoadAction implements ActionListener {

    public void actionPerformed(ActionEvent e) {
      JFileChooser fc = loadSaveFileChooser;
      int returnVal = fc.showOpenDialog(mapMVC.view);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
	loadSaveFile.set(fc.getSelectedFile());
	loadMap();
      }
    } // actionPerformed
    
  } // LoadAction
  
  class SaveAction implements ActionListener {
    
    public void actionPerformed(ActionEvent e) {
      if (loadSaveFile.get() != null) {
	saveMap();
      }
      else {
	new SaveAsAction().actionPerformed(e);
      }
    } // actionPerformed
    
  } // SaveAction
  
  class SaveAsAction implements ActionListener {
    
    public void actionPerformed(ActionEvent e) {
      JFileChooser fc = loadSaveFileChooser;
      int returnVal = fc.showSaveDialog(mapMVC.view);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
	loadSaveFile.set(fc.getSelectedFile());
	saveMap();
      }
    } // actionPerformed
    
  } // SaveAsAction
  
  class ExitAction implements ActionListener {
    
    public void actionPerformed(ActionEvent e) {
      System.exit(0);
    } // actionPerformed
    
  } // ExitAction
  
  class ClearAction implements ActionListener {
    
    public void actionPerformed(ActionEvent e) {
      String quest = "All rooms will be lost. Really clear map?";
      String title = "Clear Map Confirmation";
      int yesNo = JOptionPane.showConfirmDialog(MainFrame.get(), 
						quest, title, 
						JOptionPane.YES_NO_OPTION);
      if (yesNo == JOptionPane.YES_OPTION) 
	mapMVC.model.execute(new CmdClearMap());
    } // actionPerformed
    
  } // ClearAction

  class ResizeMapAction implements ActionListener {

    public void actionPerformed(ActionEvent e) {
      String title = "Configure Map Size";
      MultiInputDialog sizeDialog =
	new MultiInputDialog(title);
      Dimension mapDim = mapMVC.model.getSize();
      sizeDialog.addInput("map width: ", 
			  new Integer(mapDim.width).toString());
      sizeDialog.addInput("map height: ", 
			  new Integer(mapDim.height).toString());
      sizeDialog.pack();
      sizeDialog.show();
      // ... now the dialog does its thing
      // and now we're back..
      if (!sizeDialog.confirmed())
	return;
      // check if input is valid
      int sizeX, sizeY;
      try {
	sizeX = Integer.parseInt(sizeDialog.getInput(0));
	sizeY = Integer.parseInt(sizeDialog.getInput(1));
      } 
      catch (NumberFormatException ex) {
	return;
      }
      // keep size within spec
      //sizeX = AwtUtil.bond(sizeX, 1, 50);
      //sizeY = AwtUtil.bond(sizeY, 1, 50);
      sizeX = AwtUtil.bond(sizeX, 1, 1000);
      sizeY = AwtUtil.bond(sizeY, 1, 1000);
      // resize the map
      mapMVC.model.execute(new CmdSetSize(new Dimension(sizeX, sizeY)));
      // if new view would prefere a smaller size, grant it
      checkPack();
    } // actionPerformed

  } // ResizeMapAction

  class ViewSizeAction implements ActionListener {

    int sizeToSet;
    boolean request;

    /** lets the user customize the view size
     */
    public ViewSizeAction() {
      request = true;
    } // ViewSizeAction

    /** sets the view size
     * @param sizeToSet the new view-size
     */
    public ViewSizeAction(int sizeToSet) {
      this.sizeToSet = sizeToSet;
      request = false;
    } // ViewSizeAction

    public void actionPerformed(ActionEvent e) {
      int newSize = sizeToSet;
      if (request) {
	String title = "Configure View Size";
	MultiInputDialog sizeDialog =
	  new MultiInputDialog(title);
	// assume that MapGraphics is used as view
	int viewSize = ((ExtendedMapMVC)mapMVC).mapGraphics.getPaintSize();
	sizeDialog.addInput("view size: ", 
			    new Integer(viewSize).toString());
	sizeDialog.pack();
	sizeDialog.show();
	// ... now the dialog does its thing
	// and now we're back..
	if (!sizeDialog.confirmed())
	  return;
	// check if input is valid
	try {
	  newSize = Integer.parseInt(sizeDialog.getInput(0));
	} 
	catch (NumberFormatException ex) {
	  return;
	}
	// make sure view size is within limits
	newSize = AwtUtil.bond(newSize, 1, 20);
      } 
      // resize the view
      mapMVC.mapGraphics.setPaintSize(newSize);
      // if new view would prefere a smaller size, grant it
      checkPack();
    } // actionPerformed

  } // ViewSizeAction

  class UpdateViewAction implements ActionListener {

    public void actionPerformed(ActionEvent e) {
      // must pack twice - some bug in pack?
      MainFrame.get().pack();
      MainFrame.get().pack();
    } // actionPerformed

  } // UpdateViewAction

  class ImageAction implements ActionListener {

    StateController legendController;

    public ImageAction(StateController legendController) {
      this.legendController = legendController;
    } // ImageAction

    public void actionPerformed(ActionEvent e) {
      JFileChooser fc = imageChooser;
      int returnVal = fc.showSaveDialog(MainFrame.get());
      if (returnVal == JFileChooser.APPROVE_OPTION) {
	MapImagePainter mip = new MapImagePainter(mapMVC.model);
	mip.layout(((Integer)legendController.state()).intValue());
	BufferedImage mapImage = mip.paintMapImage();
	if (mapImage == null) {
	  String msg = "Nothing there to be painted";
	  JOptionPane.showMessageDialog(MainFrame.get(), msg, "Error", 
				    JOptionPane.ERROR_MESSAGE);
	  return;
	}
	ImageUtil.saveImageAsPNG(mapImage, fc.getSelectedFile());
	fc.rescanCurrentDirectory();
      }
    } // actionPerformed

  } // ImageAction

  class PreviewAction implements ActionListener {

    StateController legendController;

    public PreviewAction(StateController legendController) {
      this.legendController = legendController;
    } // PreviewAction

    public void actionPerformed(ActionEvent e) {
      MapImagePainter mip = new MapImagePainter(mapMVC.model);
      mip.layout(((Integer)legendController.state()).intValue());
      Image mapImage = mip.paintMapImage();
      if (mapImage == null)
	return;
      ImageIcon mapIcon = new ImageIcon(mapImage);
      JFrame window = new JFrame("Preview");
      window.getContentPane().add(new JScrollPane(new JLabel(mapIcon)));
      window.pack();
      window.show();
    } // actionPerformed

  } // PreviewAction

  class SearchRoomsAction implements ActionListener {

    public void actionPerformed(ActionEvent e) {
      String title = "Search Room Descriptions";
      CheckBoxDialog searchDialog =
	new CheckBoxDialog(title);
      searchDialog.getCheckBox().setText("match room name");
      searchDialog.addInput("Search String: ", "");
      searchDialog.pack();
      searchDialog.show();
      // now the dialog does its thing ...
      // ... and now we're back
      if (!searchDialog.confirmed())
	return;
      String searchString = searchDialog.getInput(0);
      boolean nameOnly = searchDialog.getCheckBox().isSelected();
      MapUtil.markRoomsWithDesc(mapMVC.model, searchString, nameOnly);
    } // actionPerformed

  } // SearchRoomsAction

  class UnmarkRoomsAction implements ActionListener {

    public void actionPerformed(ActionEvent e) {
      MapUtil.unmarkRooms(mapMVC.model);
    } // actionPerformed

  } // UnmarkRoomsAction

  class ColorRoomsAction implements ActionListener {

    protected NamedColor color;

    public ColorRoomsAction(NamedColor color) {
      this.color = color;
    } // ColorRoomsAction

    public void actionPerformed(ActionEvent e) {
      MapUtil.colorMarkedRooms(mapMVC.model, color);
    } // actionPerformed

  } // ColorRoomsAction

  /** presents a String selection to the user and returns his choice;
   * if the user cancels, returns null
   * @param title the title of the Dialog
   */
  static String requestChoice(String title, String[] choice) {
    ConfirmDialog dialog = new ConfirmDialog(title);
    JComboBox<String> comboBox = new JComboBox<String>(choice);
    JPanel comboPanel = new JPanel();
    comboPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 50, 10));
    comboPanel.add(comboBox);
    dialog.setInputComponent(comboPanel);
    dialog.pack();
    dialog.show();
    if (!dialog.confirmed())
      return null;
    else
      return (String)comboBox.getSelectedItem();
  } // requestChoice

  static String requestRoomFlag(String title) {
    String[] choice = RomConverterTool.getRoomFlagCommands();
    return requestChoice(title, choice);
  } // requestRoomFlag

  static String requestSector(String title) {
    String[] choice = RomConverterTool.getSectorNames();
    String param = requestChoice(title, choice);
    if (param == null)
      return null;
    else
      return RomConverterTool.SECTOR_CMD + " " + param;
  } // requestSector

  class SetSectorAction implements ActionListener {

    public void actionPerformed(ActionEvent e) {
      String command = requestSector("Set Sector");
      if (command != null)
	MapUtil.replaceCommandOnMarkedRooms(mapMVC.model, command);
    } // actionPerformed
    
  } // SetRoomFlagAction

  class SetRoomFlagAction implements ActionListener {

    public void actionPerformed(ActionEvent e) {
      String command = requestRoomFlag("Set Room Flag");
      if (command != null)
	MapUtil.replaceCommandOnMarkedRooms(mapMVC.model, command);
    } // actionPerformed
    
  } // SetRoomFlagAction

  class RemoveRoomFlagAction implements ActionListener {

    public void actionPerformed(ActionEvent e) {
      String command = requestRoomFlag("Remove Room Flag");
      if (command != null)
	MapUtil.removeCommandOnMarkedRooms(mapMVC.model, command);
    } // actionPerformed
    
  } // RemoveRoomFlagAction

  class MapConvertAction implements ActionListener {
    
    MapConverter mapConverter;
    JFileChooser fc = createFileChooser();

    public MapConvertAction(MapConverter mapConverter, String extension) {
      this.mapConverter = mapConverter;
      /* wait for swing 1.0.3
	 FileFilter areaFilter = new ExtensionFileFilter(extension);
	 fc.addChoosableFileFilter(areaFilter);
	 fc.setFileFilter(areaFilter);
      */
    } // MapConvertAction
    
    public void actionPerformed(ActionEvent e) {
      int returnVal = fc.showSaveDialog(MainFrame.get());
      if (returnVal != JFileChooser.APPROVE_OPTION)
	return;
      File outFile = fc.getSelectedFile();
      String title = "Vnum Chooser";
      MultiInputDialog vnumDialog =
	new MultiInputDialog(title);
      vnumDialog.addInput("Starting Vnum: ", "");
      vnumDialog.pack();
      vnumDialog.show();
      // ... now the dialog does its thing
      // and now we're back..
      if (!vnumDialog.confirmed())
	return;
      int startVnum;
      // check if input is valid
      try {
	startVnum = Integer.parseInt(vnumDialog.getInput(0));
      } 
      catch (NumberFormatException ex) {
	return;
      }
      // perform conversion and save it
      Text text = mapConverter.mapToText(mapMVC.model, startVnum, 
					 outFile.getName());
      TextFileHandler.saveText(text, outFile);
      fc.rescanCurrentDirectory();
    } // actionPerformed

  }  // MapConvertAction   

  class InfoAction implements ActionListener {

    String title;
    File file;

    public InfoAction(String title, String filename) {
      this.title = title;
      this.file = new File(filename);
    } // InfoAction

    public void actionPerformed(ActionEvent e) {
      InfoFrame.showFile(title, file);
    } // actionPerformed

  } // InfoAction

  class MapNotifyAction implements ActionListener {

    AreaMap map;
    Object change;

    public MapNotifyAction(AreaMap map, Object change) {
      this.map = map;
      this.change = change;
    } // MapNotifyAction

    public void actionPerformed(ActionEvent e) {
      map.notifyOfChange(change);
    } // actionPerformed

  } // MapNotifyAction

  class AboutAction implements ActionListener {

    public void actionPerformed(ActionEvent e) {
      String msg =
	"        MapMaker v2.3.5\n" +
	"(c) 2000 by Henning Koehler\n" +
	"        koehlerh@in.tum.de";
      JOptionPane.showMessageDialog(MainFrame.get(), msg, "About", 
				    JOptionPane.INFORMATION_MESSAGE);
    } // actionPerformed

  } // AboutAction

} // MapMaker



