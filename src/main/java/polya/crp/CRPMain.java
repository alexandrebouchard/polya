package polya.crp;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import polya.parametric.normal.NIWs;
import polya.totransfer.OutputManager;

import bayonet.coda.CodaParser;
import bayonet.coda.SimpleCodaPlots;
import bayonet.rplot.PlotContour;

import com.beust.jcommander.internal.Lists;



public class CRPMain
{
  private static final String SUMMARY_STATS = "summaryStatistics";
  
  private static int thinPeriod = 10, burnIn = 100;
//  private static List<? extends SampleProcessor> processors = Arrays.asList(
//      
//      new SampleProcessor() {
//        @Override public void process(CompleteState state)
//        {
//          System.out.println(state);
//        }
//      },
//      
//      new Sample
//  
//  );

  public static void main(String [] args)
  {
    CompleteState completeState = CompleteState.standardInit(new File("/Users/bouchard/experiments/pgc/data/circle/data.csv"));
    Random rand = new Random(1);
    
    OutputManager output = new OutputManager();
    File csvSamples = new File("samples-csv");
    output.setOutputFolder(csvSamples);
    
    for (int mcmcSweep = 0; mcmcSweep < 10000; mcmcSweep++)
    {
      
      completeState.doOneSamplingRound(rand);
      
      if (mcmcSweep % thinPeriod  == 0 && mcmcSweep > burnIn)
      {
        for (String key : completeState.realValuedStatistics().keySet())
          output.printWrite(key, "mcmcIter", mcmcSweep, key, completeState.realValuedStatistics().get(key).getValue());
        System.out.println("Number of individual sampling steps so far: " + (mcmcSweep+1)* completeState.nVariables());
        System.out.println();
        
        PlotContour pc = PlotContour.fromFunction(completeState.logPredictive());
        pc.min_x = -15;
        pc.max_x = 15;
        pc.min_y = -15;
        pc.max_y = 15;
        pc.toPDF(new File("iter-" + mcmcSweep + ".pdf"));
        
      }
    }
    
    output.close();
    
    File 
      indexFile = new File("CODAindex.txt"),
      chainFile = new File("CODAchain1.txt");
    CodaParser.CSVToCoda(indexFile, chainFile, csvSamples);
    SimpleCodaPlots codaPlots = new SimpleCodaPlots(chainFile, indexFile);
    codaPlots.toPDF(new File("codaPlots.pdf"));
  }
  

//  public static interface SampleProcessor
//  {
//    public void process(CompleteState state);
//  }

}
