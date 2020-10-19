package com.haohandata.httpclient;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonGenerator;
import com.google.gson.stream.JsonWriter;

public class GsonGenerator extends JsonGenerator{

    private final JsonWriter writer;
    private final GsonFactory factory;

    GsonGenerator(GsonFactory factory, JsonWriter writer) {
        this.factory = factory;
        this.writer = writer;
        // lenient to allow top-level values of any type
        writer.setLenient(true);
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    @Override
    public JsonFactory getFactory() {
        return factory;
    }

    @Override
    public void writeBoolean(boolean state) throws IOException {
        writer.value(state);
    }

    @Override
    public void writeEndArray() throws IOException {
        writer.endArray();
    }

    @Override
    public void writeEndObject() throws IOException {
        writer.endObject();
    }

    @Override
    public void writeFieldName(String name) throws IOException {
        writer.name(name);
    }

    @Override
    public void writeNull() throws IOException {
        writer.nullValue();
    }

    @Override
    public void writeNumber(int v) throws IOException {
        writer.value(v);
    }

    @Override
    public void writeNumber(long v) throws IOException {
        writer.value(v);
    }

    @Override
    public void writeNumber(BigInteger v) throws IOException {
        writer.value(v);
    }

    @Override
    public void writeNumber(double v) throws IOException {
        writer.value(v);
    }

    @Override
    public void writeNumber(float v) throws IOException {
        writer.value(v);
    }

    @Override
    public void writeNumber(BigDecimal v) throws IOException {
        writer.value(v);
    }

    /**
     * Hack to support numbers encoded as a string for JsonWriter. Unfortunately,
     * JsonWriter doesn't provide a way to print an arbitrary-precision number given
     * a String and instead expects the number to extend Number. So this lets us
     * bypass that problem by overriding the toString() implementation of Number to
     * use our string. Note that this is not actually a valid Number.
     */
    static final class StringNumber extends Number {
        private static final long serialVersionUID = 1L;
        private final String encodedValue;

        StringNumber(String encodedValue) {
            this.encodedValue = encodedValue;
        }

        @Override
        public double doubleValue() {
            return 0;
        }

        @Override
        public float floatValue() {
            return 0;
        }

        @Override
        public int intValue() {
            return 0;
        }

        @Override
        public long longValue() {
            return 0;
        }

        @Override
        public String toString() {
            return encodedValue;
        }
    }

    @Override
    public void writeNumber(String encodedValue) throws IOException {
        writer.value(new StringNumber(encodedValue));
    }

    @Override
    public void writeStartArray() throws IOException {
        writer.beginArray();
    }

    @Override
    public void writeStartObject() throws IOException {
        writer.beginObject();
    }

    @Override
    public void writeString(String value) throws IOException {
        writer.value(value);
    }

    @Override
    public void enablePrettyPrint() throws IOException {
        writer.setIndent("  ");
    }
}
