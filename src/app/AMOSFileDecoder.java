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
                
            } catch (java.io.FileNotFoundException exc) {
                System.err.println( "" + exc );
            } catch (amos.io.UnsupportedFormat exc) {
                System.err.println( "" + exc );
            } catch (java.io.IOException exc) {
                System.err.println( "" + exc );
            }
        }
    }
}