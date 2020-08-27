package com.umarabdul.networking.jmailer;

import java.util.*;
import java.io.*;
import com.umarabdul.wrapper.socketwrapper.SocketWrapper;
import com.umarabdul.util.argparser.ArgParser;


/**
* JMailer is a simple library for sending emails using an SMTP server.
*
* @author Umar Abdul
* @version 1.1
* @since 2020
*/

public class JMailer{

  private String host;
  private int port;
  private String domain;
  private ArrayList<String> recipients;
  private String sender = null;
  private String subject = null;
  private boolean useSSL;

  /**
  * JMailer's constructor.
  * @param host Host running SMTP server.
  * @param port SMTP port.
  * @param domain Domain name to use in EHLO command.
  * @param useSSL Use SSL connection.
  */
  public JMailer(String host, int port, String domain, boolean useSSL){

    this.host = host;
    this.port = port;
    this.domain = domain;
    this.useSSL = useSSL;
    recipients = new ArrayList<String>();
  }

  /**
  * Add an email address to list of recipients.
  * @param address Email address of recipient.
  */
  public void addRecipient(String address){

    if (!(recipients.contains(address)))
      recipients.add(address);
  }

  /**
  * Remove an email address from list of recipients.
  * @param address Email address to remove.
  */
  public void removeRecipient(String address){
    recipients.remove(address);
  }

  /**
  * Obtain the ArrayList of recipients.
  * @return An ArrayList of email recipients.
  */
  public ArrayList<String> getRecipients(){
    return recipients;
  }

  /**
  * Set email address of sender.
  * @param address Email address of sender.
  */
  public void setSender(String address){
    sender = address;
  }

  /**
  * Set subject of email to send.
  * @param subject Email subject.
  */
  public void setSubject(String subject){
    this.subject = subject;
  }

  /**
  * Send the email, provided all requirements are satisfied.
  * @param body Body of email to send.
  * @return {@code true} on success.
  */
  public boolean send(String body){

    try{
      if (sender == null || subject == null || recipients.size() == 0)
        return false;
      String rsp = null;
      // Create and wrap socket.
      SocketWrapper conn = new SocketWrapper(host, port, useSSL);
      conn.read(1024); // banner.
      conn.write(String.format("EHLO %s\r\n", domain));
      rsp = conn.read(1024);
      if (!(rsp.startsWith("250")))
        return false;
      // Add mail sender and recipients addresses.
      conn.write(String.format("mail from: %s\r\n", sender));
      rsp = conn.read(1024);
      if (!(rsp.startsWith("250")))
        return false;
      for (String addr : recipients){
        conn.write(String.format("rcpt to: %s\r\n", addr));
        rsp = conn.read(1024);
        if (!(rsp.startsWith("250")))
          return false;
      }
      conn.write("data\r\n");
      rsp = conn.read(1024);
      if (!(rsp.startsWith("354")))
        return false;
      String data = String.format("Subject: %s\r\n\r\n", subject);
      data += body;
      data += "\r\n.\r\n";
      conn.write(data);
      rsp = conn.read(1024);
      if (!(rsp.startsWith("250")))
        return false;
      conn.write("quit\r\n");
      conn.getSocket().close();
      return true;
    }catch(Exception e){
      System.out.println(String.format("[-] JMailer: %s : %s", e.getClass().getName(), e.getMessage()));
      return false;
    }
  }

  /**
  * Used by JMailer.main() for prompting user input.
  * @param p Prompt to display.
  */
  public static void prompt(String p){
    System.out.print("[?] " +p+ " > ");
  }

  /**
  * Run JMailer from terminal.
  * @param args Command-line args.
  * @throws IOException on mail file read error.
  */
  public static void main(String[] args) throws IOException{

    String helpPage = "JMailer v1.0 - Java SMTP client  (Author: https://github.com/UmarAbdul01)\n"+
                      "       Usage: jmailer [options]\n"+
                      "     Options:\n"+
                      "             -h|--host       <host>             :  Server host/IP\n"+
                      "             -p|--port       <port>             :  Server port (default: 25)\n"+
                      "             -d|--domain     <domain>           :  EHLO domain\n"+
                      "             -s|--sender     <address>          :  Sender's address\n"+
                      "           -sub|--subject    <mail_subject>     :  Subject of email\n"+
                      "             -r|--recipient  <addr1,addr2,...>  :  Recipients address\n"+
                      "             -b|--body       <filename>         :  File containing mail body\n"+
                      "               |--ssl        <bool>             :  Use SSL connection\n"+
                      "               |--help                          :  Print this help page";
    ArgParser agp = new ArgParser(args);
    agp.setAlias("host", "h");
    agp.setAlias("port", "p");
    agp.setDefault("port", "25");
    agp.setAlias("domain", "d");
    agp.setDefault("domain", "localhost");
    agp.setAlias("sender", "s");
    agp.setAlias("subject", "sub");
    agp.setAlias("recipient", "r");
    agp.setAlias("body", "b");
    agp.setDefault("ssl", "false");
    if (agp.hasArg("--help")){
      System.out.println(helpPage);
      return;
    }
    if (!(agp.hasKWarg("host") && agp.hasKWarg("sender") && agp.hasKWarg("subject") && agp.hasKWarg("recipient") && agp.hasKWarg("body"))){
      System.out.println(helpPage);
      return;
    }
    JMailer mailer = new JMailer(agp.getString("host"), agp.getInt("port"), agp.getString("domain"), agp.getBoolean("ssl"));
    mailer.setSender(agp.getString("sender"));
    mailer.setSubject(agp.getString("subject"));
    for (String addr : agp.getString("recipient").split(","))
      mailer.addRecipient(addr.trim());
    System.out.println("[*] Loading mail body...");
    File file = new File(agp.getString("body"));
    if (file.length() == 0){
      System.out.println("[-] Given file is empty!");
      return;
    }
    DataInputStream dis = new DataInputStream(new FileInputStream(file));
    byte[] buffer = new byte[(int)file.length()];
    dis.read(buffer, 0, (int)file.length());
    String body = new String(buffer);
    System.out.println("[+] Sending mail...");
    if (mailer.send(body))
      System.out.println("[+] Mail sent successfully!");
    else
      System.out.println("[-] Error sending mail!");
  }

}

