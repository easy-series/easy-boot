package com.easy.cache.serializer;

import java.io.*;

/**
 * JDK序列化器实现
 */
public class JdkSerializer implements Serializer {

    @Override
    public Object serialize(Object object) {
        if (object == null) {
            return null;
        }
        
        if (!(object instanceof Serializable)) {
            throw new IllegalArgumentException("Object must implement Serializable interface");
        }
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize object", e);
        }
    }

    @Override
    public Object deserialize(Object data) {
        if (data == null) {
            return null;
        }
        
        if (!(data instanceof byte[])) {
            throw new IllegalArgumentException("Data must be byte[]");
        }
        
        try (ByteArrayInputStream bais = new ByteArrayInputStream((byte[]) data);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to deserialize object", e);
        }
    }
} 