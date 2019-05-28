package com.empire;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class Compressor {
	private static final Logger log = Logger.getLogger(Compressor.class.getName());

	public static byte[] compress(String str) {
		ByteArrayOutputStream s = new ByteArrayOutputStream();
		try (OutputStreamWriter w = new OutputStreamWriter(new GZIPOutputStream(s), StandardCharsets.UTF_8)) {
			w.write(str);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Compression failure.", e);
		}
		return s.toByteArray();
	}

	public static String decompress(byte[] in) {
		StringBuilder b = new StringBuilder();
		try (InputStreamReader r = new InputStreamReader(new GZIPInputStream(new ByteArrayInputStream(in)), StandardCharsets.UTF_8)) {
			char[] buffer = new char[1024 * 50];
			int length = r.read(buffer);
			while (length > 0) {
				b.append(buffer, 0, length);
				length = r.read(buffer);
			}
		} catch (IOException e) {
			log.log(Level.SEVERE, "Decompression failure.", e);
		}
		return b.toString(); 
	}

	private Compressor() {}
}
