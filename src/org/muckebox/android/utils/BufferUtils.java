package org.muckebox.android.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class BufferUtils {
    static public int bytesRemaining(ByteBuffer buf)
    {
        return buf.capacity() - buf.position();
    }
    
    static public boolean isFull(ByteBuffer buf)
    {
        return buf.position() >= buf.capacity();
    }
    
    static public boolean isEmpty(ByteBuffer buf)
    {
        return buf.position() == 0;
    }
    
    static public void increasePosition(ByteBuffer buf, int delta)
    {
        buf.position(buf.position() + delta);
    }
    
    static public boolean readIntoBuffer(InputStream is, ByteBuffer buf) throws IOException
    {
        while (! isFull(buf))
        {
            int bytes_read = is.read(buf.array(),
                    buf.position(), bytesRemaining(buf));
            
            if (bytes_read == -1)
                return true;

            increasePosition(buf, bytes_read);
        }
        
        return false;
    }
    
}
