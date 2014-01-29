package polya.mcmc;




/**
 * A uniform prior. For example, UniformPrior.onUnitInteval()
 * return a uniform prior on the unit interval.
 * 
 * @author Alexandre Bouchard (alexandre.bouchard@gmail.com)
 *
 */
public class UniformPrior implements RealNodePrior
{
  private RealVariable variable;
  private final double min = 0.0, max = 1.0;
  
  /**
   * 
   * @return A uniform prior on the unit interval.
   */
  public static UniformPrior onUnitInteval()
  {
    return new UniformPrior();
  }

  @Override
  public double logUnnormalizedPotential()
  {
    double x = variable.getValue();
    if (x < min) return Double.NEGATIVE_INFINITY;
    if (x > max) return Double.NEGATIVE_INFINITY;
    return 0.0;
  }

  @Override
  public void setVariable(RealVariable variable)
  {
    this.variable = variable;
  }
}