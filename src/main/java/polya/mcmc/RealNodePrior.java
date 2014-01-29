package polya.mcmc;


/**
 * 
 * A special type of Factor, coming from a prior distribution over
 * a real variable.
 * 
 * @author Alexandre Bouchard (alexandre.bouchard@gmail.com)
 *
 */
public interface RealNodePrior extends Factor
{
  /**
   * 
   * @param variable
   */
  public void setVariable(RealVariable variable);
}