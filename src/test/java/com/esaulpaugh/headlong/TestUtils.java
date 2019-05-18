/*
   Copyright 2019 Evan Saulpaugh

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.esaulpaugh.headlong;

import com.esaulpaugh.headlong.abi.Tuple;
import com.esaulpaugh.headlong.rlp.util.Integers;
import com.esaulpaugh.headlong.util.FastHex;
import com.esaulpaugh.headlong.util.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.function.Function;

public class TestUtils {

    public static byte[] readFile(File file) throws IOException {

        byte[] data = new byte[(int) file.length()];

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            int available;
            int offset = 0;
            while ((available = bis.available()) > 0) {
                offset += bis.read(data, offset, available);
            }
        }

        System.out.println("READ " + file.getName());

        return data;
    }

    public static String readResourceAsString(Class<?> clazz, String name) throws IOException {
        URL url = clazz.getClassLoader().getResource(name);
        if(url == null) {
            throw new IOException("url null");
        }
        byte[] bytes = readFile(new File(url.getFile()));
        return new String(bytes, Charset.forName("UTF-8"));
    }

    public static byte[] parsePrimitiveToBytes(JsonElement in) {
        try {
            return Integers.toBytes(parseLong(in));
        } catch (NumberFormatException | IllegalStateException e) {
            String inString = in.getAsString();
            if(inString.startsWith("#")) {
                return parseBigInteger(in).toByteArray();
            } else {
                return inString.getBytes(Charset.forName("UTF-8"));
            }
        }
    }

    public static ArrayList<Object> parseArrayToBytesHierarchy(final JsonArray array) {
        ArrayList<Object> arrayList = new ArrayList<>();
        for (JsonElement element : array) {
            if(element.isJsonObject()) {
                arrayList.add(parseObject(element));
            } else if(element.isJsonArray()) {
                arrayList.add(parseArrayToBytesHierarchy(element.getAsJsonArray()));
            } else if(element.isJsonPrimitive()) {
                arrayList.add(parsePrimitiveToBytes(element));
            } else if(element.isJsonNull()) {
                throw new RuntimeException("null??");
            } else {
                throw new RuntimeException("?????");
            }
        }
        return arrayList;
    }

    public static Integer parseInteger(JsonElement in) {
        return in.getAsInt();
    }

    public static int[] parseIntArray(final JsonArray array) {
//        ArrayList<Object> arrayList = new ArrayList<>();
        final int size = array.size();
        int[] ints = new int[size];
        for (int i = 0; i < size; i++) {
            JsonElement element = array.get(i);
//            if(element.isJsonObject()) {
//
//            } else if(element.isJsonArray()) {
//
//            } else
            if(element.isJsonPrimitive()) {
                ints[i] = parseInteger(element);
            } else if(element.isJsonNull()) {
                throw new RuntimeException("null??");
            } else {
                throw new RuntimeException("?????");
            }
        }
        return ints;
    }

    public static byte[] parseBytes(String utf8) {
        return Strings.decode(utf8, Strings.UTF_8);
    }

    public static byte[] parseBytesX(String string, int x) {
        if(string.length() == x) {
            byte[] bytesX = new byte[x];
            for (int i = 0; i < x; i++) {
                bytesX[i] = (byte) string.charAt(i);
            }
            return bytesX;
        } else {
            return FastHex.decode(string);
        }
    }

    public static String parseString(JsonElement in) {
        return in.getAsString();
    }

    public static BigInteger parseBigInteger(JsonElement in) {
        String string = in.getAsString();
        return new BigInteger(string, 10);
    }

    public static BigInteger parseBigIntegerStringPoundSign(JsonElement in) {
        String string = in.getAsString();
        return new BigInteger(string.substring(1), 10);
    }

    public static long parseLong(JsonElement in) {
        return in.getAsLong();
    }

    public static Object parseObject(JsonElement in) {
        throw new UnsupportedOperationException("unsupported");
    }

    public static Function<JsonElement, ?> getArrayParser(Class<?> c, int depth, Parser<?> baseParser) {
        try {
            Class<?> prevClass = c;
            Parser<?> prevParser = baseParser;
            for (int i = 0; i < depth; i++) {
                final Class finalPrevClass = prevClass;
                final Parser<?> finalPrevParser = prevParser;
                Parser<?> newParser = (JsonElement j) -> TestUtils.parseObjectArray(j, finalPrevClass, finalPrevParser);
                final String className = prevClass.getName();
                prevClass = Class.forName(className.startsWith("[") ? "[" + className : "[L" + className + ";");
                prevParser = newParser;
            }
            return prevParser;

        } catch (ClassNotFoundException cnfe) {
            throw new RuntimeException(cnfe);
        }
    }

    public static <T> Object[] parseObjectArray(JsonElement in, Class<T> elementClass, Parser<T> elementParser) {
        JsonArray arr = in.getAsJsonArray();
        Object[] array = (Object[]) Array.newInstance(elementClass, arr.size());
        for(int i = 0; i < array.length; i++) {
            array[i] = elementParser.apply(arr.get(i));
        }
        return array;
    }

    public static BigInteger parseAddress(JsonElement in) { // uint160
        String hex = "00" + in.getAsString().substring(2);
        byte[] bytes = FastHex.decode(hex);
        return new BigInteger(bytes);
    }

    public static Parser<Tuple> getTupleParser(int depth, Parser<Object> baseParser) {
        Parser prevParser = baseParser;
        for (int i = 0; i < depth; i++) {
            final Parser finalPrevParser = prevParser;
            prevParser = (Parser<Tuple>) (JsonElement j) -> TestUtils.parseTuple(j, finalPrevParser);
        }
        return (Parser<Tuple>) prevParser;
    }

    public static Tuple parseTuple(JsonElement in, Parser<Object> elementParser) {
        JsonArray elements = in.getAsJsonObject().getAsJsonArray("elements");
        Object[] tupleElements = new Object[elements.size()];
        for (int j = 0; j < tupleElements.length; j++) {
            tupleElements[j] = elementParser.apply(elements.get(j));
        }
        return new Tuple(tupleElements);
    }

    public static Tuple parseTuple(JsonElement in, Parser<?>... elementParsers) {
        JsonArray elements = in.getAsJsonObject().getAsJsonArray("elements");
        Object[] tupleElements = new Object[elements.size()];
        for (int i = 0; i < tupleElements.length; i++) {
            tupleElements[i] = elementParsers[i].apply(elements.get(i));
        }
        return new Tuple(tupleElements);
    }

    // -----------------

    public interface CustomRunnable {
        void run() throws Throwable;
    }

    public static void assertThrown(Class<? extends Throwable> clazz, CustomRunnable r) throws Throwable {
        try {
            r.run();
        } catch (Throwable t) {
            if (clazz.isAssignableFrom(t.getClass())) {
                return;
            }
            throw t;
        }
        throw new AssertionError("no " + clazz.getName() + " thrown");
    }

    public static void assertThrown(Class<? extends Throwable> clazz, String substr, CustomRunnable r) throws Throwable {
        try {
            r.run();
        } catch (Throwable t) {
            if (clazz.isAssignableFrom(t.getClass()) && t.getMessage().contains(substr)) {
                return;
            }
            throw t;
        }
        throw new AssertionError("no " + clazz.getName() + " thrown");
    }

    public interface Parser<T> extends Function<JsonElement, T> {
    }
}
