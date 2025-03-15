package mapmaker;

import util.*;

import javax.swing.*;

/** Concrete Description Object
 */
public class CDescObj
  extends JPanel
  implements DescObj {

  private Text desc = new Text(); // ensure desc is never null

  public Text getDesc() {
    return (Text)desc.clone();
  } // getDesc

  public void setDesc(Text desc) {
    if (desc == null)
      this.desc = new Text();
    else
      this.desc = (Text)desc.clone();
  } // setDesc

} // CDescObj
