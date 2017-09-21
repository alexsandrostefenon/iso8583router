package org.domain.financial.messages.comm;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;

import org.domain.financial.entity.CommConf;
import org.domain.financial.messages.Message;

public class Test {
	public static void sendRequest(Connector manager, String connName, Message message) throws Exception {
		CommConf commConf = (CommConf) manager.getEntityManager().createQuery("from CommConf o where o.name='" + connName + "'").getSingleResult();
		Comm comm = new Comm(commConf, manager);
		comm.send(message);
		Message messageIn = new Message();
		comm.receive(messageIn, null);
	}
	
	public static String[] getArgsFomMap(Map<String, String> properties) {
		String[] list = new String[properties.size()];
		int i = 0;
		
		for (String key : properties.keySet()) {
			list[i++] = "--" + key + "=" + properties.get(key);
		}
		
		return list;
	}

	public static void main(String[] args) {
		try {
			Map<String, String> properties = new HashMap<String, String>();
			properties.put("javax.persistence.jdbc.driver", "org.h2.Driver");
			properties.put("javax.persistence.jdbc.url", "jdbc:h2:tcp://localhost/~/iso8583router");
			properties.put("javax.persistence.jdbc.user", "sa");
			properties.put("javax.persistence.jdbc.password", "sa");
			properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
			properties.put("hibernate.hbm2ddl.auto", "update");
			Connector.adjustArgsMap(properties, args);
			Class.forName(properties.get("javax.persistence.jdbc.driver"));
			String url = properties.get("javax.persistence.jdbc.url");
			String user = properties.get("javax.persistence.jdbc.user");
			String password = properties.get("javax.persistence.jdbc.password");
			Connection connection;
			
			try {
				connection = DriverManager.getConnection(url, user, password);
			} catch (Exception e) {
				// verifica se deve rodar o servidor H2
				if (url.startsWith("jdbc:h2:tcp://")) {
					org.h2.tools.Server.main("-web", "-tcp", "-pg");
					connection = DriverManager.getConnection(url, user, password);
				} else {
					throw e;
				}
			}

			connection.createStatement().execute(
				"CREATE TABLE IF NOT EXISTS connector (name varchar(255) not null, adapter varchar(255), backlog integer, direction integer, enabled boolean, endianType integer, ip varchar(255), listen boolean, maxOpenedConnections integer, messageConf varchar(255), permanent boolean, port integer, sizeAscii boolean, primary key (name));"+
				"DELETE FROM connector WHERE name like('%-test%');" +
				"INSERT INTO connector (messageconf, adapter, backlog, direction, enabled, endiantype, ip, listen, maxopenedconnections, name, permanent, port, sizeascii) VALUES ('iso8583default', 'org.domain.financial.messages.comm.CommAdapterPayload', 50, 0, true, 1, 'localhost', true, 1, 'POS-test', true, 2001, false);"+
				"INSERT INTO connector (messageconf, adapter, backlog, direction, enabled, endiantype, ip, listen, maxopenedconnections, name, permanent, port, sizeascii) VALUES ('iso8583default', 'org.domain.financial.messages.comm.CommAdapterSizePayload', 50, 0, true, 1, 'localhost', true, 1, 'TEF-test', true, 2002, true);"+
				"INSERT INTO connector (messageconf, adapter, backlog, direction, enabled, endiantype, ip, listen, maxopenedconnections, name, permanent, port, sizeascii) VALUES ('iso8583default', 'org.domain.financial.messages.comm.CommAdapterSizePayload', 50, 2, true, 1, 'localhost', false, 1, 'MASTERCARD-test', true, 3001, true);"+
				""+
				"INSERT INTO connector (messageconf, adapter, backlog, direction, enabled, endiantype, ip, listen, maxopenedconnections, name, permanent, port, sizeascii) VALUES ('iso8583default', 'org.domain.financial.messages.comm.CommAdapterPayload', 50, 1, true, 1, 'localhost', false, 1, 'POS-test-emu', true, 2001, false);"+
				"INSERT INTO connector (messageconf, adapter, backlog, direction, enabled, endiantype, ip, listen, maxopenedconnections, name, permanent, port, sizeascii) VALUES ('iso8583default', 'org.domain.financial.messages.comm.CommAdapterSizePayload', 50, 1, true, 1, 'localhost', false, 1, 'TEF-test-emu', true, 2002, true);"+
				"INSERT INTO connector (messageconf, adapter, backlog, direction, enabled, endiantype, ip, listen, maxopenedconnections, name, permanent, port, sizeascii) VALUES ('iso8583default', 'org.domain.financial.messages.comm.CommAdapterSizePayload', 50, 0, true, 1, 'localhost', true, 1, 'MASTERCARD-test-emu', true, 3001, true);"
			);
/*
	private String name;
	private String parent = "iso8583default";
	private String adapter = "org.domain.financial.messages.MessageAdapterISO8583";
	private String tagPrefix = null;
	private Boolean compress = false;
 */
			properties.put("logLevel", "DEBUG");
			Connector.adjustArgsMap(properties, args);
			Connector manager = Connector.daemon(Test.getArgsFomMap(properties));
			// Disparar clientes
			Message message = new Message();
			message.setMsgType("0200");
			message.setCodeProcess("003000");
			message.setCaptureNsu("1");
			message.setCaptureEc("1");
			message.setEquipamentId("1");
			message.setNumPayments("1");
			message.setPan("1234567890123456");
			message.setTransactionValue("10000");
			Test.sendRequest(manager, "POS-test-emu", message);
			manager.stop();
			java.lang.System.out.print("servidor finalizado");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
