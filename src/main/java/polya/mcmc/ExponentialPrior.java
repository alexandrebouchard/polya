package polya.mcmc;





public class ExponentialPrior implements RealNodePrior
{
  private RealVariable variable;
  private final double min;
  private final double rate;
  
  
  public static ExponentialPrior withRate(double lambda)
  {
    return new ExponentialPrior(lambda, 0.0);
  }
  
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
    return -rate * x;
  }

  @Override
  public void setVariable(RealVariable variable)
  {
    this.variable = variable;
  }
}