package polya.totransfer;

import java.util.Collection;



public class CSV
{

  public static String toCSV(Collection objects)
  {
    return toCSV(objects.toArray());
  }
  public static String toCSV(Object... objects)
  {
    StringBuilder result = new StringBuilder();
    for (int i =0 ; i < objects.length; i++)
    {
      String cur = objects[i] == null ? "" : objects[i].toString();
      cur = cur.replace("\\", "\\\\");
      if (cur.contains("\"")||cur.contains(","))
        result.append("\"" + cur.replaceAll("\"", "\"\"") + "\"");
      else
        result.append(cur);
      if (i != objects.length-1)
        result.append(",");
    }
    return result.toString();
  }
}
