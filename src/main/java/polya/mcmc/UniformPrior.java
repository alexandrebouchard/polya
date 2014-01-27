package polya.mcmc;





public class UniformPrior implements RealNodePrior
{
  private RealVariable variable;
  private final double min = 0.0, max = 1.0;
  
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