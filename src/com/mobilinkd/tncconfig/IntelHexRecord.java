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

public class IntelHexRecord {

	private String record = null;
	private int length = 0;
	private int address = 0;
	private int type = 0;
	private int checksum = 0;
	private byte[] data = null;
	
	private char parseStartCode() {
		return record.charAt(0);
	}
	
	private int parseLength() {
		return Integer.parseInt(record.substring(1, 3), 16);
	}
	
	private int parseAddress() {
		return Integer.parseInt(record.substring(4, 7), 16);
	}
	
	private int parseType() {
		return Integer.parseInt(record.substring(7, 9), 16);
	}
	
	private int parseChecksum() {
		return Integer.parseInt(record.substring(record.length() - 2), 16);
	}
	
	private byte[] parseData(int size) {
		byte[] result = new byte[size];
		for (int i = 0; i != size; i++) {
			int pos = 9 + (i * 2);
			result[i] = (byte) Integer.parseInt(
					record.substring(pos, pos + 2), 16);
		}
		return result;
	}
	
	private int computeChecksum() {
		int checksum = 0;
		for (int i = 1; i != record.length() - 2; i += 2) {
			checksum += Integer.parseInt(
					record.substring(i, i + 2), 16);
		}
		checksum %= 256;
		checksum = 256 - checksum;
		checksum %=256;
		return checksum;
	}
	
	private boolean parse() {
		if (parseStartCode() != ':') return false;
		
		length = parseLength();
		address = parseAddress();
		type = parseType();
		data = parseData(length);
		checksum = parseChecksum();
		return checksum == computeChecksum();
	}
	
	public IntelHexRecord(String line) throws IllegalArgumentException {
		record = line;
		if (!parse()) throw new IllegalArgumentException();
	}
	
	public int length() {
		return length;
	}
	
	public int type() {
		return type;
	}
	
	public int address() {
		return address;
	}

	public byte[] data() {
		return data;
	}
}
