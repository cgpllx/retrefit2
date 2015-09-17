/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit;

import static retrofit.Utils.closeQuietly;

import java.io.IOException;

import com.squareup.okhttp.ResponseBody;

final class OkHttpResponseBodyConverter implements Converter<ResponseBody, ResponseBody> {
	private final boolean isStreaming;

	OkHttpResponseBodyConverter(boolean isStreaming) {
		this.isStreaming = isStreaming;
	}

	@Override
	public ResponseBody convert(ResponseBody value) throws IOException {
		if (isStreaming) {
			return value;
		}

		// Buffer the entire body to avoid future I/O.
		try {
			return Utils.readBodyToBytesIfNecessary(value);
		} finally {
			closeQuietly(value);
		}
	}
 
}
