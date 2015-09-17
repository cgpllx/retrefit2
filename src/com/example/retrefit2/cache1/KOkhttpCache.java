package com.example.retrefit2.cache1;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.util.Log;

/**
 * retrofit 缓存辅助类
 * 
 * @author Administrator
 *
 */
public class KOkhttpCache extends DiskLruCacheHelper {

	public KOkhttpCache(Context context) throws IOException {
		super(context);
	}

	public KOkhttpCache(Context context, int cacheVersion) throws IOException {
		super(context, cacheVersion);
	}

	public KOkhttpCache(Context context, String dirName, int cacheVersion) throws IOException {
		super(context, dirName, cacheVersion);
	}


	/**
	 * Reads the contents of an InputStream into a byte[].
	 * */
	private byte[] streamToBytes(InputStream in, int length) throws IOException {
		byte[] bytes = new byte[length];
		int count;
		int pos = 0;
		while (pos < length && ((count = in.read(bytes, pos, length - pos)) != -1)) {
			pos += count;
		}
		if (pos != length) {
			throw new IOException("Expected " + length + " bytes, read " + pos + " bytes");
		}
		return bytes;
	}

	private static class CountingInputStream extends FilterInputStream {
		private int bytesRead = 0;

		private CountingInputStream(InputStream in) {
			super(in);
		}

		@Override
		public int read() throws IOException {
			int result = super.read();
			if (result != -1) {
				bytesRead++;
			}
			return result;
		}

		@Override
		public int read(byte[] buffer, int offset, int count) throws IOException {
			int result = super.read(buffer, offset, count);
			if (result != -1) {
				bytesRead += result;
			}
			return result;
		}
	}
}
