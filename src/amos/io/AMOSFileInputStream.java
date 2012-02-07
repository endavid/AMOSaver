package amos.io ;

import java.io.File ;
import java.io.FileInputStream ;
import java.io.FileNotFoundException ;

/**
 * @see http://www.exotica.org.uk/wiki/AMOS_file_formats
 * @see http://www.amigacoding.com/index.php/AMOS:Picture_Bank_format
 */
public class AMOSFileInputStream
{
    public static final String[] VALID_HEADERS = {
        // 16-byte header, Tested?, Saved from which AMOS?
        "AMOS Pro101V\0\0\0\0",//	Yes	AMOS Professional
        "AMOS Basic V134 ",//	Yes	AMOS Pro, but AMOS 1.3 compatible
        "AMOS Basic V1.3 ",//	Yes	AMOS The Creator v1.3
        "AMOS Basic V1.00",//	Yes	AMOS The Creator v1.0 - v1.2
        "AMOS Pro101v\0\0\0\0",//	No	AMOS Professional
        "AMOS Pro101v\0\0\0\3",//	No	AMOS Professional
        "AMOS Basic v134 ",//	No	AMOS Pro, but AMOS 1.3 compatible
        "AMOS Basic v1.3 ",//	No	AMOS The Creator v1.3
        "AMOS Basic v1.00"//	No	AMOS The Creator v1.0 - v1.2
    };
    FileInputStream m_stream ;
    boolean m_isSanityTested ;
    long    m_numBASICTokens ;
    
    public boolean isSanityTested()
    {
        return m_isSanityTested ;
    }
    
    public AMOSFileInputStream(java.io.File file) 
        throws java.io.FileNotFoundException, amos.io.UnsupportedFormat, java.io.IOException
    {
        m_stream = new FileInputStream(file);
        String headerString ;
        try {
            byte[] header = new byte[16];
            m_stream.read(header);
            headerString = new String(header);
            
        } catch (java.io.IOException exc) {
            throw( new amos.io.UnsupportedFormat("I/O error") );
        }
        
        if (!isValidHeader(headerString)) {
            throw( new amos.io.UnsupportedFormat(headerString) );
        }
        
        byte[] bigEndianUnsigned = new byte[4];
        m_stream.read(bigEndianUnsigned);
        // there's no unsigned int in Java, so store it in a long
        m_numBASICTokens = readUnsignedInt(bigEndianUnsigned);

        System.out.println("ntokens: "+m_numBASICTokens);

    }
    
    public boolean isValidHeader(String header)
    {
        m_isSanityTested = true ;
        for (int i=0; i<4; ++i) {
            if (header.equals(VALID_HEADERS[i])) return true ;
        }
        m_isSanityTested = false ;
        for (int i=4; i<VALID_HEADERS.length; ++i) {
            if (header.equals(VALID_HEADERS[i])) return true ;
        }
        return false ;
    }
    
    public static long readUnsignedInt(byte[] bigEndianUnsigned)
    {
        long value = 0xFFFFFFFFL & (long)(
                ((0x000000ff & (int)bigEndianUnsigned[0]) << 24) |
                ((0x000000ff & (int)bigEndianUnsigned[1]) << 16) |
                ((0x000000ff & (int)bigEndianUnsigned[2]) <<  8) |
                ((0x000000ff & (int)bigEndianUnsigned[3]) ) );
        return value ;
    }
    
}