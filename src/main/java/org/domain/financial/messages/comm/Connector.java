package org.domain.financial.messages.comm;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.domain.financial.entity.CommConf;
import org.domain.financial.entity.Log;
import org.domain.financial.messages.Message;
import org.domain.plugins.Plugin;
import org.domain.utils.Logger;

public class Connector implements Logger {
	// atributos internos
	private EntityManager entityManager;
	private boolean isStopped = true;
	private ArrayList<ConnectorServer> servers = new ArrayList<ConnectorServer>(256);
	private ArrayList<SessionClientToServerUnidirecional> clients = new ArrayList<SessionClientToServerUnidirecional>(256);
	private ArrayList<SessionClientToServerBidirecional> bidirecionals = new ArrayList<SessionClientToServerBidirecional>(256);
	private List<CommConf> confs;
	// route
	private List<Message> messagesRouteRef = new ArrayList<Message>(256);
	private String[] fieldsRouteMask = "moduleIn,msgType,codeProcess,root,codeCountry,providerId".split(",");
	// plugin manager
	private final Map<String, Plugin> listInstances = new HashMap<String, Plugin>();
	private final ArrayList<Semaphore> listSemaphores = new ArrayList<Semaphore>();
	// logger
	SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS ");
	private int logLevelsEnabled = 0;
	private long lastLogTime = 0;

	public EntityManager getEntityManager() {
		return entityManager;
	}
	private Plugin loadModule(String className) throws Exception {
		Class<?> _class = Class.forName(className);
		Type[] types = _class.getGenericInterfaces();
		boolean found = false;

		for (Type type : types) {
			String typeName = type.toString();

			if (typeName.equals("interface org.domain.plugins.Plugin")) {
				found = true;
				break;
			}
		}

		if (found == false) {
			throw new Exception("Don't found Plugin interface");
		}

		Plugin instance = null;

		try {
			instance = (Plugin) _class.newInstance();
		} catch (InstantiationException e) {
			throw new Exception("instanciation exception");
		}

		this.listInstances.put(className, instance);
		this.listSemaphores.add(new Semaphore(1));
		return instance;
	}

