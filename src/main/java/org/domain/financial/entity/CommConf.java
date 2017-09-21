package org.domain.financial.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.domain.financial.messages.comm.Comm;

@Entity
@Table(name = "connector", schema = "public")
public class CommConf implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6552305915855815264L;

	public enum RequestsDirection {
		CLIENT_TO_SERVER, SERVER_TO_CLIENT, BIDIRECIONAL
	}
	// GPRS_V1, TEF_DEDICADO, TEF_DISCADO, GOODCARD, OI, SANTANDER, CancelamentoUnik (SYSCAP), etc...
	@Id
	private String name; // TEF, HSM, ETH_V2
	private Boolean enabled = true; // false, true
	// Server or Client
	private Boolean listen = false; // CLIENT = 0, SERVER = 1
	private String ip = "127.0.0.1";
	private Integer port = 3001; // 1100, 7000, 24000
	// número máximo de conexões que podem aguardar na fila para ser capturadas pelo servidor
	private Integer backlog = 50;
	// if Persistent, don't close connection
	// PERMANENT, TEMPORARY
	private Boolean permanent = true; // TEMPORARY
	// Client to Server, Server to Client, Bidirecional
	private RequestsDirection direction = RequestsDirection.CLIENT_TO_SERVER; // CLIENT_TO_SERVER = 0, SERVER_TO_CLIENT = 1, BIDIRECIONAL = 2
	// número máximo de seções simultâneas que o servidor pode processar
	private Integer maxOpenedConnections = 100; // n�mro m�ximo de conex�es : 1, 100, 1000
	private String adapter = "org.domain.financial.messages.comm.CommAdapterSizePayload"; // org.domain.financial.messages.comm.CommAdapterPayload, org.domain.financial.messages.comm.CommAdapterSizePayload
	private String messageConf = "default"; // org.domain.financial.messages.comm.CommAdapterPayload, org.domain.financial.messages.comm.CommAdapterSizePayload
	// Comm.ENDIAN_BIG ou Comm.ENDIAN_LITTLE
	private Comm.BinaryEndian endianType = Comm.BinaryEndian.UNKNOW; // BIG
	private Boolean sizeAscii = true;
	
	@Override
	public String toString() {
		return "CommConf [name=" + name + ", enabled=" + enabled + ", listen=" + listen + ", ip=" + ip + ", port="
				+ port + ", permanent=" + permanent + ", direction=" + direction + ", adapter=" + adapter
				+ ", messageConf=" + messageConf + "]";
	}

	public CommConf() {
	}

	public CommConf(String name, Integer port, Boolean permanent, RequestsDirection direction, String adapter, String messageConf, Boolean sizeAscii) {
		this.name = name;
		this.port = port;
		this.permanent = permanent;
		this.direction = direction;
		this.adapter = adapter;
		this.messageConf = messageConf;
		this.sizeAscii = sizeAscii;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Boolean getEnabled() {
		return this.enabled != null ? this.enabled : false;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public Boolean getListen() {
		return this.listen != null ? this.listen : false;
	}

	public void setListen(Boolean listen) {
		this.listen = listen;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public Boolean getPermanent() {
		return this.permanent != null ? this.permanent : false;
	}

	public void setPermanent(Boolean permanent) {
		this.permanent = permanent;
	}

	public RequestsDirection getDirection() {
		return this.direction != null ? this.direction : RequestsDirection.CLIENT_TO_SERVER;
	}

	public void setDirection(RequestsDirection direction) {
		this.direction = direction;
	}

	public Integer getMaxOpenedConnections() {
		return maxOpenedConnections;
	}

	public void setMaxOpenedConnections(Integer maxOpenedConnections) {
		this.maxOpenedConnections = maxOpenedConnections;
	}

	public String getAdapter() {
		return adapter;
	}

	public void setAdapter(String adapter) {
		this.adapter = adapter;
	}

	public String getMessageConf() {
		return messageConf;
	}

	public void setMessageConf(String messageConf) {
		this.messageConf = messageConf;
	}

	public Comm.BinaryEndian getEndianType() {
		return endianType;
	}

	public void setEndianType(Comm.BinaryEndian endianType) {
		this.endianType = endianType;
	}
	
	public Boolean getSizeAscii() {
		return sizeAscii;
	}

	public void setSizeAscii(Boolean sizeAscii) {
		this.sizeAscii = sizeAscii;
	}

	public Integer getBacklog() {
		return backlog;
	}

	public void setBacklog(Integer backlog) {
		this.backlog = backlog;
	}

}
