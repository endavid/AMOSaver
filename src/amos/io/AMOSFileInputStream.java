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
    long    m_sourceSizeBytes ;
    long    m_readBytes ;
    byte[]  m_tmp4B = {0,0,0,0};
    byte[]  m_tmp2B = {0,0};
    byte[]  m_tmp1B = {0};
    
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
        
        m_stream.read(m_tmp4B); // big endian unsigned
        // there's no unsigned int in Java, so store it in a long
        m_sourceSizeBytes = readUnsignedInt(m_tmp4B);
        m_readBytes = 0;

        //System.out.println("source size: "+m_sourceSizeBytes);
    }
    
    /**
     * Checks if there are still tokens to read
     */
    public boolean isSourceCodeEnd()
    {
        return ( m_readBytes >= m_sourceSizeBytes );
    }
    
    /**
     * Reads tokens from a line of AMOS BASIC code
     */
    public String readLine() throws java.io.IOException, java.io.StreamCorruptedException
    {
        String line = "";
        m_stream.read(m_tmp1B); // unsigned byte
        int lineLength = readUnsignedByte(m_tmp1B) ; // in words (2 bytes)
        m_readBytes += lineLength * 2 ; // in bytes
        
        m_stream.read(m_tmp1B);
        int indentLevel = readUnsignedByte(m_tmp1B) ;
        for (int i=1; i<indentLevel; ++i) line = line + " ";
        
        int tokenID = 0 ;
        //System.out.println("line l: "+lineLength+" indent: "+indentLevel);

        
        // big parsing loop
        int readWords = 1 ;
        while (readWords<lineLength) {
            m_stream.read(m_tmp2B);
            ++readWords ;
            tokenID = readUnsignedWord(m_tmp2B);
            switch(tokenID) {
                case 0: // NULL token
                    break ;
                // Specially printed tokens
                // -------------------------------------------
                case 0x0006: // Variable reference
                {
                    m_stream.read(m_tmp2B); // unknown purpose 
                    m_stream.read(m_tmp1B); // string size 
                    int strlength = readUnsignedByte(m_tmp1B);
                    if ( (strlength%2)==1 ) strlength += 1; // round up to words
                    m_stream.read(m_tmp1B); // flag
                    int flag = readUnsignedByte(m_tmp1B);
                    byte[] str = new byte[strlength];
                    m_stream.read(str);
                    readWords += 2 + (strlength>>1);
                    line = line + (new String(str)) ;
                    if ( (flag&0x01)!=0 ) {
                        line = line + "#" ; // float reference
                    } else if ( (flag&0x02)!=0 ) {
                        line = line + "$" ; // string reference
                    }
                    //m_tmp2B[0] = str[strlength-2];
                    //m_tmp2B[1] = str[strlength-1];
                    //tokenID = readUnsignedWord(m_tmp2B);
                    break;
                }
                case 0x000C: // Label
                {
                    m_stream.read(m_tmp2B); // unknown purpose 
                    m_stream.read(m_tmp1B); // string size 
                    int strlength = readUnsignedByte(m_tmp1B);
                    if ( (strlength%2)==1 ) strlength += 1; // round up to words
                    m_stream.read(m_tmp1B); // flag
                    int flag = readUnsignedByte(m_tmp1B);
                    byte[] str = new byte[strlength];
                    m_stream.read(str);
                    readWords += 2 + (strlength>>1);
                    line = line + (new String(str)) +":" ;
                    break;
                }
                case 0x0012: // Procedure call reference
                {
                    m_stream.read(m_tmp2B); // unknown purpose 
                    m_stream.read(m_tmp1B); // string size 
                    int strlength = readUnsignedByte(m_tmp1B);
                    if ( (strlength%2)==1 ) strlength += 1; // round up to words
                    m_stream.read(m_tmp1B); // flag
                    int flag = readUnsignedByte(m_tmp1B);
                    byte[] str = new byte[strlength];
                    m_stream.read(str);
                    readWords += 2 + (strlength>>1);
                    line = line + (new String(str)) ;
                    if ( (flag&0x01)!=0 ) {
                        line = line + "#" ; // float reference
                    } else if ( (flag&0x02)!=0 ) {
                        line = line + "$" ; // string reference
                    }
                    break;
                }
                case 0x0018: // Label reference
                {
                    m_stream.read(m_tmp2B); // unknown purpose 
                    m_stream.read(m_tmp1B); // string size 
                    int strlength = readUnsignedByte(m_tmp1B);
                    if ( (strlength%2)==1 ) strlength += 1; // round up to words
                    m_stream.read(m_tmp1B); // flag
                    int flag = readUnsignedByte(m_tmp1B);
                    byte[] str = new byte[strlength];
                    m_stream.read(str);
                    readWords += 2 + (strlength>>1);
                    line = line + (new String(str)) ;
                    if ( (flag&0x01)!=0 ) {
                        line = line + "#" ; // float reference
                    } else if ( (flag&0x02)!=0 ) {
                        line = line + "$" ; // string reference
                    }
                    break;
                }
                case 0x0026: // String with double quotes
                {
                    m_stream.read(m_tmp2B); // length of the string 
                    int strlength = readUnsignedWord(m_tmp2B);
                    if ( (strlength%2)==1 ) strlength += 1; // round up to words
                    byte[] str = new byte[strlength];
                    m_stream.read(str);
                    readWords += 1 + (strlength>>1);
                    line = line + "\""+(new String(str))+"\"" ;
                    break;
                }
                case 0x002E: // String with single quotes
                {
                    m_stream.read(m_tmp2B); // length of the string 
                    int strlength = readUnsignedWord(m_tmp2B);
                    if ( (strlength%2)==1 ) strlength += 1; // round up to words
                    byte[] str = new byte[strlength];
                    m_stream.read(str);
                    readWords += 1 + (strlength>>1);
                    line = line + "\'"+(new String(str))+"\'" ;
                    break;
                }
                // Specially sized tokens
                // -------------------------------------------
                case 0x023C: // FOR
                {
                    line = line + "For ";
                    m_stream.read(m_tmp2B); // unknown purpose
                    readWords += 1;
                    break;
                }
                case 0x0250: // REPEAT
                {
                    line = line + "Repeat ";
                    m_stream.read(m_tmp2B); // unknown purpose
                    readWords += 1;
                    break;
                }
                case 0x0268: // WHILE
                {
                    line = line + "While ";
                    m_stream.read(m_tmp2B); // unknown purpose
                    readWords += 1;
                    break;
                }
                case 0x027E: // DO
                {
                    line = line + "Do ";
                    m_stream.read(m_tmp2B); // unknown purpose
                    readWords += 1;
                    break;
                }
                case 0x02BE: // IF
                {
                    line = line + "If ";
                    m_stream.read(m_tmp2B); // unknown purpose
                    readWords += 1;
                    break;
                }
                case 0x02D0: // ELSE
                {
                    line = line + "Else ";
                    m_stream.read(m_tmp2B); // unknown purpose
                    readWords += 1;
                    break;
                }
                case 0x0404: // DATA
                {
                    line = line + "Data ";
                    m_stream.read(m_tmp2B); // unknown purpose
                    readWords += 1;
                    break;
                }
                case 0x0290: // EXIT IF
                {
                    line = line + "Exit If ";
                    m_stream.read(m_tmp4B); // unknown purpose
                    readWords += 2;
                    break;
                }
                case 0x029E: // EXIT
                {
                    line = line + "Exit ";
                    m_stream.read(m_tmp4B); // unknown purpose
                    readWords += 2;
                    break;
                }
                case 0x0316: // ON
                {
                    line = line + "On ";
                    m_stream.read(m_tmp4B); // unknown purpose
                    readWords += 2;
                    break;
                }                    
                case 0x0376: // PROCEDURE
                {
                    line = line + "Procedure ";
                    m_stream.read(m_tmp4B); // number of bytes to corresponding End Proc line
                    m_stream.read(m_tmp2B); // part of seed for encryption
                    m_stream.read(m_tmp1B); // flags
                    m_stream.read(m_tmp1B); // part of seed for encryption
                    readWords += 4;
                    break;
                }                    
                case 0x064A: // REM
                case 0x0652: // REM type 2
                {
                    line = line + "REM ";
                    m_stream.read(m_tmp1B); // unused 
                    m_stream.read(m_tmp1B); // string size 
                    int strlength = readUnsignedByte(m_tmp1B);
                    if ( (strlength%2)==1 ) strlength += 1; // round up to words
                    byte[] str = new byte[strlength];
                    m_stream.read(str);
                    readWords += 1 + (strlength>>1);
                    line = line + (new String(str)) ;
                    break;
                }
                default:
                    line = line + (" 0x" + Integer.toHexString(tokenID));                    
            }
        }
        
        // last token is supposed to be null
        if ( tokenID != 0 ) {
            throw( new java.io.StreamCorruptedException("Line didn't end with a NULL token, but 0x"+Integer.toHexString(tokenID)+" ("+line+")") );
        }

        // stop at the first line just for testing
        //m_readBytes = m_sourceSizeBytes ;

        return line;
    } // end readLine()
    
    
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
    public static int readUnsignedWord(byte[] bigEndianUnsigned)
    {
        int value = 0xFFFF & (int)(
                ((0x00ff & (int)bigEndianUnsigned[0]) << 8) |
                ((0x00ff & (int)bigEndianUnsigned[1]) ) );
        return value ;
    }
    public static int readUnsignedByte(byte[] bigEndianUnsigned)
    {
        int value = 0xff & (int)bigEndianUnsigned[0] ;
        return value ;
    }
}