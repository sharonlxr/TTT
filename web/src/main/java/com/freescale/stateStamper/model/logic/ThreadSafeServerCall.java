package com.freescale.stateStamper.model.logic;

import java.io.IOException;

import com.motorola.gramms.util.net.ServerCall;

public class ThreadSafeServerCall {
	private ServerCall serverCall; 
	
	public ThreadSafeServerCall(ServerCall serverCall){
		this.serverCall = serverCall;
	}
	
	public ServerCall getServerCall() {
		return serverCall;
	}

	public void setServerCall(ServerCall serverCall) {
		this.serverCall = serverCall;
	}

	public  synchronized String getReply (String message) throws IOException {
		return serverCall.getReply(message);
	}
	
	public synchronized void  closeSocket() throws IOException{
		serverCall.closeSocket();
	}
}
