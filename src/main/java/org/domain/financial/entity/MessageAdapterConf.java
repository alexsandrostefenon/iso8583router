package org.domain.financial.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "message_adapter_conf")
public class MessageAdapterConf implements java.io.Serializable {
	public MessageAdapterConf(String name, String parent) {
		super();
		this.name = name;
		this.parent = parent;
	}
	
	public MessageAdapterConf() {
		// TODO Auto-generated constructor stub
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = -5585520573142916221L;
	@Id
	private String name;
	private String parent = "iso8583default";
	@NotNull
	private String adapter = "org.domain.financial.messages.MessageAdapterISO8583";
	private String tagPrefix = null;
	private Boolean compress = false;
	@Transient
	private List<MessageAdapterConfItem> items = null;
	
	public static MessageAdapterConfItem getMessageAdapterConfItemFromTag(List<MessageAdapterConfItem> confs, String tagName) {
		MessageAdapterConfItem ret = null;
		
		for (MessageAdapterConfItem conf : confs) {
			if (tagName.equals(conf.getTag())) {
				ret = conf;
			}
		}
		
		return ret;
	}
	
	public List<MessageAdapterConfItem> getMessageAdapterConfItems(String root) throws Exception {
		ArrayList<MessageAdapterConfItem> ret = new ArrayList<MessageAdapterConfItem>();
		
		for (MessageAdapterConfItem item : this.items) {
			String regex = item.getRootPattern();
			
			if (regex == null || Pattern.matches(regex, root)) {
				ret.add(item);
			}
		}

		return ret;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getParent() {
		return parent;
	}
	public void setParent(String parent) {
		this.parent = parent;
	}
	public String getAdapter() {
		return adapter;
	}
	public void setAdapter(String adapter) {
		this.adapter = adapter;
	}
	public String getTagPrefix() {
		return tagPrefix;
	}
	public void setTagPrefix(String tagPrefix) {
		this.tagPrefix = tagPrefix;
	}
	public Boolean getCompress() {
		return compress;
	}
	public void setCompress(Boolean compress) {
		this.compress = compress;
	}

	public List<MessageAdapterConfItem> getItems() {
		return items;
	}

	public void setItems(List<MessageAdapterConfItem> items) {
		this.items = items;
	}

}
