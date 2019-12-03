package io.seata.codec.hessian;

import com.caucho.hessian.io.*;
import io.seata.common.loader.LoadLevel;
import io.seata.core.codec.Codec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @Xin Wang
 */
@LoadLevel(name = "HESSIAN")
public class HessianCodec implements Codec {
    private final Logger logger = LoggerFactory.getLogger(HessianCodec.class);

    @Override
    public <T> byte[] encode(T t) {
        byte[] stream = null;
        SerializerFactory hessian = HessianSerializerFactory.getInstance();
        try {
            Serializer serializer = hessian.getSerializer(t.getClass());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Hessian2Output output = new Hessian2Output(baos);
            serializer.writeObject(t, output);
            output.close();
            stream = baos.toByteArray();
        } catch (HessianProtocolException e) {
            logger.error("Hessian encode error", e);
        } catch (IOException e) {
            logger.error("Hessian encode error", e);
        }
        return stream;
    }

    @Override
    public <T> T decode(byte[] bytes) {
        Hessian2Input input = null;
        try {
            ByteArrayInputStream is = new ByteArrayInputStream(bytes);
            input = new Hessian2Input(is);
            try {
                return (T) input.readObject();
            } catch (IOException e) {
                logger.error("Hessian decode error", e);
            }
        } finally {
            try {
                input.close();
            } catch (IOException e) {
                logger.error("Hessian decode error", e);
            }
        }
        return null;
    }
}
