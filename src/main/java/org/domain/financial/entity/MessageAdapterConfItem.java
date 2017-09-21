package org.domain.financial.entity;

import java.security.InvalidParameterException;
import java.util.Calendar;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.domain.financial.messages.Message;

@Entity
@Table(name = "message_adapter_conf_item", schema = "public")
public class MessageAdapterConfItem implements java.io.Serializable {
	
	private static final long serialVersionUID = -2253404418736276959L;
	
	public enum DataAlign {
		NONE, ZERO_LEFT, ZERO_RIGHT, SPACE_LEFT, SPACE_RIGHT
	}
	
	public static int DATA_TYPE_DECIMAL = 1;
	public static int DATA_TYPE_HEX = 2 | 1;
	public static int DATA_TYPE_ALPHA = 4 | 2;
	public static int DATA_TYPE_SPECIAL = 8;
	public static int DATA_TYPE_MASK = 16;
	public static String year = Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
	
	@Id
	private Integer id;
	private String fieldName;
	@NotNull
	private String tag;
	@NotNull
	private Integer minLength = 1;
	@NotNull
	private Integer maxLength = 2 * 999;
	@NotNull
	@Min(0)
	private Integer sizeHeader = 0;
	@NotNull
	private String rootPattern = "\\d\\d\\d\\d";
	@NotNull
	private String messageAdapterConfName = "iso8583default"; // iso8583default

	@NotNull
	private Integer dataType = DATA_TYPE_DECIMAL | DATA_TYPE_ALPHA | DATA_TYPE_SPECIAL;
	private String dataFormat;
	private DataAlign alignment = DataAlign.ZERO_LEFT;
	
	public MessageAdapterConfItem(Integer id, String fieldName, String tag, Integer minLength, Integer maxLength,
			Integer sizeHeader, String rootPattern, String messageAdapterConfName, Integer dataType) {
		this.id = id;
		this.fieldName = fieldName;
		this.tag = tag;
		this.minLength = minLength;
		this.maxLength = maxLength;
		this.sizeHeader = sizeHeader;
		this.rootPattern = rootPattern;
		this.messageAdapterConfName = messageAdapterConfName;
		this.dataType = dataType;
	}

	public MessageAdapterConfItem() {
	}

	private static boolean isAlpha(char ch) {
		boolean ret = false;

		if (ch >= 'A' && ch <= 'Z') {
			ret = true;
		} else if (ch >= 'a' && ch <= 'z') {
			ret = true;
		}

		return ret;
	}
	
	private int checkContentType(String data) {
		int pos = -1;
		boolean mayBeNumberDecimal = (this.dataType & DATA_TYPE_DECIMAL) > 0;
		boolean mayBeNumberHex = (this.dataType & DATA_TYPE_HEX) > 0;
		boolean mayBeAlpha = (this.dataType & DATA_TYPE_ALPHA) > 0;
		boolean mayBeSpecial = (this.dataType & DATA_TYPE_SPECIAL) > 0;
		boolean mayBeMask = (this.dataType & DATA_TYPE_MASK) > 0;
		// varre cada caracter da string, cada letra tem que encaixar em um grupo válido
		for (int i = 0; i < data.length(); i++) {
			char ch = data.charAt(i);

			if (mayBeNumberDecimal && ch >= '0' && ch <= '9') {
				continue;
			}

			if (mayBeNumberHex && ch >= '0' && ch <= '9') {
				continue;
			}

			if (mayBeNumberHex && ch >= 'A' && ch <= 'F') {
				continue;
			}

			if (mayBeNumberHex && ch >= 'a' && ch <= 'f') {
				continue;
			}

			if (mayBeAlpha && MessageAdapterConfItem.isAlpha(ch)) {
				continue;
			}

			if (mayBeSpecial && (ch >= '0' && ch <= '9') == false && MessageAdapterConfItem.isAlpha(ch) == false) {
				continue;
			}

			if (mayBeMask && ch == '*') {
				continue;
			}

			pos = i;
			break;
		}
		
		return pos;
	}
	
