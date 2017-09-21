package org.domain.financial.messages.comm;

import java.io.IOException;

import org.domain.financial.messages.Message;

public class CommAdapterPayload implements CommAdapter {
	
	public void send(Comm comm, Message message, byte[] payload) throws Exception {
		comm.os.write(payload);
	}

	public int receive(Comm comm, Message message, byte[] payload) throws Exception {
		int rc = -1;
		int readen = comm.is.read(payload, 0, payload.length);
		
		if (readen > 0) {
			rc = readen;
		} else {
			throw new IOException("CommAdapterPayload.read : Invalid size received");
		}
		
		return rc;
	}

	public void setup(String paramsSend, String paramsReceive) {
	}

}
