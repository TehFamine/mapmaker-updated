package mapmaker;

public class Compressor {

  /** provides information to resize an array to a given size by 
   * inserting or removing elements in it; which elements are 
   * allowed to be removed is given in advance;
   * if number of non-removable elements is larger that targeted
   * size, resizes to an array containing only the non-removeables;
   * returns an array of size equal to the new array size, with each
   * element equal to -1 if none of the old elements got transfered
   * to this position, or equal to that element's previous index 
   * in the given array otherwise
   * @param removable is of length of the old array, indicates for
   * each element wether it's allowed to be removed or not
   * @param size the size the new array should have if possible
   */
  public static int[] compress(boolean[] removable, int size) {
    int minSize = 0;
    for (int i = 0; i < removable.length; i++)
      if (!removable[i])
	minSize++;
    // three cases: no removal, max removal, or something in between
    // no removal:
    if (size >= removable.length) {
      int[] compressed = new int[size];
      for (int i = 0; i < compressed.length; i++) 
	if (i < removable.length)
	  compressed[i] = i;
	else
	  compressed[i] = -1;
      return compressed;
    }
    // max removal:
    if (size <= minSize) {
      int[] compressed = new int[minSize];
      int pos = 0;
      for (int i = 0; i < compressed.length; i++) {
	// find next non-removable element
	while (removable[pos])
	  pos++;
	compressed[i] = pos;
	pos++;
      }
      return compressed;
    }
    // something in between:
    // Strategy: remove elements from the outside first
    int[] compressed = new int[size];
    int maxRemovable = size - minSize; // nr of removable elements to copy
    // find last non-removable element
    int lastPos = size - 1;
    while (lastPos >= 0 && removable[lastPos])
      lastPos--;
    // find first non-removable element
    int firstPos = 0;
    while (firstPos < compressed.length && removable[firstPos])
      firstPos++;
    // calculate maximum ammout of beginning elements to skip
    int maxSkip = (lastPos + 1) - size;
    if (maxSkip < 0)
      maxSkip = 0;
    // pos = min(maxSkip, firstPos)
    int pos = Math.min(maxSkip, firstPos);
    // start coppying positions at pos, copy removable positions
    // until maxRemovable reached
    int removed = 0;
    for (int i = 0; i < compressed.length; i++) {
      if (removed >= maxRemovable)
	while (removable[pos])
	  pos++;
      else
	if (removable[pos])
	  removed++;
      compressed[i] = pos++;
    }
    return compressed;
  } // compress
    
} // Compressor
