package polya.mcmc;




/**
 * 
 * @author Alexandre Bouchard (alexandre.bouchard@gmail.com)
 *
 */
public class ExponentialPrior implements RealNodePrior
{
  /**
   * The variable this is a prior on 
   */
  private RealVariable variable;
  
  /**
   * The exponential can be translated; corresponding to
   * min being different than 0. For example, min=1 means
   * that X-1 is exponential.
   */
  private final double min;
  
  /**
   * Rate parameter (NOT mean)
   */
  private final double rate;
  
  /**
   * 
   * @param lambda Rate parameter (NOT mean)
   * @return
   */
  public static ExponentialPrior withRate(double lambda)
  {
    return new ExponentialPrior(lambda, 0.0);
  }
  
  /**
   * 
   * @param minValue
   * @return
   */
  public ExponentialPrior truncateAt(double minValue)
  {
    return new ExponentialPrior(this.rate, minValue);
  }
  
  private ExponentialPrior(
      double rate,
      double min)
  {
    this.rate = rate;
    this.min = min;
  }

  @Override
  public double logUnnormalizedPotential()
  {
    double x = variable.getValue() - min;
    if (x < 0.0)
      return Double.NEGATIVE_INFINITY;
    return -rate * x;
  }

  @Override
  public void setVariable(RealVariable variable)
  {
    this.variable = variable;
  }
}