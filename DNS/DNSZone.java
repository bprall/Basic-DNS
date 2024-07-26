package dns;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.NumberFormatException;
import java.util.NoSuchElementException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Class representing a single DNS zone file.
 *
 * @version 1.0
 */
public class DNSZone {

    private ArrayList<DNSRecord> records;
    
    public DNSZone(String zonefile_name) {
        // make the records list, fill it in in parseFile()
        records = new ArrayList<DNSRecord>();
        parseFile(zonefile_name);
    }

    private void parseFile(String zonefile_name) {
        // open a Scanner to read the file
        try (var scanner = new Scanner(new File(zonefile_name))) {

            // every line should be a complete record
            while(scanner.hasNextLine()) {

                // split on spaces
                var tokens = scanner.nextLine().split("\\s+");

                // if the line was empty (except for whitespace), then skip it
                if(tokens.length == 1 && tokens[0].isEmpty()) {
                    continue;
                }

                // each record is of the form: NAME TTL CLASS TYPE RDATA
                if(tokens.length != 5) {
                    System.out.println("Error in zone file: format incorrect.");
                    System.exit(0);
                }

                // get the five values, all as Strings except for the TTL
                var record_name = tokens[0];
                var record_ttl = Integer.parseInt(tokens[1]);
                var record_class = tokens[2];
                var record_type = tokens[3];
                var record_data = tokens[4];

                // for now, only support class IN records
                if(!record_class.equals("IN")) {
                    System.out.println("Error in zone file: non-IN record found.");
                    System.exit(0);
                }

                // make a new record object to store the details for this record and add it to the list
                var record = new DNSRecord(record_name, record_ttl, record_class, record_type, record_data);
                records.add(record);
            }
        } catch(NumberFormatException e) {
            // any mismatch in types means the file was formatted incorrectly
            System.out.println("Error in zone file: format incorrect.");
            System.exit(0);
        } catch(NoSuchElementException e) {
            // any time we read and can't that means the file was formatted incorrectly
            System.out.println("Error in zone file: format incorrect.");
            System.exit(0);
        } catch(FileNotFoundException e) {
            // print a specific message if the file wasn't found in the first place
            System.out.println("Error: zone file not found.");
            System.exit(0);
        }
    }

    public ArrayList<DNSRecord> getRecords(String name, String type, String rclass) {
        var matches = new ArrayList<DNSRecord>();

        for (var record : records) {
            if(record.getName().equals(name) &&
               record.getClassStr().equals(rclass) &&
               record.getTypeStr().equals(type)) {
                matches.add(record);
            }
        }

        return matches;
    }
}