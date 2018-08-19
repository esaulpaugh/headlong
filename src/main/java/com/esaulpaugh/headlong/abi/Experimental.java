//package com.esaulpaugh.headlong.abi;
//
//public class Experimental {
//
//        Constructor[] constructors = Void.class.getDeclaredConstructors();
//        constructors[0].setAccessible(true);
//        return (Void) constructors[0].newInstance(new Object[] { });
//
//    private static final Void v;
//
//    static {
//        Keccak keccak = new Keccak(256);
//        ByteBuffer outBuffer = ByteBuffer.wrap(new byte[0]);
//        Void vv = null;
//        try {
//            vv = keccak.digest(outBuffer, 0);
//        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
//            e.printStackTrace();
//        }
//        v = vv;
//    }
//
//    synchronized (v) {
//        v.notifyAll();
//    }
//
////        --add-exports java.base/jdk.internal.misc=ALL-UNNAMED
////                -XaddExports java.base/jdk.internal.misc=ALL-UNNAMED
//
//        System.out.println(v.toString());
//    Object[] signers = v.getClass().getSigners();
//
//    //        jdk.internal.misc.InnocuousThread;
//    Field f = jdk.internal.misc.Unsafe.class.getDeclaredField("theUnsafe");
//        f.setAccessible(true);
//    Unsafe u = (Unsafe) f.get(SharedSecrets.class.newInstance());
//
////        Constructor[] x = SharedSecrets.class.getDeclaredConstructors();
////        x[0].setAccessible(true);
////        SharedSecrets s = (SharedSecrets) x[0].newInstance();
////        System.out.println(s.hashCode());
//
//
//
//        System.out.println(u.addressSize() + " " + u.unalignedAccess() + " " + u.isBigEndian());
//
//}
