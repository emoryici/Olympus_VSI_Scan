# Olympus_VSI_Scan_Convert

### Scan

Generate report of folder containing .vsi files.  Reports image filename, number of images in file, boolean list of which sub-images have metadata, and a list of file sizes.  Save report as .csv file and open in Excel.  If certain files produce errors or contain vastly different numbers of sub-images (called series in Fiji) check the number of subfolders in the data folder sharing the same name as the vsi file.  Move additional subfolders to temp location and rescan.

Example:
mytissue.vsi, 10, (1;0;0;1;0;0;0;0;0;0), (1377:1038),(689:519),(345:260),(13853:10445),(6927:5223),(3464:2612),(1732:1306),(866:653),(433:327),(512:386)

filename, #images, boolean list of hashtables present, (x,y) pixel sizes of images

Note, in example, images 1,2,3 are pictures, and the second set starting from the second hashtable are datasets.

### Conversion

Use below macro (right click and Save As... and then drag onto Fiji) to convert specified sub-image/series within vsi file to tif (also saves png thumbnail for easy viewing).  Select common size from above report, using the series number counted along the columns.  Note, the first sizes listed prior to the largest are commonly image files, not data files.
