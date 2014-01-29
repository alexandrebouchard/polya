package polya.mcmc;


/**
 * A factor in a factor graph.
 * 
 * @author Alexandre Bouchard (alexandre.bouchard@gmail.com)
 *
 */
public interface Factor
{
  /**
   * 
   * @return
   */
  public double logUnnormalizedPotential();
}