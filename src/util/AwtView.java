package util;

import java.awt.*;

public interface AwtView {

  /* paints to the given graphics context
   */
  void paint(Graphics g);

  /* adds event listeneners to given component
   */
  void addListeners(Component speaker);

  /* returns the view's preferred size
   */
  Dimension getSize();

} // AwtView 
