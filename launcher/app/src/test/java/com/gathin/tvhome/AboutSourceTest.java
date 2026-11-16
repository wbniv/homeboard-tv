package com.gathin.tvhome;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import java.io.File;
import org.junit.Test;
import static org.junit.Assert.*;

/** Independent decoding guards against shipping a QR for the wrong source destination. */
public class AboutSourceTest {
    @Test public void homeboardQrOpensProjectSource() throws Exception { decode(AboutSource.HOMEBOARD); }
    @Test public void cleanYourTvQrOpensUpstreamSource() throws Exception { decode(AboutSource.CLEANYOURTV); }
    @Test public void outfitQrOpensFontSource() throws Exception { decode(AboutSource.OUTFIT); }
    @Test public void androidxQrOpensLibrarySource() throws Exception { decode(AboutSource.ANDROIDX); }

    private void decode(AboutSource source) throws Exception {
        // Android's test compiler has an Android bootclasspath. The JUnit JVM still
        // supplies the desktop PNG reader, without adding a decoder to the APK.
        Object image = Class.forName("javax.imageio.ImageIO").getMethod("read", File.class)
                .invoke(null, new File("src/main/res/drawable-nodpi/" + source.qrName + ".png"));
        assertNotNull(image);
        Class<?> type = Class.forName("java.awt.image.BufferedImage");
        int width = (Integer) type.getMethod("getWidth").invoke(image);
        int height = (Integer) type.getMethod("getHeight").invoke(image);
        assertEquals(width, height); assertTrue(width >= 200);
        int[] pixels = (int[]) type.getMethod("getRGB", int.class, int.class, int.class, int.class,
                int[].class, int.class, int.class).invoke(image, 0, 0, width, height, null, 0, width);
        assertEquals(0xffffffff, pixels[0]);
        RGBLuminanceSource luminance = new RGBLuminanceSource(width, height, pixels);
        assertEquals(source.url, new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(luminance))).getText());
    }
}
