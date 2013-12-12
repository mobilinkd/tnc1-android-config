/*
 * Copyright (C) 2013 Mobilinkd LLC
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

package com.mobilinkd.tncconfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

public class Firmware {

	private class Segment {
		@SuppressWarnings("unused")
		public char memoryType;
		@SuppressWarnings("unused")
		public int address;
		@SuppressWarnings("unused")
		public byte[] data;
		
		public Segment(char type, int addr, byte[] d) {
			memoryType = type;
			address = addr;
			data = d;
		}
	}
	
	private List<Segment> segments;
	
	Firmware(String urlPath) {
		InputStream stream = null;
		int address = 0;
		try {
			URL url = new URL(urlPath);
			stream = url.openConnection().getInputStream();
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(stream));
		    String line = null;
		    StringBuilder sb = new StringBuilder(65536);
		    while ((line = reader.readLine()) != null) {
		    	IntelHexRecord record = new IntelHexRecord(line);
		    	if (record.type() == 1) break;
		    	if (record.address() != address) {
		    		int len = sb.length();
		    		if (len != 0) {
		    			Segment segment = new Segment(
		    					'F', address - len, sb.toString().getBytes());
		    			segments.add(segment);
		    			sb.setLength(0);
		    			address = record.address();
		    		}
		    		sb.append(record.data());
		    	}
		    }
    		int len = sb.length();
			Segment segment = new Segment(
					'F', address - len, sb.toString().getBytes());
			segments.add(segment);
		} catch (IOException x) {
		    System.err.format("IOException: %s%n", x);
		}
	}
}
