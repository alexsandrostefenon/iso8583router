package org.domain.financial.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "log")
public class Log implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4308430862241707265L;
	@Id
	private String timeStamp;
	@NotNull
	private String loglevel;
	private Integer transactionId;
	@NotNull
	private String header;
	private String root;
	@NotNull
	private String modules;
	@NotNull
	private String message;
	private String transaction;
	
	@Override
	public String toString() {
		return String.format("%s - %10s - %10d - %20s - %10s - %s - %s", timeStamp, loglevel, transactionId, header, root, modules, message, transaction);
	}
	
	public Log() {
		// TODO Auto-generated constructor stub
	}

	public Log(String timeStamp, String loglevel, Integer transactionId, String header, String root, String modules, String message, String transaction) {
		this.timeStamp = timeStamp;
		this.loglevel = loglevel;
		this.transactionId = transactionId;
		this.header = header;
		this.modules = modules;
		this.message = message;
		this.transaction = transaction;
	}

	public String getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}

	public String getLoglevel() {
		return loglevel;
	}

	public void setLoglevel(String loglevel) {
		this.loglevel = loglevel;
	}

	public Integer getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(Integer transactionId) {
		this.transactionId = transactionId;
	}

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public String getRoot() {
		return root;
	}

	public void setRoot(String root) {
		this.root = root;
	}

	public String getModules() {
		return modules;
	}

	public void setModules(String modules) {
		this.modules = modules;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getTransaction() {
		return transaction;
	}

	public void setTransaction(String transaction) {
		this.transaction = transaction;
	}

	
}
