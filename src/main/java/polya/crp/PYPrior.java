package polya.crp;

import polya.mcmc.RealVariable;


/**
 * A Pitman-Yor prior.
 * 
 * See: http://en.wikipedia.org/wiki/Pitman%E2%80%93Yor_process
 * 
 * Note: the values alpha0 and discount contained in an instance of 
 * this class may change during its existence, because of
 * resampling.
 * 
 * @author Alexandre Bouchard (alexandre.bouchard@gmail.com)
 *
 */
public class PYPrior
{
  /**
   * The strength parameter. Sometimes denoted theta or alpha0.
   * 
   * Higher values tends to create more table.
   */
  private double alpha0;
  
  /**
   * The discount parameter. 
   * 
   * Boosts table creation proportionally
   * to the number of existing tables.
   */
  private double discount;
  
  /**
   * 
   * @param alpha0
   * @param discount
   */
  public PYPrior(double alpha0, double discount)
  {
    this.alpha0 = alpha0;
    this.discount = discount;
    checkBounds();
  }

  /**
   * @throws RuntimeException if bounds not respected
   */
  public void checkBounds()
  {
    if (!inBounds())
      throw new RuntimeException();
  }
  
  /**
   * 
   * @return
   */
  public boolean inBounds()
  {
    return discount >= 0 && discount < 1 && alpha0 > -discount;
  }

  /**
   * The predictive probability  of assigning a customer to a table
   * where there was already nCustomersAtTable people (before the 
   * queried insertion), and where there are alredy nTables (before 
   * the queried insertion).
   * 
   * Use nCustomersAtTable set to zero for table creation.
   *   
   * @param nCustomersAtTable
   * @param nTables
   * @return The predictive probability
   */
  public double logUnnormalizedPredictive(int nCustomersAtTable, int nTables)
  {
    if (nCustomersAtTable == 0)
      return Math.log(alpha0 + nTables * discount);
    else
      return Math.log(nCustomersAtTable - discount);
  }
  
  /**
   * Used by polya.mcmc to resample the alpha0 variable
   * 
   * @return
   */
  public RealVariable alpha0VariableView()
  {
    return new RealVariable() {
      @Override public void setValue(double newValue) { alpha0 = newValue; }
      @Override  public double getValue()             { return alpha0; }
    };
  }

  
  /**
   * Used by polya.mcmc to resample the discount variable
   * 
   * @return
   */
  public RealVariable discountVariableView()
  {
    return new RealVariable() {
      @Override public void setValue(double newValue) { discount = newValue; }
      @Override  public double getValue()             { return discount; }
    };
  }

  /**
   * The discount parameter. 
   * 
   * Boosts table creation proportionally
   * to the number of existing tables.
   */
  public double discount()
  {
    return discount;
  }

  /**
   * The strength parameter. Sometimes denoted theta or alpha0.
   * 
   * Higher values tends to create more table.
   */
  public double alpha0()
  {
    return alpha0;
  }
}