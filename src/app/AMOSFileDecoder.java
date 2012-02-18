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
        if (args.length == 0) {
            System.out.println( "Usage: AMOSFileDecoder FILE" );
        } else {
            try {
                File file = new File( args[0] );
                AMOSFileInputStream fileDecoder = new AMOSFileInputStream(file);
                
                // Decode source code
                while (!fileDecoder.isSourceCodeEnd()) {
                    System.out.println( fileDecoder.readLine() );
                }
                
                // Read memory banks
                int numBanks = fileDecoder.readNumBanks();
                System.out.println("Num banks: "+numBanks);
                
                // process banks
                for(int i=0;i<numBanks;i++) {
                    AMOSBankType bankType = fileDecoder.readBankType();
                    switch(bankType) {
                        case SPRITEBANK:
                        {
                            System.out.println("SpriteBank");
                            String format = "png";

                            List<BufferedImage> imgList = fileDecoder.readImages();
                            int count = 0;
                            for (Iterator<BufferedImage> it = imgList.iterator(); it.hasNext(); ) {
                                count++;
                                BufferedImage img = it.next();
                                File imgfile = new File(String.format("Sprite_%03d.png",count));
                                ImageIO.write(img, format, imgfile);
                            }
                            break;
                        }
                        case ICONBANK:
                        {
                            System.out.println("IconBank");
                            fileDecoder.readImages();
                            break;
                        }
                        case MEMORYBANK:
                        {
                            System.out.println("MemoryBank");
                            break;
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
    }
}