	public void setFieldData(Message message, String data) throws Exception {
		if (data == null || data.length() == 0) {
			return;
		}

		// first check for forced manipulation of ISO8583 fields
		if (MessageAdapterConfItem.year != null && fieldName != null) {
			if (fieldName.equals("dateTimeGmt") || fieldName.equals("systemDateTime")) {
				if (data.length() == 10 && maxLength == 14) {
					// TODO : trocar o valor chumbado do ano por um valor dinÃ¢mico
					data = MessageAdapterConfItem.year + data;
				} else if (data.length() == 14 && maxLength == 10) {
					data = data.substring(4);
				}
			} else if (fieldName.equals("captureDate")) {
				if (data.length() == 4 && maxLength == 8) {
					// TODO : trocar o valor chumbado do ano por um valor dinÃ¢mico
					data = MessageAdapterConfItem.year + data;
				} else if (data.length() == 8 && maxLength == 4) {
					data = data.substring(4);
				}
			} else if (fieldName.equals("lastOkDate")) {
				if (data.length() == 6 && maxLength == 8) {
					// TODO : trocar o valor chumbado do ano por um valor dinÃ¢mico
					data = MessageAdapterConfItem.year.substring(0, 2) + data;
				} else if (data.length() == 8 && maxLength == 6) {
					data = data.substring(2);
				}
			}
		}

		int length = data.length();

		if (length > maxLength) {
			String str = String.format("length[%d] > maxLength [%d] - field [%s] - str = %s", length, maxLength, fieldName, data);
//			debug(str);
			InvalidParameterException exception = new InvalidParameterException(str);
			throw exception;
		}

		int pos = this.checkContentType(data); 
		
		if (pos >= 0) {
			String str = String.format("field [%s] - pos [%d], dataType (%s) : str = %s", fieldName, pos, this.dataType, data);
			throw new InvalidParameterException(str);
		}

		if (this.sizeHeader == 0) {
			if (length <= maxLength && alignment != DataAlign.NONE) {
				// por padrÃ£o Ã© ISO8583Conf.ZERO_LEFT_ALIGNMENT
				char ch = '0';
				int side = 0;
				int posIni = 0;
				int posEnd = length-1;

				if (alignment == DataAlign.SPACE_RIGHT) {
					ch = ' ';
					side = 1;
				} else if (alignment == DataAlign.SPACE_LEFT) {
					ch = ' ';
				}

				if (side == 0) {
					while (posIni < posEnd && data.charAt(posIni) == ch) {
						posIni++;
					}
				} else {
					while (posIni < posEnd && data.charAt(posEnd) == ch) {
						posEnd--;
					}
				}

				data = data.substring(posIni, posEnd+1);
			} else if (length != maxLength) {
				String str = String.format("length [%d] < fizedLength [%d] - field [%s] - str = %s", length, maxLength, fieldName, data);
//				debug(str);
				throw new InvalidParameterException(str);
			}
		}

		if (this.fieldName != null) {
			message.setFieldData(this.fieldName, data);
		} else {
			message.setFieldData(this.tag, data);
		}
	}

	private static String getLeftAlign(StringBuilder buffer, String value, char ch, int lenght) {
		if (value != null) {
			int len = value.length();
			int diff = lenght - len;

			if (diff > 0) {
				buffer.setLength(0);

				for (int i = 0; i < diff; i++) {
					buffer.append(ch);
				}

				buffer.append(value);
				value = buffer.toString();
			} else if (diff < 0) {
				diff *= -1;
				int i = 0;
				
				while (i < diff && value.charAt(i) == ch) {
					i++;
				}
				
				value = value.substring(i);
			}
		}

		return value;
	}

	private static String getRightAlign(StringBuilder buffer, String value, char ch, int lenght) {
		if (value != null) {
			buffer.setLength(0);
			buffer.append(value);
			int len = value.length();
			int diff = lenght - len;

			if (diff > 0) {
				for (int i = 0; i < diff; i++) {
					buffer.append(ch);
				}

				value = buffer.toString();
			}
		}

		return value;
	}
	
