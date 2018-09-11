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
 * Utility class for converting Base64 data to bytes and back again.
 */
public class Base64
{
    private static final Encoder encoder = new Base64Encoder();

    public static String toBase64String(
        byte[] data)
    {
        return toBase64String(data, 0, data.length);
    }

    public static String toBase64String(
        byte[] data,
        int off,
        int length)
    {
        byte[] encoded = encode(data, off, length);
        return Strings.fromByteArray(encoded);
    }

    /**
     * encode the input data producing a base 64 encoded byte array.
     *
     * @return a byte array containing the base 64 encoded data.
     */
    public static byte[] encode(
        byte[] data)
    {
        return encode(data, 0, data.length);
    }

    /**
     * encode the input data producing a base 64 encoded byte array.
     *
     * @return a byte array containing the base 64 encoded data.
     */
    public static byte[] encode(
        byte[] data,
        int off,
        int length)
    {
        int len = (length + 2) / 3 * 4;
        ByteArrayOutputStream bOut = new ByteArrayOutputStream(len);

        try
        {
            encoder.encode(data, off, length, bOut);
        }
        catch (Exception e)
        {
            throw new EncoderException("exception encoding base64 string: " + e.getMessage(), e);
        }

        return bOut.toByteArray();
    }

    /**
     * Encode the byte data to base 64 writing it to the given output stream.
     *
     * @return the number of bytes produced.
     */
    public static int encode(
        byte[] data,
        OutputStream out)
        throws IOException
    {
        return encoder.encode(data, 0, data.length, out);
    }

    /**
     * Encode the byte data to base 64 writing it to the given output stream.
     *
     * @return the number of bytes produced.
     */
    public static int encode(
        byte[] data,
        int off,
        int length,
        OutputStream out)
        throws IOException
    {
        return encoder.encode(data, off, length, out);
    }

    /**
     * decode the base 64 encoded input data. It is assumed the input data is valid.
     *
     * @return a byte array representing the decoded data.
     */
    public static byte[] decode(
        byte[] data)
    {
        int len = data.length / 4 * 3;
        ByteArrayOutputStream bOut = new ByteArrayOutputStream(len);

        try
        {
            encoder.decode(data, 0, data.length, bOut);
        }
        catch (Exception e)
        {
            throw new DecoderException("unable to decode base64 data: " + e.getMessage(), e);
        }

        return bOut.toByteArray();
    }

    /**
     * decode the base 64 encoded String data - whitespace will be ignored.
     *
     * @return a byte array representing the decoded data.
     */
    public static byte[] decode(
        String data)
    {
        int len = data.length() / 4 * 3;
        ByteArrayOutputStream bOut = new ByteArrayOutputStream(len);

        try
        {
            encoder.decode(data, bOut);
        }
        catch (Exception e)
        {
            throw new DecoderException("unable to decode base64 string: " + e.getMessage(), e);
        }

        return bOut.toByteArray();
    }

    /**
     * decode the base 64 encoded String data writing it to the given output stream,
     * whitespace characters will be ignored.
     *
     * @return the number of bytes produced.
     */
    public static int decode(
        String data,
        OutputStream out)
        throws IOException
    {
        return encoder.decode(data, out);
    }

    /**
     * Decode to an output stream;
     *
     * @param base64Data       The source data.
     * @param start            Start position.
     * @param length           the length.
     * @param out The output stream to write to.
     */
    public static int decode(byte[] base64Data, int start, int length, OutputStream out)
    {
        try
        {
           return encoder.decode(base64Data, start, length, out);
        }
        catch (Exception e)
        {
            throw new DecoderException("unable to decode base64 data: " + e.getMessage(), e);
        }

    }
}
