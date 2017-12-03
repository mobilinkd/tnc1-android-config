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

/**
 * Default TNC values, primarily for version 282 firmware.  All others
 * should support "GET_ALL_VALUES" which will return these from the
 * TNC.
 */
public class TncConfigDefaults {
	
	public static final int TX_DELAY = 50;
	public static final int PERSISTENCE = 64;
	public static final int SLOT_TIME = 10;
	public static final int TX_TAIL = 2;
	public static final boolean DUPLEX = false;

	public static final int OUTPUT_VOLUME = 128;
	public static final int INPUT_VOLUME = 0;
	public static final int SQUELCH_LEVEL = 2;	// DCD off; DCD on = 0.
	
	public static final int HW_VERSION = 1;
	public static final int SW_VERSION = 282;
}
