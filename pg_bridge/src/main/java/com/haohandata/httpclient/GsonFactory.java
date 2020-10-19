package com.haohandata.httpclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.Charset;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonGenerator;
import com.google.api.client.json.JsonParser;
import com.google.api.client.util.Beta;
import com.google.api.client.util.Charsets;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class GsonFactory extends JsonFactory {

    /**
     * {@link Beta} <br/>
     * Returns a global thread-safe instance.
     *
     * @since 1.16
     */
    @Beta
    public static GsonFactory getDefaultInstance() {
        return InstanceHolder.INSTANCE;
    }

    /** Holder for the result of {@link #getDefaultInstance()}. */
    @Beta
    static class InstanceHolder {
        static final GsonFactory INSTANCE = new GsonFactory();
    }

    @Override
    public JsonParser createJsonParser(InputStream in) throws IOException {
    // TODO(mlinder): Parser should try to detect the charset automatically when using GSON
    // http://code.google.com/p/google-http-java-client/issues/detail?id=6
    return createJsonParser(new InputStreamReader(in, Charsets.UTF_8));
  }

  @Override
  public JsonParser createJsonParser(InputStream in, Charset charset) throws IOException {
    if (charset == null) {
      return createJsonParser(in);
    }
    return createJsonParser(new InputStreamReader(in, charset));
  }

  @Override
  public JsonParser createJsonParser(String value) {
    return createJsonParser(new StringReader(value));
  }

  @Override
  public JsonParser createJsonParser(Reader reader) {
    return new GsonParser(this, new JsonReader(reader));
  }

  @Override
  public JsonGenerator createJsonGenerator(OutputStream out, Charset enc) {
    return createJsonGenerator(new OutputStreamWriter(out, enc));
  }

  @Override
  public JsonGenerator createJsonGenerator(Writer writer) {
    return new GsonGenerator(this, new JsonWriter(writer));
  }
}
