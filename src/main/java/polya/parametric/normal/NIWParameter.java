package polya.parametric.normal;

import org.ejml.simple.SimpleMatrix;

import polya.parametric.Parameter;



public class NIWParameter implements Parameter
{
  private final SimpleMatrix meanParameter, covarianceParameter;

  public NIWParameter(
      SimpleMatrix meanParameter,
      SimpleMatrix covarianceParameter)
  {
    this.meanParameter = meanParameter;
    this.covarianceParameter = covarianceParameter;
  }

  public SimpleMatrix getMeanParameter()
  {
    return meanParameter;
  }

  public SimpleMatrix getCovarianceParameter()
  {
    return covarianceParameter;
  }
}
