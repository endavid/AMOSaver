package amos.io ;

import java.io.IOException ;

public class UnsupportedFormat extends java.io.IOException
{
    String m_message ;
    
    public UnsupportedFormat()
    {
        m_message = "Unknown";
    }
    public UnsupportedFormat(String message)
    {
        m_message = message ;
    }
    public String toString()
    {
        return (super.toString() + ": "+m_message);
    }
}