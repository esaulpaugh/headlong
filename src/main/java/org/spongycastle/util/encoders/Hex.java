/*
 * Copyright (c) 2000 - 2018 The Legion of the Bouncy Castle Inc. (https://www.bouncycastle.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.spongycastle.util.encoders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.spongycastle.util.Strings;

/**
 * Utility class for converting hex data to bytes and back again.
 */
public class Hex
{
    private static final Encoder encoder = new HexEncoder();
    
    public static String toHexString(
        byte[] data)
    {
        return toHexString(data, 0, data.length);
    }

    public static String toHexString(
        byte[] data,
        int    off,
        int    length)
    {
        byte[] encoded = encode(data, off, length);
        return Strings.fromByteArray(encoded);
    }

    /**
     * encode the input data producing a Hex encoded byte array.
     *
     * @return a byte array containing the Hex encoded data.
     */
    public static byte[] encode(
        byte[]    data)
    {
        return encode(data, 0, data.length);
    }
    
    /**
     * encode the input data producing a Hex encoded byte array.
     *
     * @return a byte array containing the Hex encoded data.
     */
    public static byte[] encode(
        byte[]    data,
        int       off,
        int       length)
    {
        ByteArrayOutputStream    bOut = new ByteArrayOutputStream();
        
        try
        {
            encoder.encode(data, off, length, bOut);
        }
        catch (Exception e)
        {
            throw new EncoderException("exception encoding Hex string: " + e.getMessage(), e);
        }
        
        return bOut.toByteArray();
    }

    /**
     * Hex encode the byte data writing it to the given output stream.
     *
     * @return the number of bytes produced.
     */
    public static int encode(
        byte[]         data,
        OutputStream   out)
        throws IOException
    {
        return encoder.encode(data, 0, data.length, out);
    }
    
    /**
     * Hex encode the byte data writing it to the given output stream.
     *
     * @return the number of bytes produced.
     */
    public static int encode(
        byte[]         data,
        int            off,
        int            length,
        OutputStream   out)
        throws IOException
    {
        return encoder.encode(data, off, length, out);
    }
    
    /**
     * decode the Hex encoded input data. It is assumed the input data is valid.
     *
     * @return a byte array representing the decoded data.
     */
    public static byte[] decode(
        byte[]    data)
    {
        ByteArrayOutputStream    bOut = new ByteArrayOutputStream();
        
        try
        {
            encoder.decode(data, 0, data.length, bOut);
        }
        catch (Exception e)
        {
            throw new DecoderException("exception decoding Hex data: " + e.getMessage(), e);
        }
        
        return bOut.toByteArray();
    }
    
    /**
     * decode the Hex encoded String data - whitespace will be ignored.
     *
     * @return a byte array representing the decoded data.
     */
    public static byte[] decode(
        String    data)
    {
        ByteArrayOutputStream    bOut = new ByteArrayOutputStream();
        
        try
        {
            encoder.decode(data, bOut);
        }
        catch (Exception e)
        {
            throw new DecoderException("exception decoding Hex string: " + e.getMessage(), e);
        }
        
        return bOut.toByteArray();
    }
    
    /**
     * decode the Hex encoded String data writing it to the given output stream,
     * whitespace characters will be ignored.
     *
     * @return the number of bytes produced.
     */
    public static int decode(
        String          data,
        OutputStream    out)
        throws IOException
    {
        return encoder.decode(data, out);
    }
}
