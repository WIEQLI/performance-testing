package uk.ac.open.kmi.ptAnywhere;

import com.cisco.pt.ipc.sim.Network;
import com.cisco.pt.ipc.sim.CiscoDevice;
import com.cisco.pt.ipc.ui.IPC;
import com.cisco.pt.ipc.IPCFactory;
import com.cisco.pt.ptmp.ConnectionNegotiationProperties;
import com.cisco.pt.ptmp.PacketTracerSession;
import com.cisco.pt.ptmp.PacketTracerSessionFactory;
import com.cisco.pt.ptmp.impl.PacketTracerSessionFactoryImpl;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This class allows you to check when a Packet Tracer session is up and
 * able to answer IPC requests.
 *
 * Also, it can be used to measure the average response time of the instance
 * for a predefine request.
 *
 * Note that this class depends on the ptipc library.
 * I cannot provide a version of it as its intellectual property belongs to Cisco.
 */
public class PTChecker extends PacketTracerClient {

    final static int defaultWaitTime = 5;
    final static int retryMiliseconds = 500;

    public PTChecker(String host, int port) {
        super(host, port);
    }

    protected long waitUntilPTResponds(int maxWaitingSeconds) throws Exception {
        int waitingMs = maxWaitingSeconds * 1000;
        final long init = System.currentTimeMillis();
        while (waitingMs>0) {
            final long initLoop = System.currentTimeMillis();
            try {
                final IPC ipc = getIPC();
                final Network network = this.ipcFactory.network(ipc);
                final CiscoDevice dev = (CiscoDevice) network.getDevice("MySwitch");
                if (dev!=null) return System.currentTimeMillis() - init;  // elapsed
            } catch(Error e) {
                long elapsedLoop = System.currentTimeMillis() - initLoop;
                if (elapsedLoop<PTChecker.retryMiliseconds) Thread.sleep(PTChecker.retryMiliseconds-elapsedLoop);
                waitingMs -= PTChecker.retryMiliseconds;
            }
        }
        return -1;  // In miliseconds
    }

    protected long getAverageResponseTime(int repetitions) {
        // TODO
        return 0;  // In miliseconds
    }

    public static void main(String[] args) throws Exception {
        if (args.length<2) {
            System.out.println("usage: java PTChecker hostname port [wait]\n");
            System.out.println("Checks the time needed to contact a PacketTracer instance.\n");
            System.out.println("\thostname\tstring with the name of the Packet Tracer instance host.");
            System.out.println("\tport    \tan integer for the port number of the Packet Tracer instance.");
            System.out.println("\twait    \t(optional, default: " + PTChecker.defaultWaitTime +
                                            ") number of seconds that the program will retry connections.");
        } else {
            Logger logger = Logger.getLogger("com.cisco.pt");

            // Now set its level. Normally you do not need to set the
            // level of a logger programmatically. This is usually done
            // in configuration files.
            logger.setLevel(Level.OFF);

            int waitTime = PTChecker.defaultWaitTime;
            if(args.length>=3) {
                waitTime = Integer.parseInt(args[2]);
            }
            final PTChecker checker = new PTChecker(args[0], Integer.parseInt(args[1]));
            System.out.println( checker.waitUntilPTResponds(waitTime) );

            checker.stop();
            //checker.getAverageResponseTime(100);
        }
    }
}

abstract class PacketTracerClient {
    protected PacketTracerSession packetTracerSession;
  	protected IPCFactory ipcFactory;
    final protected String hostName; // "localhost";
  	final protected int port;

    public PacketTracerClient(String hostName, int port) {
        this.hostName = hostName;
        this.port = port;
    }

    public void start() throws Exception {
        final PacketTracerSessionFactory sessionFactory = PacketTracerSessionFactoryImpl.getInstance();
        this.packetTracerSession = createSession(sessionFactory);
        this.ipcFactory = new IPCFactory(this.packetTracerSession);
    }

    public IPC getIPC() throws Exception {
        if (this.ipcFactory==null) start();
        return this.ipcFactory.getIPC();
    }

    public void stop() throws Exception {
        if (this.packetTracerSession!=null)
            this.packetTracerSession.close();
    }

  	protected PacketTracerSession createSession(PacketTracerSessionFactory sessionFactory) throws Exception {
       return createDefaultSession(sessionFactory);
  	}

  	protected PacketTracerSession createDefaultSession(PacketTracerSessionFactory sessionFactory) throws Exception {
        return sessionFactory.openSession(this.hostName, this.port);
  	}

  	protected PacketTracerSession createSession(PacketTracerSessionFactory sessionFactory, ConnectionNegotiationProperties negotiationProperties) throws Exception {
        return sessionFactory.openSession(this.hostName, this.port, negotiationProperties);
  	}
}
