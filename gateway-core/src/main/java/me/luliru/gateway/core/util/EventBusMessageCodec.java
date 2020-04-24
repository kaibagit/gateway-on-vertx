package me.luliru.gateway.core.util;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * EventBusMessageCodec
 * Created by luliru on 2019-07-05.
 */
public class EventBusMessageCodec implements MessageCodec {

    @Override
    public void encodeToWire(Buffer buffer, Object o) {
        try {
            ByteArrayOutputStream os=new ByteArrayOutputStream();
            HessianOutput output=new HessianOutput(os);
            output.writeObject(o);
            byte[] strBytes = os.toByteArray();
            buffer.appendInt(strBytes.length);
            buffer.appendBytes(strBytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Object decodeFromWire(int pos, Buffer buffer) {
        try{
            int length = buffer.getInt(pos);
            pos += 4;
            byte[] bytes = buffer.getBytes(pos, pos + length);
            ByteArrayInputStream ins=new ByteArrayInputStream(bytes);
            HessianInput input=new HessianInput(ins);
            Object readObject = input.readObject();
            return readObject;
        }catch (IOException e){
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Object transform(Object o) {
        return o;
    }

    @Override
    public String name() {
        return "json_default";
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }
}
