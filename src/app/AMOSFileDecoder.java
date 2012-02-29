// Copyright (C) 2012 David Gavilan Ruiz
// 
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
// 
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
package app ;

import java.io.File ;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import javax.imageio.ImageIO;
import amos.io.* ;

public class AMOSFileDecoder {

    public static void main(String args[]) {
        // default arguments
        String sourceFile = "";
        boolean isVerbose = false;
        boolean isSourceOnly = false;
        String imageFolder = "";
        String dataFolder = "";
        // parse arguments
        int argIndex = 0;
        while(argIndex < args.length) {
            if (args[argIndex].equals("--sourceonly")) {
                isSourceOnly = true ;
            } else if (args[argIndex].equals("--verbose") || args[argIndex].equals("-v")) {
                isVerbose = true;
            } else if (args[argIndex].equals("--imagefolder")) {
                if (argIndex+1<args.length) {
                    imageFolder = args[++argIndex];
                }
            } else if (args[argIndex].equals("--datafolder")) {
                if (argIndex+1<args.length) {
                    dataFolder = args[++argIndex];
                }
            } else if (argIndex!=args.length-1) {
                // wrong arguments
                argIndex = args.length-1;
            } else {
                sourceFile = args[argIndex];
            }
            ++argIndex;
        }
        
        if (sourceFile.isEmpty()) {
            printHelp();
        } else {
            try {
                File file = new File( sourceFile );
                AMOSFileInputStream fileDecoder = new AMOSFileInputStream(file, isVerbose);
                
                // Decode source code
                while (!fileDecoder.isSourceCodeEnd()) {
                    System.out.println( fileDecoder.readLine() );
                }
                
                if (!isSourceOnly) {
                    // Read memory banks
                    int numBanks = fileDecoder.readNumBanks();
                    if (isVerbose) {
                        System.err.println("Decoding "+numBanks+" banks...");
                    }
                    
                    // process banks
                    for(int i=0;i<numBanks;i++) {
                        AMOSBankType bankType = fileDecoder.readBankType();
                        if (bankType == AMOSBankType.MEMORYBANK) { // subtype
                            bankType = fileDecoder.readMemoryBankType();
                        }
                        switch(bankType) {
                            case SPRITEBANK:
                            {
                                String format = "png";                                
                                List<BufferedImage> imgList = fileDecoder.readImages();
                                int count = 0;
                                for (Iterator<BufferedImage> it = imgList.iterator(); it.hasNext(); ) {
                                    count++;
                                    BufferedImage img = it.next();
                                    File imgfile = new File(imageFolder+String.format("Sprite_%03d.png",count));
                                    ImageIO.write(img, format, imgfile);
                                }
                                break;
                            }
                            case ICONBANK:
                            {
                                String format = "png";                                
                                List<BufferedImage> imgList = fileDecoder.readImages();
                                int count = 0;
                                for (Iterator<BufferedImage> it = imgList.iterator(); it.hasNext(); ) {
                                    count++;
                                    BufferedImage img = it.next();
                                    File imgfile = new File(imageFolder+String.format("Icon_%03d.png",count));
                                    ImageIO.write(img, format, imgfile);
                                }
                                break;
                            }
                            case PACKED_PICTURE:
                            {
                                String format = "png";
                                BufferedImage img = fileDecoder.readPacPic();
                                File imgFile = new File(dataFolder+String.format("PacPic_%02d.png",fileDecoder.getCurrentBankNumber()));
                                ImageIO.write(img, format, imgFile);
                                break;
                            }
                            default:
                            case MEMORYBANK: // Generic memory bank
                            {
                                fileDecoder.readMemoryBankRaw();
                                break;
                            }
                        }
                    }
                }
                
            } catch (java.io.FileNotFoundException exc) {
                System.err.println( "" + exc );
            } catch (amos.io.UnsupportedFormat exc) {
                System.err.println( "" + exc );
            } catch (java.io.StreamCorruptedException exc) {
                System.err.println( "" + exc );
            } catch (java.io.IOException exc) {
                System.err.println( "" + exc );
            }
        }
    } // end main()
    
    /**
     * Prints help
     */
    public static void printHelp() {
        System.out.println( "Usage: AMOSFileDecoder [options] AMOS_SOURCE_FILE" );
        System.out.println( "Options:" );
        System.out.println( "  -v | --verbose: outputs more information" );        
        System.out.println( "  --sourceonly: decode only the source code");
        System.out.println( "  --imagefolder PATH: output images to PATH");
        System.out.println( "  --datafolder PATH: output memory banks to PATH");
    }
}