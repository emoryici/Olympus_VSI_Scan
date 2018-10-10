//  Integrated Cellular Imaging (ICI) - Emory University
//  Integrated Core Facilities
//
//  Neil R. Anthony  -  3/11/2016
//  convert selected series image in all vsi files 
//  creates a composite image which is saved as a tif, and then a reduced size thumbnail, saved as a png for easy viewing/reference


dir = getDirectory("Choose a Directory ");
count = 1;
default_series = 0;
bf_str = "";
filename = "";
filename2 = "";
str = "";
save_str = "";
series = getNumber("Series #", default_series); 

list = getFileList(dir);
//print("\\Clear");

for (n=0; n<list.length; n++) {
	if (endsWith(list[n], ".vsi")){
    	
    	bf_in_str = "open=[" + dir + list[n] + "] autoscale color_mode=Colorized view=Hyperstack stack_order=XYCZT series_" + series;
    	//print(bf_in_str);
    	run("Bio-Formats Importer", bf_in_str);
    	filename = replace(list[n], ".vsi", "_series" + series + ".tif");
    	//print(filename);
    	bf_out_str = dir + filename;
    	setSlice(1);
		run("Blue");
		run("Enhance Contrast", "saturated=0.35");
		setSlice(2);
		run("Enhance Contrast", "saturated=0.35");
		setSlice(3);
		run("Red");
		run("Enhance Contrast", "saturated=0.35");
		run("Make Composite");
    	saveAs("Tiff", bf_out_str);
    	filename2 = replace(list[n], ".vsi", "_series" + series + "_thmb.png");
    	str = "x=- y=- z=1.0 width=500 height=377 depth=3 interpolation=Bilinear average create title=[" + filename2 + "]";
    	run("Scale...", str);
    	save_str = dir + filename2;
    	saveAs("PNG", save_str);
		run("Close All");
		}
	}


