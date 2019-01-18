package com.esaulpaugh.headlong;

import com.esaulpaugh.headlong.rlp.util.Integers;
import com.esaulpaugh.headlong.util.FastHex;
import com.esaulpaugh.headlong.util.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;

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

    public static BigInteger parseAddress(JsonElement in) { // uint160
        String hex = "00" + in.getAsString().substring(2);
        byte[] bytes = FastHex.decode(hex);
        return new BigInteger(bytes);
    }

    // -----------------

    public interface CustomRunnable {
        void run() throws Throwable;
    }

    public static void assertThrown(Class<? extends Throwable> clazz, String substr, CustomRunnable r) throws Throwable {
        try {
            r.run();
        } catch (Throwable t) {
            if(clazz.isAssignableFrom(t.getClass()) && t.getMessage().contains(substr)) {
                return;
            }
            throw t;
        }
        throw new AssertionError("no " + clazz.getName() + " thrown");
    }
}