	private String getFieldData(Message message) {
		String data = null;
		
		if (fieldName != null) {
			data = message.getFieldData(fieldName);
		} else if (tag != null) {
			data = message.getFieldData(tag);
		}
		// first check for forced manipulation of ISO8583 fields
		if (data != null && MessageAdapterConfItem.year != null && fieldName != null) {
			if (fieldName.equals("dateTimeGmt") || fieldName.equals("systemDateTime")) {
				if (data.length() == 10 && maxLength == 14) {
					// TODO : trocar o valor chumbado do ano por um valor dinÃ¢mico
					data = MessageAdapterConfItem.year + data;
				} else if (data.length() == 14 && maxLength == 10) {
					data = data.substring(4);
				}
			} else if (fieldName.equals("captureDate")) {
				if (data.length() == 4 && maxLength == 8) {
					// TODO : trocar o valor chumbado do ano por um valor dinÃ¢mico
					data = MessageAdapterConfItem.year + data;
				} else if (data.length() == 8 && maxLength == 4) {
					data = data.substring(4);
				}
			} else if (fieldName.equals("lastOkDate")) {
				if (data.length() == 6 && maxLength == 8) {
					// TODO : trocar o valor chumbado do ano por um valor dinÃ¢mico
					data = MessageAdapterConfItem.year.substring(0, 2) + data;
				} else if (data.length() == 8 && maxLength == 6) {
					data = data.substring(2);
				}
			}
		}
		
		return data;
	}

	private String getFieldDataLeftAlign(Message message, char ch, int lenght) {
		String value = getFieldData(message);
		value = MessageAdapterConfItem.getLeftAlign(message.auxData, value, ch, lenght);
		return value;
	}

	private String getFieldDataRightAlign(Message message, char ch, int lenght) {
		String value = getFieldData(message);
		value = MessageAdapterConfItem.getRightAlign(message.auxData, value, ch, lenght);
		return value;
	}

	public String getFieldDataWithAlign(Message message) {
		String data;
		int lenght = this.minLength;
		// adiciona os caracteres de alinhamento
		if (alignment == DataAlign.SPACE_RIGHT) {
			data = getFieldDataRightAlign(message, ' ', lenght);
		} else if (alignment == DataAlign.SPACE_LEFT) {
			data = getFieldDataLeftAlign(message, ' ', lenght);
		} else if (alignment == DataAlign.ZERO_LEFT) {
			data = getFieldDataLeftAlign(message, '0', lenght);
		} else if (alignment == DataAlign.ZERO_RIGHT) {
			data = getFieldDataRightAlign(message, '0', lenght);
		} else {
			data = getFieldData(message);
		}

		return data;
	}

	public String getMessageAdapterConfName() {
		return messageAdapterConfName;
	}

	public void setMessageAdapterConfName(String messageAdapterConfName) {
		this.messageAdapterConfName = messageAdapterConfName;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getRootPattern() {
		return rootPattern;
	}

	public void setRootPattern(String rootPattern) {
		this.rootPattern = rootPattern;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public Integer getMinLength() {
		return minLength;
	}

	public void setMinLength(Integer minLength) {
		this.minLength = minLength;
	}

	public Integer getMaxLength() {
		return maxLength;
	}

	public void setMaxLength(Integer maxLength) {
		this.maxLength = maxLength;
	}

	public Integer getSizeHeader() {
		return sizeHeader;
	}

	public void setSizeHeader(Integer sizeHeader) {
		this.sizeHeader = sizeHeader;
	}

	public Integer getDataType() {
		return dataType;
	}

	public void setDataType(Integer dataType) {
		this.dataType = dataType;
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public String getDataFormat() {
		return dataFormat;
	}

	public void setDataFormat(String dataFormat) {
		this.dataFormat = dataFormat;
	}

	public DataAlign getAlignment() {
		return alignment;
	}

	public void setAlignment(DataAlign alignment) {
		this.alignment = alignment;
	}


}
