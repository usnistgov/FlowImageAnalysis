package Variable_Threshold;
import java.util.Arrays;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.ResultsTable;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.frame.*;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Arrays;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.lang.*;
import ij.WindowManager;
import java.awt.*;
import java.util.Vector;
import java.util.Properties;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.measure.*;
import ij.text.*;
import ij.util.Tools;
import ij.macro.Interpreter;
import ij.io.Opener;
import ij.io.OpenDialog;
import static ij.measure.Measurements.*;
import ij.plugin.filter.ParticleAnalyzer;

public class Variable_Threshold implements PlugIn {

    // Order must agree with order of checkboxes in Set Measurements dialog box
    private static final int[] list = {AREA, MEAN, STD_DEV, MODE, MIN_MAX,
        CENTROID, CENTER_OF_MASS, PERIMETER, RECT, ELLIPSE, SHAPE_DESCRIPTORS, FERET,
        INTEGRATED_DENSITY, MEDIAN, SKEWNESS, KURTOSIS, AREA_FRACTION, STACK_POSITION};
    private static final String MEASUREMENTS = "measurements";
    private static int systemMeasurements = Prefs.getInt(MEASUREMENTS, AREA + MEAN + MIN_MAX);

    /**
     * Sets the system-wide measurement options.
     */
    public void run(String arg) {

        ImageCalculator ic = new ImageCalculator();
        ImagePlus impR = IJ.getImage();

        int framew = impR.getWidth();
        int frameh = impR.getHeight();
                    int measurements = systemMeasurements;
                    String MeasCommand="";
        boolean EnhanceContrastCollage = false;
//        ImageStatistics stats = imp.getStatistics();
        String RawStack = impR.getTitle();
        // Create window with identified ROIs against black background, autocontrast raw image for better visibility       
            IJ.selectWindow(RawStack);
            ImagePlus impr2 = new Duplicator().run(IJ.getImage());
        String name = "";
        String runnumber = "";
        IJ.selectWindow(RawStack);
        String path = IJ.getDirectory("image");
        IJ.log(RawStack);
        String fname = "";
        int index = RawStack.lastIndexOf('.');
        int i;
        boolean LabelCollageParticles = true;
        boolean ShowParticlesInImageStack=true;
        int labelinterval = 5;
        int maxcollageslices = 100;
        if (index != -1) {
            name = RawStack.substring(0, index);
            fname = path + name + "0" + ".xls";
            Path firstPath;
            i = 0;
            do {
                i++;
                runnumber = Integer.toString(i);
                fname = path + name + runnumber + ".xls";
                firstPath = Paths.get(fname);
            } while (Files.exists(firstPath));
        }
        IJ.log("\\Clear");
        boolean SaveResults = true;
        if (ij.WindowManager.getWindow("ROI Manager")!=null){
        IJ.selectWindow("ROI Manager");
           IJ.run("Close");}
               ResultsTable rt = new ResultsTable();
        GenericDialog gdA = new GenericDialog("Variable Threshold Process");
        String[] Runoption = new String[]{
            "Do VT on a New Image Stack", "Filter previous VT-processed Image Stack"};
        gdA.addChoice("Run type", Runoption, Runoption[0]);
        gdA.showDialog();
        if (gdA.wasCanceled()) {
            return;
        }
        int runtype = gdA.getNextChoiceIndex();
        IJ.log("Run Type " + Runoption[runtype]);
                GenericDialog DoFilter = new GenericDialog("Do Filter?");
        boolean morefilter = true;
//int runtype=0;
        if (runtype == 1) {
            GenericDialog gdB = new GenericDialog("Filter previous VT-processed Image Stack");
            gdB.addCheckbox("Save Results ", SaveResults);
            gdB.addCheckbox("Label collage particles ", LabelCollageParticles);
            gdB.addCheckbox("Enhance Contrast collage particles ", EnhanceContrastCollage);
            gdB.addNumericField("label interval for collage particles: ", labelinterval, 0);
            gdB.addNumericField("Max number of collage slices: ", maxcollageslices, 0);

            gdB.addMessage("After 'OK' Load ROI .zip File associated with image stack");
            gdB.showDialog();
            if (gdB.wasCanceled()) {
                return;
            }
            SaveResults = gdB.getNextBoolean();
            LabelCollageParticles = gdB.getNextBoolean();
            EnhanceContrastCollage  = gdB.getNextBoolean();
            labelinterval = (int) gdB.getNextNumber();
            maxcollageslices = (int) gdB.getNextNumber();
                        RoiManager rm = RoiManager.getInstance();
        if (rm == null) {
            rm = new RoiManager();
        }
            rm.runCommand("reset");
            OpenDialog od = new OpenDialog("Load ROI .zip File associated with image stack", "");
            String directory = od.getDirectory();
            fname = directory + od.getFileName();
            if (name == null) {
                return;
            }
                        if (EnhanceContrastCollage) {
                IJ.run(impr2, "Enhance Contrast", "saturated=0.20");
                IJ.run(impr2, "Apply LUT", "stack");
            }
            IJ.run("Clear Results", "");
            rm.runCommand("Open", fname);
                   MeasCommand=DefineMeasurements(measurements); 
                           IJ.run("Set Measurements...", MeasCommand + " redirect=" + RawStack + " decimal=3");
            rm.runCommand("Measure");
            rt = ResultsTable.getResultsTable();
        } else {
            RoiManager rmA = new RoiManager(true);

        if (rmA == null) {
            rmA = new RoiManager();
        } 
            ImagePlus imp = Remove_Background(impR);
            ImageStatistics stats = ImageStatistics.getStatistics(imp.getProcessor(), ImageStatistics.MIN_MAX, null);
            IJ.log("max of raw image" + Double.toString(stats.max));
            String StartStack = imp.getTitle();
            imp.show();
            int StackSize = imp.getNSlices() * imp.getNFrames();
            System.out.println("Nslices" + StackSize);
            ImagePlus impc = IJ.createImage("Collage", "8-bit black", impR.getWidth(), impR.getHeight(), StackSize);
            rmA.runCommand("reset");
            IJ.run("Colors...", "foreground=white background=black selection=blue");
            int aa;
            int j;
            int k;
            int border = 5;
            int Minsize = 5;
            String Minsizes;
            int Maxsize = 100000;
            String Maxsizes;
            float MinCirc = (float) .02;
            String MinCircs = Float.toString(MinCirc);
            float MaxCirc = (float) 1.0;
            String MaxCircs = Float.toString(MaxCirc);
            int FinalDilateErodeSteps = 0;
            int InitialDilateErodeSteps = 1;
//            int ThreshStart = 20;
            int ThreshStart = 8;
            int ThreshDelta = 0;
            int nThresholds = 20;
 //           int nThresholds = 20;
            float MaxGray2ThreshFactor = (float) 0.5;
            String MaxGray2ThreshFactors = Float.toString(MaxGray2ThreshFactor);
            float ThreshReset;
            float ThreshResetMax = 50;
            String ThreshResetMaxs = Float.toString(ThreshResetMax);
            boolean DoHull = true;
            boolean areaboolean = true;
            boolean[] ROI_BinEmptyArray = new boolean[nThresholds];
            int[] SelectArray;
            int nROIs;
            int nROIs2;
            int nROIs3;
            int nROIs4;
            String ThreshMask = "HyperStack";
            String HullMask = "HullMask";
            String ROIfname;
            GenericDialog gd = new GenericDialog("Set Parameters");
            gd.addNumericField("Beginning Threshold: ", ThreshStart, 0);
            gd.addStringField("MaxGray2ThreshFactors: ", MaxGray2ThreshFactors);
            gd.addNumericField("Number of Thresholds: ", nThresholds, 0);
            gd.addStringField("Max Value for Reset Threshold: ", ThreshResetMaxs);
            gd.addStringField("Minimum Circularity: ", MinCircs);
            gd.addStringField("Maximum Circularity: ", MaxCircs);
            gd.addNumericField("Minsize (pixels): ", Minsize, 0);
            gd.addNumericField("Maxsize (pixels): ", Maxsize, 0);
            gd.addNumericField("InitialDilateErodeSteps: ", InitialDilateErodeSteps, 0);
            gd.addNumericField("FinalDilateErodeSteps (if Do Hull not selected): ", FinalDilateErodeSteps, 0);
            gd.addNumericField("label interval for collage particles: ", labelinterval, 0);
            gd.addNumericField("max number of collage slices: ", maxcollageslices, 0);
            gd.addCheckbox("Save Results ", SaveResults);
            gd.addCheckbox("Do Hull Analysis ", DoHull);
            gd.addCheckbox("Enhance Contrast collage particles ", EnhanceContrastCollage);
            gd.addCheckbox("Label collage particles ", LabelCollageParticles);
            gd.addCheckbox("Show Particles in Image Stack with black backgnd", ShowParticlesInImageStack);
            gd.showDialog();
            if (gd.wasCanceled()) {
                return;
            }
            ThreshStart = (int) gd.getNextNumber();
            MaxGray2ThreshFactor = Float.valueOf(gd.getNextString());
            nThresholds = (int) gd.getNextNumber();
            ThreshResetMaxs = gd.getNextString();
            MinCircs = gd.getNextString();
            MaxCircs = gd.getNextString();
            Minsize = (int) gd.getNextNumber();
            Maxsize = (int) gd.getNextNumber();
            InitialDilateErodeSteps = (int) gd.getNextNumber();
            FinalDilateErodeSteps = (int) gd.getNextNumber();
            labelinterval = (int) gd.getNextNumber();
            maxcollageslices = (int) gd.getNextNumber();
            SaveResults = (boolean) gd.getNextBoolean();
            DoHull = (boolean) gd.getNextBoolean();
            EnhanceContrastCollage = (boolean) gd.getNextBoolean();
            LabelCollageParticles = (boolean) gd.getNextBoolean();
            ShowParticlesInImageStack=(boolean) gd.getNextBoolean();
            Minsizes = Integer.toString(Minsize);
            Maxsizes = Integer.toString(Maxsize);
            ThreshResetMax = Float.valueOf(ThreshResetMaxs);
                        if (EnhanceContrastCollage) {
                IJ.run(impr2, "Enhance Contrast", "saturated=0.20");
                IJ.run(impr2, "Apply LUT", "stack");
            }
            if (LabelCollageParticles) {
                border = 7;
            }
            ImagePlus impf = IJ.createImage("FusedMask", "8-bit black", framew, frameh, StackSize);
            ImagePlus impg = IJ.createImage(HullMask, "8-bit white", framew, frameh, StackSize);                

            if (SaveResults) {
               MeasCommand=DefineMeasurements(measurements); 
            }       
            IJ.log("MaxGray2ThreshFactor " + Float.toString(MaxGray2ThreshFactor));
            IJ.log("ThreshStart " + Integer.toString(ThreshStart));
            IJ.log("nThresholds " + Integer.toString(nThresholds));
            IJ.log("InitialDilateErodeSteps " + Integer.toString(InitialDilateErodeSteps));
            IJ.log("FinalDilateErodeSteps " + Integer.toString(FinalDilateErodeSteps));
            IJ.log("MinSize " + Integer.toString(Minsize));
            IJ.log("Min Circularity " + MinCircs);
            IJ.log("Max Circularity " + MaxCircs);
            IJ.log("HullAnalysis " + Boolean.toString(DoHull));
            rmA.runCommand("reset");
            ImagePlus imp2 = new Duplicator().run(imp);
            IJ.setThreshold(imp2, ThreshStart, 255);
            Prefs.blackBackground = false;
            IJ.run(imp2, "Make Binary", "method=Default background=Default");
            for (i = 0; i < InitialDilateErodeSteps; i++) {
                IJ.run(imp2, "Dilate", "stack");
            }
            IJ.run(imp2, "Fill Holes", "stack");
            for (i = 0; i < InitialDilateErodeSteps; i++) {
                IJ.run(imp2, "Erode", "stack");
            }
            //       draws line around image so that edge particles will be excluded
            IJ.run("Colors...", "foreground=black background=black selection=blue");
            IJ.run(imp2, "Line Width...", "line=2");
            imp2.setRoi(new Line(0, 0, framew - 1, 0));
            IJ.run(imp2, "Fill", "stack");
            IJ.run(imp2, "Draw", "stack");
            imp2.setRoi(new Line(framew - 1, 0, framew - 1, frameh - 1));
            IJ.run(imp2, "Fill", "stack");
            IJ.run(imp2, "Draw", "stack");
            imp2.setRoi(new Line(framew - 1, frameh - 1, 0, frameh - 1));
            IJ.run(imp2, "Fill", "stack");
            IJ.run(imp2, "Draw", "stack");
            imp2.setRoi(new Line(0, frameh - 1, 0, 0));
            IJ.run(imp2, "Fill", "stack");
            IJ.run(imp2, "Draw", "stack");
            IJ.run("Colors...", "foreground=white background=black selection=blue");
            IJ.run("Set Measurements...", "area min center  stack redirect=" + StartStack + " decimal=3");
            if (nThresholds == 1) /*& (!DoHull)) */{
                IJ.run("Set Measurements...", MeasCommand + " redirect=" + RawStack + " decimal=3");
                DoHull=false;
            }

//             ParticleAnalyzer.setRoiManager(rmA);
             if (nThresholds > 1){
             ParticleAnalyzer.setRoiManager(rmA);}

            IJ.run(imp2, "Analyze Particles...", "size=" + Minsizes + "-" + Maxsizes +/*Infinity"+*/ " pixel " + " circularity=" + MinCircs + "-" + MaxCircs + " display exclude clear add stack");
            String fDataSt;

            if (nThresholds == 1) 
            {rmA = RoiManager.getInstance();
             }            
            nROIs = rmA.getCount();
            IJ.log("Initial #nROIs " + Integer.toString(nROIs));

            float[] MaxGrayArray = new float[nROIs];
            int[] MaxGrayArrayIndex = new int[nROIs];
            int[] Bx1 = new int[nROIs];
            int[] By1 = new int[nROIs];
            int[] Bw1 = new int[nROIs];
            int[] Bh1 = new int[nROIs];
            int[] Bz1 = new int[nROIs];
            rt = ResultsTable.getResultsTable();
            if (nThresholds > 1) {
            MaxGrayArray = rt.getColumn(5);

            float MaxGraymax; 
            MaxGraymax = MaxGrayArray[0];

            float MaxGraymin = 1000;
            float MaxGrayupper = 150;
            Roi roi2;
            Rectangle r2;
            ij.WindowManager.setTempCurrentImage(imp2);
            for (i = 0; i <= nROIs - 1; i++) {
                rmA.select(i);
                roi2 = imp2.getRoi();
                r2 = roi2.getBounds();
                Bx1[i] = r2.x;
                By1[i] = r2.y;
                Bw1[i] = r2.width;
                Bh1[i] = r2.height;
                Bz1[i] = roi2.getPosition();

                if (MaxGrayArray[i] > MaxGraymax) {
                    MaxGraymax = MaxGrayArray[i];
                }
                if (MaxGrayArray[i] < MaxGraymin) {
                    MaxGraymin = MaxGrayArray[i];
                }
            }
            IJ.log("MaxGraymax " + Float.toString(MaxGraymax));
            IJ.log("MaxGraymin " + Float.toString(MaxGraymin));


                float deltaMaxGray = (MaxGrayupper - MaxGraymin) / nThresholds + (float) 0.01;
                int jsum = 0;
                for (k = 0; k < nThresholds; k++) {
                    IJ.log("working on kth threshold " + Integer.toString(k));
                    j = 0;
                    float MaxGrayLo = MaxGraymin + deltaMaxGray * (k);
                    float MaxGrayHi = MaxGrayLo + deltaMaxGray;
                    if (k == nThresholds - 1) {
                        MaxGrayHi = MaxGraymax;
                    }
                    IJ.log("Range of MaxGray Dev for this bin: " + Float.toString(MaxGrayLo) + " - " + Float.toString(MaxGrayHi));
                    for (i = 0; i <= (nROIs - 1); i++) {
//              Finds all ROIs within the next Max Gray Range, counts them with j, and adds their location to MaxGrayArrayIndex     
                        if ((MaxGrayArray[i] >= MaxGrayLo) && (MaxGrayArray[i] < MaxGrayHi)) {
                            MaxGrayArrayIndex[j] = i;
                            j++;
                        }
                    }
                    //              Batch Create a new mask for this set of ROIs, and OR this mask with building-up mask from previous iterations        
                    if (j > 0) {
//               Create image of just this batch of ROIs     
                        SelectArray = Arrays.copyOf(MaxGrayArrayIndex, j);
                        ImagePlus impn = IJ.createImage(ThreshMask, "8-bit black", framew, frameh, StackSize);
                        WindowManager.setTempCurrentImage(impn);
                        rmA.setSelectedIndexes(SelectArray);
                        rmA.runCommand("Fill");
                        ImagePlus impt = ic.run("And create stack", imp, impn);
                        IJ.log("Number of ROIs in MaxGray Dev Bin " + Integer.toString(j));
                        impn.close();
//               Set Threshold for this batch of ROIs
                        ThreshReset = Math.max(ThreshStart, MaxGray2ThreshFactor * MaxGrayLo);
                        if (ThreshReset > ThreshResetMax) {
                            ThreshReset = ThreshResetMax;
                        }
                        IJ.log("ThreshReset: " + Float.toString(ThreshReset));
                        IJ.setThreshold(impt, ThreshReset, 255);
                        IJ.run(impt, "Make Binary", "method=Default background=Default");
//                OR this mask with building-up mask from previous iterations               
                        impf = ic.run("Or create stack", impf, impt);
                        impt.close();
                        System.gc();
                    }
                }
                IJ.run(impf, "Invert LUT", "");
//for (i = 0; i <FinalDilateErodeSteps; i++) {IJ.run(impf, "Dilate", "stack");}
                IJ.run(impf, "Fill Holes", "stack");
//for (i = 0; i <FinalDilateErodeSteps; i++) {IJ.run(impf, "Erode", "stack");}
                ij.WindowManager.setTempCurrentImage(impf);

                
                rmA.runCommand("Deselect");
                rmA.runCommand("Delete");
                IJ.run("Set Measurements...", "area min center stack redirect=" + StartStack + " decimal=3");
                 ParticleAnalyzer.setRoiManager(rmA);
                IJ.run(impf, "Analyze Particles...", "size=" + "1" + "-Infinity pixel circularity=" + ".0" + "-1.00 display exclude clear add stack");
                nROIs2 = rmA.getCount();           
                int[] Bx2 = new int[nROIs2];
                int[] By2 = new int[nROIs2];
                int[] Bw2 = new int[nROIs2];
                int[] Bh2 = new int[nROIs2];
                int[] Bz2 = new int[nROIs2];
                Roi roi3;
                Polygon foo;
                IJ.log("#ROIS after Thresholding " + Integer.toString(nROIs2));
                for (i = 0; i <= nROIs2 - 1; i++) {
                    rmA.select(i);
                    roi3 = impf.getRoi();
                    foo = roi3.getPolygon();
                    Bx2[i] = foo.xpoints[1];
                    By2[i] = foo.ypoints[1];
                    Bz2[i] = roi3.getPosition();
                }
// Examine each original ROI bounds to see if there are now fragments in there
                ImageProcessor ipf = impf.getProcessor();
                ipf.setColor(Color.black);
                int jmin = 0;
                int newjmin = 0;
                int fragcount = 1;
                int fragtotal = 0;
                int xbegin = 0;
                int ybegin = 0;
                int xend;
                int yend;
                for (i = 0; i <= nROIs - 1; i++) {
                    fragtotal = fragtotal + fragcount - 1;
                    fragcount = 0;
                    jloop:
                    for (j = jmin; j <= nROIs2 - 1; j++) {
                        if (Bx2[j] >= Bx1[i] && By2[j] >= By1[i] && (Bz2[j] == Bz1[i])) {
                            if (Bx2[j] <= Bx1[i] + Bw1[i] && By2[j] <= By1[i] + Bh1[i]) {
                                //inside the ith box of original ROI
                                if (fragcount == 0) {
                                    //first found particle in the ith box of original ROI
                                    xbegin = Bx2[j];
                                    ybegin = By2[j];
                                    newjmin = j;
                                    fragcount++;
                                } else {
                                    //second or more particles in the ith box of original ROI
                                    xend = Bx2[j];
                                    yend = By2[j];
                                    if (fragcount == 1) {
                                        fragcount++;
                                    }
                                    impf.setPosition(Bz2[j]);
                                    ipf.setLineWidth(3);
                                    ipf.drawLine(xbegin, ybegin, xend, yend);
                                    ybegin = yend;
                                    xbegin = xend;
                                }
                            } else //outside the i'th box of original ROI
                            {
                                if ((Bx2[j] > Bx1[i] + Bw1[i] && By2[j] > By1[i] + Bh1[i]) || (Bz2[j] > Bz1[i])) {
                                    //outside and beyond in both x,y  or beyond in z the ith box of original ROI    
                                    break jloop;
                                }
                            }
                        }
                    }
                    jmin = newjmin; //next time look for particles beyond the ones already found
                }
                IJ.log("Fragmented Particles Total " + Integer.toString(fragtotal));
                IJ.setAutoThreshold(impf, "Default");
                Prefs.blackBackground = false;
                IJ.run(impf, "Convert to Mask", "method=Default background=Light");
                if (!DoHull) {
                    for (i = 0; i < FinalDilateErodeSteps; i++) {
                        IJ.run(impf, "Dilate", "stack");
                    }
                    IJ.run(impf, "Fill Holes", "stack");
                    for (i = 0; i < FinalDilateErodeSteps; i++) {
                        IJ.run(impf, "Erode", "stack");
                    }
                }
                IJ.run("Set Measurements...", MeasCommand + " redirect=" + StartStack + " decimal=3");
                rmA.reset();
                rmA.close();
                        RoiManager rm = RoiManager.getInstance();
        if (rm == null) {
            rm = new RoiManager();
        }
                 ParticleAnalyzer.setRoiManager(rm);               
                IJ.run(impf, "Analyze Particles...", "size=1-Infinity pixel circularity=0.00-1.00 show=Nothing display exclude clear add stack");
            }
                                    RoiManager rm = RoiManager.getInstance();
        if (rm == null) {
            rm = new RoiManager();
        }
            nROIs3 = rm.getCount();
            IJ.log("#ROIs after Joining Fragments " + Integer.toString(nROIs3));
            int[] SelectArray2 = new int[nROIs3];

// Convex Hull Analysis
            if (nThresholds == 1) {       
                impf = imp2;
            }
            if (DoHull) {
                String roiname = "";
                long startTime = System.currentTimeMillis();
                int ii;
                int ithous;
                Roi[] oldRois = rm.getRoisAsArray();
                     IJ.selectWindow("ROI Manager");
//           IJ.run("Close");
                rm.reset(); 
                IJ.log("Start Hull Loop ");impf.show();                    
                rm.runCommand(impf,"Show None");
                for (ii = 0; ii <= nROIs3 - 1; ii++) {
                    roiname=oldRois[ii].getName();
                    rm.addRoi(oldRois[ii]);
                    rm.select(ii);  
                    IJ.run(impf, "Convex Hull", "");
                    rm.addRoi(impf.getRoi());
                    rm.select(ii);
                    rm.runCommand(impf,"Delete");rm.select(ii);
                    oldRois[ii] = impf.getRoi();                              
                                    ithous = ii / 2000;
                    if ((2000 * ithous) == ii) {
                         IJ.showProgress(ii, nROIs3);
                        IJ.showStatus("Hull Loop");
                    }
                }
                for (ii = 0; ii <= nROIs3 - 1; ii++) {
                                        rm.addRoi(oldRois[ii]);
                                        }  
                IJ.showProgress(1, 0);
                IJ.showStatus(" ");
                IJ.log("Hull Loop Duration " + Long.toString((System.currentTimeMillis() - startTime) / 1000));
                rm.runCommand("Deselect");
                rm.runCommand("Deselect");
                impf.changes = false;  impf.close();
                IJ.run("Colors...", "foreground=black background=black selection=blue");
                ij.WindowManager.setTempCurrentImage(impg);
                rm.runCommand("Fill");
                IJ.run(impg, "Invert", "stack");
                ij.WindowManager.setTempCurrentImage(imp2);
                rm.runCommand("Show All");
                rm.runCommand("Show None");
                impg = ic.run("And create stack", imp2, impg);
                IJ.run(impg, "Invert", "stack");
                IJ.run(impg, "Invert LUT", "");
                rt.update(measurements, impg, null);
                IJ.run("Set Measurements...", MeasCommand + " redirect=" + RawStack + " decimal=3");
                IJ.run(impg, "Analyze Particles...", "size=" + "1" + "-Infinity pixel circularity=" + "0.0" + "-1.00 display exclude clear add stack");                          
                nROIs3 = rm.getCount();
                IJ.log("#ROIs after Convex Hull " + Integer.toString(nROIs3));
            }
//******
            imp.close();            
            imp2.close();     
            if (DoHull) {
                IJ.run(impg, "Invert", "stack");
            }
            ImagePlus impt = ic.run("And create stack", impr2, impg);
  if (ShowParticlesInImageStack) {
      IJ.run(impt, "Properties...", "channels=1 slices=1 frames="+StackSize+" unit=pixel pixel_width=1.0000 pixel_height=1.0000 voxel_depth=1.0000 frame=[1 sec]");
      impt.show();
 //                     IJ.saveAs(impt, "Tiff", path+name  + "T.tif");
  }

//Save Results Table as xls file 
            if (SaveResults) {
                String roiname;

                try {
                    rt.saveAs(fname);
                    IJ.log("Saved as " + name + runnumber + ".xls");

                } catch (IOException ex) {
                    Logger.getLogger(Variable_Threshold.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
            int[] RoiIndexArray = new int[nROIs3];
            for (i = 0; i <= nROIs3 - 1; i++) {
                RoiIndexArray[i] = i;
            }

            impc = Make_Collage(impr2, RoiIndexArray, LabelCollageParticles, labelinterval, maxcollageslices);

            if (SaveResults) {
                IJ.saveAs(impc, "Tiff", fname.substring(0, fname.length() - 4) + "Collage.tif");
            }
            impc.show();
impg.close();
            DoFilter.addMessage("press OK to perform Filters");
            DoFilter.addMessage("press Cancel to stop");
            DoFilter.showDialog();
            morefilter = !(DoFilter.wasCanceled());
        }
        while (morefilter) {
            ImagePlus impfil = Make_Filtered_Collage(impr2, rt, fname, LabelCollageParticles, labelinterval,maxcollageslices, SaveResults);
            impfil.show();
            GenericDialog DoFilter2 = new GenericDialog("Do Filter?");
            DoFilter2.removeAll();
            DoFilter2.revalidate();
            DoFilter2.addMessage("press OK to perform Filters");
            DoFilter2.addMessage("press Cancel to stop");
            DoFilter2.showDialog();
            morefilter = !(DoFilter2.wasCanceled());
        }
                    impr2.close();

if (SaveResults&&runtype!=1) {
                           String roiname;
         RoiManager rmB = RoiManager.getInstance();
                try {
                    rt.saveAs(fname);
                    IJ.selectWindow("Log");
                    IJ.saveAs("Text", path + name + "log" + runnumber + ".txt");
                    roiname = path + name + runnumber;
                    rmB.runCommand("Deselect");
                    rmB.runCommand("Save", roiname + ".zip");
                              } catch (IOException ex) {
                    Logger.getLogger(Variable_Threshold.class.getName()).log(Level.SEVERE, null, ex);
                }
            }  
//IJ.run(impR, "Close All", "");

Prefs.savePreferences();
    }


    public static String DefineMeasurements(int measurements) {
        int i;
        String MeasCommand="";

GenericDialog gdResults = new GenericDialog("Set Measurements", IJ.getInstance());

                String[] labels = new String[18];
                String[] Measure = new String[18];
                boolean[] states = new boolean[18];
                labels[0] = "Area";
                states[0] = (systemMeasurements & AREA) != 0;
                Measure[0] = "area";
                labels[1] = "Mean gray value";
                states[1] = (systemMeasurements & MEAN) != 0;
                Measure[1] = "mean";
                labels[2] = "Standard deviation";
                states[2] = (systemMeasurements & STD_DEV) != 0;
                Measure[2] = "standard";
                labels[3] = "Modal gray value";
                states[3] = (systemMeasurements & MODE) != 0;
                Measure[3] = "modal";
                labels[4] = "Min & max gray value";
                states[4] = (systemMeasurements & MIN_MAX) != 0;
                Measure[4] = "min";
                labels[5] = "Centroid";
                states[5] = (systemMeasurements & CENTROID) != 0;
                Measure[5] = "centroid";
                labels[6] = "Center of mass";
                states[6] = (systemMeasurements & CENTER_OF_MASS) != 0;
                Measure[6] = "center";
                labels[7] = "Perimeter";
                states[7] = (systemMeasurements & PERIMETER) != 0;
                Measure[7] = "perimeter";
                labels[8] = "Bounding rectangle";
                states[8] = (systemMeasurements & RECT) != 0;
                Measure[8] = "bounding";
                labels[9] = "Fit ellipse";
                states[9] = (systemMeasurements & ELLIPSE) != 0;
                Measure[9] = "fit";
                labels[10] = "Shape descriptors";
                states[10] = (systemMeasurements & SHAPE_DESCRIPTORS) != 0;
                Measure[10] = "shape";
                labels[11] = "Feret's diameter";
                states[11] = (systemMeasurements & FERET) != 0;
                Measure[11] = "feret's";
                labels[12] = "Integrated density";
                states[12] = (systemMeasurements & INTEGRATED_DENSITY) != 0;
                Measure[12] = "integrated";
                labels[13] = "Median";
                states[13] = (systemMeasurements & MEDIAN) != 0;
                Measure[13] = "median";
                labels[14] = "Skewness";
                states[14] = (systemMeasurements & SKEWNESS) != 0;
                Measure[14] = "skewness";
                labels[15] = "Kurtosis";
                states[15] = (systemMeasurements & KURTOSIS) != 0;
                Measure[15] = "kurtosis";
                labels[16] = "Area_fraction";
                states[16] = (systemMeasurements & AREA_FRACTION) != 0;
                Measure[16] = "area_fraction";
                labels[17] = "Stack position";
                states[17] = (systemMeasurements & STACK_POSITION) != 0;
                Measure[17] = "stack";
                gdResults.setInsets(0, 0, 0);
                gdResults.addCheckboxGroup(9, 2, labels, states);
                gdResults.showDialog();
                if (gdResults.wasCanceled()) {
                    return "";

                }
                int oldMeasurements = systemMeasurements;
                int previous = 0;
                boolean b = false;
                for (i = 0; i < list.length; i++) {
                    if (list[i]!=previous)
                      b = gdResults.getNextBoolean();
 //                     b=states[i];
                    previous = list[i];
                    if (b) {
                        measurements |= list[i];
                        MeasCommand = MeasCommand + Measure[i] + " ";
                    } else {
                        measurements &= ~list[i];
                    }
                }
systemMeasurements = measurements;
Prefs.savePreferences();
        return MeasCommand;
    }
    public static ImagePlus Remove_Background(ImagePlus imp) {
        int MedianStartSlice = 1;
//        int MedianEndSlice = 400;
        int MedianEndSlice = 30;
        double ScaleFactorLightPix = 0.0;
        String ScaleFactorLightPixSt = Double.toString(ScaleFactorLightPix);
        ImageCalculator ic = new ImageCalculator();
        MedianEndSlice = Math.min(MedianEndSlice, imp.getNSlices());
        GenericDialog gdBackgnd = new GenericDialog("Background Subtraction");
        gdBackgnd.addNumericField("Calc MedianStartSlice: ", MedianStartSlice, 0);
        gdBackgnd.addNumericField("Calc MedianEndSlice: ", MedianEndSlice, 0);
        gdBackgnd.addStringField("Calc ScaleFactorLightPix: ", ScaleFactorLightPixSt);
        gdBackgnd.showDialog();
        MedianStartSlice = (int) gdBackgnd.getNextNumber();
        MedianEndSlice = (int) gdBackgnd.getNextNumber();
        ScaleFactorLightPixSt = gdBackgnd.getNextString();
        ScaleFactorLightPix = Double.valueOf(ScaleFactorLightPixSt);
        ZProjector ZP = new ZProjector(imp);
        ZP.setMethod(ZP.MEDIAN_METHOD);
        ZP.setStartSlice(MedianStartSlice);
        ZP.setStopSlice(MedianEndSlice);
        ZP.doProjection();
        ImagePlus impm = ZP.getProjection();
 /*       impm.show();
        int food=5/0;*/
        ImagePlus impP = ic.run("Subtract create stack", imp, impm);
        ImagePlus impD = ic.run("Difference create stack", imp, impm);
        ImagePlus impN = ic.run("Subtract create stack", impD, impP);
        IJ.run(impP, "Multiply...", "value=" + ScaleFactorLightPixSt + " stack");
        ImagePlus impsubfiles = ic.run("Add create stack", impP, impN);
        impsubfiles.setTitle("subfiles");
        IJ.log("Calc ScaleFactorLightPix: " + ScaleFactorLightPixSt);
        return impsubfiles;
    }
    //Makes collage of particles

    public static ImagePlus Make_Collage(ImagePlus imp, int[] RoiArray, boolean LabelCollageParticles, int labelinterval, int maxcollageslices) {
        int framew = imp.getWidth();
        int frameh = imp.getHeight();
        int border = 7;
        int xp;
        int yp;
        int ypmax;
        int i;
        int w;
        int h;
        int x;
        int y;
        int xs;
        int ys;
        int z;
        boolean newpage=false;
        int labeloffsety = 5;
        int labeloffsetx = 7;
        Roi roiA;
        ImagePlus impw = IJ.createImage("Collage", "8-bit black", framew, frameh, 1);
        ImagePlus impm = IJ.createImage("Mask", "8-bit black", framew, frameh, 1);
        ij.WindowManager.setTempCurrentImage(impm);
        RoiManager roim = RoiManager.getInstance();
        xp = border;
        yp = border;
        String ROInumber = "";
        ypmax = 0;
        int xgap = 4;
        int ygap = 10;
        Overlay overlay1 = new Overlay();
        int slices = 0;
        int xb;
        int yb;
        for (i = 0; i < RoiArray.length; i++) {
            ROInumber = Integer.toString(i + 1);
            ij.WindowManager.setTempCurrentImage(imp);
            roim.select(RoiArray[i]);
            ij.WindowManager.setTempCurrentImage(impm);
            roim.select(RoiArray[i]);
            Roi roi = impm.getRoi();
            z = roi.getPosition();
            Rectangle r = roi.getBounds();
            w = r.width;
            h = r.height;
            x = r.x;
            y = r.y;
            if (xp + w + xgap + 2 * border > framew) {

                xp = border;
                yp = ypmax;
                ypmax = yp + h + ygap + 2 * border;
            } else if (yp + h + ygap + 2 * border > ypmax) {
                ypmax = yp + h + ygap + 2 * border;
            }
            if (yp + h + ygap + 2 * border > frameh) {
                newpage=true;
                slices++;
                if (slices<maxcollageslices) {
                IJ.run(impm, "Create Selection", "");
                Roi roi1 = impm.getRoi();
                roi1.setPosition(slices);
                overlay1.add(roi1);
                IJ.run(impw, "Add Slice", "");
                IJ.run(impm, "Add Slice", "");
                xp = border;
                yp = border;
                ypmax = yp + h + ygap + 2 * border;
                roim.select(RoiArray[i]);
                impm.setSlice(slices+1);
                roi = impm.getRoi();
                roi.setPosition(slices + 1);
                }
            }
            if (slices<maxcollageslices) {
            WindowManager.setTempCurrentImage(impm);
            xs = (int) roi.getXBase();
            ys = (int) roi.getYBase();
            roi.setLocation(xp, yp);
            impm.setSlice(slices+1);
            ImageProcessor ipm = impm.getProcessor();
            ipm.setColor(255);
            ipm.fill(roi.getMask());
            if (LabelCollageParticles) {
                if (i % labelinterval == 0) {
                    ipm.drawString(ROInumber, xp - labeloffsetx, yp + labeloffsety);
                }
            }
            WindowManager.setTempCurrentImage(imp);
            if (x - border < 0) {
                xb = x;
                w = w + border + x;
            } else {
                xb = border;
                w = w + 2 * border;
            }
            if (y - border < 0) {
                yb = y;
                h = h + border + y;
            } else {
                yb = border;
                h = h + 2 * border;
            }
            if (x + w + border > framew) {
                xb = border;
                w = framew - x + border;
            }
            if (y + h + border > frameh) {
                yb = border;
                h = frameh - y + border;
            }
            x = x - xb;
            y = y - yb;
            imp.setPosition(z);
            imp.setRoi(x, y, w, h);
            IJ.run(imp, "Copy", "");
            WindowManager.setTempCurrentImage(impw);
            impw.setRoi(xp - xb, yp - yb, w, h);
            IJ.run(impw, "Paste", "");
            xp = xp + w + xgap;
            roi.setLocation(xs, ys);
            if (newpage) {                
                roi.setPosition(z);
                newpage=false;}
            } else {break;}
            
        }
        slices++;
        IJ.run(impm, "Create Selection", "");
        roiA = impm.getRoi();
        roiA.setPosition(slices);
        overlay1.add(roiA);
        impw.setOverlay(overlay1);
        impw.show();
        WindowManager.setTempCurrentImage(impw);
        roim.runCommand("Show All");
        roim.runCommand("Show None");
 //       roim.select(1);
        IJ.run("Overlay Options...", "stroke=blue width=1 fill=none set");
        impw.show();
        return impw;
    }
    public static ImagePlus Make_Filtered_Collage(ImagePlus imp, ResultsTable rt, String fname, boolean LabelCollageParticles, int labelinterval,int maxcollageslices, boolean SaveResults) {
//Filter results and generate a new collage
        int framew = imp.getWidth();
        int frameh = imp.getHeight();
        ImagePlus impfil = IJ.createImage("Collage", "8-bit black", framew, frameh, 1);;
        int i;
        int k;
        RoiManager roim = RoiManager.getInstance();
        int nROIs3 = roim.getCount();
        int[] RoiIndexArray = new int[nROIs3];
        String headingsStr = rt.getColumnHeadings();
        IJ.log(headingsStr + Integer.toString(nROIs3));
        String[] headings = Arrays.copyOfRange(headingsStr.split("\\s+"), 1, (headingsStr.split("\\s+")).length);;
        boolean[] filter = new boolean[headings.length];
        int[] SetfilterRanges = new int[headings.length];
        int[] Columns = new int[headings.length];
        int ia = 0;
        int[] ROIFilterArray;
        int[] deleterows = new int[nROIs3];
        boolean inrange = true;
        int fdrows = headings.length / 2;
        if (fdrows * 2 != headings.length) {
            fdrows = fdrows + 1;
        }
        int jj = 0;
        double testvalue = 0;
        GenericDialog fd = new GenericDialog("Filter Options");
        fd.setInsets(0, 0, 0);
        fd.addCheckboxGroup(fdrows, 2, headings, filter);
        fd.showDialog();
        for (i = 0; i < headings.length; i++) {
            while (!rt.columnExists(ia)) {
                ia++;
            }
            Columns[i] = ia;
            ia++;
            filter[i] = fd.getNextBoolean();
            if (filter[i]) {
                SetfilterRanges[jj] = i;
                jj++;
            }
        }
        if (jj > 0) {
            GenericDialog frd = new GenericDialog("Filter Ranges");
            int gridWidth = 2;
            int gridHeight = jj;
            Double d;
            String Filtname = "Filter";
            String st = "";
            int gridSize = gridWidth * gridHeight;
            TextField[] tf = new TextField[gridSize];
            double[] value = new double[gridSize];
            Panel panel = new Panel();
            panel.setLayout(new GridLayout(gridHeight, gridWidth));
            for (k = 0; k < jj; k++) {
                panel.add(new Label(headings[SetfilterRanges[k]] + " "));
                tf[2 * k] = new TextField("" + value[2 * k]);
                tf[2 * k + 1] = new TextField("" + value[2 * k + 1]);
                panel.add(tf[2 * k]);
                panel.add(tf[2 * k + 1]);
            }
            frd.addPanel(panel);
            frd.showDialog();
            for (k = 0; k < jj; k++) {
                st = tf[2 * k].getText();
                try {
                    d = new Double(st);
                } catch (NumberFormatException e) {
                    d = null;
                }
                value[2 * k] = d;
                st = tf[2 * k + 1].getText();
                try {
                    d = new Double(st);
                } catch (NumberFormatException e) {
                    d = null;
                }
                value[2 * k + 1] = d;
                Filtname = Filtname + headings[SetfilterRanges[k]] + Double.toString(value[2 * k]) + "-" + Double.toString(value[2 * k + 1]);
            }
            ia = 0;
            int ib = 0;
            ResultsTable rtselect = (ResultsTable) rt.clone();
            rtselect.show(Filtname);
            for (i = 0; i < nROIs3; i++) {
                inrange = true;
                for (k = 0; k < jj; k++) {
                    testvalue = rtselect.getValueAsDouble(Columns[SetfilterRanges[k]], i);
                    inrange = inrange & ((testvalue > value[2 * k]) & (testvalue < value[2 * k + 1]));
                }
                if (inrange) {
                    RoiIndexArray[ia] = i;
                    ia++;
                } else {
                    deleterows[ib] = i;
                    ib++;
                }
            }
            for (i = ib - 1; i > -1; i--) {
                rtselect.deleteRow(deleterows[i]);
            }
            rtselect.updateResults();
            rtselect.show(Filtname);
            fname = fname.substring(0, fname.length() - 4);
            if (SaveResults) {
                try {
                    rtselect.saveAs(fname + Filtname + ".xls");
                } catch (IOException ex) {
                    Logger.getLogger(Variable_Threshold.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            ROIFilterArray = Arrays.copyOf(RoiIndexArray, ia);
            impfil = Make_Collage(imp, ROIFilterArray, LabelCollageParticles, labelinterval, maxcollageslices);
            impfil.setTitle("Collage" + Filtname);
            if (SaveResults) {
                IJ.saveAs(impfil, "Tiff", fname + Filtname + "Collage.tif");
            }
            impfil.show();
        }
        return impfil;
    }

}
