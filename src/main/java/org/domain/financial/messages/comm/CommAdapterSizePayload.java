package org.domain.financial.messages.comm;

import java.io.IOException;

import org.domain.financial.messages.Message;

public class CommAdapterSizePayload implements CommAdapter {
	
	public void send(Comm comm, Message message, byte[] payload) throws Exception {
		int size = payload.length;
		int offset = 0;
		byte[] buffer = new byte[size+4];
		
		if (comm.conf.getSizeAscii()) {
			String strSize = String.format("%04d", size);
			offset = Comm.pack(buffer, offset, strSize.length(), strSize.getBytes());
		} else {
			offset = Comm.pack(buffer, offset, 2, comm.conf.getEndianType(), size);
		}
		
		offset = Comm.pack(buffer, offset, payload.length, payload);
		comm.os.write(buffer, 0, offset);
	}

	public int receive(Comm comm, Message message, byte[] payload) throws Exception {
		int rc = -1;
		Comm.RefInt size = new Comm.RefInt();
		
		if (comm.conf.getSizeAscii()) {
			int readen = comm.is.read(payload, 0, 4);
			
			if (readen != 4) {
				throw new IOException("CommAdapterSizePayload.read : Invalid size len received");
			}
			
			String strSize = new String(payload, 0, 4, "ISO-8859-1");
			size.value = Integer.parseInt(strSize);
		} else {
			Comm.unpack(comm.is, payload, 0, 2, comm.conf.getEndianType(), size);
		}
		
		if (size.value > 0 && size.value < payload.length) {
			int readen = comm.is.read(payload, 0, size.value);
			
			if (readen != size.value) {
				throw new IOException("CommAdapterSizePayload.read : Invalid size len received");
			}
			
			rc = size.value;
		}

		return rc;
	}

	public void setup(String paramsSend, String paramsReceive) {
	}

}
