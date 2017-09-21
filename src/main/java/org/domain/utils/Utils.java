package org.domain.utils;

import java.security.InvalidParameterException;
import java.util.ArrayList;

public class Utils {
	// Message, hexToInt
	// Converte uma letra hexadecimal em seu valor inteiro
 	public static int hexAsciiToInt(char ch, boolean enableUpper, boolean enableLower, char escapeCharIn, char escapeCharOut) {
		int ret = ch;
		
		if (ch >= '0' && ch <= '9') {
			ret -= 0x30;
		} else if (enableUpper && ch >= 'A' && ch <= 'F') {
			ret -= 0x37;
		} else if (enableLower && ch >= 'a' && ch <= 'f') {
			ret -= 0x37;
		} else if (ch == escapeCharIn) {
			ch = escapeCharOut;
			ret = hexAsciiToInt(ch, enableUpper, enableLower, '0', '0');
		} else {
			ret = -1;
		}

		return ret;
	}
	// Message, appendAsciiHexToBinary, AsciiHexToBinary
	// Converte uma sequencia de até quatro letras hexadecimais em um valor inteiro
	public static int hexToInt(CharSequence data, int offset, int length, char escapeCharIn, char escapeCharOut) {
		if (length <= 0 || length > 2*4) {
			throw new InvalidParameterException();
		}

		int bits = 0;
		int pos = offset;
		int posEnd = offset + length;

		while (pos < posEnd && pos < data.length()) {
			char ch = data.charAt(pos);
			int val = hexAsciiToInt(ch, true, true, escapeCharIn, escapeCharOut);
			
			if (val >= 0) {
				bits <<= 4;
				bits |= val;
				pos++;
			} else {
				throw new InvalidParameterException(String.format("hexToInt : must by val >= 0 (val : %d), ch = %s, pos = %d, data = %s", val, ch, pos, data));
			}
		}

		while (pos < posEnd) {
			bits <<= 4;
			pos++;
		}

		return bits;
	}
	// Message, AddAsciiHexFromUnsignedByte
	public static char intToHexAsciiChar(int value) {
		if (value < 0 || value > 15) {
			throw new InvalidParameterException(String.format("IntToHexChar : only alowed 0 -> 15 (value = %s)", value));
		}

		char ch = 0x30;

		if (value > 9) {
			ch = 0x37;
		}

		return (char)(ch + value);
	}
	//
	private static boolean isHex(char ch, boolean enableUpper, boolean enableLower) {
		boolean ret = false;

		if (ch >= '0' && ch <= '9') {
			ret = true;
		} else if (enableUpper && ch >= 'A' && ch <= 'F') {
			ret = true;
		} else if (enableLower && ch >= 'a' && ch <= 'f') {
			ret = true;
		}

		return ret;
	}
	// unused
	public static boolean isHex(CharSequence str, int posIni, int posEnd, boolean enableUpper, boolean enableLower) {
		if (str == null || str.length() == 0) {
			return false;
		}

		boolean ret = true;

		for (int i = posIni; i < posEnd; i++) {
			char ch = str.charAt(i);
			
			if (isHex(ch, enableUpper, enableLower) == false) {
				ret = false;
				break;
			}
		}

		return ret;
	}
	// Message, Converter
	public static boolean isUnsignedInteger(String str) {
		if (str == null || str.length() == 0 || str.length() > 18) {
			return false;
		}

		boolean ret = false;

		int i = 0;

		if (str.charAt(i) == '-') {
			i++;
		}

		while (i < str.length()) {
			char ch = str.charAt(i++);

			if (ch >= '0' && ch <= '9') {
				ret = true;
				continue;
			}

			ret = false;
			break;
		}

		return ret;
	}

	// Converter, ConverterLogger
	public static int findInList(ArrayList<String> list, String item) {
		int index = -1;
		
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).equals(item)) {
				index = i;
				break;
			}
		}
		
		return index;
	}

	// transforma as cadeias de (hh) e (h) em ?
	public static String replaceBinarySkipes(String str, int offset, int size) {
//		isoStr = isoStr.replaceAll("\\(\\w\\w\\)", "?");
//		isoStr = isoStr.replaceAll("\\(\\w\\)", "?");
//		str = str.replace('\\', '?');
		StringBuilder buffer = new StringBuilder(str);
		int pos = offset;
		int posMax = offset + size;

		while (pos < buffer.length() && pos < posMax + 3) {
			pos  = buffer.indexOf("(", pos);

			if (pos >= offset && pos < posMax) {
				int posEnd = buffer.indexOf(")", pos);

				if (posEnd >= pos+2 && posEnd <= pos+3) {
					if (Utils.isHex(buffer, pos+1, posEnd, true, true)) {
						String content = buffer.substring(pos+1, posEnd);
						char ch = (char) Utils.hexToInt(content, 0, content.length(), '0', '0');
//						ch = '?';
						buffer.replace(pos, posEnd+1, Character.toString(ch));
					} else {
/*
						if (forceReplace) {
							buffer.insert(pos+1, replace);
						}
*/						
//						System.out.printf("not hex : %s\n", content);
					}
				} else {
//					System.out.printf("missing valid close : %s\n", buffer.substring(pos+1, posEnd));
				}

				pos++;
			} else {
				break;
			}
		}

		return buffer.toString();
	}

}
