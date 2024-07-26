package dns;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class representing a DNS Server.
 *
 * @version 1.0
 */
public class DNSServer {
    
    // DNS uses port UDP port 53 for the server

    final private int PORT = 53;

    // set the maximum packet size to be 512 bytes for DNS messages
    final private int MAX_SIZE = 512;

    // this server will handle requests for a single zone/domain
    private DNSZone zone;

    // add class variable for the cache
    private DNSCache cache;
    // add class variable to track pending queries
    private HashMap<Integer, DNSMessage> pendingQueries;

    // all queries sent from this server will go to a single "upstream" server

    private InetAddress nextServer;
    private int nextServerPort;

    public DNSServer(DNSZone zone) {
        this.zone = zone;

        // add a DNSCache object */
        this.cache = new DNSCache();
        // set our upstream server to be 127.0.0.53
        // note: we're assuming this is running on a recent Ubuntu system
        //       and we're using the Ubuntu server as our upstream
        try {
            nextServer = InetAddress.getByName("127.0.0.53");
        } catch(UnknownHostException e) {
            System.out.println("Should never get here.");
            System.exit(0);
        }
        nextServerPort = 53;

        System.out.printf("Starting server on port %d%n", PORT);
    }

    // handle one incoming DNS query message
    private DatagramPacket handleQuery(DNSMessage query) {
        // print the query message contents
        System.out.println("Query received from " + query.getPacket().getSocketAddress());
        System.out.println(query);

        // look for the record in our zone
        boolean inZone = true;
        var records = zone.getRecords(query.getQuestionName(), query.getQuestionType(), query.getQuestionClass());
        
        ArrayList<DNSRecord> cachedRecord;
        
        // look for the record in the cache if it's not in our zone
        if (records.size() == 0) {
        	cachedRecord = cache.lookup(query.getQuestionName(), query.getQuestionType(), query.getQuestionClass());
        } else {
        	cachedRecord = null; 
    	}

        // send the response back to the client if we found the record either in the zone or the cache
        if(records.size() != 0 || cachedRecord != null) {
            // make a response message
            var reply = new DNSMessage(query, records, inZone);

            // print the response message contents
            System.out.println("Reply to " + query.getPacket().getSocketAddress());
            System.out.println(reply);

            // make and return a response packet
            return new DatagramPacket(reply.getData(), reply.getDataLength(), query.getPacket().getSocketAddress());
        } else {
            // if we didn't find the record, send to the next server
            System.out.println("Forwarding query to " + nextServer + ":" + nextServerPort);

            // store the query so we can respond to it when we get a reply
            pendingQueries.put(query.getID(), query);

            // make and return a new DatagramPacket query packet to forward
            return new DatagramPacket(query.getData(), query.getDataLength(), nextServer, nextServerPort);
        }
    }


    private DatagramPacket handleReply(DNSMessage reply) {
        // print the reply message contents
        System.out.println("Reply received from " + reply.getPacket().getSocketAddress());
        System.out.println(reply);

        // match the reply to the original query
        int originalQueryID = reply.getID();
        DNSMessage originalQuery = pendingQueries.get(originalQueryID);

        if (originalQuery != null) {
        // add answers to the cache
		cache.addRecords(originalQuery.getQuestionName(), originalQuery.getQuestionType(), originalQuery.getQuestionClass(), reply.getAnswers());
        // remove the original query from the outstanding set
		pendingQueries.remove(originalQueryID);
        // print the reply message again for consistency
		System.out.println("Matched Reply to Original Query:");
        System.out.println(reply);
        // make and return a new response packet to send to the original client
        return new DatagramPacket(reply.getData(), reply.getDataLength(), originalQuery.getPacket().getSocketAddress());
        }
        return null;
    }

    private DatagramPacket handleMessage(DatagramPacket incomingPkt) {
        // create a DNS Message object that will parse the request packet data
        var incomingMessage = new DNSMessage(incomingPkt);

        // handle queries
        if(incomingMessage.isQuery()) {
            return handleQuery(incomingMessage);
        }

        // handle replies
        else {
            return handleReply(incomingMessage);
        }
    }

    // Open a socket to receive UDP packets and handle those packets
    public void run() {
        // open the socket, ensure it will close when the try block finishes
        try (
            // listen on localhost only
            var sock = new DatagramSocket(PORT, InetAddress.getLoopbackAddress());
        ) {
            // keep reading packets one at a time, forever
            while(true) {
                // packet to store the incoming message
                var in_packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);

                // blocking call, read one packet
                sock.receive(in_packet);

                // handle this packet
                var out_packet = handleMessage(in_packet);

                // only send a response if there were no errors
                if (out_packet != null) {
                    sock.send(out_packet);
                }
            }
        } catch(IOException e) {
            // Have to catch IOexceptions for most socket calls
            System.out.println("Network error!");
        }
    }

    public static void main(String[] args) {
        // must have exactly a single command line argument
        if(args.length != 1) {
            System.out.println("Usage: sudo java dns.DNSServer zone_file");
            System.exit(0);
        }

        // make the zone, which will exit() if the file is invalid in any way
        var zone = new DNSZone(args[0]);

        // make the server object then start listening for DNS requests
        var server = new DNSServer(zone);
        server.run();
    }
}