/*   
 * Copyright 2013 Karsten Patzwaldt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
