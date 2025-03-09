package mapmaker;

import util.*;

public class DescParser {
  
  private CompositeFilter filter = new CompositeFilter();

  /** adds a string to the set of comment strings;
   * lines that start with a comment string will be ignored 
   * when parsing a text
   */
  public void addCommentString(String commentString) {
    filter.addFilter(new CommentFilter(commentString));
  } // addCommentString

  /** returns wether the given string starts with a commentString
   * @param s must not be null
   */
  public boolean isComment(String s) {
    return !filter.accept(s);
  } // isComment

  /** returns the first non-comment line of the given text
   * or "" if no such line exists
   */
  public String getName(Text desc) {
    Text filtered = filterDesc(desc);
    if (filtered.getLineCount() == 0)
      return "";
    else
      return filtered.getLine(0);
  } // getName

  /** filters the coment lines out of the given descriptions
   */
  public Text filterDesc(Text desc) {
    return filter.filter(desc);
  } // filterDesc

} // DescParser
