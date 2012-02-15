package app ;

import java.io.File ;
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
                            fileDecoder.readImages();
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