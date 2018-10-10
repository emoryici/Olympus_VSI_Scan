package res;

import ij.IJ;
import ij.io.DirectoryChooser;
import java.util.Vector;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.WindowConstants;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.ImageReader;
import loci.formats.ImageWriter;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import loci.plugins.in.ImporterOptions;
import loci.formats.tools.BF_ImageConverter;

/**
 * 
 * @author Neil Anthony <neil.anthony@emory.edu>
 */
public class VSI_Size_Combo_Ctrl {

        //  Vector containing size strings that all files contain
    protected static Vector<String> sizesVector = new Vector<>();
    //  list of path and filenames for convert class
    final static ArrayList<String> fileList = new ArrayList<>();
    //  2D list holding series location for each size
    protected static ArrayList<ArrayList<Integer>> seriesNumberList = new ArrayList<>();
    
    //  list to store the number of series items found in each file
    private static ArrayList<Integer> seriesList = new ArrayList<>();
    //  list to store the number of channels for each series
    private ArrayList<ArrayList<Integer>> chansLists = new ArrayList<>();
    //  list to populat above chans list
    private ArrayList<Integer> chnList = new ArrayList<>();
    //  list to store boolean indicator of hash list present or not
    private ArrayList<String> hashPresentList = new ArrayList<>();
    //  list of pointslist (sizes stored as Point); one for each file
    private ArrayList<ArrayList<Point>> imgSizesList = new ArrayList<>();
    // list to populate and add for each file
    private ArrayList<Point> pntsList = new ArrayList<>();
    //  list for checking
    private ArrayList<Point> chkList = new ArrayList<>();

    
    public static VSI_Size_Combo_Frame comboForm;
    
    public VSI_Size_Combo_Ctrl(Boolean scanOnly) {
        VSI_Scan_Files();
        if (!scanOnly){
            VSI_Compare_Sizes();
            comboForm = new VSI_Size_Combo_Frame(sizesVector);
            comboForm.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
            Point IJ_locPnt = IJ.getInstance().getLocation();
            comboForm.setLocation(IJ_locPnt.x, IJ_locPnt.y+100);
            comboForm.setVisible(true);
        }
       
    }    
    
