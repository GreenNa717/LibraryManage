package library.common;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BarcodeUtil {

    public static byte[] generateCode128(String content, int width, int height) throws IOException {
        Code128Writer writer = new Code128Writer();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.CODE_128, width, height);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", os);
        return os.toByteArray();
    }
}
