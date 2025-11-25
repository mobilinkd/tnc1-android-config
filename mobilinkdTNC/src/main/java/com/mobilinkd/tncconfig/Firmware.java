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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class Firmware {

	// Debugging
	private static final String TAG = "Firmware";
	private static final boolean D = false;


	public static class Segment {
		public char memoryType;
		public int address;
		public byte[] data;
		
		public Segment(char type, int addr, byte[] d) {
			memoryType = type;
			address = addr;
			data = d;
			if (D) Log.i(TAG, "segment address: " + Integer.toHexString(address));
		}
	}
	
	private List<Segment> segments;
	
	Firmware(InputStream stream) throws IOException, IllegalArgumentException {
		segments = new ArrayList<Segment>();
		int address = 0;
		try {
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(stream));
		    String line = null;
		    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		    while ((line = reader.readLine()) != null) {
		    	IntelHexRecord record = new IntelHexRecord(line);
		    	
		    	if (record.type() == 1) break;
		    	
		    	// Append all adjoining records into one.
		    	if (record.address() != address) {
					if (D) Log.i(TAG, "expected address: " + Integer.toHexString(address));
					if (D) Log.i(TAG, "record address: " + Integer.toHexString(record.address()));
		    		
					byte[] segmentData = buffer.toByteArray();
					
		    		if (segmentData.length != 0) {
		    			if (segmentData.length % 2 != 0)
		    			{
		    				throw new IllegalArgumentException("bad segment alignment");
		    			}
		    			Segment segment = new Segment(
                                'F', address - segmentData.length, segmentData);
		    			segments.add(segment);
		    			address = record.address();
		    		}
	    			buffer.reset();
		    	}
	    		buffer.write(record.data());
	    		address += record.length();
		    }
		    
			byte[] segmentData = buffer.toByteArray();
 			Segment segment = new Segment(
                    'F', address - segmentData.length, segmentData);
			segments.add(segment);
			
		} catch (IOException x) {
		    System.err.format("IOException: %s%n", x);
		    throw(x);
		} catch (IllegalArgumentException x) {
			System.err.format("IllegalArgumentException: %s%n", x);
			throw(x);
		}
	}
	
	List<Segment> getSegments() {
		return segments;
	}
}