    protected void VSI_Scan_Files(){
        // prompt user for directory to process
        DirectoryChooser dc = new DirectoryChooser("Select VSI Images Folder");
        String dirPath = dc.getDirectory();
        ImageReader ImR = new ImageReader();  // image reader object

        // process all files in the chosen directory
        File dir = new File(dirPath);
        File[] files = dir.listFiles();
        IJ.showStatus("Scanning directory");

        Hashtable<String, Object> Htab = new Hashtable<>();
        
        String Hstr;
        int HtabPres;
        String key;
        String sizesStr;
        int series;
        String logStr;
        
        String id;

        for (int i=0; i<files.length; i++) {  //  for all files in the given folder
            
            id = files[i].getAbsolutePath();
            IJ.showProgress((double) i / files.length);

            String name = files[i].getName();
            if (name.endsWith(".vsi")) fileList.add(id);  //  create list that are vsi files
            else continue;
            
            try {
                ImR.setId(id);  //  give file full path to image reader, ImR
                } catch (FormatException | IOException ex) {
                Logger.getLogger(VSI_Size_Combo_Ctrl.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Exception e) {
                IJ.log(e.toString() + " Error with file " + id);
                }
            //  empty lists for this file being scanned
            pntsList.clear();  //  points list, sizes stored as points
            chnList.clear();  //  number of chans for each size
            Htab.clear();  //  hash table of metadata

            series = ImR.getSeriesCount();  //  number of series entries in this file
            seriesList.add(series);  //  record file series number in the list

            Hstr = "(";
            for (int j = 0; j < series; j++) {  //  for every series # in the file   ********************************
                ImR.setSeries(j);
                Htab = ImR.getSeriesMetadata();
                HtabPres = 0;
                if (Htab.size()>0) HtabPres = 1;
                Hstr += Integer.toString(HtabPres) + ";";
                pntsList.add(j, new Point(ImR.getSizeX(), ImR.getSizeY()));  //  new size point added for each series #
                chnList.add(ImR.getSizeC());  //  add number of channels for each series #

                }  //  ***************************
            
            imgSizesList.add(new ArrayList<>(pntsList));  // add created points list to the list of points lists
            chansLists.add(new ArrayList<>(chnList));  //  add channel numbers to list of lists

            sizesStr = "";
            for (int k = 0; k < pntsList.size(); k++) { //  create string detailing all sizes for report   ********************************
                sizesStr += "(" + Integer.toString(pntsList.get(k).x)
                        + ":" + Integer.toString(pntsList.get(k).y) + "),";
            }  //  ***************************
            sizesStr = sizesStr.substring(0, sizesStr.length()-1) + "";
            
            Hstr = Hstr.substring(0, Hstr.length()-1) + ")";
            hashPresentList.add(Hstr);

            logStr = name + ", "                        //  filename
                    + Integer.toString(series) + ", "   //  number of series in file
                    + Hstr + ", "                       //  boolean list for available hashtable metadata
                    + sizesStr;                         //  list of image sizes

            IJ.log(logStr);

        }

        IJ.showProgress(1.0);
        IJ.showStatus("");

        try {
            ImR.close();
        } catch (IOException ex) {
            Logger.getLogger(VSI_Size_Combo_Ctrl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
        
    protected void VSI_Compare_Sizes(){
        //  check for sizes available in all images ****************************************
        //  -- Get an array index that contains the max number of series and that max number 
        int max = 0;
        int maxIndx = 0;
        int series;
        String sizesStr;
        
        for (int i = 0; i < seriesList.size(); i++) {
            series = seriesList.get(i);
            if (series>max) {
                max = series;
                maxIndx = i;
            }
        }
        
        //  -- Compare all sizes in maxIndex points list to all other lists
        //  aim here is to retain only the files that have a consistent set of image sizes
        pntsList = imgSizesList.get(maxIndx);  //  PointListMax
        chnList = chansLists.get(maxIndx);  //  chanslist to accompany
        //  found in all list  --  list of found results for checking across all images
        ArrayList<Boolean> foundList = new ArrayList<>();
        //  list for hodling the series # for each size comparison
        ArrayList<Integer> seriesN = new ArrayList<>();  
        //  clear all lists 
        sizesVector.clear();
        seriesNumberList.clear();
        
        int ic = 0;  //  increment index for adding to Vector and seriesNumberList
        Boolean found;  //  found in points for loop
        Boolean foundInAll;  //  found in all points lists
        Point pnt, pntChk;  //  current and to be cross checked points
        IJ.showStatus("Comparing Sizes");
        
        
        for (int i = 0; i < pntsList.size(); i++) {  //  for all points/sizes in the list with max number of series
            
            IJ.showProgress((double) i / pntsList.size());
            
            pnt = pntsList.get(i);
            seriesN.clear();
            foundList.clear();
            
            //  for all pointslists in the dimslist check current Points size exists
            for (int j = 0; j < imgSizesList.size(); j++) {   //  for all files
                
                chkList = imgSizesList.get(j);
                found = false;
                
                for (int k = 0; k < chkList.size(); k++) {   //  for all sizes
                    
                    pntChk = chkList.get(k);
                    if (pnt.equals(pntChk)) {
                        found = true;
                        seriesN.add(j, k);
//                        IJ.log("pnt: " + pnt.toString() + ", pntChk: " + pntChk.toString()
//                                + "   " + Integer.toString(i) + ", " + Integer.toString(j));
                        break;
                    }
                }
                foundList.add(j, found);
            }
            //  check the found list for agreement
            foundInAll = true;
            for (int n = 0; n < foundList.size(); n++) {
                if (!foundList.get(n)) {
                    foundInAll = false;
                }
            }
            
            //  if found in all add info to vector and lists for combo and convert
            if (foundInAll){
                //  add current Points size to combo Vector string
                sizesStr = "(" + Integer.toString(pntsList.get(i).x)
                        + "," + Integer.toString(pntsList.get(i).y) + ")"
                        + "  " + Integer.toString(chnList.get(i)) + "C"
                        + " s# " + Integer.toString(i+1);  //  series in BF is 1 to N
                sizesVector.add(ic, sizesStr);
//                IJ.log(sizesStr);
                seriesNumberList.add(ic, new ArrayList<>(seriesN));
//                IJ.log(seriesN.toString());
                ic++;
            }
        }
        IJ.showProgress(1.0);
        IJ.showStatus("Found " + Integer.toString(ic) + " common image sizes.");
    }

    protected static void VSI_Convert(int idx) throws IOException, FormatException {
        
        
        //  setup reader and metadata
        ImageReader reader = new ImageReader();
        IMetadata metadata = null;
        try {
            ServiceFactory factory = new ServiceFactory();
            OMEXMLService service = factory.getInstance(OMEXMLService.class);
            metadata = service.createOMEXMLMetadata();
        }
        catch (DependencyException | ServiceException exc) {
            try {
                throw new FormatException("Could not create OME-XML store.", exc);
            } catch (FormatException ex) {
                Logger.getLogger(VSI_Size_Combo_Ctrl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        reader.setMetadataStore(metadata);
        
        //  setup writer
        ImageWriter writer = new ImageWriter();
        writer.setMetadataRetrieve(metadata);
//        MetadataRetrieve retrieve;
//        retrieve = reader.getMetadataStore();
//        writer.setMetadataRetrieve(retrieve);
        
       
        String out_id;
        String in_id;
        int series;
        seriesList.clear();
        //List<CoreMetadata> coreMeta = new ArrayList<CoreMetadata>();
        
        ImporterOptions options = new ImporterOptions();
        
        
        for (int i = 0; i < fileList.size(); i++) {
            
            seriesList = seriesNumberList.get(idx);  //  get list of series #'s for this file
            
            try {
                in_id = fileList.get(i);
                out_id = in_id.replace(".vsi", ".tif");
                series = seriesList.get(i);
                        
                reader.setId(in_id);
                writer.setId(out_id);
                reader.setSeries(series);
                writer.setSeries(series);
                
                byte[] imgBytes = reader.openBytes(0);
                writer.saveBytes(0, imgBytes);
                
                
 
            } catch (IOException ex) {
                Logger.getLogger(VSI_Size_Combo_Ctrl.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            
            
        }
        
    }
    
    public static void ConvertFiles(int idx) throws FormatException, IOException{
        BF_ImageConverter imgCon = new BF_ImageConverter();
        ImageWriter writer = new ImageWriter();
        
        String out_id;
        String in_id;
        //Path path;
        //String filename;
        int series;
        seriesList.clear();
        
        for (int i = 0; i < fileList.size(); i++) {
            
            seriesList = seriesNumberList.get(idx);  //  get list of series #'s for this file
            
            in_id = fileList.get(i);
            //path = Paths.get(in_id);
            //filename = path.getFileName().toString();
            //out_id = filename.replace(".vsi", ".tif");
            out_id = in_id.replace(".vsi", ".tif");
            
            series = seriesList.get(i);
            
            String[] args = new String[5];
            args[0] = "-series";
            args[1] = Integer.toString(series);
            args[2] = "-overwrite";
            args[3] = in_id;
            args[4] = out_id;
            imgCon.testConvert(writer, args);
        }
    }

}
