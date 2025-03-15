package mapmaker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/** a wrapper for the View of mapmaker's MVC pattern
 */
public class JMap extends JPanel implements Observer {

  MapViewer mv;
  Dimension size;

  /** @param map should be the same Observerable as given
   * to the Model object
   * @param mv the View object
   */
  public JMap(Observable map, MapViewer mv) {
    this.mv = mv;
    map.addObserver(this);
    mv.addListeners(this);
    size = mv.getSize();
    setBackground(new Color(241, 234, 234));
    setOpaque(true);
  } // JMap

  public void paintComponent(Graphics g) {
    super.paintComponent(g); //paint background
    mv.paint(g);
  } // paintComponent

  public void update(Observable o, Object arg) {
    if (!size.equals(mv.getSize())) {
      size = mv.getSize();
      // ensure that resizing takes place
      // even if enclosing component doesn't deem it neccessary
      revalidate();
      repaint();
    }
    else
      repaint();
    requestFocus();
  } // update

  public Dimension getPreferredSize() {
    return mv.getSize();
  } // getPreferredSize

  public Dimension getMaximumSize() {
    return getPreferredSize();
  } // getMaximumSize

} // JMap