	public synchronized void log(int logLevel, String header, String text, Message message) {
		String logLevelName = "";

		if (logLevel == Logger.LOG_LEVEL_DEBUG) {
			logLevelName = "DEBUG";
		} else if (logLevel == Logger.LOG_LEVEL_INFO) {
			logLevelName = "INFO";
		} else if (logLevel == Logger.LOG_LEVEL_TRACE) {
			logLevelName = "TRACE";
		} else if (logLevel == Logger.LOG_LEVEL_WARNING) {
			logLevelName = "WARNING";
		} else if (logLevel == Logger.LOG_LEVEL_ERROR) {
			logLevelName = "ERROR";
		}

		if ((logLevel & this.logLevelsEnabled) != 0) {
			try {
				long time = System.currentTimeMillis();
				
				if (time <= this.lastLogTime) {
					time = this.lastLogTime + 1;
				}
				
				String timeStamp = sdf.format(new Date(time));
				Integer transactionId = null;
				String modules = null;
				String objStr = null;
				String root = null;

				if (message != null) {
					objStr = message.toString();
					root = message.getRoot();

					transactionId = message.getId();
					String moduleIn = message.getModuleIn();
					String module = message.getModule();
					String moduleOut = message.getModuleOut();

					if (moduleIn != null && moduleIn.length() > 15) {
						moduleIn = moduleIn.substring(0, 15);
					}

					if (module != null && module.length() > 15) {
						module = module.substring(0, 15);
					}

					if (moduleOut != null && moduleOut.length() > 15) {
						moduleOut = moduleOut.substring(0, 15);
					}

					if (root == null) {
						root = "";
					}

					if (moduleIn != null || module != null || moduleOut != null) {
						modules = String.format("%s -> %s -> %s", moduleIn, module, moduleOut);

						if (modules.length() > 35) {
							modules = modules.substring(0, 35);
						}
					}
				}

				Log log = new Log(timeStamp, logLevelName, transactionId, header, root, modules, text, objStr);
				System.out.println(log);
				entityManager.getTransaction().begin();
				this.entityManager.persist(log);
				entityManager.getTransaction().commit();
//				entityManager.clear();
				this.lastLogTime = time;
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}
	}

	private long commIn(Message messageIn, Message messageOut) {
		long rc = -1;
		String className = messageIn.getModule();
		
		if (className != null) {
			String msgType = messageIn.getMsgType();
			Plugin plugin = listInstances.get(className);
			
			if (plugin == null) {
				try {
					plugin = loadModule(className);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			if (plugin != null) {
				rc = plugin.execute(messageIn);
				messageIn.setMsgTypeResponse(msgType);
			}
		} else {
			rc = commOut(messageIn, messageOut);
		}
		
		return rc;
	}

	public long route(Message messageIn, CommConf conf) throws Exception {
		long rc = -1;
		messageIn.setSendResponse(false);
		Calendar rightNow = Calendar.getInstance();
		int yyyy = rightNow.get(Calendar.YEAR);
		int MM = rightNow.get(Calendar.MONTH);
		int dd = rightNow.get(Calendar.DAY_OF_MONTH);
		int hh = rightNow.get(Calendar.HOUR_OF_DAY);
		int mm = rightNow.get(Calendar.MINUTE);
		int ss = rightNow.get(Calendar.SECOND);
		String systemDateTime = String.format("%04d%02d%02d%02d%02d%02d", yyyy, MM+1, dd, hh, mm, ss);
		messageIn.setSystemDateTime(systemDateTime);

		String module = null;
		Message ref = Message.getFirstCompatible(this.messagesRouteRef, messageIn, this.fieldsRouteMask, false, this, "Connector.route");

		if (ref != null) {
			module = ref.getModule();
			messageIn.copyFrom(ref, false);
		}

		long timeExternal = 0;

		if (module != null) {
			messageIn.setModule(module);
			log(Logger.LOG_LEVEL_TRACE, "Connector.route", String.format("routing to module [%s]", module), messageIn);
			rc = this.commIn(messageIn, messageIn);

			if (rc >= 0) {
				rc = timeExternal;
			}
		} else {
			log(Logger.LOG_LEVEL_ERROR, "Connector.route", "fail to find router destination", messageIn);
		}

		return rc;
	}

	private static boolean checkValue(String data, String value) {
		boolean ret = data != null && data.equals(value);
		return ret;
	}
	
	class ConnectorServer extends Thread {
		private CommConf conf;
		private boolean stopThread = false;
		private ServerSocket server;
		
		class ProcessConnection extends Thread {
			Socket client;

			private boolean processRequest(Comm comm, Message message) {
				log(Logger.LOG_LEVEL_TRACE, "ConnectorServer.req", String.format("[%s] : routing", conf.getName()), message);
				boolean waitResponse = false;
				long rc;

				try {
					rc = Connector.this.route(message, conf);
				} catch (Exception e1) {
					rc = -1;
					log(Logger.LOG_LEVEL_ERROR, "ConnectorServer.req", String.format("[%s] : exception in route", conf.getName()), message);
					e1.printStackTrace();
				}

				if (rc >= 0) {
					Boolean sendResponse = message.getSendResponse();

					if (sendResponse != null && sendResponse == true) {
						try {
							comm.send(message);
							String replyEspected = message.getReplyEspected();
							waitResponse = Connector.checkValue(replyEspected, "1");
						} catch (Exception e) {
							log(Logger.LOG_LEVEL_ERROR, "ConnectorServer.req",
									String.format("[%s] : exception in send response : %s", conf.getName(), e.getMessage()), message);
							e.printStackTrace();
						}
					}
				} else {
					log(Logger.LOG_LEVEL_ERROR, "ConnectorServer.req",
							String.format("[%s] : fail to route", conf.getName()), message);
				}
				
				if (stopThread == false) {
					waitResponse = false;
				}

				return !waitResponse;
			}

			class ServerPermanentProcessRequest extends Thread {
				private Message message;
				private Comm comm;

				@Override
				public void run() {
					processRequest(comm, message);
				}

				public ServerPermanentProcessRequest(CommConf conf, Comm comm,
						Message message) {
					this.comm = comm;
					this.message = message;
				}
			}
			
			@Override
			public void run() {
				log(Logger.LOG_LEVEL_DEBUG, "ConnectorServer.run",
						String.format("conexao recebida do cliente [%s - %s - %s]", ConnectorServer.this.conf.getName(), client.getPort(), client.getLocalPort()), null);
				Comm comm;

				try {
					comm = new Comm(client, ConnectorServer.this.conf, Connector.this);
				} catch (Exception e2) {
					log(Logger.LOG_LEVEL_DEBUG, "ConnectorServer.run",
							String.format("fail in comm initialize, module [%s]", ConnectorServer.this.conf.getName()), null);
					e2.printStackTrace();
					return;
				}

				boolean stopComm;

				do {
					Message message = new Message();

					try {
						int size = comm.receive(message, null);

						if (size <= 0) {
							log(Logger.LOG_LEVEL_ERROR, "ConnectorServer.run",
									String.format("[%s] : Conexao encerrada inesperadamente", ConnectorServer.this.conf.getName()), message);
							break;
						}

						log(Logger.LOG_LEVEL_TRACE, "ConnectorServer.run",
								String.format("[%s] : routing", ConnectorServer.this.conf.getName()), message);
					} catch (Exception e) {
						// a conexão foi fechada pelo cliente
						break;
					}

					if (conf.getPermanent()) {
						ServerPermanentProcessRequest processRequest = new ServerPermanentProcessRequest(ConnectorServer.this.conf, comm,	message);
						processRequest.start();
						stopComm = ConnectorServer.this.stopThread;
					} else {
						stopComm = processRequest(comm, message);
					}
				} while (stopComm == false);
			}
			
			public ProcessConnection(Socket client) {
				this.client = client;
			}
		}

		@Override
		public void run() {
			while (stopThread == false) {
				try {
					Socket client = server.accept();
					ProcessConnection processConnection = new ProcessConnection(client);
					processConnection.start();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					System.out.printf("Error in server.accept() from port %d\n", ConnectorServer.this.conf.getPort());
					e.printStackTrace();
				}
			}
		}
		
		public void closeServer() {
			try {
				this.stopThread = true;
				this.server.close();
			} catch (IOException e) {
				log(Logger.LOG_LEVEL_ERROR, "C.Server.closeServer", e.getMessage(), null);
			}
		}

		public ConnectorServer(CommConf conf) throws Exception {
			this.server = new ServerSocket(conf.getPort(), conf.getBacklog());
			this.conf = conf;
		}
	}
	
	interface SessionClient {
		public long execute(Message messageOut, Message messageIn);
	}

	// acesso aos servidores que não mandam sonda e ou outras solicitações na ordem inversa da conexão
	class SessionClientToServerUnidirecional implements SessionClient {
		private Comm poolComm[];
		private CommConf conf;
		Logger logger;
		
		private Comm newComm(int pool) {
			Comm comm = null;
			
			try {
				comm = new Comm(this.conf, logger);
				this.poolComm[pool] = comm;
			} catch (Exception e) {
				log(Logger.LOG_LEVEL_ERROR, "C.SClient.newComm",
						String.format("error in new conection : %s [%s] : %s", pool, conf.getName(), e.getMessage()),
						null);
			}

			return comm;
		}

		private Comm getPool(Message message) {
			Comm comm = null;
			Integer pool = message.getPoolCommId();

			if (pool != null) {
				if (pool >= 0 && pool < this.poolComm.length) {
					comm = this.poolComm[pool];
				} else {
					logger.log(Logger.LOG_LEVEL_ERROR, "C.SClient.getComm",
							String.format("invalid pool : %s [%s]", pool, conf.getName()), message);
				}
			} else {
				for (int i = 0; i < this.poolComm.length; i++) {
					if (this.poolComm[i] == null) {
						comm = newComm(i);
						message.setPoolCommId(i);
						break;
					}
				}

				if (comm == null) {
					logger.log(Logger.LOG_LEVEL_ERROR, "C.SClient.getComm",
							String.format("missing free comm [%s]", conf.getName()), message);
				}
			}

			return comm;
		}

		private long releasePool(Message message) {
			long rc = -1;

			if (this.conf.getPermanent()) {
				logger.log(Logger.LOG_LEVEL_ERROR, "C.SClient.releasePool",
						String.format("error in closing permanent conection : [%s]", conf.getName()),
						message);
				return rc;
			}
			
			Integer pool = message.getPoolCommId();
			Comm comm = this.poolComm[pool];

			if (comm != null) {
				try {
					comm.close();
					logger.log(Logger.LOG_LEVEL_TRACE, "C.SClient.releasePool",
							String.format("closed conection : %s [%s]", pool, conf.getName()), message);
					rc = 0;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					logger.log(Logger.LOG_LEVEL_ERROR, "C.SClient.releasePool",
							String.format("error in closing conection : %s [%s] : %s", pool, conf.getName(), e.getMessage()),
							message);
				}

				this.poolComm[pool] = null;
			}

			message.setPoolCommId(null);
			return rc;
		}

		public long execute(Message messageOut, Message messageIn) {
			long rc = 0;
			Comm comm;
			
			synchronized (this.poolComm) {
				if (this.conf.getPermanent()) {
					comm = this.poolComm[0];
					
					if (comm == null) {
						comm = newComm(0);
					}
				} else {
					comm = getPool(messageOut);
				}
			}

			if (comm == null) {
				return -1;
			}

			try {
				// não precisa fazer nada, basta assumir o timeout padrão
				Integer timeout = messageOut.getTimeout();

				if (timeout == null || timeout == 0) {
					timeout = 30000;
				}

				comm.send(messageOut);
				boolean waitResponse = Connector.checkValue(messageOut.getReplyEspected(), "1");

				if (waitResponse == true) {
					comm.receive(messageIn, messageOut);
				} else if (this.conf.getPermanent() == false) {
					synchronized (this.poolComm) {
						releasePool(messageOut);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				log(Logger.LOG_LEVEL_ERROR, "C.SessionClient.execute",
						String.format("[%s] : %s", this.conf.getName(), e.getMessage()), messageOut);
				rc = -1;
			}

			return rc;
		}

		public SessionClientToServerUnidirecional(CommConf conf, Logger logger) throws Exception {
			this.conf = conf;
			this.logger = logger;
			this.poolComm = new Comm[conf.getMaxOpenedConnections()];
		}
	}

	// acesso aos servidores mandam sonda e ou outras solicitações na ordem inversa da conexão (ex.: OI. Claro, Bancos, etc...)
	class SessionClientToServerBidirecional extends Thread implements SessionClient {
		private Comm comm;
		private CommConf conf;
		private ArrayList<Message> listSend;
		private ArrayList<Message> listReceive;
		private String[] fieldsCompare;
		ServerSocket server;
		
		// processa as requisições (Sondas) das AUTORIZADORAS (OI. Claro, etc...)
		class SessionBidirecionalProcessRequest extends Thread {
			Message transaction;

			@Override
			public void run() {
				try {
					Connector.this.route(transaction, SessionClientToServerBidirecional.this.conf);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			public SessionBidirecionalProcessRequest(Message transaction) {
				this.transaction = transaction;
			}
		}

		public void closeServer() {
			if (this.server != null) {
				try {
					this.server.close();
				} catch (IOException e) {
					Connector.this.log(Logger.LOG_LEVEL_ERROR, "C.Bid.closeServer", e.getMessage(), null);
				}
			}
		}

 		public int getCount() {
			return this.listSend.size();
		}

		@Override
		public void run() {
			try {
				// fica bloqueante até que a conexão seja estabelecida, ou até que a
				// flag de cancelamento seja ativada.
				if (this.conf.getListen() == true) {
					this.server = new ServerSocket(this.conf.getPort(), this.conf.getBacklog());
					Connector.this.log(Logger.LOG_LEVEL_TRACE, "C.Bidirecional.run",
							String.format("servidor levantado [%s : %s]", this.conf.getName(), this.conf.getPort()), null);
					Socket socket = server.accept();
					Connector.this.log(
							Logger.LOG_LEVEL_TRACE,
							"C.Bidirecional.run",
							String.format("server.accept() [%s : %s : %s]", this.conf.getName(), this.conf.getPort(),
									socket.getRemoteSocketAddress()), null);
					this.comm = new Comm(socket, this.conf, Connector.this);
				} else {
					this.comm = new Comm(this.conf, Connector.this);
				}

				while (isStopped == false) {
					Message messageRef = null;
					Message messageIn = new Message();
					int size = this.comm.receive(messageIn, null);

					if (size <= 0) {
						messageIn = null;
						continue;
					}
					// check if received message match witch any of already send
					synchronized (this.listSend) {
						messageRef = Message.getFirstCompatible(this.listSend, messageIn, fieldsCompare, true, Connector.this, "C.Bidirecional.run");
					}

					if (messageRef != null) {
						synchronized (messageRef) {
							// quando está rodando com o Simulador é necessário aguardar
							// que entre o wait antes do notify,
							// para este fim foi criada a variável lockNofify.
							while (messageRef.lockNotify == true) {
								Connector.this.log(Logger.LOG_LEVEL_TRACE, "C.Bidirecional.run",
										String.format("%s - waiting lockNotify...", this.conf.getName()), messageRef);
								Thread.sleep(100);
							}

							messageRef.clear();
							messageRef.copyFrom(messageIn, false);
							messageRef.transmissionTimeout = false;
							messageRef.notify();
						}
					} else {
						SessionBidirecionalProcessRequest request = new SessionBidirecionalProcessRequest(messageIn);
						request.start();
						// inserir na lista de transações recebidas
						synchronized (this.listReceive) {
							this.listReceive.add(messageIn);
						}
						// TODO : ativar sinal de notificação para averiguar as SONDAS
						// this.semaphoreReceive.notify();
					}
				}
			} catch (Throwable e1) {
				e1.printStackTrace();
				// log(Logger.LOG_LEVEL_ERROR, "Connector.SessionBidirecional.run",
				// String.format("[%s] : %s", this.conf.getName(), e1.getMessage()));
			}
		}

		public long execute(Message messageOut, Message messageIn) {
			// TODO : identificar a operadora e acionar o execute de seu Session
			// vinculado
			if (this.comm == null) {
				Connector.this.log(Logger.LOG_LEVEL_ERROR, "C.SessionBidirecional.execute",
						String.format("sem conexão [%s]", this.conf.getName()), messageOut);
				return -1;
			}

			long rc = 0;

			try {
				String reply = messageOut.getReplyEspected();
				boolean waitResponse = Connector.checkValue(reply, "1");
				// se não tiver timeout definido, vou assumir o timeout padrão
				long timeout = messageOut.getTimeout();

				if (timeout == 0) {
					timeout = 30000;
				}

				if (waitResponse) {
					synchronized (this.listSend) {
						this.listSend.add(messageOut);
					}
				}

				this.comm.send(messageOut);

				if (waitResponse) {
					try {
						Connector.this.log(Logger.LOG_LEVEL_TRACE, "C.SessionBidirecional.execute",
								String.format("wait response in %d ms [%s] ...", timeout, this.conf.getName()), messageOut);

						synchronized (messageOut) {
							messageOut.lockNotify = false;
							messageOut.wait(timeout);
						}

						Connector.this.log(Logger.LOG_LEVEL_TRACE, "C.SessionBidirecional.execute",
								String.format("... wait signed [%s]", this.conf.getName()), messageOut);

						if (messageOut.transmissionTimeout == true) {
							rc = -1;
						}
					} catch (Exception e) {
						rc = -1;
					}
				}
			} catch (Exception e) {
				log(Logger.LOG_LEVEL_ERROR, "C.SessionBidirecional.execute",
						String.format("[%s] : %s", this.conf.getName(), e.getMessage()), messageOut);
				// TODO Auto-generated catch block
				e.printStackTrace();
				rc = -1;
			}

			return rc;
		}

		public SessionClientToServerBidirecional(CommConf conf, String[] fieldsCompare)	throws Exception {
			this.conf = conf;
			this.fieldsCompare = fieldsCompare;
			this.comm = null;
			this.listSend = new ArrayList<Message>(conf.getMaxOpenedConnections());
			this.listReceive = new ArrayList<Message>(conf.getMaxOpenedConnections());
		}
	}

	public boolean getIsStopped() {
		return this.isStopped;
	}
	
	@SuppressWarnings("unchecked")
	public void start(EntityManager entityManager, String logLevel) {
		this.logLevelsEnabled = 0;
		
		if ("ERROR".equals(logLevel)) {
			this.logLevelsEnabled = Logger.LOG_LEVEL_ERROR;
		} else if ("WARNING".equals(logLevel)) {
			this.logLevelsEnabled = Logger.LOG_LEVEL_WARNING;
		} else if ("TRACE".equals(logLevel)) {
			this.logLevelsEnabled = Logger.LOG_LEVEL_TRACE;
		} else if ("INFO".equals(logLevel)) {
			this.logLevelsEnabled = Logger.LOG_LEVEL_INFO;
		} else if ("DEBUG".equals(logLevel)) {
			this.logLevelsEnabled = Logger.LOG_LEVEL_DEBUG;
		}

		this.entityManager = entityManager;
		log(Logger.LOG_LEVEL_TRACE, "Connector.start", "inicializando...", null);
		stop();
		this.isStopped = false;
		// TODO : recarregar messagesRouteRef do banco de dados
		Comm.loadConfs(entityManager, this);
		this.confs = entityManager.createQuery("from CommConf").getResultList();

		for (CommConf conf : this.confs) {
			if (conf.getEnabled() == false) {
				continue;
			}

			String moduleName = conf.getName();
			boolean found = false;

			try {
				log(Logger.LOG_LEVEL_DEBUG, "Connector.start", String.format("avaliando conexao : %s", conf), null);

				if (conf.getListen() == false && conf.getPermanent() == false && conf.getDirection() == CommConf.RequestsDirection.CLIENT_TO_SERVER) {
					log(Logger.LOG_LEVEL_TRACE, "Connector.start",
							String.format("habilitando cliente : %s porta %s", conf.getName(), conf.getPort()), null);
					SessionClientToServerUnidirecional session = new SessionClientToServerUnidirecional(conf, this);
					this.clients.add(session);
					found = true;
				}

				for (int j = 0; j < conf.getMaxOpenedConnections(); j++) {
					if ((conf.getListen() == true && conf.getDirection() == CommConf.RequestsDirection.CLIENT_TO_SERVER)) {
						try {
							ConnectorServer session = new ConnectorServer(conf);
							log(Logger.LOG_LEVEL_TRACE, "Connector.start",
									String.format("iniciado servidor : %s porta %s", conf.getName(), conf.getPort()), null);
							session.start();
							this.servers.add(session);
						} catch (Exception e) {
							log(Logger.LOG_LEVEL_ERROR, "Connector.start", String.format(
									"nao foi possivel iniciar servidor (ja existe o servico ?) : %s porta %s", conf.getName(),
									conf.getPort()), null);
							break;
						}
					} else if (conf.getDirection() == CommConf.RequestsDirection.BIDIRECIONAL) {
						String[] fieldsCompare = new String[] { "providerEC", "equipamenId", "captureNSU" };
						log(Logger.LOG_LEVEL_TRACE, "Connector.start", String.format("iniciado sessao bidirecional : %s porta %s", conf.getName(), conf.getPort()), null);
						SessionClientToServerBidirecional session = new SessionClientToServerBidirecional(conf, fieldsCompare);
						session.setDaemon(true);
						session.start();
						this.bidirecionals.add(session);
					} else if (found == false) {
						System.out.println("Connector.start : invalid communication configuration for module " + moduleName);
						break;
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue;
			}
		}

		log(Logger.LOG_LEVEL_TRACE, "Connector.start", "...iniciado", null);
	}

	public void stop() {
		// TODO : aguardar todas as sessons finalizarem
		log(Logger.LOG_LEVEL_TRACE, "Connector.stop",	"--------------------------------------------------------------------------------------", null);
		log(Logger.LOG_LEVEL_TRACE, "Connector.stop", "Finishing sessions...", null);
		this.isStopped = true;
		
		for (SessionClientToServerBidirecional session : this.bidirecionals) {
			session.closeServer();
		}
		
		for (ConnectorServer session : this.servers) {
			session.closeServer();
		}
		// exclui as sess�es antigas para garantir que ap�s o start as transa��es
		// sejam feitas coms as configura��es atualizadas
		this.clients.clear();
		this.bidirecionals.clear();
		this.servers.clear();
		log(Logger.LOG_LEVEL_TRACE, "Connector.stop", "...sessions finischieds.", null);
		log(Logger.LOG_LEVEL_TRACE, "Connector.stop",	"--------------------------------------------------------------------------------------", null);
	}

	private long commOut(Message messageOut, Message messageIn) {
		long rc = -1;
		String name = messageOut.getModuleOut();

		if (name == null || name.length() <= 0) {
			log(Logger.LOG_LEVEL_ERROR, "Connector.commOut", "parameter 'name' is invalid", messageOut);
			return rc;
		}

		if (name.toUpperCase().equals("PROVIDER")) {
			name = messageOut.getProviderName();
		}
		
		SessionClient sessionClient = null;

		for (int i = 0; i < this.clients.size(); i++) {
			SessionClientToServerUnidirecional client = clients.get(i);
			String clientName = client.conf.getName();
			log(Logger.LOG_LEVEL_DEBUG, "Connector.commOut", String.format("testing SessionClient client [%s] for [%s]", clientName, name), messageOut);

			if (clientName.equals(name)) {
				sessionClient = client;
				break;
			}
		}

		if (sessionClient == null) {
			for (int i = 0; i < bidirecionals.size(); i++) {
				SessionClientToServerBidirecional client = bidirecionals.get(i);
				String clientName = client.conf.getName();
				log(Logger.LOG_LEVEL_DEBUG, "Connector.commOut", String.format("testing SessionBidirecional clien [%s] for [%s]", clientName, name), messageOut);

				if (clientName.equals(name) && client.getCount() < client.conf.getMaxOpenedConnections()) {
					sessionClient = client;
					break;
				}
			}
		}

		if (sessionClient != null) {
			log(Logger.LOG_LEVEL_TRACE, "Connector.commOut", String.format("sending to server [%s]", name), messageOut);
			rc = sessionClient.execute(messageOut, messageIn);
		} else {
			log(Logger.LOG_LEVEL_ERROR, "Connector.commOut", String.format("don't found connection for requested module [%s]", name), messageOut);
		}

		return rc;
	}

	public static void adjustArgsMap(Map<String, String> map, String[] args) {
		for (String name : map.keySet()) {
			for (String arg : args) {
				if (arg.startsWith("--" + name + "=")) {
					String value = arg.substring(name.length()+2+1);
					map.put(name, value);
				}
			}
		}
	}
	
	public static Connector daemon(String[] args) {
		Map<String, String> propertiesPersistence = new HashMap<String, String>();
		propertiesPersistence.put("javax.persistence.jdbc.driver", "org.h2.Driver");
		propertiesPersistence.put("javax.persistence.jdbc.url", "jdbc:h2:tcp://localhost/~/iso8583router");
		propertiesPersistence.put("javax.persistence.jdbc.user", "sa");
		propertiesPersistence.put("javax.persistence.jdbc.password", "sa");
		propertiesPersistence.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
		propertiesPersistence.put("hibernate.hbm2ddl.auto", "update");
		Connector.adjustArgsMap(propertiesPersistence, args);
		EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("principal", propertiesPersistence);
		Map<String, String> propertiesConnector = new HashMap<String, String>();
		propertiesConnector.put("logLevel", "INFO");
		Connector.adjustArgsMap(propertiesConnector, args);
		Connector manager = new Connector();
		manager.start(entityManagerFactory.createEntityManager(), propertiesConnector.get("logLevel").toUpperCase());
		return manager;
	}
	
	public static void main(String[] args) {
		try {
			Connector manager = Connector.daemon(args);
			java.lang.System.out.print("press 'q' to stop and exit !");
			char ch;
			
			do {
				ch = (char) System.in.read();
			} while (ch != 'q');
			
			manager.stop();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		java.lang.System.out.print("servidor finalizado");
	}

}
